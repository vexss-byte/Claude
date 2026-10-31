package dev.lapershop.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import dev.lapershop.LaperShopMod;
import net.fabricmc.loader.api.FabricLoader;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static volatile LaperConfig config = new LaperConfig();
    private static long lastModified = 0L;

    private ConfigManager() {}

    public static LaperConfig get() { return config; }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("lapershop.json");
    }

    public static void load() {
        Path p = path();
        try {
            if (!Files.exists(p)) {
                LaperConfig fresh = new LaperConfig();
                fresh.sanitize();
                config = fresh;
                try (Writer w = Files.newBufferedWriter(p)) { GSON.toJson(fresh, w); }
                lastModified = Files.getLastModifiedTime(p).toMillis();
                LaperShopMod.LOGGER.info("Config default dibuat: {}", p);
                return;
            }
            try (Reader r = Files.newBufferedReader(p)) {
                LaperConfig c = GSON.fromJson(r, LaperConfig.class);
                if (c == null) c = new LaperConfig();
                c.sanitize();
                config = c;
            }
            lastModified = Files.getLastModifiedTime(p).toMillis();
        } catch (Exception e) {
            LaperShopMod.LOGGER.error("Gagal membaca {} - memakai konfigurasi sebelumnya.", p, e);
        }
    }

    /** Hot-reload: dipanggil berkala; memuat ulang jika file berubah. */
    public static void reloadIfChanged() {
        try {
            Path p = path();
            if (!Files.exists(p)) return;
            long m = Files.getLastModifiedTime(p).toMillis();
            if (m != lastModified) {
                LaperShopMod.LOGGER.info("lapershop.json berubah, memuat ulang...");
                load();
            }
        } catch (IOException e) {
            LaperShopMod.LOGGER.warn("Gagal memeriksa perubahan config", e);
        }
    }
}
