package com.lerdorf.kimetsunoyaibamultiplayer.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.HashMap;
import java.util.Map;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;

/**
 * Configuration for enhanced spawning rules system.
 *
 * Provides comprehensive control over entity spawning including:
 * - Master switch to enable/disable enhanced rules
 * - Generic spawn rate reductions for demons and demon slayers
 * - Entity replacement system configuration
 * - Protective spawning mechanics settings
 * - Maximum entity tracking configuration
 */
public class EnhancedSpawnConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // ========== MASTER SWITCH ==========
    static {
        BUILDER.comment("Enhanced Spawning Rules Configuration",
                       "Controls advanced spawning mechanics for demons, demon slayers, and other entities")
               .push("enhanced_spawning");
    }

    private static final ForgeConfigSpec.BooleanValue ENHANCED_SPAWNING_RULES = BUILDER
            .comment("Master switch for enhanced spawning rules.",
                    "If true, uses complex biome/structure/dimension rules.",
                    "If false, falls back to simple spawn priority system.")
            .define("enhanced_spawning_rules", true);

    private static final ForgeConfigSpec.BooleanValue REPLACE_BASE_GENERIC_DEMON_SLAYERS = BUILDER
            .comment("Replace base mod generic demon slayers with kimetsunoyaibamultiplayer demon slayers.",
                    "Replaces:",
                    "- kimetsunoyaiba:demon_slayer -> multiplayer demon slayer (level 0-3)",
                    "- kimetsunoyaiba:dice_steak_senior -> multiplayer demon slayer (level 4)",
                    "- kimetsunoyaiba:dice_steak_senior_super -> multiplayer demon slayer (level 5)")
            .define("replace_base_generic_demon_slayers", true);

    private static final ForgeConfigSpec.BooleanValue REPLACE_BASE_NEZUKO = BUILDER
            .comment("Replace base mod Nezuko with kimetsunoyaibamultiplayer Nezuko.",
                    "Replaces:",
                    "- kimetsunoyaiba:nezuko -> kimetsunoyaibamultiplayer:nezuko")
            .define("replace_base_nezuko", true);

    private static final ForgeConfigSpec.BooleanValue PREVENT_YORICHI_TYPE_ZERO_NATURAL_SPAWNS = BUILDER
            .comment("Prevent Yorichi Type 0 from spawning naturally.",
                    "When enabled, kimetsunoyaiba:yorichi_0 is blocked from natural spawning anywhere.",
                    "Commands, spawn eggs, and other manual spawn paths are unaffected.")
            .define("prevent_yorichi_type_0_natural_spawns", true);

    // ========== GENERIC SPAWN RATES ==========
    static {
        BUILDER.comment("Generic spawn rate reductions")
               .push("generic_spawn_rates");
    }

    private static final ForgeConfigSpec.DoubleValue GENERIC_DEMON_SPAWN_RATE = BUILDER
            .comment("Spawn rate multiplier for generic demons outside designated areas (0.0-1.0).",
                    "0.4 = 40% of normal spawn rate, 1.0 = 100% normal rate, 0.0 = never spawn")
            .defineInRange("generic_demon_spawn_rate", 0.4, 0.0, 1.0);

    private static final ForgeConfigSpec.DoubleValue GENERIC_DEMON_SLAYER_SPAWN_RATE = BUILDER
            .comment("Spawn rate multiplier for demon slayers outside designated areas (0.0-1.0).",
                    "0.1 = 10% of normal spawn rate")
            .defineInRange("generic_demon_slayer_spawn_rate", 0.1, 0.0, 1.0);

    static {
        BUILDER.pop(); // generic_spawn_rates
    }

    // ========== MAXIMUM ENTITY TRACKING ==========
    static {
        BUILDER.comment("Maximum entity tracking configuration")
               .push("max_entity_tracking");
    }

    private static final ForgeConfigSpec.IntValue MAX_ENTITY_CHECK_RADIUS = BUILDER
            .comment("Radius in blocks to check for maximum entity limits.",
                    "Used to prevent multiple unique entities from spawning too close together.")
            .defineInRange("max_entity_check_radius", 500, 50, 2000);

    static {
        BUILDER.pop(); // max_entity_tracking
    }

    // ========== PROTECTIVE SPAWNING ==========
    static {
        BUILDER.comment("Protective spawning mechanics (Iron Golem style)",
                       "Spawns demon slayers/hashira when civilians are threatened")
               .push("protective_spawning");
    }

    private static final ForgeConfigSpec.BooleanValue ENABLE_PROTECTIVE_SPAWNING = BUILDER
            .comment("Enable protective spawning mechanics")
            .define("enable_protective_spawning", true);

    private static final ForgeConfigSpec.DoubleValue HASHIRA_SPAWN_CHANCE = BUILDER
            .comment("Chance for a hashira to spawn when a civilian sees/is attacked by a twelve kizuki (0.0-1.0)")
            .defineInRange("hashira_spawn_chance", 0.05, 0.0, 1.0);

    private static final ForgeConfigSpec.DoubleValue KAMABOKO_SPAWN_CHANCE = BUILDER
            .comment("Chance for a kamaboko member to spawn when a civilian sees/is attacked by a twelve kizuki (0.0-1.0)")
            .defineInRange("kamaboko_spawn_chance", 0.05, 0.0, 1.0);

    private static final ForgeConfigSpec.DoubleValue DEMON_SLAYER_SPAWN_CHANCE = BUILDER
            .comment("Chance for demon slayers to spawn when a civilian sees/is attacked by a demon (0.0-1.0)")
            .defineInRange("demon_slayer_spawn_chance", 0.15, 0.0, 1.0);

    private static final ForgeConfigSpec.IntValue DEMON_SLAYER_GROUP_SIZE_MIN = BUILDER
            .comment("Minimum number of demon slayers to spawn in a protective spawn")
            .defineInRange("demon_slayer_group_size_min", 2, 1, 10);

    private static final ForgeConfigSpec.IntValue DEMON_SLAYER_GROUP_SIZE_MAX = BUILDER
            .comment("Maximum number of demon slayers to spawn in a protective spawn")
            .defineInRange("demon_slayer_group_size_max", 4, 1, 20);

    static {
        BUILDER.pop(); // protective_spawning
    }

    // ========== ENTITY REPLACER ==========
    static {
        BUILDER.comment("Entity replacement system",
                       "Replaces kimetsunoyaiba entities with kimetsu mod entities if loaded")
               .push("entity_replacer");
    }

    private static final ForgeConfigSpec.BooleanValue ENABLE_KIMETSU_REPLACER = BUILDER
            .comment("Enable replacement of kimetsunoyaiba entities with kimetsu mod entities",
                    "Requires kimetsu mod to be installed",
                    "Currently only replaces: nakime -> nakime_spawn_egg")
            .define("enable_kimetsu_replacer", true);

    private static final ForgeConfigSpec.BooleanValue ENABLE_KIMETSU_MOVIE_REPLACER = BUILDER
            .comment("Enable replacement with kimetsu mod movie versions",
                    "Requires kimetsu mod to be installed",
                    "Replaces: nakime, daki, gyutaro, kaigaku, gyokko, zohakuten, akaza, doma, kokushibo, himejima, kocho")
            .define("enable_kimetsu_movie_replacer", false);

    // Individual entity replacer toggles
    private static final Map<String, ForgeConfigSpec.BooleanValue> ENTITY_REPLACER_TOGGLES = new HashMap<>();

    static {
        String[] replaceableEntities = {
            "nakime", "daki", "gyutaro", "kaigaku", "gyokko", "zohakuten",
            "akaza", "doma", "kokushibo", "himejima", "kocho"
        };

        for (String entity : replaceableEntities) {
            ENTITY_REPLACER_TOGGLES.put(entity, BUILDER
                .comment("Enable replacement for " + entity)
                .define("replace_" + entity, true));
        }

        BUILDER.pop(); // entity_replacer
    }

    // ========== DIMENSION SPECIFIC ==========
    static {
        BUILDER.comment("Mt Fujikasane dimension spawning configuration")
               .push("mt_fujikasane_dimension");
    }

    private static final ForgeConfigSpec.IntValue MT_FUJIKASANE_DEMON_CENTER_X = BUILDER
            .comment("X coordinate of demon spawn center in Mt Fujikasane")
            .defineInRange("demon_spawn_center_x", 227, -10000, 10000);

    private static final ForgeConfigSpec.IntValue MT_FUJIKASANE_DEMON_CENTER_Y = BUILDER
            .comment("Y coordinate of demon spawn center in Mt Fujikasane")
            .defineInRange("demon_spawn_center_y", 319, 0, 500);

    private static final ForgeConfigSpec.IntValue MT_FUJIKASANE_DEMON_CENTER_Z = BUILDER
            .comment("Z coordinate of demon spawn center in Mt Fujikasane")
            .defineInRange("demon_spawn_center_z", 70, -10000, 10000);

    private static final ForgeConfigSpec.IntValue MT_FUJIKASANE_DEMON_RADIUS = BUILDER
            .comment("Radius around demon spawn center where demons can spawn")
            .defineInRange("demon_spawn_radius", 400, 50, 1000);

    private static final ForgeConfigSpec.IntValue MT_FUJIKASANE_KAMABOKO_X = BUILDER
            .comment("X coordinate of kamaboko spawn area in Mt Fujikasane")
            .defineInRange("kamaboko_spawn_x", 311, -10000, 10000);

    private static final ForgeConfigSpec.IntValue MT_FUJIKASANE_KAMABOKO_Y = BUILDER
            .comment("Y coordinate of kamaboko spawn area in Mt Fujikasane")
            .defineInRange("kamaboko_spawn_y", 76, 0, 500);

    private static final ForgeConfigSpec.IntValue MT_FUJIKASANE_KAMABOKO_Z = BUILDER
            .comment("Z coordinate of kamaboko spawn area in Mt Fujikasane")
            .defineInRange("kamaboko_spawn_z", 736, -10000, 10000);

    private static final ForgeConfigSpec.IntValue MT_FUJIKASANE_KAMABOKO_RADIUS = BUILDER
            .comment("Radius around kamaboko spawn point where kamaboko can spawn")
            .defineInRange("kamaboko_spawn_radius", 20, 5, 100);

    static {
        BUILDER.pop(); // mt_fujikasane_dimension
        BUILDER.pop(); // enhanced_spawning
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    // Runtime cache - Initialize with default values before config loads
    public static boolean enhancedSpawningRules = true;  // Default to true
    public static boolean replaceBaseGenericDemonSlayers = true;
    public static boolean replaceBaseNezuko = true;
    public static boolean preventYorichiTypeZeroNaturalSpawns = true;
    public static double genericDemonSpawnRate = 0.4;
    public static double genericDemonSlayerSpawnRate = 0.1;
    public static int maxEntityCheckRadius = 500;
    public static boolean enableProtectiveSpawning = true;
    public static double hashiraSpawnChance = 0.05;
    public static double kamabokoSpawnChance = 0.05;
    public static double demonSlayerSpawnChance = 0.15;
    public static int demonSlayerGroupSizeMin = 2;
    public static int demonSlayerGroupSizeMax = 4;
    public static boolean enableKimetsuReplacer = true;
    public static boolean enableKimetsuMovieReplacer = false;
    public static final Map<String, Boolean> entityReplacerToggles = new HashMap<>();
    public static int mtFujikasaneDemonCenterX = 227;
    public static int mtFujikasaneDemonCenterY = 319;
    public static int mtFujikasaneDemonCenterZ = 70;
    public static int mtFujikasaneDemonRadius = 400;
    public static int mtFujikasaneKamabokoX = 311;
    public static int mtFujikasaneKamabokoY = 76;
    public static int mtFujikasaneKamabokoZ = 736;
    public static int mtFujikasaneKamabokoRadius = 20;

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event) {
        Log.debug("ENHANCED SPAWN CONFIG LOADING...");
        updateCache();
        Log.debug("Enhanced spawn config loaded successfully.");
    }

    @SubscribeEvent
    public static void onReload(final ModConfigEvent.Reloading event) {
        Log.debug("ENHANCED SPAWN CONFIG RELOADING...");
        updateCache();
        Log.debug("Enhanced spawn config reloaded successfully.");
    }

    private static void updateCache() {
        enhancedSpawningRules = ENHANCED_SPAWNING_RULES.get();
        replaceBaseGenericDemonSlayers = REPLACE_BASE_GENERIC_DEMON_SLAYERS.get();
        replaceBaseNezuko = REPLACE_BASE_NEZUKO.get();
        preventYorichiTypeZeroNaturalSpawns = PREVENT_YORICHI_TYPE_ZERO_NATURAL_SPAWNS.get();
        genericDemonSpawnRate = GENERIC_DEMON_SPAWN_RATE.get();
        genericDemonSlayerSpawnRate = GENERIC_DEMON_SLAYER_SPAWN_RATE.get();
        maxEntityCheckRadius = MAX_ENTITY_CHECK_RADIUS.get();
        enableProtectiveSpawning = ENABLE_PROTECTIVE_SPAWNING.get();
        hashiraSpawnChance = HASHIRA_SPAWN_CHANCE.get();
        kamabokoSpawnChance = KAMABOKO_SPAWN_CHANCE.get();
        demonSlayerSpawnChance = DEMON_SLAYER_SPAWN_CHANCE.get();
        demonSlayerGroupSizeMin = DEMON_SLAYER_GROUP_SIZE_MIN.get();
        demonSlayerGroupSizeMax = DEMON_SLAYER_GROUP_SIZE_MAX.get();
        enableKimetsuReplacer = ENABLE_KIMETSU_REPLACER.get();
        enableKimetsuMovieReplacer = ENABLE_KIMETSU_MOVIE_REPLACER.get();

        // Update entity replacer toggles
        for (Map.Entry<String, ForgeConfigSpec.BooleanValue> entry : ENTITY_REPLACER_TOGGLES.entrySet()) {
            entityReplacerToggles.put(entry.getKey(), entry.getValue().get());
        }

        // Update Mt Fujikasane dimension settings
        mtFujikasaneDemonCenterX = MT_FUJIKASANE_DEMON_CENTER_X.get();
        mtFujikasaneDemonCenterY = MT_FUJIKASANE_DEMON_CENTER_Y.get();
        mtFujikasaneDemonCenterZ = MT_FUJIKASANE_DEMON_CENTER_Z.get();
        mtFujikasaneDemonRadius = MT_FUJIKASANE_DEMON_RADIUS.get();
        mtFujikasaneKamabokoX = MT_FUJIKASANE_KAMABOKO_X.get();
        mtFujikasaneKamabokoY = MT_FUJIKASANE_KAMABOKO_Y.get();
        mtFujikasaneKamabokoZ = MT_FUJIKASANE_KAMABOKO_Z.get();
        mtFujikasaneKamabokoRadius = MT_FUJIKASANE_KAMABOKO_RADIUS.get();
    }

    /**
     * Check if a specific entity replacement is enabled
     */
    public static boolean isEntityReplacementEnabled(String entityName) {
        return entityReplacerToggles.getOrDefault(entityName, true);
    }
}
