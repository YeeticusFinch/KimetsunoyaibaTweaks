package com.lerdorf.kimetsunoyaibamultiplayer.structures;

import com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedSpawnConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.MaxEntityTracker;
import com.lerdorf.kimetsunoyaibamultiplayer.raids.StructureLocationCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Ensures key structure "residents" exist (e.g., Doma in temple_doma).
 *
 * This runs when any entity joins a level; if the location is inside a
 * structure that should have a unique resident but none is present, the
 * resident is spawned at the structure center. Spawns are throttled to
 * avoid repeats.
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class StructureResidentSpawner {

    private static final long RESPAWN_COOLDOWN_MS = 60_000; // 60s throttle

    // Key: dimensionId + ":" + structureId + ":" + centerPos
    private static final Map<String, Long> lastSpawnTime = new HashMap<>();

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!EnhancedSpawnConfig.enhancedSpawningRules) {
            return;
        }

        LevelAccessor accessor = event.getLevel();
        Entity e = event.getEntity();
        if (!(accessor instanceof ServerLevel level) || !(e instanceof Mob)) {
            return;
        }

        BlockPos pos = e.blockPosition();

        Optional<StructureLocationCache.CachedStructure> cachedOpt = StructureLocationCache.getStructureAt(level, pos);
        if (cachedOpt.isEmpty()) return;

        StructureLocationCache.CachedStructure cached = cachedOpt.get();
        String structurePath = cached.structureId.getPath();

        // Currently enforce Doma in temple_doma only
        if ("temple_doma".equals(structurePath)) {
            ensureDomaPresent(level, cached.structureId, cached.center);
        }
    }

    private static void ensureDomaPresent(ServerLevel level, ResourceLocation structureId, BlockPos center) {
        // Throttle per structure center
        String key = level.dimension().location() + ":" + structureId + ":" + center.getX() + "," + center.getY() + "," + center.getZ();
        long now = System.currentTimeMillis();
        if (lastSpawnTime.getOrDefault(key, 0L) + RESPAWN_COOLDOWN_MS > now) {
            return;
        }

        // Check if a doma already exists nearby (include replacers)
        EntityType<?> domaType = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "doma"));
        if (domaType != null && MaxEntityTracker.entityTypeExists(level, center, domaType, 150)) {
            return; // Already present
        }

        // Shade allowance: if it's day, require shade; night is fine
        if (level.isDay()) {
            if (level.canSeeSky(center)) {
                int sky = level.getBrightness(LightLayer.SKY, center);
                if (sky >= 12) {
                    return; // Too bright to spawn safely
                }
            }
        }

        // Spawn Doma at structure center (or slightly offset to valid ground)
        if (domaType != null) {
            Entity doma = domaType.create(level);
            if (doma != null) {
                BlockPos spawn = findSpawnablePosition(level, center);
                doma.moveTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, level.random.nextFloat() * 360F, 0);
                level.addFreshEntity(doma);
                lastSpawnTime.put(key, now);
            }
        }
    }

    private static BlockPos findSpawnablePosition(ServerLevel level, BlockPos origin) {
        // Simple search: try center, then small offsets
        int[] offsets = {0, 1, -1, 2, -2, 3, -3};
        for (int dx : offsets) {
            for (int dz : offsets) {
                BlockPos p = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        origin.offset(dx, 0, dz));
                if (p.getY() > level.getMinBuildHeight()) {
                    return p;
                }
            }
        }
        return origin;
    }
}

