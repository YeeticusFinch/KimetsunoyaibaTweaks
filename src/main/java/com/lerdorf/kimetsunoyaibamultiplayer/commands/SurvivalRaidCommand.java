package com.lerdorf.kimetsunoyaibamultiplayer.commands;

import com.lerdorf.kimetsunoyaibamultiplayer.config.SurvivalRaidConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.raids.SurvivalRaid;
import com.lerdorf.kimetsunoyaibamultiplayer.raids.SurvivalRaidRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/**
 * Manual command controls for survival raids.
 */
public class SurvivalRaidCommand {

    private SurvivalRaidCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("survivalraid")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("start")
                .then(Commands.argument("level", IntegerArgumentType.integer(1, 5))
                    .executes(ctx -> startRaid(
                        ctx.getSource(),
                        IntegerArgumentType.getInteger(ctx, "level"),
                        SurvivalRaidConfig.defaultRadius.get()
                    ))
                    .then(Commands.argument("radius", IntegerArgumentType.integer(32, 1000))
                        .executes(ctx -> startRaid(
                            ctx.getSource(),
                            IntegerArgumentType.getInteger(ctx, "level"),
                            IntegerArgumentType.getInteger(ctx, "radius")
                        )))))
            .then(Commands.literal("stop")
                .executes(ctx -> stopRaid(ctx.getSource())))
            .then(Commands.literal("status")
                .executes(ctx -> status(ctx.getSource())))
        );
    }

    private static int startRaid(CommandSourceStack source, int difficultyLevel, int radius) {
        if (!SurvivalRaidConfig.enableSurvivalRaids.get()) {
            source.sendFailure(Component.literal("Survival raids are disabled in config."));
            return 0;
        }

        ServerLevel level = source.getLevel();
        long timeOfDay = level.getDayTime() % 24000L;
        if (timeOfDay < 13000L || timeOfDay > 23000L) {
            source.sendFailure(Component.literal("Survival raids can only start at night (13000-23000)."));
            return 0;
        }

        SurvivalRaid existing = SurvivalRaidRegistry.getRaid(level);
        if (existing != null) {
            source.sendFailure(Component.literal("A survival raid is already active in this dimension."));
            return 0;
        }

        SurvivalRaid raid = SurvivalRaidRegistry.createRaid(
            level,
            net.minecraft.core.BlockPos.containing(source.getPosition()),
            radius,
            difficultyLevel
        );

        source.sendSuccess(() -> Component.literal("Started survival raid (level " + raid.getDifficultyLevel() + ", radius " + raid.getRadius() + ")."), true);
        return 1;
    }

    private static int stopRaid(CommandSourceStack source) {
        boolean stopped = SurvivalRaidRegistry.stopRaid(source.getLevel(), "Stopped by command");
        if (!stopped) {
            source.sendFailure(Component.literal("No active survival raid in this dimension."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Stopped survival raid."), true);
        return 1;
    }

    private static int status(CommandSourceStack source) {
        SurvivalRaid raid = SurvivalRaidRegistry.getRaid(source.getLevel());
        if (raid == null) {
            source.sendSuccess(() -> Component.literal("No active survival raid in this dimension."), false);
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Survival raid status:"), false);
        source.sendSuccess(() -> Component.literal("- State: " + raid.getState()), false);
        source.sendSuccess(() -> Component.literal("- Difficulty: " + raid.getDifficultyLevel()), false);
        source.sendSuccess(() -> Component.literal("- Wave: " + raid.getCurrentWave()), false);
        source.sendSuccess(() -> Component.literal("- Bosses defeated: " + raid.getBossesDefeated()), false);
        source.sendSuccess(() -> Component.literal("- Alive raid entities: " + raid.getAliveEntityCount()), false);
        source.sendSuccess(() -> Component.literal("- Alive bosses: " + raid.getAliveBossCount()), false);
        source.sendSuccess(() -> Component.literal("- Active players: " + raid.getActivePlayerCount()), false);
        return 1;
    }
}
