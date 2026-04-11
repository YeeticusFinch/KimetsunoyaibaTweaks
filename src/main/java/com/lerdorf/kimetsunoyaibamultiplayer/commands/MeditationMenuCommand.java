package com.lerdorf.kimetsunoyaibamultiplayer.commands;

import com.lerdorf.kimetsunoyaibamultiplayer.config.CustomProgressionConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.meditation.MeditationMenuService;
import com.lerdorf.kimetsunoyaibamultiplayer.meditation.MeditationPromptHandler;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class MeditationMenuCommand {
    private MeditationMenuCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("meditation")
            .requires(CommandSourceStack::isPlayer)
            .then(Commands.literal("confirm").executes(context -> respond(context.getSource().getPlayerOrException(), true)))
            .then(Commands.literal("decline").executes(context -> respond(context.getSource().getPlayerOrException(), false)))
            .then(Commands.literal("open").executes(context -> openDirect(context.getSource().getPlayerOrException())))
        );
    }

    private static int respond(ServerPlayer player, boolean accepted) {
        if (!CustomProgressionConfig.isCustomProgressionEnabled()) {
            player.sendSystemMessage(Component.literal("§cCustom progression is disabled."));
            return 0;
        }
        if (!MeditationPromptHandler.hasPendingPrompt(player)) {
            player.sendSystemMessage(Component.literal("§cYou do not have a pending meditation prompt."));
            return 0;
        }

        MeditationPromptHandler.clearPendingPrompt(player);
        MeditationPromptHandler.applyResponseCooldown(player);
        if (!accepted) {
            player.sendSystemMessage(Component.literal("§7You decide not to meditate right now."));
            return 1;
        }

        MeditationMenuService.openFor(player);
        return 1;
    }

    private static int openDirect(ServerPlayer player) {
        if (!CustomProgressionConfig.isCustomProgressionEnabled()) {
            player.sendSystemMessage(Component.literal("§cCustom progression is disabled."));
            return 0;
        }
        MeditationPromptHandler.clearPendingPrompt(player);
        MeditationMenuService.openFor(player);
        return 1;
    }
}
