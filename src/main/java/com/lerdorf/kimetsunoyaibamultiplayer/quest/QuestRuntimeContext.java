package com.lerdorf.kimetsunoyaibamultiplayer.quest;

import net.minecraft.server.level.ServerPlayer;

public record QuestRuntimeContext(ServerPlayer player, QuestGroupDefinition group,
                                  QuestStageDefinition stage, QuestStepDefinition step,
                                  int stageIndex, int stepIndex) {
}
