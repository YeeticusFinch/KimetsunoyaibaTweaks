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

    // Grant training sword when initiation is blocked
    public static ForgeConfigSpec.BooleanValue grantTrainingSword;

    // Replace the base mod's color changing procedure (sword transformation)
    public static ForgeConfigSpec.BooleanValue replaceColorChangingProcedure;

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
                    "  - Retention of base rank advancements (mizunoto through strongest)",
                    "",
                    "This is useful if:",
                    "  - The base mod is bugging out and giving items multiple times",
                    "  - You want to create custom progression via datapacks/commands",
                    "  - You want to use your own rewards system",
                    "",
                    "NOTE: This blocks the base mod's SupplyProcedure, AdvancementRewardProcedure,",
                    "CheckAdvancementDemonProcedure, Advanvement1Procedure, and ColorChangeProcedure",
                    "from granting these rewards and progression advancements.",
                    "It also enables a managed datapack that hides related base advancement chat/toast.")
            .define("disable_base_mod_demon_slayer_initiation", true);

        grantTrainingSword = builder
            .comment("Grant a training sword when initiation is blocked",
                    "",
                    "When enabled (and disable_base_mod_demon_slayer_initiation is also enabled),",
                    "the player will receive a nichirinsword that has been converted to a training sword.",
                    "Training swords can only use the 1st Form and have reduced damage.",
                    "",
                    "This is useful for servers that want new demon slayers to start with a training sword",
                    "and earn a real sword through gameplay.")
            .define("grant_training_sword", true);

        builder.pop();

        // Sword transformation override
        builder.comment("Sword Transformation Override").push("sword_transformation");

        replaceColorChangingProcedure = builder
            .comment("Replace the base mod's color changing procedure",
                    "",
                    "When enabled, this completely overrides the base mod's ColorChangeProcedure",
                    "which transforms the basic nichirinsword into a colored breathing sword",
                    "when the player holds it for a certain amount of time.",
                    "",
                    "Enable this if you want to implement custom sword transformation logic,",
                    "or to prevent the automatic sword transformation entirely.")
            .define("replace_color_changing_procedure", true);

        builder.pop();

        // Debug section
        builder.comment("Debug Settings").push("debug");

        enableDebugLogging = builder
            .comment("Enable debug logging for progression overrides",
                    "Logs when advancements are blocked, items are removed, or crows are prevented from spawning")
            .define("enable_debug_logging", true);

        builder.pop();

        builder.pop();

        SPEC = builder.build();
    }
}
