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
    
    private static final ForgeConfigSpec.BooleanValue LOG_INFO = BUILDER
            .comment("Enable info logging for the mod")
            .define("log-info", false);
    
    private static final ForgeConfigSpec.BooleanValue LOG_WARNING = BUILDER
            .comment("Enable warning logging for the mod")
            .define("log-warning", false);
    
    private static final ForgeConfigSpec.BooleanValue LOG_ERROR = BUILDER
            .comment("Enable error logging for the mod")
            .define("log-error", true);

    private static final ForgeConfigSpec.BooleanValue ON_SCREEN_DEBUG = BUILDER
            .comment("Enable on-screen debug information display")
            .define("on-screen-debug", false);

    static {
        BUILDER.pop(); // common
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean logDebug;
    public static boolean logWarning;
    public static boolean logInfo;
    public static boolean logError;
    public static boolean onScreenDebug;

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event)
    {
    	if (Config.logDebug)
        System.out.println("COMMON CONFIG LOADING...");
        logDebug = LOG_DEBUG.get();
        logWarning = LOG_WARNING.get();
        logInfo = LOG_INFO.get();
        logError = LOG_ERROR.get();
        onScreenDebug = ON_SCREEN_DEBUG.get();
        if (Config.logDebug)
        System.out.println("Common config loaded: logDebug=" + logDebug + ", onScreenDebug=" + onScreenDebug);
    }
}