package dev.lapershop.util;

import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;

public final class Ids {
    private Ids() {}

    /** Item dari id string (mis. "minecraft:diamond" atau "modid:item"). null jika tidak ada. */
    public static Item item(String s) {
        if (s == null) return null;
        Identifier id = Identifier.tryParse(s.trim());
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) return null;
        return BuiltInRegistries.ITEM.getValue(id);
    }

    public static Holder<Enchantment> enchantment(RegistryAccess access, String s) {
        if (s == null) return null;
        Identifier id = Identifier.tryParse(s.trim());
        if (id == null) return null;
        ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, id);
        Optional<Holder.Reference<Enchantment>> h = access.lookupOrThrow(Registries.ENCHANTMENT).get(key);
        return h.orElse(null);
    }
}
