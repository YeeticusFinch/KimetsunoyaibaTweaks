package com.lerdorf.kimetsunoyaibamultiplayer.config;

import com.lerdorf.kimetsunoyaibamultiplayer.gravity.engine.RotationParameters;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;

public final class GravityConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue ENABLE_GRAVITY_CHANGING = BUILDER
        .comment("Master switch for KNY gravity changing. When false, public gravity helpers return vanilla-safe values and field sources are inert.")
        .define("enable-gravity-changing", false);

    public static final ForgeConfigSpec.IntValue ROTATION_TIME_MS = BUILDER
        .comment("Default visual rotation time for gravity changes, in milliseconds.")
        .defineInRange("rotation-time-ms", 500, 0, 5000);

    public static final ForgeConfigSpec.DoubleValue GRAVITY_STRENGTH_MULTIPLIER = BUILDER
        .comment("Multiplier for custom gravity acceleration.")
        .defineInRange("gravity-strength-multiplier", 1.0D, 0.0D, 10.0D);

    public static final ForgeConfigSpec.BooleanValue RESET_GRAVITY_ON_RESPAWN = BUILDER
        .comment("Reset player base gravity to DOWN after death respawn.")
        .define("reset-gravity-on-respawn", true);

    public static final ForgeConfigSpec.BooleanValue ADJUST_POSITION_AFTER_CHANGING_GRAVITY = BUILDER
        .comment("Reserved for the full movement/collision mixin layer.")
        .define("adjust-position-after-changing-gravity", true);

    public static final ForgeConfigSpec.IntValue MAX_FIELD_RANGE = BUILDER
        .comment("Maximum gravity projector field range.")
        .defineInRange("max-field-range", 32, 1, 256);

    public static final ForgeConfigSpec.BooleanValue FIELD_DEBUG_RENDER = BUILDER
        .comment("Enable client-side gravity field debug rendering when implemented.")
        .define("field-debug-render", false);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean enableGravityChanging;
    public static int rotationTimeMs;
    public static double gravityStrengthMultiplier;
    public static boolean resetGravityOnRespawn;
    public static boolean adjustPositionAfterChangingGravity;
    public static int maxFieldRange;
    public static boolean fieldDebugRender;
    public static boolean fieldDebugCommandEnabled;

    private GravityConfig() {
    }

    @SubscribeEvent
    public static void onLoad(ModConfigEvent event) {
        enableGravityChanging = ENABLE_GRAVITY_CHANGING.get();
        rotationTimeMs = ROTATION_TIME_MS.get();
        gravityStrengthMultiplier = GRAVITY_STRENGTH_MULTIPLIER.get();
        resetGravityOnRespawn = RESET_GRAVITY_ON_RESPAWN.get();
        adjustPositionAfterChangingGravity = ADJUST_POSITION_AFTER_CHANGING_GRAVITY.get();
        maxFieldRange = MAX_FIELD_RANGE.get();
        fieldDebugRender = FIELD_DEBUG_RENDER.get();
        RotationParameters.updateDefault(rotationTimeMs);
    }
}
