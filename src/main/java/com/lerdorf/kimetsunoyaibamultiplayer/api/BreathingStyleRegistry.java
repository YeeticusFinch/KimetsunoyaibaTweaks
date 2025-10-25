package com.lerdorf.kimetsunoyaibamultiplayer.api;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for breathing styles and techniques.
 * This allows other mods to register their own breathing techniques
 * and have them integrate seamlessly with the KnY Multiplayer systems.
 *
 * Thread-safe implementation for use during mod initialization.
 */
public class BreathingStyleRegistry {

    private static final Map<String, RegisteredBreathingStyle> STYLES = new ConcurrentHashMap<>();
    private static final Map<String, Integer> STYLE_NAME_TO_RANGE = new ConcurrentHashMap<>();

    /**
     * Register a breathing style with the system.
     *
     * @param styleId Unique identifier for this style (e.g., "frost_breathing", "ice_breathing")
     * @param styleName Display name of the style (e.g., "Frost Breathing", "Ice Breathing")
     * @param technique The BreathingTechnique containing all forms
     * @param styleRange The numeric range for this style (e.g., 1000 for Ice, 1600 for Frost).
     *                   Should be unique and in increments of 100.
     * @param defaultParticle The default particle to use for sword swings
     * @return The registered style object
     * @throws IllegalArgumentException if styleId is already registered
     */
    public static RegisteredBreathingStyle register(
            String styleId,
            String styleName,
            BreathingTechnique technique,
            int styleRange,
            ParticleOptions defaultParticle,
            Map<String, String> replaceAnimations) {

        if (STYLES.containsKey(styleId)) {
            throw new IllegalArgumentException("Breathing style already registered: " + styleId);
        }

        if (styleRange % 100 != 0) {
            throw new IllegalArgumentException("Style range must be a multiple of 100, got: " + styleRange);
        }

        RegisteredBreathingStyle style = new RegisteredBreathingStyle(
            styleId, styleName, technique, styleRange, defaultParticle, replaceAnimations
        );

        STYLES.put(styleId, style);
        STYLE_NAME_TO_RANGE.put(styleName, styleRange);

        return style;
    }

    /**
     * Get a registered breathing style by its ID.
     *
     * @param styleId The unique identifier of the style
     * @return The registered style, or null if not found
     */
    public static RegisteredBreathingStyle getStyle(String styleId) {
        return STYLES.get(styleId);
    }

    /**
     * Get all registered breathing styles.
     *
     * @return Unmodifiable collection of all registered styles
     */
    public static Collection<RegisteredBreathingStyle> getAllStyles() {
        return Collections.unmodifiableCollection(STYLES.values());
    }

    /**
     * Get the style range number for a given style name.
     * This is used by BreathingInfoDetector to map style names to numeric ranges.
     *
     * @param styleName The display name of the style
     * @return The style range, or 0 if not found
     */
    public static int getStyleRangeByName(String styleName) {
        return STYLE_NAME_TO_RANGE.getOrDefault(styleName, 0);
    }

    /**
     * Check if a style is registered.
     *
     * @param styleId The unique identifier of the style
     * @return true if the style is registered
     */
    public static boolean isRegistered(String styleId) {
        return STYLES.containsKey(styleId);
    }

    /**
     * Get a registered style by its style range number.
     *
     * @param styleRange The numeric range (e.g., 1000, 1600)
     * @return The registered style, or null if not found
     */
    public static RegisteredBreathingStyle getStyleByRange(int styleRange) {
        return STYLES.values().stream()
            .filter(style -> style.getStyleRange() == styleRange)
            .findFirst()
            .orElse(null);
    }

    /**
     * Clear all registered styles. Used for testing.
     */
    public static void clear() {
        STYLES.clear();
        STYLE_NAME_TO_RANGE.clear();
    }

    /**
     * Represents a registered breathing style with all its metadata.
     */
    public static class RegisteredBreathingStyle {
        private final String styleId;
        private final String styleName;
        private final BreathingTechnique technique;
        private final int styleRange;
        private final ParticleOptions defaultParticle;
        private final Map<String, String> replaceAnimations;

        private RegisteredBreathingStyle(
                String styleId,
                String styleName,
                BreathingTechnique technique,
                int styleRange,
                ParticleOptions defaultParticle,
                Map<String, String> replaceAnimations) {
            this.styleId = styleId;
            this.styleName = styleName;
            this.technique = technique;
            this.styleRange = styleRange;
            this.defaultParticle = defaultParticle;
            this.replaceAnimations = replaceAnimations;
        }
        
        public Map<String, String> getReplaceAnimations() {
        	return replaceAnimations;
        }
        
        public String getAnim(String ogAnim) {
        	if (replaceAnimations.containsKey(ogAnim)) {
        		return replaceAnimations.get(ogAnim);
        	}
        	if (ogAnim.contains(":")) {
        		String noNameSpace = ogAnim.substring(ogAnim.indexOf(ogAnim.indexOf(':'))+1);
        		if (replaceAnimations.containsKey(noNameSpace)) {
            		return replaceAnimations.get(noNameSpace);
            	}
        	} else {
        		String withNameSpace = "kimetsunoyaiba:" + ogAnim;
        		if (replaceAnimations.containsKey(withNameSpace)) {
            		return replaceAnimations.get(withNameSpace);
            	}
        	}
        	
        	
        	return ogAnim;
        }

        public String getStyleId() {
            return styleId;
        }

        public String getStyleName() {
            return styleName;
        }

        public BreathingTechnique getTechnique() {
            return technique;
        }

        public int getStyleRange() {
            return styleRange;
        }

        public ParticleOptions getDefaultParticle() {
            return defaultParticle;
        }

        @Override
        public String toString() {
            return String.format("BreathingStyle{id=%s, name=%s, range=%d, forms=%d}",
                styleId, styleName, styleRange, technique.getFormCount());
        }
    }
}
