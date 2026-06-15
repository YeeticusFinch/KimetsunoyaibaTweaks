package com.lerdorf.kimetsunoyaibamultiplayer.commands;

import com.lerdorf.kimetsunoyaibamultiplayer.quest.QuestScenarioActions;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class TestTamayoHouseCommand {
    private TestTamayoHouseCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("testtamayohouse")
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

                if (!QuestScenarioActions.startTamayoHouseTest(player)) {
                    source.sendFailure(Component.literal("No nearby Tamayo house structure found to test."));
                    return 0;
                }

                source.sendSuccess(() -> Component.literal("Tamayo house test particles enabled for 45 seconds."), true);
                return 1;
            }));
    }
}
