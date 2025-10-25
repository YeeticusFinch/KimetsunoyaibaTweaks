package com.lerdorf.kimetsunoyaibamultiplayer.raids;

/**
 * Defines power scaling tiers for demons and demon slayers.
 *
 * Used by the raid system to categorize entities and generate balanced waves.
 *
 * Demon scales range from EASY_DEMON to DEMON_KING.
 * Slayer scales range from GENERIC_SLAYER to SUPER_HASHIRA.
 */
public enum EntityPowerScale {
    // ========== DEMON POWER SCALES ==========

    /**
     * Easy demons - Weakest demon tier.
     * Examples: demon, demon_2, demon_3, spider_demon
     */
    EASY_DEMON,

    /**
     * Medium demons - Intermediate threat level.
     * Examples: demon_4, demon_5, demon_9, temple_demon, swamp_demon
     */
    MEDIUM_DEMON,

    /**
     * Hard demons / Easy boss demons - Significant threat.
     * Examples: demon_6, demon_7, demon_8, demon_10, hand_demon,
     * dice_steak_senior_demon, rui family members, susamaru, yahaba
     */
    HARD_DEMON,

    /**
     * Easy boss demons - Same as HARD_DEMON, maintained for clarity.
     * Examples: Same as HARD_DEMON
     */
    EASY_BOSS_DEMON,

    /**
     * Medium boss demons - Lower Twelve Kizuki.
     * Examples: kyogai, kamanue, rui, mukago, wakuraba, rokuro, hairo, enmu
     */
    MEDIUM_BOSS_DEMON,

    /**
     * Hard boss demons - Upper Twelve Kizuki.
     * Examples: daki, kaigaku, gyokko, hantengu, nakime, akaza, doma, kokushibo
     * Note: gyutaro excluded (spawns automatically with daki)
     */
    HARD_BOSS_DEMON,

    /**
     * Demon kings - Strongest demons.
     * Examples: muzan, tanjiro_demon (only if muzan killed)
     */
    DEMON_KING,

    // ========== DEMON SLAYER POWER SCALES ==========

    /**
     * Generic demon slayers - Most common slayer type.
     * Examples: demon_slayer, frost_slayer, ice_slayer, murata
     * Note: frost_slayer and ice_slayer are automatic replacements
     */
    GENERIC_SLAYER,

    /**
     * Named demon slayers - Mid-tier named characters.
     * Examples: genya, masachika, inosuke, tanjiro, kaigaku_human,
     * sabito, zennitsu, kanawo
     */
    NAMED_SLAYER,

    /**
     * Hard demon slayers - Elite non-hashira.
     * Examples: dice_steak_senior, dice_steak_senior_super
     */
    HARD_SLAYER,

    /**
     * Hashira - Top-tier demon slayers.
     * Examples: kocho, kanroji, kanae, shinazugawa, rengoku, iguro,
     * uzui, tomioka, muichirou, himejima
     */
    HASHIRA,

    /**
     * Super hashira - Legendary hashira.
     * Examples: michikatsu, yoriichi, yoriichi_old
     */
    SUPER_HASHIRA;

    /**
     * Check if this is a demon power scale.
     *
     * @return true if this is a demon scale
     */
    public boolean isDemonScale() {
        return this == EASY_DEMON || this == MEDIUM_DEMON || this == HARD_DEMON ||
               this == EASY_BOSS_DEMON || this == MEDIUM_BOSS_DEMON ||
               this == HARD_BOSS_DEMON || this == DEMON_KING;
    }

    /**
     * Check if this is a demon slayer power scale.
     *
     * @return true if this is a slayer scale
     */
    public boolean isSlayerScale() {
        return this == GENERIC_SLAYER || this == NAMED_SLAYER ||
               this == HARD_SLAYER || this == HASHIRA || this == SUPER_HASHIRA;
    }

    /**
     * Check if this is a boss-tier scale (for either demons or slayers).
     *
     * @return true if this is a boss-tier scale
     */
    public boolean isBossTier() {
        return this == EASY_BOSS_DEMON || this == MEDIUM_BOSS_DEMON ||
               this == HARD_BOSS_DEMON || this == DEMON_KING ||
               this == HARD_SLAYER || this == HASHIRA || this == SUPER_HASHIRA;
    }

    /**
     * Get the display name for this power scale.
     *
     * @return Display name
     */
    public String getDisplayName() {
        switch (this) {
            case EASY_DEMON: return "Easy Demon";
            case MEDIUM_DEMON: return "Medium Demon";
            case HARD_DEMON: return "Hard Demon";
            case EASY_BOSS_DEMON: return "Easy Boss Demon";
            case MEDIUM_BOSS_DEMON: return "Lower Kizuki";
            case HARD_BOSS_DEMON: return "Upper Kizuki";
            case DEMON_KING: return "Demon King";
            case GENERIC_SLAYER: return "Demon Slayer";
            case NAMED_SLAYER: return "Named Slayer";
            case HARD_SLAYER: return "Elite Slayer";
            case HASHIRA: return "Hashira";
            case SUPER_HASHIRA: return "Legendary Hashira";
            default: return name();
        }
    }
}
