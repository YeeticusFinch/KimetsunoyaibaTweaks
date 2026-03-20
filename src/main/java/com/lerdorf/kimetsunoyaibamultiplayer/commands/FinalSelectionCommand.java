package com.lerdorf.kimetsunoyaibamultiplayer.commands;

import com.lerdorf.kimetsunoyaibamultiplayer.raids.FinalSelectionProcedure;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/**
 * Registers /finalselection subcommands for exit confirmation prompts and ore selection.
 */
public class FinalSelectionCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("finalselection")
            .then(Commands.literal("ore")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    if (!FinalSelectionProcedure.reopenOreSelection(player)) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "\u00a7cOre selection is only available during Final Selection in Mt. Fujikasane."
                        ));
                        return 0;
                    }
                    return 1;
                })
            )
            .then(Commands.literal("leave")
                .then(Commands.literal("confirm")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        return FinalSelectionProcedure.confirmExit(player) ? 1 : 0;
                    })
                )
                .then(Commands.literal("cancel")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        FinalSelectionProcedure.cancelExit(player);
                        return 1;
                    })
                )
            )
            .then(Commands.literal("kakushi")
                .then(Commands.literal("accept")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        return FinalSelectionProcedure.acceptKakushiOffer(player) ? 1 : 0;
                    })
                )
                .then(Commands.literal("decline")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        FinalSelectionProcedure.declineKakushiOffer(player);
                        return 1;
                    })
                )
            );

        dispatcher.register(command);
    }
}
