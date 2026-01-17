package com.lerdorf.kimetsunoyaibamultiplayer.api;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registry for breathing style metadata including parent relationships and eligibility flags.
 * This is separate from BreathingStyleRegistry which stores the actual BreathingTechnique instances.
 *
 * This registry tracks:
 * - Parent-child relationships between styles (e.g., Mist derives from Wind)
 * - Color change eligibility (whether swords of this style can be obtained via color change)
 * - Ore selection eligibility (for future feature where players choose ores)
 */
public class StyleMetadataRegistry {

    private static final Map<String, StyleMetadata> STYLES = new ConcurrentHashMap<>();

    /**
     * Register a new breathing style's metadata.
     *
     * @param styleId Unique identifier for the style (e.g., "water_breathing")
     * @param parentStyleId Parent style ID, or null for root styles (e.g., sun_breathing)
     * @param colorChangeEligible Whether swords of this style can be obtained via color change
     * @param oreSelectionEligible Whether this style appears in ore selection (future feature)
     * @return The registered style metadata
     * @throws IllegalArgumentException if styleId is already registered
     */
    public static StyleMetadata register(
            String styleId,
            String parentStyleId,
            boolean colorChangeEligible,
            boolean oreSelectionEligible) {

        if (STYLES.containsKey(styleId)) {
            throw new IllegalArgumentException("Style already registered: " + styleId);
        }

        StyleMetadata metadata = new StyleMetadata(styleId, parentStyleId, colorChangeEligible, oreSelectionEligible);
        STYLES.put(styleId, metadata);

        return metadata;
    }

    /**
     * Get the metadata for a registered style.
     *
     * @param styleId The style identifier
     * @return The style metadata, or null if not found
     */
    public static StyleMetadata getMetadata(String styleId) {
        return STYLES.get(styleId);
    }

    /**
     * Check if a style is registered.
     *
     * @param styleId The style identifier
     * @return true if the style is registered
     */
    public static boolean isRegistered(String styleId) {
        return STYLES.containsKey(styleId);
    }

    /**
     * Get all styles that are eligible for color change transformation.
     *
     * @return List of color-change-eligible style metadata
     */
    public static List<StyleMetadata> getColorChangeEligibleStyles() {
        return STYLES.values().stream()
                .filter(StyleMetadata::isColorChangeEligible)
                .collect(Collectors.toList());
    }

    /**
     * Get all styles that are eligible for ore selection (future feature).
     *
     * @return List of ore-selection-eligible style metadata
     */
    public static List<StyleMetadata> getOreSelectionEligibleStyles() {
        return STYLES.values().stream()
                .filter(StyleMetadata::isOreSelectionEligible)
                .collect(Collectors.toList());
    }

    /**
     * Get all child styles of a given parent style.
     *
     * @param parentStyleId The parent style identifier
     * @return List of child style metadata
     */
    public static List<StyleMetadata> getChildStyles(String parentStyleId) {
        return STYLES.values().stream()
                .filter(m -> parentStyleId.equals(m.getParentStyleId()))
                .collect(Collectors.toList());
    }

    /**
     * Get all registered styles.
     *
     * @return Unmodifiable collection of all registered style metadata
     */
    public static Collection<StyleMetadata> getAllStyles() {
        return Collections.unmodifiableCollection(STYLES.values());
    }

    /**
     * Clear all registered styles. Used for testing.
     */
    public static void clear() {
        STYLES.clear();
    }

    /**
     * Represents metadata for a registered breathing style.
     */
    public static class StyleMetadata {
        private final String styleId;
        private final String parentStyleId;
        private final boolean colorChangeEligible;
        private final boolean oreSelectionEligible;

        private StyleMetadata(
                String styleId,
                String parentStyleId,
                boolean colorChangeEligible,
                boolean oreSelectionEligible) {
            this.styleId = styleId;
            this.parentStyleId = parentStyleId;
            this.colorChangeEligible = colorChangeEligible;
            this.oreSelectionEligible = oreSelectionEligible;
        }

        /**
         * Get the unique style identifier.
         *
         * @return The style ID
         */
        public String getStyleId() {
            return styleId;
        }

        /**
         * Get the parent style ID.
         *
         * @return The parent style ID, or null for root styles
         */
        public String getParentStyleId() {
            return parentStyleId;
        }

        /**
         * Check if this style is a root style (has no parent).
         *
         * @return true if this is a root style
         */
        public boolean isRootStyle() {
            return parentStyleId == null;
        }

        /**
         * Check if swords of this style can be obtained via color change.
         *
         * @return true if color-change eligible
         */
        public boolean isColorChangeEligible() {
            return colorChangeEligible;
        }

        /**
         * Check if this style appears in ore selection (future feature).
         *
         * @return true if ore-selection eligible
         */
        public boolean isOreSelectionEligible() {
            return oreSelectionEligible;
        }

        @Override
        public String toString() {
            return String.format("StyleMetadata{id=%s, parent=%s, colorChange=%s, oreSelect=%s}",
                styleId, parentStyleId, colorChangeEligible, oreSelectionEligible);
        }
    }
}
