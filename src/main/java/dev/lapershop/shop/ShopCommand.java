package dev.lapershop.shop;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import dev.lapershop.config.ConfigManager;
import dev.lapershop.util.TextUtil;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public final class ShopCommand {
    private ShopCommand() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(Commands.literal("lapershop").executes(ctx -> {
                CommandSourceStack source = ctx.getSource();
                ServerPlayer player = source.getPlayerOrException();

                // Validasi sepenuhnya di server: client tidak bisa memaksa membuka toko.
                if (!ShopManager.isOpen()) {
                    String msg = ConfigManager.get().shop.closedMessage.replace("{next}", ShopManager.nextOpeningText());
                    source.sendFailure(TextUtil.legacy(msg));
                    return 0;
                }
                ShopMerchant.open(player);
                return 1;
            })));
    }
}
