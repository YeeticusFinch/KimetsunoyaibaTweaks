package com.lerdorf.kimetsunoyaibamultiplayer.biome;

import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.HashMap;
import java.util.Map;

/**
 * A chunk-owned evaluator of the SEEDED, UNWRAPPED vanilla graph. Implements its
 * interpolation markers at arbitrary coordinates, including vertically shifted
 * coordinates. Never samples NoiseChunk's stateful interpolators with a foreign
 * context, or exposes its quart-resolution flat caches directly to block sampling.
 */
public final class NatagumoNoiseSampler {
    // The marker enum's enclosing class is package-private in vanilla. Use the
    // public factory API to identify marker kinds without reflection or ATs.
    private static final Object INTERPOLATED = ((DensityFunctions.MarkerOrMarked)
            DensityFunctions.interpolated(DensityFunctions.zero())).type();
    private static final Object FLAT_CACHE = ((DensityFunctions.MarkerOrMarked)
            DensityFunctions.flatCache(DensityFunctions.zero())).type();
    private static final Object CACHE_2D = ((DensityFunctions.MarkerOrMarked)
            DensityFunctions.cache2d(DensityFunctions.zero())).type();
    private final DensityFunction density;
    private final Blender blender;
    private long cornerEvaluations;

    public NatagumoNoiseSampler(DensityFunction rawDensity, int cellWidth, int cellHeight, Blender blender) {
        this.blender = blender;
        Map<DensityFunction, DensityFunction> mapped = new HashMap<>();
        density = rawDensity.mapAll(function -> mapped.computeIfAbsent(function, node -> {
            if (node instanceof DensityFunctions.MarkerOrMarked marker) {
                Object type = marker.type();
                return type == INTERPOLATED ? new Interpolated(marker.wrapped(), cellWidth, cellHeight)
                        : new Memoized(marker.wrapped(), type == FLAT_CACHE || type == CACHE_2D);
            }
            if (node == DensityFunctions.blendAlpha() || node == DensityFunctions.blendOffset()) {
                return new BlendingValue(node, node == DensityFunctions.blendAlpha());
            }
            return node;
        }));
    }

    public double compute(int x, int y, int z) {
        return density.compute(new Point(x, y, z, blender));
    }

    /** Diagnostic counter, local to this evaluator; no logging in the sampling loop. */
    public long cornerEvaluations() {
        return cornerEvaluations;
    }

    private record Point(int blockX, int blockY, int blockZ, Blender getBlender)
            implements DensityFunction.FunctionContext {
    }

    private abstract static class RuntimeFunction implements DensityFunction.SimpleFunction {
        protected final DensityFunction input;

        private RuntimeFunction(DensityFunction input) {
            this.input = input;
        }

        @Override public double minValue() { return input.minValue(); }
        @Override public double maxValue() { return input.maxValue(); }
        @Override public KeyDispatchDataCodec<? extends DensityFunction> codec() { return input.codec(); }
    }

    private static final class BlendingValue extends RuntimeFunction {
        private final boolean alpha;

        private BlendingValue(DensityFunction input, boolean alpha) {
            super(input);
            this.alpha = alpha;
        }

        @Override
        public double compute(FunctionContext context) {
            Blender.BlendingOutput result = context.getBlender().blendOffsetAndFactor(context.blockX(), context.blockZ());
            return alpha ? result.alpha() : result.blendingOffset();
        }
    }

    private static final class Memoized extends RuntimeFunction {
        private final boolean column;
        private final Long2DoubleOpenHashMap values = new Long2DoubleOpenHashMap(128);

        private Memoized(DensityFunction input, boolean column) {
            super(input);
            this.column = column;
            values.defaultReturnValue(Double.NaN);
        }

        @Override
        public double compute(FunctionContext context) {
            if (!column && (context.blockY() < -2048 || context.blockY() > 2047)) {
                return input.compute(context);
            }
            long key = column ? columnKey(context.blockX(), context.blockZ())
                    : BlockPos.asLong(context.blockX(), context.blockY(), context.blockZ());
            double value = values.get(key);
            if (Double.isNaN(value)) {
                value = input.compute(context);
                if (values.size() >= (column ? 1024 : 4096)) {
                    values.clear();
                }
                values.put(key, value);
            }
            return value;
        }
    }

    private final class Interpolated extends RuntimeFunction {
        private final int width;
        private final int height;
        private final Long2DoubleOpenHashMap corners = new Long2DoubleOpenHashMap(256);

        private Interpolated(DensityFunction input, int width, int height) {
            super(input);
            this.width = width;
            this.height = height;
            corners.defaultReturnValue(Double.NaN);
        }

        @Override
        public double compute(FunctionContext context) {
            int x = Math.floorDiv(context.blockX(), width) * width;
            int y = Math.floorDiv(context.blockY(), height) * height;
            int z = Math.floorDiv(context.blockZ(), width) * width;
            double dx = (context.blockX() - x) / (double) width;
            double dy = (context.blockY() - y) / (double) height;
            double dz = (context.blockZ() - z) / (double) width;
            // Same axis order as vanilla NoiseInterpolator; nonlinear operations
            // (squeeze, cave min/max, range choices) remain OUTSIDE this marker.
            return Mth.lerp3(dx, dy, dz,
                    corner(x, y, z), corner(x + width, y, z),
                    corner(x, y + height, z), corner(x + width, y + height, z),
                    corner(x, y, z + width), corner(x + width, y, z + width),
                    corner(x, y + height, z + width), corner(x + width, y + height, z + width));
        }

        private double corner(int x, int y, int z) {
            if (y < -2048 || y > 2047) {
                return input.compute(new Point(x, y, z, blender));
            }
            long key = BlockPos.asLong(x, y, z);
            double value = corners.get(key);
            if (Double.isNaN(value)) {
                cornerEvaluations++;
                value = input.compute(new Point(x, y, z, blender));
                if (corners.size() >= 8192) {
                    corners.clear();
                }
                corners.put(key, value);
            }
            return value;
        }
    }

    static long columnKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }
}
