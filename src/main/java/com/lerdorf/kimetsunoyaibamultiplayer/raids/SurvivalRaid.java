package com.lerdorf.kimetsunoyaibamultiplayer.raids;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.config.SurvivalRaidConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.BossArrowPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Independent, command-driven survival raid. Runs in memory and ends at sunrise.
 */
public class SurvivalRaid {

    public enum RaidState {
        PREPARING,
        ACTIVE,
        VICTORY,
        DEFEAT
    }

    private static class DelayedSpawnTask {
        private final ResourceLocation entityId;
        private final BlockPos pos;
        private final boolean boss;
        private final long executeAt;

        private DelayedSpawnTask(ResourceLocation entityId, BlockPos pos, boolean boss, long executeAt) {
            this.entityId = entityId;
            this.pos = pos;
            this.boss = boss;
            this.executeAt = executeAt;
        }
    }

    private final UUID raidId;
    private final ServerLevel level;
    private final BlockPos center;
    private final int radius;
    private final int difficultyLevel;

    private RaidState state = RaidState.PREPARING;
    private int currentWave = 0;
    private SurvivalWaveGenerator.WaveType currentWaveType = SurvivalWaveGenerator.WaveType.EASY;

    private final Deque<ResourceLocation> pendingSpawns = new ArrayDeque<>();
    private int bossPendingCount = 0;

    private final Set<UUID> allRaidEntities = new HashSet<>();
    private final Set<UUID> aliveEntities = new HashSet<>();
    private final Set<UUID> aliveBosses = new HashSet<>();

    private int bossesDefeated = 0;

    private final ServerBossEvent bossBar;
    private final Set<UUID> participants = new HashSet<>();

    private UUID currentBossForArrow;
    private long bossArrowEndTime = 0L;

    private long nextSpawnTime = 0L;
    private long nextStateTime = 0L;
    private long nextWaveOrReinforcementTime = 0L;

    private List<ResourceLocation> bossWaveReinforcements = List.of();
    private boolean waveHasSpawnedBoss = false;

    private final List<DelayedSpawnTask> delayedTasks = new ArrayList<>();
    private boolean cleanupDone = false;

    public SurvivalRaid(ServerLevel level, BlockPos center, int radius, int difficultyLevel) {
        this.raidId = UUID.randomUUID();
        this.level = level;
        this.center = center;
        this.radius = Math.max(32, radius);
        this.difficultyLevel = Math.max(1, Math.min(5, difficultyLevel));

        this.bossBar = new ServerBossEvent(
            Component.literal("Sunrise Countdown"),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.PROGRESS
        );

        level.playSound(null, center.getX(), center.getY(), center.getZ(), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 128.0f, 1.0f);
        broadcastToNearby(Component.literal("Survival Raid has begun! Defeat at least one boss before sunrise.")
            .withStyle(style -> style.withColor(0xAA55FF)));

        scheduleNextWave();
    }

    public void tick() {
        if (!SurvivalRaidConfig.enableSurvivalRaids.get()) {
            stop("Survival raids disabled");
            return;
        }

        long gameTime = level.getGameTime();
        updateBossBarPlayers();
        processDelayedTasks(gameTime);
        updateSunriseProgress();
        sendBossArrowIfNeeded(gameTime);

        if (isSunriseOrLater()) {
            if (bossesDefeated > 0) {
                triggerVictory();
            } else {
                triggerDefeat("No boss defeated before sunrise");
            }
        }

        if (state == RaidState.VICTORY || state == RaidState.DEFEAT) {
            if (gameTime >= nextStateTime) {
                cleanup();
            }
            return;
        }

        if (!hasAnyAliveNonDemonPlayerInRange()) {
            triggerDefeat("All non-demon players left the raid");
            return;
        }

        if (state == RaidState.PREPARING) {
            tickPreparing(gameTime);
        } else if (state == RaidState.ACTIVE) {
            tickActive(gameTime);
        }
    }

    private void tickPreparing(long gameTime) {
        if (gameTime < nextStateTime) {
            return;
        }

        state = RaidState.ACTIVE;
        nextSpawnTime = gameTime;
        nextWaveOrReinforcementTime = gameTime + SurvivalRaidConfig.waveInterval.get() * 20L;
    }

    private void tickActive(long gameTime) {
        if (!pendingSpawns.isEmpty() && gameTime >= nextSpawnTime) {
            spawnNextEntity(gameTime);
        }

        if (currentWaveType == SurvivalWaveGenerator.WaveType.BOSS) {
            if (waveHasSpawnedBoss && aliveBosses.isEmpty()) {
                scheduleNextWave();
                return;
            }

            // Reinforcement loop: while a boss is still alive, re-spawn non-boss support every interval.
            if (waveHasSpawnedBoss && !aliveBosses.isEmpty() && gameTime >= nextWaveOrReinforcementTime) {
                pendingSpawns.addAll(bossWaveReinforcements);
                nextWaveOrReinforcementTime = gameTime + SurvivalRaidConfig.waveInterval.get() * 20L;
            }
        } else {
            // Non-boss waves advance on a timer, not on cleanup.
            if (gameTime >= nextWaveOrReinforcementTime) {
                scheduleNextWave();
                return;
            }

            // If wave naturally clears early, move on immediately.
            if (aliveEntities.isEmpty() && pendingSpawns.isEmpty()) {
                scheduleNextWave();
            }
        }
    }

    private void updateSunriseProgress() {
        long timeOfDay = level.getDayTime() % 24000L;

        float progress;
        if (timeOfDay < 13000) {
            progress = 0.0f;
        } else if (timeOfDay > 23000) {
            progress = 0.0f;
        } else {
            progress = 1.0f - ((timeOfDay - 13000L) / 10000.0f);
        }

        bossBar.setProgress(Math.max(0.0f, Math.min(1.0f, progress)));
        bossBar.setName(Component.literal("Sunrise Countdown"));

        if (state == RaidState.PREPARING) {
            bossBar.setColor(BossEvent.BossBarColor.PURPLE);
        }
    }

    private void scheduleNextWave() {
        currentWave++;
        state = RaidState.PREPARING;
        waveHasSpawnedBoss = false;

        int cycleLength = SurvivalWaveGenerator.getCycleLength(difficultyLevel);
        int cyclePhase = ((currentWave - 1) % cycleLength) + 1;

        int playerCount = countAliveNonDemonPlayersInRange();
        SurvivalWaveGenerator.WaveBundle bundle = SurvivalWaveGenerator.generateWave(difficultyLevel, Math.max(1, playerCount), cyclePhase);
        currentWaveType = SurvivalWaveGenerator.getWaveType(difficultyLevel, cyclePhase);

        pendingSpawns.clear();
        pendingSpawns.addAll(bundle.entities());
        bossPendingCount = bundle.bossCount();
        bossWaveReinforcements = bundle.reinforcementEntities();

        nextStateTime = level.getGameTime() + SurvivalRaidConfig.wavePreparationTime.get() * 20L;
        nextSpawnTime = nextStateTime;

        bossBar.setColor(currentWaveType == SurvivalWaveGenerator.WaveType.BOSS
            ? BossEvent.BossBarColor.RED
            : BossEvent.BossBarColor.PURPLE);

        broadcastToNearby(Component.literal("Survival Wave " + currentWave + " incoming...")
            .withStyle(style -> style.withColor(0xFFFFFF)));

        Log.debug("Survival raid {} scheduled wave {} (phase {} / {}, bosses: {}, total spawns: {})",
            raidId, currentWave, cyclePhase, cycleLength, bossPendingCount, pendingSpawns.size());
    }

    private void spawnNextEntity(long gameTime) {
        ResourceLocation id = pendingSpawns.pollFirst();
        if (id == null) return;

        boolean boss = bossPendingCount > 0;
        if (boss) {
            bossPendingCount--;
            waveHasSpawnedBoss = true;
        }

        spawnEntity(id, boss, gameTime);
        nextSpawnTime = gameTime + SurvivalRaidConfig.entitySpawnInterval.get() * 20L;
    }

    private void spawnEntity(ResourceLocation entityId, boolean boss, long gameTime) {
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(entityId);
        if (entityType == null) return;

        try {
            Mob mob = (Mob) entityType.create(level);
            if (mob == null) return;

            BlockPos spawnPos = boss ? findBossSpawnPosition() : findNonBossSpawnPosition();
            if (spawnPos == null) {
                spawnPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, center);
            }

            if (boss && isKizukiDemon(entityId)) {
                com.lerdorf.kimetsunoyaibamultiplayer.entities.MugenDoorEntity door =
                    com.lerdorf.kimetsunoyaibamultiplayer.entities.MugenDoorEntity.createForSpawning(level, spawnPos, 6);
                level.addFreshEntity(door);
                delayedTasks.add(new DelayedSpawnTask(entityId, spawnPos, true, gameTime + 6));
                return;
            }

            spawnMobNow(mob, spawnPos, boss);
        } catch (Exception e) {
            Log.debug("Failed to spawn survival raid entity {}: {}", entityId, e.getMessage());
        }
    }

    private void spawnMobNow(Mob mob, BlockPos spawnPos, boolean boss) {
        mob.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, level.random.nextFloat() * 360.0f, 0.0f);
        mob.setPersistenceRequired();
        mob.getPersistentData().putUUID("SurvivalRaidId", raidId);

        level.addFreshEntity(mob);

        UUID id = mob.getUUID();
        allRaidEntities.add(id);
        aliveEntities.add(id);

        if (boss) {
            aliveBosses.add(id);
            applyBossEffects(mob);
        } else {
            setTargetNearestNonDemonPlayer(mob);
        }
    }

    private void processDelayedTasks(long gameTime) {
        Iterator<DelayedSpawnTask> it = delayedTasks.iterator();
        while (it.hasNext()) {
            DelayedSpawnTask task = it.next();
            if (gameTime < task.executeAt) continue;

            try {
                EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(task.entityId);
                if (entityType != null) {
                    Mob mob = (Mob) entityType.create(level);
                    if (mob != null) {
                        spawnMobNow(mob, task.pos, task.boss);
                    }
                }
            } catch (Exception e) {
                Log.debug("Failed delayed survival spawn {}: {}", task.entityId, e.getMessage());
            }

            it.remove();
        }
    }

    private void applyBossEffects(Mob boss) {
        level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 64.0f, 1.0f);

        boss.addEffect(new MobEffectInstance(MobEffects.GLOWING, SurvivalRaidConfig.bossGlowDuration.get(), 0, false, false, true));

        if (SurvivalRaidConfig.enableBossArrow.get()) {
            currentBossForArrow = boss.getUUID();
            bossArrowEndTime = level.getGameTime() + SurvivalRaidConfig.bossArrowDuration.get();
            sendBossArrowToNearbyPlayers((int) SurvivalRaidConfig.bossArrowDuration.get());
        }

        setTargetNearestNonDemonPlayer(boss);
    }

    private void setTargetNearestNonDemonPlayer(Mob mob) {
        ServerPlayer nearest = null;
        double nearestDist = Double.MAX_VALUE;
        int radiusSq = radius * radius;

        for (ServerPlayer player : level.players()) {
            if (!player.isAlive()) continue;
            if (player.getPersistentData().getBoolean("oni")) continue;
            if (player.blockPosition().distSqr(center) > radiusSq) continue;

            double dist = mob.distanceToSqr(player);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = player;
            }
        }

        if (nearest != null) {
            mob.setTarget(nearest);
            mob.setLastHurtByMob(nearest);
        }
    }

    private void sendBossArrowIfNeeded(long gameTime) {
        if (!SurvivalRaidConfig.enableBossArrow.get()) return;
        if (currentBossForArrow == null) return;
        if (gameTime > bossArrowEndTime) return;
        if (gameTime % 5L != 0L) return;

        Entity entity = level.getEntity(currentBossForArrow);
        if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
            return;
        }

        int remaining = (int) Math.max(0L, bossArrowEndTime - gameTime);
        sendBossArrowToNearbyPlayers(remaining);
    }

    private void sendBossArrowToNearbyPlayers(int remainingTicks) {
        Entity entity = currentBossForArrow == null ? null : level.getEntity(currentBossForArrow);
        if (entity == null) return;

        BossArrowPacket packet = new BossArrowPacket(entity.getX(), entity.getY() + 1.0, entity.getZ(), remainingTicks);
        int radiusSq = radius * radius;

        for (ServerPlayer player : level.players()) {
            if (player.blockPosition().distSqr(center) > radiusSq) continue;
            if (player.getPersistentData().getBoolean("oni")) continue;
            ModNetworking.sendToPlayer(packet, player);
        }
    }

    private BlockPos findBossSpawnPosition() {
        int bossRadius = Math.min(radius, SurvivalRaidConfig.bossSpawnRadius.get());

        for (int attempt = 0; attempt < 10; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0;
            int dist = 12 + level.random.nextInt(Math.max(4, bossRadius - 12));
            int x = center.getX() + (int) Math.round(Math.cos(angle) * dist);
            int z = center.getZ() + (int) Math.round(Math.sin(angle) * dist);

            BlockPos test = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, center.getY(), z));
            if (isValidSpawnPosition(test)) {
                return test;
            }
        }

        return null;
    }

    private BlockPos findNonBossSpawnPosition() {
        List<ServerPlayer> eligible = getAliveNonDemonPlayersInRange();
        if (eligible.isEmpty()) return findBossSpawnPosition();

        ServerPlayer anchor = eligible.get(level.random.nextInt(eligible.size()));
        int maxRadius = SurvivalRaidConfig.entitySpawnNearPlayerRadius.get();

        for (int attempt = 0; attempt < 10; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0;
            int dist = 12 + level.random.nextInt(Math.max(4, maxRadius - 12));
            int x = (int) Math.round(anchor.getX() + Math.cos(angle) * dist);
            int z = (int) Math.round(anchor.getZ() + Math.sin(angle) * dist);

            BlockPos test = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, anchor.blockPosition().getY(), z));
            if (isValidSpawnPosition(test)) {
                return test;
            }
        }

        return null;
    }

    private boolean isValidSpawnPosition(BlockPos pos) {
        BlockPos below = pos.below();
        if (!level.getBlockState(below).isSolidRender(level, below)) return false;
        if (!level.getBlockState(pos).isAir()) return false;
        if (!level.getBlockState(pos.above()).isAir()) return false;
        if (level.getBlockState(pos).getFluidState().isSource()) return false;
        if (!level.canSeeSky(pos)) return false;
        return Math.abs(pos.getY() - center.getY()) <= 40;
    }

    private boolean isKizukiDemon(ResourceLocation id) {
        String path = id.getPath();

        Set<String> lowerMoons = Set.of(
            "kyogai", "kamanue", "rui", "mukago", "wakuraba", "rokuro", "hairo", "enmu"
        );

        Set<String> upperMoons = Set.of(
            "daki", "gyutaro", "kaigaku", "gyokko", "hantengu", "nakime", "akaza", "doma", "kokushibo"
        );

        return lowerMoons.contains(path) || upperMoons.contains(path);
    }

    public boolean onEntityKilled(UUID entityId) {
        if (!aliveEntities.contains(entityId)) return false;

        aliveEntities.remove(entityId);

        if (aliveBosses.remove(entityId)) {
            bossesDefeated++;
            broadcastToNearby(Component.literal("Boss defeated! (" + bossesDefeated + ")")
                .withStyle(style -> style.withColor(0xFFAA00)));
        }

        return true;
    }

    public void stop(String reason) {
        triggerDefeat(reason != null ? reason : "Raid stopped");
        cleanup();
    }

    private void triggerVictory() {
        if (state == RaidState.VICTORY || state == RaidState.DEFEAT) return;

        state = RaidState.VICTORY;
        bossBar.setColor(BossEvent.BossBarColor.GREEN);

        broadcastToNearby(Component.literal("Survival raid complete! You survived the night.")
            .withStyle(style -> style.withColor(0x55FF55).withBold(true)));

        sendTitleToParticipants(
            Component.literal("VICTORY").withStyle(style -> style.withColor(0x55FF55).withBold(true)),
            Component.literal("Survived until sunrise")
        );

        giveRewards();
        despawnRaidEntities();
        nextStateTime = level.getGameTime() + 80L;
    }

    private void triggerDefeat(String reason) {
        if (state == RaidState.VICTORY || state == RaidState.DEFEAT) return;

        state = RaidState.DEFEAT;
        bossBar.setColor(BossEvent.BossBarColor.WHITE);

        broadcastToNearby(Component.literal("Survival raid failed: " + reason)
            .withStyle(style -> style.withColor(0xFF5555)));

        sendTitleToParticipants(
            Component.literal("DEFEAT").withStyle(style -> style.withColor(0xFF5555).withBold(true)),
            Component.literal(reason)
        );

        despawnRaidEntities();
        nextStateTime = level.getGameTime() + 80L;
    }

    private void giveRewards() {
        int xp = currentWave * 50 + bossesDefeated * 200;
        if (xp <= 0) return;

        for (ServerPlayer player : getAliveNonDemonPlayersInRange()) {
            player.giveExperiencePoints(xp);
            player.sendSystemMessage(Component.literal("Reward: " + xp + " XP")
                .withStyle(style -> style.withColor(0xFFAA00)));
        }
    }

    private void sendTitleToParticipants(Component title, Component subtitle) {
        for (UUID id : participants) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
            if (player == null) continue;

            player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
            player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
            player.connection.send(new ClientboundSetTitleTextPacket(title));
        }
    }

    private void despawnRaidEntities() {
        for (UUID entityId : new HashSet<>(aliveEntities)) {
            Entity entity = level.getEntity(entityId);
            if (entity != null) {
                entity.discard();
            }
        }

        aliveEntities.clear();
        aliveBosses.clear();
        pendingSpawns.clear();
    }

    private void cleanup() {
        if (cleanupDone) return;

        bossBar.removeAllPlayers();
        cleanupDone = true;
    }

    private void updateBossBarPlayers() {
        int radiusSq = radius * radius;
        Set<ServerPlayer> nearby = new HashSet<>();

        for (ServerPlayer player : level.players()) {
            if (player.blockPosition().distSqr(center) > radiusSq) continue;
            if (player.getPersistentData().getBoolean("oni")) continue;

            nearby.add(player);
            participants.add(player.getUUID());
            bossBar.addPlayer(player);
        }

        for (ServerPlayer player : level.players()) {
            if (!nearby.contains(player)) {
                bossBar.removePlayer(player);
            }
        }
    }

    private void broadcastToNearby(Component message) {
        int radiusSq = radius * radius;
        for (ServerPlayer player : level.players()) {
            if (player.blockPosition().distSqr(center) <= radiusSq) {
                player.sendSystemMessage(message);
            }
        }
    }

    private List<ServerPlayer> getAliveNonDemonPlayersInRange() {
        List<ServerPlayer> out = new ArrayList<>();
        int radiusSq = radius * radius;

        for (ServerPlayer player : level.players()) {
            if (!player.isAlive()) continue;
            if (player.getPersistentData().getBoolean("oni")) continue;
            if (player.blockPosition().distSqr(center) > radiusSq) continue;
            out.add(player);
        }

        return out;
    }

    private int countAliveNonDemonPlayersInRange() {
        return getAliveNonDemonPlayersInRange().size();
    }

    private boolean hasAnyAliveNonDemonPlayerInRange() {
        return countAliveNonDemonPlayersInRange() > 0;
    }

    private boolean isSunriseOrLater() {
        long timeOfDay = level.getDayTime() % 24000L;
        return timeOfDay >= 23000L || timeOfDay < 13000L;
    }

    public UUID getRaidId() {
        return raidId;
    }

    public ServerLevel getLevel() {
        return level;
    }

    public BlockPos getCenter() {
        return center;
    }

    public int getRadius() {
        return radius;
    }

    public int getDifficultyLevel() {
        return difficultyLevel;
    }

    public int getCurrentWave() {
        return currentWave;
    }

    public int getBossesDefeated() {
        return bossesDefeated;
    }

    public int getAliveEntityCount() {
        return aliveEntities.size();
    }

    public int getAliveBossCount() {
        return aliveBosses.size();
    }

    public int getActivePlayerCount() {
        return countAliveNonDemonPlayersInRange();
    }

    public RaidState getState() {
        return state;
    }

    public boolean isFinished() {
        return (state == RaidState.VICTORY || state == RaidState.DEFEAT) && cleanupDone;
    }

    public boolean isRaidEntity(UUID entityId) {
        return allRaidEntities.contains(entityId);
    }
}
