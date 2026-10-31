package dev.lapershop.feature;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import dev.lapershop.config.ConfigManager;
import dev.lapershop.config.LaperConfig;
import dev.lapershop.util.ItemUtil;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * Cooldown Trident (Riptide) dan Ender Pearl.
 *
 * Cooldown dilacak per-PEMAIN (UUID + waktu nyata), bukan per-slot/item. Karena itu memindahkan item,
 * ganti slot, buka inventory, atau relog tidak bisa menghindari cooldown. Seluruh validasi dilakukan
 * di server. Countdown ditampilkan lewat action bar (tepat di atas hotbar), tanpa mod client.
 */
public final class CooldownManager {
    private static final Map<UUID, Long> TRIDENT = new HashMap<>();
    private static final Map<UUID, Long> PEARL = new HashMap<>();
    private static final Set<UUID> SPINNING = new HashSet<>();
    private static final Set<UUID> HUD_ACTIVE = new HashSet<>();

    private CooldownManager() {}

    public static void register() {
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
            LaperConfig.Cooldowns cfg = ConfigManager.get().cooldowns;
            ItemStack stack = player.getItemInHand(hand);
            long now = System.currentTimeMillis();
            UUID id = sp.getUUID();

            if (cfg.enderPearlEnabled && stack.is(Items.ENDER_PEARL)) {
                if (remaining(PEARL, id, now) > 0) return InteractionResult.FAIL;
                PEARL.put(id, now + cfg.enderPearlSeconds * 1000L);
                return InteractionResult.PASS;
            }

            if (cfg.tridentEnabled && stack.is(Items.TRIDENT)
                    && (!cfg.tridentRiptideOnly || hasRiptide(stack))) {
                if (remaining(TRIDENT, id, now) > 0) return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
    }

    public static boolean hasRiptide(ItemStack stack) {
        return ItemUtil.has(stack.get(DataComponents.ENCHANTMENTS), Enchantments.RIPTIDE);
    }

    private static long remaining(Map<UUID, Long> map, UUID id, long now) {
        Long end = map.get(id);
        return end == null ? 0L : Math.max(0L, end - now);
    }

    public static void onDisconnect(ServerPlayer p) {
        SPINNING.remove(p.getUUID());
        HUD_ACTIVE.remove(p.getUUID());
    }

    /** Dipanggil setiap tick server. */
    public static void tick(MinecraftServer server) {
        LaperConfig.Cooldowns cfg = ConfigManager.get().cooldowns;
        long now = System.currentTimeMillis();
        int tick = server.getTickCount();

        if (tick % 200 == 0) { // bersihkan entri kedaluwarsa
            TRIDENT.values().removeIf(end -> end <= now);
            PEARL.values().removeIf(end -> end <= now);
        }

        boolean refreshHud = tick % 2 == 0;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            UUID id = p.getUUID();

            // Deteksi Riptide benar-benar terpakai: pemain mulai "spin attack".
            boolean spin = p.isAutoSpinAttack();
            if (spin) {
                if (SPINNING.add(id) && cfg.tridentEnabled) {
                    TRIDENT.put(id, now + cfg.tridentSeconds * 1000L);
                }
            } else {
                SPINNING.remove(id);
            }

            if (refreshHud && cfg.showHud) updateHud(p, now);
        }
    }

    private static void updateHud(ServerPlayer p, long now) {
        UUID id = p.getUUID();
        long t = remaining(TRIDENT, id, now);
        long e = remaining(PEARL, id, now);

        if (t <= 0 && e <= 0) {
            if (HUD_ACTIVE.remove(id)) p.sendSystemMessage(Component.empty(), true);
            return;
        }
        MutableComponent line = Component.empty();
        if (t > 0) {
            line.append(Component.literal("Trident ").withStyle(ChatFormatting.AQUA))
                .append(Component.literal(fmt(t)).withStyle(ChatFormatting.WHITE));
        }
        if (e > 0) {
            if (t > 0) line.append(Component.literal("  |  ").withStyle(ChatFormatting.DARK_GRAY));
            line.append(Component.literal("Ender Pearl ").withStyle(ChatFormatting.LIGHT_PURPLE))
                .append(Component.literal(fmt(e)).withStyle(ChatFormatting.WHITE));
        }
        HUD_ACTIVE.add(id);
        p.sendSystemMessage(line, true);
    }

    private static String fmt(long ms) {
        return String.format(Locale.ROOT, "%.1fs", ms / 1000.0);
    }
}
