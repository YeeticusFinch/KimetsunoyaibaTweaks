package com.lerdorf.kimetsunoyaibamultiplayer.commands;

import com.lerdorf.kimetsunoyaibamultiplayer.meditation.MeditationMenuService;
import com.lerdorf.kimetsunoyaibamultiplayer.meditation.PassiveSkillManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class AddSkillPointCommand {
    private AddSkillPointCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("addskillpoint")
            .requires(source -> source.hasPermission(2))
            .executes(context -> add(context.getSource(), getSourcePlayer(context.getSource()), 1))
            .then(Commands.argument("amount", IntegerArgumentType.integer(1, 100))
                .executes(context -> add(
                    context.getSource(),
                    getSourcePlayer(context.getSource()),
                    IntegerArgumentType.getInteger(context, "amount"))))
            .then(Commands.argument("target", EntityArgument.player())
                .executes(context -> add(
                    context.getSource(),
                    EntityArgument.getPlayer(context, "target"),
                    1))
                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 100))
                    .executes(context -> add(
                        context.getSource(),
                        EntityArgument.getPlayer(context, "target"),
                        IntegerArgumentType.getInteger(context, "amount"))))));
    }

    private static ServerPlayer getSourcePlayer(CommandSourceStack source) {
        try {
            return source.getPlayerOrException();
        } catch (Exception exception) {
            return null;
        }
    }

    private static int add(CommandSourceStack source, ServerPlayer target, int amount) {
        if (target == null) {
            source.sendFailure(Component.literal("This form of the command must be run by a player."));
            return 0;
        }
        if (!PassiveSkillManager.addSkillPoints(target, amount)) {
            source.sendFailure(Component.literal(
                target.getName().getString() + " is not a completed demon slayer."));
            return 0;
        }

        MeditationMenuService.openFor(target);
        source.sendSuccess(() -> Component.literal(
            "Added " + amount + " demon slayer skill point" + (amount == 1 ? "" : "s")
                + " to " + target.getName().getString() + "."), true);
        return 1;
    }
}
