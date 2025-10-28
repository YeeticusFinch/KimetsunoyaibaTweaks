package com.lerdorf.kimetsunoyaibamultiplayer.biome;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Adds custom biomes to the overworld and optionally replaces vanilla biome occurrences.
 *
 * NOTE: As of KimetsunoYaiba ver3, mt_yoko and mt_natagumo are now properly spawned by the base mod,
 * so we no longer need to fix their spawning. This handler now focuses on adding our custom biomes
 * (wisteria_forest) and optionally replacing vanilla biome occurrences with KnY biomes.
 */
@Mod.EventBusSubscriber(modid = com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer.MODID)
public class KnYBiomeSpawnFixer {

    /**
     * This runs AFTER the KimetsunoYaiba mod's biome registration (priority = LOW means we run later)
     */
    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOW)
    public static void fixBiomeSpawning(ServerAboutToStartEvent event) {
        // Check if biome fix is enabled
        if (!com.lerdorf.kimetsunoyaibamultiplayer.config.BiomeConfig.enableBiomeFix) {
            return;
        }
        MinecraftServer server = event.getServer();
        Registry<DimensionType> dimensionTypeRegistry = server.registryAccess().registryOrThrow(Registries.DIMENSION_TYPE);
        Registry<LevelStem> levelStemRegistry = server.registryAccess().registryOrThrow(Registries.LEVEL_STEM);
        Registry<Biome> biomeRegistry = server.registryAccess().registryOrThrow(Registries.BIOME);

        for (LevelStem levelStem : levelStemRegistry.stream().toList()) {
            DimensionType dimensionType = levelStem.type().value();

            // Only modify overworld
            if (dimensionType == dimensionTypeRegistry.getOrThrow(BuiltinDimensionTypes.OVERWORLD)) {
                ChunkGenerator chunkGenerator = levelStem.generator();

                if (chunkGenerator.getBiomeSource() instanceof MultiNoiseBiomeSource noiseSource) {
                    try {
                        // Get the existing biome parameters
                        List<Pair<Climate.ParameterPoint, Holder<Biome>>> parameters =
                            new ArrayList<>(noiseSource.parameters().values());

                        // Get biome holder for wisteria forest (our custom biome)
                        Holder<Biome> wisteriaForest = getBiomeHolder(biomeRegistry, "kimetsunoyaibamultiplayer", "wisteria_forest");

                        // Add wisteria_forest with configurable parameters
                        if (wisteriaForest != null) {
                            addConfigurableBiome(parameters, wisteriaForest, "wisteria_forest",
                                com.lerdorf.kimetsunoyaibamultiplayer.config.BiomeConfig.wisteriaForestSpawnFrequency,
                                com.lerdorf.kimetsunoyaibamultiplayer.config.BiomeConfig.wisteriaForestSizeMultiplier);
                        }

                        // Update the biome source with our fixed parameters
                        chunkGenerator.biomeSource = MultiNoiseBiomeSource.createFromList(new Climate.ParameterList<>(parameters));

                        // Note: featuresPerStep will be automatically lazy-initialized when needed by Minecraft
                        // We don't need to manually refresh it

                        if (com.lerdorf.kimetsunoyaibamultiplayer.config.BiomeConfig.logBiomeChanges) {
                            com.lerdorf.kimetsunoyaibamultiplayer.Log.info("Successfully added custom biomes to overworld");
                        }

                    } catch (Exception e) {
                        System.err.println("Error fixing KnY biome spawning: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    /**
     * Add a biome with configurable parameters (size, frequency, climate)
     */
    private static void addConfigurableBiome(
            List<Pair<Climate.ParameterPoint, Holder<Biome>>> parameters,
            Holder<Biome> biome,
            String biomeName,
            int spawnFrequency,
            double sizeMultiplier) {

        // Get base climate parameters (from config or defaults)
        BiomeClimateParams baseParams = getBaseClimateParams(biomeName);

        // Apply size multiplier to parameter ranges
        BiomeClimateParams scaledParams = scaleParams(baseParams, sizeMultiplier);

        // Add multiple parameter points based on spawn frequency
        for (int i = 0; i < spawnFrequency; i++) {
            // Create variations for each frequency level
            BiomeClimateParams variedParams = createParamVariation(scaledParams, i, spawnFrequency);

            Climate.ParameterPoint point = new Climate.ParameterPoint(
                variedParams.temperature,
                variedParams.humidity,
                variedParams.continentalness,
                variedParams.erosion,
                variedParams.depth,
                variedParams.weirdness,
                0L
            );

            parameters.add(new Pair<>(point, biome));
        }

        if (com.lerdorf.kimetsunoyaibamultiplayer.config.BiomeConfig.logBiomeChanges) {
            com.lerdorf.kimetsunoyaibamultiplayer.Log.info("Added biome " + biomeName +
                " with " + spawnFrequency + " spawn point(s) and " + sizeMultiplier + "x size");
        }
    }

    /**
     * Get base climate parameters for a biome (from config or defaults)
     */
    private static BiomeClimateParams getBaseClimateParams(String biomeName) {
        if (biomeName.equals("wisteria_forest")) {
            return new BiomeClimateParams(
                0.0f, 0.9f,            // Temperature: Cool to warm (avoiding frozen and hot areas)
                0.2f, 1.2f,            // Humidity: Moderate to high (forests need moisture)
                -0.2f, 0.7f,           // Continentalness: Coastal to inland (not deep ocean/extreme inland)
                -0.3f, 0.5f,           // Erosion: Low to moderate (varied terrain)
                0.0f, 0.0f,            // Depth: Surface
                -0.4f, 0.4f            // Weirdness: Relatively normal terrain
            );
        }

        // Default fallback
        return new BiomeClimateParams(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f);
    }

    /**
     * Scale climate parameters by size multiplier
     */
    private static BiomeClimateParams scaleParams(BiomeClimateParams base, double multiplier) {
        // Calculate centers
        float tempCenter = (base.tempMin + base.tempMax) / 2f;
        float humidCenter = (base.humidMin + base.humidMax) / 2f;
        float contCenter = (base.contMin + base.contMax) / 2f;
        float erosionCenter = (base.erosionMin + base.erosionMax) / 2f;
        float weirdCenter = (base.weirdMin + base.weirdMax) / 2f;

        // Calculate half-ranges
        float tempHalf = (base.tempMax - base.tempMin) / 2f;
        float humidHalf = (base.humidMax - base.humidMin) / 2f;
        float contHalf = (base.contMax - base.contMin) / 2f;
        float erosionHalf = (base.erosionMax - base.erosionMin) / 2f;
        float weirdHalf = (base.weirdMax - base.weirdMin) / 2f;

        // Apply multiplier to ranges (clamped to -2.0 to 2.0)
        float mult = (float) multiplier;
        return new BiomeClimateParams(
            clamp(tempCenter - tempHalf * mult, -2f, 2f),
            clamp(tempCenter + tempHalf * mult, -2f, 2f),
            clamp(humidCenter - humidHalf * mult, -2f, 2f),
            clamp(humidCenter + humidHalf * mult, -2f, 2f),
            clamp(contCenter - contHalf * mult, -2f, 2f),
            clamp(contCenter + contHalf * mult, -2f, 2f),
            clamp(erosionCenter - erosionHalf * mult, -2f, 2f),
            clamp(erosionCenter + erosionHalf * mult, -2f, 2f),
            base.depthMin, base.depthMax,  // Depth doesn't scale
            clamp(weirdCenter - weirdHalf * mult, -2f, 2f),
            clamp(weirdCenter + weirdHalf * mult, -2f, 2f)
        );
    }

    /**
     * Create a variation of climate parameters for multiple spawn points
     */
    private static BiomeClimateParams createParamVariation(BiomeClimateParams base, int index, int totalFrequency) {
        if (totalFrequency == 1 || index == 0) {
            // First/only spawn point uses base parameters
            return base;
        }

        // Create variations by shifting multiple climate dimensions for better distribution
        // Different frequencies shift different parameters
        float tempShift = (index == 1 || index == 4) ? ((index / (float) totalFrequency) * 0.6f - 0.3f) : 0f;
        float humidShift = (index == 2 || index == 4) ? ((index / (float) totalFrequency) * 0.6f - 0.3f) : 0f;
        float contShift = (index == 3) ? ((index / (float) totalFrequency) * 0.4f - 0.2f) : 0f;

        return new BiomeClimateParams(
            clamp(base.tempMin + tempShift, -2f, 2f),
            clamp(base.tempMax + tempShift, -2f, 2f),
            clamp(base.humidMin + humidShift, -2f, 2f),
            clamp(base.humidMax + humidShift, -2f, 2f),
            clamp(base.contMin + contShift, -2f, 2f),
            clamp(base.contMax + contShift, -2f, 2f),
            base.erosionMin, base.erosionMax,  // Keep erosion constant
            base.depthMin, base.depthMax,
            base.weirdMin, base.weirdMax
        );
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Helper class to hold climate parameter ranges
     */
    private static class BiomeClimateParams {
        final float tempMin, tempMax;
        final float humidMin, humidMax;
        final float contMin, contMax;
        final float erosionMin, erosionMax;
        final float depthMin, depthMax;
        final float weirdMin, weirdMax;

        Climate.Parameter temperature, humidity, continentalness, erosion, depth, weirdness;

        BiomeClimateParams(float tempMin, float tempMax, float humidMin, float humidMax,
                          float contMin, float contMax, float erosionMin, float erosionMax,
                          float depthMin, float depthMax, float weirdMin, float weirdMax) {
            this.tempMin = tempMin;
            this.tempMax = tempMax;
            this.humidMin = humidMin;
            this.humidMax = humidMax;
            this.contMin = contMin;
            this.contMax = contMax;
            this.erosionMin = erosionMin;
            this.erosionMax = erosionMax;
            this.depthMin = depthMin;
            this.depthMax = depthMax;
            this.weirdMin = weirdMin;
            this.weirdMax = weirdMax;

            // Create Climate.Parameter objects
            this.temperature = (tempMin == tempMax) ? Climate.Parameter.point(tempMin) : Climate.Parameter.span(tempMin, tempMax);
            this.humidity = (humidMin == humidMax) ? Climate.Parameter.point(humidMin) : Climate.Parameter.span(humidMin, humidMax);
            this.continentalness = (contMin == contMax) ? Climate.Parameter.point(contMin) : Climate.Parameter.span(contMin, contMax);
            this.erosion = (erosionMin == erosionMax) ? Climate.Parameter.point(erosionMin) : Climate.Parameter.span(erosionMin, erosionMax);
            this.depth = (depthMin == depthMax) ? Climate.Parameter.point(depthMin) : Climate.Parameter.span(depthMin, depthMax);
            this.weirdness = (weirdMin == weirdMax) ? Climate.Parameter.point(weirdMin) : Climate.Parameter.span(weirdMin, weirdMax);
        }
    }


    /**
     * Get a biome holder from the registry
     */
    private static Holder<Biome> getBiomeHolder(Registry<Biome> registry, String namespace, String path) {
        try {
            ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME,
                ResourceLocation.fromNamespaceAndPath(namespace, path));
            return registry.getHolder(key).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
