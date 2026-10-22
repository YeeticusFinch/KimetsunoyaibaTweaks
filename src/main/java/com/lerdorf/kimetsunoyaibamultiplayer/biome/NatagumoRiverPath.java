package com.lerdorf.kimetsunoyaibamultiplayer.biome;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/** Deterministic, cached southbound river path for one Mount Natagumo peak. */
public final class NatagumoRiverPath {
    private static final int CONTROL_POINT_COUNT = 25;
    private static final Cache<Key, NatagumoRiverPath> CACHE = CacheBuilder.newBuilder().maximumSize(256).build();

    private final double[] x;
    private final double[] z;
    private final double[] segmentX = new double[CONTROL_POINT_COUNT - 1];
    private final double[] segmentZ = new double[CONTROL_POINT_COUNT - 1];
    private final double[] lengthSquared = new double[CONTROL_POINT_COUNT - 1];
    private final double minX;
    private final double maxX;
    private final double minZ;
    private final double maxZ;

    private NatagumoRiverPath(long seed, EnhancedMountBiomeSource.NatagumoRegion region) {
        x = new double[CONTROL_POINT_COUNT];
        z = new double[CONTROL_POINT_COUNT];

        double radius = region.radius();
        double startZ = region.centerZ() + radius * 0.20;
        double endZ = region.centerZ() + radius * 0.85;
        double phase = hashToUnit(seed, region.ring(), 0x50484153) * Math.PI * 2.0;
        for (int i = 0; i < CONTROL_POINT_COUNT; i++) {
            double progress = i / (double) (CONTROL_POINT_COUNT - 1);
            z[i] = startZ + (endZ - startZ) * progress;
            double wave = Math.sin(progress * Math.PI * 2.0 + phase) * (20.0 * radius / 800.0);
            x[i] = region.centerX() + wave;
        }
        double localMinX = x[0];
        double localMaxX = x[0];
        double localMinZ = z[0];
        double localMaxZ = z[0];
        for (int i = 1; i < CONTROL_POINT_COUNT; i++) {
            localMinX = Math.min(localMinX, x[i]);
            localMaxX = Math.max(localMaxX, x[i]);
            localMinZ = Math.min(localMinZ, z[i]);
            localMaxZ = Math.max(localMaxZ, z[i]);
        }
        minX = localMinX;
        maxX = localMaxX;
        minZ = localMinZ;
        maxZ = localMaxZ;
        for (int i = 0; i < CONTROL_POINT_COUNT - 1; i++) {
            segmentX[i] = x[i + 1] - x[i];
            segmentZ[i] = z[i + 1] - z[i];
            lengthSquared[i] = segmentX[i] * segmentX[i] + segmentZ[i] * segmentZ[i];
        }
    }

    public static NatagumoRiverPath get(long seed, EnhancedMountBiomeSource.NatagumoRegion region) {
        Key key = new Key(seed, region.ring(), region.centerX(), region.centerZ(), region.radius());
        NatagumoRiverPath path = CACHE.getIfPresent(key);
        if (path == null) {
            path = new NatagumoRiverPath(seed, region);
            CACHE.put(key, path);
        }
        return path;
    }

    /** Returns the nearest path distance and downstream progress for a block position. */
    public Sample sample(double blockX, double blockZ) {
        double bestDistanceSquared = Double.POSITIVE_INFINITY;
        double bestProgress = 0.0;
        for (int i = 0; i < CONTROL_POINT_COUNT - 1; i++) {
            double startX = x[i];
            double startZ = z[i];
            double segmentX = this.segmentX[i];
            double segmentZ = this.segmentZ[i];
            double segmentLengthSquared = lengthSquared[i];
            double amount = segmentLengthSquared == 0.0 ? 0.0
                    : ((blockX - startX) * segmentX + (blockZ - startZ) * segmentZ)
                    / segmentLengthSquared;
            amount = Math.max(0.0, Math.min(1.0, amount));
            double nearestX = startX + segmentX * amount;
            double nearestZ = startZ + segmentZ * amount;
            double dx = blockX - nearestX;
            double dz = blockZ - nearestZ;
            double distanceSquared = dx * dx + dz * dz;
            if (distanceSquared < bestDistanceSquared) {
                bestDistanceSquared = distanceSquared;
                bestProgress = (i + amount) / (CONTROL_POINT_COUNT - 1.0);
            }
        }
        return new Sample(Math.sqrt(bestDistanceSquared), bestProgress);
    }

    public boolean mayContain(double blockX, double blockZ, double halfWidth) {
        return blockX >= minX - halfWidth && blockX <= maxX + halfWidth
                && blockZ >= minZ - halfWidth && blockZ <= maxZ + halfWidth;
    }

    /** Allocation-free corridor check used by surface generation. */
    public boolean contains(double blockX, double blockZ, double halfWidth) {
        if (!mayContain(blockX, blockZ, halfWidth)) return false;
        double limit = halfWidth * halfWidth;
        for (int i = 0; i < CONTROL_POINT_COUNT - 1; i++) {
            double amount = lengthSquared[i] == 0.0 ? 0.0
                    : ((blockX - x[i]) * segmentX[i] + (blockZ - z[i]) * segmentZ[i]) / lengthSquared[i];
            amount = Math.max(0.0, Math.min(1.0, amount));
            double dx = blockX - (x[i] + segmentX[i] * amount);
            double dz = blockZ - (z[i] + segmentZ[i] * amount);
            if (dx * dx + dz * dz <= limit) return true;
        }
        return false;
    }

    int controlPointCount() {
        return CONTROL_POINT_COUNT;
    }

    double controlX(int index) {
        return x[index];
    }

    double controlZ(int index) {
        return z[index];
    }

    private static double hashToUnit(long seed, int ring, int salt) {
        long value = seed + 0x9E3779B97F4A7C15L;
        value ^= (long) ring * 0xBF58476D1CE4E5B9L;
        value ^= (long) salt * 0x94D049BB133111EBL;
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return (value >>> 11) * 0x1.0p-53;
    }

    public record Sample(double distance, double progress) {
    }

    private record Key(long seed, int ring, int centerX, int centerZ, double radius) {
    }
}
