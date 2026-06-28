package com.lerdorf.kimetsunoyaibamultiplayer.config;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;

public final class SwordRackConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final SlotTransform[] FLOOR_TRANSFORMS = new SlotTransform[3];
    private static final SlotTransform[] WALL_TRANSFORMS = new SlotTransform[3];

    static {
        BUILDER.comment("Sword Rack Configuration")
            .push("sword_rack");

        BUILDER.comment("Sword transforms when the rack is placed on the floor")
            .push("floor");
        FLOOR_TRANSFORMS[0] = new SlotTransform(BUILDER, "slot1", new RackTransform(
            1.05D, 1.1D, 0.35D,
            0.0D, -60.0D, 90.0D
        ));
        FLOOR_TRANSFORMS[1] = new SlotTransform(BUILDER, "slot2", new RackTransform(
            1.05D, 0.8D, 0.35D,
            0.0D, -60.0D, 90.0D
        ));
        FLOOR_TRANSFORMS[2] = new SlotTransform(BUILDER, "slot3", new RackTransform(
            1.05D, 0.5D, 0.35D,
            0.0D, -60.0D, 90.0D
        ));
        BUILDER.pop();

        BUILDER.comment("Sword transforms when the rack is mounted on a wall")
            .push("wall");
        WALL_TRANSFORMS[0] = new SlotTransform(BUILDER, "slot1", new RackTransform(
            1.05D, 1.0D, 0.8D,
            0.0D, -60.0D, 90.0D
        ));
        WALL_TRANSFORMS[1] = new SlotTransform(BUILDER, "slot2", new RackTransform(
             1.05D, 0.7D, 0.8D,
            0.0D, -60.0D, 90.0D
        ));
        WALL_TRANSFORMS[2] = new SlotTransform(BUILDER, "slot3", new RackTransform(
             1.05D, 0.35D, 0.8D,
            0.0D, -60.0D, 90.0D
        ));
        BUILDER.pop();

        BUILDER.pop();
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private SwordRackConfig() {
    }

    public static RackTransform getTransform(boolean wallMounted, int slot) {
        int clampedSlot = Math.max(0, Math.min(slot, 2));
        return (wallMounted ? WALL_TRANSFORMS[clampedSlot] : FLOOR_TRANSFORMS[clampedSlot]).toRackTransform();
    }

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event) {
        Log.info("Loaded sword rack config");
    }

    public record RackTransform(double translateX, double translateY, double translateZ,
                                double rotateX, double rotateY, double rotateZ) {
    }

    private static final class SlotTransform {
        private final ForgeConfigSpec.DoubleValue translateX;
        private final ForgeConfigSpec.DoubleValue translateY;
        private final ForgeConfigSpec.DoubleValue translateZ;
        private final ForgeConfigSpec.DoubleValue rotateX;
        private final ForgeConfigSpec.DoubleValue rotateY;
        private final ForgeConfigSpec.DoubleValue rotateZ;

        private SlotTransform(ForgeConfigSpec.Builder builder, String slotName, RackTransform defaults) {
            builder.comment("Transforms for " + slotName)
                .push(slotName);

            translateX = builder.comment("Local X translation in block space")
                .defineInRange("translate_x", defaults.translateX(), -8.0D, 8.0D);
            translateY = builder.comment("Local Y translation in block space")
                .defineInRange("translate_y", defaults.translateY(), -8.0D, 8.0D);
            translateZ = builder.comment("Local Z translation in block space")
                .defineInRange("translate_z", defaults.translateZ(), -8.0D, 8.0D);
            rotateX = builder.comment("Rotation around X in degrees")
                .defineInRange("rotate_x", defaults.rotateX(), -360.0D, 360.0D);
            rotateY = builder.comment("Rotation around Y in degrees")
                .defineInRange("rotate_y", defaults.rotateY(), -360.0D, 360.0D);
            rotateZ = builder.comment("Rotation around Z in degrees")
                .defineInRange("rotate_z", defaults.rotateZ(), -360.0D, 360.0D);

            builder.pop();
        }

        private RackTransform toRackTransform() {
            return new RackTransform(
                translateX.get(),
                translateY.get(),
                translateZ.get(),
                rotateX.get(),
                rotateY.get(),
                rotateZ.get()
            );
        }
    }
}
