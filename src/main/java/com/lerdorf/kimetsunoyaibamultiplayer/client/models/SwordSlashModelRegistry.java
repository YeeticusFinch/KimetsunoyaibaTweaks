package com.lerdorf.kimetsunoyaibamultiplayer.client.models;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for mapping sword items to their slash model resources
 * Supports per-sword models with fallback to generic model
 */
@OnlyIn(Dist.CLIENT)
public class SwordSlashModelRegistry {

    // Map sword item paths to model keys (e.g., "nichirinsword_mist" -> "mist")
    private static final Map<String, String> SWORD_TO_MODEL_MAP = new HashMap<>();

    // Config overrides (allows users to force specific swords to use specific models)
    private static final Map<String, String> MODEL_OVERRIDES = new HashMap<>();

    // Generic fallback model key
    private static final String GENERIC_MODEL = "generic";

    static {
        // Register mist breathing swords to use mist model
        SWORD_TO_MODEL_MAP.put("nichirinsword_mist", "mist");
        SWORD_TO_MODEL_MAP.put("nichirinsword_muichiro", "mist");

        // Other swords will automatically fall back to generic model
    }

    /**
     * Gets the model key for a given sword item
     * @param swordItem The sword ItemStack
     * @return Model key (e.g., "mist", "generic")
     */
    public static String getModelKeyForSword(ItemStack swordItem) {
        if (swordItem.isEmpty()) {
            return GENERIC_MODEL;
        }
        
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(swordItem.getItem());
        String itemPath = itemId.getPath();
        String fullId = itemId.toString();

        // Check config overrides first
        if (MODEL_OVERRIDES.containsKey(fullId)) {
            String override = MODEL_OVERRIDES.get(fullId);
            Log.debug("Using override model for " + fullId + ": " + override);
            return override;
        }

        // Check registered models
        if (SWORD_TO_MODEL_MAP.containsKey(itemPath)) {
            String modelKey = SWORD_TO_MODEL_MAP.get(itemPath);
            Log.debug("Found registered model for " + itemPath + ": " + modelKey);
            return modelKey;
        }

        // Fall back to generic
        Log.debug("Using generic model for " + itemPath);
        return GENERIC_MODEL;
    }

    /**
     * Registers a sword to use a specific model
     * @param swordItemPath The item path (e.g., "nichirinsword_frost")
     * @param modelKey The model key (e.g., "frost")
     */
    public static void registerModel(String swordItemPath, String modelKey) {
        SWORD_TO_MODEL_MAP.put(swordItemPath, modelKey);
        Log.debug("Registered model mapping: " + swordItemPath + " -> " + modelKey);
    }

    /**
     * Sets a config override to force a sword to use a specific model
     * @param swordFullId The full item ID (e.g., "kimetsunoyaiba:nichirinsword_mist")
     * @param modelKey The model key to force
     */
    public static void setOverride(String swordFullId, String modelKey) {
        MODEL_OVERRIDES.put(swordFullId, modelKey);
        Log.info("Set model override: " + swordFullId + " -> " + modelKey);
    }

    /**
     * Clears a config override
     * @param swordFullId The full item ID
     */
    public static void clearOverride(String swordFullId) {
        MODEL_OVERRIDES.remove(swordFullId);
        Log.info("Cleared model override for: " + swordFullId);
    }

    /**
     * Clears all config overrides
     */
    public static void clearAllOverrides() {
        MODEL_OVERRIDES.clear();
        Log.info("Cleared all model overrides");
    }

    /**
     * Gets the generic fallback model key
     * @return The generic model key
     */
    public static String getGenericModel() {
        return GENERIC_MODEL;
    }
}
