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
    private static final ResourceLocation HOUSE_TAMAYO = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "house_tamayo");
    private static final ResourceLocation SWAMP_DEMON_ID = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "swamp_demon");
    private static final ResourceLocation SWAMP_DOMAIN_DIM = ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "swamp_domain");

    static {
        register(new QuestGroupDefinition(
            "cruel",
            "Cruel",
            "The demon slayer main story arc. These are your missions.",
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
                                "Enter the swamp village structure.",
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

                        // Step 2: Talk to Kazumi (with delayed dialog, standing still, then walking)
                        QuestStepDefinition.builder(
                                "talk_to_kazumi",
                                "Find Kazumi",
                                "Find and speak with Kazumi inside the village.",
                                QuestStepType.CUSTOM
                            )
                            .targetKey("kazumi")
                            .onStart((player, context) -> {
                                QuestScenarioActions.ensureKazumiSpawned(player, context);
                                player.sendSystemMessage(Component.literal("§7A distraught villager is looking for a Demon Slayer."));
                            })
                            .onTick((player, context) -> {
                                // Check if player is near Kazumi and has interacted
                                if (!player.getPersistentData().getBoolean("KnYKazumiDialogStarted")) {
                                    // Wait for player to get near Kazumi to trigger dialog
                                    BlockPos kazumiPos = QuestScenarioActions.findNearestQuestEntity(player, "kazumi", 10.0D);
                                    if (kazumiPos != null && player.blockPosition().distSqr(kazumiPos) < 64.0D) {
                                        player.getPersistentData().putBoolean("KnYKazumiDialogStarted", true);
                                        QuestScenarioActions.makeKazumiLookAtPlayer(player);
                                        QuestScenarioActions.sendDelayedMessages(player, List.of(
                                            Component.literal("§6[Kazumi] §fY-you're a Demon Slayer... right?"),
                                            Component.literal("§6[Kazumi] §fPlease... you have to help me!"),
                                            Component.literal("§6[Kazumi] §fMy fiancée, Satoko... she disappeared last night... right before my eyes!"),
                                            Component.literal("§6[Kazumi] §fI'll take you to where it happened... maybe you can find something I missed.")
                                        ), 60); // 3 seconds between each message
                                    }
                                } else if (player.getPersistentData().getBoolean("KnYKazumiDialogStarted")
                                    && !player.getPersistentData().getBoolean("KnYKazumiWalking")) {
                                    // After dialog finishes (give time for last message), make Kazumi walk
                                    long dialogStartTick = player.getPersistentData().getLong("KnYKazumiDialogStartTick");
                                    long currentTick = player.level().getGameTime();
                                    // 4 messages * 60 ticks delay + initial = ~240 ticks (12 seconds) for all dialog
                                    if (currentTick >= dialogStartTick + 280L) {
                                        player.getPersistentData().putBoolean("KnYKazumiWalking", true);
                                        QuestScenarioActions.makeKazumiWalkToRandomSpot(player, context);
                                    }
                                }
                                
                                // Continuously update Kazumi's pathfinding
                                if (player.getPersistentData().getBoolean("KnYKazumiWalking")) {
                                    QuestScenarioActions.tickKazumiPathing(player, context);
                                }
                            })
                            .customCheck((player, context) -> {
                                // Complete when player is within 10 blocks of Kazumi's target spot
                                return player.getPersistentData().getBoolean("KnYKazumiWalking")
                                    && QuestScenarioActions.isKazumiNearTarget(player, 10.0D);
                            })
                            .onComplete((player, context) -> {
                                QuestScenarioActions.sendDelayedMessages(player, List.of(
                                    Component.literal("§6[Kazumi] §fThis is where she was taken..."),
                                    Component.literal("§6[Kazumi] §fPlease... bring her back...")
                                ), 50);
                                // Clean up tracking flags
                                player.getPersistentData().remove("KnYKazumiDialogStarted");
                                player.getPersistentData().remove("KnYKazumiWalking");
                                player.getPersistentData().remove("KnYKazumiDialogStartTick");
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

                        // Step 4: Encounter the Swamp Demon (spawns in village, dialog, then enters swamp domain to find bow)
                        QuestStepDefinition.builder(
                                "encounter_swamp_demon",
                                "Confront the Swamp Demon",
                                "Face the demon Numa in the swamp village and enter the swamp domain.",
                                QuestStepType.CUSTOM
                            )
                            .targetKey("swamp_demon_kidnappers_bog")
                            .onStart((player, context) -> {
                                player.sendSystemMessage(Component.literal("§7A dark presence stirs. The swamp demon reveals itself."));
                                // Spawn encounter dialogue with delays
                                QuestScenarioActions.sendDelayedMessages(player, List.of(
                                    Component.literal("§c[Numa] §fHeh... another one has come to be devoured..."),
                                    Component.literal("§c[Numa] §fYou Demon Slayers are always so predictable...")
                                ), 60);
                                // Spawn the swamp demon near the player
                                QuestScenarioActions.spawnSwampDemonEncounter(player, context);
                            })
                            .onTick((player, context) -> {
                                // Check if player has entered the swamp domain (the swamp demon will send them there)
                                if (player.level().dimension().equals(SwampDemonArt.SWAMP_DOMAIN_LEVEL)) {
                                    player.getPersistentData().putBoolean("KnYEnteredSwampDomain", true);
                                }
                            })
                            .customCheck((player, context) -> {
                                // Complete when player enters the swamp domain
                                return player.getPersistentData().getBoolean("KnYEnteredSwampDomain");
                            })
                            .onComplete((player, context) -> {
                                // Dialog when entering the swamp domain
                                QuestScenarioActions.sendSwampDomainDialogue(player);
                            })
                            .markerResolver((player, context) ->
                                QuestScenarioActions.findNearestQuestEntity(player, "swamp_demon_kidnappers_bog", 256.0D))
                            .build(),

                        // Step 5: Kill the Swamp Demon wearing Satoko's bow in the swamp domain
                        QuestStepDefinition.builder(
                                "kill_swamp_demon",
                                "Retrieve Satoko's Bow",
                                "Kill the swamp demon clone wearing Satoko's bow in the swamp domain.",
                                QuestStepType.KILL_ENTITY
                            )
                            .targetKey("swamp_demon_kidnappers_bog_satoko")
                            .onStart((player, context) -> {
                                player.sendSystemMessage(Component.literal("§cOne of the clones wears Satoko's bow! Kill it!"));
                            })
                            .markerResolver((player, context) ->
                                QuestScenarioActions.findNearestQuestEntity(player, "swamp_demon_kidnappers_bog_satoko", 256.0D))
                            .build(),

                        // Step 6: Return to Kazumi with Satoko's Bow
                        QuestStepDefinition.builder(
                                "return_to_kazumi",
                                "Return to Kazumi",
                                "Bring Satoko's bow back to Kazumi.",
                                QuestStepType.CUSTOM
                            )
                            .targetKey("kazumi")
                            .onStart((player, context) -> {
                                player.sendSystemMessage(Component.literal("§7The swamp domain fades. Return to Kazumi with what you found."));
                                QuestScenarioActions.removeKidnappersBogSwampDemonResistanceNear(player, 256.0D);
                                // Ensure Kazumi is respawned for return
                                QuestScenarioActions.ensureKazumiSpawned(player, context);
                            })
                            .onTick((player, context) -> {
                                // Check if player is near Kazumi
                                BlockPos kazumiPos = QuestScenarioActions.findNearestQuestEntity(player, "kazumi", 10.0D);
                                if (kazumiPos != null && player.blockPosition().distSqr(kazumiPos) < 100.0D) {
                                    if (!player.getPersistentData().getBoolean("KnYKazumiReturnDialogStarted")) {
                                        player.getPersistentData().putBoolean("KnYKazumiReturnDialogStarted", true);
                                        QuestScenarioActions.sendDelayedMessages(player, List.of(
                                            Component.literal("§6[Kazumi] §fYou're back...! Did you find her?!")
                                        ), 40);
                                    }
                                }
                            })
                            .customCheck((player, context) -> {
                                // Complete when player has bow, dialog started, and is near Kazumi
                                if (!QuestScenarioActions.hasSatokosBow(player, context)) return false;
                                if (!player.getPersistentData().getBoolean("KnYKazumiReturnDialogStarted")) return false;
                                BlockPos kazumiPos = QuestScenarioActions.findNearestQuestEntity(player, "kazumi", 10.0D);
                                return kazumiPos != null && player.blockPosition().distSqr(kazumiPos) < 100.0D;
                            })
                            .onComplete((player, context) -> {
                                QuestScenarioActions.giveSatokosBowToKazumi(player, context);
                                player.getPersistentData().remove("KnYKazumiReturnDialogStarted");
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
                ),
                new QuestStageDefinition(
                    "asakusa",
                    "Mission No.2 - Asakusa",
                    "Visit Tamayo's House and defend Tamayo and Yushiro from Muzan's assassins.",
                    List.of(
                        QuestStepDefinition.builder(
                                "enter_tamayo_house",
                                "Travel to Tamayo's House",
                                "Find the demon doctor Tamayo at her hidden house.",
                                QuestStepType.ENTER_STRUCTURE
                            )
                            .targetId(HOUSE_TAMAYO)
                            .onStart((player, context) -> {
                                player.sendSystemMessage(Component.literal("§6[Kasugai Crow] §fYour Kasugai Crow has received new orders."));
                                player.sendSystemMessage(Component.literal("§6[Kasugai Crow] §fA demon doctor named Tamayo wishes to speak with Demon Slayers."));
                            })
                            .onComplete(QuestScenarioActions::storeTamayoHouseContext)
                            .markerResolver((player, context) ->
                                QuestScenarioActions.findNearestStructure(player.serverLevel(), player.blockPosition(), HOUSE_TAMAYO))
                            .build(),

                        QuestStepDefinition.builder(
                                "meet_tamayo_and_yushiro",
                                "Speak with Tamayo",
                                "Wait for nightfall, then meet Tamayo and Yushiro inside Tamayo's House.",
                                QuestStepType.CUSTOM
                            )
                            .requiredTimeOfDay(false)
                            .onStart((player, context) -> {
                                QuestScenarioActions.storeTamayoHouseContext(player, context);
                                QuestScenarioActions.repairStoredTamayoHouse(player);
                                QuestScenarioActions.ensureTamayoAndYushiroAtReception(player, context);
                            })
                            .onTick(QuestScenarioActions::ensureTamayoAndYushiroAtReception)
                            .customCheck(QuestScenarioActions::isTamayoReceptionReady)
                            .onComplete(QuestScenarioActions::sendTamayoReceptionDialogue)
                            .markerResolver((player, context) ->
                                QuestScenarioActions.getTamayoHousePoint(player, QuestScenarioActions.TamayoHousePoint.RECEPTION))
                            .build(),

                        QuestStepDefinition.builder(
                                "visit_tamayo_basement",
                                "Visit Tamayo's Basement",
                                "Follow Tamayo and Yushiro to the basement laboratory.",
                                QuestStepType.CUSTOM
                            )
                            .onStart((player, context) -> {
                                QuestScenarioActions.storeTamayoHouseContext(player, context);
                                QuestScenarioActions.tickTamayoBasementBriefing(player, context);
                            })
                            .onTick(QuestScenarioActions::tickTamayoBasementBriefing)
                            .customCheck(QuestScenarioActions::isTamayoBasementReady)
                            .onComplete(QuestScenarioActions::sendTamayoBasementDialogue)
                            .markerResolver((player, context) ->
                                QuestScenarioActions.getTamayoHousePoint(player, QuestScenarioActions.TamayoHousePoint.BASEMENT))
                            .build(),

                        QuestStepDefinition.builder(
                                "return_to_tamayo_house_entrance",
                                "Return Upstairs",
                                "Return with Tamayo and Yushiro before the intruders arrive.",
                                QuestStepType.CUSTOM
                            )
                            .requiredTimeOfDay(false)
                            .onStart((player, context) -> {
                                QuestScenarioActions.sendTamayoAmbushPrelude(player, context);
                                QuestScenarioActions.tickTamayoReturnToReception(player, context);
                            })
                            .onTick(QuestScenarioActions::tickTamayoReturnToReception)
                            .customCheck(QuestScenarioActions::isTamayoReturnReady)
                            .markerResolver((player, context) ->
                                QuestScenarioActions.getTamayoHousePoint(player, QuestScenarioActions.TamayoHousePoint.RECEPTION))
                            .build(),

                        QuestStepDefinition.builder(
                                "defeat_susamaru_and_yahaba",
                                "Defend Tamayo and Yushiro",
                                "Defeat Susamaru and Yahaba before Tamayo is slain.",
                                QuestStepType.CUSTOM
                            )
                            .requiredTimeOfDay(false)
                            .onStart(QuestScenarioActions::startSusamaruYahabaAttack)
                            .onTick(QuestScenarioActions::tickSusamaruYahabaAttack)
                            .customCheck(QuestScenarioActions::areSusamaruAndYahabaDefeated)
                            .onComplete(QuestScenarioActions::sendTamayoHouseVictoryDialogue)
                            .markerResolver(QuestScenarioActions::findNearestTamayoHouseEnemy)
                            .build()
                    ),
                    new QuestRewardDefinition()
                        .experiencePoints(75)
                        .item(ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "yen"), 20)
                )
            )
        ));

        register(new QuestGroupDefinition(
            "permanence",
            "Permanence",
            "The demon main story arc.",
            Set.of(PlayerRole.DEMON),
            List.of(
                new QuestStageDefinition(
                    "first_taste_of_blood",
                    "Stage No.1 - First Taste of Blood",
                    "Kill and devour humans to take your first steps as a demon.",
                    List.of(
                        QuestStepDefinition.builder(
                                "eat_human_flesh",
                                "First Taste of Blood",
                                "Kill 10 humans and eat 10 human flesh items in any order.",
                                QuestStepType.CUSTOM
                            )
                            .onStart((player, context) -> player.sendSystemMessage(
                                Component.literal("§cStage No.1 - First Taste of Blood: §fKill 10 humans and eat 10 human flesh items.")))
                            .customCheck((player, context) ->
                                QuestProgressionManager.getPermanenceFirstTasteKillProgress(player) >= 10
                                    && QuestProgressionManager.getPermanenceFirstTasteEatenProgress(player) >= 10)
                            .onComplete((player, context) -> {
                                QuestProgressionManager.markPermanenceFirstTasteCompleted(player);
                                player.sendSystemMessage(Component.literal("§6Stage Complete: §fFirst Taste of Blood"));
                            })
                            .markerResolver((player, context) ->
                                QuestScenarioActions.findNearestVanillaVillage(player.serverLevel(), player.blockPosition()))
                            .build()
                    ),
                    new QuestRewardDefinition()
                        .experiencePoints(25)
                ),
                new QuestStageDefinition(
                    "hunger_unending",
                    "Stage No.2 - Hunger Unending",
                    "Find hunting grounds at night and continue feeding without drawing the Corps' attention.",
                    List.of(
                        QuestStepDefinition.builder(
                                "travel_to_village_at_night",
                                "Travel to a Village at Night",
                                "Find nearby hunting grounds after night falls.",
                                QuestStepType.CUSTOM
                            )
                            .requiredTimeOfDay(false)
                            .onStart((player, context) -> {
                                player.sendSystemMessage(Component.literal("§cStage No.2 - Hunger Unending: §fFind a village under cover of night."));
                                player.sendSystemMessage(Component.literal("§7Stay unseen and feed before demon slayers are drawn to your trail."));
                            })
                            .customCheck((player, context) -> QuestScenarioActions.isNearVanillaVillage(player, 64.0D))
                            .markerResolver((player, context) ->
                                QuestScenarioActions.findNearestVanillaVillage(player.serverLevel(), player.blockPosition()))
                            .build(),
                        QuestStepDefinition.builder(
                                "feed_without_detection",
                                "Feed Without Detection",
                                "Kill and eat 10 villagers in their sleep without being detected.",
                                QuestStepType.CUSTOM
                            )
                            .onStart((player, context) -> player.sendSystemMessage(
                                Component.literal("§7The village sleeps. Kill 10 sleeping humans and eat 10 human flesh items.")))
                            .customCheck((player, context) ->
                                QuestProgressionManager.getPermanenceHungerUnendingSleepKillProgress(player) >= 10
                                    && QuestProgressionManager.getPermanenceHungerUnendingEatenProgress(player) >= 10)
                            .onComplete((player, context) ->
                                player.sendSystemMessage(Component.literal("§6Stage Complete: §fHunger Unending")))
                            .markerResolver((player, context) ->
                                QuestScenarioActions.findNearestVanillaVillage(player.serverLevel(), player.blockPosition()))
                            .build()
                    ),
                    new QuestRewardDefinition()
                        .experiencePoints(50)
                ),
                new QuestStageDefinition(
                    "slayers_blood",
                    "Stage No.3 - Slayer's Blood",
                    "Learn what makes demon slayers dangerous, then break one.",
                    List.of(
                        QuestStepDefinition.builder(
                                "travel_to_lower_moon_den",
                                "Find a Lower Moon's Den",
                                "Travel to a deep dungeon where a lower moon demon is hiding.",
                                QuestStepType.CUSTOM
                            )
                            .onStart((player, context) -> {
                                QuestScenarioActions.ensureOrochiCompanion(player);
                                player.sendSystemMessage(Component.literal("§6[Orochi] §fThe strength you have shown has drawn the attention of a powerful demon."));
                                player.sendSystemMessage(Component.literal("§cStage No.3 - Slayer's Blood: §fRumors speak of a powerful demon dwelling deep beneath the earth..."));
                            })
                            .onTick((player, context) -> {
                                if (QuestScenarioActions.isInSlayersBloodDungeonCave(player, context)) {
                                    QuestScenarioActions.ensureKamanueSpawned(player, context);
                                }
                            })
                            .customCheck(QuestScenarioActions::isInSlayersBloodDungeonCave)
                            .onComplete((player, context) -> {
                                QuestScenarioActions.ensureKamanueSpawned(player, context);
                                player.sendSystemMessage(Component.literal("§7A presence waits in the dark. Find Kamanue."));
                            })
                            .markerResolver((player, context) -> QuestScenarioActions.getOrStoreSlayersBloodDungeon(player))
                            .build(),
                        QuestStepDefinition.builder(
                                "talk_to_kamanue",
                                "Find Kamanue",
                                "Find Kamanue in the dungeon caves and speak with him.",
                                QuestStepType.CUSTOM
                            )
                            .targetKey("kamanue")
                            .onStart((player, context) -> {
                                QuestScenarioActions.ensureKamanueSpawned(player, context);
                                player.sendSystemMessage(Component.literal("§7Find Kamanue and speak with him."));
                            })
                            .onTick((player, context) -> {
                                QuestScenarioActions.ensureKamanueSpawned(player, context);
                                QuestScenarioActions.tickKamanueNeutrality(player, context);
                            })
                            .customCheck(QuestScenarioActions::isKamanueDialogueComplete)
                            .onComplete(QuestScenarioActions::completeKamanueDialogue)
                            .markerResolver((player, context) -> {
                                QuestScenarioActions.ensureKamanueSpawned(player, context);
                                return QuestScenarioActions.findKamanuePosition(player);
                            })
                            .build(),
                        QuestStepDefinition.builder(
                                "terrorize_village",
                                "Terrorize a Village",
                                "Attack villagers until demon slayers arrive.",
                                QuestStepType.CUSTOM
                            )
                            .targetKey("slayers_blood_slayer")
                            .onStart((player, context) -> player.sendSystemMessage(
                                Component.literal("§7The village is gripped by fear...")))
                            .onTick(QuestProgressionManager::tickPermanenceSlayersBloodVillageAmbush)
                            .customCheck(QuestProgressionManager::isPermanenceSlayersBloodCaptiveReady)
                            .onComplete((player, context) -> player.sendSystemMessage(
                                Component.literal("§aQuest Updated: §fReturn the Demon Slayer's remains to Kamanue.")))
                            .markerResolver((player, context) -> QuestProgressionManager.resolveSlayersBloodVillageMarker(player))
                            .build(),
                        QuestStepDefinition.builder(
                                "bring_captive_to_kamanue",
                                "Return the Remains to Kamanue",
                                "Bring the Demon Slayer's flesh back to Kamanue.",
                                QuestStepType.CUSTOM
                            )
                            .onStart((player, context) -> QuestScenarioActions.ensureKamanueSpawned(player, context))
                            .onTick((player, context) -> {
                                QuestScenarioActions.ensureKamanueSpawned(player, context);
                                QuestProgressionManager.tickPermanenceSlayersBloodVillageAmbush(player, context);
                                QuestProgressionManager.tickPermanenceSlayersBloodCaptiveReturn(player, context);
                            })
                            .customCheck(QuestProgressionManager::isPermanenceSlayersBloodCaptiveDelivered)
                            .onComplete((player, context) -> player.sendSystemMessage(
                                Component.literal("§aQuest Updated: §fSpeak with Kamanue.")))
                            .markerResolver((player, context) -> {
                                QuestScenarioActions.ensureKamanueSpawned(player, context);
                                return QuestScenarioActions.findKamanuePosition(player);
                            })
                            .build(),
                        QuestStepDefinition.builder(
                                "study_slayer_breathing",
                                "Speak with Kamanue",
                                "Give Kamanue the Demon Slayer's flesh.",
                                QuestStepType.CUSTOM
                            )
                            .targetKey("kamanue")
                            .onStart((player, context) -> {
                                QuestScenarioActions.ensureKamanueSpawned(player, context);
                                player.sendSystemMessage(Component.literal("§7Speak with Kamanue while carrying the Demon Slayer's flesh."));
                            })
                            .onTick((player, context) -> {
                                QuestScenarioActions.ensureKamanueSpawned(player, context);
                                QuestScenarioActions.tickKamanueNeutrality(player, context);
                            })
                            .customCheck(QuestProgressionManager::isPermanenceSlayersBloodStudyComplete)
                            .onComplete((player, context) -> player.sendSystemMessage(
                                Component.literal("§aQuest Updated: §fDefeat the Demon Slayer.")))
                            .markerResolver((player, context) -> QuestScenarioActions.findKamanuePosition(player))
                            .build(),
                        QuestStepDefinition.builder(
                                "kill_studied_slayer",
                                "Defeat the Demonized Demon Slayer",
                                "Kill the resurrected Demon Slayer.",
                                QuestStepType.CUSTOM
                            )
                            .onTick(QuestProgressionManager::tickPermanenceSlayersBloodFinalFight)
                            .customCheck(QuestProgressionManager::isPermanenceSlayersBloodFinalSlayerKilled)
                            .onComplete(QuestScenarioActions::despawnKamanueWithMugenDoor)
                            .markerResolver((player, context) ->
                                QuestProgressionManager.findPermanenceSlayersBloodFinalSlayer(player))
                            .build()
                    ),
                    new QuestRewardDefinition()
                        .experiencePoints(200)
                        .item(ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "blood_of_muzan"), 3)
                        .item(ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "cushion_red"), 1)
                )
            )
        ));

        register(new QuestGroupDefinition(
            "veil",
            "Veil",
            "The Kakushi main story arc.",
            Set.of(PlayerRole.KAKUSHI),
            List.of()
        ));

        register(new QuestGroupDefinition(
            "temper",
            "Temper",
            "The swordsmith main story arc.",
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
            case DEMON -> get("permanence");
            default -> null;
        };
    }
}
