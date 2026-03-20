package com.lerdorf.kimetsunoyaibamultiplayer.commands;

import com.lerdorf.kimetsunoyaibamultiplayer.util.SunBreathingLevelHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Sets sun breathing level progression (0-12).
 *
 * Usage:
 * - /sunbreathinglevel <level>                    (self)
 * - /sunbreathinglevel <player> <level>           (target player)
 */
public class SunBreathingLevelCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("sunbreathinglevel")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("level", IntegerArgumentType.integer(
                SunBreathingLevelHelper.MIN_LEVEL, SunBreathingLevelHelper.MAX_LEVEL))
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    ServerPlayer self = source.getPlayerOrException();
                    int level = IntegerArgumentType.getInteger(context, "level");
                    return setLevel(source, self, level);
                }))
            .then(Commands.argument("target", EntityArgument.player())
                .then(Commands.argument("level", IntegerArgumentType.integer(
                    SunBreathingLevelHelper.MIN_LEVEL, SunBreathingLevelHelper.MAX_LEVEL))
                    .executes(context -> {
                        CommandSourceStack source = context.getSource();
                        ServerPlayer target = EntityArgument.getPlayer(context, "target");
                        int level = IntegerArgumentType.getInteger(context, "level");
                        return setLevel(source, target, level);
                    })));

        dispatcher.register(command);
    }

    private static int setLevel(CommandSourceStack source, ServerPlayer target, int level) {
        int clamped = SunBreathingLevelHelper.clampLevel(level);
        SunBreathingLevelHelper.setSunBreathingLevel(target, clamped);

        source.sendSuccess(() -> Component.literal(
            "Set sun breathing level for " + target.getName().getString() + " to " + clamped
        ), true);
        return 1;
    }
}
