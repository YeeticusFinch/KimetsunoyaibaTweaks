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

    private static final ForgeConfigSpec.BooleanValue SHOW_BREATHES_VALUE = BUILDER
            .comment("Show the raw breathes NBT value in the breathing display (useful for debugging form IDs)")
            .define("show-breathes-value", false);

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

    private static final ForgeConfigSpec.BooleanValue ENABLE_NICHIRIN_SPRINT_ANIMATION = BUILDER
            .comment("Enable custom sprint animation when holding a nichirin sword")
            .define("enable-nichirin-sprint-animation", true);

    private static final ForgeConfigSpec.BooleanValue DISABLE_BASE_MOD_SWORD_SWING_PARTICLES = BUILDER
            .comment("Disable sword swing particles from the base KimetsunoYaiba mod (left-click particles only, does not affect breathing form or right-click particles)")
            .define("disable-base-mod-sword-swing-particles", false);

    private static final ForgeConfigSpec.BooleanValue DEMONIZED_BREATHING_STYLES = BUILDER
            .comment("Demons (entities or demonized players) using Serpent Breathing emit black dust particles instead of white ones")
            .define("demonized-breathing-styles", true);

    private static final ForgeConfigSpec.BooleanValue REQUIRE_LINE_OF_SIGHT_AGGRO = BUILDER
            .comment("Require line of sight for demon/demon-slayer aggro",
                     "When enabled, demons and demon slayers (from the base mod and this mod) cannot aggro",
                     "entities they cannot see, e.g. a demon far underground will not target a player on the surface.")
            .define("require-line-of-sight-aggro", true);

    private static final ForgeConfigSpec.BooleanValue SHOW_BACKSTEP_HUD = BUILDER
            .comment("Show the base KimetsunoYaiba backstep charge HUD icon and count")
            .define("show-backstep-hud", true);

    private static final ForgeConfigSpec.DoubleValue BACKSTEP_HUD_OFFSET_X = BUILDER
            .comment("Horizontal offset as a percentage of the GUI-scaled window width for the backstep charge HUD. 0 keeps the base mod position.")
            .defineInRange("backstep-hud-offset-x", 0.0, -100.0, 100.0);

    private static final ForgeConfigSpec.DoubleValue BACKSTEP_HUD_OFFSET_Y = BUILDER
            .comment("Vertical offset as a percentage of the GUI-scaled window height for the backstep charge HUD. 0 keeps the base mod position.")
            .defineInRange("backstep-hud-offset-y", 0.0, -100.0, 100.0);

    static {
        BUILDER.comment("Breathing form announcements")
                .push("breathing-form-announcements");
    }

    private static final ForgeConfigSpec.BooleanValue PLAYERS_ANNOUNCE_BREATHING_FORMS = BUILDER
            .comment("Announce breathing forms used by players in chat to nearby players")
            .define("players-announce-breathing-forms", false);

    private static final ForgeConfigSpec.BooleanValue ENTITIES_ANNOUNCE_BREATHING_FORMS = BUILDER
            .comment("Announce breathing forms used by entities in chat to nearby players")
            .define("entities-announce-breathing-forms", false);

    private static final ForgeConfigSpec.DoubleValue BREATHING_FORM_ANNOUNCEMENT_RADIUS = BUILDER
            .comment("Radius in blocks for breathing form chat announcements")
            .defineInRange("breathing-form-announcement-radius", 20.0, 1.0, 256.0);

    static {
        BUILDER.pop();
    }

    // Networking / visuals radius
    private static final ForgeConfigSpec.DoubleValue MOB_SLASH_BROADCAST_RANGE = BUILDER
            .comment("Max distance in blocks to send mob sword slash packets to clients",
                     "Lower to reduce network traffic; Default: 100")
            .defineInRange("mob-slash-broadcast-range", 100.0, 8.0, 512.0);

    private static final ForgeConfigSpec.IntValue WISTERIA_INCENSE_MAX_POISON_AMPLIFIER = BUILDER
            .comment("Maximum MobEffect amplifier for wisteria poison from overlapping lit wisteria incense auras")
            .defineInRange("wisteria-incense-max-poison-amplifier", 50, 0, 255);

    // Kanroji entity sword rendering offsets
    private static final ForgeConfigSpec.DoubleValue KANROJI_ENTITY_HAND_OFFSET_X = BUILDER
            .comment("X offset for Kanroji entity sword position to compensate for model alignment (in pixels, divided by 16)")
            .defineInRange("kanroji-entity-hand-offset-x", -9.3, -16.0, 16.0);

    private static final ForgeConfigSpec.DoubleValue KANROJI_ENTITY_HAND_OFFSET_Y = BUILDER
            .comment("Y offset for Kanroji entity sword position to compensate for model alignment (in pixels, divided by 16)")
            .defineInRange("kanroji-entity-hand-offset-y", -7.5, -16.0, 16.0);

    private static final ForgeConfigSpec.DoubleValue KANROJI_ENTITY_HAND_OFFSET_Z = BUILDER
            .comment("Z offset for Kanroji entity sword position to compensate for model alignment (in pixels, divided by 16)")
            .defineInRange("kanroji-entity-hand-offset-z", -8.7, -16.0, 16.0);

    public enum DisplayPosition {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        CENTER_BELOW_CROSSHAIR
    }

    static {
        // Initialize first-person sword swing config
        com.lerdorf.kimetsunoyaibamultiplayer.config.FirstPersonSwordSwingConfig.init(BUILDER);

        BUILDER.pop(); // common
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean logDebug;
    public static boolean logWarning;
    public static boolean logInfo;
    public static boolean logError;
    public static boolean onScreenDebug;
    public static boolean showBreathesValue;
    public static boolean showBreathingDisplay;
    public static DisplayPosition breathingDisplayPosition;
    public static double breathingDisplayScale;
    public static boolean suppressFormCycleChat;
    public static boolean enableSwordClashing;
    public static boolean enableNichirinSprintAnimation;
    public static boolean disableBaseModSwordSwingParticles;
    public static boolean demonizedBreathingStyles;
    public static boolean requireLineOfSightAggro;
    public static boolean showBackstepHud;
    public static double backstepHudOffsetX;
    public static double backstepHudOffsetY;
    public static boolean playersAnnounceBreathingForms;
    public static boolean entitiesAnnounceBreathingForms;
    public static double breathingFormAnnouncementRadius;
    public static double mobSlashBroadcastRange;
    public static int wisteriaIncenseMaxPoisonAmplifier = 50;
    public static double kanrojiEntityHandOffsetX;
    public static double kanrojiEntityHandOffsetY;
    public static double kanrojiEntityHandOffsetZ;

    // First-person sword swing config
    public static boolean customFirstPersonSwingEnabled;
    public static double counterSwingRotateX;
    public static double counterSwingRotateY;
    public static double counterSwingRotateZ;
    public static double counterSwingTranslateX;
    public static double counterSwingTranslateY;
    public static double counterSwingTranslateZ;

    public static double counterSwingRotateX2;
    public static double counterSwingRotateY2;
    public static double counterSwingRotateZ2;
    public static double counterSwingTranslateX2;
    public static double counterSwingTranslateY2;
    public static double counterSwingTranslateZ2;
    
    public static double translateScale;

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event)
    {
        Log.debug("COMMON CONFIG LOADING...");
        logDebug = LOG_DEBUG.get();
        logWarning = LOG_WARNING.get();
        logInfo = LOG_INFO.get();
        logError = LOG_ERROR.get();
        Log.startupProbe("Config.onLoad");
        onScreenDebug = ON_SCREEN_DEBUG.get();
        showBreathesValue = SHOW_BREATHES_VALUE.get();
        showBreathingDisplay = SHOW_BREATHING_DISPLAY.get();
        breathingDisplayPosition = BREATHING_DISPLAY_POSITION.get();
        breathingDisplayScale = BREATHING_DISPLAY_SCALE.get();
        suppressFormCycleChat = SUPPRESS_FORM_CYCLE_CHAT.get();
        enableSwordClashing = ENABLE_SWORD_CLASHING.get();
        enableNichirinSprintAnimation = ENABLE_NICHIRIN_SPRINT_ANIMATION.get();
        disableBaseModSwordSwingParticles = DISABLE_BASE_MOD_SWORD_SWING_PARTICLES.get();
        demonizedBreathingStyles = DEMONIZED_BREATHING_STYLES.get();
        requireLineOfSightAggro = REQUIRE_LINE_OF_SIGHT_AGGRO.get();
        showBackstepHud = SHOW_BACKSTEP_HUD.get();
        backstepHudOffsetX = BACKSTEP_HUD_OFFSET_X.get();
        backstepHudOffsetY = BACKSTEP_HUD_OFFSET_Y.get();
        playersAnnounceBreathingForms = PLAYERS_ANNOUNCE_BREATHING_FORMS.get();
        entitiesAnnounceBreathingForms = ENTITIES_ANNOUNCE_BREATHING_FORMS.get();
        breathingFormAnnouncementRadius = BREATHING_FORM_ANNOUNCEMENT_RADIUS.get();
        Log.info("[KnY-MP] Config loaded - disableBaseModSwordSwingParticles: {}", disableBaseModSwordSwingParticles);
        mobSlashBroadcastRange = MOB_SLASH_BROADCAST_RANGE.get();
        wisteriaIncenseMaxPoisonAmplifier = WISTERIA_INCENSE_MAX_POISON_AMPLIFIER.get();
        kanrojiEntityHandOffsetX = KANROJI_ENTITY_HAND_OFFSET_X.get();
        kanrojiEntityHandOffsetY = KANROJI_ENTITY_HAND_OFFSET_Y.get();
        kanrojiEntityHandOffsetZ = KANROJI_ENTITY_HAND_OFFSET_Z.get();

        // Load first-person sword swing config
        customFirstPersonSwingEnabled = com.lerdorf.kimetsunoyaibamultiplayer.config.FirstPersonSwordSwingConfig.customSwingEnabled.get();
        counterSwingRotateX = com.lerdorf.kimetsunoyaibamultiplayer.config.FirstPersonSwordSwingConfig.counterSwingRotateX.get();
        counterSwingRotateY = com.lerdorf.kimetsunoyaibamultiplayer.config.FirstPersonSwordSwingConfig.counterSwingRotateY.get();
        counterSwingRotateZ = com.lerdorf.kimetsunoyaibamultiplayer.config.FirstPersonSwordSwingConfig.counterSwingRotateZ.get();
        counterSwingTranslateX = com.lerdorf.kimetsunoyaibamultiplayer.config.FirstPersonSwordSwingConfig.counterSwingTranslateX.get();
        counterSwingTranslateY = com.lerdorf.kimetsunoyaibamultiplayer.config.FirstPersonSwordSwingConfig.counterSwingTranslateY.get();
        counterSwingTranslateZ = com.lerdorf.kimetsunoyaibamultiplayer.config.FirstPersonSwordSwingConfig.counterSwingTranslateZ.get();
        counterSwingRotateX2 = com.lerdorf.kimetsunoyaibamultiplayer.config.FirstPersonSwordSwingConfig.counterSwingRotateX2.get();
        counterSwingRotateY2 = com.lerdorf.kimetsunoyaibamultiplayer.config.FirstPersonSwordSwingConfig.counterSwingRotateY2.get();
        counterSwingRotateZ2 = com.lerdorf.kimetsunoyaibamultiplayer.config.FirstPersonSwordSwingConfig.counterSwingRotateZ2.get();
        counterSwingTranslateX2 = com.lerdorf.kimetsunoyaibamultiplayer.config.FirstPersonSwordSwingConfig.counterSwingTranslateX2.get();
        counterSwingTranslateY2 = com.lerdorf.kimetsunoyaibamultiplayer.config.FirstPersonSwordSwingConfig.counterSwingTranslateY2.get();
        counterSwingTranslateZ2 = com.lerdorf.kimetsunoyaibamultiplayer.config.FirstPersonSwordSwingConfig.counterSwingTranslateZ2.get();
        translateScale = com.lerdorf.kimetsunoyaibamultiplayer.config.FirstPersonSwordSwingConfig.translateScale.get();
        
        Log.debug("Common config loaded: logDebug={}, onScreenDebug={}, showBreathingDisplay={}, breathingDisplayPosition={}, breathingDisplayScale={}, suppressFormCycleChat={}, enableSwordClashing={}, enableNichirinSprintAnimation={}, playersAnnounceBreathingForms={}, entitiesAnnounceBreathingForms={}, breathingFormAnnouncementRadius={}, mobSlashBroadcastRange={}",
            logDebug, onScreenDebug, showBreathingDisplay, breathingDisplayPosition, breathingDisplayScale,
            suppressFormCycleChat, enableSwordClashing, enableNichirinSprintAnimation,
            playersAnnounceBreathingForms, entitiesAnnounceBreathingForms,
            breathingFormAnnouncementRadius, mobSlashBroadcastRange);
    }
}
