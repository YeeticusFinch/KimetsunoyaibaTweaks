package com.lerdorf.kimetsunoyaibamultiplayer.commands;

import com.lerdorf.kimetsunoyaibamultiplayer.progression.DemonSlayerRank;
import com.lerdorf.kimetsunoyaibamultiplayer.progression.DemonSlayerRankManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.Locale;

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
                    StringArgumentType.getString(context, "rank")))));
    }

    private static int setRank(CommandSourceStack source, String input) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(Component.literal("This command must be run by a player."));
            return 0;
        }

        DemonSlayerRank rank = DemonSlayerRank.parse(input);
        if (rank == null) {
            source.sendFailure(Component.literal("Unknown demon slayer rank. Use one of the suggested ranks."));
            return 0;
        }

        DemonSlayerRankManager.assignRank(player, rank);
        source.sendSuccess(() -> Component.literal(
            "Set your demon slayer rank to " + rank.displayName()
                + " and granted " + rank.level() + " passive skill point"
                + (rank.level() == 1 ? "." : "s.")), true);
        return 1;
    }

    private static String[] rankNames() {
        return Arrays.stream(DemonSlayerRank.values())
            .map(rank -> rank.name().toLowerCase(Locale.ROOT))
            .toArray(String[]::new);
    }
}
