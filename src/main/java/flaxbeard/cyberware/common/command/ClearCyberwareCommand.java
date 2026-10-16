package flaxbeard.cyberware.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.CyberwareUserData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /clearcyberware [player]}: resets the cyberware of a player to the default body parts.
 */
public final class ClearCyberwareCommand {
    private ClearCyberwareCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("clearcyberware")
                .requires(source -> source.hasPermission(2))
                .executes(context -> clear(context.getSource(), context.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> clear(context.getSource(), EntityArgument.getPlayer(context, "player")))));
    }

    private static int clear(CommandSourceStack source, ServerPlayer player) throws CommandSyntaxException {
        CyberwareUserData data = CyberwareAPI.getUserData(player);
        if (data == null) return 0;
        data.resetWare(player);
        CyberwareAPI.updateData(player);
        source.sendSuccess(() -> Component.translatable("cyberware.commands.clear_cyberware.success", player.getName()), true);
        return 1;
    }
}
