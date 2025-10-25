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
        .comment("Enable biome spawn fix for mt_yoko and mt_natagumo from KimetsunoYaiba mod")
        .comment("This fixes the issue where these biomes don't spawn in the overworld")
        .define("enableBiomeFix", true);

    private static final ForgeConfigSpec.BooleanValue ENABLE_VANILLA_REPLACEMENT = BUILDER
        .comment("Enable replacement of some vanilla biome occurrences with KnY biomes")
        .comment("This is seed-based and deterministic - same seed = same world")
        .define("enableVanillaReplacement", true);

    private static final ForgeConfigSpec.DoubleValue MT_YOKO_REPLACEMENT_CHANCE = BUILDER
        .comment("Chance to replace vanilla taiga-like biomes with mt_yoko (0.0 - 1.0)")
        .comment("Default: 0.15 (15% of taiga biomes)")
        .defineInRange("mtYokoReplacementChance", 0.15, 0.0, 1.0);

    private static final ForgeConfigSpec.DoubleValue MT_NATAGUMO_REPLACEMENT_CHANCE = BUILDER
        .comment("Chance to replace vanilla dark forest biomes with mt_natagumo (0.0 - 1.0)")
        .comment("Default: 0.10 (10% of dark forests)")
        .defineInRange("mtNatagumoReplacementChance", 0.10, 0.0, 1.0);
    
    private static final ForgeConfigSpec.DoubleValue MUGEN_REPLACEMENT_CHANCE = BUILDER
        .comment("Chance to replace vanilla savanna-like biomes with mugen_biome (0.0 - 1.0)")
        .comment("Default: 0.15 (15% of savanna biomes)")
        .defineInRange("mtYokoReplacementChance", 0.15, 0.0, 1.0);

    private static final ForgeConfigSpec.BooleanValue LOG_BIOME_CHANGES = BUILDER
        .comment("Log biome replacement information to console")
        .define("logBiomeChanges", true);

    // Biome Size and Frequency Settings
    private static final ForgeConfigSpec.DoubleValue MT_YOKO_SIZE_MULTIPLIER = BUILDER
        .comment("")
        .comment("=== Biome Size and Spawn Frequency Settings ===")
        .comment("Size multiplier for mt_yoko biome (1.0 = default, higher = larger biomes)")
        .comment("Adjusts climate parameter ranges - larger values create bigger continuous biome areas")
        .comment("Range: 0.5 (small) to 3.0 (very large)")
        .defineInRange("mtYokoSizeMultiplier", 1.0, 0.5, 3.0);

    private static final ForgeConfigSpec.DoubleValue MT_NATAGUMO_SIZE_MULTIPLIER = BUILDER
        .comment("Size multiplier for mt_natagumo biome (1.0 = default, higher = larger biomes)")
        .comment("Adjusts climate parameter ranges - larger values create bigger continuous biome areas")
        .comment("Range: 0.5 (small) to 3.0 (very large)")
        .defineInRange("mtNatagumoSizeMultiplier", 1.0, 0.5, 3.0);

    private static final ForgeConfigSpec.IntValue MT_YOKO_SPAWN_FREQUENCY = BUILDER
        .comment("Spawn frequency for mt_yoko biome (1 = default, higher = more common)")
        .comment("Number of different climate parameter points where this biome can spawn")
        .comment("Higher values make the biome spawn in more varied locations")
        .comment("Range: 1 to 5")
        .defineInRange("mtYokoSpawnFrequency", 1, 1, 5);

    private static final ForgeConfigSpec.IntValue MT_NATAGUMO_SPAWN_FREQUENCY = BUILDER
        .comment("Spawn frequency for mt_natagumo biome (1 = default, higher = more common)")
        .comment("Number of different climate parameter points where this biome can spawn")
        .comment("Higher values make the biome spawn in more varied locations")
        .comment("Range: 1 to 5")
        .defineInRange("mtNatagumoSpawnFrequency", 1, 1, 5);

    // Wisteria Forest biome settings
    private static final ForgeConfigSpec.DoubleValue WISTERIA_FOREST_SIZE_MULTIPLIER = BUILDER
        .comment("Size multiplier for wisteria_forest biome (1.0 = default, higher = larger biomes)")
        .comment("Adjusts climate parameter ranges - larger values create bigger continuous biome areas")
        .comment("Range: 0.5 (small) to 3.0 (very large)")
        .defineInRange("wisteriaForestSizeMultiplier", 0.8, 0.5, 3.0);

    private static final ForgeConfigSpec.IntValue WISTERIA_FOREST_SPAWN_FREQUENCY = BUILDER
        .comment("Spawn frequency for wisteria_forest biome (1 = default, higher = more common)")
        .comment("Number of different climate parameter points where this biome can spawn")
        .comment("Higher values make the biome spawn in more varied locations")
        .comment("Range: 1 to 5")
        .defineInRange("wisteriaForestSpawnFrequency", 1, 1, 5);

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

    // Advanced Climate Parameter Overrides
    private static final ForgeConfigSpec.BooleanValue USE_CUSTOM_CLIMATE_PARAMS = BUILDER
        .comment("")
        .comment("=== Advanced Settings - Climate Parameter Adjustments ===")
        .comment("These allow fine-tuning where biomes spawn. Default values are recommended.")
        .comment("Temperature: -2.0 (frozen) to 2.0 (hot)")
        .comment("Humidity: -2.0 (dry) to 2.0 (wet)")
        .comment("Continentalness: -2.0 (ocean) to 2.0 (inland)")
        .comment("Erosion: -2.0 (low erosion/peaks) to 2.0 (high erosion/valleys)")
        .comment("Weirdness: -2.0 (normal) to 2.0 (weird terrain)")
        .comment("Enable custom climate parameter overrides (false = use defaults)")
        .define("useCustomClimateParams", false);

    // Mt Yoko climate overrides
    private static final ForgeConfigSpec.DoubleValue MT_YOKO_TEMP_MIN = BUILDER
        .comment("Mt Yoko minimum temperature")
        .defineInRange("mtYokoTempMin", -0.5, -2.0, 2.0);

    private static final ForgeConfigSpec.DoubleValue MT_YOKO_TEMP_MAX = BUILDER
        .comment("Mt Yoko maximum temperature")
        .defineInRange("mtYokoTempMax", 0.3, -2.0, 2.0);

    private static final ForgeConfigSpec.DoubleValue MT_YOKO_HUMIDITY_MIN = BUILDER
        .comment("Mt Yoko minimum humidity")
        .defineInRange("mtYokoHumidityMin", 0.0, -2.0, 2.0);

    private static final ForgeConfigSpec.DoubleValue MT_YOKO_HUMIDITY_MAX = BUILDER
        .comment("Mt Yoko maximum humidity")
        .defineInRange("mtYokoHumidityMax", 1.0, -2.0, 2.0);

    // Mt Natagumo climate overrides
    private static final ForgeConfigSpec.DoubleValue MT_NATAGUMO_TEMP_MIN = BUILDER
        .comment("Mt Natagumo minimum temperature")
        .defineInRange("mtNatagumoTempMin", -0.8, -2.0, 2.0);

    private static final ForgeConfigSpec.DoubleValue MT_NATAGUMO_TEMP_MAX = BUILDER
        .comment("Mt Natagumo maximum temperature")
        .defineInRange("mtNatagumoTempMax", 0.0, -2.0, 2.0);

    private static final ForgeConfigSpec.DoubleValue MT_NATAGUMO_HUMIDITY_MIN = BUILDER
        .comment("Mt Natagumo minimum humidity")
        .defineInRange("mtNatagumoHumidityMin", -0.3, -2.0, 2.0);

    private static final ForgeConfigSpec.DoubleValue MT_NATAGUMO_HUMIDITY_MAX = BUILDER
        .comment("Mt Natagumo maximum humidity")
        .defineInRange("mtNatagumoHumidityMax", 0.3, -2.0, 2.0);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    // Cached config values
    public static boolean enableBiomeFix;
    public static boolean enableVanillaReplacement;
    public static double mtYokoReplacementChance;
    public static double mtNatagumoReplacementChance;
	public static double mugenReplacementChance;
    public static boolean logBiomeChanges;

    // Size and frequency
    public static double mtYokoSizeMultiplier;
    public static double mtNatagumoSizeMultiplier;
    public static int mtYokoSpawnFrequency;
    public static int mtNatagumoSpawnFrequency;
    public static double wisteriaForestSizeMultiplier;
    public static int wisteriaForestSpawnFrequency;
    public static double mtFujikasaneSizeMultiplier;
    public static int mtFujikasaneSpawnFrequency;
    public static double wisteriaRingWidthMultiplier;

    // Advanced climate parameters
    public static boolean useCustomClimateParams;
    public static double mtYokoTempMin;
    public static double mtYokoTempMax;
    public static double mtYokoHumidityMin;
    public static double mtYokoHumidityMax;
    public static double mtNatagumoTempMin;
    public static double mtNatagumoTempMax;
    public static double mtNatagumoHumidityMin;
    public static double mtNatagumoHumidityMax;

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event) {
        enableBiomeFix = ENABLE_BIOME_FIX.get();
        enableVanillaReplacement = ENABLE_VANILLA_REPLACEMENT.get();
        mtYokoReplacementChance = MT_YOKO_REPLACEMENT_CHANCE.get();
        mtNatagumoReplacementChance = MT_NATAGUMO_REPLACEMENT_CHANCE.get();
        mugenReplacementChance = MUGEN_REPLACEMENT_CHANCE.get();
        logBiomeChanges = LOG_BIOME_CHANGES.get();

        // Size and frequency
        mtYokoSizeMultiplier = MT_YOKO_SIZE_MULTIPLIER.get();
        mtNatagumoSizeMultiplier = MT_NATAGUMO_SIZE_MULTIPLIER.get();
        mtYokoSpawnFrequency = MT_YOKO_SPAWN_FREQUENCY.get();
        mtNatagumoSpawnFrequency = MT_NATAGUMO_SPAWN_FREQUENCY.get();
        wisteriaForestSizeMultiplier = WISTERIA_FOREST_SIZE_MULTIPLIER.get();
        wisteriaForestSpawnFrequency = WISTERIA_FOREST_SPAWN_FREQUENCY.get();
        mtFujikasaneSizeMultiplier = MT_FUJIKASANE_SIZE_MULTIPLIER.get();
        mtFujikasaneSpawnFrequency = MT_FUJIKASANE_SPAWN_FREQUENCY.get();
        wisteriaRingWidthMultiplier = WISTERIA_RING_WIDTH_MULTIPLIER.get();

        // Advanced climate parameters
        useCustomClimateParams = USE_CUSTOM_CLIMATE_PARAMS.get();
        mtYokoTempMin = MT_YOKO_TEMP_MIN.get();
        mtYokoTempMax = MT_YOKO_TEMP_MAX.get();
        mtYokoHumidityMin = MT_YOKO_HUMIDITY_MIN.get();
        mtYokoHumidityMax = MT_YOKO_HUMIDITY_MAX.get();
        mtNatagumoTempMin = MT_NATAGUMO_TEMP_MIN.get();
        mtNatagumoTempMax = MT_NATAGUMO_TEMP_MAX.get();
        mtNatagumoHumidityMin = MT_NATAGUMO_HUMIDITY_MIN.get();
        mtNatagumoHumidityMax = MT_NATAGUMO_HUMIDITY_MAX.get();

        if (logBiomeChanges) {
            com.lerdorf.kimetsunoyaibamultiplayer.Log.info("BiomeConfig loaded:");
            com.lerdorf.kimetsunoyaibamultiplayer.Log.info("  enableBiomeFix: " + enableBiomeFix);
            com.lerdorf.kimetsunoyaibamultiplayer.Log.info("  enableVanillaReplacement: " + enableVanillaReplacement);
            com.lerdorf.kimetsunoyaibamultiplayer.Log.info("  mtYokoReplacementChance: " + mtYokoReplacementChance);
            com.lerdorf.kimetsunoyaibamultiplayer.Log.info("  mtNatagumoReplacementChance: " + mtNatagumoReplacementChance);
            com.lerdorf.kimetsunoyaibamultiplayer.Log.info("  mtYokoSizeMultiplier: " + mtYokoSizeMultiplier);
            com.lerdorf.kimetsunoyaibamultiplayer.Log.info("  mtNatagumoSizeMultiplier: " + mtNatagumoSizeMultiplier);
            com.lerdorf.kimetsunoyaibamultiplayer.Log.info("  mtYokoSpawnFrequency: " + mtYokoSpawnFrequency);
            com.lerdorf.kimetsunoyaibamultiplayer.Log.info("  mtNatagumoSpawnFrequency: " + mtNatagumoSpawnFrequency);
            if (useCustomClimateParams) {
                com.lerdorf.kimetsunoyaibamultiplayer.Log.info("  Using custom climate parameters");
            }
        }
    }
}
