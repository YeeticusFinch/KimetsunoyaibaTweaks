package com.lerdorf.kimetsunoyaibamultiplayer;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;

public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    static {
        BUILDER.comment("Common Configuration")
                .push("common");
    }

    // Debug and general settings
    private static final ForgeConfigSpec.BooleanValue LOG_DEBUG = BUILDER
            .comment("Enable debug logging for the mod")
            .define("log-debug", false);

    private static final ForgeConfigSpec.BooleanValue ON_SCREEN_DEBUG = BUILDER
            .comment("Enable on-screen debug information display")
            .define("on-screen-debug", false);

    static {
        BUILDER.pop(); // common
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean logDebug;
    public static boolean onScreenDebug;

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event)
    {
        System.out.println("COMMON CONFIG LOADING...");
        logDebug = LOG_DEBUG.get();
        onScreenDebug = ON_SCREEN_DEBUG.get();
        System.out.println("Common config loaded: logDebug=" + logDebug + ", onScreenDebug=" + onScreenDebug);
    }
}