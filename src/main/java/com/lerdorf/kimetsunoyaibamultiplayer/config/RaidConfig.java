package com.lerdorf.kimetsunoyaibamultiplayer.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Configuration for the raid system.
 *
 * Controls all aspects of demon raids (omen_of_muzan) and demon slayer raids (omen_of_ubuyashiki).
 *
 * Master switch: kimetsu_raids gamerule (registered separately)
 *
 * Features:
 * - Wave-based raid system with 5 difficulty levels
 * - Structure-size-aware spawning
 * - Hardcore mode integration
 * - Boss bar UI
 * - Omen potion rewards
 */
public class RaidConfig {
    public static final ForgeConfigSpec SPEC;

    // Master switches
    public static ForgeConfigSpec.BooleanValue enableRaids;
    public static ForgeConfigSpec.BooleanValue enableDebugLogging;

    // Structure spawn radii (in blocks)
    public static ForgeConfigSpec.IntValue smallStructureSpawnRadius;
    public static ForgeConfigSpec.IntValue mediumStructureSpawnRadius;
    public static ForgeConfigSpec.IntValue largeStructureSpawnRadius;

    // Timing settings (in seconds)
    public static ForgeConfigSpec.IntValue wavePreparationTime;
    public static ForgeConfigSpec.IntValue entitySpawnInterval;
    public static ForgeConfigSpec.IntValue raidTimeout;

    // Participation and tracking
    public static ForgeConfigSpec.IntValue raidParticipationRadius;

    // Reward settings
    public static ForgeConfigSpec.BooleanValue enableOmenPotionRewards;
    public static ForgeConfigSpec.DoubleValue omenPotionSameLevelChance;

    // Entity restrictions
    public static ForgeConfigSpec.BooleanValue disableYoriichiInRaids;
    public static ForgeConfigSpec.BooleanValue disableYoriichiOldInRaids;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Raid System Configuration")
            .comment("Controls demon raids (omen_of_muzan) and demon slayer raids (omen_of_ubuyashiki)")
            .comment("Note: kimetsu_raids gamerule is the master switch for enabling/disabling raids")
            .push("raids");

        // Master switches
        builder.comment("Master Switches").push("switches");

        enableRaids = builder
            .comment("Enable raid system (fallback if gamerule not available)")
            .define("enable_raids", true);

        enableDebugLogging = builder
            .comment("Enable debug logging for raid events")
            .define("enable_debug_logging", false);

        builder.pop();

        // Structure spawn radii
        builder.comment("Structure Spawn Radii")
            .comment("Defines how far from structure center entities spawn")
            .push("spawn_radii");

        smallStructureSpawnRadius = builder
            .comment("Spawn radius for small structures (houses)")
            .defineInRange("small_structure_spawn_radius", 32, 16, 128);

        mediumStructureSpawnRadius = builder
            .comment("Spawn radius for medium structures (temples, trains)")
            .defineInRange("medium_structure_spawn_radius", 48, 24, 128);

        largeStructureSpawnRadius = builder
            .comment("Spawn radius for large structures (villages)")
            .defineInRange("large_structure_spawn_radius", 96, 48, 256);

        builder.pop();

        // Timing settings
        builder.comment("Timing Settings")
            .comment("All times in seconds")
            .push("timing");

        wavePreparationTime = builder
            .comment("Time between waves (preparation phase)")
            .defineInRange("wave_preparation_time", 10, 5, 60);

        entitySpawnInterval = builder
            .comment("Time between individual entity spawns (prevents lag)")
            .defineInRange("entity_spawn_interval", 2, 1, 10);

        raidTimeout = builder
            .comment("Maximum raid duration before automatic defeat (30 minutes)")
            .defineInRange("raid_timeout", 1800, 600, 7200);

        builder.pop();

        // Participation and tracking
        builder.comment("Participation and Tracking").push("participation");

        raidParticipationRadius = builder
            .comment("Radius for raid participation and boss bar display")
            .defineInRange("raid_participation_radius", 500, 100, 1000);

        builder.pop();

        // Reward settings
        builder.comment("Reward Settings").push("rewards");

        enableOmenPotionRewards = builder
            .comment("Enable omen potion rewards for completing raids")
            .define("enable_omen_potion_rewards", true);

        omenPotionSameLevelChance = builder
            .comment("Chance to receive same level omen potion (vs +1 level)")
            .defineInRange("omen_potion_same_level_chance", 0.5, 0.0, 1.0);

        builder.pop();

        // Entity restrictions
        builder.comment("Entity Restrictions")
            .comment("Disable specific powerful entities from spawning in raids")
            .push("entity_restrictions");

        disableYoriichiInRaids = builder
            .comment("Disable Yoriichi from spawning in demon slayer raids")
            .define("disable_yoriichi", false);

        disableYoriichiOldInRaids = builder
            .comment("Disable Old Yoriichi from spawning in demon slayer raids")
            .define("disable_yoriichi_old", false);

        builder.pop();

        builder.pop();

        SPEC = builder.build();
    }
}
