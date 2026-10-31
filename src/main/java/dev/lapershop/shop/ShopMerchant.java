package dev.lapershop.shop;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

import dev.lapershop.LaperShopMod;
import dev.lapershop.config.ConfigManager;
import dev.lapershop.config.LaperConfig;
import dev.lapershop.data.DataStore;
import dev.lapershop.util.Ids;
import dev.lapershop.util.TextUtil;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

/**
 * GUI toko ala Villager Trading: Merchant virtual per-pemain yang membuka MerchantMenu vanilla.
 * Pemain tidak butuh mod client. Penawaran dibuat dari config; batas beli dihitung per-pemain
 * dari DataStore dan seluruh transaksi divalidasi oleh logika vanilla di server.
 */
public final class ShopMerchant implements Merchant {
    private static final Set<ShopMerchant> OPEN = new HashSet<>();
    private static final int UNLIMITED = 99999;

    private final ServerPlayer owner;
    private final MerchantOffers offers = new MerchantOffers();
    private final Map<MerchantOffer, LaperConfig.Offer> source = new IdentityHashMap<>();
    private Player trading;

    private ShopMerchant(ServerPlayer owner) { this.owner = owner; }

    // ---------------------------------------------------------------- membuka GUI
    public static void open(ServerPlayer player) {
        if (!ShopManager.isOpen()) return; // validasi server: toko harus sedang buka

        player.closeContainer();
        ShopMerchant m = new ShopMerchant(player);
        m.build(player.level().registryAccess());
        if (m.offers.isEmpty()) {
            player.sendSystemMessage(TextUtil.legacy("&cBelum ada penawaran yang tersedia."));
            return;
        }
        m.trading = player;
        OptionalInt id = player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, p) -> new MerchantMenu(containerId, inventory, m),
                TextUtil.legacy("&6&l" + ConfigManager.get().shop.guiTitle)));
        if (id.isPresent()) {
            OPEN.add(m);
            // level 5 + showProgress=false: tampilan bersih tanpa bar XP & tanpa restock.
            player.sendMerchantOffers(id.getAsInt(), m.offers, 5, 0, false, false);
        }
    }

    /** Tutup semua GUI toko yang terbuka (dipakai saat event berakhir). */
    public static void closeAll() {
        for (ShopMerchant m : new ArrayList<>(OPEN)) {
            if (m.trading instanceof ServerPlayer sp) {
                sp.closeContainer();
                sp.sendSystemMessage(TextUtil.legacy("&cLAPER SHOP telah ditutup."));
            }
        }
        OPEN.clear();
    }

    // ---------------------------------------------------------------- membangun penawaran dari config
    private void build(RegistryAccess access) {
        LaperConfig.Shop shop = ConfigManager.get().shop;
        for (LaperConfig.Offer o : shop.offers) {
            if (o == null || !o.enabled || o.id == null || o.payment == null || o.reward == null) continue;

            ItemCost costA = cost(o.payment);
            if (costA == null) continue;
            Optional<ItemCost> costB = Optional.empty();
            if (o.payment2 != null && o.payment2.item != null) {
                costB = Optional.ofNullable(cost(o.payment2));
            }
            ItemStack result = ShopItems.build(o.reward, access);
            if (result == null || result.isEmpty()) continue;

            int limit = Math.max(0, o.perPlayerLimit);
            int used = limit > 0 ? Math.min(limit, DataStore.purchases(key(owner, o))) : 0;
            int maxUses = limit > 0 ? limit : UNLIMITED;

            MerchantOffer offer = new MerchantOffer(costA, costB, result, used, maxUses, 0, 0f);
            offers.add(offer);
            source.put(offer, o);
        }
    }

    private static ItemCost cost(LaperConfig.ItemSpec spec) {
        Item item = Ids.item(spec.item);
        if (item == null) {
            LaperShopMod.LOGGER.warn("Item pembayaran tidak dikenal di config: '{}'", spec.item);
            return null;
        }
        int max = new ItemStack(item).getMaxStackSize();
        return new ItemCost(item, Math.max(1, Math.min(spec.count, max)));
    }

    private static String key(ServerPlayer p, LaperConfig.Offer o) {
        return p.getUUID() + "|" + o.id + "|" + ShopManager.scopeKey();
    }

    // ---------------------------------------------------------------- Merchant
    @Override
    public void setTradingPlayer(Player player) {
        this.trading = player;
        if (player == null) OPEN.remove(this);
    }

    @Override
    public Player getTradingPlayer() { return trading; }

    @Override
    public MerchantOffers getOffers() { return offers; }

    @Override
    public void overrideOffers(MerchantOffers newOffers) { /* tidak dipakai */ }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        LaperConfig.Offer cfg = source.get(offer);
        if (cfg == null || !(trading instanceof ServerPlayer sp)) return;

        DataStore.addPurchase(key(sp, cfg)); // batas beli permanen per-pemain

        if (cfg.bonusRewards != null) {
            RegistryAccess access = sp.level().registryAccess();
            for (LaperConfig.ItemSpec bonus : cfg.bonusRewards) {
                ItemStack stack = ShopItems.build(bonus, access);
                if (stack == null || stack.isEmpty()) continue;
                if (!sp.getInventory().add(stack)) sp.drop(stack, false);
            }
        }
    }

    @Override
    public void notifyTradeUpdated(ItemStack stack) { /* tidak dipakai */ }

    @Override
    public int getVillagerXp() { return 0; }

    @Override
    public void overrideXp(int xp) { /* tidak dipakai */ }

    @Override
    public boolean showProgressBar() { return false; }

    @Override
    public SoundEvent getNotifyTradeSound() { return SoundEvents.VILLAGER_YES; }

    @Override
    public boolean isClientSide() { return false; }

    /** Menu tetap valid hanya selama toko buka dan pemain adalah pemilik sesi. */
    public boolean stillValid(Player player) {
        return ShopManager.isOpen() && player == trading;
    }
}
