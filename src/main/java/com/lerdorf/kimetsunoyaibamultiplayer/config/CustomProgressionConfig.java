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

    // Custom demon initiation flow
    public static ForgeConfigSpec.BooleanValue customDemonInitiation;

    // Keep demon state through death by replaying blood consumption on respawn
    public static ForgeConfigSpec.BooleanValue persistentDemonhood;

    // Replace the base mod's muzan blood ore with hemolith ore
    public static ForgeConfigSpec.BooleanValue replaceMuzanBloodOre;

    // Replace the base mod's scarlet ore drops with nichirin ore
    public static ForgeConfigSpec.BooleanValue replaceScarletOre;

    // Human flesh conversion for demon progression power
    public static ForgeConfigSpec.IntValue humanFleshPerEffectiveMuzanBlood;

    // Replace the base mod's color changing procedure (sword transformation)
    public static ForgeConfigSpec.BooleanValue replaceColorChangingProcedure;

    // Debug logging
    public static ForgeConfigSpec.BooleanValue enableDebugLogging;
    
    // Toril gate worldgen guarantees
    public static ForgeConfigSpec.BooleanValue guaranteeTorilGateNearOrigin;

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

        // Demon progression override
        builder.comment("Demon Progression Override").push("demon_progression");

        customDemonInitiation = builder
            .comment("Enable the custom demon initiation flow",
                    "",
                    "When enabled, consuming kimetsunoyaibamultiplayer:blood_of_muzan starts a",
                    "custom demon transformation phase instead of immediately turning the player",
                    "into a demon.",
                    "",
                    "During this phase:",
                    "  - The player receives the Demon Transformation effect",
                    "  - The player is tagged with oni_transform",
                    "  - Demon slayers and demons stay neutral unless provoked",
                    "  - Slowness, weakness, and hunger are continuously applied",
                    "",
                    "When the effect expires naturally, the player is converted using the base",
                    "mod's blood_of_muzan logic. If the effect is removed early or the player dies,",
                    "the transformation is cancelled.",
                    "",
                    "This also replaces base mod blood_of_muzan items in player inventories with",
                    "kimetsunoyaibamultiplayer:blood_of_muzan.")
            .define("custom_demon_initiation", true);

        persistentDemonhood = builder
            .comment("Persistent Demonhood",
                    "",
                    "When enabled, demon players keep their demon progression through death.",
                    "On respawn the mod replays the remembered Muzan blood consumption so the",
                    "base mod restores demon state, then reapplies the tracked custom demon data.",
                    "",
                    "Default: true")
            .define("persistent_demonhood", true);

        replaceMuzanBloodOre = builder
            .comment("Replace base mod muzan blood ore with hemolith ore",
                    "",
                    "When enabled, any player who starts mining kimetsunoyaiba:muzanblood_ore",
                    "will cause that block to be replaced with",
                    "kimetsunoyaibamultiplayer:hemolith_ore.",
                    "",
                    "If a player somehow finishes breaking the original base ore before the swap",
                    "fully resolves, the break is intercepted and hemolith drops are forced so",
                    "the original muzan blood ore drop never appears.")
            .define("replace_muzan_blood_ore", true);

        replaceScarletOre = builder
            .comment("Replace base mod scarlet ore drops with nichirin ore",
                    "",
                    "When enabled, any dropped kimetsunoyaiba:scarlet_ore item is rewritten into",
                    "a nichirin ore stack for a random breathing style.",
                    "",
                    "This uses the same item-replacement flow as hemolith dust so the base item",
                    "never reaches the player inventory unchanged.")
            .define("replace_scarlet_ore", true);

        humanFleshPerEffectiveMuzanBlood = builder
            .comment("How many consumed human flesh items count as one effective Muzan blood for demon progression.",
                    "",
                    "This does not change the raw Muzan Blood Consumed display. It only affects",
                    "progression systems that scale from the demon player's blood count, such as",
                    "passive skill points and custom Blood Demon Art slot unlocks.")
            .defineInRange("human_flesh_per_effective_muzan_blood", 3, 1, 64);

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

        // Toril gate generation guarantees
        builder.comment("Toril Gate Guarantees").push("toril_gate");

        guaranteeTorilGateNearOrigin = builder
            .comment("Guarantee at least one toril gate exists between 800 and 1600 blocks of (0, 0) in the overworld.",
                    "",
                    "On server start, if no existing toril gate is found in that distance band,",
                    "the mod will choose a random non-ocean location in that band, paint a wisteria biome patch,",
                    "and place a toril gate structure there.")
            .define("guarantee_toril_gate_within_1000_of_origin", true);

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

    public static boolean isCustomProgressionEnabled() {
        return disableBaseModDemonSlayerInitiation != null && disableBaseModDemonSlayerInitiation.get();
    }

    public static int getHumanFleshPerEffectiveMuzanBlood() {
        return humanFleshPerEffectiveMuzanBlood == null ? 3 : Math.max(1, humanFleshPerEffectiveMuzanBlood.get());
    }

    public static boolean isPersistentDemonhoodEnabled() {
        return persistentDemonhood != null && persistentDemonhood.get();
    }
}
