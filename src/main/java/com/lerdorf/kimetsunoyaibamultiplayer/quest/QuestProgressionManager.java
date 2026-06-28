package com.lerdorf.kimetsunoyaibamultiplayer.quest;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.config.CustomProgressionConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.events.DemonTransformationHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.DemonSlayerEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities;
import com.lerdorf.kimetsunoyaibamultiplayer.meditation.MeditationMenuService;
import com.lerdorf.kimetsunoyaibamultiplayer.meditation.MeditationMenuData;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SetCrowQuestMarkerPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.raids.StructureLocationCache;
import com.lerdorf.kimetsunoyaibamultiplayer.util.SunBreathingLevelHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.util.EntityTagHelper;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class QuestProgressionManager {
    private static final String ACTIVE_GROUP_ID = "KnYQuestActiveGroup";
    private static final String ACTIVE_STAGE_INDEX = "KnYQuestActiveStageIndex";
    private static final String ACTIVE_STEP_INDEX = "KnYQuestActiveStepIndex";
    private static final String ACTIVE_STEP_STARTED = "KnYQuestActiveStepStarted";
    private static final String STEP_TIME_BLOCKED_NOTICE_TICK = "KnYQuestStepTimeBlockedNoticeTick";
    private static final long STEP_TIME_BLOCKED_NOTICE_INTERVAL_TICKS = 20L * 60L * 5L;
    private static final String STEP_RESTART_COOLDOWN_UNTIL = "KnYQuestStepRestartCooldownUntil";
    private static final String PERMANENCE_FIRST_TASTE_EATEN = "KnYPermanenceFirstTasteProgress";
    private static final String PERMANENCE_FIRST_TASTE_KILLS = "KnYPermanenceFirstTasteKills";
    private static final String PERMANENCE_FIRST_TASTE_COMPLETED = "KnYPermanenceFirstTasteCompleted";
    private static final String PERMANENCE_HUNGER_UNENDING_EATEN = "KnYPermanenceHungerUnendingEaten";
    private static final String PERMANENCE_HUNGER_UNENDING_SLEEP_KILLS = "KnYPermanenceHungerUnendingSleepKills";
    private static final String SLAYERS_BLOOD_AMBUSH_SPAWNED = "KnYPermanenceSlayersBloodAmbushSpawned";
    private static final String SLAYERS_BLOOD_VILLAGE_ENTER_TICK = "KnYPermanenceSlayersBloodVillageEnterTick";
    private static final String SLAYERS_BLOOD_VILLAGE_LAST_SPAWN_TICK = "KnYPermanenceSlayersBloodVillageLastSpawnTick";
    private static final String SLAYERS_BLOOD_VILLAGE_X = "KnYPermanenceSlayersBloodVillageX";
    private static final String SLAYERS_BLOOD_VILLAGE_Y = "KnYPermanenceSlayersBloodVillageY";
    private static final String SLAYERS_BLOOD_VILLAGE_Z = "KnYPermanenceSlayersBloodVillageZ";
    private static final String SLAYERS_BLOOD_CAPTIVE_READY = "KnYPermanenceSlayersBloodCaptiveReady";
    private static final String SLAYERS_BLOOD_CAPTIVE_DELIVERED = "KnYPermanenceSlayersBloodCaptiveDelivered";
    private static final String SLAYERS_BLOOD_STUDY_STARTED = "KnYPermanenceSlayersBloodStudyStarted";
    private static final String SLAYERS_BLOOD_STUDY_START_TICK = "KnYPermanenceSlayersBloodStudyStartTick";
    private static final String SLAYERS_BLOOD_STUDY_COMPLETE = "KnYPermanenceSlayersBloodStudyComplete";
    private static final String SLAYERS_BLOOD_FINAL_KILLED = "KnYPermanenceSlayersBloodFinalKilled";
    private static final String SLAYERS_BLOOD_CAPTIVE_UUID = "KnYPermanenceSlayersBloodCaptiveUuid";
    private static final String SLAYERS_BLOOD_FINAL_UUID = "KnYPermanenceSlayersBloodFinalUuid";
    private static final String SLAYERS_BLOOD_SLAYER_TAG = "slayers_blood_slayer";
    private static final String SLAYERS_BLOOD_CAPTIVE_TAG = "slayers_blood_captive";
    private static final String SLAYERS_BLOOD_FINAL_TAG = "slayers_blood_final_slayer";
    private static final float SLAYERS_BLOOD_CAPTIVE_DISARM_HEALTH_RATIO = 0.40F;
    private static final long SLAYERS_BLOOD_VILLAGE_REINFORCEMENT_INTERVAL = 20L * 60L;
    private static final double SLAYERS_BLOOD_VILLAGE_DETECTION_RADIUS = 96.0D;
    private static final double SLAYERS_BLOOD_INITIAL_SPAWN_RADIUS = 20.0D;
    private static final double SLAYERS_BLOOD_REINFORCEMENT_SPAWN_RADIUS = 40.0D;
    private static final int PERMANENCE_FIRST_TASTE_REQUIRED = 10;
    private static final int PERMANENCE_HUNGER_UNENDING_REQUIRED = 10;
    private static final TagKey<EntityType<?>> WOMAN =
        TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("forge", "woman"));
    private static final ResourceLocation HOUSE_TAMAYO = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "house_tamayo");
    private static final ResourceLocation SUSAMARU_ID = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "susamaru");
    private static final ResourceLocation YAHABA_ID = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "yahaba");
    private static final String SWORDSMITH_VILLAGE_LOCATION_ID = "swordsmith_village";
    private static final String SWORDSMITH_NAV_MESSAGE_TICK = "KnYSwordsmithVillageNavMessageTick";
    private static final String SWORDSMITH_NAV_STRUCTURE_X = "KnYSwordsmithVillageNavStructureX";
    private static final String SWORDSMITH_NAV_STRUCTURE_Z = "KnYSwordsmithVillageNavStructureZ";
    private static final String SWORDSMITH_NAV_KAKUSHI_TAG = "KnYSwordsmithVillageGuideKakushi";
    private static final int SWORDSMITH_NAV_MESSAGE_COOLDOWN_TICKS = 20 * 30;
    private static final double SWORDSMITH_KAKUSHI_SEARCH_RADIUS = 48.0D;
    private static final ResourceLocation KAKUSHI_ID = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "kakushi");
    private static final List<ResourceLocation> DEMON_HUNT_STRUCTURES = List.of(
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "village_swamp"),
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "house_tanjiro"),
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "house_ubuyashiki")
    );
    private static final List<ResourceLocation> SWORDSMITH_ACCESS_STRUCTURES = List.of(
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "house_kocho"),
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "house_rengoku"),
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "house_ubuyashiki"),
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "graveyard")
    );

    private static final ResourceLocation MT_YOKO_BIOME = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba",
            "mt_yoko");
    private static final ResourceLocation MT_NATAGUMO_BIOME = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba",
            "mt_natagumo");

    private static final ResourceLocation COMPLETED_FINAL_SELECTION = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID, "completed_final_selectioni");

    private QuestProgressionManager() {
    }

    public static void tick(ServerPlayer player, PlayerRole role) {
        if (!CustomProgressionConfig.isCustomProgressionEnabled()) {
            clearRuntimeState(player);
            return;
        }

        // Process any queued delayed messages
        QuestScenarioActions.processDelayedMessages(player);
        tickSwordsmithVillageNavigation(player);

        QuestRuntimeContext context = getOrInitializeContext(player, role);
        if (context == null) {
            return;
        }

        QuestStepDefinition step = context.step();
        if (!player.getPersistentData().getBoolean(ACTIVE_STEP_STARTED)) {
            long restartCooldownUntil = player.getPersistentData().getLong(STEP_RESTART_COOLDOWN_UNTIL);
            if (restartCooldownUntil > 0L) {
                long now = player.level().getGameTime();
                if (now < restartCooldownUntil) {
                    return;
                }
                player.getPersistentData().remove(STEP_RESTART_COOLDOWN_UNTIL);
            }
            if (!isStepStartTimeSatisfied(player, step)) {
                maybeSendStepTimeBlockedMessage(player, step);
                return;
            }
            step.onStart().accept(player, context);
            player.getPersistentData().putBoolean(ACTIVE_STEP_STARTED, true);
        }

        step.onTick().accept(player, context);
        if (isPermanenceSlayersBlood(context)) {
            QuestScenarioActions.tickKamanueNeutrality(player, context);
        }
        if (isStepComplete(player, context)) {
            completeStep(player, context);
        }
    }

    public static boolean handleTalkToEntity(ServerPlayer player, Entity target, PlayerRole role) {
        if (!CustomProgressionConfig.isCustomProgressionEnabled()) {
            return false;
        }
        QuestRuntimeContext context = getOrInitializeContext(player, role);
        if (context == null) {
            return false;
        }

        if (isPermanenceSlayersBloodTalkToKamanue(context) && matchesTarget(context.step(), target)) {
            QuestScenarioActions.startKamanueDialogue(player);
            return true;
        }

        if (isPermanenceSlayersBloodStudy(context) && matchesTarget(context.step(), target)) {
            startPermanenceSlayersBloodStudy(player, context);
            return true;
        }

        // Handle TALK_TO_ENTITY steps
        if (context.step().type() == QuestStepType.TALK_TO_ENTITY && matchesTarget(context.step(), target)) {
            completeStep(player, context);
            return true;
        }

        // Handle CUSTOM steps that are waiting for a talk interaction (like return_to_kazumi)
        if (context.step().type() == QuestStepType.CUSTOM && !context.step().targetKey().isBlank() && matchesTarget(context.step(), target)) {
            // Check if the custom condition is met (e.g., has Satoko's Bow)
            if (isStepComplete(player, context)) {
                completeStep(player, context);
                return true;
            }
        }

        return false;
    }

    public static boolean handleKazumiBowTurnIn(ServerPlayer player, Entity target, PlayerRole role, boolean swampDemonAliveNearby) {
        if (!CustomProgressionConfig.isCustomProgressionEnabled()) {
            return false;
        }
        QuestRuntimeContext context = getOrInitializeContext(player, role);
        if (context == null) {
            return false;
        }

        String questNpcId = target.getPersistentData().getString(QuestScenarioActions.QUEST_NPC_ID_TAG);
        if (!"kazumi".equals(questNpcId) || !"return_to_kazumi".equals(context.step().id())) {
            return false;
        }

        if (swampDemonAliveNearby) {
            player.sendSystemMessage(Component.literal("§6[Kazumi] §fThat swamp demon is still alive... please kill it first!"));
            return true;
        }

        if (!QuestScenarioActions.hasSatokosBow(player, context)) {
            return true;
        }

        completeStep(player, context);
        return true;
    }

    public static void handleKill(ServerPlayer player, LivingEntity victim, PlayerRole role) {
        if (!CustomProgressionConfig.isCustomProgressionEnabled()) {
            return;
        }
        QuestRuntimeContext context = getOrInitializeContext(player, role);
        if (context == null) {
            return;
        }

        if (isPermanenceFirstTaste(context)) {
            handlePermanenceFirstTasteKill(player, victim, context);
            return;
        }

        if (isPermanenceSlayersBloodTerrorize(context) && isQuestHuman(victim)) {
            return;
        }

        if ("cruel".equals(context.group().id())
            && "asakusa".equals(context.stage().id())
            && "defeat_susamaru_and_yahaba".equals(context.step().id())) {
            QuestScenarioActions.markTamayoHouseTargetKilled(player, victim);
            if (context.step().customCheck().test(player, context)) {
                completeStep(player, context);
            }
            return;
        }

        if (context.step().type() != QuestStepType.KILL_ENTITY) {
            return;
        }

        if (matchesTarget(context.step(), victim)) {
            completeStep(player, context);
        }
    }

    public static boolean isRaidSuppressedForPlayer(ServerPlayer player, ResourceLocation structureId) {
        if (!CustomProgressionConfig.isCustomProgressionEnabled() || player == null || structureId == null) {
            return false;
        }

        QuestRuntimeContext context = getOrInitializeContext(player, MeditationMenuService.resolveRoleForProgression(player));
        if (context == null) {
            return false;
        }

        if (!"cruel".equals(context.group().id())) {
            return false;
        }

        if ("kidnappers_bog".equals(context.stage().id())) {
            return true;
        }

        return "asakusa".equals(context.stage().id()) && HOUSE_TAMAYO.equals(structureId);
    }

    public static boolean shouldSuppressOmenForQuestKill(ServerPlayer player, LivingEntity victim) {
        if (!CustomProgressionConfig.isCustomProgressionEnabled() || player == null || victim == null) {
            return false;
        }

        QuestRuntimeContext context = getOrInitializeContext(player, MeditationMenuService.resolveRoleForProgression(player));
        if (context == null || !"cruel".equals(context.group().id())) {
            return false;
        }

        String questTargetId = victim.getPersistentData().getString(QuestScenarioActions.QUEST_TARGET_ID_TAG);
        ResourceLocation victimId = EntityType.getKey(victim.getType());

        if ("kidnappers_bog".equals(context.stage().id())) {
            return "swamp_demon_kidnappers_bog".equals(questTargetId)
                || "swamp_demon_kidnappers_bog_satoko".equals(questTargetId);
        }

        if ("asakusa".equals(context.stage().id())) {
            return SUSAMARU_ID.equals(victimId) || YAHABA_ID.equals(victimId);
        }

        return false;
    }

    public static void handlePlayerDeath(ServerPlayer player, PlayerRole role) {
        if (!CustomProgressionConfig.isCustomProgressionEnabled()) {
            return;
        }
        QuestRuntimeContext context = getOrInitializeContext(player, role);
        if (context == null) {
            return;
        }
        if ("cruel".equals(context.group().id())
            && "asakusa".equals(context.stage().id())
            && "defeat_susamaru_and_yahaba".equals(context.step().id())) {
            QuestScenarioActions.resetTamayoHouseFailure(player);
            player.getPersistentData().putInt(ACTIVE_STEP_INDEX, context.stepIndex());
            player.getPersistentData().putBoolean(ACTIVE_STEP_STARTED, false);
            player.sendSystemMessage(Component.literal("§cQuest Failed: §fYou were slain while defending Tamayo. Return to Tamayo's House and try again."));
        }
    }

    public static void scheduleCurrentStepRestart(ServerPlayer player, int cooldownTicks) {
        if (player == null) {
            return;
        }
        player.getPersistentData().putBoolean(ACTIVE_STEP_STARTED, false);
        long now = player.level().getGameTime();
        player.getPersistentData().putLong(STEP_RESTART_COOLDOWN_UNTIL, now + Math.max(0, cooldownTicks));
    }

    public static void handleHumanFleshConsumed(ServerPlayer player, ItemStack stack, PlayerRole role) {
        if (role != PlayerRole.DEMON || stack.isEmpty()) {
            return;
        }

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null || !isHumanFleshQuestItem(itemId)) {
            return;
        }
        DemonTransformationHandler.addTrackedHumansConsumed(player, 1);

        if (!CustomProgressionConfig.isCustomProgressionEnabled()) {
            return;
        }

        QuestRuntimeContext context = getOrInitializeContext(player, role);
        if (context == null) {
            return;
        }

        if (isPermanenceFirstTaste(context)) {
            int progress = Math.min(PERMANENCE_FIRST_TASTE_REQUIRED,
                player.getPersistentData().getInt(PERMANENCE_FIRST_TASTE_EATEN) + 1);
            player.getPersistentData().putInt(PERMANENCE_FIRST_TASTE_EATEN, progress);
            sendPermanenceFirstTasteProgress(player, "Human flesh eaten");
        } else if (isPermanenceHungerUnendingFeed(context)) {
            int progress = Math.min(PERMANENCE_HUNGER_UNENDING_REQUIRED,
                player.getPersistentData().getInt(PERMANENCE_HUNGER_UNENDING_EATEN) + 1);
            player.getPersistentData().putInt(PERMANENCE_HUNGER_UNENDING_EATEN, progress);
            sendPermanenceHungerUnendingProgress(player, "Humans eaten");
        } else {
            return;
        }

        if (context.step().customCheck().test(player, context)) {
            completeStep(player, context);
        }
    }

    public static void handleSleepingHumanKilled(ServerPlayer player, LivingEntity victim) {
        if (!CustomProgressionConfig.isCustomProgressionEnabled() || player == null || victim == null) {
            return;
        }
        PlayerRole role = MeditationMenuService.resolveRoleForProgression(player);
        if (role != PlayerRole.DEMON || !isQuestHuman(victim)) {
            return;
        }

        QuestRuntimeContext context = getOrInitializeContext(player, role);
        if (context == null || !isPermanenceHungerUnendingFeed(context)) {
            return;
        }

        int progress = Math.min(PERMANENCE_HUNGER_UNENDING_REQUIRED,
            player.getPersistentData().getInt(PERMANENCE_HUNGER_UNENDING_SLEEP_KILLS) + 1);
        player.getPersistentData().putInt(PERMANENCE_HUNGER_UNENDING_SLEEP_KILLS, progress);
        sendPermanenceHungerUnendingProgress(player, "Sleeping humans killed");
        if (context.step().customCheck().test(player, context)) {
            completeStep(player, context);
        }
    }

    public static int getPermanenceFirstTasteEatenProgress(ServerPlayer player) {
        return Math.min(PERMANENCE_FIRST_TASTE_REQUIRED,
            player.getPersistentData().getInt(PERMANENCE_FIRST_TASTE_EATEN));
    }

    public static int getPermanenceFirstTasteKillProgress(ServerPlayer player) {
        return Math.min(PERMANENCE_FIRST_TASTE_REQUIRED,
            player.getPersistentData().getInt(PERMANENCE_FIRST_TASTE_KILLS));
    }

    public static int getPermanenceHungerUnendingEatenProgress(ServerPlayer player) {
        return Math.min(PERMANENCE_HUNGER_UNENDING_REQUIRED,
            player.getPersistentData().getInt(PERMANENCE_HUNGER_UNENDING_EATEN));
    }

    public static int getPermanenceHungerUnendingSleepKillProgress(ServerPlayer player) {
        return Math.min(PERMANENCE_HUNGER_UNENDING_REQUIRED,
            player.getPersistentData().getInt(PERMANENCE_HUNGER_UNENDING_SLEEP_KILLS));
    }

    public static boolean isPermanenceFirstTasteCompleted(ServerPlayer player) {
        return player.getPersistentData().getBoolean(PERMANENCE_FIRST_TASTE_COMPLETED);
    }

    public static void handleKamanueHurt(ServerPlayer player, LivingEntity target, float projectedHealth) {
        if (!CustomProgressionConfig.isCustomProgressionEnabled() || player == null || target == null) {
            return;
        }
        PlayerRole role = MeditationMenuService.resolveRoleForProgression(player);
        if (role != PlayerRole.DEMON || !QuestScenarioActions.isQuestKamanue(target)) {
            return;
        }
        QuestRuntimeContext context = getOrInitializeContext(player, role);
        if (!isPermanenceSlayersBlood(context) || QuestScenarioActions.isQuestKamanueHostile(target)) {
            return;
        }
        if (projectedHealth > target.getMaxHealth() * 0.75F) {
            return;
        }

        QuestScenarioActions.resetSlayersBloodDialogueState(player);
        QuestScenarioActions.makeKamanueHostile(player, target);
        player.getPersistentData().putInt(ACTIVE_STEP_INDEX, 0);
        player.getPersistentData().putBoolean(ACTIVE_STEP_STARTED, false);
        player.sendSystemMessage(Component.literal("§cQuest Failed: §fYou provoked Kamanue. Find another opening and try again."));
    }

    public static void handleSlayersBloodSlayerHurt(ServerPlayer player, LivingEntity target, float projectedHealth,
                                                    LivingHurtEvent event) {
        if (!CustomProgressionConfig.isCustomProgressionEnabled() || player == null || !(target instanceof DemonSlayerEntity slayer)) {
            return;
        }
        PlayerRole role = MeditationMenuService.resolveRoleForProgression(player);
        if (role != PlayerRole.DEMON) {
            return;
        }
        QuestRuntimeContext context = getOrInitializeContext(player, role);
        if (!isPermanenceSlayersBloodTerrorize(context)) {
            return;
        }
        String targetKey = slayer.getPersistentData().getString(QuestScenarioActions.QUEST_TARGET_ID_TAG);
        if (!SLAYERS_BLOOD_SLAYER_TAG.equals(targetKey) || slayer.getPowerLevel() != 0) {
            return;
        }

        float breakThreshold = slayer.getMaxHealth() * SLAYERS_BLOOD_CAPTIVE_DISARM_HEALTH_RATIO;
        if (projectedHealth > breakThreshold) {
            return;
        }

        float disarmedHealth = Math.max(1.0F, slayer.getMaxHealth() * SLAYERS_BLOOD_CAPTIVE_DISARM_HEALTH_RATIO);
        if (event != null) {
            event.setAmount(Math.max(0.0F, target.getHealth() - disarmedHealth));
        }
        slayer.setHealth(disarmedHealth);
        slayer.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        slayer.getPersistentData().putString(QuestScenarioActions.QUEST_TARGET_ID_TAG, SLAYERS_BLOOD_CAPTIVE_TAG);
        slayer.getPersistentData().putBoolean(SLAYERS_BLOOD_CAPTIVE_TAG, true);
        slayer.setTarget(null);
        slayer.setLastHurtByMob(null);
        slayer.setNoAi(false);
        slayer.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 30, 2, false, true, true));
        slayer.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 30, 3, false, true, true));

        for (ServerPlayer involved : getPlayersSharingSlayersBlood(player, 256.0D)) {
            involved.getPersistentData().putBoolean(SLAYERS_BLOOD_CAPTIVE_READY, true);
            involved.getPersistentData().putUUID(SLAYERS_BLOOD_CAPTIVE_UUID, slayer.getUUID());
            involved.sendSystemMessage(Component.literal("§7The Demon Slayer's sword breaks. Bring the captive back to Kamanue."));
        }
    }

    public static void markPermanenceFirstTasteCompleted(ServerPlayer player) {
        player.getPersistentData().putBoolean(PERMANENCE_FIRST_TASTE_COMPLETED, true);
    }

    public static void copyQuestProgressOnClone(Player original, Player clone) {
        copyQuestProgressOnClone(original, clone, false);
    }

    public static void copyQuestProgressOnClone(Player original, Player clone, boolean wasDeath) {
        if (original == null || clone == null) {
            return;
        }

        CompoundTag originalData = original.getPersistentData();
        CompoundTag cloneData = clone.getPersistentData();

        List<String> cloneKeys = new ArrayList<>(cloneData.getAllKeys());
        for (String key : cloneKeys) {
            if (isQuestProgressKey(key)) {
                cloneData.remove(key);
            }
        }

        for (String key : originalData.getAllKeys()) {
            if (!isQuestProgressKey(key)) {
                continue;
            }
            Tag value = originalData.get(key);
            if (value != null) {
                cloneData.put(key, value.copy());
            }
        }

        if (wasDeath) {
            resetPermanenceSlayersBloodOnDeath(clone);
        }
    }

    public static void resetPermanenceSlayersBloodOnDeath(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        CompoundTag data = serverPlayer.getPersistentData();
        QuestGroupDefinition group = QuestGroupRegistry.get(data.getString(ACTIVE_GROUP_ID));
        if (group == null || !"permanence".equals(group.id()) || group.stages().isEmpty()) {
            return;
        }

        int stageIndex = Math.min(Math.max(0, data.getInt(ACTIVE_STAGE_INDEX)), group.stages().size() - 1);
        QuestStageDefinition stage = group.stages().get(stageIndex);
        if (!"slayers_blood".equals(stage.id())) {
            return;
        }

        data.putInt(ACTIVE_STEP_INDEX, 0);
        data.putBoolean(ACTIVE_STEP_STARTED, false);
        data.remove(STEP_RESTART_COOLDOWN_UNTIL);
        clearSlayersBloodVillageAmbushState(serverPlayer);
        data.remove(SLAYERS_BLOOD_AMBUSH_SPAWNED);
        data.remove(SLAYERS_BLOOD_CAPTIVE_READY);
        data.remove(SLAYERS_BLOOD_CAPTIVE_DELIVERED);
        data.remove(SLAYERS_BLOOD_STUDY_STARTED);
        data.remove(SLAYERS_BLOOD_STUDY_START_TICK);
        data.remove(SLAYERS_BLOOD_STUDY_COMPLETE);
        data.remove(SLAYERS_BLOOD_FINAL_KILLED);
        data.remove(SLAYERS_BLOOD_CAPTIVE_UUID);
        data.remove(SLAYERS_BLOOD_FINAL_UUID);
        QuestScenarioActions.resetSlayersBloodKamanueState(serverPlayer);
    }

    public static List<ServerPlayer> getPlayersSharingSlayersBlood(ServerPlayer source, double radius) {
        List<ServerPlayer> players = new ArrayList<>();
        if (source == null || !(source.level() instanceof ServerLevel serverLevel)) {
            return players;
        }
        QuestRuntimeContext sourceContext = getOrInitializeContext(source, MeditationMenuService.resolveRoleForProgression(source));
        if (!isPermanenceSlayersBlood(sourceContext)) {
            return players;
        }
        BlockPos sourceDungeon = QuestScenarioActions.getOrStoreSlayersBloodDungeon(source);
        double radiusSqr = radius * radius;
        for (ServerPlayer candidate : serverLevel.players()) {
            if (candidate.distanceToSqr(source) > radiusSqr) {
                continue;
            }
            QuestRuntimeContext candidateContext = getOrInitializeContext(candidate, MeditationMenuService.resolveRoleForProgression(candidate));
            if (!isPermanenceSlayersBlood(candidateContext)) {
                continue;
            }
            BlockPos candidateDungeon = QuestScenarioActions.getOrStoreSlayersBloodDungeon(candidate);
            if (sourceDungeon != null && candidateDungeon != null
                && sourceDungeon.distSqr(candidateDungeon) <= 128.0D * 128.0D) {
                players.add(candidate);
            }
        }
        return players;
    }

    public static void tickPermanenceSlayersBloodVillageAmbush(ServerPlayer player, QuestRuntimeContext context) {
        if (!isPermanenceSlayersBloodVillageAmbushActive(context)) {
            return;
        }
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos village = QuestScenarioActions.findNearestVanillaVillage(serverLevel, player.blockPosition());
        if (village == null) {
            clearSlayersBloodVillageAmbushState(player);
            return;
        }

        if (player.blockPosition().distSqr(village) > SLAYERS_BLOOD_VILLAGE_DETECTION_RADIUS * SLAYERS_BLOOD_VILLAGE_DETECTION_RADIUS) {
            clearSlayersBloodVillageAmbushState(player);
            return;
        }

        updateSlayersBloodVillageState(player, village);
        if (hasSlayersBloodDemonSlayerInVillage(serverLevel, village)) {
            return;
        }

        long now = player.level().getGameTime();
        long enteredAt = player.getPersistentData().getLong(SLAYERS_BLOOD_VILLAGE_ENTER_TICK);
        if (enteredAt <= 0L) {
            player.getPersistentData().putLong(SLAYERS_BLOOD_VILLAGE_ENTER_TICK, now);
            enteredAt = now;
        }
        if (now - enteredAt < 100L) {
            return;
        }

        long lastSpawn = player.getPersistentData().getLong(SLAYERS_BLOOD_VILLAGE_LAST_SPAWN_TICK);
        if (lastSpawn <= 0L) {
            spawnSlayersBloodVillageReinforcements(player, 3, SLAYERS_BLOOD_INITIAL_SPAWN_RADIUS);
            player.getPersistentData().putLong(SLAYERS_BLOOD_VILLAGE_LAST_SPAWN_TICK, now);
            player.sendSystemMessage(Component.literal("§7Your actions have attracted attention."));
            return;
        }

        if (now - lastSpawn >= SLAYERS_BLOOD_VILLAGE_REINFORCEMENT_INTERVAL) {
            spawnSlayersBloodVillageReinforcement(player, SLAYERS_BLOOD_REINFORCEMENT_SPAWN_RADIUS);
            player.getPersistentData().putLong(SLAYERS_BLOOD_VILLAGE_LAST_SPAWN_TICK, now);
        }
    }

    public static boolean isPermanenceSlayersBloodCaptiveReady(ServerPlayer player, QuestRuntimeContext context) {
        return player.getPersistentData().getBoolean(SLAYERS_BLOOD_CAPTIVE_READY);
    }

    public static void tickPermanenceSlayersBloodCaptiveReturn(ServerPlayer player, QuestRuntimeContext context) {
        if (!isPermanenceSlayersBloodCaptiveReturn(context)) {
            return;
        }
        if (isPermanenceSlayersBloodCaptiveDelivered(player, context)) {
            markSlayersBloodFlagForGroup(player, SLAYERS_BLOOD_CAPTIVE_DELIVERED);
        }
    }

    public static boolean isPermanenceSlayersBloodCaptiveDelivered(ServerPlayer player, QuestRuntimeContext context) {
        BlockPos kamanue = QuestScenarioActions.findKamanuePosition(player);
        BlockPos captive = findPermanenceSlayersBloodCaptive(player);
        return kamanue != null && captive != null && captive.distSqr(kamanue) <= 12.0D * 12.0D
            || player.getPersistentData().getBoolean(SLAYERS_BLOOD_CAPTIVE_DELIVERED);
    }

    public static boolean isPermanenceSlayersBloodStudyComplete(ServerPlayer player, QuestRuntimeContext context) {
        if (player.getPersistentData().getBoolean(SLAYERS_BLOOD_STUDY_COMPLETE)) {
            return true;
        }
        if (!player.getPersistentData().getBoolean(SLAYERS_BLOOD_STUDY_STARTED)) {
            return false;
        }
        long startedAt = player.getPersistentData().getLong(SLAYERS_BLOOD_STUDY_START_TICK);
        if (startedAt > 0L && player.level().getGameTime() >= startedAt + 30L * 7L + 10L) {
            player.getPersistentData().putBoolean(SLAYERS_BLOOD_STUDY_COMPLETE, true);
            return true;
        }
        return false;
    }

    public static void tickPermanenceSlayersBloodFinalFight(ServerPlayer player, QuestRuntimeContext context) {
        if (!isPermanenceSlayersBloodFinalFight(context)) {
            return;
        }
        BlockPos finalSlayer = findPermanenceSlayersBloodFinalSlayer(player);
        if (finalSlayer != null && player.level() instanceof ServerLevel serverLevel) {
            Entity entity = findFinalSlayerEntity(player);
            if (entity instanceof DemonSlayerEntity slayer) {
                slayer.setTarget(player);
            }
        }
    }

    public static boolean isPermanenceSlayersBloodFinalSlayerKilled(ServerPlayer player, QuestRuntimeContext context) {
        return player.getPersistentData().getBoolean(SLAYERS_BLOOD_FINAL_KILLED);
    }

    public static BlockPos findPermanenceSlayersBloodFinalSlayer(ServerPlayer player) {
        return findNearestSlayersBloodSlayer(player, SLAYERS_BLOOD_FINAL_TAG, -1, 320.0D);
    }

    private static void startPermanenceSlayersBloodStudy(ServerPlayer player, QuestRuntimeContext context) {
        if (!isPermanenceSlayersBloodStudy(context)
            || !player.getPersistentData().getBoolean(SLAYERS_BLOOD_CAPTIVE_DELIVERED)
            || player.getPersistentData().getBoolean(SLAYERS_BLOOD_STUDY_STARTED)) {
            return;
        }

        DemonSlayerEntity captive = findCaptiveSlayerEntity(player);
        if (captive == null) {
            player.sendSystemMessage(Component.literal("§7Kamanue needs the captive closer."));
            return;
        }

        List<Component> messages = List.of(
            Component.literal("§c[Kamanue] §fYou actually did it..."),
            Component.literal("§c[Kamanue] §fGood. Very good. Bring them closer."),
            Component.literal("§c[Kamanue] §fWe will study their breathing technique."),
            Component.literal("§c[Kamanue] §fTheir breathing changes everything."),
            Component.literal("§c[Kamanue] §fMore oxygen. Faster muscles. Stronger blood flow."),
            Component.literal("§c[Kamanue] §fA fragile human body pushed beyond its limits."),
            Component.literal("§c[Kamanue] §fThat is the secret of the Demon Slayers.")
        );
        for (ServerPlayer involved : getPlayersSharingSlayersBlood(player, 256.0D)) {
            involved.getPersistentData().putBoolean(SLAYERS_BLOOD_STUDY_STARTED, true);
            involved.getPersistentData().putLong(SLAYERS_BLOOD_STUDY_START_TICK, involved.level().getGameTime());
            involved.getPersistentData().putUUID(SLAYERS_BLOOD_FINAL_UUID, captive.getUUID());
            QuestScenarioActions.sendDelayedMessages(involved, messages, 30);
        }

        captive.getPersistentData().putString(QuestScenarioActions.QUEST_TARGET_ID_TAG, SLAYERS_BLOOD_FINAL_TAG);
        captive.getPersistentData().putBoolean(SLAYERS_BLOOD_CAPTIVE_TAG, false);
        captive.setNoAi(false);
        captive.configurePowerLevelLoadout(Math.max(0, Math.min(1, captive.getPowerLevel())));
        captive.setHealth(captive.getMaxHealth());
        captive.setTarget(player);
    }

    private static void spawnSlayersBloodVillageReinforcements(ServerPlayer player, int count, double radius) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (int i = 0; i < count; i++) {
            spawnSingleVillageSlayer(player, serverLevel, radius);
        }
    }

    private static void spawnSlayersBloodVillageReinforcement(ServerPlayer player, double radius) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        spawnSingleVillageSlayer(player, serverLevel, radius);
    }

    private static void spawnSlayersBloodAmbushForGroup(ServerPlayer player, QuestRuntimeContext context) {
        for (ServerPlayer involved : getPlayersSharingSlayersBlood(player, 256.0D)) {
            involved.sendSystemMessage(Component.literal("§7Your actions have attracted attention."));
        }
        spawnSlayersBloodVillageReinforcements(player, 3, SLAYERS_BLOOD_INITIAL_SPAWN_RADIUS);
    }

    private static void spawnSingleVillageSlayer(ServerPlayer player, ServerLevel serverLevel, double radius) {
        DemonSlayerEntity slayer = (player.getRandom().nextBoolean()
            ? ModEntities.DEMON_SLAYER
            : ModEntities.DEMON_SLAYER_FEMALE).get().create(serverLevel);
        if (slayer == null) {
            return;
        }

        BlockPos spawnPos = findSafeVillageSpawn(serverLevel, player.blockPosition(), Math.max(8, (int) Math.ceil(radius)));
        slayer.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, 0.0F, 0.0F);
        slayer.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(spawnPos), MobSpawnType.MOB_SUMMONED, null, null);
        slayer.configurePowerLevelLoadout(0);
        slayer.getPersistentData().putString(QuestScenarioActions.QUEST_TARGET_ID_TAG, SLAYERS_BLOOD_SLAYER_TAG);
        slayer.setTarget(player);
        slayer.setPersistenceRequired();
        serverLevel.addFreshEntity(slayer);
    }

    private static void clearSlayersBloodVillageAmbushState(ServerPlayer player) {
        player.getPersistentData().remove(SLAYERS_BLOOD_VILLAGE_ENTER_TICK);
        player.getPersistentData().remove(SLAYERS_BLOOD_VILLAGE_LAST_SPAWN_TICK);
        player.getPersistentData().remove(SLAYERS_BLOOD_VILLAGE_X);
        player.getPersistentData().remove(SLAYERS_BLOOD_VILLAGE_Y);
        player.getPersistentData().remove(SLAYERS_BLOOD_VILLAGE_Z);
    }

    private static void updateSlayersBloodVillageState(ServerPlayer player, BlockPos village) {
        CompoundTag data = player.getPersistentData();
        boolean changed = data.getInt(SLAYERS_BLOOD_VILLAGE_X) != village.getX()
            || data.getInt(SLAYERS_BLOOD_VILLAGE_Y) != village.getY()
            || data.getInt(SLAYERS_BLOOD_VILLAGE_Z) != village.getZ();
        if (changed) {
            data.putInt(SLAYERS_BLOOD_VILLAGE_X, village.getX());
            data.putInt(SLAYERS_BLOOD_VILLAGE_Y, village.getY());
            data.putInt(SLAYERS_BLOOD_VILLAGE_Z, village.getZ());
            data.putLong(SLAYERS_BLOOD_VILLAGE_ENTER_TICK, player.level().getGameTime());
            data.remove(SLAYERS_BLOOD_VILLAGE_LAST_SPAWN_TICK);
        }
        if (!data.contains(SLAYERS_BLOOD_VILLAGE_ENTER_TICK)) {
            data.putLong(SLAYERS_BLOOD_VILLAGE_ENTER_TICK, player.level().getGameTime());
        }
    }

    private static boolean hasSlayersBloodDemonSlayerInVillage(ServerLevel level, BlockPos village) {
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(village).inflate(SLAYERS_BLOOD_VILLAGE_DETECTION_RADIUS),
                entity -> entity.isAlive()
                    && !entity.isRemoved()
                    && EntityTagHelper.isDemonSlayer(entity)
                    && entity instanceof DemonSlayerEntity slayer
                    && slayer.getPowerLevel() == 0)
            .stream()
            .findAny()
            .isPresent();
    }

    private static BlockPos findSafeVillageSpawn(ServerLevel level, BlockPos center, int radius) {
        for (int attempt = 0; attempt < 64; attempt++) {
            int dx = level.random.nextInt(radius * 2 + 1) - radius;
            int dz = level.random.nextInt(radius * 2 + 1) - radius;
            int x = center.getX() + dx;
            int z = center.getZ() + dz;
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) {
                return pos;
            }
        }
        return center;
    }

    private static void markSlayersBloodFlagForGroup(ServerPlayer player, String flag) {
        for (ServerPlayer involved : getPlayersSharingSlayersBlood(player, 256.0D)) {
            involved.getPersistentData().putBoolean(flag, true);
        }
    }

    private static BlockPos findPermanenceSlayersBloodCaptive(ServerPlayer player) {
        DemonSlayerEntity captive = findCaptiveSlayerEntity(player);
        return captive == null ? null : captive.blockPosition();
    }

    private static DemonSlayerEntity findCaptiveSlayerEntity(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        if (player.getPersistentData().contains(SLAYERS_BLOOD_CAPTIVE_UUID)) {
            UUID uuid = player.getPersistentData().getUUID(SLAYERS_BLOOD_CAPTIVE_UUID);
            Entity entity = serverLevel.getEntity(uuid);
            if (entity instanceof DemonSlayerEntity slayer && slayer.isAlive()) {
                return slayer;
            }
        }
        return serverLevel.getEntitiesOfClass(DemonSlayerEntity.class,
                new AABB(player.blockPosition()).inflate(256.0D),
                slayer -> slayer.isAlive()
                    && SLAYERS_BLOOD_CAPTIVE_TAG.equals(slayer.getPersistentData().getString(QuestScenarioActions.QUEST_TARGET_ID_TAG)))
            .stream()
            .min(java.util.Comparator.comparingDouble(player::distanceToSqr))
            .orElse(null);
    }

    public static BlockPos findNearestSlayersBloodSlayer(ServerPlayer player, String targetKey, int powerLevel, double radius) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        return serverLevel.getEntitiesOfClass(DemonSlayerEntity.class,
                new AABB(player.blockPosition()).inflate(radius),
                slayer -> slayer.isAlive()
                    && targetKey.equals(slayer.getPersistentData().getString(QuestScenarioActions.QUEST_TARGET_ID_TAG))
                    && (powerLevel < 0 || slayer.getPowerLevel() == powerLevel))
            .stream()
            .min(java.util.Comparator.comparingDouble(player::distanceToSqr))
            .map(Entity::blockPosition)
            .orElse(null);
    }

    public static boolean isPermanenceSlayersBloodVillageAmbushReady(ServerPlayer player, QuestRuntimeContext context) {
        if (!isPermanenceSlayersBloodTerrorize(context)) {
            return false;
        }
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        BlockPos village = QuestScenarioActions.findNearestVanillaVillage(serverLevel, player.blockPosition());
        return village != null
            && player.blockPosition().distSqr(village) <= SLAYERS_BLOOD_VILLAGE_DETECTION_RADIUS * SLAYERS_BLOOD_VILLAGE_DETECTION_RADIUS
            && hasSlayersBloodDemonSlayerInVillage(serverLevel, village);
    }

    public static BlockPos resolveSlayersBloodVillageMarker(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return null;
        }

        BlockPos village = QuestScenarioActions.findNearestVanillaVillage(serverLevel, player.blockPosition());
        if (village == null) {
            BlockPos slayer = findNearestSlayersBloodSlayer(player, SLAYERS_BLOOD_SLAYER_TAG, 0, 100.0D);
            return slayer;
        }

        boolean nearVillage = player.blockPosition().distSqr(village)
            <= SLAYERS_BLOOD_VILLAGE_DETECTION_RADIUS * SLAYERS_BLOOD_VILLAGE_DETECTION_RADIUS;
        if (!nearVillage) {
            return village;
        }

        BlockPos slayer = findNearestSlayersBloodSlayer(player, SLAYERS_BLOOD_SLAYER_TAG, 0, 100.0D);
        if (slayer != null) {
            return slayer;
        }

        spawnSlayersBloodVillageReinforcement(player, SLAYERS_BLOOD_INITIAL_SPAWN_RADIUS);
        slayer = findNearestSlayersBloodSlayer(player, SLAYERS_BLOOD_SLAYER_TAG, 0, 100.0D);
        return slayer != null ? slayer : village;
    }

    public static boolean handleSlayersBloodCaptiveDeath(DemonSlayerEntity captive) {
        if (!CustomProgressionConfig.isCustomProgressionEnabled() || captive == null) {
            return false;
        }
        if (!(captive.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        if (!SLAYERS_BLOOD_CAPTIVE_TAG.equals(captive.getPersistentData().getString(QuestScenarioActions.QUEST_TARGET_ID_TAG))) {
            return false;
        }

        boolean handled = false;
        UUID captiveUuid = captive.getUUID();
        for (ServerPlayer player : serverLevel.players()) {
            if (!player.getPersistentData().contains(SLAYERS_BLOOD_CAPTIVE_UUID)) {
                continue;
            }
            if (!player.getPersistentData().getUUID(SLAYERS_BLOOD_CAPTIVE_UUID).equals(captiveUuid)) {
                continue;
            }

            PlayerRole role = MeditationMenuService.resolveRoleForProgression(player);
            QuestRuntimeContext context = getOrInitializeContext(player, role);
            if (context == null || !isPermanenceSlayersBloodCaptiveReturn(context)) {
                continue;
            }

            resetPermanenceSlayersBloodOnDeath(player);
            player.sendSystemMessage(Component.literal("§cQuest Failed: §fThe captive Demon Slayer died. Return to terrorizing a village."));
            handled = true;
        }
        return handled;
    }

    public static boolean handleSlayersBloodFinalSlayerDeath(DemonSlayerEntity finalSlayer) {
        if (!CustomProgressionConfig.isCustomProgressionEnabled() || finalSlayer == null) {
            return false;
        }
        if (!(finalSlayer.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        if (!SLAYERS_BLOOD_FINAL_TAG.equals(finalSlayer.getPersistentData().getString(QuestScenarioActions.QUEST_TARGET_ID_TAG))) {
            return false;
        }

        List<Component> messages = List.of(
            Component.literal("§c[Kamanue] §fYou've learned something important today."),
            Component.literal("§c[Kamanue] §fTo defeat your enemy, you must first understand them.")
        );

        boolean handled = false;
        UUID finalUuid = finalSlayer.getUUID();
        for (ServerPlayer player : serverLevel.players()) {
            if (!player.getPersistentData().contains(SLAYERS_BLOOD_FINAL_UUID)) {
                continue;
            }
            if (!player.getPersistentData().getUUID(SLAYERS_BLOOD_FINAL_UUID).equals(finalUuid)) {
                continue;
            }

            PlayerRole role = MeditationMenuService.resolveRoleForProgression(player);
            QuestRuntimeContext context = getOrInitializeContext(player, role);
            if (context == null || !isPermanenceSlayersBloodFinalFight(context)) {
                continue;
            }

            if (!player.getPersistentData().getBoolean(SLAYERS_BLOOD_FINAL_KILLED)) {
                player.getPersistentData().putBoolean(SLAYERS_BLOOD_FINAL_KILLED, true);
                QuestScenarioActions.sendDelayedMessages(player, messages, 30);
            }

            if (context.step().customCheck().test(player, context)) {
                completeStep(player, context);
            }
            handled = true;
        }
        return handled;
    }

    private static Entity findFinalSlayerEntity(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        if (player.getPersistentData().contains(SLAYERS_BLOOD_FINAL_UUID)) {
            Entity entity = serverLevel.getEntity(player.getPersistentData().getUUID(SLAYERS_BLOOD_FINAL_UUID));
            if (entity instanceof DemonSlayerEntity slayer && slayer.isAlive()) {
                return slayer;
            }
        }
        return serverLevel.getEntitiesOfClass(DemonSlayerEntity.class,
                new AABB(player.blockPosition()).inflate(320.0D),
                slayer -> slayer.isAlive()
                    && SLAYERS_BLOOD_FINAL_TAG.equals(slayer.getPersistentData().getString(QuestScenarioActions.QUEST_TARGET_ID_TAG)))
            .stream()
            .min(java.util.Comparator.comparingDouble(player::distanceToSqr))
            .orElse(null);
    }

    private static boolean isQuestProgressKey(String key) {
        return key.startsWith("KnYQuest")
            || key.startsWith("KnYKazumi")
            || key.startsWith("KnYKidnappersBog")
            || key.startsWith("KnYSwamp")
            || key.startsWith("KnYTamayo")
            || key.startsWith("KnYSusamaru")
            || key.startsWith("KnYYahaba")
            || key.startsWith("KnYYushiro")
            || key.startsWith("KnYDelayed")
            || key.equals("KnYEnteredSwampDomain")
            || key.equals("KnYDoctorsRequestUnlocked")
            || key.startsWith("KnYPermanence")
            || key.equals(PERMANENCE_FIRST_TASTE_EATEN)
            || key.equals(PERMANENCE_FIRST_TASTE_KILLS)
            || key.equals(PERMANENCE_FIRST_TASTE_COMPLETED)
            || key.equals(PERMANENCE_HUNGER_UNENDING_EATEN)
            || key.equals(PERMANENCE_HUNGER_UNENDING_SLEEP_KILLS);
    }

    public static boolean handleCrowInteract(ServerPlayer player, PlayerRole role) {
        return handleQuestEntityInteract(player, role, "Crow");
    }

    public static boolean handleQuestEntityInteract(ServerPlayer player, PlayerRole role, String speakerName) {
        if (!CustomProgressionConfig.isCustomProgressionEnabled()) {
            return false;
        }
        String speaker = speakerName == null || speakerName.isBlank() ? "Crow" : speakerName;

        // Check if Kazumi is walking and show Satoko's disappearance coordinates
        String kazumiCoords = QuestScenarioActions.getKazumiTargetCoordinates(player);
        if (kazumiCoords != null && QuestScenarioActions.isKazumiWalking(player)) {
            player.sendSystemMessage(Component.literal("§6[" + speaker + "] §fSatoko disappeared at " + kazumiCoords));
            return true;
        }

        // First check meditation menu selection for waypoint
        MarkerResult meditationMarker = resolveMeditationMenuMarker(player, role);
        if (meditationMarker != null) {
            ModNetworking.sendToPlayer(new SetCrowQuestMarkerPacket(
                meditationMarker.targetLocation(),
                20 * 60
            ), player);
            player.sendSystemMessage(Component.literal("§6[" + speaker + "] §f" + meditationMarker.name() + " is at "
                + formatCrowQuestCoordinates(meditationMarker.position(), meditationMarker.targetLocation())));
            if (meditationMarker.extraCrowMessage() != null && !meditationMarker.extraCrowMessage().isBlank()) {
                player.sendSystemMessage(Component.literal("§6[" + speaker + "] §f" + meditationMarker.extraCrowMessage()));
            }
            return true;
        }

        // Fall back to current quest step marker
        QuestRuntimeContext context = getOrInitializeContext(player, role);
        if (context == null) {
            player.sendSystemMessage(Component.literal("§7No active custom-progression quest objective is available."));
            return true;
        }

        if (isPermanenceFirstTaste(context)) {
            return handlePermanenceFirstTasteCrowInteract(player);
        }

        Vec3 targetLocation = resolveQuestMarkerTargetLocation(player, context);
        if (targetLocation == null) {
            // Check if we're in the encounter or kill steps - point to swamp demon directly
            if ("encounter_swamp_demon".equals(context.step().id()) || "kill_swamp_demon".equals(context.step().id())) {
                BlockPos targetPos = resolveSwampDemonPosition(player);
                if (targetPos != null) {
                    targetLocation = Vec3.atBottomCenterOf(targetPos);
                }
            }
        }
        
        if (targetLocation == null) {
            player.sendSystemMessage(Component.literal("§7" + speaker + " cannot find a marker for this objective yet."));
            return true;
        }

        ModNetworking.sendToPlayer(new SetCrowQuestMarkerPacket(
            targetLocation,
            20 * 60
        ), player);
        BlockPos targetPos = resolveMarkerTarget(player, context);
        if (targetPos != null) {
            player.sendSystemMessage(Component.literal("§6[" + speaker + "] §f" + context.step().title() + " is at "
                + formatCrowQuestCoordinates(targetPos, targetLocation)));
        } else {
            player.sendSystemMessage(Component.literal("§6[" + speaker + "] §f" + context.step().title() + " is nearby."));
        }
        if (isPermanenceHungerUnendingFeed(context)) {
            player.sendSystemMessage(Component.literal("§6[" + speaker + "] §f" + getPermanenceHungerUnendingProgressSummary(player)));
        }
        return true;
    }

    private static BlockPos resolveSwampDemonPosition(ServerPlayer player) {
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return null;
        }
        // Find the nearest quest-targeted swamp demon
        net.minecraft.world.phys.AABB searchArea = new net.minecraft.world.phys.AABB(player.blockPosition()).inflate(256.0D);
        List<net.minecraft.world.entity.Entity> demons = serverLevel.getEntities((net.minecraft.world.entity.Entity) null, searchArea,
            entity -> "swamp_demon_kidnappers_bog_satoko".equals(entity.getPersistentData().getString(QuestScenarioActions.QUEST_TARGET_ID_TAG))
                || "swamp_demon_kidnappers_bog".equals(entity.getPersistentData().getString(QuestScenarioActions.QUEST_TARGET_ID_TAG)));
        if (demons.isEmpty()) {
            return null;
        }
        return demons.stream()
            .min(java.util.Comparator.comparingDouble(player::distanceToSqr))
            .map(net.minecraft.world.entity.Entity::blockPosition)
            .orElse(null);
    }

    private static MarkerResult resolveMeditationMenuMarker(ServerPlayer player, PlayerRole role) {
        String selectedType = player.getPersistentData().getString("MeditationSelectedType");
        String selectedId = player.getPersistentData().getString("MeditationSelectedId");
        if (selectedType == null || selectedType.isBlank() || selectedId == null || selectedId.isBlank()) {
            return null;
        }

        if (SELECTED_TYPE_LOCATION.equals(selectedType)) {
            return resolveLocationMarker(player, selectedId);
        }
        if (SELECTED_TYPE_QUEST.equals(selectedType)) {
            return resolveQuestMarker(player, role, selectedId);
        }
        return null;
    }

    private static final String SELECTED_TYPE_LOCATION = "location";
    private static final String SELECTED_TYPE_QUEST = "quest";

    private static MarkerResult resolveLocationMarker(ServerPlayer player, String locationId) {
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return null;
        }

        if (SWORDSMITH_VILLAGE_LOCATION_ID.equals(locationId)) {
            return resolveSwordsmithVillageMarker(player, serverLevel, true);
        }
        if ("night_hunt".equals(locationId)) {
            BlockPos huntPos = resolveDemonHuntMarker(player);
            return huntPos == null ? null : new MarkerResult("Night Hunting Grounds", huntPos);
        }
        if ("mt_yoko".equals(locationId)) {
            BlockPos mtYokoPos = resolveMtYokoMarker(player);
            return mtYokoPos == null ? null : new MarkerResult("Mt. Yoko", mtYokoPos);
        }
        if ("mt_natagumo".equals(locationId)) {
            BlockPos mtNatagumoPos = resolveMtNatagumoMarker(player);
            return mtNatagumoPos == null ? null : new MarkerResult("Mt. Natagumo", mtNatagumoPos);
        }

        ResourceLocation structureId = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", locationId);
        TagKey<Structure> tagKey = QuestStructureTags.tagFor(structureId);
        BlockPos structurePos = serverLevel.findNearestMapStructure(tagKey,
            player.blockPosition(), 100, false);
        if (structurePos == null) {
            return null;
        }

        int y = serverLevel.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
            structurePos.getX(), structurePos.getZ());
        BlockPos surfacePos = new BlockPos(structurePos.getX(), y + 1, structurePos.getZ());

        // Get display name from location ID
        String displayName = switch (locationId) {
            case "house_kocho" -> "Kocho House";
            case "graveyard" -> "Graveyard";
            case "house_rengoku" -> "Rengoku House";
            case "house_tamayo" -> "Tamayo House";
            case "house_tanjiro" -> "Tanjiro House";
            case "house_ubuyashiki" -> "Ubuyashiki House";
            case "house_urokodaki" -> "Urokodaki House";
            case SWORDSMITH_VILLAGE_LOCATION_ID -> "Swordsmith Village";
            default -> locationId;
        };

        return new MarkerResult(displayName, surfacePos);
    }

    private static MarkerResult resolveSwordsmithVillageMarker(ServerPlayer player, ServerLevel serverLevel, boolean includePrompt) {
        StructureLocationCache.CachedStructure currentStructure = StructureLocationCache
            .getStructureAt(serverLevel, player.blockPosition())
            .filter(cached -> isSwordsmithAccessStructure(cached.structureId))
            .orElse(null);

        if (currentStructure != null) {
            Mob kakushi = ensureSwordsmithGuideKakushi(serverLevel, currentStructure);
            if (kakushi == null) {
                return new MarkerResult("Swordsmith Village Kakushi", currentStructure.center);
            }
            return new MarkerResult(
                "Swordsmith Village Kakushi",
                kakushi.blockPosition(),
                includePrompt ? "Show your Scarlet Ore to the Kakushi to be transported to the Swordsmith Village." : null
            );
        }

        MarkerResult nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (ResourceLocation structureId : SWORDSMITH_ACCESS_STRUCTURES) {
            BlockPos structurePos = serverLevel.findNearestMapStructure(
                QuestStructureTags.tagFor(structureId),
                player.blockPosition(),
                100,
                false
            );
            if (structurePos == null) {
                continue;
            }

            int y = serverLevel.getHeight(Heightmap.Types.WORLD_SURFACE, structurePos.getX(), structurePos.getZ());
            BlockPos surfacePos = new BlockPos(structurePos.getX(), y + 1, structurePos.getZ());
            double distance = player.blockPosition().distSqr(surfacePos);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = new MarkerResult("Swordsmith Village", surfacePos);
            }
        }

        return nearest;
    }

    private static void tickSwordsmithVillageNavigation(ServerPlayer player) {
        String selectedType = player.getPersistentData().getString("MeditationSelectedType");
        String selectedId = player.getPersistentData().getString("MeditationSelectedId");
        if (!SELECTED_TYPE_LOCATION.equals(selectedType) || !SWORDSMITH_VILLAGE_LOCATION_ID.equals(selectedId)) {
            return;
        }
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        StructureLocationCache.CachedStructure currentStructure = StructureLocationCache
            .getStructureAt(serverLevel, player.blockPosition())
            .filter(cached -> isSwordsmithAccessStructure(cached.structureId))
            .orElse(null);
        if (currentStructure == null) {
            return;
        }

        Mob kakushi = ensureSwordsmithGuideKakushi(serverLevel, currentStructure);
        if (kakushi == null) {
            return;
        }

        long now = serverLevel.getGameTime();
        int lastX = player.getPersistentData().getInt(SWORDSMITH_NAV_STRUCTURE_X);
        int lastZ = player.getPersistentData().getInt(SWORDSMITH_NAV_STRUCTURE_Z);
        long lastMessageTick = player.getPersistentData().getLong(SWORDSMITH_NAV_MESSAGE_TICK);
        boolean sameStructure = lastX == currentStructure.center.getX() && lastZ == currentStructure.center.getZ();
        if (sameStructure && now - lastMessageTick < SWORDSMITH_NAV_MESSAGE_COOLDOWN_TICKS) {
            return;
        }

        player.getPersistentData().putInt(SWORDSMITH_NAV_STRUCTURE_X, currentStructure.center.getX());
        player.getPersistentData().putInt(SWORDSMITH_NAV_STRUCTURE_Z, currentStructure.center.getZ());
        player.getPersistentData().putLong(SWORDSMITH_NAV_MESSAGE_TICK, now);

        ModNetworking.sendToPlayer(new SetCrowQuestMarkerPacket(
            Vec3.atBottomCenterOf(kakushi.blockPosition()),
            20 * 60
        ), player);
        player.sendSystemMessage(Component.literal(
            "§6[Crow] §fShow your Scarlet Ore to the Kakushi to be transported to the Swordsmith Village."
        ));
    }

    private static boolean isSwordsmithAccessStructure(ResourceLocation structureId) {
        return SWORDSMITH_ACCESS_STRUCTURES.contains(structureId);
    }

    private static Mob ensureSwordsmithGuideKakushi(ServerLevel level, StructureLocationCache.CachedStructure structure) {
        Mob existing = findSwordsmithGuideKakushi(level, structure.center);
        if (existing != null) {
            configureSwordsmithGuideKakushi(existing);
            return existing;
        }

        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(KAKUSHI_ID).orElse(null);
        if (entityType == null) {
            return null;
        }

        BlockPos spawnPos = findSafeSurfacePosNear(level, structure.center, 24);
        if (spawnPos == null) {
            return null;
        }

        Entity created = entityType.create(level);
        if (!(created instanceof Mob kakushi)) {
            return null;
        }

        kakushi.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, 0.0F, 0.0F);
        configureSwordsmithGuideKakushi(kakushi);
        kakushi.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.MOB_SUMMONED, null, null);
        return level.addFreshEntity(kakushi) ? kakushi : null;
    }

    private static Mob findSwordsmithGuideKakushi(ServerLevel level, BlockPos center) {
        List<Mob> kakushi = level.getEntitiesOfClass(
            Mob.class,
            new AABB(center).inflate(SWORDSMITH_KAKUSHI_SEARCH_RADIUS),
            mob -> mob.isAlive() && KAKUSHI_ID.equals(EntityType.getKey(mob.getType()))
        );
        if (kakushi.isEmpty()) {
            return null;
        }
        return kakushi.stream()
            .min(java.util.Comparator.comparingDouble(mob -> mob.blockPosition().distSqr(center)))
            .orElse(null);
    }

    private static void configureSwordsmithGuideKakushi(Mob kakushi) {
        kakushi.setPersistenceRequired();
        kakushi.getPersistentData().putBoolean(SWORDSMITH_NAV_KAKUSHI_TAG, true);
    }

    private static BlockPos findSafeSurfacePosNear(ServerLevel level, BlockPos center, int maxRadius) {
        for (int radius = 0; radius <= maxRadius; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }
                    int x = center.getX() + dx;
                    int z = center.getZ() + dz;
                    int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                    BlockPos candidate = new BlockPos(x, y, z);
                    if (isSafeSpawnSurface(level, candidate)) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    private static boolean isSafeSpawnSurface(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return false;
        }

        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        BlockState below = level.getBlockState(pos.below());

        return below.isSolidRender(level, pos.below())
            && feet.getCollisionShape(level, pos).isEmpty()
            && head.getCollisionShape(level, pos.above()).isEmpty();
    }

    private static MarkerResult resolveQuestMarker(ServerPlayer player, PlayerRole role, String questGroupId) {
        QuestRuntimeContext context = getOrInitializeContext(player, role);
        if (context == null || !context.group().id().equals(questGroupId)) {
            return null;
        }

        BlockPos targetPos = resolveMarkerTarget(player, context);
        Vec3 targetLocation = resolveQuestMarkerTargetLocation(player, context);
        if (targetPos == null && targetLocation == null) {
            return null;
        }
        if (targetPos == null) {
            targetPos = BlockPos.containing(targetLocation);
        }

        String stepName = context.step().title();
        String extraMessage = null;
        if (isPermanenceFirstTaste(context)) {
            extraMessage = getPermanenceFirstTasteProgressSummary(player);
        } else if (isPermanenceHungerUnendingFeed(context)) {
            extraMessage = getPermanenceHungerUnendingProgressSummary(player);
        }
        return new MarkerResult(stepName, targetPos, targetLocation, extraMessage);
    }

    private static String formatCrowQuestCoordinates(BlockPos position, Vec3 targetLocation) {
        if (targetLocation != null) {
            int x = (int) Math.floor(targetLocation.x);
            int y = (int) Math.floor(targetLocation.y);
            int z = (int) Math.floor(targetLocation.z);
            if (position == null || Math.abs(targetLocation.y - position.getY()) > 0.001D) {
                return x + " " + y + " " + z;
            }
        }
        if (position != null) {
            return position.getX() + " ~ " + position.getZ();
        }
        return "";
    }

    private record MarkerResult(String name, BlockPos position, Vec3 targetLocation, String extraCrowMessage) {
        MarkerResult {
        }

        private MarkerResult(String name, BlockPos position) {
            this(name, position, Vec3.atBottomCenterOf(position), null);
        }

        private MarkerResult(String name, BlockPos position, String extraCrowMessage) {
            this(name, position, Vec3.atBottomCenterOf(position), extraCrowMessage);
        }
    }

    public static List<MeditationMenuData.QuestEntry> buildQuestEntries(ServerPlayer player, PlayerRole role) {
        QuestRuntimeContext active = getOrInitializeContext(player, role);
        List<MeditationMenuData.QuestEntry> entries = new ArrayList<>();

        for (QuestGroupDefinition group : QuestGroupRegistry.getAvailableGroups(role)) {
            // Skip quest groups that are locked behind requirements not yet met
            if (!isQuestGroupUnlocked(player, group)) {
                continue;
            }
            boolean completed = isQuestGroupComplete(player, group);
            boolean isActive = active != null && active.group().id().equals(group.id());
            List<String> description = new ArrayList<>();
            description.add(group.summary());

            String progressText = "No runtime stages available";

            if (!group.stages().isEmpty()) {
                int stageIndex = isActive ? active.stageIndex() : 0;
                QuestStageDefinition stage = group.stages().get(Math.min(stageIndex, group.stages().size() - 1));
                description.add("");
                description.add("Current Quest: " + stage.name());
                description.add(stage.summary());

                if (isActive) {
                    description.add("");
                    description.add("Current Step: " + active.step().title());
                    description.add(active.step().description());
                    progressText = buildProgressText(player, active, group, stage);
                } else if (completed) {
                    progressText = "Stage No.1 complete";
                } else {
                    progressText = "Quest chain ready";
                }
            }

            List<String> rewards = new ArrayList<>();
            if (!group.stages().isEmpty()) {
                QuestRewardDefinition rewardDefinition = group.stages().get(0).rewards();
                if (rewardDefinition.experiencePoints() > 0) {
                    rewards.add(rewardDefinition.experiencePoints() + " XP");
                }
                if (rewardDefinition.sunBreathingLevelDelta() > 0) {
                    rewards.add("Sun Breathing +" + rewardDefinition.sunBreathingLevelDelta());
                }
                for (QuestRewardDefinition.ItemReward itemReward : rewardDefinition.itemRewards()) {
                    Item item = ForgeRegistries.ITEMS.getValue(itemReward.itemId());
                    String itemName;
                    if (item != null) {
                        itemName = item.getDescription().getString();
                    } else {
                        itemName = itemReward.itemId().getPath().replace('_', ' ');
                    }
                    rewards.add(itemReward.count() + "x " + itemName);
                }
            }

            entries.add(new MeditationMenuData.QuestEntry(
                group.id(),
                group.name(),
                QuestCategory.MAIN_STORY.getDisplayName(),
                QuestCategory.MAIN_STORY.getColor(),
                description,
                rewards,
                progressText,
                completed,
                false
            ));
        }

        return entries;
    }

    public static String getCurrentStageName(ServerPlayer player, PlayerRole role, String groupId) {
        QuestGroupDefinition groupDef = QuestGroupRegistry.get(groupId);
        // Don't auto-initialize if the quest group isn't unlocked yet
        if (groupDef != null && !isQuestGroupUnlocked(player, groupDef)) {
            return "";
        }
        QuestRuntimeContext context = getOrInitializeContext(player, role);
        if (context != null && context.group().id().equals(groupId)) {
            return context.stage().name();
        }
        QuestGroupDefinition group = QuestGroupRegistry.get(groupId);
        if (group == null || group.stages().isEmpty()) {
            return "";
        }
        return group.stages().get(0).name();
    }

    private static QuestRuntimeContext getOrInitializeContext(ServerPlayer player, PlayerRole role) {
        if (!CustomProgressionConfig.isCustomProgressionEnabled()) {
            return null;
        }

        String groupId = player.getPersistentData().getString(ACTIVE_GROUP_ID);
        QuestGroupDefinition group = QuestGroupRegistry.get(groupId);
        if (group == null || !group.isAvailableFor(role)) {
            if (DemonTransformationHandler.isPersistentDemonhoodPending(player)) {
                return null;
            }
            group = QuestGroupRegistry.getInitialActiveGroup(role);
            if (group == null) {
                clearRuntimeState(player);
                return null;
            }
            // Gate auto-activation of the "cruel" quest on final selection completion
            if (group.id().equals("cruel") && !hasCompletedFinalSelection(player)) {
                clearRuntimeState(player);
                return null;
            }
            if (isQuestGroupComplete(player, group)) {
                clearRuntimeState(player);
                return null;
            }
            player.getPersistentData().putString(ACTIVE_GROUP_ID, group.id());
            player.getPersistentData().putInt(ACTIVE_STAGE_INDEX, initialStageIndex(player, group));
            player.getPersistentData().putInt(ACTIVE_STEP_INDEX, 0);
            player.getPersistentData().putBoolean(ACTIVE_STEP_STARTED, false);
        }

        if (group.stages().isEmpty()) {
            return null;
        }

        migratePermanenceStageIfNeeded(player, group);

        int stageIndex = Math.min(player.getPersistentData().getInt(ACTIVE_STAGE_INDEX), group.stages().size() - 1);
        QuestStageDefinition stage = group.stages().get(stageIndex);
        int stepIndex = Math.min(player.getPersistentData().getInt(ACTIVE_STEP_INDEX), stage.steps().size() - 1);
        QuestStepDefinition step = stage.steps().get(stepIndex);
        return new QuestRuntimeContext(player, group, stage, step, stageIndex, stepIndex);
    }

    private static boolean isStepComplete(ServerPlayer player, QuestRuntimeContext context) {
        QuestStepDefinition step = context.step();
        return switch (step.type()) {
            case ENTER_STRUCTURE -> isInTargetStructure(player, step.targetId());
            case ENTER_BIOME -> isInTargetBiome(player, step.targetId());
            case KILL_ENTITY, TALK_TO_ENTITY -> false;
            case OBTAIN_ITEM -> hasRequiredItem(player, step.targetId(), step.requiredCount());
            case WAIT_FOR_NIGHT -> player.level().isNight();
            case CUSTOM -> step.customCheck().test(player, context);
        };
    }

    private static boolean isStepStartTimeSatisfied(ServerPlayer player, QuestStepDefinition step) {
        Boolean requiredDay = step.requiredTimeOfDay();
        if (requiredDay == null) {
            return true;
        }
        return requiredDay ? player.level().isDay() : player.level().isNight();
    }

    private static void maybeSendStepTimeBlockedMessage(ServerPlayer player, QuestStepDefinition step) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        long now = serverLevel.getGameTime();
        long lastNotice = player.getPersistentData().getLong(STEP_TIME_BLOCKED_NOTICE_TICK);
        if (now - lastNotice < STEP_TIME_BLOCKED_NOTICE_INTERVAL_TICKS) {
            return;
        }
        player.getPersistentData().putLong(STEP_TIME_BLOCKED_NOTICE_TICK, now);
        Boolean requiredDay = step.requiredTimeOfDay();
        if (requiredDay == null) {
            return;
        }
        player.sendSystemMessage(Component.literal(requiredDay
            ? "§7You need to wait until daytime before this step can begin."
            : "§7You need to wait until nighttime before this step can begin."));
    }

    private static boolean isHumanFleshQuestItem(ResourceLocation itemId) {
        String namespace = itemId.getNamespace();
        String path = itemId.getPath();
        return ("kimetsunoyaiba".equals(namespace) && ("human_flesh".equals(path) || "human_flesh_2".equals(path)))
            || (KimetsunoyaibaMultiplayer.MODID.equals(namespace)
                && ("human_flesh_3".equals(path) || "human_flesh_4".equals(path) || "human_flesh_5".equals(path)));
    }

    private static String buildProgressText(ServerPlayer player, QuestRuntimeContext active,
                                            QuestGroupDefinition group, QuestStageDefinition stage) {
        if (isPermanenceFirstTaste(active)) {
            return "Quest " + (active.stageIndex() + 1) + "/" + group.stages().size()
                + " | Kills " + getPermanenceFirstTasteKillProgress(player) + "/" + PERMANENCE_FIRST_TASTE_REQUIRED
                + " | Eaten " + getPermanenceFirstTasteEatenProgress(player) + "/" + PERMANENCE_FIRST_TASTE_REQUIRED;
        }
        if (isPermanenceHungerUnendingFeed(active)) {
            return "Quest " + (active.stageIndex() + 1) + "/" + group.stages().size()
                + " | Sleeping Kills " + getPermanenceHungerUnendingSleepKillProgress(player) + "/" + PERMANENCE_HUNGER_UNENDING_REQUIRED
                + " | Eaten " + getPermanenceHungerUnendingEatenProgress(player) + "/" + PERMANENCE_HUNGER_UNENDING_REQUIRED;
        }

        return "Quest " + (active.stageIndex() + 1) + "/" + group.stages().size() +
            " | Step " + (active.stepIndex() + 1) + "/" + stage.steps().size();
    }

    private static void completeStep(ServerPlayer player, QuestRuntimeContext context) {
        context.step().onComplete().accept(player, context);

        int nextStepIndex = context.stepIndex() + 1;
        if (nextStepIndex < context.stage().steps().size()) {
            player.getPersistentData().putInt(ACTIVE_STEP_INDEX, nextStepIndex);
            player.getPersistentData().putBoolean(ACTIVE_STEP_STARTED, false);
            QuestStepDefinition nextStep = context.stage().steps().get(nextStepIndex);
            player.sendSystemMessage(Component.literal("§aQuest Updated: §f" + nextStep.title()));
            return;
        }

        applyRewards(player, context.stage().rewards());
        int nextStageIndex = context.stageIndex() + 1;
        if (nextStageIndex < context.group().stages().size()) {
            player.getPersistentData().putInt(ACTIVE_STAGE_INDEX, nextStageIndex);
            player.getPersistentData().putInt(ACTIVE_STEP_INDEX, 0);
            player.getPersistentData().putBoolean(ACTIVE_STEP_STARTED, false);
            QuestStageDefinition nextStage = context.group().stages().get(nextStageIndex);
            player.sendSystemMessage(Component.literal("§6New Quest: §f" + nextStage.name()));
        } else {
            player.sendSystemMessage(Component.literal("§6Quest Group Complete: §f" + context.group().name()));
            clearRuntimeState(player);
        }
    }

    private static void applyRewards(ServerPlayer player, QuestRewardDefinition rewards) {
        if (rewards.experiencePoints() > 0) {
            player.giveExperiencePoints(rewards.experiencePoints());
        }
        if (rewards.sunBreathingLevelDelta() > 0) {
            int newLevel = SunBreathingLevelHelper.getSunBreathingLevel(player) + rewards.sunBreathingLevelDelta();
            SunBreathingLevelHelper.setSunBreathingLevel(player, newLevel);
        }
        for (QuestRewardDefinition.ItemReward itemReward : rewards.itemRewards()) {
            Item item = ForgeRegistries.ITEMS.getValue(itemReward.itemId());
            if (item != null) {
                QuestItemHelper.addQuestItem(player, new ItemStack(item, itemReward.count()));
            }
        }
        for (ResourceLocation advancementId : rewards.advancementRewards()) {
            Advancement advancement = player.server.getAdvancements().getAdvancement(advancementId);
            if (advancement != null) {
                for (String criterion : player.getAdvancements().getOrStartProgress(advancement).getRemainingCriteria()) {
                    player.getAdvancements().award(advancement, criterion);
                }
            }
        }
    }

    private static BlockPos resolveMarkerTarget(ServerPlayer player, QuestRuntimeContext context) {
        if (isPermanenceFirstTaste(context)) {
            return resolveDemonHuntMarker(player);
        }

        if (shouldRespawnMissingKazumiForMarker(context)) {
            BlockPos kazumiPos = QuestScenarioActions.findNearestQuestEntity(player, "kazumi", 400.0D);
            if (kazumiPos == null) {
                QuestScenarioActions.ensureKazumiSpawned(player, context);
                kazumiPos = QuestScenarioActions.findNearestQuestEntity(player, "kazumi", 400.0D);
            }
            if (kazumiPos != null) {
                return kazumiPos;
            }
        }

        BlockPos custom = context.step().markerResolver().apply(player, context);
        if (custom != null) {
            return custom;
        }

        return switch (context.step().type()) {
            case ENTER_STRUCTURE -> QuestScenarioActions.findNearestStructure(player.serverLevel(), player.blockPosition(), context.step().targetId());
            case TALK_TO_ENTITY, KILL_ENTITY -> QuestScenarioActions.findNearestQuestEntity(player, context.step().targetKey(), 400.0D);
            default -> null;
        };
    }

    private static Vec3 resolveQuestMarkerTargetLocation(ServerPlayer player, QuestRuntimeContext context) {
        if (!context.step().targetKey().isBlank()) {
            Vec3 entityCenter = QuestScenarioActions.findNearestQuestEntityCenter(player, context.step().targetKey(), 400.0D);
            if (entityCenter != null) {
                return entityCenter;
            }
        }

        BlockPos targetPos = resolveMarkerTarget(player, context);
        if (targetPos != null) {
            return Vec3.atBottomCenterOf(targetPos);
        }

        return null;
    }

    private static boolean shouldRespawnMissingKazumiForMarker(QuestRuntimeContext context) {
        return "cruel".equals(context.group().id())
            && "kidnappers_bog".equals(context.stage().id())
            && "kazumi".equals(context.step().targetKey());
    }

    private static boolean isInTargetStructure(ServerPlayer player, ResourceLocation structureId) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        return StructureLocationCache.getStructureAt(serverLevel, player.blockPosition())
            .map(cached -> cached.structureId.equals(structureId))
            .orElse(false);
    }

    private static boolean isInTargetBiome(ServerPlayer player, ResourceLocation biomeId) {
        return player.level().getBiome(player.blockPosition()).unwrapKey()
            .map(key -> key.location().equals(biomeId))
            .orElse(false);
    }

    private static boolean hasRequiredItem(ServerPlayer player, ResourceLocation itemId, int count) {
        Item item = ForgeRegistries.ITEMS.getValue(itemId);
        if (item == null) {
            return false;
        }
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total >= count;
    }

    private static boolean matchesTarget(QuestStepDefinition step, Entity entity) {
        if (!step.targetKey().isBlank()) {
            if (step.targetKey().equals(entity.getPersistentData().getString(QuestScenarioActions.QUEST_NPC_ID_TAG))) {
                return true;
            }
            if (step.targetKey().equals(entity.getPersistentData().getString(QuestScenarioActions.QUEST_TARGET_ID_TAG))) {
                return true;
            }
        }
        if (step.targetId() == null) {
            return false;
        }
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return step.targetId().equals(entityId);
    }

    private static void clearRuntimeState(ServerPlayer player) {
        player.getPersistentData().remove(ACTIVE_GROUP_ID);
        player.getPersistentData().remove(ACTIVE_STAGE_INDEX);
        player.getPersistentData().remove(ACTIVE_STEP_INDEX);
        player.getPersistentData().remove(ACTIVE_STEP_STARTED);
    }

    private static boolean hasCompletedFinalSelection(ServerPlayer player) {
        if (player.server == null) {
            return false;
        }
        Advancement advancement = player.server.getAdvancements().getAdvancement(COMPLETED_FINAL_SELECTION);
        if (advancement == null) {
            return false;
        }
        return player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    private static boolean isQuestGroupUnlocked(ServerPlayer player, QuestGroupDefinition group) {
        return switch (group.id()) {
            case "cruel" -> hasCompletedFinalSelection(player);
            default -> true;
        };
    }

    private static boolean isQuestGroupComplete(ServerPlayer player, QuestGroupDefinition group) {
        return false;
    }

    private static int initialStageIndex(ServerPlayer player, QuestGroupDefinition group) {
        if ("permanence".equals(group.id()) && isPermanenceFirstTasteCompleted(player) && group.stages().size() > 1) {
            return 1;
        }
        return 0;
    }

    private static void migratePermanenceStageIfNeeded(ServerPlayer player, QuestGroupDefinition group) {
        if (!"permanence".equals(group.id()) || !isPermanenceFirstTasteCompleted(player) || group.stages().size() <= 1) {
            return;
        }
        if (player.getPersistentData().getInt(ACTIVE_STAGE_INDEX) > 0) {
            return;
        }
        player.getPersistentData().putInt(ACTIVE_STAGE_INDEX, 1);
        player.getPersistentData().putInt(ACTIVE_STEP_INDEX, 0);
        player.getPersistentData().putBoolean(ACTIVE_STEP_STARTED, false);
    }

    private static boolean isPermanenceFirstTaste(QuestRuntimeContext context) {
        return context != null
            && "permanence".equals(context.group().id())
            && "first_taste_of_blood".equals(context.stage().id())
            && "eat_human_flesh".equals(context.step().id());
    }

    private static boolean isPermanenceHungerUnendingFeed(QuestRuntimeContext context) {
        return context != null
            && "permanence".equals(context.group().id())
            && "hunger_unending".equals(context.stage().id())
            && "feed_without_detection".equals(context.step().id());
    }

    private static boolean isPermanenceSlayersBlood(QuestRuntimeContext context) {
        return context != null
            && "permanence".equals(context.group().id())
            && "slayers_blood".equals(context.stage().id());
    }

    private static boolean isPermanenceSlayersBloodTalkToKamanue(QuestRuntimeContext context) {
        return isPermanenceSlayersBlood(context) && "talk_to_kamanue".equals(context.step().id());
    }

    private static boolean isPermanenceSlayersBloodTerrorize(QuestRuntimeContext context) {
        return isPermanenceSlayersBlood(context) && "terrorize_village".equals(context.step().id());
    }

    private static boolean isPermanenceSlayersBloodCaptiveReturn(QuestRuntimeContext context) {
        return isPermanenceSlayersBlood(context) && "bring_captive_to_kamanue".equals(context.step().id());
    }

    private static boolean isPermanenceSlayersBloodStudy(QuestRuntimeContext context) {
        return isPermanenceSlayersBlood(context) && "study_slayer_breathing".equals(context.step().id());
    }

    private static boolean isPermanenceSlayersBloodFinalFight(QuestRuntimeContext context) {
        return isPermanenceSlayersBlood(context) && "kill_studied_slayer".equals(context.step().id());
    }

    private static boolean isPermanenceSlayersBloodVillageAmbushActive(QuestRuntimeContext context) {
        return isPermanenceSlayersBlood(context)
            && ("terrorize_village".equals(context.step().id())
            || "bring_captive_to_kamanue".equals(context.step().id()));
    }

    private static void handlePermanenceFirstTasteKill(ServerPlayer player, LivingEntity victim, QuestRuntimeContext context) {
        if (!isQuestHuman(victim)) {
            return;
        }

        int progress = Math.min(PERMANENCE_FIRST_TASTE_REQUIRED,
            player.getPersistentData().getInt(PERMANENCE_FIRST_TASTE_KILLS) + 1);
        player.getPersistentData().putInt(PERMANENCE_FIRST_TASTE_KILLS, progress);
        sendPermanenceFirstTasteProgress(player, "Humans killed");
        if (context.step().customCheck().test(player, context)) {
            completeStep(player, context);
        }
    }

    private static boolean isQuestHuman(LivingEntity victim) {
        if (victim == null) {
            return false;
        }
        if (victim instanceof Player player) {
            return !player.getPersistentData().getBoolean("oni");
        }

        ResourceLocation victimId = EntityType.getKey(victim.getType());
        return KAKUSHI_ID.equals(victimId)
            || EntityTagHelper.isDemonSlayer(victim)
            || EntityTagHelper.isSwordSmith(victim)
            || EntityTagHelper.isCivilian(victim)
            || victim.getType().is(WOMAN);
    }

    private static boolean handlePermanenceFirstTasteCrowInteract(ServerPlayer player) {
        sendPermanenceFirstTasteProgress(player, null);

        BlockPos targetPos = resolveDemonHuntMarker(player);
        if (targetPos == null) {
            player.sendSystemMessage(Component.literal("§6[Crow] §fNo nearby hunting grounds could be found yet."));
            return true;
        }

        ModNetworking.sendToPlayer(new SetCrowQuestMarkerPacket(
            Vec3.atBottomCenterOf(targetPos),
            20 * 60
        ), player);
        player.sendSystemMessage(Component.literal("§6[Crow] §fNight Hunting Grounds are at "
            + targetPos.getX() + " ~ " + targetPos.getZ()));
        return true;
    }

    private static void sendPermanenceFirstTasteProgress(ServerPlayer player, String changedCounter) {
        String prefix = changedCounter == null ? "§cFirst Taste of Blood" : "§cFirst Taste of Blood - " + changedCounter;
        player.sendSystemMessage(Component.literal(prefix + ": §f" + getPermanenceFirstTasteProgressSummary(player)));
    }

    private static void sendPermanenceHungerUnendingProgress(ServerPlayer player, String changedCounter) {
        String prefix = changedCounter == null ? "§cHunger Unending" : "§cHunger Unending - " + changedCounter;
        player.sendSystemMessage(Component.literal(prefix + ": §f" + getPermanenceHungerUnendingProgressSummary(player)));
    }

    private static String getPermanenceFirstTasteProgressSummary(ServerPlayer player) {
        int kills = getPermanenceFirstTasteKillProgress(player);
        int eaten = getPermanenceFirstTasteEatenProgress(player);
        return "Kills " + kills + "/" + PERMANENCE_FIRST_TASTE_REQUIRED
            + " | Eaten " + eaten + "/" + PERMANENCE_FIRST_TASTE_REQUIRED;
    }

    private static String getPermanenceHungerUnendingProgressSummary(ServerPlayer player) {
        int kills = getPermanenceHungerUnendingSleepKillProgress(player);
        int eaten = getPermanenceHungerUnendingEatenProgress(player);
        return "Sleeping Kills " + kills + "/" + PERMANENCE_HUNGER_UNENDING_REQUIRED
            + " | Eaten " + eaten + "/" + PERMANENCE_HUNGER_UNENDING_REQUIRED;
    }

    private static BlockPos resolveDemonHuntMarker(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        return QuestScenarioActions.findNearestVanillaVillage(serverLevel, player.blockPosition());
    }

    private static BlockPos resolveMtYokoMarker(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        return QuestScenarioActions.findNearestBiome(serverLevel, player.blockPosition(), MT_YOKO_BIOME);
    }

    private static BlockPos resolveMtNatagumoMarker(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        return QuestScenarioActions.findNearestBiome(serverLevel, player.blockPosition(), MT_NATAGUMO_BIOME);
    }
}
