package com.lerdorf.kimetsunoyaibamultiplayer.quest;

import java.util.Comparator;
import java.util.List;

import com.lerdorf.kimetsunoyaibamultiplayer.entities.KazumiEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.SwampDemonEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.raids.StructureLocationCache;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.registries.ForgeRegistries;

public final class QuestScenarioActions {
    public static final String QUEST_NPC_ID_TAG = "KnYQuestNpcId";
    public static final String QUEST_TARGET_ID_TAG = "KnYQuestTargetId";
    public static final String CURRENT_STRUCTURE_X = "KnYQuestStructureX";
    public static final String CURRENT_STRUCTURE_Y = "KnYQuestStructureY";
    public static final String CURRENT_STRUCTURE_Z = "KnYQuestStructureZ";
    public static final String KIDNAPPERS_BOG_ACTIVE_TAG = "KnYKidnappersBogActive";
    public static final String SWAMP_DOMAIN_ENCOUNTER_STARTED_TAG = "KnYSwampDomainEncounterStarted";

    private static final ResourceLocation VILLAGE_SWAMP = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "village_swamp");
    private static final ResourceLocation SWAMP_DEMON_ID = ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "swamp_demon");
    private static final ResourceLocation SATOKOS_BOW = ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "satokos_bow");

    private QuestScenarioActions() {
    }

    public static void storeCurrentStructureCenter(ServerPlayer player, QuestRuntimeContext context) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        StructureLocationCache.getStructureAt(serverLevel, player.blockPosition()).ifPresent(cached -> {
            player.getPersistentData().putInt(CURRENT_STRUCTURE_X, cached.center.getX());
            player.getPersistentData().putInt(CURRENT_STRUCTURE_Y, cached.center.getY());
            player.getPersistentData().putInt(CURRENT_STRUCTURE_Z, cached.center.getZ());
        });
    }

    public static BlockPos getStoredStructureCenter(ServerPlayer player) {
        if (!player.getPersistentData().contains(CURRENT_STRUCTURE_X)) {
            return null;
        }
        return new BlockPos(
            player.getPersistentData().getInt(CURRENT_STRUCTURE_X),
            player.getPersistentData().getInt(CURRENT_STRUCTURE_Y),
            player.getPersistentData().getInt(CURRENT_STRUCTURE_Z)
        );
    }

    public static void ensureKazumiSpawned(ServerPlayer player, QuestRuntimeContext context) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos center = getStoredStructureCenter(player);
        if (center == null) {
            center = findNearestStructure(serverLevel, player.blockPosition(), VILLAGE_SWAMP);
        }
        if (center == null) {
            center = player.blockPosition();
        }

        List<Entity> existing = serverLevel.getEntities((Entity) null,
            new net.minecraft.world.phys.AABB(center).inflate(400.0D),
            entity -> "kazumi".equals(entity.getPersistentData().getString(QUEST_NPC_ID_TAG)));
        if (!existing.isEmpty()) {
            return;
        }

        KazumiEntity kazumi = ModEntities.KAZUMI.get().create(serverLevel);
        if (kazumi == null) {
            return;
        }

        BlockPos spawnPos = findRandomSurfacePosition(serverLevel, center, 24, 12);
        kazumi.getPersistentData().putString(QUEST_NPC_ID_TAG, "kazumi");
        kazumi.setCustomName(Component.literal("Kazumi"));
        kazumi.setCustomNameVisible(true);
        kazumi.setPos(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D);
        serverLevel.addFreshEntity(kazumi);
    }

    public static void makeKazumiLookAtPlayer(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        KazumiEntity kazumi = findNearestKazumi(serverLevel, player);
        if (kazumi == null) {
            return;
        }
        // Stop AI so Kazumi stands still, but keep the entity alive and responsive
        kazumi.setNoAi(true);
        // Make Kazumi face the player
        kazumi.getLookControl().setLookAt(player);
        kazumi.yHeadRot = kazumi.getYRot();
        kazumi.yBodyRot = kazumi.getYRot();
    }

    public static void makeKazumiWalkToRandomSpot(ServerPlayer player, QuestRuntimeContext context) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        KazumiEntity kazumi = findNearestKazumi(serverLevel, player);
        if (kazumi == null) {
            return;
        }

        // Enable AI again
        kazumi.setNoAi(false);

        // Pick a random spot in the swamp village
        BlockPos center = getStoredStructureCenter(player);
        if (center == null) {
            center = findNearestStructure(serverLevel, player.blockPosition(), VILLAGE_SWAMP);
        }
        if (center == null) {
            return;
        }

        BlockPos targetPos = findRandomSurfacePosition(serverLevel, center, 30, 20);
        kazumi.getPersistentData().putInt("KnYKazumiTargetX", targetPos.getX());
        kazumi.getPersistentData().putInt("KnYKazumiTargetY", targetPos.getY());
        kazumi.getPersistentData().putInt("KnYKazumiTargetZ", targetPos.getZ());
        kazumi.getPersistentData().putBoolean("KnYKazumiWalking", true);
    }

    /**
     * Continuously updates Kazumi's pathfinding toward his target spot.
     * Also teleports Kazumi to the target if the player reaches it first.
     * Should be called every tick during the talk_to_kazumi step after dialog ends.
     */
    public static void tickKazumiPathing(ServerPlayer player, QuestRuntimeContext context) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        KazumiEntity kazumi = findNearestKazumi(serverLevel, player);
        if (kazumi == null) {
            return;
        }

        int tx = kazumi.getPersistentData().getInt("KnYKazumiTargetX");
        int ty = kazumi.getPersistentData().getInt("KnYKazumiTargetY");
        int tz = kazumi.getPersistentData().getInt("KnYKazumiTargetZ");
        if (tx == 0 && ty == 0 && tz == 0) {
            return; // No target set
        }

        BlockPos targetPos = new BlockPos(tx, ty, tz);

        // Check if player reached the target location first
        if (player.blockPosition().distSqr(targetPos) <= 100.0D) {
            // Teleport Kazumi to the target if he's not already close
            if (kazumi.blockPosition().distSqr(targetPos) > 100.0D) {
                kazumi.teleportToWithTicket(targetPos.getX() + 0.5D, targetPos.getY(), targetPos.getZ() + 0.5D);
            }
            resetKazumiSpeed(kazumi);
            kazumi.getPersistentData().putBoolean("KnYKazumiWalking", false);
            return;
        }

        // Continuously give Kazumi the pathfinding goal
        if (kazumi.getNavigation().isDone() || kazumi.getNavigation().isStuck()) {
            // Increase speed temporarily so he actually moves
            kazumi.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.45D);
            kazumi.getNavigation().moveTo(targetPos.getX() + 0.5D, targetPos.getY(), targetPos.getZ() + 0.5D, 1.2D);
        }
    }

    /**
     * Resets Kazumi's movement speed to normal. Should be called when he reaches the destination.
     */
    public static void resetKazumiSpeed(KazumiEntity kazumi) {
        if (kazumi != null && kazumi.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            kazumi.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.22D);
        }
    }

    /**
     * Returns the coordinates where Kazumi is walking to, formatted as "X ~ Z".
     * Returns null if no target is set.
     */
    public static String getKazumiTargetCoordinates(ServerPlayer player) {
        KazumiEntity kazumi = findNearestKazumi((ServerLevel) player.level(), player);
        if (kazumi == null) {
            return null;
        }
        int tx = kazumi.getPersistentData().getInt("KnYKazumiTargetX");
        int ty = kazumi.getPersistentData().getInt("KnYKazumiTargetY");
        int tz = kazumi.getPersistentData().getInt("KnYKazumiTargetZ");
        if (tx == 0 && ty == 0 && tz == 0) {
            return null;
        }
        return tx + " ~ " + tz;
    }

    public static boolean isKazumiWalking(ServerPlayer player) {
        KazumiEntity kazumi = findNearestKazumi((ServerLevel) player.level(), player);
        return kazumi != null && kazumi.getPersistentData().getBoolean("KnYKazumiWalking");
    }

    public static boolean isKazumiNearTarget(ServerPlayer player, double maxDistance) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        KazumiEntity kazumi = findNearestKazumi(serverLevel, player);
        if (kazumi == null) {
            return false;
        }
        int tx = kazumi.getPersistentData().getInt("KnYKazumiTargetX");
        int ty = kazumi.getPersistentData().getInt("KnYKazumiTargetY");
        int tz = kazumi.getPersistentData().getInt("KnYKazumiTargetZ");
        if (tx == 0 && ty == 0 && tz == 0) {
            return false;
        }
        BlockPos target = new BlockPos(tx, ty, tz);
        boolean isNear = player.blockPosition().distSqr(target) <= (maxDistance * maxDistance);
        if (isNear) {
            resetKazumiSpeed(kazumi);
            kazumi.getPersistentData().putBoolean("KnYKazumiWalking", false);
        }
        return isNear;
    }

    public static boolean isKazumiAlive(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        KazumiEntity kazumi = findNearestKazumi(serverLevel, player);
        return kazumi != null && kazumi.isAlive();
    }

    private static KazumiEntity findNearestKazumi(ServerLevel serverLevel, ServerPlayer player) {
        List<KazumiEntity> kazumis = serverLevel.getEntitiesOfClass(KazumiEntity.class,
            new net.minecraft.world.phys.AABB(player.blockPosition()).inflate(400.0D),
            entity -> "kazumi".equals(entity.getPersistentData().getString(QUEST_NPC_ID_TAG)));
        if (kazumis.isEmpty()) {
            return null;
        }
        return kazumis.stream().min(java.util.Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
    }

    /**
     * Queues delayed messages for a player. The messages will be delivered by the quest tick handler.
     * Format: stores message index and target tick in persistent data.
     */
    public static void sendDelayedMessages(ServerPlayer player, List<Component> messages, int delayTicks) {
        if (messages.isEmpty()) return;
        
        // Store messages as NBT
        net.minecraft.nbt.ListTag messagesTag = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < messages.size(); i++) {
            net.minecraft.nbt.CompoundTag msgTag = new net.minecraft.nbt.CompoundTag();
            msgTag.putString("text", Component.Serializer.toJson(messages.get(i)));
            msgTag.putLong("deliverAt", ((ServerLevel) player.level()).getGameTime() + (long) i * delayTicks);
            messagesTag.add(msgTag);
        }
        player.getPersistentData().put("KnYDelayedMessages", messagesTag);
        player.getPersistentData().putInt("KnYDelayedMessageIndex", 0);
        player.getPersistentData().putLong("KnYKazumiDialogStartTick", ((ServerLevel) player.level()).getGameTime());
    }

    /**
     * Processes any queued delayed messages for the player. Should be called from onTick.
     */
    public static void processDelayedMessages(ServerPlayer player) {
        if (!player.getPersistentData().contains("KnYDelayedMessages")) {
            return;
        }
        
        net.minecraft.nbt.ListTag messagesTag = player.getPersistentData().getList("KnYDelayedMessages", net.minecraft.nbt.Tag.TAG_COMPOUND);
        int index = player.getPersistentData().getInt("KnYDelayedMessageIndex");
        long currentTick = ((ServerLevel) player.level()).getGameTime();
        
        while (index < messagesTag.size()) {
            net.minecraft.nbt.CompoundTag msgTag = messagesTag.getCompound(index);
            long deliverAt = msgTag.getLong("deliverAt");
            if (currentTick >= deliverAt) {
                String json = msgTag.getString("text");
                Component msg = Component.Serializer.fromJson(json);
                if (msg != null) {
                    player.sendSystemMessage(msg);
                }
                index++;
            } else {
                break;
            }
        }
        
        player.getPersistentData().putInt("KnYDelayedMessageIndex", index);
        
        // Clean up if all messages delivered
        if (index >= messagesTag.size()) {
            player.getPersistentData().remove("KnYDelayedMessages");
            player.getPersistentData().remove("KnYDelayedMessageIndex");
        }
    }

    public static void ensureSwampDemonSpawned(ServerPlayer player, QuestRuntimeContext context) {
        if (!(player.level() instanceof ServerLevel serverLevel) || !serverLevel.isNight()) {
            return;
        }

        BlockPos center = getStoredStructureCenter(player);
        if (center == null) {
            center = findNearestStructure(serverLevel, player.blockPosition(), VILLAGE_SWAMP);
        }
        if (center == null) {
            return;
        }

        List<Entity> existing = serverLevel.getEntities((Entity) null,
            new net.minecraft.world.phys.AABB(center).inflate(128.0D),
            entity -> "swamp_demon_kidnappers_bog".equals(entity.getPersistentData().getString(QUEST_TARGET_ID_TAG)));
        if (!existing.isEmpty()) {
            return;
        }

        SwampDemonEntity entity = ModEntities.SWAMP_DEMON.get().create(serverLevel);
        if (entity == null) {
            return;
        }

        BlockPos spawnPos = findRandomSurfacePosition(serverLevel, center, 28, 16);
        entity.getPersistentData().putString(QUEST_TARGET_ID_TAG, "swamp_demon_kidnappers_bog");
        entity.setCustomName(Component.literal("Swamp Demon"));
        entity.setCustomNameVisible(true);
        entity.setPos(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D);
        serverLevel.addFreshEntity(entity);
    }

    /**
     * Spawns the quest-targeted swamp demon (Numa) near the player for the encounter step.
     * This demon wears Satoko's Bow and is the kill target for the quest.
     */
    public static void spawnSwampDemonEncounter(ServerPlayer player, QuestRuntimeContext context) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        // Check if already spawned
        List<Entity> existing = serverLevel.getEntities((Entity) null,
            new net.minecraft.world.phys.AABB(player.blockPosition()).inflate(100.0D),
            entity -> "swamp_demon_kidnappers_bog_satoko".equals(entity.getPersistentData().getString(QUEST_TARGET_ID_TAG)));
        if (!existing.isEmpty()) {
            return;
        }

        SwampDemonEntity demon = ModEntities.SWAMP_DEMON.get().create(serverLevel);
        if (demon == null) {
            return;
        }

        // Spawn within 50 blocks of the player
        BlockPos spawnPos = findRandomSurfacePosition(serverLevel, player.blockPosition(), 50, 16);
        demon.getPersistentData().putString(QUEST_TARGET_ID_TAG, "swamp_demon_kidnappers_bog_satoko");
        //demon.setCustomName(Component.literal("Numa, the Swamp Demon"));
        //demon.setCustomNameVisible(true);
        demon.setPos(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D);
        demon.setPersistenceRequired();
        demon.setTarget(player);
        demon.setHealth(demon.getMaxHealth());
        serverLevel.addFreshEntity(demon);
    }

    public static BlockPos findNearestQuestEntity(ServerPlayer player, String targetKey, double radius) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return null;
        }

        return serverLevel.getEntities((Entity) null,
                new net.minecraft.world.phys.AABB(player.blockPosition()).inflate(radius),
                entity -> targetKey.equals(entity.getPersistentData().getString(QUEST_NPC_ID_TAG)) ||
                    targetKey.equals(entity.getPersistentData().getString(QUEST_TARGET_ID_TAG)))
            .stream()
            .min(Comparator.comparingDouble(player::distanceToSqr))
            .map(Entity::blockPosition)
            .orElse(null);
    }

    public static BlockPos findNearestStructure(ServerLevel level, BlockPos origin, ResourceLocation structureId) {
        BlockPos nearest = level.findNearestMapStructure(QuestStructureTags.tagFor(structureId), origin, 100, false);
        if (nearest == null) {
            return null;
        }
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, nearest.getX(), nearest.getZ());
        return new BlockPos(nearest.getX(), y, nearest.getZ());
    }

    public static void sendKazumiDialogue(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("§6[Kazumi] §fSatoko disappeared last night... please help me find her."));
        player.sendSystemMessage(Component.literal("§6[Kazumi] §fStay close. I will show you where she vanished."));
    }

    public static void sendSwampDemonEncounterDialogue(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("§c[Numa] §fHeh... another one has come to be devoured..."));
        player.sendSystemMessage(Component.literal("§c[Numa] §fYou Demon Slayers are always so predictable..."));
    }

    public static void sendSwampDomainDialogue(ServerPlayer player) {
        sendDelayedMessages(player, List.of(
            Component.literal("§6[" + player.getName() + "] §fHey! You're wearing Satoko's bow in your hair!"),
            Component.literal("§c[Numa] §fA trophy from my last meal!"),
            Component.literal("§6[" + player.getName() + "] §fWhere is Satoko?"),
            Component.literal("§c[Numa] §fYou're too late! I've already eaten her!"),
            Component.literal("§c[Numa] §fI keep artifacts from all my victims! I think I'll keep your sword!"),
            Component.literal("§6[" + player.getName() + "] §fNot if I kill you first!"),
            Component.literal("§c[Numa] §fYou think you can defeat me in MY domain?!"),
            Component.literal("§c[Numa] §fI'll drag you into the swamp just like the others!")
        ), 50);
    }

    public static void sendSwampDemonLowHealthDialogue(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("§c[Numa] §fImpossible... how can a human... be this strong...?!"));
    }

    public static void sendKazumiReturnDialogue(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("§6[Kazumi] §fYou're back...! Did you find her?!"));
    }

    public static void markKidnappersBogActive(ServerPlayer player, QuestRuntimeContext context) {
        player.getPersistentData().putBoolean(KIDNAPPERS_BOG_ACTIVE_TAG, true);
        player.getPersistentData().putBoolean(SWAMP_DOMAIN_ENCOUNTER_STARTED_TAG, false);
    }

    public static void markSwampDomainEncounterStarted(ServerPlayer player, QuestRuntimeContext context) {
        if (!player.getPersistentData().getBoolean(SWAMP_DOMAIN_ENCOUNTER_STARTED_TAG)) {
            player.getPersistentData().putBoolean(SWAMP_DOMAIN_ENCOUNTER_STARTED_TAG, true);
            sendSwampDomainDialogue(player);
        }
    }

    public static void clearKidnappersBogFlags(ServerPlayer player, QuestRuntimeContext context) {
        player.getPersistentData().remove(KIDNAPPERS_BOG_ACTIVE_TAG);
        player.getPersistentData().remove(SWAMP_DOMAIN_ENCOUNTER_STARTED_TAG);
    }

    public static boolean hasSatokosBow(ServerPlayer player, QuestRuntimeContext context) {
        net.minecraft.world.item.Item bowItem = ForgeRegistries.ITEMS.getValue(SATOKOS_BOW);
        if (bowItem == null) {
            return false;
        }
        for (net.minecraft.world.item.ItemStack stack : player.getInventory().items) {
            if (stack.is(bowItem)) {
                return true;
            }
        }
        return false;
    }

    public static void giveSatokosBowToKazumi(ServerPlayer player, QuestRuntimeContext context) {
        net.minecraft.world.item.Item bowItem = ForgeRegistries.ITEMS.getValue(SATOKOS_BOW);
        if (bowItem == null) {
            return;
        }
        // Remove one Satoko's Bow from player inventory
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(bowItem)) {
                stack.shrink(1);
                player.getInventory().setChanged();
                break;
            }
        }
        player.sendSystemMessage(Component.literal("§6[Kazumi] §f...That's... Satoko's..."));
        player.sendSystemMessage(Component.literal("§6[Kazumi] §f...She's gone... isn't she...?"));
        player.sendSystemMessage(Component.literal("§6[Kazumi] §fThank you... for finding the truth..."));
    }

    private static BlockPos findRandomSurfacePosition(ServerLevel level, BlockPos center, int radius, int attempts) {
        BlockPos fallback = center;
        for (int i = 0; i < attempts; i++) {
            int x = center.getX() + level.random.nextInt(radius * 2 + 1) - radius;
            int z = center.getZ() + level.random.nextInt(radius * 2 + 1) - radius;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (level.getBlockState(pos.below()).isSolid() && level.getBlockState(pos).isAir()) {
                return pos;
            }
            fallback = pos;
        }
        return fallback;
    }
}
