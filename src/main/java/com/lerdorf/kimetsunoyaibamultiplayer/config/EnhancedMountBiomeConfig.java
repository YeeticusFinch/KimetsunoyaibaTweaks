package com.lerdorf.kimetsunoyaibamultiplayer.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/** Configuration for enhanced Mount Natagumo and Mount Yoko world generation. */
@Mod.EventBusSubscriber(modid = com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class EnhancedMountBiomeConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue ENHANCED_MOUNT_NATAGUMO_ENABLED = BUILDER
            .comment("Replace the base mod's broad Mount Natagumo climate placement with deterministic ring regions")
            .define("enhanced-mount-natagumo-enabled", true);

    private static final ForgeConfigSpec.IntValue NATAGUMO_RING_SPACING = BUILDER
            .comment("Distance between Mount Natagumo spawn rings, in blocks")
            .defineInRange("natagumo-ring-spacing", 3000, 1000, 10000);

    private static final ForgeConfigSpec.IntValue NATAGUMO_FIRST_RING_RADIUS = BUILDER
            .comment("Radius of the first Mount Natagumo spawn ring, in blocks")
            .defineInRange("natagumo-first-ring-radius", 3000, 1000, 10000);

    private static final ForgeConfigSpec.DoubleValue NATAGUMO_BIOME_THRESHOLD = BUILDER
            .comment("Minimum Natagumo terrain strength required for the Mount Natagumo biome")
            .defineInRange("natagumo-biome-threshold", 0.05, 0.0, 1.0);

    private static final ForgeConfigSpec.IntValue NATAGUMO_PEAK_HEIGHT = BUILDER
            .comment("Maximum added height of the Natagumo mountain profile, in blocks")
            .defineInRange("natagumo-peak-height", 170, 20, 250);

    private static final ForgeConfigSpec.IntValue NATAGUMO_MAX_SURFACE_Y = BUILDER
            .comment("Absolute maximum surface Y for the Natagumo mountain")
            .defineInRange("natagumo-max-surface-y", 230, 100, 320);

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
    public static int natagumoRingSpacing;
    public static int natagumoFirstRingRadius;
    public static double natagumoBiomeThreshold;
    public static int natagumoPeakHeight;
    public static int natagumoMaxSurfaceY;
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
        natagumoRingSpacing = NATAGUMO_RING_SPACING.get();
        natagumoFirstRingRadius = NATAGUMO_FIRST_RING_RADIUS.get();
        natagumoBiomeThreshold = NATAGUMO_BIOME_THRESHOLD.get();
        natagumoPeakHeight = NATAGUMO_PEAK_HEIGHT.get();
        natagumoMaxSurfaceY = NATAGUMO_MAX_SURFACE_Y.get();
        enhancedMountYokoEnabled = ENHANCED_MOUNT_YOKO_ENABLED.get();
        yokoMountainChance = YOKO_MOUNTAIN_CHANCE.get();
        yokoNoiseScale = YOKO_NOISE_SCALE.get();
        mountainContinentalnessMin = MOUNTAIN_CONTINENTALNESS_MIN.get();
        mountainErosionMax = MOUNTAIN_EROSION_MAX.get();
        mountainWeirdnessMin = MOUNTAIN_WEIRDNESS_MIN.get();
    }
}
