package com.lerdorf.kimetsunoyaibamultiplayer.api;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;

/**
 * Registers all base KimetsunoYaiba mod styles and swords at initialization.
 * This enables the color change system and other features to work with base mod items.
 *
 * Style Hierarchy:
 * - sun_breathing (root)
 *   - water_breathing
 *     - flower_breathing
 *       - insect_breathing
 *     - serpent_breathing
 *   - flame_breathing
 *     - love_breathing
 *   - wind_breathing
 *     - mist_breathing
 *     - beast_breathing
 *   - thunder_breathing
 *     - sound_breathing
 *   - stone_breathing
 * - black (root, for black blades)
 *
 * Sword Levels:
 * - 0 = Base/generic swords (eligible for color change)
 * - 1 = Named character swords (Tanjiro, Inosuke)
 * - 2 = Hashira swords (Rengoku, Uzui, etc.)
 */
public class BaseModRegistration {

    private static boolean stylesRegistered = false;
    private static boolean swordsRegistered = false;

    /**
     * Register all base mod breathing style metadata.
     * Call this during commonSetup.
     */
    public static void registerAllStyles() {
        if (stylesRegistered) {
            Log.debug("Base mod styles already registered, skipping");
            return;
        }

        // Root styles
        StyleMetadataRegistry.register("sun_breathing", null, true, true);
        StyleMetadataRegistry.register("black", null, true, true);

        // Water breathing and derivatives
        StyleMetadataRegistry.register("water_breathing", "sun_breathing", true, true);
        StyleMetadataRegistry.register("flower_breathing", "water_breathing", true, true);
        StyleMetadataRegistry.register("insect_breathing", "flower_breathing", true, true);
        StyleMetadataRegistry.register("serpent_breathing", "water_breathing", true, true);

        // Flame breathing and derivatives
        StyleMetadataRegistry.register("flame_breathing", "sun_breathing", true, true);
        StyleMetadataRegistry.register("love_breathing", "flame_breathing", true, true);

        // Wind breathing and derivatives
        StyleMetadataRegistry.register("wind_breathing", "sun_breathing", true, true);
        StyleMetadataRegistry.register("mist_breathing", "wind_breathing", true, true);
        StyleMetadataRegistry.register("beast_breathing", "wind_breathing", true, true);

        // Thunder breathing and derivatives
        StyleMetadataRegistry.register("thunder_breathing", "sun_breathing", true, true);
        StyleMetadataRegistry.register("sound_breathing", "thunder_breathing", true, true);

        // Stone breathing (no derivatives)
        StyleMetadataRegistry.register("stone_breathing", "sun_breathing", true, true);

        stylesRegistered = true;
        Log.info("Registered {} base mod style metadata entries", StyleMetadataRegistry.getAllStyles().size());
    }

    /**
     * Register all base mod sword metadata.
     * Call this during commonSetup AFTER styles are registered.
     */
    public static void registerAllSwords() {
        if (swordsRegistered) {
            Log.debug("Base mod swords already registered, skipping");
            return;
        }

        // Level 0 - Base swords (eligible for color change)
        SwordMetadataRegistry.registerLazy("kimetsunoyaiba:nichirinsword_water", "water_breathing", 0);
        SwordMetadataRegistry.registerLazy("kimetsunoyaiba:nichirinsword_flame", "flame_breathing", 0);
        SwordMetadataRegistry.registerLazy("kimetsunoyaiba:nichirinsword_wind", "wind_breathing", 0);
        SwordMetadataRegistry.registerLazy("kimetsunoyaiba:nichirinsword_mist", "mist_breathing", 0);
        SwordMetadataRegistry.registerLazy("kimetsunoyaiba:nichirinsword_thunder", "thunder_breathing", 0);
        SwordMetadataRegistry.registerLazy("kimetsunoyaiba:nichirinsword_stone", "stone_breathing", 0);
        SwordMetadataRegistry.registerLazy("kimetsunoyaiba:nichirinsword_love", "love_breathing", 0);
        SwordMetadataRegistry.registerLazy("kimetsunoyaiba:nichirinsword_serpent", "serpent_breathing", 0);
        SwordMetadataRegistry.registerLazy("kimetsunoyaiba:nichirinsword_sound", "sound_breathing", 0);
        SwordMetadataRegistry.registerLazy("kimetsunoyaiba:nichirinsword_insect", "insect_breathing", 0);
        SwordMetadataRegistry.registerLazy("kimetsunoyaiba:nichirinsword_flower", "flower_breathing", 0);
        SwordMetadataRegistry.registerLazy("kimetsunoyaiba:nichirinsword_black", "black", 0);

        // Level 1 - Named character swords (NOT eligible for color change)
        SwordMetadataRegistry.registerLazy("kimetsunoyaiba:nichirinsword_tanjiro", "water_breathing", 1);
        SwordMetadataRegistry.registerLazy("kimetsunoyaiba:nichirinsword_inosuke", "beast_breathing", 1);

        // Level 2 - Hashira swords (NOT eligible for color change)
        SwordMetadataRegistry.registerLazy("kimetsunoyaiba:nichirinsword_rengoku", "flame_breathing", 2);
        SwordMetadataRegistry.registerLazy("kimetsunoyaiba:nichirinsword_uzui", "sound_breathing", 2);
        SwordMetadataRegistry.registerLazy("kimetsunoyaiba:nichirinsword_shinobu", "insect_breathing", 2);
        SwordMetadataRegistry.registerLazy("kimetsunoyaiba:nichirinsword_iguro", "serpent_breathing", 2);
        SwordMetadataRegistry.registerLazy("kimetsunoyaiba:nichirinsword_sanemi", "wind_breathing", 2);
        SwordMetadataRegistry.registerLazy("kimetsunoyaiba:nichirinsword_tokito", "mist_breathing", 2);
        SwordMetadataRegistry.registerLazy("kimetsunoyaiba:nichirinsword_kanroji", "love_breathing", 2);
        SwordMetadataRegistry.registerLazy("kimetsunoyaiba:nichirinsword_gyomei", "stone_breathing", 2);

        swordsRegistered = true;
        Log.info("Registered {} base mod sword metadata entries", SwordMetadataRegistry.getAllSwords().size());
    }

    /**
     * Register all base mod data (styles and swords).
     * Convenience method that calls both registration methods.
     */
    public static void registerAll() {
        registerAllStyles();
        registerAllSwords();
    }

    /**
     * Check if base mod styles have been registered.
     *
     * @return true if styles are registered
     */
    public static boolean areStylesRegistered() {
        return stylesRegistered;
    }

    /**
     * Check if base mod swords have been registered.
     *
     * @return true if swords are registered
     */
    public static boolean areSwordsRegistered() {
        return swordsRegistered;
    }
}
