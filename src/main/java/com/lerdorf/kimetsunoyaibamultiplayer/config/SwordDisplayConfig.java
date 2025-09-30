package com.lerdorf.kimetsunoyaibamultiplayer.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;

public class SwordDisplayConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    static {
        BUILDER.comment("Sword Display Configuration")
                .push("sword_display");
    }

    // Enable/disable sword display feature
    private static final ForgeConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Enable displaying swords on player model when not actively held")
            .define("enabled", true);

    // Position preference (HIP or BACK)
    private static final ForgeConfigSpec.EnumValue<SwordDisplayPosition> POSITION = BUILDER
            .comment("Where to display swords on the player model (HIP or BACK)")
            .defineEnum("position", SwordDisplayPosition.HIP);

    // Scale of displayed swords
    private static final ForgeConfigSpec.DoubleValue SCALE = BUILDER
            .comment("Scale of displayed swords (0.5 = half size, 1.0 = normal size, 2.0 = double size)")
            .defineInRange("scale", 1.0, 0.1, 5.0);

    // Hip position settings
    static {
        BUILDER.comment("Hip Display Position Configuration")
                .push("hip_position");
    }

    // Left hip
    private static final ForgeConfigSpec.DoubleValue HIP_LEFT_TRANSLATE_X = BUILDER
            .comment("Left hip X translation")
            .defineInRange("left_translate_x", 0.3, -5.0, 5.0);
    private static final ForgeConfigSpec.DoubleValue HIP_LEFT_TRANSLATE_Y = BUILDER
            .comment("Left hip Y translation")
            .defineInRange("left_translate_y", 0.55, -5.0, 5.0);
    private static final ForgeConfigSpec.DoubleValue HIP_LEFT_TRANSLATE_Z = BUILDER
            .comment("Left hip Z translation")
            .defineInRange("left_translate_z", -0.1, -5.0, 5.0);
    private static final ForgeConfigSpec.DoubleValue HIP_LEFT_ROTATE_Z = BUILDER
            .comment("Left hip Z rotation (degrees)")
            .defineInRange("left_rotate_z", 0, -360.0, 360.0);
    private static final ForgeConfigSpec.DoubleValue HIP_LEFT_ROTATE_Y = BUILDER
            .comment("Left hip Y rotation (degrees)")
            .defineInRange("left_rotate_y", 180, -360.0, 360.0);
    private static final ForgeConfigSpec.DoubleValue HIP_LEFT_ROTATE_X = BUILDER
            .comment("Left hip X rotation (degrees)")
            .defineInRange("left_rotate_x", -46.0, -360.0, 360.0);

    // Right hip
    private static final ForgeConfigSpec.DoubleValue HIP_RIGHT_TRANSLATE_X = BUILDER
            .comment("Right hip X translation")
            .defineInRange("right_translate_x", -0.3, -5.0, 5.0);
    private static final ForgeConfigSpec.DoubleValue HIP_RIGHT_TRANSLATE_Y = BUILDER
            .comment("Right hip Y translation")
            .defineInRange("right_translate_y", 0.55, -5.0, 5.0);
    private static final ForgeConfigSpec.DoubleValue HIP_RIGHT_TRANSLATE_Z = BUILDER
            .comment("Right hip Z translation")
            .defineInRange("right_translate_z", -0.1, -5.0, 5.0);
    private static final ForgeConfigSpec.DoubleValue HIP_RIGHT_ROTATE_Z = BUILDER
            .comment("Right hip Z rotation (degrees)")
            .defineInRange("right_rotate_z", 0, -360.0, 360.0);
    private static final ForgeConfigSpec.DoubleValue HIP_RIGHT_ROTATE_Y = BUILDER
            .comment("Right hip Y rotation (degrees)")
            .defineInRange("right_rotate_y", 180, -360.0, 360.0);
    private static final ForgeConfigSpec.DoubleValue HIP_RIGHT_ROTATE_X = BUILDER
            .comment("Right hip X rotation (degrees)")
            .defineInRange("right_rotate_x", -65.0, -360.0, 360.0);

    static {
        BUILDER.pop(); // hip_position
    }

    // Back position settings
    static {
        BUILDER.comment("Back Display Position Configuration")
                .push("back_position");
    }

    // Left back
    private static final ForgeConfigSpec.DoubleValue BACK_LEFT_TRANSLATE_X = BUILDER
            .comment("Left back X translation")
            .defineInRange("left_translate_x", 0.2, -2.0, 2.0);
    private static final ForgeConfigSpec.DoubleValue BACK_LEFT_TRANSLATE_Y = BUILDER
            .comment("Left back Y translation")
            .defineInRange("left_translate_y", -0.1, -2.0, 2.0);
    private static final ForgeConfigSpec.DoubleValue BACK_LEFT_TRANSLATE_Z = BUILDER
            .comment("Left back Z translation")
            .defineInRange("left_translate_z", 0.2, -2.0, 2.0);
    private static final ForgeConfigSpec.DoubleValue BACK_LEFT_ROTATE_Z = BUILDER
            .comment("Left back Z rotation (degrees)")
            .defineInRange("left_rotate_z", 35, -360.0, 360.0);
    private static final ForgeConfigSpec.DoubleValue BACK_LEFT_ROTATE_Y = BUILDER
            .comment("Left back Y rotation (degrees)")
            .defineInRange("left_rotate_y", 90.0, -360.0, 360.0);
    private static final ForgeConfigSpec.DoubleValue BACK_LEFT_ROTATE_X = BUILDER
            .comment("Left back X rotation (degrees)")
            .defineInRange("left_rotate_x", 0.0, -360.0, 360.0);

    // Right back
    private static final ForgeConfigSpec.DoubleValue BACK_RIGHT_TRANSLATE_X = BUILDER
            .comment("Right back X translation")
            .defineInRange("right_translate_x", -0.4, -2.0, 2.0);
    private static final ForgeConfigSpec.DoubleValue BACK_RIGHT_TRANSLATE_Y = BUILDER
            .comment("Right back Y translation")
            .defineInRange("right_translate_y", 0.0, -2.0, 2.0);
    private static final ForgeConfigSpec.DoubleValue BACK_RIGHT_TRANSLATE_Z = BUILDER
            .comment("Right back Z translation")
            .defineInRange("right_translate_z", 0.2, -2.0, 2.0);
    private static final ForgeConfigSpec.DoubleValue BACK_RIGHT_ROTATE_Z = BUILDER
            .comment("Right back Z rotation (degrees)")
            .defineInRange("right_rotate_z", -35.0, -360.0, 360.0);
    private static final ForgeConfigSpec.DoubleValue BACK_RIGHT_ROTATE_Y = BUILDER
            .comment("Right back Y rotation (degrees)")
            .defineInRange("right_rotate_y", 90.0, -360.0, 360.0);
    private static final ForgeConfigSpec.DoubleValue BACK_RIGHT_ROTATE_X = BUILDER
            .comment("Right back X rotation (degrees)")
            .defineInRange("right_rotate_x", 0.0, -360.0, 360.0);

    static {
        BUILDER.pop(); // back_position
        BUILDER.pop(); // sword_display
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean enabled;
    public static SwordDisplayPosition position;
    public static double scale;

    // Hip position values
    public static double hipLeftTranslateX;
    public static double hipLeftTranslateY;
    public static double hipLeftTranslateZ;
    public static double hipLeftRotateZ;
    public static double hipLeftRotateY;
    public static double hipLeftRotateX;
    public static double hipRightTranslateX;
    public static double hipRightTranslateY;
    public static double hipRightTranslateZ;
    public static double hipRightRotateZ;
    public static double hipRightRotateY;
    public static double hipRightRotateX;

    // Back position values
    public static double backLeftTranslateX;
    public static double backLeftTranslateY;
    public static double backLeftTranslateZ;
    public static double backLeftRotateZ;
    public static double backLeftRotateY;
    public static double backLeftRotateX;
    public static double backRightTranslateX;
    public static double backRightTranslateY;
    public static double backRightTranslateZ;
    public static double backRightRotateZ;
    public static double backRightRotateY;
    public static double backRightRotateX;

    public enum SwordDisplayPosition {
        HIP,
        BACK
    }

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event) {
        System.out.println("SWORD DISPLAY CONFIG LOADING...");
        enabled = ENABLED.get();
        position = POSITION.get();
        scale = SCALE.get();

        // Load hip position values
        hipLeftTranslateX = HIP_LEFT_TRANSLATE_X.get();
        hipLeftTranslateY = HIP_LEFT_TRANSLATE_Y.get();
        hipLeftTranslateZ = HIP_LEFT_TRANSLATE_Z.get();
        hipLeftRotateZ = HIP_LEFT_ROTATE_Z.get();
        hipLeftRotateY = HIP_LEFT_ROTATE_Y.get();
        hipLeftRotateX = HIP_LEFT_ROTATE_X.get();
        hipRightTranslateX = HIP_RIGHT_TRANSLATE_X.get();
        hipRightTranslateY = HIP_RIGHT_TRANSLATE_Y.get();
        hipRightTranslateZ = HIP_RIGHT_TRANSLATE_Z.get();
        hipRightRotateZ = HIP_RIGHT_ROTATE_Z.get();
        hipRightRotateY = HIP_RIGHT_ROTATE_Y.get();
        hipRightRotateX = HIP_RIGHT_ROTATE_X.get();

        // Load back position values
        backLeftTranslateX = BACK_LEFT_TRANSLATE_X.get();
        backLeftTranslateY = BACK_LEFT_TRANSLATE_Y.get();
        backLeftTranslateZ = BACK_LEFT_TRANSLATE_Z.get();
        backLeftRotateZ = BACK_LEFT_ROTATE_Z.get();
        backLeftRotateY = BACK_LEFT_ROTATE_Y.get();
        backLeftRotateX = BACK_LEFT_ROTATE_X.get();
        backRightTranslateX = BACK_RIGHT_TRANSLATE_X.get();
        backRightTranslateY = BACK_RIGHT_TRANSLATE_Y.get();
        backRightTranslateZ = BACK_RIGHT_TRANSLATE_Z.get();
        backRightRotateZ = BACK_RIGHT_ROTATE_Z.get();
        backRightRotateY = BACK_RIGHT_ROTATE_Y.get();
        backRightRotateX = BACK_RIGHT_ROTATE_X.get();

        System.out.println("Sword display config loaded: enabled=" + enabled +
                         ", position=" + position + ", scale=" + scale);
    }
}
