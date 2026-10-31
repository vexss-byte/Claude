package dev.lapershop.shop;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.time.LocalTime;

import dev.lapershop.LaperShopMod;
import dev.lapershop.config.ConfigManager;
import dev.lapershop.config.LaperConfig;
import dev.lapershop.data.DataStore;
import dev.lapershop.util.Gradient;
import dev.lapershop.util.TextUtil;
import dev.lapershop.util.TimeFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;

/** Status event LAPER SHOP: jadwal, pengumuman chat, dan Boss Bar countdown. Semua di thread server. */
public final class ShopManager {
    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    private static ServerBossEvent bar;
    private static ShopSchedule.Window current;
    private static boolean open;

    private ShopManager() {}

    public static boolean isOpen() { return open; }

    /** Kunci cakupan batas beli: "all" (forever) atau epoch mulai event. */
    public static String scopeKey() {
        if ("forever".equalsIgnoreCase(ConfigManager.get().shop.limitScope)) return "all";
        return current == null ? "none" : String.valueOf(current.startEpochSec());
    }

    // ---------------------------------------------------------------- tick (tiap 1 detik)
    public static void tick(MinecraftServer server) {
        LaperConfig.Shop cfg = ConfigManager.get().shop;
        ShopSchedule.Window w = null;
        if (cfg.enabled) {
            w = ShopSchedule.active(Instant.now(), zone(cfg), times(cfg), cfg.durationMinutes);
        }

        if (w == null) {
            if (open) close(server, cfg);
            return;
        }
        if (!open) {
            open(server, cfg, w);
        } else if (current.startEpochSec() != w.startEpochSec()) {
            current = w;
            announceOpenOnce(server, cfg, w);
        } else {
            current = w; // akhir event bisa berubah jika config diedit
        }
        updateBar(cfg);
    }

    private static void open(MinecraftServer server, LaperConfig.Shop cfg, ShopSchedule.Window w) {
        current = w;
        open = true;
        DataStore.prunePurchases(scopeKey());
        announceOpenOnce(server, cfg, w);

        bar = new ServerBossEvent(UUID.randomUUID(), Component.empty(), color(cfg), BossEvent.BossBarOverlay.PROGRESS);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) bar.addPlayer(p);
        LaperShopMod.LOGGER.info("LAPER SHOP dibuka.");
    }

    private static void close(MinecraftServer server, LaperConfig.Shop cfg) {
        open = false;
        current = null;
        if (bar != null) { bar.removeAllPlayers(); bar = null; }
        ShopMerchant.closeAll();
        server.getPlayerList().broadcastSystemMessage(gradient(cfg, cfg.closeMessage), false);
        LaperShopMod.LOGGER.info("LAPER SHOP ditutup.");
    }

    /** Dipanggil saat server berhenti: bersihkan state tanpa pengumuman. */
    public static void shutdown() {
        if (bar != null) { bar.removeAllPlayers(); bar = null; }
        open = false;
        current = null;
        ShopMerchant.closeAll();
    }

    public static void onJoin(ServerPlayer p) {
        if (open && bar != null) bar.addPlayer(p);
    }

    // ---------------------------------------------------------------- pengumuman
    private static void announceOpenOnce(MinecraftServer server, LaperConfig.Shop cfg, ShopSchedule.Window w) {
        if (DataStore.lastAnnouncedStart() == w.startEpochSec()) return; // sudah diumumkan (mis. server restart)
        DataStore.setLastAnnouncedStart(w.startEpochSec());

        String end = ZonedDateTime.ofInstant(Instant.ofEpochSecond(w.endEpochSec()), zone(cfg)).format(HHMM);
        server.getPlayerList().broadcastSystemMessage(gradient(cfg, cfg.openMessage), false);
        if (cfg.openSubMessage != null && !cfg.openSubMessage.isBlank()) {
            server.getPlayerList().broadcastSystemMessage(
                    TextUtil.legacy(cfg.openSubMessage.replace("{end}", end)), false);
        }
    }

    private static MutableComponent gradient(LaperConfig.Shop cfg, String text) {
        return TextUtil.gradient(text, Gradient.parseHex(cfg.gradientStart, 0xFFFF55),
                Gradient.parseHex(cfg.gradientEnd, 0xFFAA00), true);
    }

    // ---------------------------------------------------------------- boss bar
    private static void updateBar(LaperConfig.Shop cfg) {
        if (bar == null || current == null) return;
        long remaining = current.remainingSeconds(Instant.now());
        long total = Math.max(1, current.durationSeconds());

        MutableComponent name = gradient(cfg, cfg.bossBarText)
                .append(Component.literal(" | ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(TimeFormat.hms(remaining)).withStyle(ChatFormatting.WHITE));
        bar.setName(name);
        bar.setProgress(Math.max(0f, Math.min(1f, remaining / (float) total)));
    }

    private static BossEvent.BossBarColor color(LaperConfig.Shop cfg) {
        try {
            return BossEvent.BossBarColor.valueOf(cfg.bossBarColor.trim().toUpperCase());
        } catch (Exception e) {
            return BossEvent.BossBarColor.YELLOW;
        }
    }

    // ---------------------------------------------------------------- helper
    public static String nextOpeningText() {
        LaperConfig.Shop cfg = ConfigManager.get().shop;
        ZonedDateTime next = ShopSchedule.next(Instant.now(), zone(cfg), times(cfg));
        return next == null ? "-" : next.format(HHMM) + " (" + zone(cfg).getId() + ")";
    }

    static ZoneId zone(LaperConfig.Shop cfg) {
        try {
            return ZoneId.of(cfg.timezone);
        } catch (Exception e) {
            LaperShopMod.LOGGER.warn("Timezone '{}' tidak valid, memakai UTC.", cfg.timezone);
            return ZoneOffset.UTC;
        }
    }

    static List<LocalTime> times(LaperConfig.Shop cfg) {
        return ShopSchedule.parseTimes(cfg.startTimes);
    }
}
