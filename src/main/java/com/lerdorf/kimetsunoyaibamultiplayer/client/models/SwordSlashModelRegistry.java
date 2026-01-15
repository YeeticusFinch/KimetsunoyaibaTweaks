package com.lerdorf.kimetsunoyaibamultiplayer.client.models;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
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

    // Map model keys to frame counts for animated textures (e.g., "forest" -> 10)
    private static final Map<String, Integer> ANIMATED_TEXTURE_FRAMES = new HashMap<>();

    // Map model keys to frame delay in ticks (e.g., "forest" -> 2 means change frame every 2 ticks)
    private static final Map<String, Integer> ANIMATED_TEXTURE_FRAME_DELAY = new HashMap<>();

    // Default frame delay in ticks
    private static final int DEFAULT_FRAME_DELAY = 2;

    // Map model keys to resource namespaces (e.g., "forest" -> "knyextraadditions")
    private static final Map<String, String> MODEL_KEY_TO_NAMESPACE = new HashMap<>();

    // Generic fallback model key
    private static final String GENERIC_MODEL = "generic";

    static {
        // Register mist breathing swords to use mist model
        SWORD_TO_MODEL_MAP.put("nichirinsword_mist", "mist");
        SWORD_TO_MODEL_MAP.put("nichirinsword_muichiro", "mist");

        // Register sound breathing swords to use sound model
        SWORD_TO_MODEL_MAP.put("nichirinsword_uzui", "sound");

        // Other swords will automatically fall back to generic model
    }
    
    public static String getModelKeyByName(String name) {
    	// Check config overrides first
        if (MODEL_OVERRIDES.containsKey(name)) {
            String override = MODEL_OVERRIDES.get(name);
            Log.debug("Using override model for " + name + ": " + override);
            return override;
        }

        // Check registered models
        if (SWORD_TO_MODEL_MAP.containsKey(name)) {
            String modelKey = SWORD_TO_MODEL_MAP.get(name);
            Log.debug("Found registered model for " + name + ": " + modelKey);
            return modelKey;
        }

        // Fall back to generic
        Log.debug("Using generic model for " + name);
        return GENERIC_MODEL;
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
     * Registers a custom namespace for a model key's resources.
     * @param modelKey The model key (e.g., "forest")
     * @param namespace The resource namespace where model/texture files live
     */
    public static void registerModelNamespace(String modelKey, String namespace) {
        if (namespace == null || namespace.isEmpty()) {
            Log.warn("Namespace must be non-empty for model key: " + modelKey);
            return;
        }
        MODEL_KEY_TO_NAMESPACE.put(modelKey, namespace);
        Log.debug("Registered model namespace: " + modelKey + " -> " + namespace);
    }

    /**
     * Gets the namespace for a model key's resources.
     * Defaults to the core mod namespace when not explicitly registered.
     * @param modelKey The model key
     * @return Namespace to use for model resources
     */
    public static String getNamespaceForModelKey(String modelKey) {
        return MODEL_KEY_TO_NAMESPACE.getOrDefault(modelKey, KimetsunoyaibaMultiplayer.MODID);
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

    /**
     * Registers a model key to use animated textures with a specified frame count
     * @param modelKey The model key (e.g., "forest")
     * @param frameCount Number of texture frames (must be > 1)
     */
    public static void registerAnimatedTexture(String modelKey, int frameCount) {
        if (frameCount > 1) {
            ANIMATED_TEXTURE_FRAMES.put(modelKey, frameCount);
            Log.info("Registered animated texture: " + modelKey + " with " + frameCount + " frames");
        } else {
            Log.warn("Frame count must be > 1 for animated textures. Ignoring registration for: " + modelKey);
        }
    }

    /**
     * Gets the frame count for a model key
     * @param modelKey The model key
     * @return Frame count (1 if not animated, >1 if animated)
     */
    public static int getFrameCount(String modelKey) {
        return ANIMATED_TEXTURE_FRAMES.getOrDefault(modelKey, 1);
    }

    /**
     * Checks if a model key uses animated textures
     * @param modelKey The model key
     * @return true if animated, false if static
     */
    public static boolean isAnimated(String modelKey) {
        return ANIMATED_TEXTURE_FRAMES.containsKey(modelKey) && ANIMATED_TEXTURE_FRAMES.get(modelKey) > 1;
    }

    /**
     * Registers an animated texture with both frame count and frame delay
     * @param modelKey The model key (e.g., "forest")
     * @param frameCount Number of texture frames (must be > 1)
     * @param ticksPerFrame Ticks to wait before changing to next frame (default: 2)
     */
    public static void registerAnimatedTexture(String modelKey, int frameCount, int ticksPerFrame) {
        registerAnimatedTexture(modelKey, frameCount);
        if (ticksPerFrame > 0) {
            ANIMATED_TEXTURE_FRAME_DELAY.put(modelKey, ticksPerFrame);
            Log.info("Set frame delay for " + modelKey + ": " + ticksPerFrame + " ticks per frame");
        }
    }

    /**
     * Sets the frame delay for an animated texture model
     * @param modelKey The model key
     * @param ticksPerFrame Ticks to wait before changing to next frame
     */
    public static void setFrameDelay(String modelKey, int ticksPerFrame) {
        if (ticksPerFrame > 0) {
            ANIMATED_TEXTURE_FRAME_DELAY.put(modelKey, ticksPerFrame);
            Log.info("Set frame delay for " + modelKey + ": " + ticksPerFrame + " ticks per frame");
        } else {
            Log.warn("Frame delay must be > 0. Ignoring for: " + modelKey);
        }
    }

    /**
     * Gets the frame delay for a model key (ticks per frame)
     * @param modelKey The model key
     * @return Ticks per frame (default: 2)
     */
    public static int getFrameDelay(String modelKey) {
        return ANIMATED_TEXTURE_FRAME_DELAY.getOrDefault(modelKey, DEFAULT_FRAME_DELAY);
    }
}
