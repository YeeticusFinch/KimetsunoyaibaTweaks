package com.lerdorf.kimetsunoyaibamultiplayer.commands;

import com.lerdorf.kimetsunoyaibamultiplayer.demonranking.DemonRank;
import com.lerdorf.kimetsunoyaibamultiplayer.demonranking.DemonRankManager;
import com.lerdorf.kimetsunoyaibamultiplayer.demonranking.DemonRankingSavedData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Admin commands for the Demon Ranking (Twelve Kizuki) system.
 *
 * Usage:
 * - /freerank <rank>            makes a rank takeable from its offline holder via the fallback entity
 * - /clearrank <target>         removes a player's rank entirely
 * - /setrank <target> <rank>    debug/admin convenience to assign a rank directly
 */
public class DemonRankCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("freerank")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("rank", StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(rankNames(), builder))
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    DemonRank rank = parseRank(StringArgumentType.getString(context, "rank"));
                    if (rank == null) {
                        source.sendFailure(Component.literal("Unknown rank."));
                        return 0;
                    }
                    DemonRankingSavedData data = DemonRankingSavedData.get(source.getLevel());
                    data.forceFree(rank);
                    source.sendSuccess(() -> Component.literal(
                        rank.displayName() + " can now be taken from its offline holder by killing the fallback entity."), true);
                    return 1;
                })));

        dispatcher.register(Commands.literal("clearrank")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("target", EntityArgument.player())
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    ServerPlayer target = EntityArgument.getPlayer(context, "target");
                    DemonRankManager.clearRank(target);
                    source.sendSuccess(() -> Component.literal(
                        "Cleared " + target.getName().getString() + "'s demon rank."), true);
                    return 1;
                })));

        dispatcher.register(Commands.literal("setrank")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("target", EntityArgument.player())
                .then(Commands.argument("rank", StringArgumentType.word())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(rankNames(), builder))
                    .executes(context -> {
                        CommandSourceStack source = context.getSource();
                        ServerPlayer target = EntityArgument.getPlayer(context, "target");
                        DemonRank rank = parseRank(StringArgumentType.getString(context, "rank"));
                        if (rank == null) {
                            source.sendFailure(Component.literal("Unknown rank."));
                            return 0;
                        }
                        DemonRankingSavedData data = DemonRankingSavedData.get(target.serverLevel());
                        data.assign(rank, target.getUUID());
                        DemonRankManager.applyBuffs(target, rank);
                        target.getPersistentData().putString("kizuki_rank", rank.displayName());
                        source.sendSuccess(() -> Component.literal(
                            "Set " + target.getName().getString() + "'s demon rank to " + rank.displayName() + "."), true);
                        return 1;
                    }))));
    }

    private static List<String> rankNames() {
        List<String> names = new ArrayList<>();
        for (DemonRank rank : DemonRank.values()) {
            names.add(rank.name().toLowerCase(Locale.ROOT));
        }
        return names;
    }

    private static DemonRank parseRank(String input) {
        try {
            return DemonRank.valueOf(input.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
