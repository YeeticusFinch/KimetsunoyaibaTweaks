package com.lerdorf.kimetsunoyaibamultiplayer.util;

/**
 * Utility class for encoding and decoding variation information in breathes values.
 *
 * Encoding scheme: encodedBreathes = (formId * 10000) + variationIndex
 *
 * Examples:
 * - Base form (Water 2nd): breathes = 102
 * - Variation 1: breathes = 1020001 (102 * 10000 + 1)
 * - Variation 2: breathes = 1020002 (102 * 10000 + 2)
 *
 * This allows encoding both form ID and variation index in a single value,
 * eliminating the need for separate variation tracking.
 */
public class VariationEncoder {
    private static final int ENCODING_MULTIPLIER = 10000;
    // Treat values below this as plain form IDs (or indices); only decode when at/above 1,000,000
    private static final int ENCODED_THRESHOLD = 1_000_000;

    /**
     * Encode a form ID and variation index into a single breathes value.
     *
     * @param formId The form ID (e.g., 102 for Water 2nd Form)
     * @param variationIndex The variation index (0 = base form, 1+ = variations)
     * @return The encoded breathes value
     */
    public static double encode(int formId, int variationIndex) {
        if (formId <= 0) {
            throw new IllegalArgumentException("Form ID must be positive, got: " + formId);
        }
        if (variationIndex < 0 || variationIndex >= ENCODING_MULTIPLIER) {
            throw new IllegalArgumentException("Variation index must be between 0 and " + (ENCODING_MULTIPLIER - 1) + ", got: " + variationIndex);
        }

        if (variationIndex == 0) {
            // Base form - no encoding needed
            return formId;
        }

        return (formId * ENCODING_MULTIPLIER) + variationIndex;
    }

    /**
     * Decode a breathes value to extract the form ID.
     *
     * @param breathes The breathes value (encoded or plain)
     * @return The form ID
     */
    public static int getFormId(double breathes) {
        if (breathes <= 0) {
            return 0;
        }

        // If below encoded threshold, it's a plain form value (no variation)
        if (breathes < ENCODED_THRESHOLD) {
            return (int) breathes;
        }

        // Integer division to extract form ID
        return (int) breathes / ENCODING_MULTIPLIER;
    }

    /**
     * Decode a breathes value to extract the variation index.
     *
     * @param breathes The breathes value (encoded or plain)
     * @return The variation index (0 = base form, 1+ = variations)
     */
    public static int getVariationIndex(double breathes) {
        if (breathes <= 0) {
            return 0;
        }

        // No variation info encoded below the threshold
        if (breathes < ENCODED_THRESHOLD) {
            return 0;
        }

        // Modulo to extract variation index
        return (int) breathes % ENCODING_MULTIPLIER;
    }

    /**
     * Check if a breathes value is encoded (contains variation information).
     *
     * @param breathes The breathes value to check
     * @return true if the value is encoded (>= 10000), false otherwise
     */
    public static boolean isEncoded(double breathes) {
        return breathes >= ENCODED_THRESHOLD;
    }

    /**
     * Get the base form (without variation) from a breathes value.
     * If the value is encoded, this extracts just the form ID.
     *
     * @param breathes The breathes value (encoded or plain)
     * @return The base form breathes value (form ID only, no variation)
     */
    public static double getBaseForm(double breathes) {
        if (breathes <= 0) {
            return 0;
        }

        if (isEncoded(breathes)) {
            return (int) breathes / ENCODING_MULTIPLIER;
        }

        return breathes;
    }
}
