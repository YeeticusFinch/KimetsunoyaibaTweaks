package com.lerdorf.kimetsunoyaibamultiplayer.raids;

import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * Maps entities to their power scales for the raid system.
 *
 * All entity categorizations are based on the power scales defined in raids.md.
 *
 * Usage:
 * - getEntityPowerScale(entityId) - Get the power scale for an entity
 * - getEntitiesForScale(scale) - Get all entities in a power scale
 * - isDemon(entityId) - Check if entity is a demon
 * - isDemonSlayer(entityId) - Check if entity is a demon slayer
 */
public class EntityCategorization {

    // Map entity IDs to power scales
    private static final Map<ResourceLocation, EntityPowerScale> ENTITY_SCALES = new HashMap<>();

    // Reverse lookup: power scale to entity list
    private static final Map<EntityPowerScale, List<ResourceLocation>> SCALE_ENTITIES = new EnumMap<>(EntityPowerScale.class);

    static {
        // Initialize all entity categorizations
        initializeDemonScales();
        initializeSlayerScales();

        // Build reverse lookup map
        buildReverseLookup();
    }

    /**
     * Initialize demon power scales.
     */
    private static void initializeDemonScales() {
        // EASY_DEMON
        registerDemon("demon", EntityPowerScale.EASY_DEMON);
        registerDemon("demon_2", EntityPowerScale.EASY_DEMON);
        registerDemon("demon_3", EntityPowerScale.EASY_DEMON);
        registerDemon("spider_demon", EntityPowerScale.EASY_DEMON);

        // MEDIUM_DEMON
        registerDemon("demon_4", EntityPowerScale.MEDIUM_DEMON);
        registerDemon("demon_5", EntityPowerScale.MEDIUM_DEMON);
        registerDemon("demon_9", EntityPowerScale.MEDIUM_DEMON);
        registerDemon("temple_demon", EntityPowerScale.MEDIUM_DEMON);
        registerDemon("swamp_demon", EntityPowerScale.MEDIUM_DEMON);

        // HARD_DEMON / EASY_BOSS_DEMON
        registerDemon("demon_8", EntityPowerScale.HARD_DEMON);
        registerDemon("demon_10", EntityPowerScale.HARD_DEMON);
        registerDemon("hand_demon", EntityPowerScale.HARD_DEMON);
        registerDemon("dice_steak_senior_demon", EntityPowerScale.HARD_DEMON);
        registerDemon("goldfishbig", EntityPowerScale.HARD_DEMON);
        registerDemon("rui_sister", EntityPowerScale.HARD_DEMON);
        registerDemon("rui_brother", EntityPowerScale.HARD_DEMON);
        registerDemon("rui_mother", EntityPowerScale.HARD_DEMON);
        registerDemon("rui_father", EntityPowerScale.HARD_DEMON);
        registerDemon("susamaru", EntityPowerScale.HARD_DEMON);
        registerDemon("yahaba", EntityPowerScale.HARD_DEMON);

        registerDemon("demon_6", EntityPowerScale.MEDIUM_BOSS_DEMON);
        registerDemon("demon_7", EntityPowerScale.MEDIUM_BOSS_DEMON);

        // MEDIUM_BOSS_DEMON (Lower Twelve Kizuki)
        // Listed in order from weakest to strongest
        registerDemon("kyogai", EntityPowerScale.MEDIUM_BOSS_DEMON);
        registerDemon("kamanue", EntityPowerScale.MEDIUM_BOSS_DEMON);
        registerDemon("rui", EntityPowerScale.MEDIUM_BOSS_DEMON);
        registerDemon("mukago", EntityPowerScale.MEDIUM_BOSS_DEMON);
        registerDemon("wakuraba", EntityPowerScale.MEDIUM_BOSS_DEMON);
        registerDemon("rokuro", EntityPowerScale.MEDIUM_BOSS_DEMON);
        registerDemon("hairo", EntityPowerScale.MEDIUM_BOSS_DEMON);
        registerDemon("enmu", EntityPowerScale.MEDIUM_BOSS_DEMON);

        // MEDIUM_BOSS_DEMON (kimetsu addon - optional)
        registerKimetsuDemon("kocho", EntityPowerScale.MEDIUM_BOSS_DEMON);

        // HARD_BOSS_DEMON (Upper Twelve Kizuki)
        // Listed in order from weakest to strongest
        registerDemon("daki", EntityPowerScale.HARD_BOSS_DEMON);
        // Note: gyutaro excluded (spawns automatically with daki)
        registerDemon("kaigaku", EntityPowerScale.HARD_BOSS_DEMON);
        registerDemon("gyokko", EntityPowerScale.HARD_BOSS_DEMON);
        registerDemon("hantengu", EntityPowerScale.HARD_BOSS_DEMON);
        registerDemon("nakime", EntityPowerScale.HARD_BOSS_DEMON);
        registerDemon("akaza", EntityPowerScale.HARD_BOSS_DEMON);
        registerDemon("doma", EntityPowerScale.HARD_BOSS_DEMON);
        registerDemon("kokushibo", EntityPowerScale.HARD_BOSS_DEMON);

        // HARD_BOSS_DEMON (kimetsu addon - optional)
        registerKimetsuDemon("tomioka", EntityPowerScale.HARD_BOSS_DEMON);
        registerKimetsuDemon("nakime_real", EntityPowerScale.HARD_BOSS_DEMON);
        registerKimetsuDemon("demon_nakime", EntityPowerScale.HARD_BOSS_DEMON);
        registerKimetsuDemon("gyokko", EntityPowerScale.HARD_BOSS_DEMON);
        registerKimetsuDemon("doma", EntityPowerScale.HARD_BOSS_DEMON);
        registerKimetsuDemon("kaigaku", EntityPowerScale.HARD_BOSS_DEMON);
        registerKimetsuDemon("kokushibo", EntityPowerScale.HARD_BOSS_DEMON);
        registerKimetsuDemon("zohakuten", EntityPowerScale.HARD_BOSS_DEMON);

        // DEMON_KING
        registerDemon("muzan", EntityPowerScale.DEMON_KING);
        registerDemon("tanjiro_demon", EntityPowerScale.DEMON_KING);

        // DEMON_KING (kimetsu addon - optional)
        registerKimetsuDemon("yoriichi", EntityPowerScale.DEMON_KING);
    }

    /**
     * Initialize demon slayer power scales.
     */
    private static void initializeSlayerScales() {
        // GENERIC_SLAYER
        registerSlayer("demon_slayer", EntityPowerScale.GENERIC_SLAYER);
        registerSlayer("murata", EntityPowerScale.GENERIC_SLAYER);
        // Note: frost_slayer and ice_slayer are automatic replacements handled elsewhere

        // NAMED_SLAYER
        // Listed in order from weakest to strongest
        registerSlayer("genya", EntityPowerScale.NAMED_SLAYER);
        registerSlayer("masachika", EntityPowerScale.NAMED_SLAYER);
        registerSlayer("inosuke", EntityPowerScale.NAMED_SLAYER);
        registerSlayer("tanjiro", EntityPowerScale.NAMED_SLAYER);
        registerSlayer("kaigaku_human", EntityPowerScale.NAMED_SLAYER);
        registerSlayer("sabito", EntityPowerScale.NAMED_SLAYER);
        registerSlayer("zennitsu", EntityPowerScale.NAMED_SLAYER);
        registerSlayer("kanawo", EntityPowerScale.NAMED_SLAYER);

        // HARD_SLAYER
        registerSlayer("dice_steak_senior", EntityPowerScale.HARD_SLAYER);

        // HASHIRA
        // Listed in order from weakest to strongest
        registerSlayer("kocho", EntityPowerScale.HASHIRA);
        registerSlayer("kanroji", EntityPowerScale.HASHIRA);
        registerSlayer("kanae", EntityPowerScale.HASHIRA);
        registerSlayer("shinazugawa", EntityPowerScale.HASHIRA);
        registerSlayer("rengoku", EntityPowerScale.HASHIRA);
        registerSlayer("iguro", EntityPowerScale.HASHIRA);
        registerSlayer("uzui", EntityPowerScale.HASHIRA);
        registerSlayer("tomioka", EntityPowerScale.HASHIRA);
        registerSlayer("muichirou", EntityPowerScale.HASHIRA);
        registerSlayer("himejima", EntityPowerScale.HASHIRA);
        registerSlayer("dice_steak_senior_super", EntityPowerScale.HASHIRA);

        // HASHIRA (kimetsu addon - optional)
        registerKimetsuSlayer("slayer_kocho", EntityPowerScale.HASHIRA);
        registerKimetsuSlayer("slayer_kocho_kakusei", EntityPowerScale.HASHIRA);
        registerKimetsuSlayer("himejima", EntityPowerScale.HASHIRA);

        // HASHIRA (kimetsunoyaibamultiplayer - enhanced breathing)
        registerMultiplayerSlayer("kanroji", EntityPowerScale.HASHIRA);
        registerMultiplayerSlayer("muichiro", EntityPowerScale.HASHIRA);

        // SUPER_HASHIRA
        // Listed in order from weakest to strongest
        registerSlayer("michikatsu", EntityPowerScale.SUPER_HASHIRA);
        registerSlayer("yoriichi", EntityPowerScale.SUPER_HASHIRA);
        registerSlayer("yoriichi_old", EntityPowerScale.SUPER_HASHIRA);
    }

    /**
     * Register a demon entity.
     */
    private static void registerDemon(String path, EntityPowerScale scale) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", path);
        ENTITY_SCALES.put(id, scale);
    }

    /**
     * Register a demon slayer entity.
     */
    private static void registerSlayer(String path, EntityPowerScale scale) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", path);
        ENTITY_SCALES.put(id, scale);
    }

    /**
     * Register a kimetsu addon demon entity.
     */
    private static void registerKimetsuDemon(String path, EntityPowerScale scale) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("kimetsu", path);
        ENTITY_SCALES.put(id, scale);
    }

    /**
     * Register a kimetsu addon demon slayer entity.
     */
    private static void registerKimetsuSlayer(String path, EntityPowerScale scale) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("kimetsu", path);
        ENTITY_SCALES.put(id, scale);
    }

    /**
     * Register a kimetsunoyaibamultiplayer demon slayer entity.
     */
    private static void registerMultiplayerSlayer(String path, EntityPowerScale scale) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", path);
        ENTITY_SCALES.put(id, scale);
    }

    /**
     * Register or override a custom entity categorization at runtime.
     */
    public static synchronized void registerCustomEntity(ResourceLocation entityId, EntityPowerScale scale) {
        EntityPowerScale previousScale = ENTITY_SCALES.put(entityId, scale);
        if (previousScale != null) {
            List<ResourceLocation> previousList = SCALE_ENTITIES.get(previousScale);
            if (previousList != null) {
                previousList.remove(entityId);
            }
        }
        SCALE_ENTITIES.computeIfAbsent(scale, key -> new ArrayList<>()).add(entityId);
    }

    /**
     * Build reverse lookup map (scale -> entity list).
     */
    private static void buildReverseLookup() {
        for (Map.Entry<ResourceLocation, EntityPowerScale> entry : ENTITY_SCALES.entrySet()) {
            EntityPowerScale scale = entry.getValue();
            ResourceLocation entityId = entry.getKey();

            SCALE_ENTITIES.computeIfAbsent(scale, k -> new ArrayList<>()).add(entityId);
        }
    }

    /**
     * Get the power scale for an entity.
     *
     * @param entityId The entity ID
     * @return The power scale, or null if not categorized
     */
    public static EntityPowerScale getEntityPowerScale(ResourceLocation entityId) {
        return ENTITY_SCALES.get(entityId);
    }

    /**
     * Get all entities in a power scale.
     *
     * @param scale The power scale
     * @return List of entity IDs, or empty list if none
     */
    public static List<ResourceLocation> getEntitiesForScale(EntityPowerScale scale) {
        return SCALE_ENTITIES.getOrDefault(scale, Collections.emptyList());
    }

    /**
     * Get all entities in multiple power scales.
     *
     * @param scales The power scales
     * @return Combined list of entity IDs
     */
    public static List<ResourceLocation> getEntitiesForScales(EntityPowerScale... scales) {
        List<ResourceLocation> result = new ArrayList<>();
        for (EntityPowerScale scale : scales) {
            result.addAll(getEntitiesForScale(scale));
        }
        return result;
    }

    /**
     * Check if an entity is a demon.
     *
     * @param entityId The entity ID
     * @return true if this is a demon
     */
    public static boolean isDemon(ResourceLocation entityId) {
        EntityPowerScale scale = getEntityPowerScale(entityId);
        return scale != null && scale.isDemonScale();
    }

    /**
     * Check if an entity is a demon slayer.
     *
     * @param entityId The entity ID
     * @return true if this is a demon slayer
     */
    public static boolean isDemonSlayer(ResourceLocation entityId) {
        EntityPowerScale scale = getEntityPowerScale(entityId);
        return scale != null && scale.isSlayerScale();
    }

    /**
     * Check if an entity is categorized.
     *
     * @param entityId The entity ID
     * @return true if this entity is categorized
     */
    public static boolean isCategorized(ResourceLocation entityId) {
        return ENTITY_SCALES.containsKey(entityId);
    }

    /**
     * Get all categorized entity IDs.
     *
     * @return Set of all entity IDs
     */
    public static Set<ResourceLocation> getAllEntityIds() {
        return ENTITY_SCALES.keySet();
    }

    /**
     * Get all power scales that have entities.
     *
     * @return Set of all power scales
     */
    public static Set<EntityPowerScale> getAllScales() {
        return SCALE_ENTITIES.keySet();
    }
}
