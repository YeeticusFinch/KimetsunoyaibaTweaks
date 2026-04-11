package com.lerdorf.kimetsunoyaibamultiplayer.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(
    modid = com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer.MODID,
    bus = Mod.EventBusSubscriber.Bus.MOD
)
public final class SwordsmithVillageConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.IntValue MAX_POPULATION = BUILDER
        .comment("Maximum stored population target for the Swordsmith Village dimension.")
        .defineInRange("maxPopulation", 50, 0, 10000);

    private static final ForgeConfigSpec.DoubleValue NOON_RECOVERY_CHANCE = BUILDER
        .comment("Chance at each noon to recover 1 stored population if below the configured maximum.")
        .defineInRange("noonRecoveryChance", 0.30D, 0.0D, 1.0D);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static int maxPopulation = 50;
    public static double noonRecoveryChance = 0.30D;

    private SwordsmithVillageConfig() {
    }

    @SubscribeEvent
    public static void onLoad(ModConfigEvent event) {
        maxPopulation = MAX_POPULATION.get();
        noonRecoveryChance = NOON_RECOVERY_CHANCE.get();
    }
}
