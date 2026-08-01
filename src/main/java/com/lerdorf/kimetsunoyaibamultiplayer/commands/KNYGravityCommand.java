package com.lerdorf.kimetsunoyaibamultiplayer.commands;

import com.lerdorf.kimetsunoyaibamultiplayer.config.GravityConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.KNYGravity;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class KNYGravityCommand {
    private KNYGravityCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("knygravity")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("get")
                .executes(context -> get(context.getSource())))
            .then(Commands.literal("set")
                .then(Commands.argument("direction", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        for (Direction direction : Direction.values()) {
                            builder.suggest(direction.getName());
                        }
                        return builder.buildFuture();
                    })
                    .executes(context -> set(context.getSource(), StringArgumentType.getString(context, "direction")))))
            .then(Commands.literal("reset")
                .executes(context -> reset(context.getSource())))
            .then(Commands.literal("field_debug")
                .then(Commands.literal("on")
                    .executes(context -> fieldDebug(context.getSource(), true)))
                .then(Commands.literal("off")
                    .executes(context -> fieldDebug(context.getSource(), false)))));
    }

    private static int get(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Gravity current=" + KNYGravity.getGravityDirection(player).getName()
            + " base=" + KNYGravity.getBaseGravityDirection(player).getName()
            + " enabled=" + KNYGravity.isEnabled()), false);
        return 1;
    }

    private static int set(CommandSourceStack source, String directionName) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        Direction direction = Direction.byName(directionName);
        if (direction == null) {
            source.sendFailure(Component.literal("Unknown direction: " + directionName));
            return 0;
        }
        KNYGravity.setBaseGravityDirection(player, direction);
        source.sendSuccess(() -> Component.literal("Set base gravity to " + KNYGravity.getBaseGravityDirection(player).getName()), true);
        return 1;
    }

    private static int reset(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        KNYGravity.resetGravity(player);
        source.sendSuccess(() -> Component.literal("Reset gravity"), true);
        return 1;
    }

    private static int fieldDebug(CommandSourceStack source, boolean enabled) {
        GravityConfig.fieldDebugCommandEnabled = enabled;
        source.sendSuccess(() -> Component.literal("Gravity field debug " + (enabled ? "enabled" : "disabled")), true);
        return 1;
    }
}
