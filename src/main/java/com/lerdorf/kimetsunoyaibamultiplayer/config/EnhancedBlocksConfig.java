package com.lerdorf.kimetsunoyaibamultiplayer.config;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;

public class EnhancedBlocksConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue ENHANCED_CHEST_OF_DRAWERS = BUILDER
        .comment("Enable enhanced chest of drawers replacement",
            "When true, base mod chest of drawers items/blocks are replaced with the multiplayer version:",
            "  - kimetsunoyaiba:chest_of_drawer -> kimetsunoyaibamultiplayer:chest_of_drawers",
            "Right-clicking a base mod chest of drawers will convert it and immediately pass the click through.",
            "Default: true")
        .define("enhancedChestOfDrawers", true);

    private static final ForgeConfigSpec.BooleanValue ENHANCED_VIAL_RACK = BUILDER
        .comment("Enable enhanced vial rack replacement",
            "When true, base mod medicine holder items/blocks are replaced with the multiplayer vial rack:",
            "  - kimetsunoyaiba:medicine_holder -> kimetsunoyaibamultiplayer:vial_rack",
            "Nearby base mod medicine holder blocks are periodically converted with randomized rack contents.",
            "Default: true")
        .define("enhancedVialRack", true);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean enhancedChestOfDrawers;
    public static boolean enhancedVialRack;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        enhancedChestOfDrawers = ENHANCED_CHEST_OF_DRAWERS.get();
        enhancedVialRack = ENHANCED_VIAL_RACK.get();
        Log.info("Loaded enhanced blocks config: enhancedChestOfDrawers={}, enhancedVialRack={}",
            enhancedChestOfDrawers, enhancedVialRack);
    }
}
