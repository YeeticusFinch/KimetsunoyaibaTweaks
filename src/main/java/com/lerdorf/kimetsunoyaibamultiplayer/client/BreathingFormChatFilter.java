package com.lerdorf.kimetsunoyaibamultiplayer.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Filters chat messages related to ability cycling (breathing forms, blood demon arts, etc.).
 * Used to suppress ability cycle messages when the config option is enabled.
 *
 * This filter now detects ALL abilities that can be cycled with R/Shift+R,
 * including:
 * - Breathing techniques (Water Breathing, Thunder Breathing, etc.)
 * - Hinokami Kagura (Sun Breathing variant)
 * - Moon Breathing
 * - Blood Demon Arts
 * - Any other abilities added by the Kimetsunoyaiba mod or add-ons
 */
@OnlyIn(Dist.CLIENT)
public class BreathingFormChatFilter {

    // Ability type keywords - these indicate the TYPE of ability
    private static final String[] ABILITY_TYPE_KEYWORDS = {
        "Breathing", "breathing", "BREATHING",
        "の呼吸", // Japanese for "breathing"
        "Kagura", "kagura", // Hinokami Kagura
        "Blood Demon Art", "血鬼術", // Japanese for "Blood Demon Art"
        "Demon Art", "Kekkijutsu"
    };

    // Specific technique names that might not match the above patterns
    private static final String[] TECHNIQUE_NAMES = {
        "Hinokami", "Sun", "Moon", "Water", "Thunder", "Flame", "Wind",
        "Stone", "Mist", "Serpent", "Sound", "Ice", "Frost", "Flower",
        "Insect", "Love", "Beast", "Cherry", "Sakura",
        // Blood Demon Art names
        "Temari", "Obi", "Arrow", "Thread", "Drumming"
    };

    // Form/attack keywords that appear in ability names
    private static final String[] FORM_KEYWORDS = {
        "Form", "form", "FORM",
        "型", // Japanese for "form/kata"
        "Dance", "dance", // For Hinokami Kagura
        "Slash", "Strike", "Wheel", "Flash", "Spirit"
    };

    // Pattern indicators that this is an ability selection message
    private static final String[] MESSAGE_PATTERNS = {
        "選択", // Japanese for "selected/choice"
        "Selected",
        "Switched to",
        "First", "Second", "Third", "Fourth", "Fifth", "Sixth", "Seventh",
        "Eighth", "Ninth", "Tenth", "Eleventh", "Twelfth",
        "Thirteenth" // For Sun Breathing's 13th form
    };

    /**
     * Determines if a chat message should be suppressed as an ability cycle message.
     *
     * This now detects ALL abilities from the Kimetsunoyaiba mod that can be cycled with R/Shift+R:
     * - Breathing techniques (all variants)
     * - Hinokami Kagura / Sun Breathing
     * - Moon Breathing
     * - Blood Demon Arts
     * - Any other abilities that follow similar patterns
     *
     * @param message The chat message content
     * @return true if the message should be suppressed, false otherwise
     */
    public static boolean shouldSuppressMessage(String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }

        // Remove color codes for analysis
        String cleanMessage = stripColorCodes(message);

        // Quick length check - ability messages are typically short
        // Allow up to 120 characters to accommodate longer ability names
        if (cleanMessage.length() > 120) {
            return false; // Too long to be an ability message
        }

        // Check for ability type keywords (Breathing, Kagura, Blood Demon Art)
        boolean hasAbilityType = false;
        for (String keyword : ABILITY_TYPE_KEYWORDS) {
            if (cleanMessage.contains(keyword)) {
                hasAbilityType = true;
                break;
            }
        }

        // Check for specific technique names
        boolean hasTechniqueName = false;
        for (String technique : TECHNIQUE_NAMES) {
            if (cleanMessage.contains(technique)) {
                hasTechniqueName = true;
                break;
            }
        }

        // Check for form keywords
        boolean hasFormKeyword = false;
        for (String keyword : FORM_KEYWORDS) {
            if (cleanMessage.contains(keyword)) {
                hasFormKeyword = true;
                break;
            }
        }

        // Check for pattern indicators (First, Second, Selected, etc.)
        boolean hasPatternIndicator = false;
        for (String pattern : MESSAGE_PATTERNS) {
            if (cleanMessage.contains(pattern)) {
                hasPatternIndicator = true;
                break;
            }
        }

        // Suppress if message matches ability patterns:

        // Pattern 1: Ability type + form/pattern indicator
        // e.g., "Water Breathing - First Form: Water Surface Slash"
        if (hasAbilityType && (hasFormKeyword || hasPatternIndicator)) {
            return true;
        }

        // Pattern 2: Technique name + form/pattern indicator
        // e.g., "Hinokami - Dance" or "Moon - First Form"
        if (hasTechniqueName && (hasFormKeyword || hasPatternIndicator)) {
            return true;
        }

        // Pattern 3: Contains separator " - " and either form or pattern indicator
        // This catches most ability cycle messages
        if (cleanMessage.contains(" - ") && (hasFormKeyword || hasPatternIndicator)) {
            return true;
        }

        // Pattern 4: Contains separator " : " and either form or pattern indicator
        // Alternative format used by some abilities
        if (cleanMessage.contains(": ") && (hasFormKeyword || hasPatternIndicator)) {
            return true;
        }

        // Pattern 5: Japanese patterns (for Japanese localization)
        if (cleanMessage.contains("の呼吸") || cleanMessage.contains("型") || cleanMessage.contains("血鬼術")) {
            return true;
        }

        // Pattern 6: Short message with technique name and form keyword
        // This catches compact formats
        if (cleanMessage.length() < 80 && hasTechniqueName && hasFormKeyword) {
            return true;
        }

        return false;
    }

    /**
     * Removes Minecraft color codes (§x) from a message.
     *
     * @param message The message with color codes
     * @return The message without color codes
     */
    private static String stripColorCodes(String message) {
        return message.replaceAll("§.", "");
    }
}
