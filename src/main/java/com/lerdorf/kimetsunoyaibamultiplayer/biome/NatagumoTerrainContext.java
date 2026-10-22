package com.lerdorf.kimetsunoyaibamultiplayer.biome;

import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

/** Construction-only context. Each NoiseChunk gets its own mutable sampling caches. */
public final class NatagumoTerrainContext {
    private static final ThreadLocal<Context> ACTIVE = new ThreadLocal<>();

    private NatagumoTerrainContext() {
    }

    public static void set(EnhancedMountBiomeSource source, RandomState state,
                           NoiseSettings settings, Blender blender) {
        ACTIVE.set(new Context(source, state, settings, blender));
    }

    public static void clear() {
        ACTIVE.remove();
    }

    public static void restore(Context context) {
        if (context == null) clear();
        else ACTIVE.set(context);
    }

    public static Context get() {
        return ACTIVE.get();
    }

    public record Context(EnhancedMountBiomeSource source, RandomState state,
                          NoiseSettings settings, Blender blender) {
    }
}
