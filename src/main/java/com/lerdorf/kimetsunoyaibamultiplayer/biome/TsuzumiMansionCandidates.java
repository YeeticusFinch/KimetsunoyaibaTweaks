package com.lerdorf.kimetsunoyaibamultiplayer.biome;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedMountBiomeConfig;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

import java.util.Optional;

/** Selects one deterministic Tsuzumi Mansion candidate on each world ring. */
public final class TsuzumiMansionCandidates {
    private static final double RING_SELECTION_BAND = 0.40D;
    private static final int OCEAN_SAMPLE_RADIUS = 96;
    private static final int OCEAN_SAMPLE_STEP = 48;
    private static final Cache<Key, Optional<ChunkPos>> CACHE = CacheBuilder.newBuilder()
        .maximumSize(256)
        .build();

    private TsuzumiMansionCandidates() {
    }

    public static boolean isPreferred(ChunkGenerator generator, RandomState randomState,
                                      RandomSpreadStructurePlacement placement, long seed,
                                      ChunkPos candidate) {
        int spacing = Math.max(1, EnhancedMountBiomeConfig.natagumoRingSpacing);
        int firstRadius = Math.max(1, EnhancedMountBiomeConfig.natagumoFirstRingRadius);
        double distance = Math.hypot(candidate.getMiddleBlockX(), candidate.getMiddleBlockZ());
        int ring = Math.max(1, (int) Math.round((distance - firstRadius) / spacing) + 1);
        double targetRadius = firstRadius + (ring - 1L) * spacing;
        if (Math.abs(distance - targetRadius) > spacing * RING_SELECTION_BAND) {
            return false;
        }

        Key key = new Key(seed, ring, placement);
        Optional<ChunkPos> preferred = CACHE.getIfPresent(key);
        if (preferred == null) {
            preferred = Optional.ofNullable(find(generator, randomState, placement, seed, ring,
                targetRadius, spacing));
            CACHE.put(key, preferred);
        }
        return preferred.isPresent() && candidate.equals(preferred.get());
    }

    private static ChunkPos find(ChunkGenerator generator, RandomState randomState,
                                 RandomSpreadStructurePlacement placement, long seed, int ring,
                                 double targetRadius, int ringSpacing) {
        int searchRadiusChunks = (int) Math.ceil((targetRadius + ringSpacing) / 16.0D) + 4;
        int placementSpacing = Math.max(1, placement.spacing());
        ChunkPos preferred = null;
        double preferredError = Double.MAX_VALUE;

        for (int gridX = Math.floorDiv(-searchRadiusChunks, placementSpacing) - 1;
             gridX <= Math.floorDiv(searchRadiusChunks, placementSpacing) + 1; gridX++) {
            for (int gridZ = Math.floorDiv(-searchRadiusChunks, placementSpacing) - 1;
                 gridZ <= Math.floorDiv(searchRadiusChunks, placementSpacing) + 1; gridZ++) {
                ChunkPos possible = placement.getPotentialStructureChunk(seed,
                    gridX * placementSpacing, gridZ * placementSpacing);
                double distance = Math.hypot(possible.getMiddleBlockX(), possible.getMiddleBlockZ());
                double error = Math.abs(distance - targetRadius);
                if (error > ringSpacing * RING_SELECTION_BAND
                    || error > preferredError
                    || !safeFromNatagumo(seed, possible)
                    || !safeFromOcean(generator, randomState, possible)) {
                    continue;
                }

                if (preferred == null || error < preferredError
                    || possible.toLong() < preferred.toLong()) {
                    preferred = possible;
                    preferredError = error;
                }
            }
        }
        return preferred;
    }

    private static boolean safeFromOcean(ChunkGenerator generator, RandomState randomState,
                                         ChunkPos candidate) {
        BiomeSource source = generator.getBiomeSource();
        if (source instanceof EnhancedMountBiomeSource enhanced) {
            source = enhanced.serializationDelegate();
        }

        int centerX = candidate.getMiddleBlockX();
        int centerZ = candidate.getMiddleBlockZ();
        for (int offsetX = -OCEAN_SAMPLE_RADIUS; offsetX <= OCEAN_SAMPLE_RADIUS;
             offsetX += OCEAN_SAMPLE_STEP) {
            for (int offsetZ = -OCEAN_SAMPLE_RADIUS; offsetZ <= OCEAN_SAMPLE_RADIUS;
                 offsetZ += OCEAN_SAMPLE_STEP) {
                Holder<Biome> biome = source.getNoiseBiome(
                    (centerX + offsetX) >> 2,
                    16,
                    (centerZ + offsetZ) >> 2,
                    randomState.sampler());
                if (biome.is(BiomeTags.IS_OCEAN)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean safeFromNatagumo(long seed, ChunkPos candidate) {
        if (!EnhancedMountBiomeConfig.enhancedMountNatagumoEnabled) {
            return true;
        }
        return !EnhancedMountBiomeSource.isNearNatagumoRegion(
            seed,
            candidate.getMiddleBlockX(),
            candidate.getMiddleBlockZ(),
            256.0D
        );
    }

    private record Key(long seed, int ring, RandomSpreadStructurePlacement placement) {
    }
}
