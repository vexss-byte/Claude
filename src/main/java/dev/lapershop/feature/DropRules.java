package dev.lapershop.feature;

import java.util.List;
import java.util.Random;

import dev.lapershop.config.ConfigManager;
import dev.lapershop.config.LaperConfig;
import dev.lapershop.util.Ids;
import dev.lapershop.util.ItemUtil;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.phys.AABB;

/**
 * Kelangkaan Totem of Undying (Evoker) dan buku Mending (memancing + Librarian).
 * Seluruhnya berbasis event Fabric API (tanpa mixin), sehingga tidak bergantung pada nama internal vanilla.
 */
public final class DropRules {
    private DropRules() {}

    public static void register() {
        // --- Totem dari Evoker (termasuk Evoker Raid) ---
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity.getType() == EntityTypes.EVOKER) onEvokerDeath(entity);
        });

        // --- Mending dari memancing ---
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof ItemEntity item && level instanceof ServerLevel sl) onItemSpawn(item, sl);
        });

        // --- Mending dari Librarian ---
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (!level.isClientSide() && entity.getType() == EntityTypes.VILLAGER && entity instanceof Merchant m) {
                rewriteVillager(entity, m, level.registryAccess());
            }
            return InteractionResult.PASS;
        });
    }

    // ------------------------------------------------------------------ Totem
    private static void onEvokerDeath(Entity evoker) {
        LaperConfig.Totem cfg = ConfigManager.get().totem;
        if (!cfg.enabled || !(evoker.level() instanceof ServerLevel level)) return;

        AABB box = evoker.getBoundingBox().inflate(3.0);
        List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, box,
                i -> i.getItem().is(Items.TOTEM_OF_UNDYING));
        for (ItemEntity drop : drops) {
            if (level.getRandom().nextDouble() >= cfg.evokerDropChance) {
                drop.discard();
            }
        }
    }

    // ------------------------------------------------------------------ Fishing
    private static void onItemSpawn(ItemEntity item, ServerLevel level) {
        LaperConfig.Mending cfg = ConfigManager.get().mending;
        if (!cfg.fishingEnabled) return;
        ItemStack stack = item.getItem();
        if (!stack.is(Items.ENCHANTED_BOOK)
                || !ItemUtil.has(stack.get(DataComponents.STORED_ENCHANTMENTS), Enchantments.MENDING)) return;

        // Hanya buku yang muncul tepat di posisi pelampung pancing.
        var hooks = level.getEntities(EntityTypes.FISHING_BOBBER, item.getBoundingBox().inflate(2.0), h -> true);
        if (hooks.isEmpty()) return;

        if (level.getRandom().nextDouble() < cfg.fishingKeepChance) return; // beruntung: Mending tetap

        Item replacement = Ids.item(cfg.fishingReplacementItem);
        item.setItem(new ItemStack(replacement != null ? replacement : Items.NAME_TAG));
    }

    // ------------------------------------------------------------------ Villager
    private static void rewriteVillager(Entity villager, Merchant merchant, net.minecraft.core.RegistryAccess access) {
        LaperConfig.Mending cfg = ConfigManager.get().mending;
        if (!cfg.villagerEnabled) return;

        MerchantOffers offers = merchant.getOffers();
        for (int i = 0; i < offers.size(); i++) {
            MerchantOffer offer = offers.get(i);
            ItemStack result = offer.getResult();
            if (!result.is(Items.ENCHANTED_BOOK)) continue;
            ItemEnchantments stored = result.get(DataComponents.STORED_ENCHANTMENTS);
            if (!ItemUtil.has(stored, Enchantments.MENDING)) continue;

            // Keputusan deterministik per (villager, slot trade): tidak berubah tiap kali dibuka.
            long seed = villager.getUUID().getMostSignificantBits()
                    ^ (villager.getUUID().getLeastSignificantBits() * 31L)
                    ^ ((i + 1L) * 0x9E3779B97F4A7C15L);
            double roll = new Random(seed).nextDouble();
            if (roll < cfg.villagerKeepChance) continue;

            ItemEnchantments.Mutable mut = new ItemEnchantments.Mutable(stored);
            mut.removeIf(h -> h.is(Enchantments.MENDING));
            Holder<Enchantment> repl = Ids.enchantment(access, cfg.villagerReplacementEnchantment);
            if (repl != null) mut.set(repl, cfg.villagerReplacementLevel);
            result.set(DataComponents.STORED_ENCHANTMENTS, mut.toImmutable());
        }
    }
}
