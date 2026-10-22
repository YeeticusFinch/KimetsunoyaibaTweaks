package com.lerdorf.kimetsunoyaibamultiplayer.biome;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedMountBiomeConfig;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

import java.util.Optional;

/** Bounded, deterministic candidate cache; includes every placement/profile dependency. */
public final class NatagumoStructureCandidates {
    private static final Cache<Key, Optional<ChunkPos>> CACHE = CacheBuilder.newBuilder().maximumSize(512).build();

    private NatagumoStructureCandidates() { }

    public static boolean isPreferred(RandomSpreadStructurePlacement placement, long seed,
                                      ChunkPos candidate, String targetRegion) {
        EnhancedMountBiomeSource.NatagumoRegion region = EnhancedMountBiomeSource.getNatagumoRegion(
                seed, candidate.getMiddleBlockX(), candidate.getMiddleBlockZ());
        if (region == null || !eligible(region, candidate, targetRegion)) return false;
        Key key = new Key(seed, region.ring(), region.centerX(), region.centerZ(), region.radius(),
                EnhancedMountBiomeConfig.natagumoBiomeThreshold,
                EnhancedMountBiomeConfig.natagumoRingSpacing, EnhancedMountBiomeConfig.natagumoFirstRingRadius,
                placement, targetRegion);
        Optional<ChunkPos> preferred = CACHE.getIfPresent(key);
        if (preferred == null) {
            preferred = Optional.ofNullable(find(placement, seed, region, targetRegion));
            CACHE.put(key, preferred);
        }
        return preferred.isPresent() && candidate.equals(preferred.get());
    }

    private static boolean eligible(EnhancedMountBiomeSource.NatagumoRegion region,
                                    ChunkPos position, String targetRegion) {
        return region.strength() >= EnhancedMountBiomeConfig.natagumoBiomeThreshold
                && targetRegion.equals(EnhancedMountBiomeSource.natagumoRegionName(
                region, position.getMiddleBlockX(), position.getMiddleBlockZ()));
    }

    private static ChunkPos find(RandomSpreadStructurePlacement placement, long seed,
                                 EnhancedMountBiomeSource.NatagumoRegion region, String targetRegion) {
        int radiusChunks = Math.max(1, (int) Math.ceil(region.radius() / 16.0) + 2);
        int centerX = Math.floorDiv(region.centerX(), 16);
        int centerZ = Math.floorDiv(region.centerZ(), 16);
        ChunkPos preferred = null;
        long preferredDistance = Long.MAX_VALUE;
        int spacing = placement.spacing();
        for (int gridX = Math.floorDiv(centerX - radiusChunks, spacing) - 1;
             gridX <= Math.floorDiv(centerX + radiusChunks, spacing) + 1; gridX++) {
            for (int gridZ = Math.floorDiv(centerZ - radiusChunks, spacing) - 1;
                 gridZ <= Math.floorDiv(centerZ + radiusChunks, spacing) + 1; gridZ++) {
                // getPotentialStructureChunk divides its CHUNK coordinates by spacing.
                ChunkPos possible = placement.getPotentialStructureChunk(seed, gridX * spacing, gridZ * spacing);
                if (Math.abs(possible.x - centerX) > radiusChunks || Math.abs(possible.z - centerZ) > radiusChunks) continue;
                EnhancedMountBiomeSource.NatagumoRegion other = EnhancedMountBiomeSource.getNatagumoRegion(
                        seed, possible.getMiddleBlockX(), possible.getMiddleBlockZ());
                if (other == null || other.ring() != region.ring() || other.centerX() != region.centerX()
                        || other.centerZ() != region.centerZ() || !eligible(other, possible, targetRegion)) continue;
                long dx = possible.getMiddleBlockX() - region.centerX();
                long dz = possible.getMiddleBlockZ() - region.centerZ();
                long distance = dx * dx + dz * dz;
                if (distance < preferredDistance) {
                    preferred = possible;
                    preferredDistance = distance;
                }
            }
        }
        return preferred;
    }

    private record Key(long seed, int ring, int x, int z, double radius, double threshold,
                       int ringSpacing, int firstRadius, RandomSpreadStructurePlacement placement, String target) { }
}
