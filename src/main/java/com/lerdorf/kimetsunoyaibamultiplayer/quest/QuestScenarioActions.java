package com.lerdorf.kimetsunoyaibamultiplayer.quest;

import java.util.Comparator;
import java.util.List;

import com.lerdorf.kimetsunoyaibamultiplayer.entities.KazumiEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.DemonVillagerEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.SwampDemonEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.raids.StructureLocationCache;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TrapDoorBlock;
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
    public static final String TAMAYO_HOUSE_X = "KnYTamayoHouseX";
    public static final String TAMAYO_HOUSE_Y = "KnYTamayoHouseY";
    public static final String TAMAYO_HOUSE_Z = "KnYTamayoHouseZ";
    public static final String TAMAYO_HOUSE_ROTATION = "KnYTamayoHouseRotation";
    private static final int TAMAYO_HOUSE_Y_OFFSET = 6;

    private static final ResourceLocation VILLAGE_SWAMP = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "village_swamp");
    private static final ResourceLocation TAMAYO_HOUSE = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "house_tamayo");
    private static final ResourceLocation TAMAYO_ID = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "tamayo");
    private static final ResourceLocation YUSHIRO_ID = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "yushiro");
    private static final ResourceLocation SUSAMARU_ID = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "susamaru");
    private static final ResourceLocation YAHABA_ID = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "yahaba");
    private static final ResourceLocation SWAMP_DEMON_ID = ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "swamp_demon");
    private static final ResourceLocation SATOKOS_BOW = ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "satokos_bow");
    private static final double TAMAYO_RESTRAINED_DEMON_MAX_RADIUS = 10.0D;

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
        String playerName = player.getName().getString();
        sendDelayedMessages(player, List.of(
            Component.literal("§6[" + playerName + "] §fHey! You're wearing Satoko's bow in your hair!"),
            Component.literal("§c[Numa] §fA trophy from my last meal!"),
            Component.literal("§6[" + playerName + "] §fWhere is Satoko?"),
            Component.literal("§c[Numa] §fYou're too late! I've already eaten her!"),
            Component.literal("§c[Numa] §fI keep artifacts from all my victims! I think I'll keep your sword!"),
            Component.literal("§6[" + playerName + "] §fNot if I kill you first!"),
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

    public static boolean isSatokosBow(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        net.minecraft.world.item.Item bowItem = ForgeRegistries.ITEMS.getValue(SATOKOS_BOW);
        return bowItem != null && stack.is(bowItem);
    }

    public static boolean isSwampDemonAliveNear(ServerPlayer player, Entity center, double radius) {
        if (!(player.level() instanceof ServerLevel serverLevel) || center == null) {
            return false;
        }
        net.minecraft.world.phys.AABB searchArea = new net.minecraft.world.phys.AABB(center.blockPosition()).inflate(radius);
        List<Entity> demons = serverLevel.getEntities((Entity) null, searchArea,
            entity -> entity instanceof SwampDemonEntity && entity.isAlive());
        return !demons.isEmpty();
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

    public static void storeTamayoHouseContext(ServerPlayer player, QuestRuntimeContext context) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos structurePos = findNearestStructureCorner(serverLevel, player.blockPosition(), TAMAYO_HOUSE);
        if (structurePos == null) {
            return;
        }

        BlockPos anchor = toTamayoHouseAnchor(structurePos);
        player.getPersistentData().putInt(TAMAYO_HOUSE_X, anchor.getX());
        player.getPersistentData().putInt(TAMAYO_HOUSE_Y, anchor.getY());
        player.getPersistentData().putInt(TAMAYO_HOUSE_Z, anchor.getZ());
        player.getPersistentData().putInt(TAMAYO_HOUSE_ROTATION, inferTamayoHouseRotation(serverLevel, anchor));
    }

    public static boolean repairStoredTamayoHouse(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        BlockPos center = getStoredTamayoHouseCenter(player);
        if (center == null) {
            return false;
        }
        int rotation = player.getPersistentData().getInt(TAMAYO_HOUSE_ROTATION);
        return placeTamayoHouse(serverLevel, center, rotation);
    }

    public static boolean repairNearestTamayoHouse(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        BlockPos structurePos = findNearestStructureCorner(serverLevel, player.blockPosition(), TAMAYO_HOUSE);
        if (structurePos == null) {
            return false;
        }
        BlockPos center = toTamayoHouseAnchor(structurePos);
        int rotation = inferTamayoHouseRotation(serverLevel, center);
        player.getPersistentData().putInt(TAMAYO_HOUSE_X, center.getX());
        player.getPersistentData().putInt(TAMAYO_HOUSE_Y, center.getY());
        player.getPersistentData().putInt(TAMAYO_HOUSE_Z, center.getZ());
        player.getPersistentData().putInt(TAMAYO_HOUSE_ROTATION, rotation);
        return placeTamayoHouse(serverLevel, center, rotation);
    }

    public static BlockPos getTamayoHousePoint(ServerPlayer player, TamayoHousePoint point) {
        BlockPos center = getStoredTamayoHouseCenter(player);
        if (center == null) {
            if (player.level() instanceof ServerLevel serverLevel) {
                BlockPos structurePos = findNearestStructureCorner(serverLevel, player.blockPosition(), TAMAYO_HOUSE);
                if (structurePos != null) {
                    center = toTamayoHouseAnchor(structurePos);
                    player.getPersistentData().putInt(TAMAYO_HOUSE_X, center.getX());
                    player.getPersistentData().putInt(TAMAYO_HOUSE_Y, center.getY());
                    player.getPersistentData().putInt(TAMAYO_HOUSE_Z, center.getZ());
                    player.getPersistentData().putInt(TAMAYO_HOUSE_ROTATION, inferTamayoHouseRotation(serverLevel, center));
                }
            }
        }
        if (center == null) {
            return null;
        }

        int rotation = player.getPersistentData().getInt(TAMAYO_HOUSE_ROTATION);
        int[] rotated = rotateOffset(point.offsetX, point.offsetZ, rotation);
        return new BlockPos(center.getX() + rotated[0], center.getY() + point.offsetY, center.getZ() + rotated[1]);
    }

    public static void ensureTamayoAndYushiroAtReception(ServerPlayer player, QuestRuntimeContext context) {
        ensureTamayoHouseNpc(player, "tamayo", TAMAYO_ID, getTamayoHousePoint(player, TamayoHousePoint.RECEPTION));
        ensureTamayoHouseNpc(player, "yushiro", YUSHIRO_ID, getTamayoHousePoint(player, TamayoHousePoint.RECEPTION));
        moveTamayoHouseNpc(player, "tamayo", getTamayoHousePoint(player, TamayoHousePoint.RECEPTION), "KnYTamayoReceptionStartTick", 6.0D);
        moveTamayoHouseNpc(player, "yushiro", getTamayoHousePoint(player, TamayoHousePoint.RECEPTION), "KnYYushiroReceptionStartTick", 6.0D);
    }

    public static boolean isTamayoReceptionReady(ServerPlayer player, QuestRuntimeContext context) {
        BlockPos target = getTamayoHousePoint(player, TamayoHousePoint.RECEPTION);
        return target != null
            && isPlayerNear(player, target, 6.0D)
            && isQuestNpcNear(player, "tamayo", target, 6.0D)
            && isQuestNpcNear(player, "yushiro", target, 6.0D);
    }

    public static void sendTamayoReceptionDialogue(ServerPlayer player, QuestRuntimeContext context) {
        sendDelayedMessages(player, List.of(
            Component.literal("§5[Yushiro] §f...A Demon Slayer?"),
            Component.literal("§5[Yushiro] §fWhy did Lady Tamayo allow someone like you here?"),
            Component.literal("§5[Yushiro] §fIf you try anything suspicious, I'll kill you myself."),
            Component.literal("§d[Tamayo] §fPlease forgive Yushiro. He is... overly protective."),
            Component.literal("§d[Tamayo] §fWelcome. I am Tamayo."),
            Component.literal("§d[Tamayo] §fYou are likely confused to see a demon who does not attack humans."),
            Component.literal("§d[Tamayo] §fCome see my work in the basement.")
        ), 55);
    }

    public static void tickTamayoBasementBriefing(ServerPlayer player, QuestRuntimeContext context) {
        ensureRestrainedDemonVillager(player, context);
        BlockPos target = getTamayoHousePoint(player, TamayoHousePoint.BASEMENT);
        moveTamayoHouseNpc(player, "tamayo", target, "KnYTamayoBasementStartTick", 3.0D);
        moveTamayoHouseNpc(player, "yushiro", target, "KnYYushiroBasementStartTick", 3.0D);
    }

    public static boolean isTamayoBasementReady(ServerPlayer player, QuestRuntimeContext context) {
        BlockPos target = getTamayoHousePoint(player, TamayoHousePoint.BASEMENT);
        return target != null
            && isPlayerNear(player, target, 3.0D)
            && isQuestNpcNear(player, "tamayo", target, 3.0D)
            && isQuestNpcNear(player, "yushiro", target, 3.0D);
    }

    public static void sendTamayoBasementDialogue(ServerPlayer player, QuestRuntimeContext context) {
        player.getPersistentData().putBoolean("KnYDoctorsRequestUnlocked", true);
        sendDelayedMessages(player, List.of(
            Component.literal("§d[Tamayo] §fNot all demons desire violence."),
            Component.literal("§d[Tamayo] §fSome of us struggle endlessly against Muzan Kibutsuji's influence."),
            Component.literal("§d[Tamayo] §fI have devoted myself to studying demon blood."),
            Component.literal("§d[Tamayo] §fIf a cure exists... it may be possible to save both humans and demons alike."),
            Component.literal("§5[Yushiro] §fLady Tamayo has spent centuries researching this."),
            Component.literal("§5[Yushiro] §fUnlike the Demon Slayers, she actually seeks a permanent solution."),
            Component.literal("§d[Tamayo] §fHowever... ordinary demon blood is not enough."),
            Component.literal("§d[Tamayo] §fThe Twelve Kizuki possess blood far closer to Muzan's own."),
            Component.literal("§d[Tamayo] §fIf samples from the Kizuki can be obtained... my research may finally progress."),
            Component.literal("§bSide Quest Unlocked: §fDoctor's Request")
        ), 55);
    }

    public static void sendTamayoAmbushPrelude(ServerPlayer player, QuestRuntimeContext context) {
        sendDelayedMessages(player, List.of(
            Component.literal("§d[Tamayo] §f...Wait."),
            Component.literal("§5[Yushiro] §f...Someone's here."),
            Component.literal("§4A sinister presence approaches...")
        ), 45);
    }

    public static void tickTamayoReturnToReception(ServerPlayer player, QuestRuntimeContext context) {
        BlockPos target = getTamayoHousePoint(player, TamayoHousePoint.RECEPTION);
        moveTamayoHouseNpc(player, "tamayo", target, "KnYTamayoReturnStartTick", 3.0D);
        moveTamayoHouseNpc(player, "yushiro", target, "KnYYushiroReturnStartTick", 3.0D);
    }

    public static boolean isTamayoReturnReady(ServerPlayer player, QuestRuntimeContext context) {
        BlockPos target = getTamayoHousePoint(player, TamayoHousePoint.RECEPTION);
        return target != null
            && isPlayerNear(player, target, 3.0D)
            && isQuestNpcNear(player, "tamayo", target, 3.0D)
            && isQuestNpcNear(player, "yushiro", target, 3.0D);
    }

    public static void startSusamaruYahabaAttack(ServerPlayer player, QuestRuntimeContext context) {
        ensureTamayoAndYushiroAtReception(player, context);
        player.getPersistentData().putBoolean("KnYSusamaruKilled", false);
        player.getPersistentData().putBoolean("KnYYahabaKilled", false);
        spawnSusamaruAndYahaba(player, context);
        createTamayoHouseExplosion(player);
        sendDelayedMessages(player, List.of(
            Component.literal("§c[Susamaru] §fFound yooou!"),
            Component.literal("§c[Susamaru] §fMuzan-sama said there was a traitor hiding here!"),
            Component.literal("§c[Susamaru] §fCan I rip them apart now?!"),
            Component.literal("§d[Tamayo] §f...So Muzan has found us after all.")
        ), 45);
    }

    public static void tickSusamaruYahabaAttack(ServerPlayer player, QuestRuntimeContext context) {
        spawnSusamaruAndYahaba(player, context);
        if (!isTamayoAliveNearHouse(player)) {
            player.sendSystemMessage(Component.literal("§cQuest Failed: §fTamayo was slain. Return to Tamayo's House and try again."));
            resetTamayoHouseFailure(player);
            resetSusamaruYahabaAttack(player, context);
            QuestProgressionManager.scheduleCurrentStepRestart(player, 100);
        }
    }

    public static void markTamayoHouseTargetKilled(ServerPlayer player, LivingEntity victim) {
        String targetKey = victim.getPersistentData().getString(QUEST_TARGET_ID_TAG);
        if ("susamaru_asakusa".equals(targetKey)) {
            player.getPersistentData().putBoolean("KnYSusamaruKilled", true);
        } else if ("yahaba_asakusa".equals(targetKey)) {
            player.getPersistentData().putBoolean("KnYYahabaKilled", true);
        }
    }

    public static boolean areSusamaruAndYahabaDefeated(ServerPlayer player, QuestRuntimeContext context) {
        return player.getPersistentData().getBoolean("KnYSusamaruKilled")
            && player.getPersistentData().getBoolean("KnYYahabaKilled");
    }

    public static void sendTamayoHouseVictoryDialogue(ServerPlayer player, QuestRuntimeContext context) {
        player.getPersistentData().remove("KnYSusamaruKilled");
        player.getPersistentData().remove("KnYYahabaKilled");
        sendDelayedMessages(player, List.of(
            Component.literal("§d[Tamayo] §fYour strength may prove vital in the battles to come."),
            Component.literal("§5[Yushiro] §fAt least you weren't completely useless."),
            Component.literal("§d[Tamayo] §fPlease remember what you learned here today."),
            Component.literal("§d[Tamayo] §fAnd if my research succeeds... perhaps this endless tragedy can finally end.")
        ), 55);
    }

    public static BlockPos findNearestTamayoHouseEnemy(ServerPlayer player, QuestRuntimeContext context) {
        BlockPos susamaru = findNearestQuestEntity(player, "susamaru_asakusa", 256.0D);
        BlockPos yahaba = findNearestQuestEntity(player, "yahaba_asakusa", 256.0D);
        if (susamaru == null) {
            return yahaba;
        }
        if (yahaba == null) {
            return susamaru;
        }
        return player.blockPosition().distSqr(susamaru) <= player.blockPosition().distSqr(yahaba) ? susamaru : yahaba;
    }

    public static void resetTamayoHouseFailure(ServerPlayer player) {
        player.getPersistentData().remove("KnYSusamaruKilled");
        player.getPersistentData().remove("KnYYahabaKilled");
        player.getPersistentData().remove("KnYTamayoReceptionStartTick");
        player.getPersistentData().remove("KnYYushiroReceptionStartTick");
        player.getPersistentData().remove("KnYTamayoBasementStartTick");
        player.getPersistentData().remove("KnYYushiroBasementStartTick");
        player.getPersistentData().remove("KnYTamayoReturnStartTick");
        player.getPersistentData().remove("KnYYushiroReturnStartTick");
        player.getPersistentData().remove("KnYTamayoHouseExplosionDone");
    }

    public enum TamayoHousePoint {
        RECEPTION(0, 1, 0),
        BASEMENT(2, -4, -1),
        DEMON_VILLAGER(-1, -4, -1),
        ATTACK_SPAWN(-18, 0, -3),
        EXPLOSION(-6, 2, 4),
        TRAPDOOR(5, 1, -4);

        private final int offsetX;
        private final int offsetY;
        private final int offsetZ;

        TamayoHousePoint(int offsetX, int offsetY, int offsetZ) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetZ = offsetZ;
        }
    }

    private static BlockPos getStoredTamayoHouseCenter(ServerPlayer player) {
        if (!player.getPersistentData().contains(TAMAYO_HOUSE_X)) {
            return null;
        }
        return new BlockPos(
            player.getPersistentData().getInt(TAMAYO_HOUSE_X),
            player.getPersistentData().getInt(TAMAYO_HOUSE_Y),
            player.getPersistentData().getInt(TAMAYO_HOUSE_Z)
        );
    }

    private static void ensureTamayoHouseNpc(ServerPlayer player, String npcKey, ResourceLocation entityTypeId, BlockPos fallbackPos) {
        if (!(player.level() instanceof ServerLevel serverLevel) || fallbackPos == null) {
            return;
        }

        BlockPos center = getStoredTamayoHouseCenter(player);
        if (center == null) {
            center = fallbackPos;
        }

        Entity existing = findNearestEntityByQuestKeyOrType(serverLevel, center, npcKey, entityTypeId, 100.0D);
        if (existing != null) {
            existing.getPersistentData().putString(QUEST_NPC_ID_TAG, npcKey);
            makePersistent(existing);
            return;
        }

        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(entityTypeId).orElse(null);
        if (entityType == null) {
            return;
        }
        Entity entity = entityType.create(serverLevel);
        if (entity == null) {
            return;
        }

        entity.getPersistentData().putString(QUEST_NPC_ID_TAG, npcKey);
        entity.setPos(fallbackPos.getX() + 0.5D, fallbackPos.getY(), fallbackPos.getZ() + 0.5D);
        makePersistent(entity);
        serverLevel.addFreshEntity(entity);
    }

    private static void ensureRestrainedDemonVillager(ServerPlayer player, QuestRuntimeContext context) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockPos pos = getTamayoHousePoint(player, TamayoHousePoint.DEMON_VILLAGER);
        if (pos == null) {
            return;
        }
        List<DemonVillagerEntity> existing = serverLevel.getEntitiesOfClass(
            DemonVillagerEntity.class,
            new net.minecraft.world.phys.AABB(pos).inflate(TAMAYO_RESTRAINED_DEMON_MAX_RADIUS),
            LivingEntity::isAlive);
        if (!existing.isEmpty()) {
            return;
        }

        Entity entity = ModEntities.DEMON_VILLAGER.get().create(serverLevel);
        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        entity.getPersistentData().putString(QUEST_NPC_ID_TAG, "tamayo_restrained_demon");
        entity.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        makePersistent(entity);
        living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Integer.MAX_VALUE, 9, false, false, false));
        living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, Integer.MAX_VALUE, 9, false, false, false));
        if (living instanceof Mob mob) {
            mob.setTarget(null);
        }
        serverLevel.addFreshEntity(entity);
    }

    private static void moveTamayoHouseNpc(ServerPlayer player, String npcKey, BlockPos target, String startTickKey, double teleportDistance) {
        if (!(player.level() instanceof ServerLevel serverLevel) || target == null) {
            return;
        }
        Entity npc = findNearestQuestNpc(serverLevel, player.blockPosition(), npcKey, 160.0D);
        if (npc == null) {
            return;
        }

        if (!player.getPersistentData().contains(startTickKey)) {
            player.getPersistentData().putLong(startTickKey, serverLevel.getGameTime());
        }

        double maxDistanceSqr = teleportDistance * teleportDistance;
        if (npc.blockPosition().distSqr(target) <= maxDistanceSqr) {
            return;
        }

        long startedAt = player.getPersistentData().getLong(startTickKey);
        if (serverLevel.getGameTime() >= startedAt + 600L) {
            npc.teleportTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D);
            return;
        }

        if (npc instanceof Mob mob) {
            mob.setNoAi(false);
            mob.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, 1.15D);
        }
    }

    private static boolean isQuestNpcNear(ServerPlayer player, String npcKey, BlockPos target, double radius) {
        if (!(player.level() instanceof ServerLevel serverLevel) || target == null) {
            return false;
        }
        Entity npc = findNearestQuestNpc(serverLevel, target, npcKey, radius);
        return npc != null && npc.isAlive();
    }

    private static boolean isPlayerNear(ServerPlayer player, BlockPos target, double radius) {
        return target != null && player.blockPosition().distSqr(target) <= radius * radius;
    }

    private static Entity findNearestQuestNpc(ServerLevel serverLevel, BlockPos origin, String npcKey, double radius) {
        return serverLevel.getEntities((Entity) null,
                new net.minecraft.world.phys.AABB(origin).inflate(radius),
                entity -> npcKey.equals(entity.getPersistentData().getString(QUEST_NPC_ID_TAG)))
            .stream()
            .min(Comparator.comparingDouble(entity -> entity.blockPosition().distSqr(origin)))
            .orElse(null);
    }

    private static Entity findNearestEntityByQuestKeyOrType(ServerLevel serverLevel, BlockPos origin, String npcKey,
                                                            ResourceLocation entityTypeId, double radius) {
        return serverLevel.getEntities((Entity) null,
                new net.minecraft.world.phys.AABB(origin).inflate(radius),
                entity -> entity.isAlive()
                    && (npcKey.equals(entity.getPersistentData().getString(QUEST_NPC_ID_TAG))
                    || entityTypeId.equals(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType())))
                )
            .stream()
            .min(Comparator.comparingDouble(entity -> entity.blockPosition().distSqr(origin)))
            .orElse(null);
    }

    private static void makePersistent(Entity entity) {
        if (entity instanceof Mob mob) {
            mob.setPersistenceRequired();
        }
    }

    private static void spawnSusamaruAndYahaba(ServerPlayer player, QuestRuntimeContext context) {
        BlockPos spawnPos = getTamayoHousePoint(player, TamayoHousePoint.ATTACK_SPAWN);
        if (!(player.level() instanceof ServerLevel serverLevel) || spawnPos == null) {
            return;
        }
        if (!player.getPersistentData().getBoolean("KnYSusamaruKilled")) {
            spawnQuestEnemy(serverLevel, player, SUSAMARU_ID, "susamaru_asakusa", spawnPos);
        }
        if (!player.getPersistentData().getBoolean("KnYYahabaKilled")) {
            spawnQuestEnemy(serverLevel, player, YAHABA_ID, "yahaba_asakusa", spawnPos.offset(2, 0, 2));
        }
    }

    private static void spawnQuestEnemy(ServerLevel serverLevel, ServerPlayer player, ResourceLocation entityTypeId,
                                        String targetKey, BlockPos spawnPos) {
        if (findNearestQuestTarget(serverLevel, spawnPos, targetKey, 128.0D) != null) {
            return;
        }

        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(entityTypeId).orElse(null);
        if (entityType == null) {
            return;
        }
        Entity entity = entityType.create(serverLevel);
        if (entity == null) {
            return;
        }

        entity.getPersistentData().putString(QUEST_TARGET_ID_TAG, targetKey);
        entity.setPos(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D);
        makePersistent(entity);
        if (entity instanceof Mob mob) {
            mob.setTarget(player);
        }
        serverLevel.addFreshEntity(entity);
    }

    private static Entity findNearestQuestTarget(ServerLevel serverLevel, BlockPos origin, String targetKey, double radius) {
        return serverLevel.getEntities((Entity) null,
                new net.minecraft.world.phys.AABB(origin).inflate(radius),
                entity -> targetKey.equals(entity.getPersistentData().getString(QUEST_TARGET_ID_TAG)) && entity.isAlive())
            .stream()
            .min(Comparator.comparingDouble(entity -> entity.blockPosition().distSqr(origin)))
            .orElse(null);
    }

    private static void createTamayoHouseExplosion(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)
            || player.getPersistentData().getBoolean("KnYTamayoHouseExplosionDone")) {
            return;
        }
        BlockPos pos = getTamayoHousePoint(player, TamayoHousePoint.EXPLOSION);
        if (pos == null) {
            return;
        }
        player.getPersistentData().putBoolean("KnYTamayoHouseExplosionDone", true);
        serverLevel.explode(null, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 3.0F, Level.ExplosionInteraction.TNT);
    }

    private static boolean isTamayoAliveNearHouse(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        BlockPos center = getStoredTamayoHouseCenter(player);
        if (center == null) {
            center = player.blockPosition();
        }
        Entity tamayo = findNearestEntityByQuestKeyOrType(serverLevel, center, "tamayo", TAMAYO_ID, 160.0D);
        return tamayo != null && tamayo.isAlive();
    }

    private static void resetSusamaruYahabaAttack(ServerPlayer player, QuestRuntimeContext context) {
        player.getPersistentData().putBoolean("KnYSusamaruKilled", false);
        player.getPersistentData().putBoolean("KnYYahabaKilled", false);
        player.getPersistentData().putBoolean("KnYTamayoHouseExplosionDone", false);
        clearAsakusaAttackers(player);
    }

    private static void clearAsakusaAttackers(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockPos center = getStoredTamayoHouseCenter(player);
        if (center == null) {
            center = player.blockPosition();
        }
        List<Entity> attackers = serverLevel.getEntities((Entity) null,
            new net.minecraft.world.phys.AABB(center).inflate(200.0D),
            entity -> "susamaru_asakusa".equals(entity.getPersistentData().getString(QUEST_TARGET_ID_TAG))
                || "yahaba_asakusa".equals(entity.getPersistentData().getString(QUEST_TARGET_ID_TAG)));
        for (Entity attacker : attackers) {
            attacker.discard();
        }
    }

    private static BlockPos findNearestStructureCorner(ServerLevel level, BlockPos origin, ResourceLocation structureId) {
        return level.findNearestMapStructure(QuestStructureTags.tagFor(structureId), origin, 100, false);
    }

    private static BlockPos toTamayoHouseAnchor(BlockPos structurePos) {
        return new BlockPos(structurePos.getX(), structurePos.getY() + TAMAYO_HOUSE_Y_OFFSET, structurePos.getZ());
    }

    private static int inferTamayoHouseRotation(ServerLevel serverLevel, BlockPos center) {
        for (int rotation = 0; rotation < 4; rotation++) {
            int[] trapdoorOffset = rotateOffset(TamayoHousePoint.TRAPDOOR.offsetX, TamayoHousePoint.TRAPDOOR.offsetZ, rotation);
            BlockPos trapdoorPos = new BlockPos(
                center.getX() + trapdoorOffset[0],
                center.getY() + TamayoHousePoint.TRAPDOOR.offsetY,
                center.getZ() + trapdoorOffset[1]
            );
            if (serverLevel.getBlockState(trapdoorPos).getBlock() instanceof TrapDoorBlock
                || serverLevel.getBlockState(trapdoorPos.below()).getBlock() instanceof TrapDoorBlock) {
                return rotation;
            }
        }
        return 0;
    }

    private static boolean placeTamayoHouse(ServerLevel level, BlockPos center, int rotation) {
        CommandSourceStack source = level.getServer()
            .createCommandSourceStack()
            .withPermission(4)
            .withSuppressedOutput();
        String rotationArg = switch (Math.floorMod(rotation, 4)) {
            case 1 -> "clockwise_90";
            case 2 -> "clockwise_180";
            case 3 -> "counterclockwise_90";
            default -> "none";
        };
        String cmd = "place structure " + TAMAYO_HOUSE + " "
            + center.getX() + " " + center.getY() + " " + center.getZ()
            + " " + rotationArg;
        int result = level.getServer().getCommands().performPrefixedCommand(source, cmd);
        return result > 0;
    }

    private static int[] rotateOffset(int x, int z, int rotation) {
        return switch (Math.floorMod(rotation, 4)) {
            case 1 -> new int[] { -z, x };
            case 2 -> new int[] { -x, -z };
            case 3 -> new int[] { z, -x };
            default -> new int[] { x, z };
        };
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
