package com.lerdorf.kimetsunoyaibamultiplayer.commands;

import com.lerdorf.kimetsunoyaibamultiplayer.quest.QuestScenarioActions;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class LocalPosCommand {
    private LocalPosCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("localpos")
            .requires(source -> source.hasPermission(2))
            .executes(context -> execute(context.getSource())));
        dispatcher.register(Commands.literal("localposition")
            .requires(source -> source.hasPermission(2))
            .executes(context -> execute(context.getSource())));
    }

    private static int execute(CommandSourceStack source) {
        if (!source.isPlayer()) {
            source.sendFailure(Component.literal("This command can only be used by players."));
            return 0;
        }

        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Could not resolve player."));
            return 0;
        }

        var localPos = QuestScenarioActions.getCurrentStructureLocalPosition(player);
        if (localPos == null) {
            source.sendFailure(Component.literal("No kimetsunoyaiba or kimetsunoyaibamultiplayer structure found at your position."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(
            "Local position: " + localPos.getX() + " " + localPos.getY() + " " + localPos.getZ()
        ), false);
        return 1;
    }
}
