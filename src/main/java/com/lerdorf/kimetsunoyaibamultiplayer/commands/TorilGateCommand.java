package com.lerdorf.kimetsunoyaibamultiplayer.commands;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.TorilGateTeleportHandler;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/**
 * Registers /torilgate confirm|cancel|return subcommands.
 * No permission required since confirm/cancel are gated by server-side pending map.
 */
public class TorilGateCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("torilgate")
            .then(Commands.literal("confirm")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    return TorilGateTeleportHandler.confirmTeleport(player) ? 1 : 0;
                })
            )
            .then(Commands.literal("cancel")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    TorilGateTeleportHandler.cancelTeleport(player);
                    return 1;
                })
            )
            .then(Commands.literal("return")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    return TorilGateTeleportHandler.returnToPreviousPosition(player) ? 1 : 0;
                })
            );

        dispatcher.register(command);
    }
}
