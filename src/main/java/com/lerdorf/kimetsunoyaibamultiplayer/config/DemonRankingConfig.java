package com.lerdorf.kimetsunoyaibamultiplayer.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Configuration for the Demon Ranking (Twelve Kizuki) system.
 */
public class DemonRankingConfig {
    public static final ForgeConfigSpec SPEC;

    public static ForgeConfigSpec.BooleanValue enableDemonRanking;
    public static ForgeConfigSpec.IntValue offlineTakeoverMinutes;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Demon Ranking (Twelve Kizuki) Configuration")
            .push("demon_ranking");

        enableDemonRanking = builder
            .comment("Master switch for the Demon Ranking (Twelve Kizuki) system.",
                    "",
                    "When disabled, kills never change anyone's rank, no rank buffs are applied,",
                    "and the Bloody Battle entry is hidden from the Meditation Menu Navigation tab.")
            .define("enable_demon_ranking", true);

        offlineTakeoverMinutes = builder
            .comment("How many minutes a ranked player must be offline before their rank",
                    "can be taken by killing that rank's fallback entity instead of the player.",
                    "",
                    "Use /freerank to bypass this timer for a specific rank immediately.",
                    "",
                    "Default: 4320 (3 days)")
            .defineInRange("offline_takeover_minutes", 4320, 0, Integer.MAX_VALUE);

        builder.pop();

        SPEC = builder.build();
    }

    public static boolean isEnabled() {
        return enableDemonRanking != null && enableDemonRanking.get();
    }

    public static long getOfflineTakeoverThresholdMillis() {
        int minutes = offlineTakeoverMinutes == null ? 4320 : offlineTakeoverMinutes.get();
        return minutes * 60_000L;
    }
}
