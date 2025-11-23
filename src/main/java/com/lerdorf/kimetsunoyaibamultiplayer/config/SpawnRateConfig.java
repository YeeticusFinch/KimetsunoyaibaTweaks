package com.lerdorf.kimetsunoyaibamultiplayer.config;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.HashMap;
import java.util.Map;

public class SpawnRateConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    static {
        BUILDER.comment("Spawn Priority Configuration",
                        "Priority values control natural spawn chances for kimetsunoyaiba mobs:",
                        "  0 = Never spawns naturally",
                        "  50 = 50% chance to spawn when selected",
                        "  100 = Always spawns when selected (default)",
                        "  >100 = Treated as 100",
                        "Note: Does NOT affect structure spawns, spawn eggs, or commands")
                .push("spawn_priority");
    }

    // Map to hold spawn priority configurations for different entities
    private static final Map<String, ForgeConfigSpec.DoubleValue> SPAWN_RATES = new HashMap<>();

    // Default spawn priorities for Kimetsunoyaiba mobs
    static {
        // Define default spawn priorities for all Kimetsunoyaiba mobs
        Map<String, Double> defaultSpawnRates = new HashMap<>();
        defaultSpawnRates.put("kimetsunoyaiba:akaza", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:boar", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:boar_golden", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:daki", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:demon2", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:demon3", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:demon4", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:demon5", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:demon6", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:demon7", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:demon8", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:demon9", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:demon10", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:demon", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:dice_steak_senior", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:dice_steak_senior_demon", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:dice_steak_senior_golden", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:dice_steak_senior_super", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:doctor", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:doma", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:enko", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:enmu", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:genya", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:goldfishbig", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:grandmother", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:gyokko", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:gyutaro", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:haganeduka", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:hairo", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:hakuji", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:hand_demon", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:hantengu", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:hasu", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:himejima", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:honoikaduchi", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:iguro", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:inosuke", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:kaigaku", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:kaigaku_human", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:kakushi", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:kamanue", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:kanae", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:kanawo", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:kanroji", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:kasugai_crow", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:kocho", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:kokushibo", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:kokushirinten", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:kotetsu", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:kuwajima", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:kyogai", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:makomo", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:masachika", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:michikatsu", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:mob_slayer", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:muichirou", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:mukago", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:murata", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:muscular_mouse", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:muzan", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:nakime", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:nezuko", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:pandan", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:rengoku", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:rokuro", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:rui_brother", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:rui", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:rui_father", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:rui_human", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:rui_mother", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:rui_sister", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:sabito", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:school_inosuke", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:school_kocho", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:school_nezuko", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:school_tanjiro", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:school_tomioka", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:school_zenitsu", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:shinazugawa", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:spider_demon", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:suigokubati", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:suirenbosatsu", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:susamaru", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:swamp_demon", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:tamayo", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:tanjiro_demon", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:tanjiro", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:temple_demon", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:tomioka", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:toyosan", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:ubuyashiki", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:urokodaki", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:uzui", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:wakuraba", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:yachan_brother", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:yachan", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:yahaba", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:yorichi_0", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:yoriichi", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:yoriichi_old", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:yushiro", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:zennitsu", 100.0);
        defaultSpawnRates.put("kimetsunoyaiba:zohakuten", 100.0);

        // Add all entities to the config
        for (Map.Entry<String, Double> entry : defaultSpawnRates.entrySet()) {
            String entityName = entry.getKey();
            Double defaultValue = entry.getValue();
            
            SPAWN_RATES.put(entityName, BUILDER
                .comment("Spawn priority for " + entityName + " (0 = never, 50 = 50% chance, 100 = always)")
                .defineInRange(entityName.replace(":", "_") + "_priority", defaultValue, 0.0, 200.0));
        }
    }

    static {
        BUILDER.pop(); // spawn_priority
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    // Runtime cache of spawn rates
    public static final Map<String, Double> SPAWN_RATE_CACHE = new HashMap<>();

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event) {
        Log.debug("SPAWN RATE CONFIG LOADING...");
        // Update the cache when config is loaded
        updateCache();
        Log.debug("Spawn rate config loaded successfully.");
    }

    @SubscribeEvent
    public static void onReload(final ModConfigEvent.Reloading event) {
        Log.debug("SPAWN RATE CONFIG RELOADING...");
        // Update the cache when config is reloaded
        updateCache();
        Log.debug("Spawn rate config reloaded successfully.");
    }

    private static void updateCache() {
        for (Map.Entry<String, ForgeConfigSpec.DoubleValue> entry : SPAWN_RATES.entrySet()) {
            SPAWN_RATE_CACHE.put(entry.getKey(), entry.getValue().get());
        }
    }

    /**
     * Get the spawn priority for an entity.
     *
     * @param entityName The name of the entity (e.g., "kimetsunoyaiba:akaza")
     * @return The spawn priority (0 = never spawns, 50 = 50% chance, 100 = always spawns)
     */
    public static double getSpawnRate(String entityName) {
        Double rate = SPAWN_RATE_CACHE.get(entityName);
        return rate != null ? rate : 100.0; // Default to 100.0 (always spawn) if not configured
    }
}