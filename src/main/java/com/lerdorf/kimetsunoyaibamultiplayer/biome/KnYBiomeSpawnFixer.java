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
 * Fixes the biome spawning issue in KimetsunoYaiba mod where mt_natagumo and mt_yoko don't spawn.
 *
 * The problem: All three biomes (mt_sagiri, mt_yoko, mt_natagumo) have identical climate parameters,
 * so they compete for the same locations and only mt_sagiri spawns.
 *
 * The solution: We inject biomes with unique climate parameters after the KnY mod has registered them,
 * and optionally replace some vanilla biome occurrences with KnY biomes based on seed-deterministic logic.
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

                        // Get biome holders
                        Holder<Biome> mtYoko = getBiomeHolder(biomeRegistry, "kimetsunoyaiba", "mt_yoko");
                        Holder<Biome> mtNatagumo = getBiomeHolder(biomeRegistry, "kimetsunoyaiba", "mt_natagumo");
                        Holder<Biome> mtSagiri = getBiomeHolder(biomeRegistry, "kimetsunoyaiba", "mt_sagiri");
                        Holder<Biome> mugen = getBiomeHolder(biomeRegistry, "kimetsunoyaiba", "mugen_biome");
                        Holder<Biome> wisteriaForest = getBiomeHolder(biomeRegistry, "kimetsunoyaibamultiplayer", "wisteria_forest");

                        if (mtYoko == null || mtNatagumo == null) {
                            com.lerdorf.kimetsunoyaibamultiplayer.Log.info("KnY biomes not found - skipping biome spawn fix");
                            return;
                        }

                        // Remove existing conflicting entries (the ones added by KnY mod with identical parameters)
                        removeConflictingBiomeEntries(parameters, mtYoko, mtNatagumo, mtSagiri);

                        // Add mt_yoko with configurable parameters
                        addConfigurableBiome(parameters, mtYoko, "mt_yoko",
                            com.lerdorf.kimetsunoyaibamultiplayer.config.BiomeConfig.mtYokoSpawnFrequency,
                            com.lerdorf.kimetsunoyaibamultiplayer.config.BiomeConfig.mtYokoSizeMultiplier);

                        // Add mt_natagumo with configurable parameters
                        addConfigurableBiome(parameters, mtNatagumo, "mt_natagumo",
                            com.lerdorf.kimetsunoyaibamultiplayer.config.BiomeConfig.mtNatagumoSpawnFrequency,
                            com.lerdorf.kimetsunoyaibamultiplayer.config.BiomeConfig.mtNatagumoSizeMultiplier);
                        
                        // Add mugen_biome with configurable parameters
                        addConfigurableBiome(parameters, mugen, "mugen_biome",
                                com.lerdorf.kimetsunoyaibamultiplayer.config.BiomeConfig.mtNatagumoSpawnFrequency,
                                com.lerdorf.kimetsunoyaibamultiplayer.config.BiomeConfig.mtNatagumoSizeMultiplier);

                        // Add wisteria_forest with configurable parameters
                        if (wisteriaForest != null) {
                            addConfigurableBiome(parameters, wisteriaForest, "wisteria_forest",
                                com.lerdorf.kimetsunoyaibamultiplayer.config.BiomeConfig.wisteriaForestSpawnFrequency,
                                com.lerdorf.kimetsunoyaibamultiplayer.config.BiomeConfig.wisteriaForestSizeMultiplier);
                        }

                        // Optionally replace some vanilla biomes with mt_yoko and mt_natagumo
                        // This is deterministic based on climate parameter hashcodes
                        if (com.lerdorf.kimetsunoyaibamultiplayer.config.BiomeConfig.enableVanillaReplacement) {
                            addDeterministicBiomeReplacements(parameters, biomeRegistry, mtYoko, mtNatagumo, mugen);
                        }

                        // Update the biome source with our fixed parameters
                        chunkGenerator.biomeSource = MultiNoiseBiomeSource.createFromList(new Climate.ParameterList<>(parameters));

                        // Note: featuresPerStep will be automatically lazy-initialized when needed by Minecraft
                        // We don't need to manually refresh it

                        if (com.lerdorf.kimetsunoyaibamultiplayer.config.BiomeConfig.logBiomeChanges) {
                            com.lerdorf.kimetsunoyaibamultiplayer.Log.info("Successfully fixed KnY biome spawning for mt_yoko and mt_natagumo");
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
     * Remove the conflicting biome entries that were added by the KnY mod
     */
    private static void removeConflictingBiomeEntries(
            List<Pair<Climate.ParameterPoint, Holder<Biome>>> parameters,
            Holder<Biome> mtYoko,
            Holder<Biome> mtNatagumo,
            Holder<Biome> mtSagiri) {

        parameters.removeIf(pair -> {
            Holder<Biome> biome = pair.getSecond();
            // Remove ALL instances of mt_yoko and mt_natagumo (we'll add them back with fixed parameters)
            // Keep mt_sagiri as-is since it works fine
            return biome.equals(mtYoko) || biome.equals(mtNatagumo);
        });
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
        boolean useCustom = com.lerdorf.kimetsunoyaibamultiplayer.config.BiomeConfig.useCustomClimateParams;

        if (biomeName.equals("mt_yoko")) {
            float tempMin = useCustom ? (float) com.lerdorf.kimetsunoyaibamultiplayer.config.BiomeConfig.mtYokoTempMin : -0.5f;
            float tempMax = useCustom ? (float) com.lerdorf.kimetsunoyaibamultiplayer.config.BiomeConfig.mtYokoTempMax : 0.3f;
            float humidMin = useCustom ? (float) com.lerdorf.kimetsunoyaibamultiplayer.config.BiomeConfig.mtYokoHumidityMin : 0.0f;
            float humidMax = useCustom ? (float) com.lerdorf.kimetsunoyaibamultiplayer.config.BiomeConfig.mtYokoHumidityMax : 1.0f;

            return new BiomeClimateParams(
                tempMin, tempMax,      // Temperature: Cool to Cold
                humidMin, humidMax,    // Humidity: Moderate to High
                0.2f, 0.8f,            // Continentalness: Inland/mountainous
                -0.5f, 0.2f,           // Erosion: Low (high mountains)
                0.0f, 0.0f,            // Depth: Surface
                -0.3f, 0.5f            // Weirdness: Slightly weird
            );
        } else if (biomeName.equals("mt_natagumo")) {
            float tempMin = useCustom ? (float) com.lerdorf.kimetsunoyaibamultiplayer.config.BiomeConfig.mtNatagumoTempMin : -0.8f;
            float tempMax = useCustom ? (float) com.lerdorf.kimetsunoyaibamultiplayer.config.BiomeConfig.mtNatagumoTempMax : 0.0f;
            float humidMin = useCustom ? (float) com.lerdorf.kimetsunoyaibamultiplayer.config.BiomeConfig.mtNatagumoHumidityMin : -0.3f;
            float humidMax = useCustom ? (float) com.lerdorf.kimetsunoyaibamultiplayer.config.BiomeConfig.mtNatagumoHumidityMax : 0.3f;

            return new BiomeClimateParams(
                tempMin, tempMax,      // Temperature: Cold to Very Cold
                humidMin, humidMax,    // Humidity: Dry to Moderate
                0.0f, 0.6f,            // Continentalness: Inland/forest mountains
                -0.2f, 0.5f,           // Erosion: Moderate (lower mountains)
                0.0f, 0.0f,            // Depth: Surface
                0.3f, 1.0f             // Weirdness: Very weird (spooky)
            );
        } else if (biomeName.equals("wisteria_forest")) {
            return new BiomeClimateParams(
                0.0f, 0.9f,            // Temperature: Cool to warm (avoiding frozen and hot areas)
                0.2f, 1.2f,            // Humidity: Moderate to high (forests need moisture)
                -0.2f, 0.7f,           // Continentalness: Coastal to inland (not deep ocean/extreme inland)
                -0.3f, 0.5f,           // Erosion: Low to moderate (varied terrain)
                0.0f, 0.0f,            // Depth: Surface
                -0.4f, 0.4f            // Weirdness: Relatively normal terrain
            );
        } else if (biomeName.equals("mugen_biome")) {
            return new BiomeClimateParams(
                0.5f, 1.5f,            // Temperature: Warm to hot (desert-like for infinity castle)
                -0.5f, 0.2f,           // Humidity: Dry to moderate
                0.3f, 0.9f,            // Continentalness: Inland
                0.2f, 0.8f,            // Erosion: Moderate to high (flatter terrain)
                0.0f, 0.0f,            // Depth: Surface
                0.5f, 1.2f             // Weirdness: Weird to very weird (otherworldly)
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
     * Replace some vanilla biome occurrences with KnY biomes based on deterministic logic.
     * Uses climate parameter hashcodes for determinism - same world setup = same result.
     */
    private static void addDeterministicBiomeReplacements(
            List<Pair<Climate.ParameterPoint, Holder<Biome>>> parameters,
            Registry<Biome> biomeRegistry,
            Holder<Biome> mtYoko,
            Holder<Biome> mtNatagumo,
            Holder<Biome> mugen) {

        // Get vanilla biomes to partially replace
        Holder<Biome> taiga = getBiomeHolder(biomeRegistry, "minecraft", "taiga");
        Holder<Biome> darkForest = getBiomeHolder(biomeRegistry, "minecraft", "dark_forest");
        Holder<Biome> oldGrowthSpruceTaiga = getBiomeHolder(biomeRegistry, "minecraft", "old_growth_spruce_taiga");
        Holder<Biome> savanna = getBiomeHolder(biomeRegistry, "minecraft", "savanna");

        // Get replacement chances from config
        double mtYokoChance = com.lerdorf.kimetsunoyaibamultiplayer.config.BiomeConfig.mtYokoReplacementChance;
        double mtNatagumoChance = com.lerdorf.kimetsunoyaibamultiplayer.config.BiomeConfig.mtNatagumoReplacementChance;
        double mugenChance = com.lerdorf.kimetsunoyaibamultiplayer.config.BiomeConfig.mugenReplacementChance;

        // Find taiga-like biomes and replace some with mt_yoko
        if (taiga != null) {
            replaceSomeBiomeOccurrences(parameters, taiga, mtYoko, mtYokoChance, "taiga -> mt_yoko");
        }
        if (oldGrowthSpruceTaiga != null) {
            replaceSomeBiomeOccurrences(parameters, oldGrowthSpruceTaiga, mtYoko, mtYokoChance * 0.7, "old_growth_spruce_taiga -> mt_yoko");
        }

        // Find dark forest biomes and replace some with mt_natagumo
        if (darkForest != null) {
            replaceSomeBiomeOccurrences(parameters, darkForest, mtNatagumo, mtNatagumoChance, "dark_forest -> mt_natagumo");
        }
        
        if (savanna != null) {
            replaceSomeBiomeOccurrences(parameters, savanna, mtNatagumo, mugenChance, "dark_forest -> mugen_biome");
        }
    }

    /**
     * Replace a percentage of occurrences of a vanilla biome with a modded biome.
     * Uses climate parameter hashcode for deterministic selection.
     */
    private static void replaceSomeBiomeOccurrences(
            List<Pair<Climate.ParameterPoint, Holder<Biome>>> parameters,
            Holder<Biome> vanillaBiome,
            Holder<Biome> moddedBiome,
            double replacementChance,
            String logMessage) {

        int replaced = 0;
        int total = 0;

        for (int i = 0; i < parameters.size(); i++) {
            Pair<Climate.ParameterPoint, Holder<Biome>> pair = parameters.get(i);

            if (pair.getSecond().equals(vanillaBiome)) {
                total++;

                // Use deterministic random based on parameter hashcode
                // Same climate parameters = same decision every time
                Random paramRandom = new Random(pair.getFirst().hashCode());

                if (paramRandom.nextDouble() < replacementChance) {
                    // Replace this occurrence
                    parameters.set(i, new Pair<>(pair.getFirst(), moddedBiome));
                    replaced++;
                }
            }
        }

        if (replaced > 0 && com.lerdorf.kimetsunoyaibamultiplayer.config.BiomeConfig.logBiomeChanges) {
            com.lerdorf.kimetsunoyaibamultiplayer.Log.info(
                "Replaced " + replaced + "/" + total + " occurrences: " + logMessage);
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
