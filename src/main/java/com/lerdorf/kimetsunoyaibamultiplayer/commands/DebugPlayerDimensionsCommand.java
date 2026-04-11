package com.lerdorf.kimetsunoyaibamultiplayer.commands;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.blooddemonarts.SwampDemonArt;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class DebugPlayerDimensionsCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("debugplayerdims")
            .requires(source -> source.hasPermission(0))
            .executes(context -> printStatus(context.getSource()))
            .then(Commands.literal("clear")
                .executes(context -> clearOverride(context.getSource())))
            .then(Commands.argument("height", FloatArgumentType.floatArg(0.01F, 10.0F))
                .then(Commands.argument("eyeHeight", FloatArgumentType.floatArg(0.0F, 10.0F))
                    .executes(context -> setOverride(
                        context.getSource(),
                        FloatArgumentType.getFloat(context, "height"),
                        FloatArgumentType.getFloat(context, "eyeHeight"))))));
    }

    private static int printStatus(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("[debugplayerdims server] " + SwampDemonArt.buildDimensionDebugSummary(player)), false);
        return 1;
    }

    private static int setOverride(CommandSourceStack source, float height, float eyeHeight) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Before: " + SwampDemonArt.buildDimensionDebugSummary(player)), false);
        SwampDemonArt.setDebugDimensionOverride(player, height, eyeHeight);
        String summary = SwampDemonArt.buildDimensionDebugSummary(player);
        Log.alwaysWarn("Server debug dimensions applied for {}: {}", player.getScoreboardName(), summary);
        source.sendSuccess(() -> Component.literal("Applied override height=" + height + " eye=" + eyeHeight), false);
        source.sendSuccess(() -> Component.literal("After: " + summary), false);
        return 1;
    }

    private static int clearOverride(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Before clear: " + SwampDemonArt.buildDimensionDebugSummary(player)), false);
        SwampDemonArt.clearDebugDimensionOverride(player);
        String summary = SwampDemonArt.buildDimensionDebugSummary(player);
        Log.alwaysWarn("Server debug dimensions cleared for {}: {}", player.getScoreboardName(), summary);
        source.sendSuccess(() -> Component.literal("After clear: " + summary), false);
        return 1;
    }
}
