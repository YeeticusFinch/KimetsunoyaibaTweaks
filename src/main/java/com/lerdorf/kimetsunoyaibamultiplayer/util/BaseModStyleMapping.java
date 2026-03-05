package com.lerdorf.kimetsunoyaibamultiplayer.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Bidirectional mapping between base KimetsunoYaiba mod breathes ranges and our style IDs.
 *
 * The base mod stores breathing styles as ranges:
 * - Water: 100-199
 * - Beast: 200-299
 * - Thunder: 300-399
 * - Flame: 400-499
 * - Wind: 500-599
 * - Stone: 600-699
 * - Mist: 700-799
 * - Serpent: 800-899
 * - Sound: 900-999
 * - Ice: 1000-1099
 * - Moon: 1100-1199
 * - Sun: 1200-1299
 * - Flower: 1300-1399
 * - Insect: 1400-1499
 * - Love: 1500-1599
 * - Frost: 1600-1699
 *
 * Within each range, the form number is stored:
 * - Example: 720.0 = Mist Breathing (700) + Form 20
 */
public class BaseModStyleMapping {

    // Map: breathes range start → styleId
    // NOTE: Only includes base mod breathing styles (0-2000 range)
    // Addon breathing styles like Ice (4300), Frost (4200) are NOT included
    private static final Map<Integer, String> RANGE_TO_STYLE = Map.ofEntries(
        Map.entry(0, "bamboo_breathing"),
        Map.entry(100, "water_breathing"),
        Map.entry(200, "beast_breathing"),
        Map.entry(300, "thunder_breathing"),
        Map.entry(400, "flame_breathing"),
        Map.entry(500, "wind_breathing"),
        Map.entry(600, "stone_breathing"),
        Map.entry(700, "mist_breathing"),
        Map.entry(800, "serpent_breathing"),
        Map.entry(900, "sound_breathing"),
        Map.entry(1100, "moon_breathing"),
        Map.entry(1200, "sun_breathing"),       // Also "hinokami"
        Map.entry(1300, "flower_breathing"),
        Map.entry(1400, "insect_breathing"),
        Map.entry(1500, "love_breathing"),
        Map.entry(1800, "sakura_breathing")     // Sakura/Cherry Blossom
        // 1700 might be "bamboo" or "cherry_blossom" - can verify in-game
    );

    // Reverse mapping: styleId → breathes range start
    private static final Map<String, Integer> STYLE_TO_RANGE;
    // Forms per style (mirrors CycleBreathingFormPacket)
    private static final Map<Integer, int[]> STYLE_FORMS;

    static {
        // Build reverse mapping
        STYLE_TO_RANGE = new HashMap<>();
        for (Map.Entry<Integer, String> entry : RANGE_TO_STYLE.entrySet()) {
            STYLE_TO_RANGE.put(entry.getValue(), entry.getKey());
        }

        STYLE_FORMS = Map.ofEntries(
            Map.entry(100, new int[]{101, 102, 103, 104, 105, 106, 107, 111, 112, 113}), // Water
            Map.entry(200, new int[]{201, 202, 203, 204, 205, 206, 207, 208, 209, 210}), // Beast
            Map.entry(300, new int[]{301, 302, 303, 304, 305, 309}), // Thunder
            Map.entry(400, new int[]{401, 402, 403, 404, 405, 409}), // Flame
            Map.entry(500, new int[]{501, 502, 503, 504, 505, 506, 507, 508, 509}), // Wind
            Map.entry(600, new int[]{601, 602, 603, 604, 605, 606, 607, 608, 609, 611}), // Stone
            Map.entry(700, new int[]{701, 702, 703, 704, 705, 707}), // Mist
            Map.entry(800, new int[]{801, 802, 803, 804, 805}), // Serpent
            Map.entry(900, new int[]{901, 902, 903}), // Sound (Final Score is hashira-exclusive)
            Map.entry(1000, new int[]{1001, 1002, 1003, 1004, 1005, 1006, 1007}), // Ice
            Map.entry(1100, new int[]{1101, 1102, 1103, 1105, 1106, 1107, 1108, 1109, 1110, 1114, 1116}), // Moon
            Map.entry(1200, new int[]{1201, 1202, 1203, 1204, 1205, 1206, 1207, 1208, 1209, 1210, 1211, 1212}), // Sun
            Map.entry(1300, new int[]{1301, 1304, 1305}), // Flower
            Map.entry(1400, new int[]{1401, 1402, 1404, 1405}), // Insect
            Map.entry(1500, new int[]{1501, 1502, 1505, 1506}), // Love
            Map.entry(1600, new int[]{1601, 1602, 1603, 1604, 1605, 1606, 1607}) // Frost
        );
    }

    /**
     * Get style ID from base mod breathes value.
     *
     * @param breathes Base mod breathes value (e.g., 720.0)
     * @return Style ID (e.g., "mist_breathing") or null if not found
     */
    public static String getStyleId(double breathes) {
        if (breathes <= 0) {
            return null;
        }

        int range = ((int)(breathes / 100)) * 100;
        return RANGE_TO_STYLE.get(range);
    }

    /**
     * Get form index (0-based) from breathes value.
     *
     * The base mod uses 1-indexed form numbers (1, 2, 3, ...).
     * We convert to 0-indexed (0, 1, 2, ...) for our variation system.
     *
     * @param breathes Base mod breathes value (e.g., 720.0)
     * @return Form index (0-based, e.g., 19 for form 20), or -1 if invalid
     */
    public static int getFormIndex(double breathes) {
        if (breathes <= 0) {
            return -1;
        }

        int formNumber = (int)breathes % 100;

        if (formNumber <= 0) {
            return -1; // Invalid form number
        }

        return formNumber - 1; // Convert to 0-indexed
    }

    /**
     * Get breathes range start for a style ID.
     *
     * @param styleId Style ID (e.g., "mist_breathing")
     * @return Range start (e.g., 700) or 0 if not found
     */
    public static int getBreathesRange(String styleId) {
        return STYLE_TO_RANGE.getOrDefault(styleId, 0);
    }

    /**
     * Get the list of known form IDs for a breathing style.
     *
     * @param styleRange Breathing style range (e.g., 100, 200, 300)
     * @return Array of form IDs for the style, or empty array if unknown
     */
    public static int[] getFormsForStyle(int styleRange) {
        return STYLE_FORMS.getOrDefault(styleRange, new int[0]);
    }

    /**
     * Check if a form ID is a known base-mod form.
     *
     * @param formId Form ID to check
     * @return true if the ID is in the known base-mod form lists
     */
    public static boolean isKnownFormId(int formId) {
        if (formId <= 0) {
            return false;
        }
        int styleRange = (formId / 100) * 100;
        int[] forms = getFormsForStyle(styleRange);
        for (int f : forms) {
            if (f == formId) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the form ID from a breathes value.
     * For base mod forms, the breathes value IS the form ID.
     *
     * IMPORTANT: This is now simplified - breathes value directly converts to form ID.
     * No complex validation needed.
     *
     * @param breathes Base mod breathes value (e.g., 102.0 for Water Second Form)
     * @return Form ID (e.g., 102), or 0 if invalid
     */
    public static int getFormId(double breathes) {
        if (breathes <= 0 || breathes > 5000) {  // Sanity check only
            return 0;
        }

        // Simple cast - breathes value IS the form ID for base mod
        return (int) breathes;
    }

    /**
     * Check if a breathes value is valid.
     *
     * @param breathes Base mod breathes value
     * @return true if the breathes value maps to a known style
     */
    public static boolean isValid(double breathes) {
        return getStyleId(breathes) != null && getFormIndex(breathes) >= 0;
    }

    /**
     * Get the expected breathes range for a sword based on its item ID.
     * Used to detect when base mod writes form IDs with incorrect offsets.
     *
     * @param sword The sword ItemStack
     * @return Expected range (e.g., 100 for Water swords), or 0 if unknown
     */
    public static int getExpectedRangeForSword(net.minecraft.world.item.ItemStack sword) {
        String itemId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(sword.getItem()).toString();

        // Match base mod sword naming patterns
        if (itemId.contains("water")) return 100;
        if (itemId.contains("beast") || itemId.contains("inosuke")) return 200;
        if (itemId.contains("thunder") || itemId.contains("zenitsu")) return 300;
        if (itemId.contains("flame") || itemId.contains("rengoku")) return 400;
        if (itemId.contains("wind") || itemId.contains("sanemi")) return 500;
        if (itemId.contains("stone") || itemId.contains("gyomei")) return 600;
        if (itemId.contains("mist") || itemId.contains("muichiro") || itemId.contains("tokito")) return 700;
        if (itemId.contains("serpent") || itemId.contains("iguro")) return 800;
        if (itemId.contains("sound") || itemId.contains("uzui")) return 900;
        if (itemId.contains("ice")) return 1000;
        if (itemId.contains("moon") || itemId.contains("kokushibo")) return 1100;
        if (itemId.contains("sun") || itemId.contains("hinokami") || itemId.contains("yoriichi")) return 1200;
        if (itemId.contains("flower") || itemId.contains("kanae")) return 1300;
        if (itemId.contains("insect") || itemId.contains("shinobu")) return 1400;
        if (itemId.contains("love") || itemId.contains("kanroji") || itemId.contains("mitsuri")) return 1500;
        if (itemId.contains("frost")) return 1600;
        if (itemId.contains("sakura") || itemId.contains("cherry")) return 1800;

        return 0; // Unknown sword type
    }
}
