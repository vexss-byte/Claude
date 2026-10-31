package dev.lapershop.util;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public final class ItemUtil {
    private ItemUtil() {}

    public static boolean has(ItemEnchantments enchants, ResourceKey<Enchantment> key) {
        if (enchants == null) return false;
        for (Holder<Enchantment> h : enchants.keySet()) {
            if (h.is(key)) return true;
        }
        return false;
    }
}
