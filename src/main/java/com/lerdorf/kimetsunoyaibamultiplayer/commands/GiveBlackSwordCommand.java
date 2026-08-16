package com.lerdorf.kimetsunoyaibamultiplayer.commands;

import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import com.lerdorf.kimetsunoyaibamultiplayer.items.NichirinSwordBlack;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Gives the executing player a black nichirin sword with a specific breathing style.
 *
 * Usage: /giveblacksword <style_id>
 */
public class GiveBlackSwordCommand {
    private static final SuggestionProvider<CommandSourceStack> STYLE_SUGGESTIONS = (context, builder) ->
        SharedSuggestionProvider.suggest(NichirinSwordBlack.getAvailableStyleIds(), builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("giveblacksword")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("style", StringArgumentType.word())
                .suggests(STYLE_SUGGESTIONS)
                .executes(context -> execute(
                    context.getSource(),
                    StringArgumentType.getString(context, "style")
                ))
            );

        dispatcher.register(command);
    }

    private static int execute(CommandSourceStack source, String styleId) {
        if (!source.isPlayer()) {
            source.sendFailure(Component.literal("This command can only be used by players."));
            return 0;
        }

        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception ignored) {
            source.sendFailure(Component.literal("This command can only be used by players."));
            return 0;
        }

        List<String> availableStyles = NichirinSwordBlack.getAvailableStyleIds();
        if (!availableStyles.contains(styleId)) {
            source.sendFailure(Component.literal("Invalid style '" + styleId + "'."));
            return 0;
        }

        ItemStack sword = new ItemStack(ModItems.NICHIRINSWORD_BLACK.get());
        if (!NichirinSwordBlack.assignStyle(sword, styleId)) {
            source.sendFailure(Component.literal("Failed to assign style '" + styleId + "' to black sword."));
            return 0;
        }

        boolean added = player.getInventory().add(sword);
        if (!added) {
            player.drop(sword, false);
        }

        source.sendSuccess(() -> Component.literal("Gave black nichirin sword with style: " + styleId), false);
        return 1;
    }
}
