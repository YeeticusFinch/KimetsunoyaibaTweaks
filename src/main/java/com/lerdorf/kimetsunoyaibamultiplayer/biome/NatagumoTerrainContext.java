package com.lerdorf.kimetsunoyaibamultiplayer.biome;

import net.minecraft.world.level.biome.Climate;

/** Carries the active world seed and climate sampler into NoiseChunk construction. */
public final class NatagumoTerrainContext {
    private static final ThreadLocal<Context> ACTIVE = new ThreadLocal<>();

    private NatagumoTerrainContext() {
    }

    public static void set(EnhancedMountBiomeSource source, Climate.Sampler sampler) {
        ACTIVE.set(new Context(source, sampler));
    }

    public static void clear() {
        ACTIVE.remove();
    }

    public static Context get() {
        return ACTIVE.get();
    }

    public record Context(EnhancedMountBiomeSource source, Climate.Sampler sampler) {
    }
}
