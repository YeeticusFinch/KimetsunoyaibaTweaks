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

    // Breathing form display settings
    private static final ForgeConfigSpec.BooleanValue SHOW_BREATHING_DISPLAY = BUILDER
            .comment("Show on-screen breathing form display when holding a nichirin sword")
            .define("show-breathing-display", true);

    private static final ForgeConfigSpec.EnumValue<DisplayPosition> BREATHING_DISPLAY_POSITION = BUILDER
            .comment("Position of the breathing form display on screen: TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER_BELOW_CROSSHAIR")
            .defineEnum("breathing-display-position", DisplayPosition.TOP_LEFT);

    private static final ForgeConfigSpec.DoubleValue BREATHING_DISPLAY_SCALE = BUILDER
            .comment("Scale/size of the breathing form display text (0.5 = half size, 1.0 = normal, 2.0 = double size)")
            .defineInRange("breathing-display-scale", 0.75, 0.1, 5.0);

    private static final ForgeConfigSpec.BooleanValue SUPPRESS_FORM_CYCLE_CHAT = BUILDER
            .comment("Suppress chat messages when cycling breathing forms with R key")
            .define("suppress-form-cycle-chat", false);

    private static final ForgeConfigSpec.BooleanValue ENABLE_SWORD_CLASHING = BUILDER
            .comment("Enable sword clashing system where attacks can be deflected or mitigated")
            .define("enable-sword-clashing", true);

    public enum DisplayPosition {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        CENTER_BELOW_CROSSHAIR
    }

    static {
        BUILDER.pop(); // common
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean logDebug;
    public static boolean logWarning;
    public static boolean logInfo;
    public static boolean logError;
    public static boolean onScreenDebug;
    public static boolean showBreathingDisplay;
    public static DisplayPosition breathingDisplayPosition;
    public static double breathingDisplayScale;
    public static boolean suppressFormCycleChat;
    public static boolean enableSwordClashing;

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
        showBreathingDisplay = SHOW_BREATHING_DISPLAY.get();
        breathingDisplayPosition = BREATHING_DISPLAY_POSITION.get();
        breathingDisplayScale = BREATHING_DISPLAY_SCALE.get();
        suppressFormCycleChat = SUPPRESS_FORM_CYCLE_CHAT.get();
        enableSwordClashing = ENABLE_SWORD_CLASHING.get();
        if (Config.logDebug)
        System.out.println("Common config loaded: logDebug=" + logDebug + ", onScreenDebug=" + onScreenDebug +
                ", showBreathingDisplay=" + showBreathingDisplay + ", breathingDisplayPosition=" + breathingDisplayPosition +
                ", breathingDisplayScale=" + breathingDisplayScale + ", suppressFormCycleChat=" + suppressFormCycleChat +
                ", enableSwordClashing=" + enableSwordClashing);
    }
}