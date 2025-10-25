package com.lerdorf.kimetsunoyaibamultiplayer.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Utility class for detecting current breathing technique and form information.
 * Works with both single-style and multi-style nichirin swords.
 */
public class BreathingInfoDetector {

    /**
     * Detects breathing information for the player's currently held sword.
     *
     * @param player The player holding the sword
     * @param heldSword The sword item in the player's main hand
     * @return BreathingInfo object containing style and form details, or null if no breathing form is active
     */
    public static BreathingInfo getBreathingInfo(Player player, ItemStack heldSword) {
        if (player == null) {
            return null;
        }

        // Check if this is our mod's breathing sword
        if (heldSword.getItem() instanceof com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem breathingSword) {
            return getOurModBreathingInfo(player, breathingSword);
        }

        // For kimetsunoyaiba mod swords: Try to get display text from cache first
        if (player.level().isClientSide()) {
            String cachedText = com.lerdorf.kimetsunoyaibamultiplayer.client.BreathingFormTracker.getCachedDisplayText(
                player.getUUID(), heldSword);

            if (cachedText != null) {
                // Parse the cached display text to create BreathingInfo
                // Keep the original text WITH color codes for display
                return parseDisplayText(cachedText);
            }
        }

        // Fallback: Use NBT-based detection (for when cache doesn't have it yet)
        return getBreathingInfoFromNBT(player, heldSword);
    }

    /**
     * Parses breathing form information from kimetsunoyaiba mod's chat message format.
     * Expected format: "§6Water Breathing §7- §bFirst Form: Water Surface Slash" or similar
     * Preserves the original colored text for display.
     */
    private static BreathingInfo parseDisplayText(String displayText) {
        if (displayText == null || displayText.isEmpty()) {
            return null;
        }

        // Remove color codes for easier parsing
        String cleanText = displayText.replaceAll("§.", "");

        // Split by " - " to separate technique name and form name
        String[] parts = cleanText.split(" - ");
        if (parts.length != 2) {
            // Try alternate separators
            parts = cleanText.split(":");
            if (parts.length < 2) {
                return null;
            }
        }

        String styleName = parts[0].trim();
        String formName = parts.length > 1 ? parts[1].trim() : "";

        // Extract form number from the form name (e.g., "First Form" -> 1)
        int formNumber = extractFormNumber(formName);

        // Estimate style range from style name
        int styleRange = getStyleRangeFromName(styleName);

        // Create pseudo breathes value
        double pseudoBreathes = styleRange + formNumber;

        // Pass the original colored text to preserve exact formatting from chat
        return new BreathingInfo(styleName, formName, formNumber, styleRange, pseudoBreathes, displayText);
    }

    /**
     * Extracts form number from form name string.
     */
    private static int extractFormNumber(String formName) {
        if (formName.contains("First")) return 1;
        if (formName.contains("Second")) return 2;
        if (formName.contains("Third")) return 3;
        if (formName.contains("Fourth")) return 4;
        if (formName.contains("Fifth")) return 5;
        if (formName.contains("Sixth")) return 6;
        if (formName.contains("Seventh")) return 7;
        if (formName.contains("Eighth")) return 8;
        if (formName.contains("Ninth")) return 9;
        if (formName.contains("Tenth")) return 10;
        if (formName.contains("Eleventh")) return 11;
        if (formName.contains("Twelfth")) return 12;

        // Try to extract number from "Form X" pattern
        String[] words = formName.split(" ");
        for (String word : words) {
            try {
                return Integer.parseInt(word);
            } catch (NumberFormatException e) {
                // Continue checking other words
            }
        }

        return 1; // Default to first form
    }

    /**
     * Gets style range number from style name.
     * First checks the BreathingStyleRegistry for dynamically registered styles,
     * then falls back to hardcoded mappings for kimetsunoyaiba mod styles.
     */
    private static int getStyleRangeFromName(String styleName) {
        // First, check the registry for dynamically registered styles
        int registeredRange = com.lerdorf.kimetsunoyaibamultiplayer.api.BreathingStyleRegistry
            .getStyleRangeByName(styleName);
        if (registeredRange != 0) {
            return registeredRange;
        }

        // Fallback to hardcoded mappings for kimetsunoyaiba mod styles
        if (styleName.contains("Water")) return 100;
        if (styleName.contains("Beast")) return 200;
        if (styleName.contains("Thunder")) return 300;
        if (styleName.contains("Flame")) return 400;
        if (styleName.contains("Wind")) return 500;
        if (styleName.contains("Stone")) return 600;
        if (styleName.contains("Mist")) return 700;
        if (styleName.contains("Serpent")) return 800;
        if (styleName.contains("Sound")) return 900;
        if (styleName.contains("Ice")) return 1000;
        if (styleName.contains("Moon")) return 1100;
        if (styleName.contains("Sun") || styleName.contains("Hinokami")) return 1200;
        if (styleName.contains("Flower")) return 1300;
        if (styleName.contains("Insect")) return 1400;
        if (styleName.contains("Love")) return 1500;
        if (styleName.contains("Frost")) return 1600;
        if (styleName.contains("Cherry")) return 1700;
        if (styleName.contains("Sakura")) return 1800;
        return 0;
    }

    /**
     * Fallback method: Gets breathing info from NBT data.
     * This is only used when cache is empty (e.g., first time holding a sword before cycling).
     * Returns a simple generic message until player cycles forms and chat message populates the cache.
     */
    private static BreathingInfo getBreathingInfoFromNBT(Player player, ItemStack heldSword) {
        // Get player's base breathes value from NBT (for kimetsunoyaiba mod swords)
        double playerBreathes = player.getPersistentData().getDouble("breathes");

        // Check if sword modifies the breathing style (multi-style swords)
        double selectOffset = 0.0;

        if (heldSword != null && !heldSword.isEmpty()) {
            CompoundTag tag = heldSword.getOrCreateTag();

            if (tag.contains("select")) {
                selectOffset = tag.getDouble("select");
            }
        }

        // Calculate actual breathes value
        double actualBreathes = playerBreathes + selectOffset;

        // If no breathes value, return null (no form selected yet)
        if (actualBreathes == 0.0) {
            return null;
        }

        // Simple extraction: style and form number
        int breathingStyle = (int)(actualBreathes / 100) * 100;
        int formNumber = (int)actualBreathes % 100;

        // Use simple generic names - chat messages will provide the real names
        String styleName = "";
        String formName = "";

        return new BreathingInfo(styleName, formName, formNumber, breathingStyle, actualBreathes);
    }

    /**
     * Gets breathing information for our mod's breathing swords.
     */
    private static BreathingInfo getOurModBreathingInfo(Player player, com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem breathingSword) {
        // Get the breathing technique from the sword
        com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique technique = breathingSword.getBreathingTechnique();
        if (technique == null) {
            return null;
        }

        // Get player's breathing data
        com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData.PlayerData data =
            com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData.getOrCreate(player.getUUID());

        // Get current form index and form object
        int formIndex = data.getCurrentFormIndex();
        com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingForm form = technique.getForm(formIndex);

        if (form == null) {
            return null;
        }

        // Extract names
        String techniqueName = technique.getName();
        String formName = form.getName();

        // Create a pseudo breathes value for consistency (technique type * 100 + form number)
        // Our forms are 0-indexed, so add 1 to make them 1-indexed like kimetsunoyaiba mod
        int formNumber = formIndex + 1;
        int styleRange = getTechniqueTypeNumber(techniqueName) * 100;
        double pseudoBreathes = styleRange + formNumber;

        return new BreathingInfo(techniqueName, formName, formNumber, styleRange, pseudoBreathes);
    }

    /**
     * Maps our mod's technique names to number ranges (matching kimetsunoyaiba mod's numbering system).
     * First checks the BreathingStyleRegistry, then falls back to hardcoded values.
     */
    private static int getTechniqueTypeNumber(String techniqueName) {
        // First, check the registry
        int registeredRange = com.lerdorf.kimetsunoyaibamultiplayer.api.BreathingStyleRegistry
            .getStyleRangeByName(techniqueName);
        if (registeredRange != 0) {
            return registeredRange / 100; // Convert range to type number
        }

        // Fallback to hardcoded mappings
        if (techniqueName.contains("Ice")) return 10;
        if (techniqueName.contains("Frost")) return 16;
        if (techniqueName.contains("Cherry Blossom")) return 17;
        if (techniqueName.contains("Sakura")) return 18;
        // Add more as techniques are added
        return 0;
    }

    /**
     * Gets the breathing style name from the style number.
     * Used only as fallback when cache is empty.
     * First checks the BreathingStyleRegistry, then falls back to hardcoded mappings.
     */
    private static String getBreathingStyleName(int breathingStyle) {
        // First, check the registry
        var registeredStyle = com.lerdorf.kimetsunoyaibamultiplayer.api.BreathingStyleRegistry
            .getStyleByRange(breathingStyle);
        if (registeredStyle != null) {
            return registeredStyle.getStyleName();
        }

        // Fallback to hardcoded mappings
        return switch (breathingStyle) {
            case 100 -> "Water Breathing";
            case 200 -> "Beast Breathing";
            case 300 -> "Thunder Breathing";
            case 400 -> "Flame Breathing";
            case 500 -> "Wind Breathing";
            case 600 -> "Stone Breathing";
            case 700 -> "Mist Breathing";
            case 800 -> "Serpent Breathing";
            case 900 -> "Sound Breathing";
            case 1000 -> "Ice Breathing";
            case 1100 -> "Moon Breathing";
            case 1200 -> "Sun Breathing";
            case 1300 -> "Flower Breathing";
            case 1400 -> "Insect Breathing";
            case 1500 -> "Love Breathing";
            case 1600 -> "Frost Breathing";
            case 1700 -> "Cherry Blossom Breathing";
            case 1800 -> "Sakura Breathing";
            default -> "";
        };
    }

    /**
     * Checks if an item is a nichirin sword (from either mod).
     */
    public static boolean isNichirinSword(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return false;
        }

        // Check if it's our mod's breathing sword
        if (item.getItem() instanceof com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem) {
            return true;
        }

        String itemId = item.getItem().toString().toLowerCase();

        // Check for kimetsunoyaiba mod swords
        if (itemId.contains("nichirin") || itemId.contains("nitirintou")) {
            return true;
        }

        // Check for our mod's breathing swords (fallback string check)
        if (itemId.contains("breathingsword") || itemId.contains("nichirinsword")) {
            return true;
        }

        return false;
    }

    /**
     * Data class containing breathing technique information.
     */
    public static class BreathingInfo {
        public final String styleName;
        public final String formName;
        public final int formNumber;
        public final int styleRange;
        public final double fullBreathesValue;
        public final String originalColoredText; // Original text from chat with color codes preserved

        public BreathingInfo(String styleName, String formName, int formNumber, int styleRange, double fullBreathesValue) {
            this(styleName, formName, formNumber, styleRange, fullBreathesValue, null);
        }

        public BreathingInfo(String styleName, String formName, int formNumber, int styleRange, double fullBreathesValue, String originalColoredText) {
            this.styleName = styleName;
            this.formName = formName;
            this.formNumber = formNumber;
            this.styleRange = styleRange;
            this.fullBreathesValue = fullBreathesValue;
            this.originalColoredText = originalColoredText;
        }

        /**
         * Returns formatted string matching the chat display.
         * If we have the original text from chat, use that directly.
         * Otherwise, construct it with plain text to match kimetsunoyaiba mod's format.
         */
        public String getColoredDisplay() {
            if (originalColoredText != null && !originalColoredText.isEmpty()) {
                return originalColoredText; // Use exact text from chat
            }
            return styleName + " - " + formName; // Plain text fallback
        }

        @Override
        public String toString() {
            return styleName + " - " + formName;
        }
    }
}
