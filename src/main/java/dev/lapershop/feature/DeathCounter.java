package dev.lapershop.feature;

import dev.lapershop.config.ConfigManager;
import dev.lapershop.data.DataStore;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.FixedFormat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

/**
 * Jumlah kematian di TAB list: scoreboard objective (slot LIST) + number format kustom
 * "☠ N" (tengkorak merah). Dirender vanilla, jadi pemain tidak butuh mod client.
 * Data permanen disimpan di DataStore (file JSON di folder world).
 */
public final class DeathCounter {
    private DeathCounter() {}

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!ConfigManager.get().deathCounter.enabled) return;
            if (entity instanceof ServerPlayer sp) {
                DataStore.addDeath(sp.getUUID());
                MinecraftServer server = sp.level().getServer();
                if (server != null) apply(server, sp);
            }
        });
    }

    public static void refreshAll(MinecraftServer server) {
        if (!ConfigManager.get().deathCounter.enabled) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) apply(server, p);
    }

    public static void apply(MinecraftServer server, ServerPlayer p) {
        if (!ConfigManager.get().deathCounter.enabled) return;
        Scoreboard sb = server.getScoreboard();
        Objective obj = ensureObjective(sb);
        int deaths = DataStore.deaths(p.getUUID());
        ScoreAccess score = sb.getOrCreatePlayerScore(p, obj);
        score.set(deaths);
        score.numberFormatOverride(new FixedFormat(format(deaths)));
    }

    private static Objective ensureObjective(Scoreboard sb) {
        String name = ConfigManager.get().deathCounter.objectiveName;
        Objective obj = sb.getObjective(name);
        if (obj == null) {
            obj = sb.addObjective(name, ObjectiveCriteria.DUMMY, Component.literal("Deaths"),
                    ObjectiveCriteria.RenderType.INTEGER, true, null);
        }
        if (sb.getDisplayObjective(DisplaySlot.LIST) != obj) {
            sb.setDisplayObjective(DisplaySlot.LIST, obj);
        }
        return obj;
    }

    private static Component format(int deaths) {
        return Component.literal("\u2620 ").withStyle(ChatFormatting.RED)
                .append(Component.literal(String.valueOf(deaths)).withStyle(ChatFormatting.WHITE));
    }
}
