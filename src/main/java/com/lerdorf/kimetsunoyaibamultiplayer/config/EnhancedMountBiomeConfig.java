package com.lerdorf.kimetsunoyaibamultiplayer.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/** Configuration for the seed-stable, mountain-only Mount Natagumo and Mount Yoko biomes. */
@Mod.EventBusSubscriber(modid = com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class EnhancedMountBiomeConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue ENHANCED_MOUNT_NATAGUMO_ENABLED = BUILDER
            .comment("Replace the base mod's Mount Natagumo climate placement with mountain-only placement")
            .define("enhanced-mount-natagumo-enabled", true);

    private static final ForgeConfigSpec.DoubleValue NATAGUMO_MOUNTAIN_CHANCE = BUILDER
            .comment("Approximate fraction of mountainous terrain selected for Mount Natagumo")
            .defineInRange("natagumo-mountain-chance", 0.10, 0.0, 1.0);

    private static final ForgeConfigSpec.DoubleValue NATAGUMO_NOISE_SCALE = BUILDER
            .comment("Horizontal scale of Mount Natagumo regions, in blocks")
            .defineInRange("natagumo-noise-scale", 1200.0, 800.0, 2000.0);

    private static final ForgeConfigSpec.BooleanValue ENHANCED_MOUNT_YOKO_ENABLED = BUILDER
            .comment("Replace the base mod's Mount Yoko climate placement with mountain-only placement")
            .define("enhanced-mount-yoko-enabled", true);

    private static final ForgeConfigSpec.DoubleValue YOKO_MOUNTAIN_CHANCE = BUILDER
            .comment("Approximate fraction of mountainous terrain selected for Mount Yoko")
            .defineInRange("yoko-mountain-chance", 0.10, 0.0, 1.0);

    private static final ForgeConfigSpec.DoubleValue YOKO_NOISE_SCALE = BUILDER
            .comment("Horizontal scale of Mount Yoko regions, in blocks")
            .defineInRange("yoko-noise-scale", 1200.0, 800.0, 2000.0);

    private static final ForgeConfigSpec.DoubleValue MOUNTAIN_CONTINENTALNESS_MIN = BUILDER
            .comment("Minimum continentalness for enhanced mount placement")
            .defineInRange("mountain-continentalness-min", 0.03, -1.0, 1.0);

    private static final ForgeConfigSpec.DoubleValue MOUNTAIN_EROSION_MAX = BUILDER
            .comment("Maximum erosion for enhanced mount placement; lower values are more mountainous")
            .defineInRange("mountain-erosion-max", 0.0, -1.0, 1.0);

    private static final ForgeConfigSpec.DoubleValue MOUNTAIN_WEIRDNESS_MIN = BUILDER
            .comment("Minimum absolute weirdness for enhanced mount placement")
            .defineInRange("mountain-weirdness-min", 0.35, 0.0, 1.0);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean enhancedMountNatagumoEnabled;
    public static double natagumoMountainChance;
    public static double natagumoNoiseScale;
    public static boolean enhancedMountYokoEnabled;
    public static double yokoMountainChance;
    public static double yokoNoiseScale;
    public static double mountainContinentalnessMin;
    public static double mountainErosionMax;
    public static double mountainWeirdnessMin;

    private EnhancedMountBiomeConfig() {
    }

    public static boolean anyEnhancedMountEnabled() {
        return enhancedMountNatagumoEnabled || enhancedMountYokoEnabled;
    }

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event) {
        enhancedMountNatagumoEnabled = ENHANCED_MOUNT_NATAGUMO_ENABLED.get();
        natagumoMountainChance = NATAGUMO_MOUNTAIN_CHANCE.get();
        natagumoNoiseScale = NATAGUMO_NOISE_SCALE.get();
        enhancedMountYokoEnabled = ENHANCED_MOUNT_YOKO_ENABLED.get();
        yokoMountainChance = YOKO_MOUNTAIN_CHANCE.get();
        yokoNoiseScale = YOKO_NOISE_SCALE.get();
        mountainContinentalnessMin = MOUNTAIN_CONTINENTALNESS_MIN.get();
        mountainErosionMax = MOUNTAIN_EROSION_MAX.get();
        mountainWeirdnessMin = MOUNTAIN_WEIRDNESS_MIN.get();
    }
}
