package com.lerdorf.kimetsunoyaibamultiplayer.biome;

import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseSettings;

/** Vertically translates interpolated vanilla density, with exact per-column offsets. */
public final class NatagumoDensityFunction implements DensityFunction {
    private final DensityFunction delegate;
    private final Columns columns;

    public NatagumoDensityFunction(DensityFunction delegate, NatagumoTerrainContext.Context context) {
        this(delegate, new Columns(context));
    }

    private NatagumoDensityFunction(DensityFunction delegate, Columns columns) {
        this.delegate = delegate;
        this.columns = columns;
    }

    @Override
    public double compute(FunctionContext context) {
        int x = context.blockX();
        int z = context.blockZ();
        double offset = columns.offset(x, z);
        if (offset == 0.0) {
            return delegate.compute(context);
        }
        double shiftedY = context.blockY() - offset;
        int lowerY = (int) Math.floor(shiftedY);
        double fraction = shiftedY - lowerY;
        double lower = columns.noise.compute(x, lowerY, z);
        if (fraction == 0.0) {
            return lower;
        }
        double upper = columns.noise.compute(x, lowerY + 1, z);
        return lower + fraction * (upper - lower);
    }

    @Override
    public void fillArray(double[] densities, ContextProvider provider) {
        provider.fillAllDirectly(densities, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        // Only the unshifted delegate belongs to NoiseChunk's visitor. The private
        // seeded evaluator and its caches must never be rewrapped as vanilla caches.
        return visitor.apply(new NatagumoDensityFunction(delegate.mapAll(visitor), columns));
    }

    @Override public double minValue() { return delegate.minValue(); }
    @Override public double maxValue() { return delegate.maxValue(); }
    @Override public KeyDispatchDataCodec<? extends DensityFunction> codec() { return delegate.codec(); }

    public long heightSearches() { return columns.heightSearches; }
    public long cornerEvaluations() { return columns.noise.cornerEvaluations(); }

    private static final class Columns {
        private final EnhancedMountBiomeSource source;
        private final Climate.Sampler climate;
        private final NatagumoNoiseSampler noise;
        private final int minY;
        private final int maxY;
        private final int searchStep;
        private final Long2DoubleOpenHashMap offsets = new Long2DoubleOpenHashMap(289);
        private long heightSearches;

        private Columns(NatagumoTerrainContext.Context context) {
            source = context.source();
            climate = context.state().sampler();
            NoiseSettings settings = context.settings();
            minY = settings.minY();
            maxY = minY + settings.height();
            searchStep = settings.getCellHeight();
            noise = new NatagumoNoiseSampler(context.state().router().finalDensity(),
                    settings.getCellWidth(), searchStep, context.blender());
            offsets.defaultReturnValue(Double.NaN);
        }

        private double offset(int x, int z) {
            long key = NatagumoNoiseSampler.columnKey(x, z);
            double offset = offsets.get(key);
            if (!Double.isNaN(offset)) {
                return offset;
            }
            double profile = source.natagumoTerrainProfile(x, z, climate);
            offset = 0.0;
            if (profile > 0.0) {
                double base = vanillaHeight(x, z);
                offset = source.natagumoSurfaceHeight(x, z, base, profile) - base;
            }
            // Normal chunk + border sampling fits without eviction. Bound unusual
            // modded callers too; open addressing resolves collisions correctly.
            if (offsets.size() >= 4096) {
                offsets.clear();
            }
            offsets.put(key, offset);
            return offset;
        }

        private double vanillaHeight(int x, int z) {
            heightSearches++;
            double upperDensity = noise.compute(x, maxY, z);
            int upperY = maxY;
            for (int lowerY = maxY - searchStep; lowerY >= minY; lowerY -= searchStep) {
                double lowerDensity = noise.compute(x, lowerY, z);
                if (!Double.isFinite(upperDensity) || !Double.isFinite(lowerDensity)) {
                    return 60.0;
                }
                if (upperDensity <= 0.0 && lowerDensity >= 0.0) {
                    double change = lowerDensity - upperDensity;
                    double crossing = Math.abs(change) < 1.0E-6 ? lowerY
                            : upperY + (lowerY - upperY) * (-upperDensity / change);
                    return Math.max(minY, Math.min(280.0, crossing));
                }
                upperY = lowerY;
                upperDensity = lowerDensity;
            }
            return 60.0;
        }
    }
}
