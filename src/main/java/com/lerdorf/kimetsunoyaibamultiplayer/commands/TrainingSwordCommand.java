package com.lerdorf.kimetsunoyaibamultiplayer.commands;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.util.TrainingSwordHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Command to convert held sword to/from training sword mode.
 *
 * Usage:
 * - /trainingsword - Convert held sword to training mode
 * - /trainingsword remove - Remove training mode from held sword
 */
public class TrainingSwordCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("trainingsword")
            .requires(source -> source.hasPermission(2)) // Requires OP level 2
            .executes(context -> {
                // Default: make training sword
                CommandSourceStack source = context.getSource();
                if (source.isPlayer()) {
                    ServerPlayer player = source.getPlayer();
                    if (player != null) {
                        return executeMakeTrainingSword(player);
                    }
                }
                source.sendFailure(Component.literal("This command can only be used by players"));
                return 0;
            })
            .then(Commands.literal("remove")
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    if (source.isPlayer()) {
                        ServerPlayer player = source.getPlayer();
                        if (player != null) {
                            return executeRemoveTrainingSword(player);
                        }
                    }
                    source.sendFailure(Component.literal("This command can only be used by players"));
                    return 0;
                })
            );

        dispatcher.register(command);
    }

    private static int executeMakeTrainingSword(ServerPlayer player) {
        ItemStack heldItem = player.getMainHandItem();

        if (heldItem.isEmpty()) {
            player.sendSystemMessage(Component.literal("\u00A7cYou must be holding a sword to use this command."));
            return 0;
        }

        // Check if already a training sword
        if (TrainingSwordHelper.isTrainingSword(heldItem)) {
            player.sendSystemMessage(Component.literal("\u00A7eThis sword is already a training sword."));
            return 0;
        }

        // Check if it's a valid nichirin/breathing sword
        if (!TrainingSwordHelper.isNichirinOrBreathingSword(heldItem)) {
            player.sendSystemMessage(Component.literal("\u00A7cThis item is not a nichirin or breathing sword."));
            return 0;
        }

        // Convert to training sword (this will also reset to first form)
        boolean success = TrainingSwordHelper.makeTrainingSword(heldItem, player);

        if (success) {
            player.sendSystemMessage(Component.literal("\u00A7aSuccessfully converted to training sword!"));
            player.sendSystemMessage(Component.literal("\u00A77Only the 1st Form is available on training swords."));
            Log.debug("Player {} converted sword to training mode", player.getName().getString());
            return 1;
        } else {
            player.sendSystemMessage(Component.literal("\u00A7cFailed to convert to training sword."));
            return 0;
        }
    }

    private static int executeRemoveTrainingSword(ServerPlayer player) {
        ItemStack heldItem = player.getMainHandItem();

        if (heldItem.isEmpty()) {
            player.sendSystemMessage(Component.literal("\u00A7cYou must be holding a sword to use this command."));
            return 0;
        }

        // Check if it's a training sword
        if (!TrainingSwordHelper.isTrainingSword(heldItem)) {
            player.sendSystemMessage(Component.literal("\u00A7eThis sword is not a training sword."));
            return 0;
        }

        // Remove training sword mode
        boolean success = TrainingSwordHelper.removeTrainingSword(heldItem);

        if (success) {
            player.sendSystemMessage(Component.literal("\u00A7aSuccessfully removed training mode from sword!"));
            Log.debug("Player {} removed training mode from sword", player.getName().getString());
            return 1;
        } else {
            player.sendSystemMessage(Component.literal("\u00A7cFailed to remove training mode."));
            return 0;
        }
    }
}
