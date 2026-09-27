package com.lerdorf.kimetsunoyaibamultiplayer.biome;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.util.KeyDispatchDataCodec;

/** Shifts vanilla density sampling downward inside a Natagumo region, preserving vanilla terrain systems. */
public final class NatagumoDensityFunction implements DensityFunction {
    private static final double BASE_HEIGHT = 60.0;
    private static final int OFFSET_CACHE_SIZE = 256;
    private static final int OFFSET_CACHE_MASK = OFFSET_CACHE_SIZE - 1;

    private final DensityFunction delegate;
    private final EnhancedMountBiomeSource source;
    private final net.minecraft.world.level.biome.Climate.Sampler sampler;
    private final NatagumoTerrainContext.TerrainOffsetCache profileCache;
    private final long[] offsetKeys = new long[OFFSET_CACHE_SIZE];
    private final double[] offsets = new double[OFFSET_CACHE_SIZE];
    private final boolean[] offsetOccupied = new boolean[OFFSET_CACHE_SIZE];
    private final VerticalContext vanillaBaseContext = new VerticalContext();
    private final VerticalContext vanillaTopContext = new VerticalContext();
    private final VerticalContext shiftedLowerContext = new VerticalContext();
    private final VerticalContext shiftedUpperContext = new VerticalContext();

    public NatagumoDensityFunction(DensityFunction delegate, EnhancedMountBiomeSource source,
                                   net.minecraft.world.level.biome.Climate.Sampler sampler,
                                   NatagumoTerrainContext.TerrainOffsetCache profileCache) {
        this.delegate = delegate;
        this.source = source;
        this.sampler = sampler;
        this.profileCache = profileCache;
    }

    @Override
    public double compute(FunctionContext context) {
        double offset = getOrComputeOffset(context);
        if (offset == 0.0) {
            return delegate.compute(context);
        }

        double shiftedY = context.blockY() - offset;
        int lowerY = (int) Math.floor(shiftedY);
        double fraction = shiftedY - lowerY;
        shiftedLowerContext.set(context, lowerY);
        double lowerDensity = delegate.compute(shiftedLowerContext);
        if (fraction == 0.0) {
            return lowerDensity;
        }

        shiftedUpperContext.set(context, lowerY + 1);
        double upperDensity = delegate.compute(shiftedUpperContext);
        return lowerDensity + fraction * (upperDensity - lowerDensity);
    }

    @Override
    public void fillArray(double[] densities, ContextProvider contextProvider) {
        for (int i = 0; i < densities.length; i++) {
            densities[i] = compute(contextProvider.forIndex(i));
        }
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return new NatagumoDensityFunction(delegate.mapAll(visitor), source, sampler, profileCache);
    }

    @Override
    public double minValue() {
        return delegate.minValue();
    }

    @Override
    public double maxValue() {
        return delegate.maxValue();
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return delegate.codec();
    }

    private double getOrComputeOffset(FunctionContext context) {
        int blockX = context.blockX();
        int blockZ = context.blockZ();
        long key = ((long) blockX << 32) ^ (blockZ & 0xFFFFFFFFL);
        int slot = offsetSlot(key);
        if (offsetOccupied[slot] && offsetKeys[slot] == key) {
            return offsets[slot];
        }

        double profile = profileCache.getOrCompute(source, sampler, blockX, blockZ);
        double offset = 0.0;
        if (profile > 0.0) {
            double vanillaHeight = estimateVanillaHeight(context);
            double finalHeight = source.natagumoSurfaceHeight(
                    blockX, blockZ, vanillaHeight, profile);
            offset = finalHeight - vanillaHeight;
        }

        offsetOccupied[slot] = true;
        offsetKeys[slot] = key;
        offsets[slot] = offset;
        return offset;
    }

    private double estimateVanillaHeight(FunctionContext context) {
        vanillaBaseContext.set(context, (int) BASE_HEIGHT);
        vanillaTopContext.set(context, (int) BASE_HEIGHT + 128);
        double baseDensity = delegate.compute(vanillaBaseContext);
        double topDensity = delegate.compute(vanillaTopContext);
        double densityChange = topDensity - baseDensity;
        if (!Double.isFinite(baseDensity) || !Double.isFinite(topDensity)
                || baseDensity * topDensity > 0.0 || Math.abs(densityChange) < 1.0E-6) {
            return BASE_HEIGHT;
        }

        double estimatedHeight = BASE_HEIGHT - baseDensity * 128.0 / densityChange;
        return Math.max(-64.0, Math.min(280.0, estimatedHeight));
    }

    private static int offsetSlot(long key) {
        key ^= key >>> 33;
        key *= 0xFF51AFD7ED558CCDL;
        key ^= key >>> 33;
        return (int) key & OFFSET_CACHE_MASK;
    }

    private static final class VerticalContext implements FunctionContext {
        private FunctionContext delegate;
        private int blockY;

        private void set(FunctionContext delegate, int blockY) {
            this.delegate = delegate;
            this.blockY = blockY;
        }

        @Override
        public int blockX() {
            return delegate.blockX();
        }

        @Override
        public int blockY() {
            return blockY;
        }

        @Override
        public int blockZ() {
            return delegate.blockZ();
        }

        @Override
        public net.minecraft.world.level.levelgen.blending.Blender getBlender() {
            return delegate.getBlender();
        }
    }
}
