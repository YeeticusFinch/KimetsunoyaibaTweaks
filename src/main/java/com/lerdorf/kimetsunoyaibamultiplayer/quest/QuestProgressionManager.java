package com.lerdorf.kimetsunoyaibamultiplayer.quest;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.config.CustomProgressionConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.meditation.MeditationStatsTracker;
import com.lerdorf.kimetsunoyaibamultiplayer.meditation.MeditationMenuData;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SetCrowQuestMarkerPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.raids.StructureLocationCache;
import com.lerdorf.kimetsunoyaibamultiplayer.util.SunBreathingLevelHelper;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public final class QuestProgressionManager {
    private static final String ACTIVE_GROUP_ID = "KnYQuestActiveGroup";
    private static final String ACTIVE_STAGE_INDEX = "KnYQuestActiveStageIndex";
    private static final String ACTIVE_STEP_INDEX = "KnYQuestActiveStepIndex";
    private static final String ACTIVE_STEP_STARTED = "KnYQuestActiveStepStarted";
    private static final String PERMANENCE_FIRST_TASTE_PROGRESS = "KnYPermanenceFirstTasteProgress";
    private static final int PERMANENCE_FIRST_TASTE_REQUIRED = 10;

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

        QuestRuntimeContext context = getOrInitializeContext(player, role);
        if (context == null) {
            return;
        }

        QuestStepDefinition step = context.step();
        if (!player.getPersistentData().getBoolean(ACTIVE_STEP_STARTED)) {
            step.onStart().accept(player, context);
            player.getPersistentData().putBoolean(ACTIVE_STEP_STARTED, true);
        }

        step.onTick().accept(player, context);
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

    public static void handleKill(ServerPlayer player, LivingEntity victim, PlayerRole role) {
        if (!CustomProgressionConfig.isCustomProgressionEnabled()) {
            return;
        }
        QuestRuntimeContext context = getOrInitializeContext(player, role);
        if (context == null || context.step().type() != QuestStepType.KILL_ENTITY) {
            return;
        }
        if (matchesTarget(context.step(), victim)) {
            completeStep(player, context);
        }
    }

    public static void handleHumanFleshConsumed(ServerPlayer player, ItemStack stack, PlayerRole role) {
        if (!CustomProgressionConfig.isCustomProgressionEnabled() || role != PlayerRole.DEMON || stack.isEmpty()) {
            return;
        }

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null || !isHumanFleshQuestItem(itemId)) {
            return;
        }

        QuestRuntimeContext context = getOrInitializeContext(player, role);
        if (context == null || !"permanence".equals(context.group().id()) || !"eat_human_flesh".equals(context.step().id())) {
            return;
        }

        int progress = Math.min(PERMANENCE_FIRST_TASTE_REQUIRED,
            player.getPersistentData().getInt(PERMANENCE_FIRST_TASTE_PROGRESS) + 1);
        player.getPersistentData().putInt(PERMANENCE_FIRST_TASTE_PROGRESS, progress);
        player.sendSystemMessage(Component.literal("§cFirst Taste of Blood: §f" + progress + "/" + PERMANENCE_FIRST_TASTE_REQUIRED));
    }

    public static int getPermanenceFirstTasteProgress(ServerPlayer player) {
        return Math.min(PERMANENCE_FIRST_TASTE_REQUIRED,
            player.getPersistentData().getInt(PERMANENCE_FIRST_TASTE_PROGRESS));
    }

    public static int getPermanenceFirstTasteKillProgress(ServerPlayer player) {
        return Math.min(PERMANENCE_FIRST_TASTE_REQUIRED,
            player.getPersistentData().getInt(MeditationStatsTracker.HUMAN_KILLS_TOTAL));
    }

    public static boolean handleCrowInteract(ServerPlayer player, PlayerRole role) {
        if (!CustomProgressionConfig.isCustomProgressionEnabled()) {
            return false;
        }

        // Check if Kazumi is walking and show Satoko's disappearance coordinates
        String kazumiCoords = QuestScenarioActions.getKazumiTargetCoordinates(player);
        if (kazumiCoords != null && QuestScenarioActions.isKazumiWalking(player)) {
            player.sendSystemMessage(Component.literal("§6[Crow] §fSatoko disappeared at " + kazumiCoords));
            return true;
        }

        // First check meditation menu selection for waypoint
        MarkerResult meditationMarker = resolveMeditationMenuMarker(player, role);
        if (meditationMarker != null) {
            ModNetworking.sendToPlayer(new SetCrowQuestMarkerPacket(
                Vec3.atBottomCenterOf(meditationMarker.position()),
                20 * 60
            ), player);
            player.sendSystemMessage(Component.literal("§6[Crow] §f" + meditationMarker.name() + " is at "
                + meditationMarker.position().getX() + " ~ " + meditationMarker.position().getZ()));
            return true;
        }

        // Fall back to current quest step marker
        QuestRuntimeContext context = getOrInitializeContext(player, role);
        if (context == null) {
            player.sendSystemMessage(Component.literal("§7No active custom-progression quest objective is available."));
            return true;
        }

        BlockPos targetPos = resolveMarkerTarget(player, context);
        if (targetPos == null) {
            // Check if we're in the encounter or kill steps - point to swamp demon directly
            if ("encounter_swamp_demon".equals(context.step().id()) || "kill_swamp_demon".equals(context.step().id())) {
                targetPos = resolveSwampDemonPosition(player);
            }
        }
        
        if (targetPos == null) {
            player.sendSystemMessage(Component.literal("§7Your crow cannot find a marker for this objective yet."));
            return true;
        }

        ModNetworking.sendToPlayer(new SetCrowQuestMarkerPacket(
            Vec3.atBottomCenterOf(targetPos),
            20 * 60
        ), player);
        player.sendSystemMessage(Component.literal("§6[Crow] §f" + context.step().title() + " is at "
            + targetPos.getX() + " ~ " + targetPos.getZ()));
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
            default -> locationId;
        };

        return new MarkerResult(displayName, surfacePos);
    }

    private static MarkerResult resolveQuestMarker(ServerPlayer player, PlayerRole role, String questGroupId) {
        QuestRuntimeContext context = getOrInitializeContext(player, role);
        if (context == null || !context.group().id().equals(questGroupId)) {
            return null;
        }

        BlockPos targetPos = resolveMarkerTarget(player, context);
        if (targetPos == null) {
            return null;
        }

        String stepName = context.step().title();
        return new MarkerResult(stepName, targetPos);
    }

    private record MarkerResult(String name, BlockPos position) { }

    public static List<MeditationMenuData.QuestEntry> buildQuestEntries(ServerPlayer player, PlayerRole role) {
        QuestRuntimeContext active = getOrInitializeContext(player, role);
        List<MeditationMenuData.QuestEntry> entries = new ArrayList<>();

        for (QuestGroupDefinition group : QuestGroupRegistry.getAvailableGroups(role)) {
            // Skip quest groups that are locked behind requirements not yet met
            if (!isQuestGroupUnlocked(player, group)) {
                continue;
            }
            boolean isActive = active != null && active.group().id().equals(group.id());
            List<String> description = new ArrayList<>();
            description.add(group.summary());

            String progressText = "No runtime stages available";
            boolean completed = false;

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
            player.getPersistentData().putString(ACTIVE_GROUP_ID, group.id());
            player.getPersistentData().putInt(ACTIVE_STAGE_INDEX, 0);
            player.getPersistentData().putInt(ACTIVE_STEP_INDEX, 0);
            player.getPersistentData().putBoolean(ACTIVE_STEP_STARTED, false);
        }

        if (group.stages().isEmpty()) {
            return null;
        }

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

    private static boolean isHumanFleshQuestItem(ResourceLocation itemId) {
        String namespace = itemId.getNamespace();
        String path = itemId.getPath();
        return ("kimetsunoyaiba".equals(namespace) && ("human_flesh".equals(path) || "human_flesh_2".equals(path)))
            || (KimetsunoyaibaMultiplayer.MODID.equals(namespace)
                && ("human_flesh_3".equals(path) || "human_flesh_4".equals(path) || "human_flesh_5".equals(path)));
    }

    private static String buildProgressText(ServerPlayer player, QuestRuntimeContext active,
                                            QuestGroupDefinition group, QuestStageDefinition stage) {
        if ("permanence".equals(group.id()) && "eat_human_flesh".equals(active.step().id())) {
            return "Quest " + (active.stageIndex() + 1) + "/" + group.stages().size()
                + " | Kills " + getPermanenceFirstTasteKillProgress(player) + "/" + PERMANENCE_FIRST_TASTE_REQUIRED
                + " | Flesh " + getPermanenceFirstTasteProgress(player) + "/" + PERMANENCE_FIRST_TASTE_REQUIRED;
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
                player.getInventory().placeItemBackInInventory(new ItemStack(item, itemReward.count()));
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
}
