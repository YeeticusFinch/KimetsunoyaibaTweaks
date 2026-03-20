package com.lerdorf.kimetsunoyaibamultiplayer.commands;

import com.lerdorf.kimetsunoyaibamultiplayer.raids.FinalSelectionProcedure;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/**
 * Opens the standalone Nichirin ore selection menu for the executing player.
 */
public final class OreSelectCommand {
    private OreSelectCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("oreselect")
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                FinalSelectionProcedure.openStandaloneOreSelection(player);
                return 1;
            }));
    }
}
