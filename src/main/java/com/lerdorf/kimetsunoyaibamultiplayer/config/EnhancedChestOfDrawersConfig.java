package com.lerdorf.kimetsunoyaibamultiplayer.config;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;

public class EnhancedChestOfDrawersConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue ENHANCED_CHEST_OF_DRAWERS = BUILDER
        .comment("Enable enhanced chest of drawers replacement",
            "When true, base mod chest of drawers items/blocks are replaced with the multiplayer version:",
            "  - kimetsunoyaiba:chest_of_drawer -> kimetsunoyaibamultiplayer:chest_of_drawers",
            "Right-clicking a base mod chest of drawers will convert it and immediately pass the click through.",
            "Default: true")
        .define("enhancedChestOfDrawers", true);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean enhancedChestOfDrawers;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        enhancedChestOfDrawers = ENHANCED_CHEST_OF_DRAWERS.get();
        Log.info("Loaded enhanced chest of drawers config: {}", enhancedChestOfDrawers);
    }
}
