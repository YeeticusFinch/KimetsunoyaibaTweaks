package com.lerdorf.kimetsunoyaibamultiplayer.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Configuration for custom progression overrides.
 *
 * Allows disabling certain base mod progression features that can be buggy
 * or that server admins want to customize.
 */
public class CustomProgressionConfig {
    public static final ForgeConfigSpec SPEC;

    // Master switch for demon slayer initiation
    public static ForgeConfigSpec.BooleanValue disableBaseModDemonSlayerInitiation;

    // Debug logging
    public static ForgeConfigSpec.BooleanValue enableDebugLogging;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Custom Progression Configuration")
            .comment("Override base mod progression features")
            .push("custom_progression");

        // Main switch
        builder.comment("Demon Slayer Initiation Override").push("demon_slayer_initiation");

        disableBaseModDemonSlayerInitiation = builder
            .comment("Disable the base mod's demon slayer initiation rewards",
                    "",
                    "When enabled, this will prevent the following from happening when a player",
                    "earns the 'demon_slayer_corps' advancement:",
                    "  - Automatic granting of uniform_chestplate, uniform_leggings, uniform_boots",
                    "  - Automatic granting of nichirinsword",
                    "  - Automatic spawning and taming of kasugai_crow",
                    "  - Automatic granting of mizunoto advancement",
                    "",
                    "This is useful if:",
                    "  - The base mod is bugging out and giving items multiple times",
                    "  - You want to create custom progression via datapacks/commands",
                    "  - You want to use your own rewards system",
                    "",
                    "NOTE: This blocks the base mod's SupplyProcedure, AdvancementRewardProcedure,",
                    "and CheckAdvancementDemonProcedure from granting these rewards.")
            .define("disable_base_mod_demon_slayer_initiation", false);

        builder.pop();

        // Debug section
        builder.comment("Debug Settings").push("debug");

        enableDebugLogging = builder
            .comment("Enable debug logging for progression overrides",
                    "Logs when advancements are blocked, items are removed, or crows are prevented from spawning")
            .define("enable_debug_logging", false);

        builder.pop();

        builder.pop();

        SPEC = builder.build();
    }
}
