package com.lerdorf.kimetsunoyaibamultiplayer.commands;

import com.lerdorf.kimetsunoyaibamultiplayer.meditation.MeditationMenuService;
import com.lerdorf.kimetsunoyaibamultiplayer.quest.PlayerRole;
import com.lerdorf.kimetsunoyaibamultiplayer.quest.QuestProgressionManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class QuestCommand {
    private QuestCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("quest")
            .requires(CommandSourceStack::isPlayer)
            .then(Commands.literal("stages")
                .executes(context -> listStages(context.getSource().getPlayerOrException())))
            .then(Commands.literal("restart")
                .executes(context -> restartCurrentStage(context.getSource().getPlayerOrException())))
            .then(Commands.literal("skip")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("stage", IntegerArgumentType.integer(1))
                    .executes(context -> skipToStage(
                        context.getSource().getPlayerOrException(),
                        IntegerArgumentType.getInteger(context, "stage")))))
        );

        dispatcher.register(Commands.literal("skipquest")
            .requires(source -> source.isPlayer() && source.hasPermission(2))
            .then(Commands.argument("stage", IntegerArgumentType.integer(1))
                .executes(context -> skipToStage(
                    context.getSource().getPlayerOrException(),
                    IntegerArgumentType.getInteger(context, "stage"))))
        );

        dispatcher.register(Commands.literal("restartquest")
            .requires(CommandSourceStack::isPlayer)
            .executes(context -> restartCurrentStage(context.getSource().getPlayerOrException()))
        );
    }

    private static int listStages(ServerPlayer player) {
        PlayerRole role = MeditationMenuService.resolveRoleForProgression(player);
        QuestProgressionManager.QuestStageListing listing =
            QuestProgressionManager.getSelectedQuestStageListing(player, role);

        if (listing.disabled()) {
            player.sendSystemMessage(Component.literal("§cCustom progression is disabled."));
            return 0;
        }
        if (listing.noQuest()) {
            player.sendSystemMessage(Component.literal("§cYou do not have a selected quest with runtime stages."));
            return 0;
        }
        if (listing.stages().isEmpty()) {
            player.sendSystemMessage(Component.literal("§e" + listing.groupName() + " has no quest stages yet."));
            return 0;
        }

        player.sendSystemMessage(Component.literal("§6Quest Stages - §f" + listing.groupName()));
        for (QuestProgressionManager.QuestStageInfo stage : listing.stages()) {
            String marker = stage.current() ? " §a(current)" : "";
            player.sendSystemMessage(Component.literal(
                "§e" + stage.number() + ". §f" + stage.name() + marker
                    + " §7[" + stage.stepCount() + " step" + (stage.stepCount() == 1 ? "" : "s") + "]"));
            if (!stage.summary().isBlank()) {
                player.sendSystemMessage(Component.literal("   §7" + stage.summary()));
            }
        }
        return 1;
    }

    private static int skipToStage(ServerPlayer player, int stageNumber) {
        PlayerRole role = MeditationMenuService.resolveRoleForProgression(player);
        QuestProgressionManager.SkipQuestStageResult result =
            QuestProgressionManager.skipSelectedQuestToStage(player, role, stageNumber);

        if (result.success()) {
            String previous = result.currentStageNumber() > 0
                ? " from stage " + result.currentStageNumber()
                : "";
            player.sendSystemMessage(Component.literal("§aSkipped " + result.groupName() + previous
                + " to stage " + result.targetStageNumber() + ": §f" + result.targetStageName()));
            return 1;
        }
        if (result.disabled()) {
            player.sendSystemMessage(Component.literal("§cCustom progression is disabled."));
        } else if (result.noQuest()) {
            player.sendSystemMessage(Component.literal("§cYou do not have a selected quest with runtime stages."));
        } else if (result.noStages()) {
            player.sendSystemMessage(Component.literal("§e" + result.groupName() + " has no quest stages yet."));
        } else if (result.invalidStage()) {
            player.sendSystemMessage(Component.literal("§cStage " + result.targetStageNumber()
                + " is out of range for " + result.groupName() + ". Use 1-" + result.maxStageNumber() + "."));
        } else if (result.notAhead()) {
            player.sendSystemMessage(Component.literal("§e" + result.groupName() + " is already at stage "
                + result.currentStageNumber() + ". Choose a later stage number."));
        }
        return 0;
    }

    private static int restartCurrentStage(ServerPlayer player) {
        PlayerRole role = MeditationMenuService.resolveRoleForProgression(player);
        QuestProgressionManager.RestartQuestStageResult result =
            QuestProgressionManager.restartCurrentQuestStage(player, role);

        if (result.success()) {
            player.sendSystemMessage(Component.literal("§aRestarted " + result.groupName()
                + " stage " + result.stageNumber() + ": §f" + result.stageName()));
            return 1;
        }
        if (result.disabled()) {
            player.sendSystemMessage(Component.literal("§cCustom progression is disabled."));
        } else if (result.noQuest()) {
            player.sendSystemMessage(Component.literal("§cYou do not have an active quest stage to restart."));
        }
        return 0;
    }
}
