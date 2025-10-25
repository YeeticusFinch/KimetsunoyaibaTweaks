package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedSpawnConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.function.Predicate;

/**
 * Utility class for tracking entity limits in specific areas.
 *
 * Provides methods to check if maximum entity counts are reached
 * within specific radii, biomes, or structures. This prevents
 * over-spawning of unique entities like the Rui family or twelve kizuki.
 */
public class MaxEntityTracker {

    /**
     * Check if there are too many entities of a specific type within a radius
     *
     * @param level The level/world
     * @param pos The center position
     * @param entityType The entity type to check for
     * @param radius The radius to check within
     * @param maxCount The maximum number allowed
     * @param includeReplacers If true, also check for replacer entities
     * @return True if max count is reached or exceeded
     */
    public static boolean isMaxReached(Level level, BlockPos pos, EntityType<?> entityType,
                                      double radius, int maxCount, boolean includeReplacers) {
        if (level == null || pos == null || entityType == null) {
            return false;
        }

        int count = countEntitiesInRadius(level, pos, entity -> {
            if (entity.getType().equals(entityType)) {
                return true;
            }

            // Check for replacer entities if enabled
            if (includeReplacers) {
                ResourceLocation entityTypeId = EntityType.getKey(entity.getType());
                ResourceLocation targetTypeId = EntityType.getKey(entityType);

                // Check if the entity is a replacer for the target type
                ResourceLocation replacementId = EntityReplacerHandler.getReplacementId(targetTypeId);
                if (replacementId != null && replacementId.equals(entityTypeId)) {
                    return true;
                }

                // Check if the target is a replacer for the entity type
                ResourceLocation reverseReplacementId = EntityReplacerHandler.getReplacementId(entityTypeId);
                if (reverseReplacementId != null && reverseReplacementId.equals(targetTypeId)) {
                    return true;
                }
            }

            return false;
        }, radius);

        return count >= maxCount;
    }

    /**
     * Check if max entities are reached (using default radius from config)
     */
    public static boolean isMaxReached(Level level, BlockPos pos, EntityType<?> entityType,
                                      int maxCount, boolean includeReplacers) {
        return isMaxReached(level, pos, entityType,
            EnhancedSpawnConfig.maxEntityCheckRadius, maxCount, includeReplacers);
    }

    /**
     * Check if max entities of multiple types are reached within a radius
     * (e.g., checking for both kimetsunoyaiba:akaza and kimetsu:akaza)
     */
    public static boolean isMaxReachedMultiType(Level level, BlockPos pos, List<EntityType<?>> entityTypes,
                                               double radius, int maxCount) {
        if (level == null || pos == null || entityTypes == null || entityTypes.isEmpty()) {
            return false;
        }

        int totalCount = 0;
        for (EntityType<?> entityType : entityTypes) {
            int count = countEntitiesInRadius(level, pos, entity -> entity.getType().equals(entityType), radius);
            totalCount += count;
        }

        return totalCount >= maxCount;
    }

    /**
     * Count entities matching a predicate within a radius
     */
    public static int countEntitiesInRadius(Level level, BlockPos pos, Predicate<Entity> filter, double radius) {
        if (level == null || pos == null || filter == null) {
            return 0;
        }

        // Create bounding box for the search area
        AABB searchBox = new AABB(pos).inflate(radius);

        // Get all entities in the area
        List<Entity> entities = level.getEntities((Entity) null, searchBox, filter);

        return entities.size();
    }

    /**
     * Check if max entities are reached within a specific biome
     */
    public static boolean isMaxReachedInBiome(Level level, BlockPos pos, EntityType<?> entityType,
                                             ResourceLocation biomeId, int maxCount, boolean includeReplacers) {
        if (level == null || pos == null || entityType == null || biomeId == null) {
            return false;
        }

        // Use a larger search radius for biome-based checks
        double searchRadius = EnhancedSpawnConfig.maxEntityCheckRadius * 2.0;

        int count = countEntitiesInRadius(level, pos, entity -> {
            // Check entity type
            boolean typeMatches = entity.getType().equals(entityType);

            if (!typeMatches && includeReplacers) {
                ResourceLocation entityTypeId = EntityType.getKey(entity.getType());
                ResourceLocation targetTypeId = EntityType.getKey(entityType);

                ResourceLocation replacementId = EntityReplacerHandler.getReplacementId(targetTypeId);
                if (replacementId != null && replacementId.equals(entityTypeId)) {
                    typeMatches = true;
                }
            }

            if (!typeMatches) {
                return false;
            }

            // Check if entity is in the target biome
            Holder<Biome> entityBiome = entity.level().getBiome(entity.blockPosition());
            ResourceLocation entityBiomeId = entityBiome.unwrapKey()
                .map(key -> key.location())
                .orElse(null);

            return biomeId.equals(entityBiomeId);

        }, searchRadius);

        return count >= maxCount;
    }

    /**
     * Count entities of a specific type within a radius (simple version)
     */
    public static int countEntityType(Level level, BlockPos pos, EntityType<?> entityType, double radius) {
        return countEntitiesInRadius(level, pos, entity -> entity.getType().equals(entityType), radius);
    }

    /**
     * Check if ANY entity matching the predicate exists within radius
     */
    public static boolean anyEntityExists(Level level, BlockPos pos, Predicate<Entity> filter, double radius) {
        return countEntitiesInRadius(level, pos, filter, radius) > 0;
    }

    /**
     * Check if a specific entity type exists within radius
     */
    public static boolean entityTypeExists(Level level, BlockPos pos, EntityType<?> entityType, double radius) {
        return countEntityType(level, pos, entityType, radius) > 0;
    }

    /**
     * Check if entities of multiple types exist within radius (OR logic)
     */
    public static boolean anyEntityTypeExists(Level level, BlockPos pos, List<EntityType<?>> entityTypes, double radius) {
        for (EntityType<?> entityType : entityTypes) {
            if (entityTypeExists(level, pos, entityType, radius)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get entities matching predicate within radius
     */
    public static List<Entity> getEntitiesInRadius(Level level, BlockPos pos, Predicate<Entity> filter, double radius) {
        if (level == null || pos == null || filter == null) {
            return List.of();
        }

        AABB searchBox = new AABB(pos).inflate(radius);
        return level.getEntities((Entity) null, searchBox, filter);
    }

    /**
     * Get entities of a specific type within radius
     */
    public static List<Entity> getEntitiesOfType(Level level, BlockPos pos, EntityType<?> entityType, double radius) {
        return getEntitiesInRadius(level, pos, entity -> entity.getType().equals(entityType), radius);
    }
}
