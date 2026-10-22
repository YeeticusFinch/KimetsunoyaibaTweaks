package com.lerdorf.kimetsunoyaibamultiplayer.biome;

import com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedMountBiomeConfig;
import com.mojang.serialization.Codec;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Adds the base mod's Mount Natagumo biome to the same deterministic regions
 * whose density field is raised by the Natagumo density hook.
 */
public final class EnhancedMountBiomeSource extends BiomeSource {
    private static final int YOKO_SALT = 0x594F4B4F;
    private static final double NATAGUMO_BASE_RADIUS = 800.0;
    private static final double NATAGUMO_BASE_Y = 75.0;
    private static final double NATAGUMO_TARGET_SUMMIT_Y = 280.0;
    static final double RIVER_WATER_HALF_WIDTH = 3.0;
    private static final int CHUNK_SAMPLE_MARGIN = 32;
    private static final Cache<CenterKey, Center> CENTERS = CacheBuilder.newBuilder()
            .maximumSize(2048).build();

    private final BiomeSource delegate;
    private final long seed;
    private final Holder<Biome> natagumo;
    private final Holder<Biome> yoko;
    private final Set<Integer> discoveredRings = ConcurrentHashMap.newKeySet();

    public EnhancedMountBiomeSource(BiomeSource delegate, long seed,
                                    Holder<Biome> natagumo, Holder<Biome> yoko) {
        this.delegate = delegate;
        this.seed = seed;
        this.natagumo = natagumo;
        this.yoko = yoko;
    }

    public long seed() {
        return seed;
    }

    /**
     * Returns the registered source that must be used when the level's dimensions are encoded.
     * This runtime wrapper is intentionally not a registered biome-source codec.
     */
    public BiomeSource serializationDelegate() {
        return delegate;
    }

    @Override
    protected Codec<? extends BiomeSource> codec() {
        // This source is installed after registries load and is never decoded from worldgen data.
        return Codec.unit(this);
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        Set<Holder<Biome>> biomes = new HashSet<>(delegate.possibleBiomes());
        if (natagumo != null) {
            biomes.add(natagumo);
        }
        if (yoko != null) {
            biomes.add(yoko);
        }
        return biomes.stream();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
        int blockX = quartX * 4;
        int blockZ = quartZ * 4;
        if (natagumo != null && EnhancedMountBiomeConfig.enhancedMountNatagumoEnabled) {
            NatagumoRegion region = getNatagumoRegion(seed, blockX, blockZ);
            if (isEligibleNatagumoRegion(region, blockX, blockZ, sampler)) {
                discover(region);
                return natagumo;
            }
        }

        // Keep the original enhanced Yoko behavior for the separate Yoko feature.
        if (yoko != null && EnhancedMountBiomeConfig.enhancedMountYokoEnabled
                && isMountainClimate(sampler.sample(quartX, quartY, quartZ))
                && isRegionSelected(seed, blockX, blockZ,
                EnhancedMountBiomeConfig.yokoNoiseScale,
                EnhancedMountBiomeConfig.yokoMountainChance, YOKO_SALT)) {
            return yoko;
        }

        return delegate.getNoiseBiome(quartX, quartY, quartZ, sampler);
    }

    @Override
    public void addDebugInfo(List<String> lines, net.minecraft.core.BlockPos pos, Climate.Sampler sampler) {
        delegate.addDebugInfo(lines, pos, sampler);
    }

    /**
     * Returns the Natagumo region containing the position, or null outside all ring mountains.
     * Ring one is centered exactly 3000 blocks from the origin, ring two at 6000, and so on.
     */
    public static NatagumoRegion getNatagumoRegion(long seed, int blockX, int blockZ) {
        int spacing = Math.max(1, EnhancedMountBiomeConfig.natagumoRingSpacing);
        int firstRadius = Math.max(1, EnhancedMountBiomeConfig.natagumoFirstRingRadius);
        double distanceFromOrigin = Math.sqrt((double) blockX * blockX + (double) blockZ * blockZ);
        int nearestRing = Math.max(1, (int) Math.round(
                (distanceFromOrigin - firstRadius) / (double) spacing) + 1);

        NatagumoRegion best = null;
        double radius = natagumoRadius();
        for (int ring = Math.max(1, nearestRing - 1); ring <= nearestRing + 1; ring++) {
            Center center = center(seed, ring, spacing, firstRadius);
            double squared = distanceSquared(center.x(), center.z(), blockX, blockZ);
            if (squared >= radius * radius) {
                continue;
            }
            double distance = Math.sqrt(squared);
            double strength = profileStrength(distance, radius);
            if (strength > 0.0 && (best == null || strength > best.strength())) {
                best = new NatagumoRegion(ring, center.x(), center.z(), radius,
                        strength, distance);
            }
        }
        return best;
    }

    /** Returns whether a chunk's noise-border sample area can intersect any mountain. */
    public boolean mayAffectChunk(int minBlockX, int minBlockZ, int chunkSize) {
        if (!EnhancedMountBiomeConfig.enhancedMountNatagumoEnabled) {
            return false;
        }

        int minX = minBlockX - CHUNK_SAMPLE_MARGIN;
        int minZ = minBlockZ - CHUNK_SAMPLE_MARGIN;
        int maxX = minBlockX + chunkSize - 1 + CHUNK_SAMPLE_MARGIN;
        int maxZ = minBlockZ + chunkSize - 1 + CHUNK_SAMPLE_MARGIN;
        double minDistance = Math.sqrt(distanceSquaredToBox(0, 0, minX, minZ, maxX, maxZ));
        double maxDistance = Math.sqrt(Math.max(
                distanceSquared(0, 0, minX, minZ),
                Math.max(distanceSquared(0, 0, minX, maxZ),
                Math.max(distanceSquared(0, 0, maxX, minZ),
                        distanceSquared(0, 0, maxX, maxZ)))));

        int spacing = Math.max(1, EnhancedMountBiomeConfig.natagumoRingSpacing);
        int firstRadius = Math.max(1, EnhancedMountBiomeConfig.natagumoFirstRingRadius);
        int firstRing = Math.max(1, (int) Math.floor((minDistance - firstRadius) / spacing) - 3);
        int lastRing = Math.max(firstRing,
                (int) Math.ceil((maxDistance - firstRadius) / spacing) + 3);
        double mountainRadius = natagumoRadius();
        double mountainRadiusSquared = mountainRadius * mountainRadius;

        for (int ring = firstRing; ring <= lastRing; ring++) {
            Center center = center(seed, ring, spacing, firstRadius);
            if (distanceSquaredToBox(center.x(), center.z(), minX, minZ, maxX, maxZ)
                    <= mountainRadiusSquared) {
                return true;
            }
        }
        return false;
    }

    public static double natagumoStrength(long seed, int blockX, int blockZ) {
        NatagumoRegion region = getNatagumoRegion(seed, blockX, blockZ);
        return region == null ? 0.0 : region.strength();
    }

    public NatagumoRiverPath riverPath(NatagumoRegion region) {
        return NatagumoRiverPath.get(seed, region);
    }

    /** Returns the horizontal distance to the local river path without changing terrain height. */
    public double riverDistance(int blockX, int blockZ) {
        NatagumoRegion region = getNatagumoRegion(seed, blockX, blockZ);
        return region == null ? Double.POSITIVE_INFINITY
                : riverPath(region).sample(blockX, blockZ).distance();
    }

    /** Returns the Natagumo surface before the river channel is carved. */
    public double natagumoSurfaceHeight(double vanillaHeight, double profile) {
        return natagumoSurfaceHeight(0, 0, vanillaHeight, profile);
    }

    /** Returns a smooth radial target surface independent of the selected biome. */
    public double natagumoSurfaceHeight(int blockX, int blockZ, double vanillaHeight, double profile) {
        if (profile <= 0.0) {
            return vanillaHeight;
        }

        double clampedProfile = clamp(profile, 0.0, 1.0);
        double targetHeight = vanillaHeight
                + ((NATAGUMO_TARGET_SUMMIT_Y - vanillaHeight) * clampedProfile
                + detailNoise(seed, blockX, blockZ) * clampedProfile)
                * natagumoHeightScale();
        // Interpolate from the local vanilla surface so the edge never imposes an absolute base Y.
        double mountainHeight = Math.max(vanillaHeight, targetHeight);
        double mountainBlend = smootherStep(clampedProfile);
        double finalHeight = lerp(vanillaHeight, mountainHeight, mountainBlend);
        return Math.max(vanillaHeight, clamp(finalHeight, -64.0, natagumoMaxSurfaceY()));
    }

    /** Returns the local mountain target used by the river; it never carries height downstream. */
    public double natagumoTargetSurfaceHeight(int blockX, int blockZ, double profile) {
        double clampedProfile = clamp(profile, 0.0, 1.0);
        double targetHeight = lerp(NATAGUMO_BASE_Y, NATAGUMO_TARGET_SUMMIT_Y, clampedProfile)
                + detailNoise(seed, blockX, blockZ) * clampedProfile;
        targetHeight = NATAGUMO_BASE_Y + (targetHeight - NATAGUMO_BASE_Y) * natagumoHeightScale();
        return clamp(targetHeight, NATAGUMO_BASE_Y, natagumoMaxSurfaceY());
    }

    /** Returns true only for the narrow water corridor over the raw mountain surface. */
    public boolean isRiverColumn(int blockX, int blockZ, Climate.Sampler sampler) {
        if (!EnhancedMountBiomeConfig.enhancedMountNatagumoEnabled) {
            return false;
        }
        NatagumoRegion region = getNatagumoRegion(seed, blockX, blockZ);
        if (region == null || !isNatagumoTerrainProfile(region, blockX, blockZ, sampler)) {
            return false;
        }
        return riverColumn(region, blockX, blockZ);
    }

    /** Surface passes share one region/ocean query per column. */
    public SurfaceColumn surfaceColumn(int x, int z, Climate.Sampler sampler) {
        if (!EnhancedMountBiomeConfig.enhancedMountNatagumoEnabled) {
            return new SurfaceColumn(0.0, false);
        }
        NatagumoRegion region = getNatagumoRegion(seed, x, z);
        if (!isNatagumoTerrainProfile(region, x, z, sampler)) {
            return new SurfaceColumn(0.0, false);
        }
        return new SurfaceColumn(region.strength(), riverColumn(region, x, z));
    }

    private boolean riverColumn(NatagumoRegion region, int x, int z) {
        return riverDepth() > 0 && riverPath(region).contains(x, z, RIVER_WATER_HALF_WIDTH);
    }

    /**
     * Water depth of the carved river channel. The default profile keeps the
     * original two-block trench; hairline mountains disable or shallow it so the
     * channel never dominates the hill.
     */
    public static int riverDepth() {
        double heightScale = natagumoHeightScale();
        if (heightScale >= 0.25) {
            return 2;
        }
        return (int) Math.round(2.0 * heightScale / 0.25);
    }

    public record SurfaceColumn(double profile, boolean river) { }

    /**
     * Returns the imaginary ring or sub-region containing the position. The rings use
     * the unwarped distance from the selected peak so that they remain concentric even
     * though terrain generation has small horizontal detail noise.
     */
    public static String natagumoRegionName(NatagumoRegion region, int blockX, int blockZ) {
        if (region == null) {
            return null;
        }

        double dx = blockX - region.centerX();
        double dz = blockZ - region.centerZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance > region.radius()) {
            return null;
        }

        int ring = Math.min(4, (int) (distance / (region.radius() / 5.0)));
        return switch (ring) {
            case 0 -> "Boss Ring";
            case 1 -> bossMinionsRegionName(dx, dz);
            case 2 -> "Mother Ring";
            case 3 -> "Headless Puppet Ring";
            default -> "Puppet Ring";
        };
    }

    /** Splits the Boss Minions Ring into its three compass-based regions. */
    private static String bossMinionsRegionName(double dx, double dz) {
        // Heading is measured clockwise from north: north=0, east=90, south=180.
        double heading = Math.toDegrees(Math.atan2(dx, -dz));
        if (heading < 0.0) {
            heading += 360.0;
        }
        if (heading <= 120.0) {
            return "Boss Minions Ring - Sister Region";
        }
        if (heading <= 240.0) {
            return "Boss Minions Ring - Father Region";
        }
        return "Boss Minions Ring - Brother Region";
    }

    /** Returns the profile used for biome selection and river generation. */
    public double natagumoProfile(int blockX, int blockZ, Climate.Sampler sampler) {
        if (!EnhancedMountBiomeConfig.enhancedMountNatagumoEnabled) {
            return 0.0;
        }

        NatagumoRegion region = getNatagumoRegion(seed, blockX, blockZ);
        if (!isEligibleNatagumoRegion(region, blockX, blockZ, sampler)) {
            return 0.0;
        }
        discover(region);
        return terrainStrength(region);
    }

    /** Returns the continuous X/Z-only profile used by terrain density generation. */
    public double natagumoTerrainProfile(int blockX, int blockZ, Climate.Sampler sampler) {
        if (!EnhancedMountBiomeConfig.enhancedMountNatagumoEnabled) {
            return 0.0;
        }

        NatagumoRegion region = getNatagumoRegion(seed, blockX, blockZ);
        if (region == null || (!EnhancedMountBiomeConfig.natagumoOverwriteOceans
                && (isOceanAt(blockX, blockZ, sampler)
                || isOceanAt(region.centerX(), region.centerZ(), sampler)))) {
            return 0.0;
        }
        return terrainStrength(region);
    }

    /** Returns true when both terrain and biome generation may use this Natagumo region. */
    public boolean isNatagumoTerrain(int blockX, int blockZ, Climate.Sampler sampler) {
        if (!EnhancedMountBiomeConfig.enhancedMountNatagumoEnabled) {
            return false;
        }
        NatagumoRegion region = getNatagumoRegion(seed, blockX, blockZ);
        boolean eligible = isEligibleNatagumoRegion(region, blockX, blockZ, sampler);
        if (eligible) {
            discover(region);
        }
        return eligible;
    }

    /** Returns the vertical shift applied to vanilla density sampling at a position. */
    public double terrainOffset(int blockX, int blockZ, Climate.Sampler sampler) {
        double profile = natagumoTerrainProfile(blockX, blockZ, sampler);
        double targetHeight = natagumoTargetSurfaceHeight(blockX, blockZ, profile);
        return Math.max(0.0, targetHeight - NATAGUMO_BASE_Y);
    }

    private boolean isOceanAt(int blockX, int blockZ, Climate.Sampler sampler) {
        Holder<Biome> baseBiome = delegate.getNoiseBiome(blockX >> 2, 16, blockZ >> 2, sampler);
        return baseBiome.is(BiomeTags.IS_OCEAN);
    }

    private static double distance(int centerX, int centerZ, int blockX, int blockZ) {
        return Math.sqrt(distanceSquared(centerX, centerZ, blockX, blockZ));
    }

    private static double distanceSquared(int centerX, int centerZ, int blockX, int blockZ) {
        double dx = blockX - centerX;
        double dz = blockZ - centerZ;
        return dx * dx + dz * dz;
    }

    private static double distanceSquaredToBox(int pointX, int pointZ,
                                               int minX, int minZ, int maxX, int maxZ) {
        double dx = pointX < minX ? minX - pointX : pointX > maxX ? pointX - maxX : 0.0;
        double dz = pointZ < minZ ? minZ - pointZ : pointZ > maxZ ? pointZ - maxZ : 0.0;
        return dx * dx + dz * dz;
    }

    private static double profileStrength(double distance, double radius) {
        if (distance > radius) {
            return 0.0;
        }
        double t = clamp(1.0 - distance / radius, 0.0, 1.0);
        return smootherStep(t);
    }

    private static double terrainStrength(NatagumoRegion region) {
        return region.strength();
    }

    private static double detailNoise(long seed, int blockX, int blockZ) {
        return signedNoise(seed, blockX / 220.0, blockZ / 220.0, 0x44455431) * 3.0
                + signedNoise(seed, blockX / 90.0, blockZ / 90.0, 0x44455432) * 1.5;
    }

    private static boolean isMountainClimate(Climate.TargetPoint target) {
        float continentalness = Climate.unquantizeCoord(target.continentalness());
        float erosion = Climate.unquantizeCoord(target.erosion());
        float weirdness = Climate.unquantizeCoord(target.weirdness());
        return continentalness >= EnhancedMountBiomeConfig.mountainContinentalnessMin
                && erosion <= EnhancedMountBiomeConfig.mountainErosionMax
                && Math.abs(weirdness) >= EnhancedMountBiomeConfig.mountainWeirdnessMin;
    }

    private static boolean isRegionSelected(long seed, int blockX, int blockZ,
                                            double scale, double chance, int salt) {
        double selector = valueNoise(seed ^ salt, blockX / scale, blockZ / scale);
        return selector >= 1.0 - chance;
    }

    private static double valueNoise(long seed, double x, double z) {
        int x0 = (int) Math.floor(x);
        int z0 = (int) Math.floor(z);
        double tx = smooth(x - x0);
        double tz = smooth(z - z0);
        double a = lerp(hashToUnit(seed, x0, z0), hashToUnit(seed, x0 + 1, z0), tx);
        double b = lerp(hashToUnit(seed, x0, z0 + 1), hashToUnit(seed, x0 + 1, z0 + 1), tx);
        return lerp(a, b, tz);
    }

    private static double signedNoise(long seed, double x, double z, int salt) {
        return valueNoise(seed ^ salt, x, z) * 2.0 - 1.0;
    }

    private static double hashToUnit(long seed, int x, int z) {
        long value = seed + 0x9E3779B97F4A7C15L;
        value ^= (long) x * 0xBF58476D1CE4E5B9L;
        value ^= (long) z * 0x94D049BB133111EBL;
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return (value >>> 11) * 0x1.0p-53;
    }

    private static double smooth(double value) {
        return value * value * (3.0 - 2.0 * value);
    }

    private static double smootherStep(double value) {
        return value * value * value * (value * (value * 6.0 - 15.0) + 10.0);
    }

    private static double lerp(double from, double to, double amount) {
        return from + amount * (to - from);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private boolean isEligibleNatagumoRegion(NatagumoRegion region, int blockX, int blockZ,
                                              Climate.Sampler sampler) {
        return region != null
                && region.strength() >= EnhancedMountBiomeConfig.natagumoBiomeThreshold
                && (EnhancedMountBiomeConfig.natagumoOverwriteOceans
                || (!isOceanAt(blockX, blockZ, sampler)
                && !isOceanAt(region.centerX(), region.centerZ(), sampler)));
    }

    private boolean isNatagumoTerrainProfile(NatagumoRegion region, int blockX, int blockZ,
                                              Climate.Sampler sampler) {
        return region != null && (EnhancedMountBiomeConfig.natagumoOverwriteOceans
                || (!isOceanAt(blockX, blockZ, sampler)
                && !isOceanAt(region.centerX(), region.centerZ(), sampler)));
    }

    public static double natagumoRadius() {
        return NATAGUMO_BASE_RADIUS * clamp(EnhancedMountBiomeConfig.natagumoWidthScale, 0.01, 1.0);
    }

    public static int natagumoPeakDisplayY() {
        double height = NATAGUMO_BASE_Y
                + (NATAGUMO_TARGET_SUMMIT_Y - NATAGUMO_BASE_Y) * natagumoHeightScale();
        return (int) Math.round(Math.min(natagumoMaxSurfaceY(), height));
    }

    public static int natagumoMaxSurfaceY() {
        return Math.max(100, Math.min(280, EnhancedMountBiomeConfig.natagumoMaxSurfaceY));
    }

    private static double natagumoHeightScale() {
        return clamp(EnhancedMountBiomeConfig.natagumoHeightScale, 0.01, 1.0);
    }

    private void discover(NatagumoRegion region) {
        if (discoveredRings.add(region.ring())) {
            NatagumoPeakSavedData.queue(seed, region);
        }
    }

    private static Center center(long seed, int ring, int spacing, int firstRadius) {
        CenterKey key = new CenterKey(seed, ring, spacing, firstRadius);
        Center cached = CENTERS.getIfPresent(key);
        if (cached != null) return cached;
        double ringRadius = firstRadius + (ring - 1L) * spacing;
        double angle = hashToUnit(seed, ring, 0x52494E47) * Math.PI * 2.0;
        Center result = new Center((int) Math.round(Math.cos(angle) * ringRadius),
                (int) Math.round(Math.sin(angle) * ringRadius));
        CENTERS.put(key, result);
        return result;
    }

    private record CenterKey(long seed, int ring, int spacing, int firstRadius) { }
    private record Center(int x, int z) { }

    public record NatagumoRegion(int ring, int centerX, int centerZ, double radius,
                                 double strength, double effectiveDistance) {
    }
}
