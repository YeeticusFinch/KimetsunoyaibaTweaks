package com.lerdorf.kimetsunoyaibamultiplayer.quest;

import com.lerdorf.kimetsunoyaibamultiplayer.util.SunBreathingLevelHelper;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class QuestGroupRegistry {
    private static final Map<String, QuestGroupDefinition> GROUPS = new LinkedHashMap<>();
    private static final ResourceLocation VILLAGE_SWAMP = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "village_swamp");

    static {
        register(new QuestGroupDefinition(
            "cruel",
            "Cruel",
            "The demon slayer main story arc built from linked mission quests.",
            Set.of(PlayerRole.DEMON_SLAYER, PlayerRole.DEMON_SLAYER_IN_TRAINING),
            List.of(
                new QuestStageDefinition(
                    "kidnappers_bog",
                    "Mission No.1 - Kidnapper's Bog",
                    "Investigate the kidnappings in Northwest Town.",
                    List.of(
                        QuestStepDefinition.enterStructure(
                            "enter_village_swamp",
                            "Reach Kidnapper's Bog",
                            "Enter a kimetsunoyaiba:village_swamp structure.",
                            VILLAGE_SWAMP
                        ),
                        QuestStepDefinition.builder(
                                "talk_to_kazumi",
                                "Talk to Kazumi",
                                "Find and speak with Kazumi inside the village.",
                                QuestStepType.TALK_TO_ENTITY
                            )
                            .targetKey("kazumi")
                            .onStart(QuestScenarioActions::ensureKazumiSpawned)
                            .onComplete((player, context) -> QuestScenarioActions.sendKazumiDialogue(player))
                            .markerResolver((player, context) -> QuestScenarioActions.findNearestQuestEntity(player, "kazumi", 400.0D))
                            .build(),
                        QuestStepDefinition.builder(
                                "wait_for_night",
                                "Wait for Nightfall",
                                "Remain in the area until night arrives.",
                                QuestStepType.WAIT_FOR_NIGHT
                            )
                            .onStart((player, context) ->
                                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                    "§7The village grows tense. Wait until nightfall."
                                )))
                            .build(),
                        QuestStepDefinition.builder(
                                "kill_swamp_demon",
                                "Defeat the Swamp Demon",
                                "Kill the special swamp demon attacking the village.",
                                QuestStepType.KILL_ENTITY
                            )
                            .targetKey("swamp_demon_kidnappers_bog")
                            .onTick(QuestScenarioActions::ensureSwampDemonSpawned)
                            .markerResolver((player, context) -> QuestScenarioActions.findNearestQuestEntity(player, "swamp_demon_kidnappers_bog", 256.0D))
                            .build()
                    ),
                    new QuestRewardDefinition()
                        .experiencePoints(150)
                )
            )
        ));

        register(new QuestGroupDefinition(
            "permanence",
            "Permanence",
            "The demon main story arc. Runtime stages are still to be authored.",
            Set.of(PlayerRole.DEMON),
            List.of()
        ));

        register(new QuestGroupDefinition(
            "veil",
            "Veil",
            "The Kakushi main story arc. Runtime stages are still to be authored.",
            Set.of(PlayerRole.KAKUSHI),
            List.of()
        ));

        register(new QuestGroupDefinition(
            "temper",
            "Temper",
            "The swordsmith main story arc. Runtime stages are still to be authored.",
            Set.of(PlayerRole.SWORDSMITH),
            List.of()
        ));
    }

    private QuestGroupRegistry() {
    }

    private static void register(QuestGroupDefinition group) {
        GROUPS.put(group.id(), group);
    }

    public static QuestGroupDefinition get(String id) {
        return GROUPS.get(id);
    }

    public static List<QuestGroupDefinition> getAvailableGroups(PlayerRole role) {
        return GROUPS.values().stream()
            .filter(group -> group.isAvailableFor(role))
            .toList();
    }

    public static QuestGroupDefinition getInitialActiveGroup(PlayerRole role) {
        return switch (role) {
            case DEMON_SLAYER, DEMON_SLAYER_IN_TRAINING -> get("cruel");
            default -> null;
        };
    }
}
