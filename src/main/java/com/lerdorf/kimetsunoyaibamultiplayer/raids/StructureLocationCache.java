package com.lerdorf.kimetsunoyaibamultiplayer.raids;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Caches structure locations for performance optimization.
 *
 * Structure lookups are expensive, so we cache them to avoid
 * repeated searches during raid triggering and spawning.
 *
 * Cache is cleared when a player changes dimensions or after
 * a configurable time period.
 */
public class StructureLocationCache {

    /**
     * Cache entry containing structure information.
     */
    public static class CachedStructure {
        public final ResourceLocation structureId;
        public final BlockPos center;
        public final BlockPos corner;
        public final Rotation rotation;
        public final long cacheTime;

        public CachedStructure(ResourceLocation structureId, BlockPos center, BlockPos corner, Rotation rotation) {
            this.structureId = structureId;
            this.center = center;
            this.corner = corner;
            this.rotation = rotation;
            this.cacheTime = System.currentTimeMillis();
        }

        /**
         * Check if this cache entry is still valid.
         *
         * @param maxAgeMs Maximum age in milliseconds
         * @return true if cache is still valid
         */
        public boolean isValid(long maxAgeMs) {
            return (System.currentTimeMillis() - cacheTime) < maxAgeMs;
        }
    }

    // Cache duration in milliseconds (5 minutes)
    private static final long CACHE_DURATION_MS = 5 * 60 * 1000;

    // Per-dimension cache: position -> structure info
    private static final Map<ResourceLocation, Map<BlockPos, CachedStructure>> DIMENSION_CACHES = new HashMap<>();
    private static final Map<ResourceLocation, Map<BlockPos, Long>> NEGATIVE_CACHES = new HashMap<>();

    /**
     * Find the structure at a given position (with caching).
     *
     * @param level The server level
     * @param pos   The position to check
     * @return Optional containing the cached structure, or empty if none found
     */
    public static Optional<CachedStructure> getStructureAt(ServerLevel level, BlockPos pos) {
        ResourceLocation dimensionId = level.dimension().location();

        // Get or create dimension cache
        Map<BlockPos, CachedStructure> dimensionCache = DIMENSION_CACHES.computeIfAbsent(
            dimensionId, k -> new HashMap<>()
        );
        Map<BlockPos, Long> negativeCache = NEGATIVE_CACHES.computeIfAbsent(
            dimensionId, k -> new HashMap<>()
        );

        // Check cache first
        CachedStructure cached = dimensionCache.get(pos);
        if (cached != null && cached.isValid(CACHE_DURATION_MS)) {
            return Optional.of(cached);
        }
        Long negativeCachedAt = negativeCache.get(pos);
        if (negativeCachedAt != null && (System.currentTimeMillis() - negativeCachedAt) < CACHE_DURATION_MS) {
            return Optional.empty();
        }

        // Cache miss - search for structure
        try {
            long searchStart = System.currentTimeMillis();
            var structureManager = level.structureManager();

            for (Holder<Structure> structureHolder : level.registryAccess()
                    .registryOrThrow(Registries.STRUCTURE)
                    .holders().toList()) {

                Structure structure = structureHolder.value();
                StructureStart structureStart = structureManager.getStructureAt(pos, structure);

                if (structureStart != null && structureStart.isValid()) {
                    // Found a structure - extract ID
                    ResourceLocation structureId = structureHolder.unwrapKey()
                        .map(key -> key.location())
                        .orElse(null);

                    if (structureId != null) {
                        // Check if this is a civilian structure
                        if (CivilianStructureRegistry.isCivilianStructure(structureId)) {
                            // Calculate structure center (use bounding box center)
                            BlockPos center = structureStart.getBoundingBox().getCenter();
                            BlockPos corner = new BlockPos(
                                structureStart.getBoundingBox().minX(),
                                structureStart.getBoundingBox().minY(),
                                structureStart.getBoundingBox().minZ()
                            );
                            Rotation rotation = findRotation(structureStart);

                            // Cache and return
                            CachedStructure newCached = new CachedStructure(structureId, center, corner, rotation);
                            dimensionCache.put(pos, newCached);
                            negativeCache.remove(pos);
                            return Optional.of(newCached);
                        }
                    }
                }
            }

            long elapsedMs = System.currentTimeMillis() - searchStart;
            if (elapsedMs >= 50L) {
                Log.debugVisibleEvery(
                    "structure-scan:" + dimensionId + ":" + pos.asLong(),
                    5000L,
                    "Structure scan took {} ms at {} in {} and found nothing",
                    elapsedMs,
                    pos,
                    dimensionId
                );
            }
        } catch (Exception e) {
            System.err.println("[Structure Cache] Error searching for structure: " + e.getMessage());
        }

        // No structure found - cache negative result with null
        negativeCache.put(pos.immutable(), System.currentTimeMillis());
        return Optional.empty();
    }

    private static Rotation findRotation(StructureStart structureStart) {
        for (StructurePiece piece : structureStart.getPieces()) {
            Rotation rotation = piece.getRotation();
            if (rotation != null) {
                return rotation;
            }
        }
        return Rotation.NONE;
    }

    /**
     * Clear the cache for a specific dimension.
     *
     * @param dimensionId The dimension ID
     */
    public static void clearDimension(ResourceLocation dimensionId) {
        DIMENSION_CACHES.remove(dimensionId);
        NEGATIVE_CACHES.remove(dimensionId);
    }

    /**
     * Clear all cached structure locations.
     */
    public static void clearAll() {
        DIMENSION_CACHES.clear();
        NEGATIVE_CACHES.clear();
    }

    /**
     * Clean up old cache entries across all dimensions.
     * Should be called periodically (e.g., every few minutes).
     */
    public static void cleanupOldEntries() {
        for (Map<BlockPos, CachedStructure> dimensionCache : DIMENSION_CACHES.values()) {
            dimensionCache.entrySet().removeIf(
                entry -> !entry.getValue().isValid(CACHE_DURATION_MS)
            );
        }
        for (Map<BlockPos, Long> negativeCache : NEGATIVE_CACHES.values()) {
            long now = System.currentTimeMillis();
            negativeCache.entrySet().removeIf(entry -> (now - entry.getValue()) >= CACHE_DURATION_MS);
        }

        // Remove empty dimension caches
        DIMENSION_CACHES.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        NEGATIVE_CACHES.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    /**
     * Get the number of cached entries across all dimensions.
     *
     * @return Total cache size
     */
    public static int getCacheSize() {
        return DIMENSION_CACHES.values().stream()
            .mapToInt(Map::size)
            .sum();
    }

    /**
     * Get cache statistics for debugging.
     *
     * @return String describing cache state
     */
    public static String getCacheStats() {
        int totalEntries = 0;
        int totalDimensions = DIMENSION_CACHES.size();

        for (Map<BlockPos, CachedStructure> cache : DIMENSION_CACHES.values()) {
            totalEntries += cache.size();
        }
        for (Map<BlockPos, Long> cache : NEGATIVE_CACHES.values()) {
            totalEntries += cache.size();
        }

        return String.format("Structure cache: %d dimensions, %d entries", totalDimensions, totalEntries);
    }
}
