package com.lerdorf.kimetsunoyaibamultiplayer;

import net.minecraft.world.level.GameRules;

/**
 * Registers custom game rules for the mod.
 *
 * Custom Game Rules:
 * - kimetsu_raids: Master switch for enabling/disabling raid system (default: true)
 */
public class ModGameRules {
    /**
     * Master switch for the raid system.
     * Controls demon raids (omen_of_muzan) and demon slayer raids (omen_of_ubuyashiki).
     * Default: true
     */
    public static GameRules.Key<GameRules.BooleanValue> RULE_KIMETSU_RAIDS;

    /**
     * Register custom game rules.
     * Called during mod initialization.
     */
    public static void register() {
        RULE_KIMETSU_RAIDS = GameRules.register(
            "kimetsu_raids",
            GameRules.Category.SPAWNING,
            GameRules.BooleanValue.create(true)
        );
    }

    /**
     * Check if raids are enabled for a given level.
     *
     * @param gameRules The game rules to check
     * @return true if raids are enabled
     */
    public static boolean areRaidsEnabled(GameRules gameRules) {
        if (RULE_KIMETSU_RAIDS == null) {
            // Fallback to config if gamerule not registered yet
            return com.lerdorf.kimetsunoyaibamultiplayer.config.RaidConfig.enableRaids.get();
        }
        return gameRules.getBoolean(RULE_KIMETSU_RAIDS);
    }

    /**
     * Placeholder for hardcore mode checking.
     * The kimetsu_hardcore gamerule is registered by the kimetsunoyaiba mod.
     * This will be properly implemented when we integrate with their gamerule system.
     *
     * @param gameRules The game rules to check
     * @return true if hardcore mode is enabled (currently always false)
     */
    public static boolean isHardcoreModeEnabled(GameRules gameRules) {
        // TODO: Implement proper kimetsu_hardcore gamerule checking
        // The kimetsunoyaiba mod registers this gamerule, but there's no
        // public API to access it by string name in Minecraft 1.20.1
        // We'll need to either:
        // 1. Get a reference to their gamerule key directly
        // 2. Use reflection to access it
        // 3. Ask the kimetsunoyaiba mod to expose an API
        return false;
    }
}
