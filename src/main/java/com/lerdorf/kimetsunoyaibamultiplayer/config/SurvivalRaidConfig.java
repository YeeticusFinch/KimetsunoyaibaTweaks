package com.lerdorf.kimetsunoyaibamultiplayer.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Configuration for command-triggered survival raids.
 * This config is intentionally separate from omen raid config.
 */
public class SurvivalRaidConfig {
    public static final ForgeConfigSpec SPEC;

    public static ForgeConfigSpec.BooleanValue enableSurvivalRaids;
    public static ForgeConfigSpec.IntValue wavePreparationTime;
    public static ForgeConfigSpec.IntValue entitySpawnInterval;
    public static ForgeConfigSpec.IntValue waveInterval;
    public static ForgeConfigSpec.IntValue defaultRadius;
    public static ForgeConfigSpec.IntValue bossSpawnRadius;
    public static ForgeConfigSpec.IntValue entitySpawnNearPlayerRadius;
    public static ForgeConfigSpec.BooleanValue enableBossArrow;
    public static ForgeConfigSpec.IntValue bossGlowDuration;
    public static ForgeConfigSpec.IntValue bossArrowDuration;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Survival Raid Configuration")
            .push("survival_raids");

        enableSurvivalRaids = builder
            .comment("Enable survival raid system")
            .define("enable_survival_raids", true);

        wavePreparationTime = builder
            .comment("Preparation time before each wave starts (seconds)")
            .defineInRange("wave_preparation_time", 8, 0, 120);

        entitySpawnInterval = builder
            .comment("Delay between each entity spawn (seconds)")
            .defineInRange("entity_spawn_interval", 1, 1, 20);

        waveInterval = builder
            .comment("Base interval between waves/reinforcements (seconds)")
            .defineInRange("wave_interval", 90, 20, 600);

        defaultRadius = builder
            .comment("Default raid radius used by command")
            .defineInRange("default_radius", 200, 50, 1000);

        bossSpawnRadius = builder
            .comment("Boss spawn radius around raid center")
            .defineInRange("boss_spawn_radius", 100, 16, 500);

        entitySpawnNearPlayerRadius = builder
            .comment("Maximum distance from raid players for non-boss spawns")
            .defineInRange("entity_spawn_near_player_radius", 200, 32, 500);

        enableBossArrow = builder
            .comment("Enable temporary boss direction arrow effect")
            .define("enable_boss_arrow", true);

        bossGlowDuration = builder
            .comment("Boss glow duration on spawn (ticks)")
            .defineInRange("boss_glow_duration", 100, 20, 1200);

        bossArrowDuration = builder
            .comment("Boss arrow duration on spawn (ticks)")
            .defineInRange("boss_arrow_duration", 100, 20, 1200);

        builder.pop();
        SPEC = builder.build();
    }
}
