package com.lerdorf.kimetsunoyaibamultiplayer.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Configuration for generic demon slayer entity spawning.
 */
public class DemonSlayerConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.DoubleValue FEMALE_SPAWN_CHANCE;

    static {
        BUILDER.comment("Generic Demon Slayer Configuration").push("demon_slayer");

        FEMALE_SPAWN_CHANCE = BUILDER
            .comment("Chance (0.0-1.0) that a demon slayer spawns as female instead of male.",
                     "0.0 = always male, 1.0 = always female, 0.3 = 30% female (default)")
            .defineInRange("femaleSpawnChance", 0.3, 0.0, 1.0);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    /**
     * Get the configured female spawn chance.
     */
    public static double getFemaleSpawnChance() {
        return FEMALE_SPAWN_CHANCE.get();
    }
}
