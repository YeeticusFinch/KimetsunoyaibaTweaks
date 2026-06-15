package com.lerdorf.kimetsunoyaibamultiplayer.raids;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.config.FinalSelectionRaidConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.DemonSlayerEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.BossArrowPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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
import net.minecraft.world.phys.AABB;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Seven-night Final Selection raid for Mt. Fujikasane.
 */
public class FinalSelectionRaid {

    private static final int TOTAL_NIGHTS = 7;

    private static final long SUNSET_OFFSET = 13000L;
    private static final long MIDNIGHT_OFFSET = 18000L;
    private static final long SUNRISE_OFFSET = 23000L;

    private static final int EASY_PER_PLAYER = 8;
    private static final int MIXED_EASY_PER_PLAYER = 4;
    private static final int MIXED_MEDIUM_PER_PLAYER = 4;
    private static final int BOSS_ESCORT_EASY_PER_PLAYER = 4;
    private static final int BOSS_ESCORT_MEDIUM_PER_PLAYER = 2;
    private static final int NON_BOSS_PER_PLAYER_SPAWN_MULTIPLIER = 2;
    private static final double NON_BOSS_SPAWN_FREQUENCY_MULTIPLIER = 6.5D;
    private static final int NON_BOSS_MIN_SPAWN_DISTANCE_FROM_PLAYER = 20;
    private static final int NON_BOSS_MAX_SPAWN_DISTANCE_FROM_PLAYER = 100;
    private static final long RAID_MOB_RETARGET_INTERVAL_TICKS = 20L;
    private static final double RAID_MOB_CHASE_SPEED = 1.3D;
    private static final long PLAYER_PRESSURE_CHECK_INTERVAL_TICKS = 100L;
    private static final int PLAYER_PRESSURE_RADIUS = 100;
    private static final int DAY_PASSIVE_ANIMAL_WAVES_PER_DAY_BREAK = 2;
    private static final int BOSS_ARROW_DURATION_TICKS = 20 * 20;
    private static final ResourceLocation HAND_DEMON_ID = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "hand_demon");
    private static final ResourceLocation BASE_SWAMP_DEMON_ID = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "swamp_demon");
    private static final ResourceLocation CUSTOM_SWAMP_DEMON_ID = ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "swamp_demon");
    private static final int HAND_DEMON_WEIGHT = 4;
    private static final List<ResourceLocation> FINAL_SELECTION_BOSS_POOL = List.of(
        //ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "demon_6"),
        //ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "demon_7"),
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "demon_8"),
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "demon_10"),
        HAND_DEMON_ID,
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "susamaru"),
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "yahaba")
    );

    private static final int ESCORT_STEPS = 4; // 2 medium steps + 2 easy steps
    private static final List<ResourceLocation> DAY_PASSIVE_ANIMALS = List.of(
        ResourceLocation.fromNamespaceAndPath("minecraft", "cow"),
        ResourceLocation.fromNamespaceAndPath("minecraft", "pig"),
        ResourceLocation.fromNamespaceAndPath("minecraft", "chicken"),
        ResourceLocation.fromNamespaceAndPath("minecraft", "goat"),
        ResourceLocation.fromNamespaceAndPath("minecraft", "sheep")
    );
    private static final List<BlockPos> RAID_BOUNDARY_POINTS = List.of(
        new BlockPos(565, 0, 428),
        new BlockPos(432, 0, 526),
        new BlockPos(276, 0, 588),
        new BlockPos(183, 0, 562),
        new BlockPos(100, 0, 563),
        new BlockPos(-36, 0, 515),
        new BlockPos(-187, 0, 445),
        new BlockPos(-240, 0, 302),
        new BlockPos(-229, 0, 226),
        new BlockPos(-186, 0, 94),
        new BlockPos(-195, 0, -173),
        new BlockPos(38, 0, -303),
        new BlockPos(279, 0, -364),
        new BlockPos(477, 0, -347),
        new BlockPos(636, 0, -309),
        new BlockPos(605, 0, 9),
        new BlockPos(614, 0, 241),
        new BlockPos(594, 0, 381)
    );
    private static final double BORDER_PARTICLE_TRIGGER_DISTANCE = 32.0D;
    private static final double BORDER_PARTICLE_SEGMENT_STEP = 2.0D;
    private static final int BORDER_PARTICLE_INTERVAL_TICKS = 10;

    public enum RaidState {
        PREPARING,
        NIGHT_ACTIVE,
        BOSS_PRE_MIDNIGHT,
        BOSS_MIDNIGHT_HOLD,
        BOSS_SUNRISE_ACCEL,
        DAY_BREAK,
        VICTORY,
        DEFEAT
    }

    private enum NightType {
        EASY,
        MIXED,
        BOSS
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
    private final int raidRadius;
    private final ServerBossEvent bossBar;

    private RaidState state = RaidState.PREPARING;

    private int currentNight = 0;
    private NightType currentNightType = NightType.EASY;

    private final Deque<ResourceLocation> pendingSpawns = new ArrayDeque<>();
    private long spawnIntervalTicks = 20L;
    private long nextSpawnTime = 0L;

    private final Set<UUID> participants = new HashSet<>();
    private final Set<UUID> allRaidEntities = new HashSet<>();
    private final Set<UUID> aliveEntities = new HashSet<>();
    private final Set<UUID> aliveBosses = new HashSet<>();
    private final Set<ResourceLocation> usedBossDemons = new HashSet<>();
    private final List<DelayedSpawnTask> delayedTasks = new ArrayList<>();
    private boolean handDemonSpawned = false;

    private UUID currentBossForArrow;
    private long bossArrowEndTime = 0L;
    private UUID currentBossEntityId;

    private long phaseStartGameTime = 0L;
    private long nightStartAbsoluteTime = 0L;
    private long lastControlledAbsoluteTime = Long.MIN_VALUE;

    private long nextEscortWaveStartTime = Long.MAX_VALUE;
    private int escortWaveStep = -1;
    private long nextEscortWaveStepTime = Long.MAX_VALUE;
    private int dayPassiveAnimalWavesSpawned = 0;
    private long nextDayPassiveAnimalWaveTime = Long.MAX_VALUE;

    private boolean cleanupDone = false;

    public FinalSelectionRaid(ServerLevel level, BlockPos center, int raidRadius) {
        this(level, center, raidRadius, UUID.randomUUID(), true);
    }

    private FinalSelectionRaid(ServerLevel level, BlockPos center, int raidRadius, UUID raidId, boolean startImmediately) {
        if (!FinalSelectionProcedure.MT_FUJIKASANE_KEY.equals(level.dimension())) {
            throw new IllegalArgumentException("FinalSelectionRaid can only run in Mt Fujikasane");
        }

        this.raidId = raidId;
        this.level = level;
        this.center = center;
        this.raidRadius = Math.max(32, raidRadius);

        this.bossBar = new ServerBossEvent(
            Component.literal("Night 1"),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.PROGRESS
        );

        if (startImmediately) {
            MtFujikasaneDaylightController.pauseDaylightCycle(level);
            startNextNight();
        }
    }

    public void tick() {
        long gameTime = level.getGameTime();

        if (!FinalSelectionRaidConfig.enableFinalSelectionRaid.get()) {
            triggerDefeat("Final Selection raid disabled");
        }

        // Keep scripted time control active for this dimension throughout the raid lifecycle.
        MtFujikasaneDaylightController.pauseDaylightCycle(level);
        if (lastControlledAbsoluteTime != Long.MIN_VALUE) {
            // Reassert the exact scripted absolute time every tick for extra resilience.
            MtFujikasaneDaylightController.setPausedTime(level, lastControlledAbsoluteTime);
        }

        updateBossBarPlayers();
        if (gameTime % BORDER_PARTICLE_INTERVAL_TICKS == 0L) {
            renderBoundaryParticlesForNearbyPlayers();
        }
        processDelayedTasks(gameTime);
        sendBossArrowIfNeeded(gameTime);
        maintainRaidMobTargeting(gameTime);
        maintainPerPlayerDemonPressure(gameTime);

        if (state == RaidState.VICTORY || state == RaidState.DEFEAT) {
            if (!cleanupDone) {
                cleanup();
            }
            return;
        }

        if (level.players().isEmpty()) {
            // Pause raid progression when nobody is online in this dimension (logout/restart safety).
            return;
        }
        if (!hasAnyAliveNonDemonPlayerInDimension()) {
            triggerDefeat("All candidates were defeated");
            return;
        }

        if (isDemonSpawnState() && !pendingSpawns.isEmpty() && gameTime >= nextSpawnTime) {
            spawnNextPending(gameTime);
        }

        switch (state) {
            case NIGHT_ACTIVE -> tickNightActive(gameTime);
            case BOSS_PRE_MIDNIGHT -> tickBossPreMidnight(gameTime);
            case BOSS_MIDNIGHT_HOLD -> tickBossMidnightHold(gameTime);
            case BOSS_SUNRISE_ACCEL -> tickBossSunriseAccel(gameTime);
            case DAY_BREAK -> tickDayBreak(gameTime);
            default -> {
            }
        }
    }

    private void tickNightActive(long gameTime) {
        long nightDuration = getNightDurationTicks();
        float progress = getPhaseProgress(gameTime, nightDuration);
        setControlledTime(nightStartAbsoluteTime + (long) ((SUNRISE_OFFSET - SUNSET_OFFSET) * progress));

        bossBar.setName(Component.literal("Night " + currentNight));
        bossBar.setColor(BossEvent.BossBarColor.PURPLE);
        bossBar.setProgress(1.0f - progress);

        if (progress >= 1.0f) {
            completeNightAndEnterDayBreak();
        }
    }

    private void tickBossPreMidnight(long gameTime) {
        long toMidnightDuration = Math.max(20L, getNightDurationTicks() / 2L);
        float progress = getPhaseProgress(gameTime, toMidnightDuration);
        setControlledTime(nightStartAbsoluteTime + (long) ((MIDNIGHT_OFFSET - SUNSET_OFFSET) * progress));

        bossBar.setName(Component.literal("Night " + currentNight));
        bossBar.setColor(BossEvent.BossBarColor.PURPLE);
        bossBar.setProgress(1.0f - (progress * 0.5f));

        if (progress >= 1.0f) {
            setControlledTime(nightStartAbsoluteTime + (MIDNIGHT_OFFSET - SUNSET_OFFSET));
            spawnBossAtMidnight();
            state = RaidState.BOSS_MIDNIGHT_HOLD;
            phaseStartGameTime = gameTime;
            scheduleNextEscortWave(gameTime);
            // Start boss escort pressure immediately once the boss appears.
            nextEscortWaveStartTime = gameTime;
        }
    }

    private void tickBossMidnightHold(long gameTime) {
        setControlledTime(nightStartAbsoluteTime + (MIDNIGHT_OFFSET - SUNSET_OFFSET));

        updateBossHealthBar();

        if (aliveBosses.isEmpty()) {
            pendingSpawns.clear(); // stop any queued reinforcements immediately after boss death
            state = RaidState.BOSS_SUNRISE_ACCEL;
            phaseStartGameTime = gameTime;
            bossBar.setColor(BossEvent.BossBarColor.YELLOW);
            return;
        }

        handleBossEscortReinforcements(gameTime);
    }

    private void tickBossSunriseAccel(long gameTime) {
        long accelDuration = getSunriseAccelerationTicks();
        float progress = getPhaseProgress(gameTime, accelDuration);

        setControlledTime(
            nightStartAbsoluteTime + (MIDNIGHT_OFFSET - SUNSET_OFFSET)
                + (long) ((SUNRISE_OFFSET - MIDNIGHT_OFFSET) * progress)
        );

        bossBar.setName(Component.literal("Night " + currentNight));
        bossBar.setColor(BossEvent.BossBarColor.YELLOW);
        bossBar.setProgress(0.5f * (1.0f - progress));

        if (progress >= 1.0f) {
            completeNightAndEnterDayBreak();
        }
    }

    private void tickDayBreak(long gameTime) {
        long dayDurationTicks = getDayDurationTicks();
        float progress = getPhaseProgress(gameTime, dayDurationTicks);

        long sunriseAbsolute = nightStartAbsoluteTime + (SUNRISE_OFFSET - SUNSET_OFFSET);
        long nextSunsetAbsolute = nightStartAbsoluteTime + 24000L;

        setControlledTime(sunriseAbsolute + (long) ((nextSunsetAbsolute - sunriseAbsolute) * progress));

        int dayLabel = Math.min(7, currentNight + 1);
        bossBar.setName(Component.literal("Day " + dayLabel));
        bossBar.setColor(BossEvent.BossBarColor.BLUE);
        bossBar.setProgress(progress);

        if (dayPassiveAnimalWavesSpawned == 0) {
            nextDayPassiveAnimalWaveTime = phaseStartGameTime;
        }

        if (dayPassiveAnimalWavesSpawned < DAY_PASSIVE_ANIMAL_WAVES_PER_DAY_BREAK
            && gameTime >= nextDayPassiveAnimalWaveTime) {
            spawnDaytimePassiveAnimals();
            dayPassiveAnimalWavesSpawned++;
            long interval = Math.max(20L, dayDurationTicks / DAY_PASSIVE_ANIMAL_WAVES_PER_DAY_BREAK);
            nextDayPassiveAnimalWaveTime = gameTime + interval;
        }

        if (progress >= 1.0f) {
            if (currentNight >= TOTAL_NIGHTS) {
                triggerVictory();
            } else {
                startNextNight();
            }
        }
    }

    private void startNextNight() {
        currentNight++;
        if (currentNight > TOTAL_NIGHTS) {
            triggerVictory();
            return;
        }

        state = RaidState.PREPARING;
        phaseStartGameTime = level.getGameTime();

        long currentAbsolute = level.getDayTime();
        long cycleBase = (currentAbsolute / 24000L) * 24000L;
        long mod = currentAbsolute % 24000L;
        nightStartAbsoluteTime = mod <= SUNSET_OFFSET ? cycleBase + SUNSET_OFFSET : cycleBase + 24000L + SUNSET_OFFSET;
        setControlledTime(nightStartAbsoluteTime);

        currentNightType = getNightType(currentNight);
        prepareNightSpawns();

        currentBossEntityId = null;
        nextEscortWaveStartTime = Long.MAX_VALUE;
        escortWaveStep = -1;
        nextEscortWaveStepTime = Long.MAX_VALUE;
        dayPassiveAnimalWavesSpawned = 0;
        nextDayPassiveAnimalWaveTime = Long.MAX_VALUE;

        state = (currentNightType == NightType.BOSS) ? RaidState.BOSS_PRE_MIDNIGHT : RaidState.NIGHT_ACTIVE;

        broadcastToDimension(Component.literal("Night " + currentNight + " begins.")
            .withStyle(style -> style.withColor(0xAA55FF)));

        Log.debug("[FinalSelectionRaid] Started night {} type={}", currentNight, currentNightType);
    }

    private void prepareNightSpawns() {
        int players = Math.max(1, getAliveNonDemonPlayersInDimension().size());
        pendingSpawns.clear();

        switch (currentNightType) {
            case EASY -> {
                pendingSpawns.addAll(pick(EntityPowerScale.EASY_DEMON, scalePerPlayerSpawnCount(EASY_PER_PLAYER, players)));
                spawnIntervalTicks = increaseSpawnFrequency(distributeAcrossDuration(getNightDurationTicks(), pendingSpawns.size()));
            }
            case MIXED -> {
                pendingSpawns.addAll(pick(EntityPowerScale.EASY_DEMON, scalePerPlayerSpawnCount(MIXED_EASY_PER_PLAYER, players)));
                pendingSpawns.addAll(pick(EntityPowerScale.MEDIUM_DEMON, scalePerPlayerSpawnCount(MIXED_MEDIUM_PER_PLAYER, players)));
                spawnIntervalTicks = increaseSpawnFrequency(distributeAcrossDuration(getNightDurationTicks(), pendingSpawns.size()));
            }
            case BOSS -> {
                // Pre-midnight escort spawns only (boss itself spawns at midnight at center)
                pendingSpawns.addAll(pick(EntityPowerScale.MEDIUM_DEMON, scalePerPlayerSpawnCount(BOSS_ESCORT_MEDIUM_PER_PLAYER, players)));
                pendingSpawns.addAll(pick(EntityPowerScale.EASY_DEMON, scalePerPlayerSpawnCount(BOSS_ESCORT_EASY_PER_PLAYER, players)));
                spawnIntervalTicks = increaseSpawnFrequency(distributeAcrossDuration(Math.max(20L, getNightDurationTicks() / 2L), pendingSpawns.size()));
            }
        }

        nextSpawnTime = level.getGameTime() + spawnIntervalTicks;
    }

    private void spawnNextPending(long gameTime) {
        ResourceLocation id = pendingSpawns.pollFirst();
        if (id == null) return;

        spawnEntity(id, false, gameTime);
        nextSpawnTime = gameTime + spawnIntervalTicks;
    }

    private void spawnBossAtMidnight() {
        ResourceLocation bossId = pickFinalSelectionBoss();
        if (bossId == null) {
            triggerDefeat("No boss available for Final Selection");
            return;
        }

        spawnEntity(bossId, true, level.getGameTime());
        if (currentBossEntityId == null) {
            triggerDefeat("Boss failed to spawn at midnight");
            return;
        }
        sendTitlesToDimension(
            Component.literal("BOSS WAVE STARTING").withStyle(style -> style.withColor(0xAA0000).withBold(true)),
            Component.literal("Defeat the boss to continue the night")
        );
        broadcastToDimension(Component.literal("A boss has appeared at the mountain center.")
            .withStyle(style -> style.withColor(0xFF5555).withBold(true)));
    }

    private void handleBossEscortReinforcements(long gameTime) {
        if (escortWaveStep == -1 && gameTime >= nextEscortWaveStartTime) {
            escortWaveStep = 0;
            nextEscortWaveStepTime = gameTime;
        }

        if (escortWaveStep == -1 || gameTime < nextEscortWaveStepTime) {
            return;
        }

        int players = Math.max(1, getAliveNonDemonPlayersInDimension().size());

        if (escortWaveStep == 0 || escortWaveStep == 1) {
            spawnEscortStep(EntityPowerScale.MEDIUM_DEMON, players, gameTime);
        } else {
            spawnEscortStep(EntityPowerScale.EASY_DEMON, players, gameTime);
        }

        escortWaveStep++;
        if (escortWaveStep >= ESCORT_STEPS) {
            scheduleNextEscortWave(gameTime);
        } else {
            nextEscortWaveStepTime = gameTime + getBossEscortStepTicks();
        }
    }

    private void spawnEscortStep(EntityPowerScale scale, int players, long gameTime) {
        List<ResourceLocation> step = pick(scale, Math.max(1, players * NON_BOSS_PER_PLAYER_SPAWN_MULTIPLIER));
        for (ResourceLocation id : step) {
            spawnEntity(id, false, gameTime);
        }
    }

    private void scheduleNextEscortWave(long gameTime) {
        escortWaveStep = -1;
        nextEscortWaveStepTime = Long.MAX_VALUE;
        nextEscortWaveStartTime = gameTime + getBossEscortRespawnIntervalTicks();
    }

    private void spawnEntity(ResourceLocation entityId, boolean boss, long gameTime) {
        if (isSwampDemonId(entityId)) {
            return;
        }
        if (!boss && !isDemonSpawnState()) {
            return;
        }

        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(entityId);
        if (entityType == null) return;

        try {
            Mob mob = (Mob) entityType.create(level);
            if (mob == null) return;

            BlockPos spawnPos = boss ? findCenterSurface() : findSpawnNearPlayer();
            if (spawnPos == null) {
                spawnPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, center);
            }

            spawnMobNow(mob, spawnPos, boss, null);
        } catch (Exception e) {
            Log.debug("[FinalSelectionRaid] spawn failed {}: {}", entityId, e.getMessage());
        }
    }

    private void spawnMobNow(Mob mob, BlockPos spawnPos, boolean boss, ServerPlayer preferredTarget) {
        ResourceLocation mobId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        if (isSwampDemonId(mobId) || (!boss && (!isDemonSpawnState() || !FinalSelectionProcedure.canSpawnAdditionalNonBossDemon(level)))) {
            mob.discard();
            return;
        }

        mob.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, level.random.nextFloat() * 360.0f, 0.0f);
        mob.setPersistenceRequired();
        mob.getPersistentData().putUUID("FinalSelectionRaidId", raidId);

        level.addFreshEntity(mob);

        UUID entityId = mob.getUUID();
        allRaidEntities.add(entityId);
        aliveEntities.add(entityId);

        if (boss) {
            aliveBosses.add(entityId);
            currentBossEntityId = entityId;
            applyBossEffects(mob);
        } else {
            setMobTarget(mob, preferredTarget);
        }
    }

    private void processDelayedTasks(long gameTime) {
        Iterator<DelayedSpawnTask> it = delayedTasks.iterator();
        while (it.hasNext()) {
            DelayedSpawnTask task = it.next();
            if (gameTime < task.executeAt) continue;

            try {
                if (isSwampDemonId(task.entityId) || (!task.boss && !isDemonSpawnState())) {
                    it.remove();
                    continue;
                }
                EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(task.entityId);
                if (entityType != null) {
                    Mob mob = (Mob) entityType.create(level);
                    if (mob != null) {
                        spawnMobNow(mob, task.pos, task.boss, null);
                    }
                }
            } catch (Exception e) {
                Log.debug("[FinalSelectionRaid] delayed spawn failed {}: {}", task.entityId, e.getMessage());
            }

            it.remove();
        }
    }

    private void applyBossEffects(Mob boss) {
        level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 64.0f, 1.0f);
        boss.addEffect(new MobEffectInstance(MobEffects.GLOWING, FinalSelectionRaidConfig.bossGlowDurationTicks.get(), 0, false, false, true));

        currentBossForArrow = boss.getUUID();
        bossArrowEndTime = level.getGameTime() + BOSS_ARROW_DURATION_TICKS;
        sendBossArrowToDimensionPlayers(BOSS_ARROW_DURATION_TICKS);

        setMobTarget(boss, null);
    }

    private void setMobTarget(Mob mob, ServerPlayer preferredTarget) {
        if (preferredTarget != null && preferredTarget.isAlive()
            && !preferredTarget.getPersistentData().getBoolean("oni")
            && !FinalSelectionProcedure.isPlayerDisqualified(preferredTarget)) {
            mob.setTarget(preferredTarget);
            mob.setLastHurtByMob(preferredTarget);
            mob.getNavigation().moveTo(preferredTarget, RAID_MOB_CHASE_SPEED);
            return;
        }

        LivingEntity chosen = choosePreferredTarget(mob);
        if (chosen == null) {
            return;
        }

        mob.setTarget(chosen);
        if (chosen instanceof Mob chosenMob) {
            mob.setLastHurtByMob(chosenMob);
        } else {
            mob.setLastHurtByMob(null);
        }
        mob.getNavigation().moveTo(chosen, RAID_MOB_CHASE_SPEED);
    }

    private LivingEntity choosePreferredTarget(Mob mob) {
        ServerPlayer nearest = null;
        double nearestPlayerDist = Double.MAX_VALUE;

        for (ServerPlayer player : level.players()) {
            if (!player.isAlive()) continue;
            if (player.getPersistentData().getBoolean("oni")) continue;
            if (FinalSelectionProcedure.isPlayerDisqualified(player)) continue;

            double dist = mob.distanceToSqr(player);
            if (dist < nearestPlayerDist) {
                nearestPlayerDist = dist;
                nearest = player;
            }
        }

        DemonSlayerEntity nearestSlayer = null;
        double nearestSlayerDist = Double.MAX_VALUE;
        AABB slayerSearch = mob.getBoundingBox().inflate(PLAYER_PRESSURE_RADIUS);
        for (DemonSlayerEntity slayer : level.getEntitiesOfClass(DemonSlayerEntity.class, slayerSearch, Entity::isAlive)) {
            double dist = mob.distanceToSqr(slayer);
            if (dist < nearestSlayerDist) {
                nearestSlayerDist = dist;
                nearestSlayer = slayer;
            }
        }

        LivingEntity currentTarget = mob.getTarget();
        if (currentTarget instanceof ServerPlayer currentPlayer
            && currentPlayer.isAlive()
            && !currentPlayer.getPersistentData().getBoolean("oni")
            && !FinalSelectionProcedure.isPlayerDisqualified(currentPlayer)) {
            double currentPlayerDist = mob.distanceToSqr(currentPlayer);
            boolean slayerDamagedMob = mob.getLastHurtByMob() instanceof DemonSlayerEntity;
            if (nearestSlayer != null && (slayerDamagedMob || nearestSlayerDist < currentPlayerDist)) {
                return nearestSlayer;
            }
            return currentPlayer;
        }

        if (nearest != null && (nearestSlayer == null || nearestPlayerDist <= nearestSlayerDist)) {
            return nearest;
        }
        return nearestSlayer;
    }

    private void maintainRaidMobTargeting(long gameTime) {
        if (gameTime % RAID_MOB_RETARGET_INTERVAL_TICKS != 0L) {
            return;
        }

        if (aliveEntities.isEmpty()) {
            return;
        }

        for (UUID entityId : new HashSet<>(aliveEntities)) {
            Entity entity = level.getEntity(entityId);
            if (!(entity instanceof Mob mob) || !mob.isAlive()) {
                aliveEntities.remove(entityId);
                aliveBosses.remove(entityId);
                continue;
            }

            LivingEntity currentTarget = mob.getTarget();
            if (currentTarget == null || !currentTarget.isAlive()) {
                setMobTarget(mob, null);
                continue;
            }

            LivingEntity preferred = choosePreferredTarget(mob);
            if (preferred != null && preferred != currentTarget) {
                mob.setTarget(preferred);
                mob.getNavigation().moveTo(preferred, RAID_MOB_CHASE_SPEED);
                continue;
            }

            if (mob.getNavigation().isDone() && mob.distanceToSqr(currentTarget) > 9.0D) {
                mob.getNavigation().moveTo(currentTarget, RAID_MOB_CHASE_SPEED);
            }
        }
    }

    private void maintainPerPlayerDemonPressure(long gameTime) {
        if (gameTime % PLAYER_PRESSURE_CHECK_INTERVAL_TICKS != 0L) {
            return;
        }
        if (!isPressureSpawnState()) {
            return;
        }

        List<ServerPlayer> players = getAliveNonDemonPlayersInDimension();
        if (players.isEmpty()) {
            return;
        }

        int requiredNearbyPerPlayer = Math.max(1, Math.min(currentNight, TOTAL_NIGHTS));
        int requiredTotalNearby = requiredNearbyPerPlayer * players.size();
        int currentTotalNearby = countDemonsNearPlayers(players, PLAYER_PRESSURE_RADIUS);
        int missing = requiredTotalNearby - currentTotalNearby;
        if (missing <= 0) {
            return;
        }

        int spawnBudget = Math.min(players.size(), missing);
        for (ServerPlayer player : players) {
            if (spawnBudget <= 0) {
                break;
            }

            ResourceLocation demonId = pickPressureDemonForCurrentNight();
            if (demonId == null) {
                continue;
            }

            EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(demonId);
            if (entityType == null) {
                continue;
            }

            Entity created = entityType.create(level);
            if (!(created instanceof Mob mob)) {
                continue;
            }

            BlockPos spawnPos = findSpawnNearPlayer(player, NON_BOSS_MIN_SPAWN_DISTANCE_FROM_PLAYER, PLAYER_PRESSURE_RADIUS);
            if (spawnPos == null) {
                spawnPos = level.getHeightmapPos(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    player.blockPosition()
                );
            }

            spawnMobNow(mob, spawnPos, false, player);
            spawnBudget--;
        }
    }

    private boolean isPressureSpawnState() {
        return isDemonSpawnState();
    }

    public boolean allowsDemonSpawns() {
        return isDemonSpawnState();
    }

    private boolean isDemonSpawnState() {
        return state == RaidState.NIGHT_ACTIVE
            || state == RaidState.BOSS_PRE_MIDNIGHT
            || state == RaidState.BOSS_MIDNIGHT_HOLD;
    }

    private int countDemonsNearPlayers(List<ServerPlayer> players, int radius) {
        double radiusSq = radius * radius;
        Set<UUID> counted = new HashSet<>();
        for (ServerPlayer player : players) {
            AABB area = player.getBoundingBox().inflate(radius);
            for (Mob mob : level.getEntitiesOfClass(Mob.class, area, Entity::isAlive)) {
                UUID id = mob.getUUID();
                if (counted.contains(id)) {
                    continue;
                }

                if (mob.distanceToSqr(player) > radiusSq) {
                    continue;
                }

                ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
                if (entityId == null || !EntityCategorization.isDemon(entityId)) {
                    continue;
                }

                counted.add(id);
            }
        }
        return counted.size();
    }

    private ResourceLocation pickPressureDemonForCurrentNight() {
        if (currentNight <= 2) {
            return pickOne(EntityPowerScale.EASY_DEMON);
        }
        if (currentNight <= 4) {
            return level.random.nextBoolean() ? pickOne(EntityPowerScale.EASY_DEMON) : pickOne(EntityPowerScale.MEDIUM_DEMON);
        }
        ResourceLocation medium = pickOne(EntityPowerScale.MEDIUM_DEMON);
        if (medium != null) {
            return medium;
        }
        return pickOne(EntityPowerScale.EASY_DEMON);
    }

    private ResourceLocation pickOne(EntityPowerScale scale) {
        List<ResourceLocation> options = EntityCategorization.getEntitiesForScale(scale);
        options = options.stream()
            .filter(id -> !isSwampDemonId(id))
            .toList();
        if (options.isEmpty()) {
            return null;
        }
        return options.get(level.random.nextInt(options.size()));
    }

    public void onEntityKilled(UUID entityId) {
        if (!aliveEntities.contains(entityId)) return;

        aliveEntities.remove(entityId);
        aliveBosses.remove(entityId);

        if (entityId.equals(currentBossEntityId)) {
            currentBossEntityId = null;
        }
    }

    private void sendBossArrowIfNeeded(long gameTime) {
        if (currentBossForArrow == null) return;
        if (gameTime > bossArrowEndTime) return;
        if (gameTime % 5L != 0L) return;

        Entity entity = level.getEntity(currentBossForArrow);
        if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
            return;
        }

        int remaining = (int) Math.max(0L, bossArrowEndTime - gameTime);
        sendBossArrowToDimensionPlayers(remaining);
    }

    private void sendBossArrowToDimensionPlayers(int remainingTicks) {
        Entity entity = currentBossForArrow == null ? null : level.getEntity(currentBossForArrow);
        if (entity == null) return;

        BossArrowPacket packet = new BossArrowPacket(entity.getX(), entity.getY() + 1.0, entity.getZ(), remainingTicks);
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive()) continue;
            ModNetworking.sendToPlayer(packet, player);
        }
    }

    private void updateBossBarPlayers() {
        Set<ServerPlayer> currentPlayers = new HashSet<>();

        for (ServerPlayer player : level.players()) {
            if (!isValidRaidParticipant(player)) {
                bossBar.removePlayer(player);
                continue;
            }

            if (isWithinRaidBoundary(player.getX(), player.getZ())) {
                participants.add(player.getUUID());
                currentPlayers.add(player);
                bossBar.addPlayer(player);
            }
        }

        for (ServerPlayer player : level.players()) {
            if (!currentPlayers.contains(player)) {
                bossBar.removePlayer(player);
            }
        }
    }

    private void updateBossHealthBar() {
        if (currentBossEntityId == null) {
            bossBar.setName(Component.literal("Night " + currentNight));
            bossBar.setProgress(0.5f);
            return;
        }

        Entity entity = level.getEntity(currentBossEntityId);
        if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
            bossBar.setName(Component.literal("Night " + currentNight));
            bossBar.setProgress(0.5f);
            return;
        }

        bossBar.setName(living.getDisplayName());
        bossBar.setColor(BossEvent.BossBarColor.RED);
        bossBar.setProgress(Math.max(0.0f, Math.min(1.0f, living.getHealth() / living.getMaxHealth())));
    }

    private float getPhaseProgress(long gameTime, long duration) {
        if (duration <= 0L) return 1.0f;
        return Math.max(0.0f, Math.min(1.0f, (float) (gameTime - phaseStartGameTime) / (float) duration));
    }

    private long distributeAcrossDuration(long durationTicks, int spawnCount) {
        if (spawnCount <= 0) return 20L;
        return Math.max(20L, durationTicks / spawnCount);
    }

    private int scalePerPlayerSpawnCount(int basePerPlayer, int playerCount) {
        return Math.max(1, basePerPlayer * playerCount * NON_BOSS_PER_PLAYER_SPAWN_MULTIPLIER);
    }

    private long increaseSpawnFrequency(long intervalTicks) {
        return Math.max(8L, (long) Math.ceil(intervalTicks / NON_BOSS_SPAWN_FREQUENCY_MULTIPLIER));
    }

    private long getNightDurationTicks() {
        return FinalSelectionRaidConfig.nightDurationSeconds.get() * 20L;
    }

    private long getDayDurationTicks() {
        return FinalSelectionRaidConfig.dayDurationSeconds.get() * 20L;
    }

    private long getSunriseAccelerationTicks() {
        return FinalSelectionRaidConfig.sunriseAccelerationSeconds.get() * 20L;
    }

    private long getBossEscortRespawnIntervalTicks() {
        return increaseSpawnFrequency(getBaseBossEscortRespawnIntervalTicks());
    }

    private long getBaseBossEscortRespawnIntervalTicks() {
        return FinalSelectionRaidConfig.bossEscortRespawnIntervalSeconds.get() * 20L;
    }

    private long getBossEscortStepTicks() {
        int configured = FinalSelectionRaidConfig.bossEscortSpawnStepSeconds.get();
        long baseStepTicks;
        if (configured > 0) {
            baseStepTicks = configured * 20L;
        } else {
            baseStepTicks = Math.max(20L, getBaseBossEscortRespawnIntervalTicks() / ESCORT_STEPS);
        }
        return increaseSpawnFrequency(baseStepTicks);
    }

    private void setControlledTime(long absoluteDayTime) {
        this.lastControlledAbsoluteTime = absoluteDayTime;
        MtFujikasaneDaylightController.setPausedTime(level, absoluteDayTime);
    }

    private void broadcastToDimension(Component component) {
        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(component);
        }
    }

    private List<ServerPlayer> getAliveNonDemonPlayersInDimension() {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            if (!isValidRaidParticipant(player)) continue;
            out.add(player);
        }
        return out;
    }

    private boolean hasAnyAliveNonDemonPlayerInDimension() {
        return !getAliveNonDemonPlayersInDimension().isEmpty();
    }

    private NightType getNightType(int night) {
        return switch (night) {
            case 1, 2, 5 -> NightType.EASY;
            case 3, 6 -> NightType.MIXED;
            case 4, 7 -> NightType.BOSS;
            default -> NightType.EASY;
        };
    }

    private BlockPos findSpawnNearPlayer() {
        List<ServerPlayer> players = getBoundaryParticipants();
        if (players.isEmpty()) return findCenterSurface();

        ServerPlayer anchor = players.get(level.random.nextInt(players.size()));
        return findSpawnNearPlayer(anchor, NON_BOSS_MIN_SPAWN_DISTANCE_FROM_PLAYER, NON_BOSS_MAX_SPAWN_DISTANCE_FROM_PLAYER);
    }

    private BlockPos findSpawnNearPlayer(ServerPlayer anchor, int minDist, int maxDist) {
        if (anchor == null) {
            return findCenterSurface();
        }

        int clampedMin = Math.max(0, minDist);
        int clampedMax = Math.max(clampedMin, maxDist);

        for (int attempt = 0; attempt < 10; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0;
            int dist = clampedMin + level.random.nextInt(Math.max(1, (clampedMax - clampedMin) + 1));
            int x = (int) Math.round(anchor.getX() + Math.cos(angle) * dist);
            int z = (int) Math.round(anchor.getZ() + Math.sin(angle) * dist);

            BlockPos test = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, anchor.blockPosition().getY(), z));
            if (isValidSpawnPosition(test)) {
                return test;
            }
        }

        return findCenterSurface();
    }

    private List<ServerPlayer> getBoundaryParticipants() {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            if (!isValidRaidParticipant(player)) continue;
            if (!isWithinRaidBoundary(player.getX(), player.getZ())) continue;
            participants.add(player.getUUID());
            out.add(player);
        }
        return out;
    }

    private boolean isValidRaidParticipant(ServerPlayer player) {
        return player != null
            && player.isAlive()
            && !player.getPersistentData().getBoolean("oni")
            && !FinalSelectionProcedure.isPlayerDisqualified(player);
    }

    private boolean isWithinRaidBoundary(double x, double z) {
        boolean inside = false;
        int count = RAID_BOUNDARY_POINTS.size();
        for (int i = 0, j = count - 1; i < count; j = i++) {
            BlockPos pi = RAID_BOUNDARY_POINTS.get(i);
            BlockPos pj = RAID_BOUNDARY_POINTS.get(j);

            if (isPointOnSegmentXZ(x, z, pj.getX(), pj.getZ(), pi.getX(), pi.getZ())) {
                return true;
            }

            double xi = pi.getX();
            double zi = pi.getZ();
            double xj = pj.getX();
            double zj = pj.getZ();

            boolean intersects = ((zi > z) != (zj > z))
                && (x < (xj - xi) * (z - zi) / (zj - zi) + xi);
            if (intersects) {
                inside = !inside;
            }
        }
        return inside;
    }

    public boolean isWithinRaidArea(double x, double z) {
        return isWithinRaidBoundary(x, z);
    }

    private boolean isPointOnSegmentXZ(double px, double pz, double ax, double az, double bx, double bz) {
        double cross = (px - ax) * (bz - az) - (pz - az) * (bx - ax);
        if (Math.abs(cross) > 0.0001D) return false;

        double dot = (px - ax) * (bx - ax) + (pz - az) * (bz - az);
        if (dot < 0.0D) return false;

        double lenSq = (bx - ax) * (bx - ax) + (bz - az) * (bz - az);
        return dot <= lenSq;
    }

    private void renderBoundaryParticlesForNearbyPlayers() {
        double triggerDistSq = BORDER_PARTICLE_TRIGGER_DISTANCE * BORDER_PARTICLE_TRIGGER_DISTANCE;

        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.getPersistentData().getBoolean("oni") || FinalSelectionProcedure.isPlayerDisqualified(player)) {
                continue;
            }

            int closestSegment = -1;
            double closestDistSq = Double.MAX_VALUE;
            int segmentCount = RAID_BOUNDARY_POINTS.size();
            for (int i = 0; i < segmentCount; i++) {
                BlockPos a = RAID_BOUNDARY_POINTS.get(i);
                BlockPos b = RAID_BOUNDARY_POINTS.get((i + 1) % segmentCount);
                double distSq = distanceSqPointToSegmentXZ(player.getX(), player.getZ(), a.getX(), a.getZ(), b.getX(), b.getZ());
                if (distSq < closestDistSq) {
                    closestDistSq = distSq;
                    closestSegment = i;
                }
            }

            if (closestSegment == -1 || closestDistSq > triggerDistSq) {
                continue;
            }

            spawnBoundaryParticlesForSegment(player, closestSegment);
        }
    }

    private double distanceSqPointToSegmentXZ(double px, double pz, double ax, double az, double bx, double bz) {
        double abx = bx - ax;
        double abz = bz - az;
        double apx = px - ax;
        double apz = pz - az;
        double lenSq = abx * abx + abz * abz;
        if (lenSq <= 0.0001D) {
            double dx = px - ax;
            double dz = pz - az;
            return dx * dx + dz * dz;
        }

        double t = (apx * abx + apz * abz) / lenSq;
        t = Math.max(0.0D, Math.min(1.0D, t));
        double cx = ax + abx * t;
        double cz = az + abz * t;
        double dx = px - cx;
        double dz = pz - cz;
        return dx * dx + dz * dz;
    }

    private void spawnBoundaryParticlesForSegment(ServerPlayer player, int segmentIndex) {
        int count = RAID_BOUNDARY_POINTS.size();
        BlockPos a = RAID_BOUNDARY_POINTS.get(segmentIndex);
        BlockPos b = RAID_BOUNDARY_POINTS.get((segmentIndex + 1) % count);
        double dx = b.getX() - a.getX();
        double dz = b.getZ() - a.getZ();
        double length = Math.sqrt(dx * dx + dz * dz);
        int steps = Math.max(1, (int) Math.ceil(length / BORDER_PARTICLE_SEGMENT_STEP));

        for (int step = 0; step <= steps; step++) {
            double t = (double) step / (double) steps;
            double x = a.getX() + dx * t + 0.5D;
            double z = a.getZ() + dz * t + 0.5D;
            BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(x, center.getY(), z));
            double y = surface.getY() + 1.2D;

            level.sendParticles(player, ParticleTypes.END_ROD, true, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private void spawnDaytimePassiveAnimals() {
        if (!FinalSelectionRaidConfig.enableDaytimePassiveAnimals.get()) return;

        int maxPerPlayer = FinalSelectionRaidConfig.daytimePassiveAnimalsPerPlayerMax.get();
        if (maxPerPlayer <= 0) return;

        int radius = FinalSelectionRaidConfig.daytimePassiveAnimalSpawnRadius.get();

        for (ServerPlayer player : getAliveNonDemonPlayersInDimension()) {
            int count = 1 + level.random.nextInt(maxPerPlayer);

            for (int i = 0; i < count; i++) {
                ResourceLocation animalId = DAY_PASSIVE_ANIMALS.get(level.random.nextInt(DAY_PASSIVE_ANIMALS.size()));
                spawnPassiveAnimalNearPlayer(player, animalId, radius);
            }
        }
    }

    private void spawnPassiveAnimalNearPlayer(ServerPlayer player, ResourceLocation animalId, int radius) {
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(animalId);
        if (entityType == null) return;

        Entity entity = entityType.create(level);
        if (!(entity instanceof Mob mob)) return;

        BlockPos spawnPos = null;
        for (int attempt = 0; attempt < 10; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0;
            int dist = 8 + level.random.nextInt(Math.max(4, radius - 8));
            int x = (int) Math.round(player.getX() + Math.cos(angle) * dist);
            int z = (int) Math.round(player.getZ() + Math.sin(angle) * dist);

            BlockPos test = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, player.blockPosition().getY(), z));
            if (isValidSpawnPosition(test)) {
                spawnPos = test;
                break;
            }
        }

        if (spawnPos == null) {
            spawnPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, player.blockPosition());
        }

        mob.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, level.random.nextFloat() * 360.0f, 0.0f);
        mob.setPersistenceRequired();
        level.addFreshEntity(mob);
    }

    private BlockPos findCenterSurface() {
        BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, center);
        return isValidSpawnPosition(surface) ? surface : surface.above();
    }

    private boolean isValidSpawnPosition(BlockPos pos) {
        BlockPos below = pos.below();
        if (!level.getBlockState(below).isSolidRender(level, below)) return false;
        if (!level.getBlockState(pos).isAir()) return false;
        if (!level.getBlockState(pos.above()).isAir()) return false;
        if (level.getBlockState(pos).getFluidState().isSource()) return false;
        return true;
    }

    private ResourceLocation pickFinalSelectionBoss() {
        List<ResourceLocation> available = new ArrayList<>();
        for (ResourceLocation id : FINAL_SELECTION_BOSS_POOL) {
            if (usedBossDemons.contains(id)) continue;
            if (!BuiltInRegistries.ENTITY_TYPE.containsKey(id)) continue;
            if (HAND_DEMON_ID.equals(id) && handDemonSpawned) continue;
            if (isSwampDemonId(id)) continue;
            available.add(id);
        }

        if (available.isEmpty()) {
            return null;
        }

        int totalWeight = 0;
        for (ResourceLocation id : available) {
            totalWeight += getBossWeight(id);
        }

        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        ResourceLocation selected = available.get(0);
        for (ResourceLocation id : available) {
            roll -= getBossWeight(id);
            if (roll < 0) {
                selected = id;
                break;
            }
        }

        usedBossDemons.add(selected);
        if (HAND_DEMON_ID.equals(selected)) {
            handDemonSpawned = true;
        }
        return selected;
    }

    private int getBossWeight(ResourceLocation id) {
        if (HAND_DEMON_ID.equals(id) && !handDemonSpawned) {
            return HAND_DEMON_WEIGHT;
        }
        return 1;
    }

    private List<ResourceLocation> pick(EntityPowerScale scale, int count) {
        List<ResourceLocation> pool = EntityCategorization.getEntitiesForScale(scale);
        pool = pool.stream()
            .filter(id -> !isSwampDemonId(id))
            .toList();
        List<ResourceLocation> out = new ArrayList<>();

        if (pool.isEmpty() || count <= 0) return out;

        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < count; i++) {
            out.add(pool.get(random.nextInt(pool.size())));
        }

        return out;
    }

    private static boolean isSwampDemonId(ResourceLocation id) {
        return BASE_SWAMP_DEMON_ID.equals(id) || CUSTOM_SWAMP_DEMON_ID.equals(id);
    }

    private void completeNightAndEnterDayBreak() {
        if (currentNight >= TOTAL_NIGHTS) {
            triggerVictory();
            return;
        }

        despawnRaidEntities();
        state = RaidState.DAY_BREAK;
        phaseStartGameTime = level.getGameTime();
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
        delayedTasks.clear();
        currentBossEntityId = null;
    }

    private void triggerVictory() {
        if (state == RaidState.VICTORY || state == RaidState.DEFEAT) return;

        state = RaidState.VICTORY;
        bossBar.setColor(BossEvent.BossBarColor.GREEN);
        bossBar.setName(Component.literal("Final Selection Complete"));
        bossBar.setProgress(1.0f);

        broadcastToDimension(Component.literal("Final Selection complete! You survived all seven nights.")
            .withStyle(style -> style.withColor(0x55FF55).withBold(true)));

        sendTitlesToDimension(
            Component.literal("FINAL SELECTION COMPLETE").withStyle(style -> style.withColor(0x55FF55).withBold(true)),
            Component.literal("Seven nights survived")
        );

        MtFujikasaneDaylightController.resumeDaylightCycle(level);
    }

    private void triggerDefeat(String reason) {
        if (state == RaidState.VICTORY || state == RaidState.DEFEAT) return;

        state = RaidState.DEFEAT;
        bossBar.setColor(BossEvent.BossBarColor.WHITE);
        bossBar.setName(Component.literal("Final Selection Failed"));
        bossBar.setProgress(0.0f);

        broadcastToDimension(Component.literal("Final Selection failed: " + reason)
            .withStyle(style -> style.withColor(0xFF5555)));

        sendTitlesToDimension(
            Component.literal("FINAL SELECTION FAILED").withStyle(style -> style.withColor(0xFF5555).withBold(true)),
            Component.literal(reason)
        );

        despawnRaidEntities();
        MtFujikasaneDaylightController.resumeDaylightCycle(level);
    }

    private void sendTitlesToDimension(Component title, Component subtitle) {
        for (ServerPlayer player : level.players()) {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
            player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
            player.connection.send(new ClientboundSetTitleTextPacket(title));
        }
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("RaidId", raidId);
        tag.putInt("CenterX", center.getX());
        tag.putInt("CenterY", center.getY());
        tag.putInt("CenterZ", center.getZ());
        tag.putInt("RaidRadius", raidRadius);
        tag.putString("State", state.name());
        tag.putInt("CurrentNight", currentNight);
        tag.putString("CurrentNightType", currentNightType.name());
        tag.putLong("SpawnIntervalTicks", spawnIntervalTicks);
        tag.putLong("NextSpawnTime", nextSpawnTime);
        tag.putLong("BossArrowEndTime", bossArrowEndTime);
        tag.putLong("PhaseStartGameTime", phaseStartGameTime);
        tag.putLong("NightStartAbsoluteTime", nightStartAbsoluteTime);
        tag.putLong("LastControlledAbsoluteTime", lastControlledAbsoluteTime);
        tag.putLong("NextEscortWaveStartTime", nextEscortWaveStartTime);
        tag.putInt("EscortWaveStep", escortWaveStep);
        tag.putLong("NextEscortWaveStepTime", nextEscortWaveStepTime);
        tag.putInt("DayPassiveAnimalWavesSpawned", dayPassiveAnimalWavesSpawned);
        tag.putLong("NextDayPassiveAnimalWaveTime", nextDayPassiveAnimalWaveTime);
        tag.putBoolean("CleanupDone", cleanupDone);
        tag.putBoolean("HandDemonSpawned", handDemonSpawned);

        if (currentBossForArrow != null) tag.putUUID("CurrentBossForArrow", currentBossForArrow);
        if (currentBossEntityId != null) tag.putUUID("CurrentBossEntityId", currentBossEntityId);

        ListTag pending = new ListTag();
        for (ResourceLocation id : pendingSpawns) {
            pending.add(net.minecraft.nbt.StringTag.valueOf(id.toString()));
        }
        tag.put("PendingSpawns", pending);

        ListTag participantTag = new ListTag();
        for (UUID id : participants) {
            participantTag.add(net.minecraft.nbt.StringTag.valueOf(id.toString()));
        }
        tag.put("Participants", participantTag);

        ListTag all = new ListTag();
        for (UUID id : allRaidEntities) all.add(net.minecraft.nbt.StringTag.valueOf(id.toString()));
        tag.put("AllRaidEntities", all);

        ListTag alive = new ListTag();
        for (UUID id : aliveEntities) alive.add(net.minecraft.nbt.StringTag.valueOf(id.toString()));
        tag.put("AliveEntities", alive);

        ListTag aliveBoss = new ListTag();
        for (UUID id : aliveBosses) aliveBoss.add(net.minecraft.nbt.StringTag.valueOf(id.toString()));
        tag.put("AliveBosses", aliveBoss);

        ListTag usedBosses = new ListTag();
        for (ResourceLocation id : usedBossDemons) {
            usedBosses.add(net.minecraft.nbt.StringTag.valueOf(id.toString()));
        }
        tag.put("UsedBossDemons", usedBosses);

        ListTag delayed = new ListTag();
        for (DelayedSpawnTask task : delayedTasks) {
            CompoundTag taskTag = new CompoundTag();
            taskTag.putString("EntityId", task.entityId.toString());
            taskTag.putInt("X", task.pos.getX());
            taskTag.putInt("Y", task.pos.getY());
            taskTag.putInt("Z", task.pos.getZ());
            taskTag.putBoolean("Boss", task.boss);
            taskTag.putLong("ExecuteAt", task.executeAt);
            delayed.add(taskTag);
        }
        tag.put("DelayedTasks", delayed);

        return tag;
    }

    public static FinalSelectionRaid fromTag(ServerLevel level, CompoundTag tag) {
        if (tag == null || !tag.contains("RaidId")) {
            return null;
        }

        UUID raidId = tag.getUUID("RaidId");
        BlockPos center = new BlockPos(tag.getInt("CenterX"), tag.getInt("CenterY"), tag.getInt("CenterZ"));
        int raidRadius = tag.getInt("RaidRadius");

        FinalSelectionRaid raid = new FinalSelectionRaid(level, center, raidRadius, raidId, false);

        raid.state = parseEnum(tag.getString("State"), RaidState.PREPARING, RaidState.class);
        raid.currentNight = Math.max(0, tag.getInt("CurrentNight"));
        raid.currentNightType = parseEnum(tag.getString("CurrentNightType"), NightType.EASY, NightType.class);
        raid.spawnIntervalTicks = tag.getLong("SpawnIntervalTicks");
        raid.nextSpawnTime = tag.getLong("NextSpawnTime");
        raid.bossArrowEndTime = tag.getLong("BossArrowEndTime");
        raid.phaseStartGameTime = tag.getLong("PhaseStartGameTime");
        raid.nightStartAbsoluteTime = tag.getLong("NightStartAbsoluteTime");
        raid.lastControlledAbsoluteTime = tag.getLong("LastControlledAbsoluteTime");
        raid.nextEscortWaveStartTime = tag.getLong("NextEscortWaveStartTime");
        raid.escortWaveStep = tag.getInt("EscortWaveStep");
        raid.nextEscortWaveStepTime = tag.getLong("NextEscortWaveStepTime");
        raid.dayPassiveAnimalWavesSpawned = tag.getInt("DayPassiveAnimalWavesSpawned");
        raid.nextDayPassiveAnimalWaveTime = tag.getLong("NextDayPassiveAnimalWaveTime");
        raid.cleanupDone = tag.getBoolean("CleanupDone");
        raid.handDemonSpawned = tag.getBoolean("HandDemonSpawned");

        if (tag.hasUUID("CurrentBossForArrow")) raid.currentBossForArrow = tag.getUUID("CurrentBossForArrow");
        if (tag.hasUUID("CurrentBossEntityId")) raid.currentBossEntityId = tag.getUUID("CurrentBossEntityId");

        raid.pendingSpawns.clear();
        ListTag pending = tag.getList("PendingSpawns", Tag.TAG_STRING);
        for (int i = 0; i < pending.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(pending.getString(i));
            if (id != null) raid.pendingSpawns.add(id);
        }

        raid.participants.clear();
        ListTag participants = tag.getList("Participants", Tag.TAG_STRING);
        for (int i = 0; i < participants.size(); i++) {
            try {
                raid.participants.add(UUID.fromString(participants.getString(i)));
            } catch (Exception ignored) {}
        }

        raid.allRaidEntities.clear();
        ListTag all = tag.getList("AllRaidEntities", Tag.TAG_STRING);
        for (int i = 0; i < all.size(); i++) {
            try {
                raid.allRaidEntities.add(UUID.fromString(all.getString(i)));
            } catch (Exception ignored) {}
        }

        raid.aliveEntities.clear();
        ListTag alive = tag.getList("AliveEntities", Tag.TAG_STRING);
        for (int i = 0; i < alive.size(); i++) {
            try {
                raid.aliveEntities.add(UUID.fromString(alive.getString(i)));
            } catch (Exception ignored) {}
        }

        raid.aliveBosses.clear();
        ListTag aliveBoss = tag.getList("AliveBosses", Tag.TAG_STRING);
        for (int i = 0; i < aliveBoss.size(); i++) {
            try {
                raid.aliveBosses.add(UUID.fromString(aliveBoss.getString(i)));
            } catch (Exception ignored) {}
        }

        raid.usedBossDemons.clear();
        ListTag usedBosses = tag.getList("UsedBossDemons", Tag.TAG_STRING);
        for (int i = 0; i < usedBosses.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(usedBosses.getString(i));
            if (id != null) raid.usedBossDemons.add(id);
        }

        raid.delayedTasks.clear();
        ListTag delayed = tag.getList("DelayedTasks", Tag.TAG_COMPOUND);
        for (int i = 0; i < delayed.size(); i++) {
            CompoundTag taskTag = delayed.getCompound(i);
            ResourceLocation entityId = ResourceLocation.tryParse(taskTag.getString("EntityId"));
            if (entityId == null) continue;
            BlockPos pos = new BlockPos(taskTag.getInt("X"), taskTag.getInt("Y"), taskTag.getInt("Z"));
            raid.delayedTasks.add(new DelayedSpawnTask(entityId, pos, taskTag.getBoolean("Boss"), taskTag.getLong("ExecuteAt")));
        }

        if (raid.lastControlledAbsoluteTime != Long.MIN_VALUE) {
            MtFujikasaneDaylightController.pauseDaylightCycle(level);
            MtFujikasaneDaylightController.setPausedTime(level, raid.lastControlledAbsoluteTime);
        }

        raid.refreshBossBarForState();
        return raid;
    }

    private void refreshBossBarForState() {
        switch (state) {
            case NIGHT_ACTIVE, BOSS_PRE_MIDNIGHT -> {
                bossBar.setName(Component.literal("Night " + Math.max(1, currentNight)));
                bossBar.setColor(BossEvent.BossBarColor.PURPLE);
            }
            case BOSS_MIDNIGHT_HOLD -> updateBossHealthBar();
            case BOSS_SUNRISE_ACCEL -> {
                bossBar.setName(Component.literal("Night " + Math.max(1, currentNight)));
                bossBar.setColor(BossEvent.BossBarColor.YELLOW);
            }
            case DAY_BREAK -> {
                int dayLabel = Math.min(7, Math.max(1, currentNight + 1));
                bossBar.setName(Component.literal("Day " + dayLabel));
                bossBar.setColor(BossEvent.BossBarColor.BLUE);
            }
            case VICTORY -> {
                bossBar.setName(Component.literal("Final Selection Complete"));
                bossBar.setColor(BossEvent.BossBarColor.GREEN);
            }
            case DEFEAT -> {
                bossBar.setName(Component.literal("Final Selection Failed"));
                bossBar.setColor(BossEvent.BossBarColor.WHITE);
            }
            default -> {
                bossBar.setName(Component.literal("Night " + Math.max(1, currentNight)));
                bossBar.setColor(BossEvent.BossBarColor.PURPLE);
            }
        }
    }

    private static <E extends Enum<E>> E parseEnum(String value, E fallback, Class<E> enumClass) {
        try {
            return Enum.valueOf(enumClass, value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public void stop(String reason) {
        triggerDefeat(reason != null ? reason : "Raid stopped");
    }

    public void cleanup() {
        despawnRaidEntities();
        bossBar.removeAllPlayers();
        cleanupDone = true;
    }

    public boolean isFinished() {
        return state == RaidState.VICTORY || state == RaidState.DEFEAT;
    }

    public boolean wasSuccessful() {
        return state == RaidState.VICTORY;
    }

    public boolean isRaidEntity(UUID entityId) {
        return allRaidEntities.contains(entityId);
    }

    public int getCurrentNight() {
        return Math.max(1, Math.min(TOTAL_NIGHTS, currentNight));
    }
}
