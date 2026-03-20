package com.lerdorf.kimetsunoyaibamultiplayer.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Configuration for Final Selection raids in Mt. Fujikasane.
 */
public class FinalSelectionRaidConfig {
    public static final ForgeConfigSpec SPEC;

    public static ForgeConfigSpec.BooleanValue enableFinalSelectionRaid;
    public static ForgeConfigSpec.IntValue raidRadius;
    public static ForgeConfigSpec.IntValue nightDurationSeconds;
    public static ForgeConfigSpec.IntValue dayDurationSeconds;
    public static ForgeConfigSpec.IntValue sunriseAccelerationSeconds;
    public static ForgeConfigSpec.IntValue entitySpawnNearPlayerRadius;
    public static ForgeConfigSpec.BooleanValue enableBossArrow;
    public static ForgeConfigSpec.IntValue bossGlowDurationTicks;
    public static ForgeConfigSpec.IntValue bossArrowDurationTicks;
    public static ForgeConfigSpec.IntValue bossEscortRespawnIntervalSeconds;
    public static ForgeConfigSpec.IntValue bossEscortSpawnStepSeconds;
    public static ForgeConfigSpec.IntValue maxDemonsPerPlayer;
    public static ForgeConfigSpec.BooleanValue enableDaytimePassiveAnimals;
    public static ForgeConfigSpec.IntValue daytimePassiveAnimalsPerPlayerMax;
    public static ForgeConfigSpec.IntValue daytimePassiveAnimalSpawnRadius;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Final Selection Raid Configuration")
            .push("final_selection_raids");

        enableFinalSelectionRaid = builder
            .comment("Enable Final Selection raid logic")
            .define("enable_final_selection_raid", true);

        raidRadius = builder
            .comment("Bossbar participation radius around raid center")
            .defineInRange("raid_radius", 480, 64, 1000);

        nightDurationSeconds = builder
            .comment("Duration of one Final Selection night for non-boss waves")
            .defineInRange("night_duration_seconds", 600, 60, 3600);

        dayDurationSeconds = builder
            .comment("Duration of daytime break between nights")
            .defineInRange("day_duration_seconds", 90, 10, 600);

        sunriseAccelerationSeconds = builder
            .comment("Duration for accelerated midnight->sunrise after boss death")
            .defineInRange("sunrise_acceleration_seconds", 20, 5, 300);

        entitySpawnNearPlayerRadius = builder
            .comment("Maximum distance from a candidate for non-boss spawns")
            .defineInRange("entity_spawn_near_player_radius", 200, 32, 500);

        enableBossArrow = builder
            .comment("Enable temporary boss direction arrow")
            .define("enable_boss_arrow", true);

        bossGlowDurationTicks = builder
            .comment("Boss glow duration on spawn (ticks)")
            .defineInRange("boss_glow_duration_ticks", 100, 20, 1200);

        bossArrowDurationTicks = builder
            .comment("Boss arrow duration on spawn (ticks)")
            .defineInRange("boss_arrow_duration_ticks", 100, 20, 1200);

        bossEscortRespawnIntervalSeconds = builder
            .comment("While boss alive, how often the escort cycle repeats")
            .defineInRange("boss_escort_respawn_interval_seconds", 300, 20, 1800);

        bossEscortSpawnStepSeconds = builder
            .comment("Delay between each escort spawn step in a cycle; if 0, uses interval/4")
            .defineInRange("boss_escort_spawn_step_seconds", 75, 0, 600);

        maxDemonsPerPlayer = builder
            .comment("Maximum non-boss demons allowed per player in Mt Fujikasane during Final Selection")
            .defineInRange("max_demons_per_player", 30, 0, 200);

        enableDaytimePassiveAnimals = builder
            .comment("Spawn passive animals during daytime breaks between nights")
            .define("enable_daytime_passive_animals", true);

        daytimePassiveAnimalsPerPlayerMax = builder
            .comment("Maximum passive animals spawned per player during each daytime break")
            .defineInRange("daytime_passive_animals_per_player_max", 5, 0, 20);

        daytimePassiveAnimalSpawnRadius = builder
            .comment("Radius around each player for daytime passive animal spawns")
            .defineInRange("daytime_passive_animal_spawn_radius", 100, 16, 300);

        builder.pop();
        SPEC = builder.build();
    }
}
