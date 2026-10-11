package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedSpawnConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.raids.RaidRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.util.EntityTagHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.server.ServerStoppedEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Spawns delayed, capped protection when civilians witness a demon threat. */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CivilianProtectionHandler {
    private static final RandomSource RNG = RandomSource.create();
    private static final int PROTECTIVE_SPAWN_RADIUS = 50;
    private static final int PROTECTIVE_COUNT_RADIUS = 100;
    private static final Map<UUID, Long> PROTECTION_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, PendingProtection> PENDING_PROTECTIONS = new HashMap<>();

    @SubscribeEvent
    public static void onCivilianAttacked(LivingAttackEvent event) {
        try {
            handleCivilianAttacked(event);
        } catch (RuntimeException exception) {
            System.err.println("[Civilian Protection] Failed to handle civilian attack: " + exception.getMessage());
        }
    }

    private static void handleCivilianAttacked(LivingAttackEvent event) {
        if (!isEnabled()) {
            return;
        }

        LivingEntity target = event.getEntity();
        if (!EntityTagHelper.isCivilian(target)) {
            return;
        }

        LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity living ? living : null;
        if (attacker == null || !isDemonThreat(attacker) || !wasWitnessedByAwakeCivilian(target, attacker)) {
            return;
        }

        CivilianDetectionNotifier.notifyDemonSpotted(attacker);
        scheduleProtection(target, attacker);
    }

    @SubscribeEvent
    public static void onCivilianTargeted(LivingChangeTargetEvent event) {
        try {
            handleCivilianTargeted(event);
        } catch (RuntimeException exception) {
            System.err.println("[Civilian Protection] Failed to handle civilian target change: " + exception.getMessage());
        }
    }

    private static void handleCivilianTargeted(LivingChangeTargetEvent event) {
        if (!isEnabled()) {
            return;
        }

        LivingEntity attacker = event.getEntity();
        LivingEntity target = event.getNewTarget();
        if (target == null || !EntityTagHelper.isCivilian(target) || !isDemonThreat(attacker)
            || !wasWitnessedByAwakeCivilian(target, attacker)) {
            return;
        }

        CivilianDetectionNotifier.notifyDemonSpotted(attacker);
        scheduleProtection(target, attacker);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        try {
            processPendingProtections(event);
        } catch (RuntimeException exception) {
            System.err.println("[Civilian Protection] Failed to process pending protection: " + exception.getMessage());
        }
    }

    private static void processPendingProtections(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING_PROTECTIONS.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<UUID, PendingProtection>> iterator = PENDING_PROTECTIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            PendingProtection pending = iterator.next().getValue();
            if (pending.level().getServer() != event.getServer()
                || pending.level().getGameTime() < pending.spawnAt()) {
                continue;
            }

            iterator.remove();
            Entity entity = pending.level().getEntity(pending.civilianId());
            if (entity instanceof LivingEntity civilian && civilian.isAlive()) {
                spawnProtection(pending.level(), civilian, pending.isDemon(), pending.isKizuki());
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        try {
            PENDING_PROTECTIONS.clear();
            PROTECTION_COOLDOWNS.clear();
        } catch (RuntimeException exception) {
            System.err.println("[Civilian Protection] Failed to clear protection state: " + exception.getMessage());
        }
    }

    private static boolean isEnabled() {
        return EnhancedSpawnConfig.enhancedSpawningRules && EnhancedSpawnConfig.enableProtectiveSpawning;
    }

    private static void scheduleProtection(LivingEntity civilian, LivingEntity threat) {
        if (!(civilian.level() instanceof ServerLevel level) || RaidRegistry.hasRaidNearby(level, civilian.blockPosition(), 200)) {
            return;
        }

        UUID civilianId = civilian.getUUID();
        long now = level.getGameTime();
        if (PENDING_PROTECTIONS.containsKey(civilianId)
            || now < PROTECTION_COOLDOWNS.getOrDefault(civilianId, Long.MIN_VALUE)
            || countNearbyDemonSlayers(level, civilian.blockPosition(), PROTECTIVE_COUNT_RADIUS)
                >= EnhancedSpawnConfig.maxNearbyProtectiveSlayers) {
            return;
        }

        boolean isKizuki = EntityTagHelper.isTwelveKizuki(threat);
        boolean isDemon = EntityTagHelper.isDemon(threat);
        if (isDemon && !isKizuki && RNG.nextDouble() >= EnhancedSpawnConfig.demonSlayerSpawnChance) {
            return;
        }

        PROTECTION_COOLDOWNS.put(civilianId, now + EnhancedSpawnConfig.protectiveSpawnCooldownTicks);
        PENDING_PROTECTIONS.put(civilianId, new PendingProtection(
            level,
            civilianId,
            now + EnhancedSpawnConfig.protectiveSpawnDelayTicks,
            isDemon,
            isKizuki
        ));
    }

    private static void spawnProtection(ServerLevel level, LivingEntity civilian, boolean isDemon, boolean isKizuki) {
        BlockPos center = civilian.blockPosition();
        if (RaidRegistry.hasRaidNearby(level, center, 200)) {
            return;
        }

        if (isKizuki) {
            if (RNG.nextDouble() < EnhancedSpawnConfig.hashiraSpawnChance) {
                spawnById(level, center, pickRandomHashiraId());
            }
            if (RNG.nextDouble() < EnhancedSpawnConfig.hashiraSpawnChance) {
                spawnById(level, center, ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "nezuko"));
            }
            if (RNG.nextDouble() < EnhancedSpawnConfig.kamabokoSpawnChance) {
                spawnById(level, center, ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", pickRandomKamaboko()));
            }
        } else if (isDemon) {
            int groupSize = clamp(
                EnhancedSpawnConfig.demonSlayerGroupSizeMin,
                EnhancedSpawnConfig.demonSlayerGroupSizeMax,
                2
            );
            for (int i = 0; i < groupSize; i++) {
                if (countNearbyDemonSlayers(level, center, PROTECTIVE_COUNT_RADIUS)
                    >= EnhancedSpawnConfig.maxNearbyProtectiveSlayers) {
                    break;
                }
                spawnById(level, center, ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", pickRandomDemonSlayer()));
            }
        }
    }

    private static int countNearbyDemonSlayers(ServerLevel level, BlockPos center, int radius) {
        double centerX = center.getX() + 0.5D;
        double centerY = center.getY() + 0.5D;
        double centerZ = center.getZ() + 0.5D;
        double radiusSqr = radius * (double) radius;
        AABB searchBox = new AABB(center).inflate(radius);
        List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(
            LivingEntity.class,
            searchBox,
            entity -> entity != null
                && entity.isAlive()
                && entity.distanceToSqr(centerX, centerY, centerZ) <= radiusSqr
                && (EntityTagHelper.isDemonSlayer(entity)
                    || EntityTagHelper.isHashira(entity)
                    || EntityTagHelper.isKamaboko(entity))
        );
        return nearbyEntities.size();
    }

    private static boolean spawnById(ServerLevel level, BlockPos center, ResourceLocation id) {
        if (id == null || countNearbyDemonSlayers(level, center, PROTECTIVE_COUNT_RADIUS)
            >= EnhancedSpawnConfig.maxNearbyProtectiveSlayers) {
            return false;
        }

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
        if (type == null || !(type.create(level) instanceof Mob mob)) {
            return false;
        }

        BlockPos spawnPos = findValidSpawnPosition(level, center);
        if (spawnPos == null) {
            return false;
        }

        mob.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D,
            level.random.nextFloat() * 360.0F, 0.0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.MOB_SUMMONED, null, null);
        level.addFreshEntity(mob);
        return true;
    }

    private static BlockPos findValidSpawnPosition(ServerLevel level, BlockPos center) {
        for (int attempt = 0; attempt < 16; attempt++) {
            double angle = RNG.nextDouble() * Math.PI * 2.0D;
            int distance = PROTECTIVE_SPAWN_RADIUS + RNG.nextInt(11) - 5;
            BlockPos candidate = center.offset(
                (int) Math.round(Math.cos(angle) * distance),
                0,
                (int) Math.round(Math.sin(angle) * distance)
            );
            BlockPos spawnPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, candidate);
            if (level.getBlockState(spawnPos.below()).isSolid()
                && level.getBlockState(spawnPos).isAir()
                && level.getBlockState(spawnPos.above()).isAir()) {
                return spawnPos;
            }
        }
        return null;
    }

    private static int clamp(int min, int max, int fallback) {
        if (min > max) {
            return fallback;
        }
        return min + RNG.nextInt(max - min + 1);
    }

    private static ResourceLocation pickRandomHashiraId() {
        String[] options = {"kocho", "kanroji", "kanae", "shinazugawa", "rengoku", "iguro", "uzui", "tomioka", "muichirou", "himejima"};
        ResourceLocation baseId = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", options[RNG.nextInt(options.length)]);
        return com.lerdorf.kimetsunoyaibamultiplayer.raids.EntitySpawnHelper.filterForProtectiveSpawning(baseId);
    }

    private static String pickRandomDemonSlayer() {
        String[] options = {"dice_steak_senior", "murata", "sabito", "makomo"};
        return options[RNG.nextInt(options.length)];
    }

    private static String pickRandomKamaboko() {
        String[] options = {"genya", "inosuke", "tanjiro", "zennitsu", "kanawo"};
        return options[RNG.nextInt(options.length)];
    }

    private static boolean isDemonThreat(LivingEntity entity) {
        return EntityTagHelper.isDemon(entity)
            || EntityTagHelper.isTwelveKizuki(entity)
            || (entity instanceof Player player && player.getPersistentData().getBoolean("oni"));
    }

    private static boolean wasWitnessedByAwakeCivilian(LivingEntity victim, LivingEntity attacker) {
        if (!(victim.level() instanceof ServerLevel level)) {
            return false;
        }

        List<LivingEntity> witnesses = level.getEntitiesOfClass(
            LivingEntity.class,
            victim.getBoundingBox().inflate(24.0D),
            entity -> entity != null
                && entity != victim
                && entity.isAlive()
                && !entity.isSleeping()
                && EntityTagHelper.isCivilian(entity)
        );
        for (LivingEntity witness : witnesses) {
            if (witness.hasLineOfSight(victim) && witness.hasLineOfSight(attacker)) {
                return true;
            }
        }
        return false;
    }

    private record PendingProtection(ServerLevel level, UUID civilianId, long spawnAt, boolean isDemon, boolean isKizuki) {
    }
}
