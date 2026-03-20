package com.lerdorf.kimetsunoyaibamultiplayer.commands;

import com.lerdorf.kimetsunoyaibamultiplayer.events.SwordsmithVillageEscortHandler;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public class SwordsmithVillageCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("swordsmithvillage")
            .then(Commands.literal("confirm")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    return SwordsmithVillageEscortHandler.confirmEscort(player) ? 1 : 0;
                }))
            .then(Commands.literal("cancel")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    SwordsmithVillageEscortHandler.cancelEscort(player);
                    return 1;
                }));

        dispatcher.register(command);
    }
}
