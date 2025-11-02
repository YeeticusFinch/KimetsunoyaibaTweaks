package com.lerdorf.kimetsunoyaibamultiplayer.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class BiomeConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // Biome Spawn Fix Settings
    private static final ForgeConfigSpec.BooleanValue ENABLE_BIOME_FIX = BUILDER
        .comment("Enable custom biome additions to the overworld")
        .comment("Controls spawning of custom biomes (wisteria_forest)")
        .define("enableBiomeFix", true);

    private static final ForgeConfigSpec.BooleanValue LOG_BIOME_CHANGES = BUILDER
        .comment("Log biome addition information to console")
        .define("logBiomeChanges", true);


    // Wisteria Forest biome settings (3 separate biomes by color)
    // Uses vanilla biome replacement approach for reliability
    private static final ForgeConfigSpec.DoubleValue WISTERIA_FOREST_REPLACEMENT_CHANCE = BUILDER
        .comment("")
        .comment("=== Wisteria Forest Biome Replacement Settings ===")
        .comment("Chance to START replacing vanilla forest/plains/birch_forest/flower_forest with main wisteria_forest (0.0 - 1.0)")
        .comment("When replacement starts, it creates large continuous forests via cluster expansion")
        .comment("This uses deterministic replacement - same world seed = same biome distribution")
        .comment("0.05 = 5% chance to start a wisteria forest, but forest will be large when it spawns")
        .comment("RECOMMENDED: 0.03-0.05 for rare but large, impressive wisteria forests")
        .defineInRange("wisteriaForestReplacementChance", 0.05, 0.0, 1.0);

    private static final ForgeConfigSpec.DoubleValue WISTERIA_FOREST_CYAN_REPLACEMENT_CHANCE = BUILDER
        .comment("Chance to START replacing vanilla forests with cyan wisteria_forest (0.0 - 1.0)")
        .comment("Creates large continuous cyan forests when replacement starts")
        .comment("RECOMMENDED: 0.02-0.03 for rare, impressive cyan forests")
        .defineInRange("wisteriaForestCyanReplacementChance", 0.02, 0.0, 1.0);

    private static final ForgeConfigSpec.DoubleValue WISTERIA_FOREST_CREAM_REPLACEMENT_CHANCE = BUILDER
        .comment("Chance to START replacing vanilla forests with cream wisteria_forest (0.0 - 1.0)")
        .comment("Creates large continuous cream forests when replacement starts")
        .comment("RECOMMENDED: 0.02-0.03 for rare, impressive cream forests")
        .defineInRange("wisteriaForestCreamReplacementChance", 0.02, 0.0, 1.0);

    private static final ForgeConfigSpec.IntValue WISTERIA_FOREST_CLUSTER_SIZE = BUILDER
        .comment("Cluster size for wisteria forest biome replacement (1-10)")
        .comment("When a replacement starts, this many adjacent biome points are also replaced")
        .comment("Higher values create larger, more continuous wisteria forests")
        .comment("1 = small patches, 3-5 = medium forests, 7-10 = huge forests")
        .comment("RECOMMENDED: 5-7 for large impressive forests")
        .defineInRange("wisteriaForestClusterSize", 6, 1, 10);

    // Legacy size/frequency settings (currently not used with replacement approach)
    private static final ForgeConfigSpec.DoubleValue WISTERIA_FOREST_CYAN_SIZE_MULTIPLIER = BUILDER
        .comment("")
        .comment("=== Legacy Settings (currently not used) ===")
        .comment("Size multiplier for wisteria_forest_cyan biome (1.0 = default, higher = larger biomes)")
        .comment("NOTE: Currently not used - biome replacement approach is active")
        .comment("Range: 0.5 (small) to 3.0 (very large)")
        .defineInRange("wisteriaForestCyanSizeMultiplier", 2.0, 0.5, 3.0);

    private static final ForgeConfigSpec.IntValue WISTERIA_FOREST_CYAN_SPAWN_FREQUENCY = BUILDER
        .comment("Spawn frequency for wisteria_forest_cyan biome")
        .comment("NOTE: Currently not used - biome replacement approach is active")
        .comment("Range: 1 to 5")
        .defineInRange("wisteriaForestCyanSpawnFrequency", 1, 1, 5);

    private static final ForgeConfigSpec.DoubleValue WISTERIA_FOREST_CREAM_SIZE_MULTIPLIER = BUILDER
        .comment("Size multiplier for wisteria_forest_cream biome")
        .comment("NOTE: Currently not used - biome replacement approach is active")
        .comment("Range: 0.5 (small) to 3.0 (very large)")
        .defineInRange("wisteriaForestCreamSizeMultiplier", 2.0, 0.5, 3.0);

    private static final ForgeConfigSpec.IntValue WISTERIA_FOREST_CREAM_SPAWN_FREQUENCY = BUILDER
        .comment("Spawn frequency for wisteria_forest_cream biome")
        .comment("NOTE: Currently not used - biome replacement approach is active")
        .comment("Range: 1 to 5")
        .defineInRange("wisteriaForestCreamSpawnFrequency", 1, 1, 5);

    private static final ForgeConfigSpec.DoubleValue WISTERIA_FOREST_SIZE_MULTIPLIER = BUILDER
        .comment("Size multiplier for wisteria_forest biome (lavender+pink, default)")
        .comment("NOTE: Currently not used - biome replacement approach is active")
        .comment("Range: 0.5 (small) to 3.0 (very large)")
        .defineInRange("wisteriaForestSizeMultiplier", 2.5, 0.5, 3.0);

    private static final ForgeConfigSpec.IntValue WISTERIA_FOREST_SPAWN_FREQUENCY = BUILDER
        .comment("Spawn frequency for wisteria_forest biome (lavender+pink, default)")
        .comment("NOTE: Currently not used - biome replacement approach is active")
        .comment("Range: 1 to 5")
        .defineInRange("wisteriaForestSpawnFrequency", 2, 1, 5);

    // Mount Fujikasane biome settings
    private static final ForgeConfigSpec.DoubleValue MT_FUJIKASANE_SIZE_MULTIPLIER = BUILDER
        .comment("")
        .comment("=== Mount Fujikasane Settings ===")
        .comment("Size multiplier for mt_fujikasane biome (1.0 = default, higher = larger mountain)")
        .comment("Larger values create a bigger mountain area (300-400 block radius at 1.0)")
        .comment("Range: 0.5 (small) to 2.0 (huge)")
        .defineInRange("mtFujikasaneSizeMultiplier", 1.0, 0.5, 2.0);

    private static final ForgeConfigSpec.IntValue MT_FUJIKASANE_SPAWN_FREQUENCY = BUILDER
        .comment("Spawn frequency for mt_fujikasane biome (1 = default, higher = more common)")
        .comment("RECOMMENDED: Keep at 1 for rarity. Higher values make multiple mountains spawn")
        .comment("Range: 1 to 3")
        .defineInRange("mtFujikasaneSpawnFrequency", 1, 1, 3);

    private static final ForgeConfigSpec.DoubleValue WISTERIA_RING_WIDTH_MULTIPLIER = BUILDER
        .comment("Width multiplier for the Wisteria forest ring around Mt Fujikasane (1.0 = default)")
        .comment("Controls how wide the protective Wisteria ring is around the mountain")
        .comment("Range: 0.5 (narrow) to 2.0 (wide)")
        .defineInRange("wisteriaRingWidthMultiplier", 1.0, 0.5, 2.0);


    public static final ForgeConfigSpec SPEC = BUILDER.build();

    // Cached config values
    public static boolean enableBiomeFix;
    public static boolean logBiomeChanges;

    // Biome replacement chances
    public static double wisteriaForestReplacementChance;
    public static double wisteriaForestCyanReplacementChance;
    public static double wisteriaForestCreamReplacementChance;
    public static int wisteriaForestClusterSize;

    // Legacy size and frequency for 3 wisteria forest biomes (currently not used)
    public static double wisteriaForestCyanSizeMultiplier;
    public static int wisteriaForestCyanSpawnFrequency;
    public static double wisteriaForestCreamSizeMultiplier;
    public static int wisteriaForestCreamSpawnFrequency;
    public static double wisteriaForestSizeMultiplier;  // Default lavender+pink variant
    public static int wisteriaForestSpawnFrequency;

    // Mt Fujikasane
    public static double mtFujikasaneSizeMultiplier;
    public static int mtFujikasaneSpawnFrequency;
    public static double wisteriaRingWidthMultiplier;

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event) {
        enableBiomeFix = ENABLE_BIOME_FIX.get();
        logBiomeChanges = LOG_BIOME_CHANGES.get();

        // Biome replacement chances
        wisteriaForestReplacementChance = WISTERIA_FOREST_REPLACEMENT_CHANCE.get();
        wisteriaForestCyanReplacementChance = WISTERIA_FOREST_CYAN_REPLACEMENT_CHANCE.get();
        wisteriaForestCreamReplacementChance = WISTERIA_FOREST_CREAM_REPLACEMENT_CHANCE.get();
        wisteriaForestClusterSize = WISTERIA_FOREST_CLUSTER_SIZE.get();

        // Legacy size and frequency for 3 wisteria forest biomes (currently not used)
        wisteriaForestCyanSizeMultiplier = WISTERIA_FOREST_CYAN_SIZE_MULTIPLIER.get();
        wisteriaForestCyanSpawnFrequency = WISTERIA_FOREST_CYAN_SPAWN_FREQUENCY.get();
        wisteriaForestCreamSizeMultiplier = WISTERIA_FOREST_CREAM_SIZE_MULTIPLIER.get();
        wisteriaForestCreamSpawnFrequency = WISTERIA_FOREST_CREAM_SPAWN_FREQUENCY.get();
        wisteriaForestSizeMultiplier = WISTERIA_FOREST_SIZE_MULTIPLIER.get();
        wisteriaForestSpawnFrequency = WISTERIA_FOREST_SPAWN_FREQUENCY.get();

        // Mt Fujikasane
        mtFujikasaneSizeMultiplier = MT_FUJIKASANE_SIZE_MULTIPLIER.get();
        mtFujikasaneSpawnFrequency = MT_FUJIKASANE_SPAWN_FREQUENCY.get();
        wisteriaRingWidthMultiplier = WISTERIA_RING_WIDTH_MULTIPLIER.get();

        if (logBiomeChanges) {
            com.lerdorf.kimetsunoyaibamultiplayer.Log.info("BiomeConfig loaded:");
            com.lerdorf.kimetsunoyaibamultiplayer.Log.info("  enableBiomeFix: " + enableBiomeFix);
            com.lerdorf.kimetsunoyaibamultiplayer.Log.info("  wisteriaForestReplacementChance: " + (wisteriaForestReplacementChance * 100) + "%");
            com.lerdorf.kimetsunoyaibamultiplayer.Log.info("  wisteriaForestCyanReplacementChance: " + (wisteriaForestCyanReplacementChance * 100) + "%");
            com.lerdorf.kimetsunoyaibamultiplayer.Log.info("  wisteriaForestCreamReplacementChance: " + (wisteriaForestCreamReplacementChance * 100) + "%");
            com.lerdorf.kimetsunoyaibamultiplayer.Log.info("  wisteriaForestClusterSize: " + wisteriaForestClusterSize);
        }
    }
}
