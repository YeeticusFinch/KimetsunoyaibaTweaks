package com.lerdorf.kimetsunoyaibamultiplayer.biome;

import com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedMountBiomeConfig;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import com.mojang.serialization.Codec;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Keeps the base biome source's climate selection, substituting enhanced mount biomes
 * only when the sampled climate describes mountainous inland terrain.
 */
public final class EnhancedMountBiomeSource extends BiomeSource {
    private static final int NATAGUMO_SALT = 0x4E415441;
    private static final int YOKO_SALT = 0x594F4B4F;

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
        // This source is installed after the server's registries are loaded and is never decoded from data.
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
        Climate.TargetPoint target = sampler.sample(quartX, quartY, quartZ);

        if (isMountainClimate(target)) {
            int blockX = quartX * 4;
            int blockZ = quartZ * 4;

            if (natagumo != null && EnhancedMountBiomeConfig.enhancedMountNatagumoEnabled
                    && isRegionSelected(seed, blockX, blockZ,
                    EnhancedMountBiomeConfig.natagumoNoiseScale,
                    EnhancedMountBiomeConfig.natagumoMountainChance, NATAGUMO_SALT)) {
                return natagumo;
            }
            if (yoko != null && EnhancedMountBiomeConfig.enhancedMountYokoEnabled
                    && isRegionSelected(seed, blockX, blockZ,
                    EnhancedMountBiomeConfig.yokoNoiseScale,
                    EnhancedMountBiomeConfig.yokoMountainChance, YOKO_SALT)) {
                return yoko;
            }
        }

        return delegate.getNoiseBiome(quartX, quartY, quartZ, sampler);
    }

    @Override
    public void addDebugInfo(java.util.List<String> lines, net.minecraft.core.BlockPos pos,
                             Climate.Sampler sampler) {
        delegate.addDebugInfo(lines, pos, sampler);
    }

    public static boolean isMountainClimate(Climate.TargetPoint target) {
        float continentalness = Climate.unquantizeCoord(target.continentalness());
        float erosion = Climate.unquantizeCoord(target.erosion());
        float weirdness = Climate.unquantizeCoord(target.weirdness());
        return continentalness >= EnhancedMountBiomeConfig.mountainContinentalnessMin
                && erosion <= EnhancedMountBiomeConfig.mountainErosionMax
                && Math.abs(weirdness) >= EnhancedMountBiomeConfig.mountainWeirdnessMin;
    }

    /** Returns the large-scale selector used by both biome placement and structure filtering. */
    public static boolean isRegionSelected(long seed, int blockX, int blockZ,
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

}
