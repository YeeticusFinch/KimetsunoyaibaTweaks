package com.lerdorf.kimetsunoyaibamultiplayer.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Server-side controls for the base mod's futon interaction.
 */
public final class FutonConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue DISABLE_TIME_SKIP = BUILDER
        .comment("Disable the base mod futon's shift-right-click time skip",
            "When enabled, shift-right-clicking a kimetsunoyaiba:futon_2 will not advance server time.",
            "Regular right-clicking still toggles between futon and futon_2.",
            "Default: false")
        .define("disable_futon_time_skip", false);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private FutonConfig() {
    }

    public static boolean isTimeSkipDisabled() {
        return DISABLE_TIME_SKIP.get();
    }
}
