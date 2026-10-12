package com.lerdorf.kimetsunoyaibamultiplayer.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/** Client-side display controls for Rui's model features. */
public final class RuiDisplayConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue FANCY_RUI_HAIR;
    public static final TransformConfig KANJI;

    static {
        BUILDER.comment(
                "Rui Display Configuration",
                "Translations use model units; 0.0625 is approximately one model pixel.",
                "Rotations use degrees."
        ).push("rui_display");

        FANCY_RUI_HAIR = BUILDER.comment(
                "Render Rui's detailed hair geometry. Disable this to reduce graphics load on slower computers."
        ).define("fancy_rui_hair", true);

        KANJI = defineTransform("kanji", "Rui kanji plane transform",
                2.05D / 16.0D, 0.18D, -4.08D / 16.0D,
                0.0D, 0.0D, 180.0D,
                1.0D, 1.0D, 1.0D);

        BUILDER.pop();
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private RuiDisplayConfig() {
    }

    private static TransformConfig defineTransform(String name, String comment,
                                                   double translateXDefault, double translateYDefault,
                                                   double translateZDefault, double rotateXDefault,
                                                   double rotateYDefault, double rotateZDefault,
                                                   double scaleXDefault, double scaleYDefault,
                                                   double scaleZDefault) {
        BUILDER.comment(comment).push(name);
        TransformConfig config = new TransformConfig(
                defineDouble("translate_x", "Local X translation in model units", translateXDefault, -8.0D, 8.0D),
                defineDouble("translate_y", "Local Y translation in model units", translateYDefault, -8.0D, 8.0D),
                defineDouble("translate_z", "Local Z translation in model units", translateZDefault, -8.0D, 8.0D),
                defineDouble("rotate_x", "Local X rotation in degrees", rotateXDefault, -360.0D, 360.0D),
                defineDouble("rotate_y", "Local Y rotation in degrees", rotateYDefault, -360.0D, 360.0D),
                defineDouble("rotate_z", "Local Z rotation in degrees", rotateZDefault, -360.0D, 360.0D),
                defineDouble("scale_x", "Local X scale", scaleXDefault, 0.01D, 8.0D),
                defineDouble("scale_y", "Local Y scale", scaleYDefault, 0.01D, 8.0D),
                defineDouble("scale_z", "Local Z scale", scaleZDefault, 0.01D, 8.0D)
        );
        BUILDER.pop();
        return config;
    }

    private static ForgeConfigSpec.DoubleValue defineDouble(String name, String comment,
                                                             double defaultValue, double min, double max) {
        return BUILDER.comment(comment).defineInRange(name, defaultValue, min, max);
    }

    public record TransformConfig(
            ForgeConfigSpec.DoubleValue translateXValue,
            ForgeConfigSpec.DoubleValue translateYValue,
            ForgeConfigSpec.DoubleValue translateZValue,
            ForgeConfigSpec.DoubleValue rotateXValue,
            ForgeConfigSpec.DoubleValue rotateYValue,
            ForgeConfigSpec.DoubleValue rotateZValue,
            ForgeConfigSpec.DoubleValue scaleXValue,
            ForgeConfigSpec.DoubleValue scaleYValue,
            ForgeConfigSpec.DoubleValue scaleZValue
    ) {
        public float translateX() {
            return translateXValue.get().floatValue();
        }

        public float translateY() {
            return translateYValue.get().floatValue();
        }

        public float translateZ() {
            return translateZValue.get().floatValue();
        }

        public float rotateX() {
            return rotateXValue.get().floatValue();
        }

        public float rotateY() {
            return rotateYValue.get().floatValue();
        }

        public float rotateZ() {
            return rotateZValue.get().floatValue();
        }

        public float scaleX() {
            return scaleXValue.get().floatValue();
        }

        public float scaleY() {
            return scaleYValue.get().floatValue();
        }

        public float scaleZ() {
            return scaleZValue.get().floatValue();
        }
    }

    @SubscribeEvent
    public static void onLoad(ModConfigEvent event) {
        // Values are read lazily so renderer changes are visible after config reload.
    }
}
