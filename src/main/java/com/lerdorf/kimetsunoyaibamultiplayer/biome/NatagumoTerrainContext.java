package com.lerdorf.kimetsunoyaibamultiplayer.biome;

import net.minecraft.world.level.biome.Climate;

/** Carries the active world seed and climate sampler into NoiseChunk construction. */
public final class NatagumoTerrainContext {
    private static final ThreadLocal<Context> ACTIVE = new ThreadLocal<>();

    private NatagumoTerrainContext() {
    }

    public static void set(EnhancedMountBiomeSource source, Climate.Sampler sampler) {
        ACTIVE.set(new Context(source, sampler, new TerrainOffsetCache()));
    }

    public static void clear() {
        ACTIVE.remove();
    }

    public static Context get() {
        return ACTIVE.get();
    }

    public record Context(EnhancedMountBiomeSource source, Climate.Sampler sampler,
                          TerrainOffsetCache cache) {
    }

    /** Caches the Y-independent terrain profile for the X/Z positions of one NoiseChunk. */
    public static final class TerrainOffsetCache {
        private static final int SIZE = 256;
        private static final int MASK = SIZE - 1;

        private final long[] keys = new long[SIZE];
        private final double[] profiles = new double[SIZE];
        private final boolean[] occupied = new boolean[SIZE];

        public double getOrCompute(EnhancedMountBiomeSource source, Climate.Sampler sampler,
                                   int blockX, int blockZ) {
            long key = ((long) blockX << 32) ^ (blockZ & 0xFFFFFFFFL);
            int slot = slot(key);
            if (occupied[slot] && keys[slot] == key) {
                return profiles[slot];
            }

            double profile = source.natagumoTerrainProfile(blockX, blockZ, sampler);
            occupied[slot] = true;
            keys[slot] = key;
            profiles[slot] = profile;
            return profile;
        }

        private static int slot(long key) {
            key ^= key >>> 33;
            key *= 0xFF51AFD7ED558CCDL;
            key ^= key >>> 33;
            return (int) key & MASK;
        }
    }
}
