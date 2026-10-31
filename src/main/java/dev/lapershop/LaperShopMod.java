package dev.lapershop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.lapershop.config.ConfigManager;
import dev.lapershop.data.DataStore;
import dev.lapershop.feature.CooldownManager;
import dev.lapershop.feature.DeathCounter;
import dev.lapershop.feature.DropRules;
import dev.lapershop.feature.Restrictions;
import dev.lapershop.shop.ShopCommand;
import dev.lapershop.shop.ShopManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;

public final class LaperShopMod implements ModInitializer {
    public static final String MOD_ID = "lapershop";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ConfigManager.load();

        CooldownManager.register();
        DropRules.register();
        DeathCounter.register();
        ShopCommand.register();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            DataStore.load(server);
            Restrictions.apply(server);
            DeathCounter.refreshAll(server);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            DataStore.save();
            ShopManager.shutdown();
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            DeathCounter.apply(server, handler.getPlayer());
            ShopManager.onJoin(handler.getPlayer());
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                CooldownManager.onDisconnect(handler.getPlayer()));

        ServerTickEvents.END_SERVER_TICK.register(LaperShopMod::tick);
        LOGGER.info("LaperShop dimuat.");
    }

    private static void tick(MinecraftServer server) {
        int t = server.getTickCount();
        CooldownManager.tick(server);
        if (t % 20 == 0) ShopManager.tick(server);
        if (t % 200 == 0) {
            ConfigManager.reloadIfChanged();
            Restrictions.apply(server);
        }
    }
}
