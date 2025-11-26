package com.lerdorf.kimetsunoyaibamultiplayer.raids;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.config.RaidConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;

import java.util.*;

/**
 * Complete raid instance with difficulty levels, boss bar, and health-based progress.
 */
public class KnYRaid {

    public enum RaidType { DEMON, SLAYER }
    public enum RaidState { PREPARING, ACTIVE, VICTORY, DEFEAT }

    private final UUID raidId;
    private final ServerLevel level;
    private final ResourceLocation structureId;
    private final BlockPos center;
    private final RaidType type;
    private final int difficultyLevel;

    private RaidState state = RaidState.PREPARING;
    private int currentWave = 0;
    private int totalWaves;
    private long nextActionTime = 0L;
    private long raidStartTime;
    private long lastPlayerNearbyTime;

    // Entity tracking
    private final Set<UUID> allRaidEntities = new HashSet<>();
    private final Set<UUID> aliveEntities = new HashSet<>();
    private final Map<UUID, Float> entityMaxHealth = new HashMap<>();
    private float totalMaxHealth = 0f;
    private float currentHealth = 0f;

    // Pending spawns for current wave
    private final Deque<ResourceLocation> pendingSpawns = new ArrayDeque<>();

    // Boss bar
    private final ServerBossEvent bossBar;
    private final Set<UUID> participants = new HashSet<>();

    // Warning state for abandonment
    private boolean abandonmentWarningShown = false;
    private long abandonmentWarningTime = 0;

    // Wave timer for glowing effect (5 minutes = 6000 ticks)
    private long waveStartTime = 0;
    private boolean glowingApplied = false;
    private static final long GLOWING_TIMER = 6000L; // 5 minutes in ticks

    // Cleanup tracking
    private boolean cleanupDone = false;

    // Clumped spawning - track last spawn location to create groups
    private BlockPos lastSpawnLocation = null;
    private int spawnClumpRemaining = 0;

    // Periodic demon scanning (for demons that spawn other demons or have phases)
    private long lastDemonScanTime = 0;
    private static final long DEMON_SCAN_INTERVAL = 100L; // Scan every 5 seconds

    public KnYRaid(ServerLevel level, ResourceLocation structureId, BlockPos center, RaidType type, int difficultyLevel) {
        this.raidId = UUID.randomUUID();
        this.level = level;
        this.structureId = structureId;
        this.center = center;
        this.type = type;
        this.difficultyLevel = Math.max(1, Math.min(5, difficultyLevel));
        this.raidStartTime = level.getGameTime();
        this.lastPlayerNearbyTime = raidStartTime;

        // Calculate total waves based on difficulty
        this.totalWaves = switch (difficultyLevel) {
            case 1 -> 3;
            case 2 -> 4;
            case 3 -> 5;
            case 4 -> 6;
            case 5 -> 7;
            default -> 3;
        };

        // Create boss bar
        String raidName = (type == RaidType.DEMON ? "Demon Raid" : "Demon Slayer Raid");
        BossEvent.BossBarColor color = type == RaidType.DEMON ? BossEvent.BossBarColor.RED : BossEvent.BossBarColor.BLUE;
        this.bossBar = new ServerBossEvent(
            Component.literal(raidName + " " + toRoman(difficultyLevel) + " : Wave 1"),
            color,
            BossEvent.BossBarOverlay.PROGRESS
        );
        bossBar.setProgress(0.0f);

        // Play raid horn
        level.playSound(null, center.getX(), center.getY(), center.getZ(), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 128.0f, 1.0f);

        // Notify players
        broadcastMessage(Component.literal(raidName + " " + toRoman(difficultyLevel) + " is beginning!").withStyle(style ->
            style.withColor(type == RaidType.DEMON ? 0xFF5555 : 0x5555FF)));

        // Schedule first wave
        scheduleNextWave();
    }

    public void tick() {
        long gameTime = level.getGameTime();

        // Update player tracking
        updateBossBarPlayers();

        // Only check abandonment and timeout during active raid phases
        if (state == RaidState.PREPARING || state == RaidState.ACTIVE) {
            // Check for abandonment
            if (!checkPlayerNearby()) {
                if (!abandonmentWarningShown) {
                    abandonmentWarningShown = true;
                    abandonmentWarningTime = gameTime;
                    broadcastMessage(Component.literal("Return to the raid area within 10 seconds or the raid will be lost!").withStyle(style ->
                        style.withColor(0xFFAA00)));
                } else if (gameTime - abandonmentWarningTime > 200) { // 10 seconds
                    triggerDefeat("Players abandoned the raid");
                    return;
                }
            } else {
                if (abandonmentWarningShown) {
                    abandonmentWarningShown = false;
                    broadcastMessage(Component.literal("Player returned to raid area").withStyle(style ->
                        style.withColor(0x55FF55)));
                }
                lastPlayerNearbyTime = gameTime;
            }

            // Check timeout
            if (gameTime - raidStartTime > RaidConfig.raidTimeout.get() * 20L) {
                triggerDefeat("Raid timed out");
                return;
            }
        }

        // State-specific handling
        switch (state) {
            case PREPARING -> tickPreparing(gameTime);
            case ACTIVE -> tickActive(gameTime);
            case VICTORY, DEFEAT -> {
                // Clean up after short delay
                if (gameTime > nextActionTime) {
                    cleanup();
                }
            }
        }
    }

    private void tickPreparing(long gameTime) {
        if (gameTime < nextActionTime) {
            // Update boss bar with countdown
            long remaining = nextActionTime - gameTime;
            int seconds = (int) (remaining / 20);
            bossBar.setProgress(1.0f - (float) remaining / (RaidConfig.wavePreparationTime.get() * 20));
            updateBossBarTitle("Preparing - Wave " + currentWave + " in " + seconds + "s");
            return;
        }

        // Start spawning
        state = RaidState.ACTIVE;
        waveStartTime = gameTime; // Reset wave timer for glowing
        glowingApplied = false;
        updateBossBarTitle("Wave " + currentWave);
        spawnNextEntity(gameTime);
    }

    private void tickActive(long gameTime) {
        // Spawn pending entities
        if (!pendingSpawns.isEmpty() && gameTime >= nextActionTime) {
            spawnNextEntity(gameTime);
            return;
        }

        // Update health-based progress
        updateHealthProgress();

        // Periodically scan for new demons (for demon raids only)
        // This handles cases where demons spawn other demons or have phase changes
        if (type == RaidType.DEMON && gameTime - lastDemonScanTime >= DEMON_SCAN_INTERVAL) {
            scanForNewDemons();
            lastDemonScanTime = gameTime;
        }

        // Apply glowing effect after 5 minutes if wave is still ongoing
        if (!aliveEntities.isEmpty() && gameTime - waveStartTime >= GLOWING_TIMER) {
            boolean anyApplied = applyGlowingToAllRaidEntities();
            if (!glowingApplied && anyApplied) {
                glowingApplied = true;
                broadcastMessage(Component.literal("Remaining raid entities are now glowing!").withStyle(style ->
                    style.withColor(0xFFFF55)));
            }
        }

        // Check if wave is complete
        if (aliveEntities.isEmpty() && pendingSpawns.isEmpty()) {
            if (currentWave >= totalWaves) {
                // Victory!
                triggerVictory();
            } else {
                // Next wave
                scheduleNextWave();
            }
        }
    }

    /**
     * Scan for new demons in the raid area (for demon raids only).
     * This handles demons that spawn other demons or have multiple phases.
     */
    private void scanForNewDemons() {
        int radius = CivilianStructureRegistry.getSpawnRadius(structureId);
        if (radius <= 0) radius = 64;

        net.minecraft.world.phys.AABB searchBox = new net.minecraft.world.phys.AABB(
            center.getX() - radius, center.getY() - 32, center.getZ() - radius,
            center.getX() + radius, center.getY() + 32, center.getZ() + radius
        );

        // Find all hostile mobs in the raid area
        for (Mob mob : level.getEntitiesOfClass(Mob.class, searchBox)) {
            // Skip if already tracked
            if (allRaidEntities.contains(mob.getUUID())) continue;

            // Only add hostile mobs (demons)
            if (mob.getType().getCategory() != net.minecraft.world.entity.MobCategory.MONSTER) continue;

            // Don't add mobs that are too far from the raid area vertically
            if (Math.abs(mob.getY() - center.getY()) > 32) continue;

            // Add this demon to the raid
            UUID entityUUID = mob.getUUID();
            allRaidEntities.add(entityUUID);
            aliveEntities.add(entityUUID);

            // Track health
            float maxHealth = mob.getMaxHealth();
            entityMaxHealth.put(entityUUID, maxHealth);
            totalMaxHealth += maxHealth;
            currentHealth += mob.getHealth();

            // Set initial pathfinding target
            setInitialPathfindingTarget(mob);

            // Make sure it doesn't despawn
            mob.setPersistenceRequired();

            Log.debug("Detected new demon in raid area: " + mob.getType().getDescriptionId() +
                " (health: " + maxHealth + ") - adding to raid");
        }
    }

    private boolean applyGlowingToAllRaidEntities() {
        boolean anyApplied = false;

        // Search for entities in the raid area - more reliable than UUID lookup
        int radius = CivilianStructureRegistry.getSpawnRadius(structureId);
        if (radius <= 0) radius = 64;

        net.minecraft.world.phys.AABB searchBox = new net.minecraft.world.phys.AABB(
            center.getX() - radius, center.getY() - 32, center.getZ() - radius,
            center.getX() + radius, center.getY() + 32, center.getZ() + radius
        );

        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, searchBox)) {
            if (aliveEntities.contains(living.getUUID())) {
                // Only apply if not already glowing
                if (!living.hasEffect(MobEffects.GLOWING)) {
                    // Apply glowing for 10 minutes (should last rest of wave)
                    living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 12000, 0, false, false, true));
                    anyApplied = true;
                }
            }
        }
        return anyApplied;
    }

    private void scheduleNextWave() {
        currentWave++;
        state = RaidState.PREPARING;

        // Generate wave entities
        List<ResourceLocation> waveEntities = WaveGenerator.generateWave(type, difficultyLevel, currentWave);
        pendingSpawns.addAll(waveEntities);

        // Reset health tracking for new wave
        totalMaxHealth = 0f;
        currentHealth = 0f;
        entityMaxHealth.clear();

        // Schedule spawn start
        nextActionTime = level.getGameTime() + RaidConfig.wavePreparationTime.get() * 20L;

        // Update boss bar
        BossEvent.BossBarColor color = type == RaidType.DEMON ? BossEvent.BossBarColor.RED : BossEvent.BossBarColor.BLUE;
        // Boss waves get purple
        if (currentWave == totalWaves) {
            bossBar.setColor(BossEvent.BossBarColor.PURPLE);
        } else {
            bossBar.setColor(color);
        }

        updateBossBarTitle("Preparing - Wave " + currentWave);
        bossBar.setProgress(0.0f);

        Log.debug("Scheduled wave " + currentWave + " with " + pendingSpawns.size() + " entities");
    }

    private void spawnNextEntity(long gameTime) {
        if (pendingSpawns.isEmpty()) return;

        ResourceLocation entityId = pendingSpawns.pollFirst();
        spawn(entityId);
        nextActionTime = gameTime + RaidConfig.entitySpawnInterval.get() * 20L;
    }

    private void spawn(ResourceLocation id) {
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(id);
        if (entityType == null) {
            Log.debug("Failed to find entity type: " + id);
            return;
        }

        try {
            Mob mob = (Mob) entityType.create(level);
            if (mob == null) return;

            int radius = CivilianStructureRegistry.getSpawnRadius(structureId);
            if (radius <= 0) radius = 32;

            // Reduce spawn radius to keep entities closer (max 40 blocks from center)
            radius = Math.min(radius, 40);

            BlockPos spawnPos = null;

            // 60% chance to spawn in a clump near last spawn location
            if (lastSpawnLocation != null && spawnClumpRemaining > 0 && level.random.nextFloat() < 0.6f) {
                // Spawn within 3-8 blocks of last spawn location
                spawnPos = findValidSpawnNear(lastSpawnLocation, 3, 8);
                spawnClumpRemaining--;
            }

            // If clump spawn failed or not in clump mode, find new location
            if (spawnPos == null) {
                // Try to find a valid spawn location (max 10 attempts)
                for (int attempt = 0; attempt < 10; attempt++) {
                    double angle = level.random.nextDouble() * Math.PI * 2.0;
                    int dist = 10 + level.random.nextInt(radius - 10); // Spawn between 10 and radius blocks
                    int x = center.getX() + (int) Math.round(Math.cos(angle) * dist);
                    int z = center.getZ() + (int) Math.round(Math.sin(angle) * dist);

                    BlockPos testPos = findValidSpawnNear(new BlockPos(x, center.getY(), z), 0, 5);
                    if (testPos != null) {
                        spawnPos = testPos;

                        // Start a new clump (2-4 entities per clump)
                        lastSpawnLocation = spawnPos;
                        spawnClumpRemaining = 2 + level.random.nextInt(3);
                        break;
                    }
                }
            }

            // If still no valid position found, fall back to heightmap
            if (spawnPos == null) {
                double angle = level.random.nextDouble() * Math.PI * 2.0;
                int dist = 10 + level.random.nextInt(radius - 10);
                int x = center.getX() + (int) Math.round(Math.cos(angle) * dist);
                int z = center.getZ() + (int) Math.round(Math.sin(angle) * dist);
                spawnPos = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    new BlockPos(x, center.getY(), z));
            }

            // Spawn the mob
            mob.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                level.random.nextFloat() * 360F, 0);
            mob.setPersistenceRequired();
            level.addFreshEntity(mob);

            // Set initial pathfinding target to nearest raid participant
            setInitialPathfindingTarget(mob);

            // Track entity
            UUID entityUUID = mob.getUUID();
            allRaidEntities.add(entityUUID);
            aliveEntities.add(entityUUID);

            // Track health
            float maxHealth = mob.getMaxHealth();
            entityMaxHealth.put(entityUUID, maxHealth);
            totalMaxHealth += maxHealth;
            currentHealth += maxHealth;

            Log.debug("Spawned " + id + " at " + spawnPos + " (health: " + maxHealth + ")");
        } catch (Exception e) {
            Log.debug("Failed to spawn entity: " + id + " - " + e.getMessage());
        }
    }

    /**
     * Find a valid spawn position near a target position.
     * Checks for: solid ground, not in water, not underground, sky visible.
     */
    private BlockPos findValidSpawnNear(BlockPos target, int minDist, int maxDist) {
        for (int attempt = 0; attempt < 5; attempt++) {
            int offsetX = (minDist == 0 ? 0 : minDist) + level.random.nextInt(maxDist - minDist + 1);
            int offsetZ = (minDist == 0 ? 0 : minDist) + level.random.nextInt(maxDist - minDist + 1);
            if (level.random.nextBoolean()) offsetX = -offsetX;
            if (level.random.nextBoolean()) offsetZ = -offsetZ;

            BlockPos testPos = level.getHeightmapPos(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                new BlockPos(target.getX() + offsetX, target.getY(), target.getZ() + offsetZ)
            );

            // Validate spawn position
            if (isValidSpawnPosition(testPos)) {
                return testPos;
            }
        }
        return null;
    }

    /**
     * Check if a position is valid for spawning (not underground, not in water, solid ground).
     */
    private boolean isValidSpawnPosition(BlockPos pos) {
        // Check if block below is solid
        BlockPos below = pos.below();
        if (!level.getBlockState(below).isSolidRender(level, below)) {
            return false;
        }

        // Check if spawn position and above are air/passable
        if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) {
            return false;
        }

        // Check not in water
        if (level.getBlockState(pos).getFluidState().isSource()) {
            return false;
        }

        // Check can see sky (not in a cave)
        if (!level.canSeeSky(pos)) {
            return false;
        }

        // Check not too far below or above the structure center (within 20 blocks Y)
        if (Math.abs(pos.getY() - center.getY()) > 20) {
            return false;
        }

        return true;
    }

    /**
     * Set initial pathfinding goal to nearest raid participant.
     */
    private void setInitialPathfindingTarget(Mob mob) {
        ServerPlayer nearestPlayer = null;
        double nearestDistSq = Double.MAX_VALUE;

        int radius = RaidConfig.raidParticipationRadius.get();
        int radiusSq = radius * radius;

        for (ServerPlayer player : level.players()) {
            if (player.blockPosition().distSqr(center) <= radiusSq) {
                double distSq = mob.distanceToSqr(player);
                if (distSq < nearestDistSq) {
                    nearestDistSq = distSq;
                    nearestPlayer = player;
                }
            }
        }

        if (nearestPlayer != null) {
            // Set target and make mob aware of player
            mob.setTarget(nearestPlayer);
            mob.setLastHurtByMob(nearestPlayer);
        }
    }

    private void updateHealthProgress() {
        if (totalMaxHealth <= 0) {
            bossBar.setProgress(0.0f);
            return;
        }

        // Calculate current health from actual entity health
        float actualHealth = 0f;
        for (UUID entityId : aliveEntities) {
            var entity = level.getEntity(entityId);
            if (entity instanceof LivingEntity living && living.isAlive()) {
                actualHealth += living.getHealth();
            }
        }
        currentHealth = actualHealth;

        float progress = Math.max(0, Math.min(1, currentHealth / totalMaxHealth));
        bossBar.setProgress(progress);
    }

    public boolean onEntityKilled(UUID entityId) {
        if (!aliveEntities.contains(entityId)) return false;

        aliveEntities.remove(entityId);
        // Don't subtract health here - updateHealthProgress will recalculate from actual entities

        Log.debug("Raid entity killed: " + entityId + " (" + aliveEntities.size() + " remaining)");
        return true;
    }

    public void onEntityDamaged(UUID entityId, float damage) {
        // Health is now calculated from actual entities in updateHealthProgress
        // This method is kept for compatibility but doesn't need to do anything
    }

    private void triggerVictory() {
        state = RaidState.VICTORY;
        bossBar.setColor(BossEvent.BossBarColor.GREEN);
        updateBossBarTitle("Victory!");
        bossBar.setProgress(0.0f);

        broadcastMessage(Component.literal("Raid Complete! Victory!").withStyle(style ->
            style.withColor(0x55FF55)));

        // Send victory title to all participants
        sendVictoryTitle();

        // Give rewards
        giveRewards();

        // Schedule cleanup
        nextActionTime = level.getGameTime() + 100; // 5 seconds

        // Play victory sound
        level.playSound(null, center.getX(), center.getY(), center.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.HOSTILE, 128.0f, 1.0f);
    }

    private void sendVictoryTitle() {
        String raidName = (type == RaidType.DEMON ? "Demon Raid" : "Demon Slayer Raid");
        Component title = Component.literal("VICTORY!").withStyle(style ->
            style.withColor(0x55FF55).withBold(true));
        Component subtitle = Component.literal(raidName + " " + toRoman(difficultyLevel) + " Complete!").withStyle(style ->
            style.withColor(0xFFFFFF));

        for (UUID playerId : participants) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
            if (player != null) {
                // Set title animation times (fadeIn, stay, fadeOut in ticks)
                player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
                player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
                player.connection.send(new ClientboundSetTitleTextPacket(title));
            }
        }
    }

    private void triggerDefeat(String reason) {
        state = RaidState.DEFEAT;
        bossBar.setColor(BossEvent.BossBarColor.WHITE);
        updateBossBarTitle("Defeat");
        bossBar.setProgress(0.0f);

        broadcastMessage(Component.literal("Raid Failed: " + reason).withStyle(style ->
            style.withColor(0xFF5555)));

        // Despawn remaining raid entities
        despawnRaidEntities();

        // Schedule cleanup
        nextActionTime = level.getGameTime() + 100; // 5 seconds
    }

    private void despawnRaidEntities() {
        for (UUID entityId : aliveEntities) {
            level.getEntity(entityId);
            var entity = level.getEntity(entityId);
            if (entity != null) {
                entity.discard();
            }
        }
        aliveEntities.clear();
    }

    private void giveRewards() {
        if (!RaidConfig.enableOmenPotionRewards.get()) return;

        for (UUID playerId : participants) {
            Player player = level.getPlayerByUUID(playerId);
            if (player == null) continue;

            // XP reward
            player.giveExperiencePoints(difficultyLevel * 100);

            // Omen potion reward
            int rewardLevel = level.random.nextFloat() < RaidConfig.omenPotionSameLevelChance.get()
                ? difficultyLevel
                : Math.min(5, difficultyLevel + 1);

            // Create appropriate potion item
            // This will use the OmenPotionItem once implemented
            ItemStack potion = createOmenPotionItem(type == RaidType.DEMON ? "muzan" : "ubuyashiki", rewardLevel);
            if (!player.addItem(potion)) {
                // Drop on ground if inventory full
                player.drop(potion, false);
            }

            player.sendSystemMessage(Component.literal("Received Omen Potion Level " + toRoman(rewardLevel) + "!").withStyle(style ->
                style.withColor(0xFFAA00)));
        }
    }

    private ItemStack createOmenPotionItem(String type, int level) {
        // Return the omen potion item
        // This needs to be connected to the actual item registration
        ResourceLocation itemId = ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer",
            "omen_of_" + type + "_potion_" + level);
        var item = BuiltInRegistries.ITEM.get(itemId);
        if (item != null && item != net.minecraft.world.item.Items.AIR) {
            return new ItemStack(item);
        }
        // Fallback - just give XP
        return ItemStack.EMPTY;
    }

    private void cleanup() {
        // Remove boss bar from all players
        bossBar.removeAllPlayers();
        cleanupDone = true;
        Log.debug("Raid cleanup complete for " + raidId);
    }

    private boolean checkPlayerNearby() {
        int radius = RaidConfig.raidParticipationRadius.get();
        int radiusSq = radius * radius;

        for (ServerPlayer player : level.players()) {
            if (player.blockPosition().distSqr(center) <= radiusSq) {
                return true;
            }
        }
        return false;
    }

    private void updateBossBarPlayers() {
        int radius = RaidConfig.raidParticipationRadius.get();
        int radiusSq = radius * radius;

        Set<ServerPlayer> currentPlayers = new HashSet<>();
        for (ServerPlayer player : level.players()) {
            if (player.blockPosition().distSqr(center) <= radiusSq) {
                currentPlayers.add(player);
                participants.add(player.getUUID());
            }
        }

        // Update boss bar visibility
        for (ServerPlayer player : level.players()) {
            if (currentPlayers.contains(player)) {
                bossBar.addPlayer(player);
            } else {
                bossBar.removePlayer(player);
            }
        }
    }

    private void updateBossBarTitle(String suffix) {
        String raidName = (type == RaidType.DEMON ? "Demon Raid" : "Demon Slayer Raid");
        bossBar.setName(Component.literal(raidName + " " + toRoman(difficultyLevel) + " : " + suffix));
    }

    private void broadcastMessage(Component message) {
        int radius = RaidConfig.raidParticipationRadius.get();
        int radiusSq = radius * radius;

        for (ServerPlayer player : level.players()) {
            if (player.blockPosition().distSqr(center) <= radiusSq) {
                player.sendSystemMessage(message);
            }
        }
    }

    private String toRoman(int num) {
        return switch (num) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(num);
        };
    }

    // Getters
    public UUID getRaidId() { return raidId; }
    public ResourceLocation getStructureId() { return structureId; }
    public BlockPos getCenter() { return center; }
    public RaidType getType() { return type; }
    public int getDifficultyLevel() { return difficultyLevel; }
    public RaidState getState() { return state; }
    public boolean isFinished() { return (state == RaidState.VICTORY || state == RaidState.DEFEAT) && cleanupDone; }
    public boolean isRaidEntity(UUID entityId) { return allRaidEntities.contains(entityId); }

    // NBT Serialization
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("raidId", raidId);
        tag.putString("structureId", structureId.toString());
        tag.putInt("centerX", center.getX());
        tag.putInt("centerY", center.getY());
        tag.putInt("centerZ", center.getZ());
        tag.putString("type", type.name());
        tag.putInt("difficultyLevel", difficultyLevel);
        tag.putString("state", state.name());
        tag.putInt("currentWave", currentWave);
        tag.putLong("raidStartTime", raidStartTime);
        tag.putLong("lastPlayerNearbyTime", lastPlayerNearbyTime);
        tag.putFloat("totalMaxHealth", totalMaxHealth);
        tag.putFloat("currentHealth", currentHealth);

        // Save alive entities
        ListTag aliveList = new ListTag();
        for (UUID id : aliveEntities) {
            CompoundTag idTag = new CompoundTag();
            idTag.putUUID("id", id);
            aliveList.add(idTag);
        }
        tag.put("aliveEntities", aliveList);

        // Save all raid entities
        ListTag allList = new ListTag();
        for (UUID id : allRaidEntities) {
            CompoundTag idTag = new CompoundTag();
            idTag.putUUID("id", id);
            allList.add(idTag);
        }
        tag.put("allRaidEntities", allList);

        // Save participants
        ListTag participantList = new ListTag();
        for (UUID id : participants) {
            CompoundTag idTag = new CompoundTag();
            idTag.putUUID("id", id);
            participantList.add(idTag);
        }
        tag.put("participants", participantList);

        return tag;
    }

    public static KnYRaid fromNBT(ServerLevel level, CompoundTag tag) {
        try {
            ResourceLocation structureId = ResourceLocation.parse(tag.getString("structureId"));
            BlockPos center = new BlockPos(tag.getInt("centerX"), tag.getInt("centerY"), tag.getInt("centerZ"));
            RaidType type = RaidType.valueOf(tag.getString("type"));
            int difficultyLevel = tag.getInt("difficultyLevel");

            KnYRaid raid = new KnYRaid(level, structureId, center, type, difficultyLevel);

            // Restore state
            raid.state = RaidState.valueOf(tag.getString("state"));
            raid.currentWave = tag.getInt("currentWave");
            raid.raidStartTime = tag.getLong("raidStartTime");
            raid.lastPlayerNearbyTime = tag.getLong("lastPlayerNearbyTime");
            raid.totalMaxHealth = tag.getFloat("totalMaxHealth");
            raid.currentHealth = tag.getFloat("currentHealth");

            // Restore alive entities
            ListTag aliveList = tag.getList("aliveEntities", Tag.TAG_COMPOUND);
            for (int i = 0; i < aliveList.size(); i++) {
                raid.aliveEntities.add(aliveList.getCompound(i).getUUID("id"));
            }

            // Restore all raid entities
            ListTag allList = tag.getList("allRaidEntities", Tag.TAG_COMPOUND);
            for (int i = 0; i < allList.size(); i++) {
                raid.allRaidEntities.add(allList.getCompound(i).getUUID("id"));
            }

            // Restore participants
            ListTag participantList = tag.getList("participants", Tag.TAG_COMPOUND);
            for (int i = 0; i < participantList.size(); i++) {
                raid.participants.add(participantList.getCompound(i).getUUID("id"));
            }

            return raid;
        } catch (Exception e) {
            Log.debug("Failed to load raid from NBT: " + e.getMessage());
            return null;
        }
    }
}
