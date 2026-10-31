package dev.lapershop.shop;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dev.lapershop.LaperShopMod;
import dev.lapershop.config.LaperConfig;
import dev.lapershop.util.Ids;
import dev.lapershop.util.TextUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/** Membangun ItemStack dari ItemSpec config (item vanilla & mod, nama, lore, enchantment). */
public final class ShopItems {
    private ShopItems() {}

    /** @return stack, atau null jika item tidak dikenal (mod belum terpasang / salah ketik). */
    public static ItemStack build(LaperConfig.ItemSpec spec, RegistryAccess access) {
        if (spec == null) return null;
        Item item = Ids.item(spec.item);
        if (item == null) {
            LaperShopMod.LOGGER.warn("Item tidak dikenal di config: '{}'", spec.item);
            return null;
        }
        int max = new ItemStack(item).getMaxStackSize();
        ItemStack stack = new ItemStack(item, Math.max(1, Math.min(spec.count, max)));

        if (spec.name != null && !spec.name.isBlank()) {
            stack.set(DataComponents.CUSTOM_NAME, TextUtil.legacy(spec.name));
        }
        if (spec.lore != null && !spec.lore.isEmpty()) {
            List<Component> lines = new ArrayList<>();
            for (String l : spec.lore) lines.add(TextUtil.legacy(l));
            stack.set(DataComponents.LORE, new ItemLore(lines));
        }
        if (spec.enchantments != null && !spec.enchantments.isEmpty()) {
            ItemEnchantments.Mutable mut = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            for (Map.Entry<String, Integer> e : spec.enchantments.entrySet()) {
                Holder<Enchantment> h = Ids.enchantment(access, e.getKey());
                if (h == null) {
                    LaperShopMod.LOGGER.warn("Enchantment tidak dikenal di config: '{}'", e.getKey());
                    continue;
                }
                mut.set(h, Math.max(1, e.getValue() == null ? 1 : e.getValue()));
            }
            DataComponentType<ItemEnchantments> type = stack.is(Items.ENCHANTED_BOOK)
                    ? DataComponents.STORED_ENCHANTMENTS : DataComponents.ENCHANTMENTS;
            stack.set(type, mut.toImmutable());
        }
        return stack;
    }
}
