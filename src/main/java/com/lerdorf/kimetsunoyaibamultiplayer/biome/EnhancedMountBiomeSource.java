package com.lerdorf.kimetsunoyaibamultiplayer.biome;

import com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedMountBiomeConfig;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Adds the base mod's Mount Natagumo biome to the same deterministic regions
 * whose density field is raised by the Natagumo density hook.
 */
public final class EnhancedMountBiomeSource extends BiomeSource {
    private static final int YOKO_SALT = 0x594F4B4F;
    private static final double NATAGUMO_RADIUS = 800.0;
    private static final double NATAGUMO_INNER_RADIUS = 700.0;
    private static final double NATAGUMO_OUTER_RADIUS = 850.0;

    private final BiomeSource delegate;
    private final long seed;
    private final Holder<Biome> natagumo;
    private final Holder<Biome> yoko;

    public EnhancedMountBiomeSource(BiomeSource delegate, long seed,
                                    Holder<Biome> natagumo, Holder<Biome> yoko) {
        this.delegate = delegate;
        this.seed = seed;
        this.natagumo = natagumo;
        this.yoko = yoko;
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
        NatagumoRegion region = getNatagumoRegion(seed, blockX, blockZ);

        if (natagumo != null && EnhancedMountBiomeConfig.enhancedMountNatagumoEnabled
                && region != null
                && region.strength() >= EnhancedMountBiomeConfig.natagumoBiomeThreshold
                && !isOceanAt(blockX, blockZ, sampler)
                && !isOceanAt(region.centerX(), region.centerZ(), sampler)) {
            return natagumo;
        }

        // Keep the original enhanced Yoko behavior for the separate Yoko feature.
        Climate.TargetPoint target = sampler.sample(quartX, quartY, quartZ);
        if (yoko != null && EnhancedMountBiomeConfig.enhancedMountYokoEnabled
                && isMountainClimate(target)
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
        for (int ring = Math.max(1, nearestRing - 1); ring <= nearestRing + 1; ring++) {
            double ringRadius = firstRadius + (ring - 1L) * spacing;
            double angle = hashToUnit(seed, ring, 0x52494E47) * Math.PI * 2.0;
            int centerX = (int) Math.round(Math.cos(angle) * ringRadius);
            int centerZ = (int) Math.round(Math.sin(angle) * ringRadius);
            double strength = profileStrength(seed, centerX, centerZ, blockX, blockZ);
            if (strength > 0.0 && (best == null || strength > best.strength())) {
                best = new NatagumoRegion(ring, centerX, centerZ, NATAGUMO_RADIUS, strength);
            }
        }
        return best;
    }

    public static double natagumoStrength(long seed, int blockX, int blockZ) {
        NatagumoRegion region = getNatagumoRegion(seed, blockX, blockZ);
        return region == null ? 0.0 : region.strength();
    }

    /** Returns true when both terrain and biome generation may use this Natagumo region. */
    public boolean isNatagumoTerrain(int blockX, int blockZ, Climate.Sampler sampler) {
        NatagumoRegion region = getNatagumoRegion(seed, blockX, blockZ);
        return EnhancedMountBiomeConfig.enhancedMountNatagumoEnabled
                && region != null
                && region.strength() >= EnhancedMountBiomeConfig.natagumoBiomeThreshold
                && !isOceanAt(blockX, blockZ, sampler)
                && !isOceanAt(region.centerX(), region.centerZ(), sampler);
    }

    /** Returns the vertical shift applied to vanilla density sampling at a position. */
    public int terrainOffset(int blockX, int blockZ, Climate.Sampler sampler) {
        if (!isNatagumoTerrain(blockX, blockZ, sampler)) {
            return 0;
        }
        NatagumoRegion region = getNatagumoRegion(seed, blockX, blockZ);
        double terrainStrength = terrainStrength(seed, region.centerX(), region.centerZ(), blockX, blockZ)
                * region.strength();
        double addedHeight = terrainStrength * EnhancedMountBiomeConfig.natagumoPeakHeight
                + detailNoise(seed, blockX, blockZ) * terrainStrength;
        int maximumOffset = Math.max(0, EnhancedMountBiomeConfig.natagumoMaxSurfaceY - 60);
        return Math.max(0, Math.min(maximumOffset, (int) Math.round(addedHeight)));
    }

    private boolean isOceanAt(int blockX, int blockZ, Climate.Sampler sampler) {
        Holder<Biome> baseBiome = delegate.getNoiseBiome(blockX >> 2, 16, blockZ >> 2, sampler);
        return baseBiome.is(BiomeTags.IS_OCEAN);
    }

    private static double profileStrength(long seed, int centerX, int centerZ, int blockX, int blockZ) {
        double dx = blockX - centerX;
        double dz = blockZ - centerZ;
        double distance = Math.sqrt(dx * dx + dz * dz);
        double effectiveDistance = distance
                + signedNoise(seed, blockX / 300.0, blockZ / 300.0, 0x44495354) * 100.0;
        if (effectiveDistance <= NATAGUMO_INNER_RADIUS) {
            return 1.0;
        }
        double t = clamp((NATAGUMO_OUTER_RADIUS - effectiveDistance)
                / (NATAGUMO_OUTER_RADIUS - NATAGUMO_INNER_RADIUS), 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    private static double terrainStrength(long seed, int centerX, int centerZ, int blockX, int blockZ) {
        double dx = blockX - centerX;
        double dz = blockZ - centerZ;
        double distance = Math.sqrt(dx * dx + dz * dz);
        double effectiveDistance = distance
                + signedNoise(seed, blockX / 300.0, blockZ / 300.0, 0x44495354) * 100.0;
        double t = clamp(1.0 - effectiveDistance / NATAGUMO_RADIUS, 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    private static double detailNoise(long seed, int blockX, int blockZ) {
        return signedNoise(seed, blockX / 180.0, blockZ / 180.0, 0x44455431) * 18.0
                + signedNoise(seed, blockX / 65.0, blockZ / 65.0, 0x44455432) * 7.0;
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

    private static double lerp(double from, double to, double amount) {
        return from + amount * (to - from);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public record NatagumoRegion(int ring, int centerX, int centerZ, double radius, double strength) {
    }
}
