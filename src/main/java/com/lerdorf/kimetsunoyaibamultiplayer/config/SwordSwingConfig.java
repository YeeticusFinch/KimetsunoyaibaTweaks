package com.lerdorf.kimetsunoyaibamultiplayer.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;

public class SwordSwingConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // ========================================
    // GENERAL SETTINGS
    // ========================================
    static {
        BUILDER.comment(
                "=".repeat(60),
                "Sword Swing 3D Model Configuration",
                "Configure 3D sword slash models that replace particles",
                "=".repeat(60)
        ).push("general");
    }

    private static final ForgeConfigSpec.BooleanValue USE_SWORD_SWING_MODEL = BUILDER
            .comment(
                    "Enable 3D sword slash models instead of particles",
                    "Set to true for animated 3D slashes, false for particle effects",
                    "Default: true"
            )
            .define("use_sword_swing_model", true);

    private static final ForgeConfigSpec.DoubleValue MODEL_SCALE = BUILDER
            .comment(
                    "Scale/size of the sword slash models",
                    "Higher values = larger slashes",
                    "Range: 0.1 to 5.0, Default: 0.5"
            )
            .defineInRange("model_scale", 0.5, 0.01, 5.0);

    private static final ForgeConfigSpec.IntValue ANIMATION_DURATION_MS = BUILDER
            .comment(
                    "Duration of slash animation in milliseconds",
                    "Lower values = faster slashes",
                    "Range: 50 to 1000, Default: 150 (3 ticks)"
            )
            .defineInRange("animation_duration_ms", 150, 50, 1000);

    private static final ForgeConfigSpec.DoubleValue BRIGHTNESS_MULTIPLIER = BUILDER
            .comment(
                    "Brightness multiplier for sword slash models",
                    "Higher values = brighter glow (values >1.0 create bloom with shaders)",
                    "Range: 1.0 to 10.0, Default: 5.0"
            )
            .defineInRange("brightness_multiplier", 5.0, 1.0, 10.0);

    static {
        BUILDER.pop(); // general
    }

    // ========================================
    // ADVANCED SETTINGS
    // ========================================
    static {
        BUILDER.comment(
                "",
                "=".repeat(60),
                "ADVANCED SETTINGS",
                "Fine-tune rotation and positioning for each slash direction",
                "WARNING: Only modify these if you know what you're doing!",
                "=".repeat(60)
        ).push("advanced");
    }

    // --- Global Offsets ---
    static {
        BUILDER.comment(
                "",
                "Global offsets applied to ALL slash types",
                "Use these for overall adjustments across all animations"
        ).push("global");
    }

    private static final ForgeConfigSpec.DoubleValue GLOBAL_YAW_OFFSET = BUILDER
            .comment("Global yaw rotation offset (degrees)", "Range: -180 to 180, Default: 0")
            .defineInRange("yaw_offset", 0.0, -180.0, 180.0);

    private static final ForgeConfigSpec.DoubleValue GLOBAL_PITCH_OFFSET = BUILDER
            .comment("Global pitch rotation offset (degrees)", "Range: -180 to 180, Default: 0")
            .defineInRange("pitch_offset", 0.0, -180.0, 180.0);

    private static final ForgeConfigSpec.DoubleValue GLOBAL_ROLL_OFFSET = BUILDER
            .comment("Global roll rotation offset (degrees)", "Range: -180 to 180, Default: 0")
            .defineInRange("roll_offset", 0.0, -180.0, 180.0);

    private static final ForgeConfigSpec.DoubleValue GLOBAL_RADIUS_MULT = BUILDER
            .comment("Global radius multiplier", "Default: 0.7")
            .defineInRange("radius_mult", 0.7, -5.0, 5.0);

    static {
        BUILDER.pop(); // global
    }

    // --- Sword to Right ---
    static {
        BUILDER.comment(
                "",
                "Horizontal slash from left to right",
                "Animation: sword_to_right"
        ).push("sword_to_right");
    }

    private static final ForgeConfigSpec.DoubleValue RIGHT_YAW_OFFSET = BUILDER
            .comment("Yaw offset for right slash (degrees)", "Default: 90")
            .defineInRange("yaw_offset", 90.0, -180.0, 180.0);

    private static final ForgeConfigSpec.DoubleValue RIGHT_PITCH = BUILDER
            .comment("Pitch angle for right slash (degrees)", "Default: 0")
            .defineInRange("pitch", 0.0, -180.0, 180.0);

    private static final ForgeConfigSpec.DoubleValue RIGHT_ROLL = BUILDER
            .comment("Roll angle for right slash (degrees)", "Default: -20")
            .defineInRange("roll", -20.0, -180.0, 180.0);

    private static final ForgeConfigSpec.DoubleValue RIGHT_ARC_OFFSET = BUILDER
            .comment("Arc angle offset for right slash (degrees)", "Default: -50")
            .defineInRange("arc_offset", -50.0, -180.0, 180.0);

    private static final ForgeConfigSpec.DoubleValue RIGHT_RADIUS_MULT = BUILDER
            .comment("Radius multiplier for right slash", "Default: 1")
            .defineInRange("right_radius_mult", 1.0, -5.0, 5.0);

    static {
        BUILDER.pop(); // sword_to_right
    }

    // --- Sword to Left ---
    static {
        BUILDER.comment(
                "",
                "Horizontal slash from right to left",
                "Animation: sword_to_left"
        ).push("sword_to_left");
    }

    private static final ForgeConfigSpec.DoubleValue LEFT_YAW_OFFSET = BUILDER
            .comment("Yaw offset for left slash (degrees)", "Default: -90")
            .defineInRange("yaw_offset", -90.0, -180.0, 180.0);

    private static final ForgeConfigSpec.DoubleValue LEFT_PITCH = BUILDER
            .comment("Pitch angle for left slash (degrees)", "Default: 0")
            .defineInRange("pitch", 0.0, -180.0, 180.0);

    private static final ForgeConfigSpec.DoubleValue LEFT_ROLL = BUILDER
            .comment("Roll angle for left slash (degrees)", "Default: -20")
            .defineInRange("roll", -20.0, -180.0, 180.0);

    private static final ForgeConfigSpec.DoubleValue LEFT_ARC_OFFSET = BUILDER
            .comment("Arc angle offset for left slash (degrees)", "Default: -50")
            .defineInRange("arc_offset", -50.0, -180.0, 180.0);

    private static final ForgeConfigSpec.DoubleValue LEFT_RADIUS_MULT = BUILDER
            .comment("Radius multiplier for left slash", "Default: 1")
            .defineInRange("left_radius_mult", 1.0, -5.0, 5.0);

    static {
        BUILDER.pop(); // sword_to_left
    }

    // --- Sword Overhead ---
    static {
        BUILDER.comment(
                "",
                "Vertical slash from top to bottom",
                "Animation: sword_overhead"
        ).push("sword_overhead");
    }

    private static final ForgeConfigSpec.DoubleValue OVERHEAD_YAW_OFFSET = BUILDER
            .comment("Yaw offset for overhead slash (degrees)", "Default: 0")
            .defineInRange("yaw_offset", 0.0, -180.0, 180.0);

    private static final ForgeConfigSpec.DoubleValue OVERHEAD_PITCH = BUILDER
            .comment("Pitch angle for overhead slash (degrees)", "Default: -90")
            .defineInRange("pitch", -90.0, -180.0, 180.0);

    private static final ForgeConfigSpec.DoubleValue OVERHEAD_ROLL = BUILDER
            .comment("Roll angle for overhead slash (degrees)", "Default: 110")
            .defineInRange("roll", 110.0, -180.0, 180.0);

    private static final ForgeConfigSpec.DoubleValue OVERHEAD_ARC_OFFSET = BUILDER
            .comment("Arc angle offset for overhead slash (degrees)", "Default: -20")
            .defineInRange("arc_offset", -20.0, -180.0, 180.0);

    private static final ForgeConfigSpec.DoubleValue OVERHEAD_RADIUS_MULT = BUILDER
            .comment("Radius multiplier for overhead slash", "Default: 1")
            .defineInRange("overhead_radius_mult", 1.0, -5.0, 5.0);
    
    static {
        BUILDER.pop(); // sword_overhead
    }

    // --- Sword to Upper ---
    static {
        BUILDER.comment(
                "",
                "Vertical slash from bottom to top",
                "Animation: sword_to_upper"
        ).push("sword_to_upper");
    }

    private static final ForgeConfigSpec.DoubleValue UPPER_YAW_OFFSET = BUILDER
            .comment("Yaw offset for upward slash (degrees)", "Default: 0")
            .defineInRange("yaw_offset", 0.0, -180.0, 180.0);

    private static final ForgeConfigSpec.DoubleValue UPPER_PITCH = BUILDER
            .comment("Pitch angle for upward slash (degrees)", "Default: 90")
            .defineInRange("pitch", 90.0, -180.0, 180.0);

    private static final ForgeConfigSpec.DoubleValue UPPER_ROLL = BUILDER
            .comment("Roll angle for upward slash (degrees)", "Default: 90")
            .defineInRange("roll", 90.0, -180.0, 180.0);

    private static final ForgeConfigSpec.DoubleValue UPPER_ARC_OFFSET = BUILDER
            .comment("Arc angle offset for upward slash (degrees)", "Default: 0")
            .defineInRange("arc_offset", 0.0, -180.0, 180.0);

    private static final ForgeConfigSpec.DoubleValue UPPER_RADIUS_MULT = BUILDER
            .comment("Radius multiplier for upper slash", "Default: 1")
            .defineInRange("upper_radius_mult", 1.0, -5.0, 5.0);

    static {
        BUILDER.pop(); // sword_to_upper
    }

    // --- Sword Rotate ---
    static {
        BUILDER.comment(
                "",
                "360-degree spin attack",
                "Animation: sword_rotate"
        ).push("sword_rotate");
    }

    private static final ForgeConfigSpec.DoubleValue ROTATE_YAW_OFFSET = BUILDER
            .comment("Yaw offset for spin attack (degrees)", "Default: -90")
            .defineInRange("yaw_offset", -90.0, -180.0, 180.0);

    private static final ForgeConfigSpec.DoubleValue ROTATE_PITCH = BUILDER
            .comment("Pitch angle for spin attack (degrees)", "Default: 0")
            .defineInRange("pitch", 0.0, -180.0, 180.0);

    private static final ForgeConfigSpec.DoubleValue ROTATE_ROLL = BUILDER
            .comment("Roll angle for spin attack (degrees)", "Default: 0")
            .defineInRange("roll", 0.0, -180.0, 180.0);

    private static final ForgeConfigSpec.DoubleValue ROTATE_ARC_OFFSET = BUILDER
            .comment("Arc angle offset for spin attack (degrees)", "Default: 50")
            .defineInRange("arc_offset", 50.0, -180.0, 180.0);

    private static final ForgeConfigSpec.DoubleValue ROTATE_RADIUS_MULT = BUILDER
            .comment("Radius multiplier for spin attack", "Default: 1")
            .defineInRange("rotate_radius_mult", 1.0, -5.0, 5.0);
    
    static {
        BUILDER.pop(); // sword_rotate
        BUILDER.pop(); // advanced
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    // ========================================
    // PUBLIC STATIC FIELDS
    // ========================================

    // General
    public static boolean useSwordSwingModel;
    public static float modelScale;
    public static int animationDurationMs;
    public static float brightnessMultiplier;

    // Global offsets
    public static float globalYawOffset;
    public static float globalPitchOffset;
    public static float globalRollOffset;
    public static float globalRadiusMult;

    // Sword to Right
    public static double rightYawOffset;
    public static float rightPitch;
    public static float rightRoll;
    public static double rightArcOffset;
    public static float rightRadiusMult;

    // Sword to Left
    public static double leftYawOffset;
    public static float leftPitch;
    public static float leftRoll;
    public static double leftArcOffset;
    public static float leftRadiusMult;

    // Sword Overhead
    public static double overheadYawOffset;
    public static float overheadPitch;
    public static float overheadRoll;
    public static double overheadArcOffset;
    public static float overheadRadiusMult;

    // Sword to Upper
    public static double upperYawOffset;
    public static float upperPitch;
    public static float upperRoll;
    public static double upperArcOffset;
    public static float upperRadiusMult;

    // Sword Rotate
    public static double rotateYawOffset;
    public static float rotatePitch;
    public static float rotateRoll;
    public static double rotateArcOffset;
    public static float rotateRadiusMult;

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event) {
        // General
        useSwordSwingModel = USE_SWORD_SWING_MODEL.get();
        modelScale = (float) (double) MODEL_SCALE.get();
        animationDurationMs = ANIMATION_DURATION_MS.get();
        brightnessMultiplier = (float) (double) BRIGHTNESS_MULTIPLIER.get();

        // Global offsets
        globalYawOffset = (float) (double) GLOBAL_YAW_OFFSET.get();
        globalPitchOffset = (float) (double) GLOBAL_PITCH_OFFSET.get();
        globalRollOffset = (float) (double) GLOBAL_ROLL_OFFSET.get();;
        globalRadiusMult = (float) (double) GLOBAL_RADIUS_MULT.get();

        // Sword to Right
        rightYawOffset = RIGHT_YAW_OFFSET.get();
        rightPitch = (float) (double) RIGHT_PITCH.get();
        rightRoll = (float) (double) RIGHT_ROLL.get();
        rightArcOffset = RIGHT_ARC_OFFSET.get();
        rightRadiusMult = (float)(double)RIGHT_RADIUS_MULT.get();

        // Sword to Left
        leftYawOffset = LEFT_YAW_OFFSET.get();
        leftPitch = (float) (double) LEFT_PITCH.get();
        leftRoll = (float) (double) LEFT_ROLL.get();
        leftArcOffset = LEFT_ARC_OFFSET.get();
        leftRadiusMult = (float)(double)LEFT_RADIUS_MULT.get();

        // Sword Overhead
        overheadYawOffset = OVERHEAD_YAW_OFFSET.get();
        overheadPitch = (float) (double) OVERHEAD_PITCH.get();
        overheadRoll = (float) (double) OVERHEAD_ROLL.get();
        overheadArcOffset = OVERHEAD_ARC_OFFSET.get();
        overheadRadiusMult = (float)(double)OVERHEAD_RADIUS_MULT.get();

        // Sword to Upper
        upperYawOffset = UPPER_YAW_OFFSET.get();
        upperPitch = (float) (double) UPPER_PITCH.get();
        upperRoll = (float) (double) UPPER_ROLL.get();
        upperArcOffset = UPPER_ARC_OFFSET.get();
        upperRadiusMult = (float)(double)UPPER_RADIUS_MULT.get();

        // Sword Rotate
        rotateYawOffset = ROTATE_YAW_OFFSET.get();
        rotatePitch = (float) (double) ROTATE_PITCH.get();
        rotateRoll = (float) (double) ROTATE_ROLL.get();
        rotateArcOffset = ROTATE_ARC_OFFSET.get();
        rotateRadiusMult = (float)(double)ROTATE_RADIUS_MULT.get();
    }
}
