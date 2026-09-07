package com.lerdorf.kimetsunoyaibamultiplayer.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/** Client-side offsets for the GeckoLib 3D skin-layer meshes. */
public final class EntitySkinLayersConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    static {
        BUILDER.comment(
                "Entity 3D Skin Layer Configuration",
                "Translations use model units; 0.0625 is approximately one model pixel.",
                "Rotations are in degrees and are applied after the GeckoLib bone transform."
        ).push("entity_skin_layers");
    }

    public static final PartConfig HEAD = definePart("head", "Head / hat layer offsets");
    public static final PartConfig TORSO = definePart("torso", "Torso / jacket layer offsets");
    public static final PartConfig RIGHT_ARM = definePart("right_arm", "Right sleeve layer offsets");
    public static final PartConfig LEFT_ARM = definePart("left_arm", "Left sleeve layer offsets");
    public static final PartConfig RIGHT_LEG = definePart("right_leg", "Right pants layer offsets");
    public static final PartConfig LEFT_LEG = definePart("left_leg", "Left pants layer offsets");

    static {
        BUILDER.pop();
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private static PartConfig definePart(String name, String comment) {
        BUILDER.comment(comment).push(name);
        ForgeConfigSpec.DoubleValue translateX = BUILDER
                .comment("Local X translation in model units")
                .defineInRange("translate_x", 0.0, -5.0, 5.0);
        ForgeConfigSpec.DoubleValue translateY = BUILDER
                .comment("Local Y translation in model units")
                .defineInRange("translate_y", 0.0, -5.0, 5.0);
        ForgeConfigSpec.DoubleValue translateZ = BUILDER
                .comment("Local Z translation in model units")
                .defineInRange("translate_z", 0.0, -5.0, 5.0);
        ForgeConfigSpec.DoubleValue rotateX = BUILDER
                .comment("Local X rotation in degrees")
                .defineInRange("rotate_x", 0.0, -360.0, 360.0);
        ForgeConfigSpec.DoubleValue rotateY = BUILDER
                .comment("Local Y rotation in degrees")
                .defineInRange("rotate_y", 0.0, -360.0, 360.0);
        ForgeConfigSpec.DoubleValue rotateZ = BUILDER
                .comment("Local Z rotation in degrees")
                .defineInRange("rotate_z", 0.0, -360.0, 360.0);
        BUILDER.pop();
        return new PartConfig(translateX, translateY, translateZ, rotateX, rotateY, rotateZ);
    }

    public record PartConfig(
            ForgeConfigSpec.DoubleValue translateXValue,
            ForgeConfigSpec.DoubleValue translateYValue,
            ForgeConfigSpec.DoubleValue translateZValue,
            ForgeConfigSpec.DoubleValue rotateXValue,
            ForgeConfigSpec.DoubleValue rotateYValue,
            ForgeConfigSpec.DoubleValue rotateZValue
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
    }

    @SubscribeEvent
    public static void onLoad(ModConfigEvent event) {
        // ForgeConfigSpec values are read lazily, so no cached copy is needed.
    }
}
