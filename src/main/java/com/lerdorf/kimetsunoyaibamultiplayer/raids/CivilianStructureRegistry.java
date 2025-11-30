package com.lerdorf.kimetsunoyaibamultiplayer.raids;

import com.lerdorf.kimetsunoyaibamultiplayer.config.RaidConfig;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * Registry of civilian structures that can trigger raids.
 *
 * Categorizes structures by size (SMALL, MEDIUM, LARGE) to determine
 * appropriate spawn radii for raid entities.
 *
 * Based on structure list from raids.md.
 */
public class CivilianStructureRegistry {

    /**
     * Structure size categories.
     * Determines spawn radius for raid entities.
     */
    public enum StructureSize {
        /**
         * Small structures (houses).
         * Default spawn radius: 32 blocks
         */
        SMALL,

        /**
         * Medium structures (temples, trains).
         * Default spawn radius: 48 blocks
         */
        MEDIUM,

        /**
         * Large structures (villages).
         * Default spawn radius: 96 blocks
         */
        LARGE
    }

    // Map structure IDs to sizes
    private static final Map<ResourceLocation, StructureSize> STRUCTURE_SIZES = new HashMap<>();

    // Set of all civilian structure IDs (for quick lookup)
    private static final Set<ResourceLocation> CIVILIAN_STRUCTURES = new HashSet<>();

    static {
        initializeStructures();
    }

    /**
     * Initialize structure categorizations.
     */
    private static void initializeStructures() {
        // SMALL STRUCTURES (Houses)
        registerStructure("house_tamayo", StructureSize.SMALL, "kimetsunoyaiba");
        registerStructure("house_tanjiro", StructureSize.SMALL, "kimetsunoyaiba");
        registerStructure("house_ubuyashiki", StructureSize.SMALL, "kimetsunoyaiba");
        registerStructure("house_urokodaki", StructureSize.SMALL, "kimetsunoyaiba");
        registerStructure("house_a", StructureSize.SMALL, "kimetsunoyaiba");
        registerStructure("house_kocho", StructureSize.SMALL, "kimetsunoyaiba");
        registerStructure("house_rengoku", StructureSize.SMALL, "kimetsunoyaiba");

        // MEDIUM STRUCTURES (Temples, special structures)
        // Note: mugen_train is medium-sized
        registerStructure("mugen_train", StructureSize.MEDIUM, "kimetsunoyaiba");
        registerStructure("temple_doma", StructureSize.MEDIUM, "kimetsunoyaiba");

        // LARGE STRUCTURES (Villages)
        // Fix typo: swamp
        registerStructure("village_swamp", StructureSize.LARGE, "kimetsunoyaiba");
        registerStructure("village_yukak", StructureSize.LARGE, "kimetsunoyaiba");

        // Vanilla villages (all LARGE)
        registerVanillaVillage("village_plains");
        registerVanillaVillage("village_desert");
        registerVanillaVillage("village_savanna");
        registerVanillaVillage("village_snowy");
        registerVanillaVillage("village_taiga");
    }

    /**
     * Register a structure with size categorization.
     */
    private static void registerStructure(String path, StructureSize size, String namespace) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, path);
        STRUCTURE_SIZES.put(id, size);
        CIVILIAN_STRUCTURES.add(id);
    }

    /**
     * Register a vanilla village (always LARGE).
     */
    private static void registerVanillaVillage(String path) {
        registerStructure(path, StructureSize.LARGE, "minecraft");
    }

    /**
     * Check if a structure is a civilian structure.
     *
     * @param structureId The structure ID
     * @return true if this is a civilian structure
     */
    public static boolean isCivilianStructure(ResourceLocation structureId) {
        return CIVILIAN_STRUCTURES.contains(structureId);
    }

    /**
     * Get the size category for a structure.
     *
     * @param structureId The structure ID
     * @return The structure size, or null if not categorized
     */
    public static StructureSize getStructureSize(ResourceLocation structureId) {
        return STRUCTURE_SIZES.get(structureId);
    }

    /**
     * Get the spawn radius for a structure (in blocks).
     *
     * @param structureId The structure ID
     * @return The spawn radius, or 0 if not categorized
     */
    public static int getSpawnRadius(ResourceLocation structureId) {
        StructureSize size = getStructureSize(structureId);
        if (size == null) {
            return 0;
        }

        switch (size) {
            case SMALL:
                return RaidConfig.smallStructureSpawnRadius.get();
            case MEDIUM:
                return RaidConfig.mediumStructureSpawnRadius.get();
            case LARGE:
                return RaidConfig.largeStructureSpawnRadius.get();
            default:
                return 0;
        }
    }

    /**
     * Get all civilian structure IDs.
     *
     * @return Unmodifiable set of structure IDs
     */
    public static Set<ResourceLocation> getAllCivilianStructures() {
        return Collections.unmodifiableSet(CIVILIAN_STRUCTURES);
    }

    /**
     * Get all structures of a specific size.
     *
     * @param size The structure size
     * @return List of structure IDs
     */
    public static List<ResourceLocation> getStructuresBySize(StructureSize size) {
        List<ResourceLocation> result = new ArrayList<>();
        for (Map.Entry<ResourceLocation, StructureSize> entry : STRUCTURE_SIZES.entrySet()) {
            if (entry.getValue() == size) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    /**
     * Get the default spawn radius for a structure size.
     *
     * @param size The structure size
     * @return The spawn radius in blocks
     */
    public static int getDefaultSpawnRadius(StructureSize size) {
        switch (size) {
            case SMALL:
                return RaidConfig.smallStructureSpawnRadius.get();
            case MEDIUM:
                return RaidConfig.mediumStructureSpawnRadius.get();
            case LARGE:
                return RaidConfig.largeStructureSpawnRadius.get();
            default:
                return 32; // Fallback
        }
    }

    /**
     * Get the containment radius for a structure (in blocks).
     *
     * @param structureId The structure ID
     * @return The containment radius, or 0 if not categorized
     */
    public static int getContainmentRadius(ResourceLocation structureId) {
        StructureSize size = getStructureSize(structureId);
        if (size == null) {
            return 0;
        }

        switch (size) {
            case SMALL:
                return RaidConfig.smallStructureContainmentRadius.get();
            case MEDIUM:
                return RaidConfig.mediumStructureContainmentRadius.get();
            case LARGE:
                return RaidConfig.largeStructureContainmentRadius.get();
            default:
                return 150; // Fallback
        }
    }
}
