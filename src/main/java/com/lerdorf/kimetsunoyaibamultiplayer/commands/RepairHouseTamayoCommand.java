package com.lerdorf.kimetsunoyaibamultiplayer.commands;

import com.lerdorf.kimetsunoyaibamultiplayer.quest.QuestScenarioActions;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class RepairHouseTamayoCommand {
    private RepairHouseTamayoCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("repairhousetamayo")
            .requires(source -> source.hasPermission(2))
            .executes(context -> {
                CommandSourceStack source = context.getSource();
                if (!source.isPlayer()) {
                    source.sendFailure(Component.literal("This command can only be used by players."));
                    return 0;
                }
                ServerPlayer player = source.getPlayer();
                if (player == null) {
                    source.sendFailure(Component.literal("Could not resolve player."));
                    return 0;
                }
                boolean repaired = QuestScenarioActions.repairNearestTamayoHouse(player);
                if (repaired) {
                    source.sendSuccess(() -> Component.literal("Repaired nearest Tamayo house structure."), true);
                    return 1;
                }
                source.sendFailure(Component.literal("No nearby Tamayo house structure found to repair."));
                return 0;
            }));
    }
}
