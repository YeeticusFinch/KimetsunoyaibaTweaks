package com.lerdorf.kimetsunoyaibamultiplayer;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue LOG_DEBUG = BUILDER
            .comment("Enable debug logging to console for animation sync debugging. Shows detailed information about animation detection, packet sending/receiving, and sync status.")
            .define("log-debug", false);

    private static final ForgeConfigSpec.BooleanValue ON_SCREEN_DEBUG = BUILDER
            .comment("Enable on-screen debug messages for animation sync. Shows colored chat messages when animations are detected, sent, or received from other players.")
            .define("on-screen-debug", false);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean logDebug;
    public static boolean onScreenDebug;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        logDebug = LOG_DEBUG.get();
        onScreenDebug = ON_SCREEN_DEBUG.get();
    }
}
