package com.lerdorf.kimetsunoyaibamultiplayer.api;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registry for base mod sword metadata.
 * This handles swords from the base KimetsunoYaiba mod which are not BreathingSwordItem instances
 * but still need to be tracked for color change and other features.
 *
 * Sword Levels:
 * - -1 = Unobtainable swords (e.g., sword_kokushibo_1, sword_kokushibo_2)
 * - 0 = Base/generic swords (e.g., nichirinsword_water, nichirinsword_flame)
 * - 1 = Named character swords (e.g., nichirinsword_tanjiro, nichirinsword_inosuke)
 * - 2 = Hashira swords (e.g., nichirinsword_rengoku, nichirinsword_uzui)
 */
public class SwordMetadataRegistry {

    private static final Map<String, SwordMetadata> SWORDS_BY_ID = new ConcurrentHashMap<>();
    private static final Map<Item, SwordMetadata> SWORDS_BY_ITEM = new ConcurrentHashMap<>();

    /**
     * Register a base mod sword's metadata.
     *
     * @param swordId Unique identifier (e.g., "kimetsunoyaiba:nichirinsword_water")
     * @param item The sword item instance
     * @param styleId The breathing style ID this sword uses
     * @param swordLevel The sword's tier (0=base, 1=named, 2=hashira)
     * @return The registered sword metadata
     * @throws IllegalArgumentException if swordId is already registered
     */
    public static SwordMetadata register(
            String swordId,
            Item item,
            String styleId,
            int swordLevel) {

        if (SWORDS_BY_ID.containsKey(swordId)) {
            throw new IllegalArgumentException("Sword already registered: " + swordId);
        }

        if (swordLevel < -1 || swordLevel > 2) {
            throw new IllegalArgumentException("Invalid sword level: " + swordLevel + ". Must be -1, 0, 1, or 2.");
        }

        SwordMetadata metadata = new SwordMetadata(swordId, item, styleId, swordLevel);
        SWORDS_BY_ID.put(swordId, metadata);
        if (item != null) {
            SWORDS_BY_ITEM.put(item, metadata);
        }

        return metadata;
    }

    /**
     * Register a base mod sword's metadata with lazy item resolution.
     * The item will be looked up from the registry when first accessed.
     *
     * @param swordId Unique identifier (e.g., "kimetsunoyaiba:nichirinsword_water")
     * @param styleId The breathing style ID this sword uses
     * @param swordLevel The sword's tier (0=base, 1=named, 2=hashira)
     * @return The registered sword metadata
     */
    public static SwordMetadata registerLazy(
            String swordId,
            String styleId,
            int swordLevel) {

        if (SWORDS_BY_ID.containsKey(swordId)) {
            throw new IllegalArgumentException("Sword already registered: " + swordId);
        }

        if (swordLevel < -1 || swordLevel > 2) {
            throw new IllegalArgumentException("Invalid sword level: " + swordLevel + ". Must be -1, 0, 1, or 2.");
        }

        SwordMetadata metadata = new SwordMetadata(swordId, null, styleId, swordLevel);
        SWORDS_BY_ID.put(swordId, metadata);

        return metadata;
    }

    /**
     * Get the metadata for a registered sword by ID.
     *
     * @param swordId The sword identifier
     * @return The sword metadata, or null if not found
     */
    public static SwordMetadata getMetadata(String swordId) {
        return SWORDS_BY_ID.get(swordId);
    }

    /**
     * Get the metadata for a registered sword by item.
     *
     * @param item The sword item
     * @return The sword metadata, or null if not found
     */
    public static SwordMetadata getMetadata(Item item) {
        // First check direct lookup
        SwordMetadata direct = SWORDS_BY_ITEM.get(item);
        if (direct != null) {
            return direct;
        }

        // Fall back to checking by registry name
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
        if (itemId != null) {
            return SWORDS_BY_ID.get(itemId.toString());
        }

        return null;
    }

    /**
     * Check if a sword is registered.
     *
     * @param swordId The sword identifier
     * @return true if the sword is registered
     */
    public static boolean isRegistered(String swordId) {
        return SWORDS_BY_ID.containsKey(swordId);
    }

    /**
     * Get all swords for a specific style and level.
     *
     * @param styleId The breathing style ID
     * @param level The sword level (0, 1, or 2)
     * @return List of matching sword metadata
     */
    public static List<SwordMetadata> getSwordsByStyleAndLevel(String styleId, int level) {
        return SWORDS_BY_ID.values().stream()
                .filter(m -> m.getStyleId().equals(styleId) && m.getSwordLevel() == level)
                .collect(Collectors.toList());
    }

    /**
     * Get all swords that are eligible for color change transformation.
     * These are level 0 swords with color-change-eligible styles.
     *
     * @return List of color-change-eligible sword metadata
     */
    public static List<SwordMetadata> getColorChangeEligibleSwords() {
        // Get all level 0 swords whose style is color-change-eligible
        List<StyleMetadataRegistry.StyleMetadata> eligibleStyles =
            StyleMetadataRegistry.getColorChangeEligibleStyles();

        Set<String> eligibleStyleIds = eligibleStyles.stream()
            .map(StyleMetadataRegistry.StyleMetadata::getStyleId)
            .collect(Collectors.toSet());

        return SWORDS_BY_ID.values().stream()
                .filter(m -> m.getSwordLevel() == 0)
                .filter(m -> eligibleStyleIds.contains(m.getStyleId()))
                .collect(Collectors.toList());
    }

    /**
     * Get all registered swords.
     *
     * @return Unmodifiable collection of all registered sword metadata
     */
    public static Collection<SwordMetadata> getAllSwords() {
        return Collections.unmodifiableCollection(SWORDS_BY_ID.values());
    }

    /**
     * Clear all registered swords. Used for testing.
     */
    public static void clear() {
        SWORDS_BY_ID.clear();
        SWORDS_BY_ITEM.clear();
    }

    /**
     * Represents metadata for a registered base mod sword.
     */
    public static class SwordMetadata {
        private final String swordId;
        private Item swordItem;
        private final String styleId;
        private final int swordLevel;

        private SwordMetadata(
                String swordId,
                Item swordItem,
                String styleId,
                int swordLevel) {
            this.swordId = swordId;
            this.swordItem = swordItem;
            this.styleId = styleId;
            this.swordLevel = swordLevel;
        }

        /**
         * Get the unique sword identifier.
         *
         * @return The sword ID (e.g., "kimetsunoyaiba:nichirinsword_water")
         */
        public String getSwordId() {
            return swordId;
        }

        /**
         * Get the sword item instance.
         * Resolves lazily from the registry if not directly set.
         *
         * @return The sword item, or null if not found
         */
        public Item getSwordItem() {
            if (swordItem == null) {
                // Lazy resolution from registry
                ResourceLocation loc = ResourceLocation.tryParse(swordId);
                if (loc != null) {
                    swordItem = ForgeRegistries.ITEMS.getValue(loc);
                    if (swordItem != null) {
                        SWORDS_BY_ITEM.put(swordItem, this);
                    }
                }
            }
            return swordItem;
        }

        /**
         * Get the breathing style ID for this sword.
         *
         * @return The style ID
         */
        public String getStyleId() {
            return styleId;
        }

        /**
         * Get the sword level.
         *
         * @return 0 for base, 1 for named character, 2 for hashira
         */
        public int getSwordLevel() {
            return swordLevel;
        }

        /**
         * Get a human-readable description of the sword level.
         *
         * @return "Unobtainable", "Base", "Named Character", or "Hashira"
         */
        public String getSwordLevelDescription() {
            return switch (swordLevel) {
                case -1 -> "Unobtainable";
                case 0 -> "Base";
                case 1 -> "Named Character";
                case 2 -> "Hashira";
                default -> "Unknown";
            };
        }

        /**
         * Check if this sword is obtainable in survival mode.
         * Swords with level -1 are unobtainable (not in loot tables, random tables, etc.)
         *
         * @return true if the sword is obtainable in survival, false if unobtainable
         */
        public boolean isObtainableInSurvival() {
            return swordLevel >= 0;
        }

        @Override
        public String toString() {
            return String.format("SwordMetadata{id=%s, style=%s, level=%d (%s)}",
                swordId, styleId, swordLevel, getSwordLevelDescription());
        }
    }
}
