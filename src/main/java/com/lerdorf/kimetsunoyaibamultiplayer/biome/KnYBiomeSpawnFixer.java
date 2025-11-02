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

                        // Get biome holders for all 3 wisteria forest biomes
                        Holder<Biome> wisteriaForestCyan = getBiomeHolder(biomeRegistry, "kimetsunoyaibamultiplayer", "wisteria_forest_cyan");
                        Holder<Biome> wisteriaForestCream = getBiomeHolder(biomeRegistry, "kimetsunoyaibamultiplayer", "wisteria_forest_cream");
                        Holder<Biome> wisteriaForest = getBiomeHolder(biomeRegistry, "kimetsunoyaibamultiplayer", "wisteria_forest");  // Default lavender+pink

                        // Use biome replacement approach with cluster expansion
                        // This creates rare but large, continuous wisteria forests
                        int replacedMain = 0;
                        int replacedCyan = 0;
                        int replacedCream = 0;
                        int clusterSize = com.lerdorf.kimetsunoyaibamultiplayer.config.BiomeConfig.wisteriaForestClusterSize;

                        for (int i = 0; i < parameters.size(); i++) {
                            Pair<Climate.ParameterPoint, Holder<Biome>> pair = parameters.get(i);
                            Holder<Biome> originalBiome = pair.getSecond();

                            // Get the biome's resource location
                            ResourceKey<Biome> biomeKey = originalBiome.unwrapKey().orElse(null);
                            if (biomeKey == null) continue;

                            ResourceLocation biomeLoc = biomeKey.location();
                            Climate.ParameterPoint point = pair.getFirst();

                            // Check if this is a vanilla biome we want to replace
                            // We target forests, plains, and birch forests for replacement
                            boolean isTargetBiome = isTargetBiomeForReplacement(biomeLoc);

                            if (isTargetBiome) {
                                // Use deterministic replacement based on climate parameter hashcode
                                // This ensures same seed = same biome distribution (multiplayer compatible)
                                int hash = parameterPointHash(point);
                                double random = ((hash & 0x7FFFFFFF) % 10000) / 10000.0; // 0.0 to 1.0

                                // Determine which wisteria forest variant to use (if any)
                                Holder<Biome> replacementBiome = null;

                                if (wisteriaForest != null && random < com.lerdorf.kimetsunoyaibamultiplayer.config.BiomeConfig.wisteriaForestReplacementChance) {
                                    replacementBiome = wisteriaForest;
                                } else if (wisteriaForestCyan != null && random >= 0.5 && random < (0.5 + com.lerdorf.kimetsunoyaibamultiplayer.config.BiomeConfig.wisteriaForestCyanReplacementChance)) {
                                    replacementBiome = wisteriaForestCyan;
                                } else if (wisteriaForestCream != null && random >= 0.75 && random < (0.75 + com.lerdorf.kimetsunoyaibamultiplayer.config.BiomeConfig.wisteriaForestCreamReplacementChance)) {
                                    replacementBiome = wisteriaForestCream;
                                }

                                // If we decided to replace, create a CLUSTER of replacements for larger forests
                                if (replacementBiome != null) {
                                    // Replace this point
                                    parameters.set(i, new Pair<>(point, replacementBiome));

                                    // Count the replacement
                                    if (replacementBiome == wisteriaForest) replacedMain++;
                                    else if (replacementBiome == wisteriaForestCyan) replacedCyan++;
                                    else if (replacementBiome == wisteriaForestCream) replacedCream++;

                                    // CLUSTER EXPANSION: Also replace the next clusterSize-1 adjacent target biomes
                                    // This creates larger, continuous wisteria forests
                                    int clustered = 0;
                                    for (int j = i + 1; j < parameters.size() && clustered < clusterSize - 1; j++) {
                                        Pair<Climate.ParameterPoint, Holder<Biome>> nextPair = parameters.get(j);
                                        Holder<Biome> nextBiome = nextPair.getSecond();
                                        ResourceKey<Biome> nextBiomeKey = nextBiome.unwrapKey().orElse(null);

                                        if (nextBiomeKey != null && isTargetBiomeForReplacement(nextBiomeKey.location())) {
                                            // Replace this adjacent target biome with the same wisteria variant
                                            Climate.ParameterPoint nextPoint = nextPair.getFirst();
                                            parameters.set(j, new Pair<>(nextPoint, replacementBiome));

                                            // Count the replacement
                                            if (replacementBiome == wisteriaForest) replacedMain++;
                                            else if (replacementBiome == wisteriaForestCyan) replacedCyan++;
                                            else if (replacementBiome == wisteriaForestCream) replacedCream++;

                                            clustered++;
                                        }
                                    }

                                    // Skip ahead past the cluster we just created
                                    i += clustered;
                                }
                            }
                        }

                        // Update the biome source with replaced biomes
                        chunkGenerator.biomeSource = MultiNoiseBiomeSource.createFromList(new Climate.ParameterList<>(parameters));

                        if (com.lerdorf.kimetsunoyaibamultiplayer.config.BiomeConfig.logBiomeChanges) {
                            com.lerdorf.kimetsunoyaibamultiplayer.Log.info("Successfully replaced vanilla biomes with wisteria forests:");
                            com.lerdorf.kimetsunoyaibamultiplayer.Log.info("  Main wisteria forest: " + replacedMain + " locations");
                            com.lerdorf.kimetsunoyaibamultiplayer.Log.info("  Cyan wisteria forest: " + replacedCyan + " locations");
                            com.lerdorf.kimetsunoyaibamultiplayer.Log.info("  Cream wisteria forest: " + replacedCream + " locations");
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
     * Check if a biome is a target biome for wisteria forest replacement
     */
    private static boolean isTargetBiomeForReplacement(ResourceLocation biomeLoc) {
        return biomeLoc.equals(ResourceLocation.fromNamespaceAndPath("minecraft", "forest")) ||
               biomeLoc.equals(ResourceLocation.fromNamespaceAndPath("minecraft", "plains")) ||
               biomeLoc.equals(ResourceLocation.fromNamespaceAndPath("minecraft", "birch_forest")) ||
               biomeLoc.equals(ResourceLocation.fromNamespaceAndPath("minecraft", "flower_forest"));
    }

    /**
     * Create a deterministic hash from a climate parameter point
     * Used for deterministic biome replacement (same params = same replacement decision)
     */
    private static int parameterPointHash(Climate.ParameterPoint point) {
        // Combine all climate parameters into a single hash
        long temp = Double.doubleToLongBits(point.temperature().min());
        long humid = Double.doubleToLongBits(point.humidity().min());
        long cont = Double.doubleToLongBits(point.continentalness().min());
        long erosion = Double.doubleToLongBits(point.erosion().min());
        long weird = Double.doubleToLongBits(point.weirdness().min());

        // Simple but effective hash combination
        long hash = temp;
        hash = hash * 31 + humid;
        hash = hash * 31 + cont;
        hash = hash * 31 + erosion;
        hash = hash * 31 + weird;

        return (int) (hash ^ (hash >>> 32));
    }

    /**
     * Get base climate parameters for a biome (from config or defaults)
     */
    private static BiomeClimateParams getBaseClimateParams(String biomeName) {
        // All 3 wisteria forest biomes need NARROW, SPECIFIC parameters to spawn as discrete biomes
        // Wide ranges cause overlap with vanilla biomes and feature leakage
        // NOTE: Continentalness >= 0.0 to prevent ocean spawning (oceans are < -0.19)
        if (biomeName.equals("wisteria_forest_cyan")) {
            return new BiomeClimateParams(
                0.25f, 0.45f,          // Temperature: narrow cool-temperate band
                0.60f, 0.85f,          // Humidity: narrow humid band
                0.05f, 0.30f,          // Continentalness: near coast but not ocean
                -0.15f, 0.20f,         // Erosion: narrow mid-range
                0.0f, 1.0f,            // Depth: surface only (no underground)
                0.35f, 0.65f           // Weirdness: narrow positive band
            );
        } else if (biomeName.equals("wisteria_forest_cream")) {
            return new BiomeClimateParams(
                0.50f, 0.75f,          // Temperature: narrow warm-temperate band
                0.65f, 0.90f,          // Humidity: narrow humid band
                0.25f, 0.55f,          // Continentalness: mid-inland
                -0.10f, 0.25f,         // Erosion: narrow mid-range
                0.0f, 1.0f,            // Depth: surface only
                0.05f, 0.25f           // Weirdness: narrow low-positive band
            );
        } else if (biomeName.equals("wisteria_forest")) {  // Default lavender+pink variant
            return new BiomeClimateParams(
                0.35f, 0.60f,          // Temperature: narrow temperate band (distinct from others)
                0.70f, 0.95f,          // Humidity: narrow high-humid band
                0.15f, 0.45f,          // Continentalness: inland (avoid ocean < 0)
                -0.20f, 0.15f,         // Erosion: narrow mid-range
                0.0f, 1.0f,            // Depth: surface only
                -0.80f, -0.50f         // Weirdness: narrow negative band (very distinct)
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
     *
     * Uses DISCRETE, NON-OVERLAPPING weirdness bands for each frequency point.
     * This allows multiple spawn locations without causing biome overlap or feature leakage.
     *
     * Each frequency point gets its own weirdness band, creating separate discrete biomes.
     */
    private static BiomeClimateParams createParamVariation(BiomeClimateParams base, int index, int totalFrequency) {
        if (totalFrequency == 1 || index == 0) {
            // First/only spawn point uses base parameters
            return base;
        }

        // Create DISCRETE variations using non-overlapping weirdness bands
        // Each index gets a completely separate weirdness range (no overlap!)
        // This creates multiple discrete biomes at different locations

        float weirdnessWidth = base.weirdMax - base.weirdMin;
        float weirdnessCenter = (base.weirdMin + base.weirdMax) / 2f;

        // Define discrete, non-overlapping weirdness bands for each frequency
        // We spread them across the available weirdness space without overlap
        float newWeirdMin, newWeirdMax;

        switch (index) {
            case 1:
                // Second spawn point: shift weirdness to adjacent band
                newWeirdMin = clamp(base.weirdMax + 0.05f, -2f, 2f);
                newWeirdMax = clamp(newWeirdMin + weirdnessWidth, -2f, 2f);
                break;
            case 2:
                // Third spawn point: shift further
                newWeirdMin = clamp(base.weirdMax + weirdnessWidth + 0.10f, -2f, 2f);
                newWeirdMax = clamp(newWeirdMin + weirdnessWidth, -2f, 2f);
                break;
            case 3:
                // Fourth spawn point: use negative weirdness if base was positive
                if (weirdnessCenter > 0) {
                    newWeirdMax = clamp(base.weirdMin - 0.05f, -2f, 2f);
                    newWeirdMin = clamp(newWeirdMax - weirdnessWidth, -2f, 2f);
                } else {
                    newWeirdMin = clamp(base.weirdMax + weirdnessWidth * 2 + 0.15f, -2f, 2f);
                    newWeirdMax = clamp(newWeirdMin + weirdnessWidth, -2f, 2f);
                }
                break;
            case 4:
                // Fifth spawn point: another discrete band
                if (weirdnessCenter > 0) {
                    newWeirdMax = clamp(base.weirdMin - weirdnessWidth - 0.10f, -2f, 2f);
                    newWeirdMin = clamp(newWeirdMax - weirdnessWidth, -2f, 2f);
                } else {
                    newWeirdMin = clamp(base.weirdMax + weirdnessWidth * 3 + 0.20f, -2f, 2f);
                    newWeirdMax = clamp(newWeirdMin + weirdnessWidth, -2f, 2f);
                }
                break;
            default:
                // Shouldn't happen, but fallback to base
                newWeirdMin = base.weirdMin;
                newWeirdMax = base.weirdMax;
        }

        // Keep temperature, humidity, continentalness, and erosion THE SAME
        // Only change weirdness to create discrete, separate spawn locations
        return new BiomeClimateParams(
            base.tempMin, base.tempMax,
            base.humidMin, base.humidMax,
            base.contMin, base.contMax,
            base.erosionMin, base.erosionMax,
            base.depthMin, base.depthMax,
            newWeirdMin, newWeirdMax
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
