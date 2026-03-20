package com.lerdorf.kimetsunoyaibamultiplayer.raids;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * In-memory registry for survival raids. One raid per dimension.
 */
public class SurvivalRaidRegistry {
    private static final Map<ResourceKey<Level>, SurvivalRaid> ACTIVE_RAIDS = new HashMap<>();

    private SurvivalRaidRegistry() {
    }

    public static SurvivalRaid createRaid(ServerLevel level, BlockPos center, int radius, int difficultyLevel) {
        ResourceKey<Level> key = level.dimension();

        SurvivalRaid existing = ACTIVE_RAIDS.get(key);
        if (existing != null && !existing.isFinished()) {
            return existing;
        }

        SurvivalRaid raid = new SurvivalRaid(level, center, radius, difficultyLevel);
        ACTIVE_RAIDS.put(key, raid);
        Log.debug("Created survival raid in {} at {}", key.location(), center);
        return raid;
    }

    public static SurvivalRaid getRaid(ServerLevel level) {
        SurvivalRaid raid = ACTIVE_RAIDS.get(level.dimension());
        if (raid == null) return null;
        if (raid.isFinished()) {
            ACTIVE_RAIDS.remove(level.dimension());
            return null;
        }
        return raid;
    }

    public static void tickAll(ServerLevel level) {
        SurvivalRaid raid = ACTIVE_RAIDS.get(level.dimension());
        if (raid == null) return;

        raid.tick();
        if (raid.isFinished()) {
            ACTIVE_RAIDS.remove(level.dimension());
        }
    }

    public static boolean stopRaid(ServerLevel level, String reason) {
        SurvivalRaid raid = ACTIVE_RAIDS.remove(level.dimension());
        if (raid == null) return false;

        raid.stop(reason != null ? reason : "Stopped by command");
        return true;
    }

    public static void onEntityKilled(ServerLevel level, UUID entityId) {
        SurvivalRaid raid = ACTIVE_RAIDS.get(level.dimension());
        if (raid == null) return;

        raid.onEntityKilled(entityId);
    }

    public static boolean isRaidEntity(ServerLevel level, UUID entityId) {
        SurvivalRaid raid = ACTIVE_RAIDS.get(level.dimension());
        return raid != null && raid.isRaidEntity(entityId);
    }

    public static boolean hasRaidNearby(ServerLevel level, BlockPos pos, int radius) {
        SurvivalRaid raid = ACTIVE_RAIDS.get(level.dimension());
        if (raid == null) return false;
        return raid.getCenter().distSqr(pos) <= (long) radius * radius;
    }
}
