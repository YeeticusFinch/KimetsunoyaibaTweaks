package com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique;

import com.lerdorf.kimetsunoyaibamultiplayer.BaseKnYForms;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.config.VariationConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Registry for breathing form variations.
 *
 * Variations are indexed by form ID OR form name. Each breathing form has a unique ID:
 * - Base mod forms: Use breathes value as ID (101-1699) OR form name from BaseKnYForms
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

    // Map: formId -> List<BreathingFormVariation> (legacy, kept for backward compatibility)
    private static final Map<Integer, List<BreathingFormVariation>> VARIATIONS
        = new ConcurrentHashMap<>();

    // Map: formName -> List<BreathingFormVariation> (new string-based system)
    private static final Map<String, List<BreathingFormVariation>> VARIATIONS_BY_NAME
        = new ConcurrentHashMap<>();

    // Set of all registered variation names (for detecting variation messages in chat)
    private static final Set<String> ALL_VARIATION_NAMES = Collections.synchronizedSet(new HashSet<>());

    /**
     * Register a variation for a specific breathing form using form name.
     *
     * @param baseFormName The base form name (e.g., "Water Breathing 2nd Form")
     * @param variation The variation to register
     */
    public static void register(String baseFormName, BreathingFormVariation variation) {
        List<BreathingFormVariation> variations = VARIATIONS_BY_NAME
            .computeIfAbsent(baseFormName, k -> new CopyOnWriteArrayList<>());

        variations.add(variation);

        // Track this variation name for chat message detection
        ALL_VARIATION_NAMES.add(variation.getName());

        int variationIndex = variations.size(); // 1-based (1 = first variation, 2 = second, etc.)

        Log.info("Registered variation '" + variation.getName() + "' for form name '" + baseFormName +
                 "' (variation index: " + variationIndex + ")");
    }

    /**
     * Register a variation for a specific breathing form using form ID.
     * This automatically registers by both ID (for backward compatibility) and name (for new system).
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
        // Legacy integer registration (backward compatibility)
        List<BreathingFormVariation> variations = VARIATIONS
            .computeIfAbsent(formId, k -> new CopyOnWriteArrayList<>());

        variations.add(variation);

        // Track this variation name for chat message detection
        ALL_VARIATION_NAMES.add(variation.getName());

        int variationIndex = variations.size(); // 1-based (1 = first variation, 2 = second, etc.)

        Log.info("Registered variation '" + variation.getName() + "' for form ID " + formId +
                 " (variation index: " + variationIndex + ")");

        // ALSO register by name if form exists in BaseKnYForms
        BaseKnYForms.BaseForm form = BaseKnYForms.forms.get(formId);
        if (form != null) {
            register(form.name, variation);
        } else {
            // For custom swords (>= 20000), create synthetic key
            if (formId >= 20000) {
                register("custom_form_" + formId, variation);
            } else {
                Log.warn("Unknown base mod form ID " + formId +
                         " - variation will not be accessible by name");
            }
        }
    }

    /**
     * Check if a message contains a registered variation name.
     * Used to detect variation messages in chat and prevent them from being cached.
     *
     * @param message The chat message to check (with or without color codes)
     * @return true if the message contains any registered variation name
     */
    public static boolean containsVariationName(String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }

        // Strip color codes for matching
        String cleanMessage = message.replaceAll("§.", "");

        // Check if message contains any registered variation name
        for (String variationName : ALL_VARIATION_NAMES) {
            if (cleanMessage.contains(variationName)) {
                return true;
            }
        }

        return false;
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
        //System.out.println("[VariationRegistry] getVariations: formId=" + formId + ", swordId=" + swordId +
        //                  ", allVariations=" + (allVariations != null ? allVariations.size() : "null"));

        if (allVariations == null) {
            //System.out.println("[VariationRegistry] No variations registered for form ID " + formId);
            return Collections.emptyList();
        }

        // Debug: print all variations found
        /*
        for (int i = 0; i < allVariations.size(); i++) {
            BreathingFormVariation v = allVariations.get(i);
            System.out.println("[VariationRegistry]   Variation " + (i+1) + ": name='" + v.getName() +
                              "', applicableSwords=" + v.getApplicableSwords() +
                              ", appliesTo(" + swordId + ")=" + v.appliesTo(swordId));
        }*/

        // Filter by sword compatibility
        if (swordId == null) {
            //System.out.println("[VariationRegistry] No sword ID filter, returning all " + allVariations.size() + " variations");
            return Collections.unmodifiableList(allVariations);
        }

        List<BreathingFormVariation> filtered = allVariations.stream()
            .filter(v -> v.appliesTo(swordId))
            .collect(Collectors.toUnmodifiableList());
        //System.out.println("[VariationRegistry] After filtering by sword ID '" + swordId + "': " + filtered.size() + " variations");

        return filtered;
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
     * Get all variations for forms matching a substring.
     * This is used for base mod swords where we don't have a reliable form ID.
     *
     * @param currentFormName The current form name to search (e.g., from chat display)
     * @param swordId Optional sword ID to filter by (can be null)
     * @return List of applicable variations (empty if none)
     */
    public static List<BreathingFormVariation> getVariationsBySubstring(
        String currentFormName,
        String swordId
    ) {
        if (currentFormName == null || currentFormName.isEmpty()) {
            return Collections.emptyList();
        }

        List<BreathingFormVariation> matches = new ArrayList<>();

        //System.out.println("[VariationRegistry] getVariationsBySubstring: currentFormName='" + currentFormName + "', swordId='" + swordId + "'");

        for (Map.Entry<String, List<BreathingFormVariation>> entry : VARIATIONS_BY_NAME.entrySet()) {
            String baseFormName = entry.getKey();

            // Exact substring match
            if (currentFormName.contains(baseFormName)) {
                //System.out.println("[VariationRegistry]   Found matching base form name: '" + baseFormName + "'");
                for (BreathingFormVariation v : entry.getValue()) {
                    boolean applies = swordId == null || v.appliesTo(swordId);
                    //System.out.println("[VariationRegistry]     Variation: '" + v.getName() +
                    //                 "', applicableSwords=" + v.getApplicableSwords() +
                    //                 ", applies=" + applies);
                    if (applies) {
                        matches.add(v);
                    }
                }
            }
        }

        //System.out.println("[VariationRegistry]   Total matches: " + matches.size());
        return Collections.unmodifiableList(matches);
    }

    /**
     * Get variation count using substring matching.
     *
     * @param currentFormName The current form name to search
     * @param swordId Optional sword ID to filter by
     * @return Count of variations (does not include base form)
     */
    public static int getVariationCountBySubstring(String currentFormName, String swordId) {
        return getVariationsBySubstring(currentFormName, swordId).size();
    }

    /**
     * Get a specific variation by index using substring matching.
     *
     * @param currentFormName The current form name to search
     * @param variationIndex The variation index (0 = base form, 1+ = variations)
     * @param swordId The sword ID to filter by
     * @return The variation, or null if not found or not applicable
     */
    public static BreathingFormVariation getVariationBySubstring(
        String currentFormName,
        int variationIndex,
        String swordId
    ) {
        if (variationIndex == 0) {
            return null; // 0 = base form, no variation
        }

        List<BreathingFormVariation> variations = getVariationsBySubstring(currentFormName, swordId);

        int index = variationIndex - 1; // Convert to 0-based index
        if (index >= 0 && index < variations.size()) {
            return variations.get(index);
        }

        return null;
    }

    /**
     * Check if variations exist using substring matching.
     *
     * @param currentFormName The current form name to search
     * @param swordId Optional sword ID to filter by
     * @return true if variations exist
     */
    public static boolean hasVariationsBySubstring(String currentFormName, String swordId) {
        return !getVariationsBySubstring(currentFormName, swordId).isEmpty();
    }

    /**
     * Clear all registered variations. Used for testing.
     */
    public static void clear() {
        VARIATIONS.clear();
        VARIATIONS_BY_NAME.clear();
    }
}
