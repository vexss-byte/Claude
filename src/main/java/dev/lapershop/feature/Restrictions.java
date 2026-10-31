package dev.lapershop.feature;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import dev.lapershop.LaperShopMod;
import dev.lapershop.config.ConfigManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;

/**
 * Menonaktifkan koordinat (gamerule reducedDebugInfo=true) dan Locator Bar (gamerule locatorBar=false).
 * Dijalankan lewat command /gamerule (nama rule dicoba snake_case lalu camelCase, supaya tahan perubahan nama)
 * dan ditegakkan ulang secara berkala bila ada yang mengubahnya kembali.
 */
public final class Restrictions {
    private static String coordRule = null;
    private static String locatorRule = null;

    private Restrictions() {}

    public static void apply(MinecraftServer server) {
        var cfg = ConfigManager.get().restrictions;
        if (cfg.disableCoordinates) {
            coordRule = enforce(server, coordRule, true, "reduced_debug_info", "reducedDebugInfo");
        }
        if (cfg.disableLocatorBar) {
            locatorRule = enforce(server, locatorRule, false, "locator_bar", "locatorBar");
        }
    }

    /** @return nama rule yang berhasil dipakai (di-cache). */
    private static String enforce(MinecraftServer server, String known, boolean value, String... names) {
        CommandSourceStack src = server.createCommandSourceStack().withSuppressedOutput();
        String[] candidates = known != null ? new String[] { known } : names;
        for (String name : candidates) {
            try {
                int current = server.getCommands().getDispatcher().execute("gamerule " + name, src);
                if (current != (value ? 1 : 0)) {
                    server.getCommands().getDispatcher().execute("gamerule " + name + " " + value, src);
                }
                return name;
            } catch (CommandSyntaxException ignored) {
                // coba nama berikutnya
            }
        }
        LaperShopMod.LOGGER.warn("Gamerule tidak ditemukan (dicoba: {})", String.join(", ", names));
        return known;
    }
}
