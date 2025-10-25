package com.lerdorf.kimetsunoyaibamultiplayer.api;

import com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for nichirin swords and special breathing swords.
 * Maintains two separate lists:
 * - Nichirin swords: Standard breathing swords (Frost, Ice, etc.)
 * - Special swords: Unique named swords (Komorebi, Shimizu, etc.)
 *
 * Thread-safe implementation for use during mod initialization.
 */
public class SwordRegistry {

    public enum SwordCategory {
        NICHIRIN,  // Standard breathing swords
        SPECIAL    // Special/named swords
    }

    private static final Map<String, RegisteredSword> ALL_SWORDS = new ConcurrentHashMap<>();
    private static final Map<SwordCategory, Set<String>> CATEGORY_SWORDS = new ConcurrentHashMap<>();
    private static final Map<Item, RegisteredSword> ITEM_TO_SWORD = new ConcurrentHashMap<>();

    static {
        CATEGORY_SWORDS.put(SwordCategory.NICHIRIN, ConcurrentHashMap.newKeySet());
        CATEGORY_SWORDS.put(SwordCategory.SPECIAL, ConcurrentHashMap.newKeySet());
    }

    /**
     * Register a breathing sword with the system.
     *
     * @param swordId Unique identifier for this sword (e.g., "nichirinsword_frost")
     * @param swordItem The sword item instance
     * @param styleId The breathing style ID this sword uses
     * @param category Whether this is a NICHIRIN or SPECIAL sword
     * @param particle Default particle for sword swings (can be null to use style default)
     * @param swingSound Custom sound effect for sword swings (can be null for no custom sound)
     * @return The registered sword object
     * @throws IllegalArgumentException if swordId is already registered
     */
    public static RegisteredSword register(
            String swordId,
            BreathingSwordItem swordItem,
            String styleId,
            SwordCategory category,
            ParticleOptions particle,
            SoundEvent swingSound,
            Map<String, String> replaceAnimations) {

        if (ALL_SWORDS.containsKey(swordId)) {
            throw new IllegalArgumentException("Sword already registered: " + swordId);
        }

        RegisteredSword sword = new RegisteredSword(swordId, swordItem, styleId, category, particle, swingSound, replaceAnimations);

        ALL_SWORDS.put(swordId, sword);
        CATEGORY_SWORDS.get(category).add(swordId);
        ITEM_TO_SWORD.put(swordItem, sword);

        return sword;
    }

    /**
     * Register a breathing sword with the system (backwards compatible, no sound effect).
     *
     * @param swordId Unique identifier for this sword (e.g., "nichirinsword_frost")
     * @param swordItem The sword item instance
     * @param styleId The breathing style ID this sword uses
     * @param category Whether this is a NICHIRIN or SPECIAL sword
     * @param particle Default particle for sword swings (can be null to use style default)
     * @return The registered sword object
     * @throws IllegalArgumentException if swordId is already registered
     * @deprecated Use {@link #register(String, BreathingSwordItem, String, SwordCategory, ParticleOptions, SoundEvent)} instead
     */
    @Deprecated
    public static RegisteredSword register(
            String swordId,
            BreathingSwordItem swordItem,
            String styleId,
            SwordCategory category,
            ParticleOptions particle) {
        return register(swordId, swordItem, styleId, category, particle, null, null);
    }

    /**
     * Get a registered sword by its ID.
     *
     * @param swordId The unique identifier of the sword
     * @return The registered sword, or null if not found
     */
    public static RegisteredSword getSword(String swordId) {
        return ALL_SWORDS.get(swordId);
    }

    /**
     * Get a registered sword by its item instance.
     *
     * @param item The sword item
     * @return The registered sword, or null if not found
     */
    public static RegisteredSword getSword(Item item) {
        return ITEM_TO_SWORD.get(item);
    }

    /**
     * Get all registered swords.
     *
     * @return Unmodifiable collection of all registered swords
     */
    public static Collection<RegisteredSword> getAllSwords() {
        return Collections.unmodifiableCollection(ALL_SWORDS.values());
    }

    /**
     * Get all swords in a specific category.
     *
     * @param category The category to query
     * @return List of registered swords in that category
     */
    public static List<RegisteredSword> getSwordsByCategory(SwordCategory category) {
        Set<String> swordIds = CATEGORY_SWORDS.get(category);
        List<RegisteredSword> swords = new ArrayList<>();
        for (String id : swordIds) {
            RegisteredSword sword = ALL_SWORDS.get(id);
            if (sword != null) {
                swords.add(sword);
            }
        }
        return Collections.unmodifiableList(swords);
    }

    /**
     * Get all nichirin swords (non-special).
     *
     * @return List of nichirin swords
     */
    public static List<RegisteredSword> getNichirinSwords() {
        return getSwordsByCategory(SwordCategory.NICHIRIN);
    }

    /**
     * Get all special swords.
     *
     * @return List of special swords
     */
    public static List<RegisteredSword> getSpecialSwords() {
        return getSwordsByCategory(SwordCategory.SPECIAL);
    }

    /**
     * Check if a sword is registered.
     *
     * @param swordId The unique identifier of the sword
     * @return true if the sword is registered
     */
    public static boolean isRegistered(String swordId) {
        return ALL_SWORDS.containsKey(swordId);
    }

    /**
     * Check if an item is a registered breathing sword.
     *
     * @param item The item to check
     * @return true if the item is a registered breathing sword
     */
    public static boolean isRegistered(Item item) {
        return ITEM_TO_SWORD.containsKey(item);
    }

    /**
     * Clear all registered swords. Used for testing.
     */
    public static void clear() {
        ALL_SWORDS.clear();
        ITEM_TO_SWORD.clear();
        CATEGORY_SWORDS.get(SwordCategory.NICHIRIN).clear();
        CATEGORY_SWORDS.get(SwordCategory.SPECIAL).clear();
    }

    /**
     * Represents a registered breathing sword with all its metadata.
     */
    public static class RegisteredSword {
        private final String swordId;
        private final BreathingSwordItem swordItem;
        private final String styleId;
        private final SwordCategory category;
        private final ParticleOptions particle;
        private final SoundEvent swingSound;
        private final Map<String, String> replaceAnimations;

        private RegisteredSword(
                String swordId,
                BreathingSwordItem swordItem,
                String styleId,
                SwordCategory category,
                ParticleOptions particle,
                SoundEvent swingSound,
                Map<String, String> replaceAnimations) {
            this.swordId = swordId;
            this.swordItem = swordItem;
            this.styleId = styleId;
            this.category = category;
            this.particle = particle;
            this.swingSound = swingSound;
            this.replaceAnimations = replaceAnimations;
        }

        public String getSwordId() {
            return swordId;
        }

        public Map<String, String> getReplaceAnimations() {
        	return replaceAnimations;
        }
        
        public BreathingSwordItem getSwordItem() {
            return swordItem;
        }

        public String getStyleId() {
            return styleId;
        }

        public SwordCategory getCategory() {
            return category;
        }

        public ParticleOptions getParticle() {
            return particle;
        }
        
        public String getAnim(String ogAnim) {
        	if (replaceAnimations == null || replaceAnimations.size() == 0)
        		return ogAnim;
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

        /**
         * Get the custom swing sound for this sword.
         *
         * @return The custom swing sound, or null if none is set
         */
        public SoundEvent getSwingSound() {
            return swingSound;
        }

        /**
         * Get the breathing style for this sword.
         *
         * @return The registered breathing style, or null if not found
         */
        public BreathingStyleRegistry.RegisteredBreathingStyle getStyle() {
            return BreathingStyleRegistry.getStyle(styleId);
        }

        /**
         * Get the effective particle for this sword.
         * If the sword has a custom particle, use that.
         * Otherwise, fall back to the style's default particle.
         *
         * @return The particle to use for sword swings
         */
        public ParticleOptions getEffectiveParticle() {
            if (particle != null) {
                return particle;
            }
            BreathingStyleRegistry.RegisteredBreathingStyle style = getStyle();
            return style != null ? style.getDefaultParticle() : null;
        }

        @Override
        public String toString() {
            return String.format("Sword{id=%s, style=%s, category=%s}",
                swordId, styleId, category);
        }
    }
}
