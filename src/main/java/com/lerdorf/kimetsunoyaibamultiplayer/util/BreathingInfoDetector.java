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

        if (heldSword.getItem() instanceof com.lerdorf.kimetsunoyaibamultiplayer.items.BloodDemonArtItem artItem) {
            return getBloodDemonArtInfo(heldSword, artItem);
        }
        if (heldSword.getItem() instanceof com.lerdorf.kimetsunoyaibamultiplayer.items.BloodDemonArtAxeItem artItem) {
            return getBloodDemonArtInfo(heldSword, artItem);
        }

        // Check if this is our mod's breathing sword
        if (heldSword.getItem() instanceof com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem breathingSword) {
            return getOurModBreathingInfo(player, heldSword, breathingSword);
        }

        // For kimetsunoyaiba mod swords: Try to get display text from cache first
        if (player.level().isClientSide()) {
            String cachedText = com.lerdorf.kimetsunoyaibamultiplayer.client.BreathingFormTracker.getCachedDisplayText(
                player.getUUID(), heldSword);
            // If no sword-specific cache, try form-specific cache using current breathes
            if (cachedText == null) {
                double currentBreathes = player.getPersistentData().getDouble("breathes");
                int formIdForCache = com.lerdorf.kimetsunoyaibamultiplayer.util.VariationEncoder.getFormId(currentBreathes);
                cachedText = com.lerdorf.kimetsunoyaibamultiplayer.client.BreathingFormTracker
                    .getDisplayTextForForm(player.getUUID(), formIdForCache);

                if (com.lerdorf.kimetsunoyaibamultiplayer.Config.logDebug) {
                    com.lerdorf.kimetsunoyaibamultiplayer.Log.debug("[BreathingInfoDetector] No sword-specific cache, using form-specific cache. " +
                                                                     "currentBreathes: " + currentBreathes +
                                                                     ", formIdForCache: " + formIdForCache +
                                                                     ", cachedText: " + cachedText);
                }
            } else if (com.lerdorf.kimetsunoyaibamultiplayer.Config.logDebug) {
                com.lerdorf.kimetsunoyaibamultiplayer.Log.debug("[BreathingInfoDetector] Using sword-specific cached text: " + cachedText);
            }

            if (cachedText != null) {
                // Parse the cached display text to create BreathingInfo
                // Keep the original text WITH color codes for display
                BreathingInfo info = parseDisplayText(cachedText);

                // Use the best-known breathes value (synced from server) for variation decoding
                double cachedBreathes = com.lerdorf.kimetsunoyaibamultiplayer.client.BreathingFormTracker
                    .getCachedForm(player.getUUID(), heldSword);
                double breathesValue = cachedBreathes > 0.0
                    ? cachedBreathes
                    : player.getPersistentData().getDouble("breathes");

                if (com.lerdorf.kimetsunoyaibamultiplayer.Config.logDebug) {
                    com.lerdorf.kimetsunoyaibamultiplayer.Log.debug("[BreathingInfoDetector] cachedBreathes: " + cachedBreathes +
                                                                     ", player NBT breathes: " + player.getPersistentData().getDouble("breathes") +
                                                                     ", using breathesValue: " + breathesValue);
                }

                int formIdFromBreathes = com.lerdorf.kimetsunoyaibamultiplayer.util.VariationEncoder.getFormId(breathesValue);
                String baseFormText = com.lerdorf.kimetsunoyaibamultiplayer.client.BreathingFormTracker
                    .getDisplayTextForForm(player.getUUID(), formIdFromBreathes);

                if (info != null && breathesValue > 0) {
                    int formId = formIdFromBreathes;
                    com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData.PlayerData pdata =
                        com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData.getOrCreate(player);
                    int variationIndex = pdata.getCurrentVariationIndex();

                    if (formId > 0) {
                        // Get sword ID for filtering variations
                        String swordId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(heldSword.getItem()).toString();
                        int totalVariations = com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.VariationRegistry
                            .getVariationCount(formId, swordId);
                        String variationName = null;
                        if (variationIndex > 0) {
                            var variation = com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.VariationRegistry
                                .getVariation(formId, variationIndex, swordId);
                            if (variation != null) {
                                variationName = variation.getName();
                            }
                        }

                        // Base form: reuse cached colored text so we don't show "Form 102"
                        if (variationIndex == 0) {
                            String colored = baseFormText != null ? baseFormText : info.originalColoredText;
                            // Ensure we show the real base form name (not "Form 102")
                            String displayFormName = info.formName;
                            return new BreathingInfo(
                                info.styleName,
                                displayFormName,
                                info.formNumber,
                                info.styleRange,
                                breathesValue,
                                colored,
                                variationIndex,
                                totalVariations,
                                null
                            );
                        }

                        // Variations: rebuild colored display using base form's color
                        String sourceText = baseFormText != null ? baseFormText : info.originalColoredText;
                        String stylePart = extractStylePart(sourceText);
                        String formColor = extractLeadingColor(sourceText);
                        String displayFormName = variationName != null ? variationName : info.formName;
                        String rebuiltColored = null;
                        if (stylePart != null && formColor != null) {
                            rebuiltColored = stylePart + " - " + formColor + displayFormName;
                        }

                        return new BreathingInfo(
                            info.styleName,
                            displayFormName,
                            info.formNumber,
                            info.styleRange,
                            breathesValue,
                            rebuiltColored != null ? rebuiltColored : info.originalColoredText,
                            variationIndex,
                            totalVariations,
                            variationName
                        );
                    }
                }

                return info;
            }
        }

        // Fallback: Use NBT-based detection (for when cache doesn't have it yet)
        return getBreathingInfoFromNBT(player, heldSword);
    }

    private static BreathingInfo getBloodDemonArtInfo(ItemStack stack,
                                                      com.lerdorf.kimetsunoyaibamultiplayer.items.BloodDemonArtItem artItem) {
        String display = artItem.getDisplayText(stack);
        if (display == null || display.isEmpty()) {
            return null;
        }

        String[] parts = display.split(": ", 2);
        String styleName = parts.length > 0 ? parts[0] : display;
        String formName = parts.length > 1 ? parts[1] : display;
        return new BreathingInfo(
            styleName,
            formName,
            artItem.getSelectedFormIndex(stack) + 1,
            0,
            0.0,
            "§2" + display,
            0,
            0,
            null
        );
    }

    private static BreathingInfo getBloodDemonArtInfo(ItemStack stack,
                                                      com.lerdorf.kimetsunoyaibamultiplayer.items.BloodDemonArtAxeItem artItem) {
        String display = artItem.getDisplayText(stack);
        if (display == null || display.isEmpty()) {
            return null;
        }

        String[] parts = display.split(": ", 2);
        String styleName = parts.length > 0 ? parts[0] : display;
        String formName = parts.length > 1 ? parts[1] : display;
        return new BreathingInfo(
            styleName,
            formName,
            artItem.getSelectedFormIndex(stack) + 1,
            0,
            0.0,
            "§2" + display,
            0,
            0,
            null
        );
    }

    /**
     * Parses breathing form information from kimetsunoyaiba mod's chat message format.
     * Expected formats:
     * - "Water Breathing 2nd Form: Water Wheel" (common format)
     * - "Water Breathing - First Form: Water Surface Slash" (alternate format)
     * Preserves the original colored text for display.
     */
    public static BreathingInfo parseDisplayText(String displayText) {
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

        // Extract form number - check BOTH styleName and formName since the format varies
        // e.g., "Water Breathing 2nd Form" or "First Form: Water Surface Slash"
        int formNumber = extractFormNumber(styleName);
        if (formNumber == 1 && !styleName.contains("First") && !styleName.contains("1st")) {
            // Didn't find form number in styleName, try formName
            formNumber = extractFormNumber(formName);
        }

        // Estimate style range from style name
        int styleRange = getStyleRangeFromName(styleName);

        // Create pseudo breathes value
        double pseudoBreathes = styleRange + formNumber;

        // IMPORTANT: Look up variation data for base mod swords
        // Extract form ID and check for registered variations
        int formId = com.lerdorf.kimetsunoyaibamultiplayer.util.BaseModStyleMapping.getFormId(pseudoBreathes);
        int variationIndex = 0;
        int totalVariations = 0;
        String variationName = null;

        if (formId > 0) {
            // Get total variations for this form
            totalVariations = com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.VariationRegistry
                .getVariationCount(formId, null);

            // Try to get player's current variation index
            // Note: This requires player context, which we don't have here
            // The variation index will be retrieved separately by the overlay
            // For now, we just include the total count so the UI can show "(1/N)"
        }

        // Pass the original colored text to preserve exact formatting from chat
        return new BreathingInfo(styleName, formName, formNumber, styleRange, pseudoBreathes,
                                displayText, variationIndex, totalVariations, variationName);
    }

    /**
     * Extracts form number from form name string.
     * Handles formats like "First", "Second", "2nd", "3rd", etc.
     */
    private static int extractFormNumber(String formName) {
        // Handle written-out numbers
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
        if (formName.contains("Thirteenth")) return 13;

        // Handle numeric formats: "2nd", "3rd", "4th", etc.
        // Split by spaces and look for numbers or ordinal suffixes
        String[] words = formName.split(" ");
        for (String word : words) {
            // Remove common suffixes (st, nd, rd, th)
            String numStr = word.replaceAll("(?i)(st|nd|rd|th)", "");
            try {
                int num = Integer.parseInt(numStr);
                if (num > 0 && num <= 20) { // Sanity check
                    return num;
                }
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

        // Fallback to hardcoded mappings for base mod breathing styles (0-2000 range only)
        // NOTE: Ice (4300) and Frost (4200) are from ADDONS, not the base mod
        if (styleName.contains("Water")) return 100;
        if (styleName.contains("Beast")) return 200;
        if (styleName.contains("Thunder")) return 300;
        if (styleName.contains("Flame")) return 400;
        if (styleName.contains("Wind")) return 500;
        if (styleName.contains("Stone")) return 600;
        if (styleName.contains("Mist")) return 700;
        if (styleName.contains("Serpent")) return 800;
        if (styleName.contains("Sound")) return 900;
        if (styleName.contains("Moon")) return 1100;
        if (styleName.contains("Sun") || styleName.contains("Hinokami")) return 1200;
        if (styleName.contains("Flower")) return 1300;
        if (styleName.contains("Insect")) return 1400;
        if (styleName.contains("Love")) return 1500;
        if (styleName.contains("Cherry")) return 1700;  // Cherry Blossom or Bamboo
        if (styleName.contains("Sakura")) return 1800;
        // Ice and Frost breathing are from addons (4200+, 4300+) - not handled here
        return 0;
    }

    /**
     * Fallback method: Gets breathing info from NBT data.
     * This is only used when cache is empty (e.g., first time holding a sword before cycling).
     * Returns a simple generic message until player cycles forms and chat message populates the cache.
     */
    private static BreathingInfo getBreathingInfoFromNBT(Player player, ItemStack heldSword) {
        // Use authoritative NBT breathes (server-synced) to avoid stale cached values
        double playerBreathes = player.getPersistentData().getDouble("breathes");

        if (com.lerdorf.kimetsunoyaibamultiplayer.Config.logDebug) {
            com.lerdorf.kimetsunoyaibamultiplayer.Log.debug("[BreathingInfoDetector] getBreathingInfoFromNBT - playerBreathes from NBT: " + playerBreathes);
        }

        // Check if sword modifies the breathing style (multi-style swords)
        double selectOffset = 0.0;

        if (heldSword != null && !heldSword.isEmpty()) {
            CompoundTag tag = heldSword.getTag();
            if (tag != null && tag.contains("select")) {
                selectOffset = tag.getDouble("select");
                if (com.lerdorf.kimetsunoyaibamultiplayer.Config.logDebug) {
                    com.lerdorf.kimetsunoyaibamultiplayer.Log.debug("[BreathingInfoDetector] Sword has selectOffset: " + selectOffset);
                }
            }
        }

        // Calculate actual breathes value (with selectOffset for multi-style swords)
        // This is only used for determining style/form, NOT for display
        double actualBreathes = playerBreathes + selectOffset;

        // If no breathes value, return null (no form selected yet)
        if (actualBreathes == 0.0) {
            return null;
        }

        // Simple extraction: style and form number (using actualBreathes with selectOffset)
        int breathingStyle = (int)(actualBreathes / 100) * 100;
        int formNumber = (int)actualBreathes % 100;

        // CRITICAL FIX: Use playerBreathes (not actualBreathes) for form ID extraction
        // The breathes NBT value should already be the base form ID (without selectOffset)
        // SelectOffset is only used by the base mod for multi-style sword display, not for our system
        int formId = com.lerdorf.kimetsunoyaibamultiplayer.util.VariationEncoder.getFormId(playerBreathes);

        if (com.lerdorf.kimetsunoyaibamultiplayer.Config.logDebug) {
            com.lerdorf.kimetsunoyaibamultiplayer.Log.debug("[BreathingInfoDetector] actualBreathes: " + actualBreathes +
                                                             ", formId: " + formId +
                                                             ", breathingStyle: " + breathingStyle +
                                                             ", formNumber: " + formNumber);
        }

        com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData.PlayerData pdata =
            com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData.getOrCreate(player.getUUID());
        int variationIndex = pdata.getCurrentVariationIndex();

        String variationName = null;
        int totalVariations = 0;

        if (formId > 0) {
            // Get sword ID for filtering variations
            String swordId = heldSword != null && !heldSword.isEmpty()
                ? net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(heldSword.getItem()).toString()
                : null;
            totalVariations = com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.VariationRegistry
                .getVariationCount(formId, swordId);

            if (variationIndex > 0) {
                com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingFormVariation variation =
                    com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.VariationRegistry
                        .getVariation(formId, variationIndex, swordId);
                if (variation != null) {
                    variationName = variation.getName();
                }
            }
        }

        // Use simple generic names - chat messages will provide the real names
        String styleName = "";
        String formName = variationName != null ? variationName : "";

        // CRITICAL FIX: Return playerBreathes (base form ID) as fullBreathesValue, NOT actualBreathes
        // actualBreathes (with selectOffset) should only be used for internal base mod compatibility
        return new BreathingInfo(styleName, formName, formNumber, breathingStyle, playerBreathes,
                                null, variationIndex, totalVariations, variationName);
    }

    /**
     * Gets breathing information for our mod's breathing swords.
     */
    private static BreathingInfo getOurModBreathingInfo(Player player, ItemStack heldSword,
                                                        com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem breathingSword) {
        // Get the breathing technique from the sword
        com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique technique;
        if (breathingSword instanceof com.lerdorf.kimetsunoyaibamultiplayer.items.NichirinSwordBlack blackSword) {
            technique = blackSword.getEffectiveTechnique(heldSword, player);
        } else {
            technique = breathingSword.getBreathingTechnique();
        }
        if (technique == null) {
            return null;
        }

        // Get player's breathing data (this will load from NBT if on server)
        com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData.PlayerData data =
            com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData.getOrCreate(player);

        int baseFormIndex = data.getCurrentFormIndex();
        int variationIndex = data.getCurrentVariationIndex();

        // Get form object (use base form index)
        com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingForm form = technique.getForm(baseFormIndex);

        if (form == null) {
            return null;
        }

        // Determine variation from synced breathes value (if it matches this form)
        double cachedBreathes = com.lerdorf.kimetsunoyaibamultiplayer.client.BreathingFormTracker
            .getCachedForm(player.getUUID(), player.getMainHandItem());
        double breathesValue = cachedBreathes > 0.0 ? cachedBreathes : player.getPersistentData().getDouble("breathes");
        if (com.lerdorf.kimetsunoyaibamultiplayer.util.VariationEncoder.getFormId(breathesValue) == form.getFormId()) {
            variationIndex = com.lerdorf.kimetsunoyaibamultiplayer.util.VariationEncoder.getVariationIndex(breathesValue);
        }

        // Get registered sword info for variation lookup
        com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.RegisteredSword registeredSword =
            com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.getSword(breathingSword);
        String swordId = registeredSword != null ? registeredSword.getSwordId() : breathingSword.toString();

        // Get form ID for variation lookup
        int formId = form.getFormId();

        // Check for active variation
        com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingFormVariation variation = null;
        String variationName = null;
        String displayFormName = form.getName(); // Default to base form name

        if (variationIndex > 0) {
            variation = com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.VariationRegistry.getVariation(
                formId, variationIndex, swordId);
            if (variation != null) {
                variationName = variation.getName();
                displayFormName = variationName; // Replace form name with variation name
            }
        }

        // Get total variation count for this form using form ID
        int totalVariations = com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.VariationRegistry.getVariationCount(
            formId, swordId);

        // Extract names
        String techniqueName = technique.getName();

        // Create a pseudo breathes value for consistency (technique type * 100 + form number)
        // Our forms are 0-indexed, so add 1 to make them 1-indexed like kimetsunoyaiba mod
        int formNumber = baseFormIndex + 1;
        int styleRange = getTechniqueTypeNumber(techniqueName) * 100;
        double displayBreathes = breathesValue > 0.0 ? breathesValue : (styleRange + formNumber);

        // Construct colored display text using technique's colors
        String techniqueColor = technique.getTechniqueColor();
        String formColor = technique.getFormColor();
        String coloredDisplayText;
        if (startsWithTechniqueName(displayFormName, techniqueName)) {
            // Form already includes style name (e.g., "Thunder Breathing 1st Form..."), avoid duplicating it.
            coloredDisplayText = formColor + displayFormName;
        } else {
            coloredDisplayText = techniqueColor + techniqueName + " - " + formColor + displayFormName;
        }

        return new BreathingInfo(techniqueName, displayFormName, formNumber, styleRange, displayBreathes,
                                coloredDisplayText, variationIndex, totalVariations, variationName);
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

    private static boolean startsWithTechniqueName(String formName, String techniqueName) {
        if (formName == null || techniqueName == null) {
            return false;
        }
        String normalizedForm = formName.toLowerCase().trim();
        String normalizedTechnique = techniqueName.toLowerCase().trim();
        return !normalizedTechnique.isEmpty() && normalizedForm.startsWith(normalizedTechnique);
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

        // Fallback to hardcoded mappings for base mod breathing styles only (0-2000 range)
        // NOTE: Ice and Frost are from addons, not the base mod
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
            case 1100 -> "Moon Breathing";
            case 1200 -> "Sun Breathing";
            case 1300 -> "Flower Breathing";
            case 1400 -> "Insect Breathing";
            case 1500 -> "Love Breathing";
            case 1700 -> "Cherry Blossom Breathing";  // Or Bamboo
            case 1800 -> "Sakura Breathing";
            // Ice (4300) and Frost (4200) are addon breathing styles, not base mod
            default -> "";
        };
    }

    /**
     * Checks if an item is a nichirin sword (from this mod, base mod, or any addon).
     */
    public static boolean isNichirinSword(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return false;
        }

        // Check if it's our mod's breathing sword
        if (item.getItem() instanceof com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem) {
            return true;
        }

        // Check if it's registered in SwordRegistry (addon support)
        if (com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.getSword(item.getItem()) != null) {
            return true;
        }

        String itemId = item.getItem().toString().toLowerCase();

        // Check for kimetsunoyaiba mod swords
        if (itemId.contains("nichirin") || itemId.contains("nitirintou") || itemId.contains("sword_kokushibo")) {
            return true;
        }

        // Check for our mod's breathing swords (fallback string check)
        if (itemId.contains("breathingsword") || itemId.contains("nichirinsword")) {
            return true;
        }

        return false;
    }

    /**
     * Extract the style part (prefix before " - ") from a colored chat string.
     */
    private static String extractStylePart(String coloredText) {
        if (coloredText == null) return null;
        int idx = coloredText.indexOf(" - ");
        if (idx <= 0) return null;
        return coloredText.substring(0, idx);
    }

    /**
     * Extract the first color code (two chars) from the text (e.g., §b).
     */
    private static String extractLeadingColor(String coloredText) {
        if (coloredText == null) return null;
        int idx = coloredText.indexOf('§');
        if (idx >= 0 && idx + 1 < coloredText.length()) {
            return coloredText.substring(idx, idx + 2);
        }
        return null;
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

        // Variation data (for our mod's breathing swords)
        public final int currentVariationIndex; // 0 = base form, 1+ = variations
        public final int totalVariations;       // Total number of variations available for this form
        public final String variationName;      // Name of active variation (null if base form)

        public BreathingInfo(String styleName, String formName, int formNumber, int styleRange, double fullBreathesValue) {
            this(styleName, formName, formNumber, styleRange, fullBreathesValue, null, 0, 0, null);
        }

        public BreathingInfo(String styleName, String formName, int formNumber, int styleRange, double fullBreathesValue, String originalColoredText) {
            this(styleName, formName, formNumber, styleRange, fullBreathesValue, originalColoredText, 0, 0, null);
        }

        public BreathingInfo(String styleName, String formName, int formNumber, int styleRange, double fullBreathesValue,
                           String originalColoredText, int currentVariationIndex, int totalVariations, String variationName) {
            this.styleName = styleName;
            this.formName = formName;
            this.formNumber = formNumber;
            this.styleRange = styleRange;
            this.fullBreathesValue = fullBreathesValue;
            this.originalColoredText = originalColoredText;
            this.currentVariationIndex = currentVariationIndex;
            this.totalVariations = totalVariations;
            this.variationName = variationName;
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
