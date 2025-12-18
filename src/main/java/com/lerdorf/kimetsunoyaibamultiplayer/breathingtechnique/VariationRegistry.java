package com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.config.VariationConfig;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Registry for breathing form variations.
 *
 * Variations are indexed by form ID. Each breathing form has a unique ID:
 * - Base mod forms: Use breathes value as ID (101-1699)
 *   - Water: 101, 102, 103, 104, 105, 106, 107, 111, 112, 113
 *   - Flame: 401, 402, 403, 404, 405, 409
 *   - Mist: 701, 702, 703, 704, 705, 707
 *   - Love: 1501, 1502, 1505, 1506
 * - Our mod forms: 20000+
 *   - Mist: 20001-20007
 *   - Love: 22001-22006
 *
 * Multiple mods can register variations for the same form without conflict.
 * Variation indices are auto-assigned based on registration order.
 *
 * Thread-safe for concurrent registration during mod initialization.
 */
public class VariationRegistry {

    // Map: formId -> List<BreathingFormVariation>
    private static final Map<Integer, List<BreathingFormVariation>> VARIATIONS
        = new ConcurrentHashMap<>();

    /**
     * Register a variation for a specific breathing form.
     * The variation index is auto-assigned based on registration order.
     *
     * Example:
     *   VariationRegistry.register(102, lateralWheelVariation); // Water Second Form
     *   VariationRegistry.register(102, rollingWheelVariation); // Another variation for same form
     *
     * @param formId The form ID (e.g., 102 for Water Second Form, 20001 for Mist First Form)
     * @param variation The variation to register
     */
    public static void register(int formId, BreathingFormVariation variation) {
        List<BreathingFormVariation> variations = VARIATIONS
            .computeIfAbsent(formId, k -> new CopyOnWriteArrayList<>());

        variations.add(variation);

        int variationIndex = variations.size(); // 1-based (1 = first variation, 2 = second, etc.)

        Log.info("Registered variation '" + variation.getName() + "' for form ID " + formId +
                 " (variation index: " + variationIndex + ")");
    }

    /**
     * Get all variations for a specific form.
     * Filters by sword compatibility if swordId is provided.
     *
     * @param formId The form ID
     * @param swordId Optional sword ID to filter by (can be null)
     * @return List of applicable variations (empty if none)
     */
    public static List<BreathingFormVariation> getVariations(int formId, String swordId) {
        List<BreathingFormVariation> allVariations = VARIATIONS.get(formId);
        if (allVariations == null) {
            return Collections.emptyList();
        }

        // Filter by sword compatibility
        if (swordId == null) {
            return Collections.unmodifiableList(allVariations);
        }

        return allVariations.stream()
            .filter(v -> v.appliesTo(swordId))
            .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Get a specific variation by index.
     *
     * @param formId The form ID
     * @param variationIndex The variation index (0 = base form, 1+ = variations)
     * @param swordId The sword ID to filter by
     * @return The variation, or null if not found or not applicable
     */
    public static BreathingFormVariation getVariation(
        int formId,
        int variationIndex,
        String swordId
    ) {
        if (variationIndex == 0) {
            return null; // 0 = base form, no variation
        }

        List<BreathingFormVariation> variations = getVariations(formId, swordId);

        int index = variationIndex - 1; // Convert to 0-based index
        if (index >= 0 && index < variations.size()) {
            return variations.get(index);
        }

        return null;
    }

    /**
     * Check if variations exist for a form.
     *
     * @param formId The form ID
     * @param swordId Optional sword ID to filter by
     * @return true if variations exist
     */
    public static boolean hasVariations(int formId, String swordId) {
        return !getVariations(formId, swordId).isEmpty();
    }

    /**
     * Get the total count of variations for a form.
     * This is used for UI display ("1/4") and cycling logic.
     *
     * @param formId The form ID
     * @param swordId Optional sword ID to filter by
     * @return Count of variations (does not include base form)
     */
    public static int getVariationCount(int formId, String swordId) {
        return getVariations(formId, swordId).size();
    }

    /**
     * Get a variation using an encoded breathes value.
     * Decodes the breathes value to extract formId and variationIndex.
     *
     * @param encodedBreathes The encoded breathes value (e.g., 1020001 for Water 2nd Form, Variation 1)
     * @param swordId Optional sword ID to filter by
     * @return The variation, or null if not found
     */
    public static BreathingFormVariation getVariation(double encodedBreathes, String swordId) {
        int formId = com.lerdorf.kimetsunoyaibamultiplayer.util.VariationEncoder.getFormId(encodedBreathes);
        int variationIndex = com.lerdorf.kimetsunoyaibamultiplayer.util.VariationEncoder.getVariationIndex(encodedBreathes);
        return getVariation(formId, variationIndex, swordId);
    }

    /**
     * Get variation count using an encoded breathes value.
     * Decodes the breathes value to extract the formId.
     *
     * @param encodedBreathes The encoded breathes value
     * @param swordId Optional sword ID to filter by
     * @return Count of variations (does not include base form)
     */
    public static int getVariationCount(double encodedBreathes, String swordId) {
        int formId = com.lerdorf.kimetsunoyaibamultiplayer.util.VariationEncoder.getFormId(encodedBreathes);
        return getVariationCount(formId, swordId);
    }

    /**
     * Check if variations exist using an encoded breathes value.
     * Decodes the breathes value to extract the formId.
     *
     * @param encodedBreathes The encoded breathes value
     * @param swordId Optional sword ID to filter by
     * @return true if variations exist
     */
    public static boolean hasVariations(double encodedBreathes, String swordId) {
        int formId = com.lerdorf.kimetsunoyaibamultiplayer.util.VariationEncoder.getFormId(encodedBreathes);
        return hasVariations(formId, swordId);
    }

    /**
     * Clear all registered variations. Used for testing.
     */
    public static void clear() {
        VARIATIONS.clear();
    }
}
