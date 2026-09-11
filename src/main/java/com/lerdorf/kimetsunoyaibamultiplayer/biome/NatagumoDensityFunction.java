package com.lerdorf.kimetsunoyaibamultiplayer.biome;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.util.KeyDispatchDataCodec;

/** Shifts vanilla density sampling downward inside a Natagumo region, preserving vanilla terrain systems. */
public final class NatagumoDensityFunction implements DensityFunction {
    private final DensityFunction delegate;
    private final EnhancedMountBiomeSource source;
    private final net.minecraft.world.level.biome.Climate.Sampler sampler;

    public NatagumoDensityFunction(DensityFunction delegate, EnhancedMountBiomeSource source,
                                   net.minecraft.world.level.biome.Climate.Sampler sampler) {
        this.delegate = delegate;
        this.source = source;
        this.sampler = sampler;
    }

    @Override
    public double compute(FunctionContext context) {
        int offset = source.terrainOffset(context.blockX(), context.blockZ(), sampler);
        if (offset <= 0) {
            return delegate.compute(context);
        }
        return delegate.compute(new ShiftedContext(context, offset));
    }

    @Override
    public void fillArray(double[] densities, ContextProvider contextProvider) {
        for (int i = 0; i < densities.length; i++) {
            densities[i] = compute(contextProvider.forIndex(i));
        }
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return new NatagumoDensityFunction(delegate.mapAll(visitor), source, sampler);
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

    private record ShiftedContext(FunctionContext delegate, int verticalOffset) implements FunctionContext {
        @Override
        public int blockX() {
            return delegate.blockX();
        }

        @Override
        public int blockY() {
            return delegate.blockY() - verticalOffset;
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
