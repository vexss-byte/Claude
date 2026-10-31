package dev.lapershop.data;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import dev.lapershop.LaperShopMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/** Penyimpanan permanen (file JSON di folder world): jumlah kematian, batas pembelian, status pengumuman. */
public final class DataStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path file;
    private static Data data = new Data();

    public static final class Data {
        public Map<String, Integer> deaths = new HashMap<>();
        public Map<String, Integer> purchases = new HashMap<>();
        public long lastAnnouncedStart = 0L;
    }

    private DataStore() {}

    public static void load(MinecraftServer server) {
        file = server.getWorldPath(LevelResource.ROOT).resolve("lapershop-data.json");
        data = new Data();
        if (Files.exists(file)) {
            try (Reader r = Files.newBufferedReader(file)) {
                Data d = GSON.fromJson(r, Data.class);
                if (d != null) data = d;
                if (data.deaths == null) data.deaths = new HashMap<>();
                if (data.purchases == null) data.purchases = new HashMap<>();
            } catch (Exception e) {
                LaperShopMod.LOGGER.error("Gagal membaca {}", file, e);
            }
        }
    }

    public static synchronized void save() {
        if (file == null) return;
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            try (Writer w = Files.newBufferedWriter(tmp)) { GSON.toJson(data, w); }
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicFail) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            LaperShopMod.LOGGER.error("Gagal menyimpan {}", file, e);
        }
    }

    // ---- deaths ----
    public static int deaths(UUID id) { return data.deaths.getOrDefault(id.toString(), 0); }

    public static int addDeath(UUID id) {
        int n = deaths(id) + 1;
        data.deaths.put(id.toString(), n);
        save();
        return n;
    }

    // ---- purchases ----
    public static int purchases(String key) { return data.purchases.getOrDefault(key, 0); }

    public static void addPurchase(String key) {
        data.purchases.merge(key, 1, Integer::sum);
        save();
    }

    /** Hapus catatan scope event lama; scope "all" dan scope aktif dipertahankan. */
    public static void prunePurchases(String keepScope) {
        boolean changed = data.purchases.keySet().removeIf(k -> {
            int i = k.lastIndexOf('|');
            if (i < 0) return false;
            String scope = k.substring(i + 1);
            return !scope.equals("all") && !scope.equals(keepScope);
        });
        if (changed) save();
    }

    // ---- announcement state ----
    public static long lastAnnouncedStart() { return data.lastAnnouncedStart; }

    public static void setLastAnnouncedStart(long v) {
        data.lastAnnouncedStart = v;
        save();
    }
}
