package com.lerdorf.kimetsunoyaibamultiplayer.config;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * Configuration for Enhanced Breathing Forms
 *
 * This config controls whether enhanced versions of breathing techniques should be enabled.
 * When enabled, base mod nichirin swords are automatically replaced with enhanced versions
 * that have improved/additional breathing forms.
 *
 * Config file location: config/kimetsunoyaibamultiplayer/enhanced-breathing.toml
 */
public class EnhancedBreathingConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    static {
        BUILDER.comment("Enhanced Breathing Forms Configuration",
                       "Controls which breathing styles use enhanced versions with improved forms")
               .push("enhanced_breathing");
    }

    // Mist Breathing Enhancement
    private static final ForgeConfigSpec.BooleanValue ENHANCED_MIST_BREATHING = BUILDER
            .comment("Enable enhanced Mist Breathing forms",
                    "When true, automatically replaces base mod mist swords with enhanced versions:",
                    "  - kimetsunoyaiba:nichirinsword_mist -> kimetsunoyaibamultiplayer:nichirinsword_mist",
                    "  - kimetsunoyaiba:nichirinsword_tokito -> kimetsunoyaibamultiplayer:nichirinsword_muichiro",
                    "",
                    "Enhanced features:",
                    "  - 7th Form: Obscuring Clouds (Muichiro's original technique)",
                    "  - Improved particle effects and animations",
                    "  - Custom sword slash models",
                    "",
                    "Default: true")
            .define("enhancedMistBreathing", true);
    

    // Flower Breathing Enhancement
    private static final ForgeConfigSpec.BooleanValue ENHANCED_FLOWER_BREATHING = BUILDER
            .comment("Enable enhanced Flower Breathing sword replacement",
                    "When true, automatically replaces base mod flower swords with enhanced versions:",
                    "  - kimetsunoyaiba:nichirinsword_kanawo -> kimetsunoyaibamultiplayer:nichirinsword_kanawo",
                    "  - kimetsunoyaiba:nichirinsword_kanae -> kimetsunoyaibamultiplayer:nichirinsword_kanae",
                    "",
                    "Enhanced features:",
                    "  - Hashira-exclusive forms (7th, 8th, 9th for Kanae's sword)",
                    "  - Final Form: Equinoctial Vermillion Eye",
                    "  - Improved particle effects and animations",
                    "",
                    "Default: true")
            .define("enhancedFlowerBreathing", true);

    // Beast Breathing Enhancement
    private static final ForgeConfigSpec.BooleanValue ENHANCED_BEAST_BREATHING = BUILDER
            .comment("Enable enhanced Beast Breathing forms",
                    "When true, automatically replaces base mod Inosuke swords with enhanced versions:",
                    "  - kimetsunoyaiba:nichirinsword_inosuke -> kimetsunoyaibamultiplayer:nichirinsword_inosuke",
                    "",
                    "Enhanced features:",
                    "  - Uses Enhanced Beast Breathing technique implementation",
                    "  - Integrates with dual-wield behavior when paired in offhand",
                    "",
                    "Default: true")
            .define("enhancedBeastBreathing", true);

    // Love Breathing Enhancement
    private static final ForgeConfigSpec.BooleanValue ENHANCED_LOVE_BREATHING = BUILDER
            .comment("Enable enhanced Love Breathing forms",
                    "When true, automatically replaces base mod love swords with enhanced versions:",
                    "  - kimetsunoyaiba:nichirinsword_kanroji -> kimetsunoyaibamultiplayer:nichirinsword_kanroji",
                    "",
                    "Enhanced features:",
                    "  - Flexible whip-like sword rendering",
                    "  - 6 Love Breathing forms",
                    "  - Dual-layer emissive rendering with heart particle trails",
                    "  - Physics-based whip motion with keyframe animations",
                    "",
                    "Default: true")
            .define("enhancedLoveBreathing", true);

    // Black Sword Enhancement
    private static final ForgeConfigSpec.BooleanValue ENHANCED_BLACK_SWORD = BUILDER
            .comment("Enable enhanced Black Sword replacement",
                    "When true, automatically replaces base mod black sword with enhanced version:",
                    "  - kimetsunoyaiba:nichirinsword_black -> kimetsunoyaibamultiplayer:nichirinsword_black",
                    "",
                    "Enhanced features:",
                    "  - Uses enhanced Black Sword technique implementation",
                    "  - Improved particle effects and animations",
                    "",
                    "Default: true")
            .define("enhancedBlackSword", true);

    static {
        BUILDER.pop(); // enhanced_breathing

        // Love Breathing Whip Physics Settings
        BUILDER.comment("Love Breathing Whip Physics Configuration",
                       "Controls the visual and physics behavior of Mitsuri's whip sword")
               .push("love_whip_physics");
    }

    // Love Sword Configuration
    private static final ForgeConfigSpec.BooleanValue DISABLE_LOVE_M1_TRAIL_PARTICLES = BUILDER
            .comment("Whether or not to disable the love sword swing particles",
            "Default: false")
            .define("disableLoveM1TrailParticles", false);

    // Whip Segment Configuration
    private static final ForgeConfigSpec.IntValue WHIP_SEGMENT_COUNT = BUILDER
            .comment("Number of whip segments (higher = smoother but more expensive)",
                    "Default: 12")
            .defineInRange("segmentCount", 12, 4, 24);

    private static final ForgeConfigSpec.DoubleValue WHIP_SEGMENT_LENGTH = BUILDER
            .comment("Rest length of each whip segment in blocks",
                    "Default: 0.3")
            .defineInRange("segmentLength", 0.3, 0.1, 1.0);

    // Physics Parameters
    private static final ForgeConfigSpec.DoubleValue WHIP_STIFFNESS = BUILDER
            .comment("Spring stiffness (higher = stiffer, less floppy)",
                    "Default: 0.4")
            .defineInRange("stiffness", 0.4, 0.1, 1.0);

    private static final ForgeConfigSpec.DoubleValue WHIP_DAMPING = BUILDER
            .comment("Damping factor (higher = less bouncy)",
                    "Default: 0.85")
            .defineInRange("damping", 0.85, 0.5, 0.99);

    private static final ForgeConfigSpec.DoubleValue WHIP_GRAVITY = BUILDER
            .comment("Gravity effect on whip (blocks/tick²)",
                    "Default: 0.02")
            .defineInRange("gravity", 0.02, 0.0, 0.1);

    // Length Configuration
    private static final ForgeConfigSpec.DoubleValue WHIP_IDLE_LENGTH = BUILDER
            .comment("Whip length when idle (in blocks)",
                    "Default: 2.5")
            .defineInRange("idleLength", 2.5, 1.0, 5.0);

    private static final ForgeConfigSpec.DoubleValue WHIP_EXTENDED_LENGTH = BUILDER
            .comment("Whip length when fully extended during attacks (in blocks)",
                    "Default: 8.0")
            .defineInRange("extendedLength", 8.0, 3.0, 15.0);

    private static final ForgeConfigSpec.IntValue WHIP_EXTENSION_TICKS = BUILDER
            .comment("Ticks to transition between idle and extended length",
                    "Default: 10")
            .defineInRange("extensionTicks", 10, 1, 40);

    // Rendering Configuration
    private static final ForgeConfigSpec.IntValue WHIP_CURVE_RESOLUTION = BUILDER
            .comment("Number of interpolation samples along curve (higher = smoother)",
                    "Default: 24")
            .defineInRange("curveResolution", 24, 6, 64);

    private static final ForgeConfigSpec.DoubleValue WHIP_WIDTH = BUILDER
            .comment("Whip ribbon width",
                    "Default: 0.08")
            .defineInRange("width", 0.08, 0.02, 0.3);

    private static final ForgeConfigSpec.BooleanValue WHIP_EMISSIVE = BUILDER
            .comment("Enable emissive (glowing) rendering for whip",
                    "Default: true")
            .define("emissive", true);

    private static final ForgeConfigSpec.DoubleValue WHIP_EMISSIVE_BRIGHTNESS = BUILDER
            .comment("Emissive layer brightness (0.0 - 1.0)",
                    "Default: 0.9")
            .defineInRange("emissiveBrightness", 0.9, 0.0, 1.0);

    // Particle Configuration
    private static final ForgeConfigSpec.IntValue WHIP_PARTICLE_DENSITY = BUILDER
            .comment("Particle spawn density (particles per block of whip length per tick)",
                    "0 = disabled, higher = more particles",
                    "Default: 2")
            .defineInRange("particleDensity", 2, 0, 10);

    private static final ForgeConfigSpec.BooleanValue WHIP_PARTICLE_IDLE = BUILDER
            .comment("Spawn particles when whip is idle (not attacking)",
                    "Default: false")
            .define("particleIdle", false);

    // Animation Blending
    private static final ForgeConfigSpec.DoubleValue WHIP_KEYFRAME_BLEND = BUILDER
            .comment("Blend factor between keyframe animation and physics (0.0 = pure physics, 1.0 = pure keyframe)",
                    "Default: 0.7")
            .defineInRange("keyframeBlend", 0.7, 0.0, 1.0);

    static {
        BUILDER.pop(); // love_whip_physics
        // Note: enhanced_breathing category was already popped above before pushing love_whip_physics
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    // Public static fields for easy access
    public static boolean enhancedMistBreathing;
    public static boolean enhancedFlowerBreathing;
    public static boolean enhancedBeastBreathing;
    public static boolean enhancedLoveBreathing;
    public static boolean enhancedBlackSword;
    public static boolean disableLoveM1TrailParticles;

    // Whip Physics Settings
    public static int whipSegmentCount;
    public static double whipSegmentLength;
    public static double whipStiffness;
    public static double whipDamping;
    public static double whipGravity;
    public static double whipIdleLength;
    public static double whipExtendedLength;
    public static int whipExtensionTicks;

    // Whip Rendering Settings
    public static int whipCurveResolution;
    public static double whipWidth;
    public static boolean whipEmissive;
    public static double whipEmissiveBrightness;

    // Whip Particle Settings
    public static int whipParticleDensity;
    public static boolean whipParticleIdle;

    // Animation Blending
    public static double whipKeyframeBlend;

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event) {
        enhancedMistBreathing = ENHANCED_MIST_BREATHING.get();
        enhancedFlowerBreathing = ENHANCED_FLOWER_BREATHING.get();
        enhancedBeastBreathing = ENHANCED_BEAST_BREATHING.get();
        enhancedLoveBreathing = ENHANCED_LOVE_BREATHING.get();
        enhancedBlackSword = ENHANCED_BLACK_SWORD.get();
        disableLoveM1TrailParticles = DISABLE_LOVE_M1_TRAIL_PARTICLES.get();

        // Load whip physics settings
        whipSegmentCount = WHIP_SEGMENT_COUNT.get();
        whipSegmentLength = WHIP_SEGMENT_LENGTH.get();
        whipStiffness = WHIP_STIFFNESS.get();
        whipDamping = WHIP_DAMPING.get();
        whipGravity = WHIP_GRAVITY.get();
        whipIdleLength = WHIP_IDLE_LENGTH.get();
        whipExtendedLength = WHIP_EXTENDED_LENGTH.get();
        whipExtensionTicks = WHIP_EXTENSION_TICKS.get();

        // Load whip rendering settings
        whipCurveResolution = WHIP_CURVE_RESOLUTION.get();
        whipWidth = WHIP_WIDTH.get();
        whipEmissive = WHIP_EMISSIVE.get();
        whipEmissiveBrightness = WHIP_EMISSIVE_BRIGHTNESS.get();

        // Load whip particle settings
        whipParticleDensity = WHIP_PARTICLE_DENSITY.get();
        whipParticleIdle = WHIP_PARTICLE_IDLE.get();

        // Load animation blending
        whipKeyframeBlend = WHIP_KEYFRAME_BLEND.get();

        Log.debug("[Enhanced Breathing Config] Loaded:");
        Log.debug("  - Enhanced Mist Breathing: " + enhancedMistBreathing);
        Log.debug("  - Enhanced Flower Breathing: " + enhancedFlowerBreathing);
        Log.debug("  - Enhanced Beast Breathing: " + enhancedBeastBreathing);
        Log.debug("  - Enhanced Love Breathing: " + enhancedLoveBreathing);
        Log.debug("  - Enhanced Black Sword: " + enhancedBlackSword);
        Log.debug("  - Disable Love/Kanroji M1 Trail Particles: " + disableLoveM1TrailParticles);
        Log.debug("  - Whip Segments: " + whipSegmentCount);
        Log.debug("  - Whip Extended Length: " + whipExtendedLength);
    }
}
