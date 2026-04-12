package com.lerdorf.kimetsunoyaibamultiplayer.quest;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.blooddemonarts.SwampDemonArt;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class QuestGroupRegistry {
    private static final Map<String, QuestGroupDefinition> GROUPS = new LinkedHashMap<>();
    private static final ResourceLocation VILLAGE_SWAMP = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "village_swamp");
    private static final ResourceLocation SWAMP_DEMON_ID = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "swamp_demon");
    private static final ResourceLocation SWAMP_DOMAIN_DIM = ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "swamp_domain");

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
                        // Step 1: Travel to swamp village structure
                        QuestStepDefinition.builder(
                                "enter_village_swamp",
                                "Reach Kidnapper's Bog",
                                "Enter the kimetsunoyaiba:village_swamp structure.",
                                QuestStepType.ENTER_STRUCTURE
                            )
                            .targetId(VILLAGE_SWAMP)
                            .onStart((player, context) -> {
                                player.sendSystemMessage(Component.literal("§7You've received word of disappearances near a swamp village."));
                                player.sendSystemMessage(Component.literal("§7Find the village and investigate."));
                                QuestScenarioActions.storeCurrentStructureCenter(player, context);
                            })
                            .markerResolver((player, context) ->
                                QuestScenarioActions.findNearestStructure(player.serverLevel(), player.blockPosition(), VILLAGE_SWAMP))
                            .build(),

                        // Step 2: Talk to Kazumi
                        QuestStepDefinition.builder(
                                "talk_to_kazumi",
                                "Find Kazumi",
                                "Find and speak with Kazumi inside the village.",
                                QuestStepType.TALK_TO_ENTITY
                            )
                            .targetKey("kazumi")
                            .onStart((player, context) -> {
                                QuestScenarioActions.ensureKazumiSpawned(player, context);
                                player.sendSystemMessage(Component.literal("§7A distraught villager is looking for a Demon Slayer."));
                            })
                            .onComplete((player, context) -> {
                                QuestScenarioActions.sendKazumiDialogue(player);
                                player.sendSystemMessage(Component.literal("§6[Kazumi] §fThis is where she was taken... Please bring her back..."));
                            })
                            .markerResolver((player, context) ->
                                QuestScenarioActions.findNearestQuestEntity(player, "kazumi", 400.0D))
                            .build(),

                        // Step 3: Wait for night
                        QuestStepDefinition.builder(
                                "wait_for_night",
                                "Wait for Nightfall",
                                "Remain in the area until night arrives.",
                                QuestStepType.WAIT_FOR_NIGHT
                            )
                            .onStart((player, context) -> {
                                QuestScenarioActions.markKidnappersBogActive(player, context);
                                player.sendSystemMessage(Component.literal("§7The village grows tense. Wait until nightfall."));
                            })
                            .build(),

                        // Step 4: Encounter the Swamp Demon (CUSTOM step - triggers when player is in swamp domain)
                        QuestStepDefinition.builder(
                                "encounter_swamp_demon",
                                "Confront the Swamp Demon",
                                "Face the demon Numa in the swamp domain.",
                                QuestStepType.CUSTOM
                            )
                            .targetKey("swamp_demon_kidnappers_bog")
                            .onStart((player, context) -> {
                                player.sendSystemMessage(Component.literal("§7A dark presence stirs. The swamp demon reveals itself."));
                                QuestScenarioActions.sendSwampDemonEncounterDialogue(player);
                            })
                            .onTick((player, context) -> {
                                // Check if player is in swamp domain and hasn't had dialogue yet
                                if (player.level().dimension().equals(SwampDemonArt.SWAMP_DOMAIN_LEVEL)) {
                                    QuestScenarioActions.markSwampDomainEncounterStarted(player, context);
                                }
                            })
                            .customCheck((player, context) -> {
                                // Complete when player is in swamp domain and has seen dialogue
                                return player.level().dimension().equals(SwampDemonArt.SWAMP_DOMAIN_LEVEL)
                                    && player.getPersistentData().getBoolean(QuestScenarioActions.SWAMP_DOMAIN_ENCOUNTER_STARTED_TAG);
                            })
                            .markerResolver((player, context) -> {
                                // If in swamp domain, don't show marker; otherwise point to structure
                                if (player.level().dimension().equals(SwampDemonArt.SWAMP_DOMAIN_LEVEL)) {
                                    return player.blockPosition();
                                }
                                return QuestScenarioActions.findNearestStructure(player.serverLevel(), player.blockPosition(), VILLAGE_SWAMP);
                            })
                            .build(),

                        // Step 5: Kill the Swamp Demon wearing Satoko's bow
                        QuestStepDefinition.builder(
                                "kill_swamp_demon",
                                "Defeat Numa, the Swamp Demon",
                                "Kill the special swamp demon wearing Satoko's bow.",
                                QuestStepType.KILL_ENTITY
                            )
                            .targetKey("swamp_demon_kidnappers_bog_satoko")
                            .onStart((player, context) -> {
                                // The demon should already be spawned from the encounter step
                                player.sendSystemMessage(Component.literal("§cThe swamp demon prepares to fight!"));
                            })
                            .markerResolver((player, context) ->
                                QuestScenarioActions.findNearestQuestEntity(player, "swamp_demon_kidnappers_bog_satoko", 256.0D))
                            .build(),

                        // Step 6: Return to Kazumi with Satoko's Bow (CUSTOM step - check if player has bow and talks to Kazumi)
                        QuestStepDefinition.builder(
                                "return_to_kazumi",
                                "Return to Kazumi",
                                "Bring Satoko's bow back to Kazumi.",
                                QuestStepType.CUSTOM
                            )
                            .targetKey("kazumi")
                            .onStart((player, context) -> {
                                player.sendSystemMessage(Component.literal("§7The swamp domain fades. Return to Kazumi with what you found."));
                                // Ensure Kazumi is respawned for return
                                QuestScenarioActions.ensureKazumiSpawned(player, context);
                            })
                            .onComplete((player, context) -> {
                                QuestScenarioActions.giveSatokosBowToKazumi(player, context);
                            })
                            .customCheck((player, context) -> {
                                // Complete when player has bow and is near Kazumi (talk interaction handles completion)
                                return QuestScenarioActions.hasSatokosBow(player, context);
                            })
                            .markerResolver((player, context) -> {
                                // Check if player is back in overworld first, then find Kazumi
                                BlockPos kazumiPos = QuestScenarioActions.findNearestQuestEntity(player, "kazumi", 400.0D);
                                if (kazumiPos != null) {
                                    return kazumiPos;
                                }
                                // Otherwise point to structure center
                                return QuestScenarioActions.getStoredStructureCenter(player);
                            })
                            .build()
                    ),
                    new QuestRewardDefinition()
                        .experiencePoints(50)
                        .item(ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "yen"), 10)
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
