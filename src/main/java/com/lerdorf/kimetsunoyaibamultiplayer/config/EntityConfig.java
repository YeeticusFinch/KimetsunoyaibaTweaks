package com.lerdorf.kimetsunoyaibamultiplayer.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;

public class EntityConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    static {
        BUILDER.comment("Entity Enhancement Configuration")
                .push("entities");
    }

    // Kasugai Crow Configuration
    static {
        BUILDER.comment("Kasugai Crow Enhancement Configuration")
                .push("kasugai_crow");
    }

    private static final ForgeConfigSpec.BooleanValue CROW_ENHANCEMENTS_ENABLED = BUILDER
            .comment("Enable all kasugai crow enhancements (master toggle)")
            .define("enhancements-enabled", true);

    private static final ForgeConfigSpec.BooleanValue CROW_FLYING_DODGE_ENABLED = BUILDER
            .comment("Enable the flying dodge mechanic where tamed crows fly away from danger")
            .define("flying-dodge-enabled", true);

    private static final ForgeConfigSpec.DoubleValue CROW_FLIGHT_HEIGHT = BUILDER
            .comment("Height in blocks the crow flies to when dodging (above its current position)")
            .defineInRange("flight-height", 15.0, 10.0, 50.0);

    private static final ForgeConfigSpec.IntValue CROW_FLIGHT_DURATION = BUILDER
            .comment("How long the crow stays flying in ticks (20 ticks = 1 second)")
            .defineInRange("flight-duration", 600, 200, 2400);

    private static final ForgeConfigSpec.DoubleValue CROW_CIRCLE_RADIUS = BUILDER
            .comment("Radius in blocks of the circular flying pattern")
            .defineInRange("circle-radius", 15.0, 10.0, 50.0);

    private static final ForgeConfigSpec.BooleanValue CROW_QUEST_ARROW_ENABLED = BUILDER
            .comment("Enable particle arrow pointing to quest locations when crow gives a quest")
            .define("quest-arrow-enabled", true);

    private static final ForgeConfigSpec.BooleanValue CROW_WAYPOINT_ENABLED = BUILDER
            .comment("Enable waypoint marker at quest target locations")
            .define("waypoint-enabled", true);

    private static final ForgeConfigSpec.IntValue CROW_ARROW_UPDATE_INTERVAL = BUILDER
            .comment("How often to update the quest arrow particles in ticks (lower = smoother but more particles)")
            .defineInRange("arrow-update-interval", 5, 1, 20);

    private static final ForgeConfigSpec.DoubleValue CROW_ARROW_LENGTH = BUILDER
            .comment("Length of the quest arrow in blocks")
            .defineInRange("arrow-length", 3.0, 1.0, 10.0);

    private static final ForgeConfigSpec.IntValue CROW_WAYPOINT_DURATION = BUILDER
            .comment("How long waypoint markers last in ticks (20 ticks = 1 second)")
            .defineInRange("waypoint-duration", 1200, 200, 6000);

    private static final ForgeConfigSpec.DoubleValue CROW_WAYPOINT_COMPLETE_DISTANCE = BUILDER
            .comment("Distance in blocks (X/Z only, Y is ignored) to complete waypoint")
            .defineInRange("waypoint-complete-distance", 2.0, 1.0, 10.0);

    private static final ForgeConfigSpec.BooleanValue CROW_AUTO_DETECT_QUESTS = BUILDER
            .comment("Automatically detect crow quest messages in chat and set waypoints")
            .define("auto-detect-quests", true);

    static {
        BUILDER.pop(); // kasugai_crow
        BUILDER.pop(); // entities
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    // Cached values
    public static boolean crowEnhancementsEnabled;
    public static boolean crowFlyingDodgeEnabled;
    public static double crowFlightHeight;
    public static int crowFlightDuration;
    public static double crowCircleRadius;
    public static boolean crowQuestArrowEnabled;
    public static boolean crowWaypointEnabled;
    public static int crowArrowUpdateInterval;
    public static double crowArrowLength;
    public static int crowWaypointDuration;
    public static double crowWaypointCompleteDistance;
    public static boolean crowAutoDetectQuests;

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event) {
        System.out.println("ENTITY CONFIG LOADING...");
        crowEnhancementsEnabled = CROW_ENHANCEMENTS_ENABLED.get();
        crowFlyingDodgeEnabled = CROW_FLYING_DODGE_ENABLED.get();
        crowFlightHeight = CROW_FLIGHT_HEIGHT.get();
        crowFlightDuration = CROW_FLIGHT_DURATION.get();
        crowCircleRadius = CROW_CIRCLE_RADIUS.get();
        crowQuestArrowEnabled = CROW_QUEST_ARROW_ENABLED.get();
        crowWaypointEnabled = CROW_WAYPOINT_ENABLED.get();
        crowArrowUpdateInterval = CROW_ARROW_UPDATE_INTERVAL.get();
        crowArrowLength = CROW_ARROW_LENGTH.get();
        crowWaypointDuration = CROW_WAYPOINT_DURATION.get();
        crowWaypointCompleteDistance = CROW_WAYPOINT_COMPLETE_DISTANCE.get();
        crowAutoDetectQuests = CROW_AUTO_DETECT_QUESTS.get();

        System.out.println("EntityConfig loaded: crowEnhancements=" + crowEnhancementsEnabled +
                         ", flyingDodge=" + crowFlyingDodgeEnabled + ", flightHeight=" + crowFlightHeight +
                         ", flightDuration=" + crowFlightDuration + ", circleRadius=" + crowCircleRadius +
                         ", questArrow=" + crowQuestArrowEnabled + ", waypoint=" + crowWaypointEnabled +
                         ", waypointDuration=" + crowWaypointDuration + ", autoDetect=" + crowAutoDetectQuests);
    }
}