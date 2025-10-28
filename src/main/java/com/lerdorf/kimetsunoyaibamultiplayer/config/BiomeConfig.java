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


    public static final ForgeConfigSpec SPEC = BUILDER.build();

    // Cached config values
    public static boolean enableBiomeFix;
    public static boolean logBiomeChanges;

    // Size and frequency
    public static double wisteriaForestSizeMultiplier;
    public static int wisteriaForestSpawnFrequency;
    public static double mtFujikasaneSizeMultiplier;
    public static int mtFujikasaneSpawnFrequency;
    public static double wisteriaRingWidthMultiplier;

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event) {
        enableBiomeFix = ENABLE_BIOME_FIX.get();
        logBiomeChanges = LOG_BIOME_CHANGES.get();

        // Size and frequency
        wisteriaForestSizeMultiplier = WISTERIA_FOREST_SIZE_MULTIPLIER.get();
        wisteriaForestSpawnFrequency = WISTERIA_FOREST_SPAWN_FREQUENCY.get();
        mtFujikasaneSizeMultiplier = MT_FUJIKASANE_SIZE_MULTIPLIER.get();
        mtFujikasaneSpawnFrequency = MT_FUJIKASANE_SPAWN_FREQUENCY.get();
        wisteriaRingWidthMultiplier = WISTERIA_RING_WIDTH_MULTIPLIER.get();

        if (logBiomeChanges) {
            com.lerdorf.kimetsunoyaibamultiplayer.Log.info("BiomeConfig loaded:");
            com.lerdorf.kimetsunoyaibamultiplayer.Log.info("  enableBiomeFix: " + enableBiomeFix);
            com.lerdorf.kimetsunoyaibamultiplayer.Log.info("  wisteriaForestSizeMultiplier: " + wisteriaForestSizeMultiplier);
            com.lerdorf.kimetsunoyaibamultiplayer.Log.info("  wisteriaForestSpawnFrequency: " + wisteriaForestSpawnFrequency);
        }
    }
}
