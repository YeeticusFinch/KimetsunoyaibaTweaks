package com.lerdorf.kimetsunoyaibamultiplayer.commands;

import com.lerdorf.kimetsunoyaibamultiplayer.progression.DemonSlayerRank;
import com.lerdorf.kimetsunoyaibamultiplayer.progression.DemonSlayerRankManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.List;

public final class SetDemonSlayerRankCommand {
    private SetDemonSlayerRankCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("setdsrank")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("rank", StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(rankNames(), builder))
                .executes(context -> setRank(
                    context.getSource(),
                    StringArgumentType.getString(context, "rank"),
                    null))
                .then(Commands.argument("target", EntityArgument.players())
                    .executes(context -> setRank(
                        context.getSource(),
                        StringArgumentType.getString(context, "rank"),
                        EntityArgument.getPlayers(context, "target"))))));
    }

    private static int setRank(CommandSourceStack source, String input, Collection<ServerPlayer> selectedPlayers) {
        DemonSlayerRank rank = DemonSlayerRank.parse(input);
        if (rank == null) {
            source.sendFailure(Component.literal("Unknown demon slayer rank. Use one of the suggested ranks."));
            return 0;
        }

        Collection<ServerPlayer> targets = selectedPlayers;
        if (targets == null) {
            try {
                targets = List.of(source.getPlayerOrException());
            } catch (Exception exception) {
                source.sendFailure(Component.literal("This form of the command must be run by a player."));
                return 0;
            }
        }

        for (ServerPlayer target : targets) {
            DemonSlayerRankManager.assignRank(target, rank, true);
        }

        Collection<ServerPlayer> finalTargets = targets;
        source.sendSuccess(() -> Component.literal(formatSuccessMessage(rank, finalTargets, selectedPlayers == null)), true);
        return finalTargets.size();
    }

    private static String formatSuccessMessage(
        DemonSlayerRank rank, Collection<ServerPlayer> targets, boolean self) {
        if (self) {
            return "Set your demon slayer rank to " + rank.displayName()
                + " and granted " + rank.level() + " passive skill point"
                + (rank.level() == 1 ? "." : "s.");
        }
        if (targets.size() == 1) {
            ServerPlayer target = targets.iterator().next();
            return "Set " + target.getName().getString() + "'s demon slayer rank to "
                + rank.displayName() + " and granted " + rank.level() + " passive skill point"
                + (rank.level() == 1 ? "." : "s.");
        }
        return "Set the demon slayer rank to " + rank.displayName() + " for "
            + targets.size() + " players.";
    }

    private static String[] rankNames() {
        return Arrays.stream(DemonSlayerRank.values())
            .map(rank -> rank.name().toLowerCase(Locale.ROOT))
            .toArray(String[]::new);
    }
}
