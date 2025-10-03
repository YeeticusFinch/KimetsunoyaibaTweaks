package com.lerdorf.kimetsunoyaibamultiplayer.particles;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.config.ParticleConfig;
import com.mojang.logging.LogUtils;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

public class SwordParticleMapping {
    //private static final Log Log = LogUtils.getLog();

    private static final Map<String, ResourceLocation> SWORD_TO_PARTICLE_MAP = new HashMap<>();

    static {
        // Initialize hardcoded mappings for specific sword types
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_thunder", ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "particle_thunder"));
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_water", ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "particle_blue_smoke"));
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_flame", ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "particle_flame"));
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_stone", ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "particle_stone"));
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_wind", ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "particle_wind"));
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_sun", ResourceLocation.fromNamespaceAndPath("minecraft", "flame"));
        SWORD_TO_PARTICLE_MAP.put("nichirinswordmoon", ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "particle_blue_smoke")); // moon is missing an underscore, not a typo!!!
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_flower", ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "particle_flower"));
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_insect", ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "particle_insect"));
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_sound", ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "particle_sound"));
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_love", ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "particle_love"));
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_mist", ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "particle_mist"));
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_serpent", ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "particle_serpent"));
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_beast", ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "particle_beast"));

        // Special cases with vanilla particles
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_inosuke", ResourceLocation.fromNamespaceAndPath("minecraft", "crit"));
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_basic", ResourceLocation.fromNamespaceAndPath("minecraft", "crit"));
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_generic", ResourceLocation.fromNamespaceAndPath("minecraft", "cloud"));

        // Our mod's breathing swords - will use custom logic in getParticleForSword
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_frost", ResourceLocation.fromNamespaceAndPath("minecraft", "snowflake"));
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_ice", ResourceLocation.fromNamespaceAndPath("minecraft", "dust")); // Light blue dust
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_hanazawa", ResourceLocation.fromNamespaceAndPath("minecraft", "dust")); // Light blue dust
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_hiori", ResourceLocation.fromNamespaceAndPath("minecraft", "snowflake"));
    }

    /**
     * Gets the particle effect for a given sword item
     * @param swordItem The sword ItemStack to get particles for
     * @return ParticleOptions for the particle to spawn, or null if no particle should be spawned
     */
    public static ParticleOptions getParticleForSword(ItemStack swordItem) {
        if (swordItem.isEmpty()) {
            return null;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(swordItem.getItem());
        String itemIdString = itemId.toString();

        // First, check config-based particle mappings
        System.out.println("Looking for particle mapping for item: " + itemIdString);
        if (ParticleConfig.particleMappings != null) {
            System.out.println("Config mappings available: " + ParticleConfig.particleMappings.size());
            if (ParticleConfig.particleMappings.containsKey(itemIdString)) {
                ParticleConfig.ParticleMapping mapping = ParticleConfig.particleMappings.get(itemIdString);
                System.out.println("Found config mapping: " + mapping.particleType);
                ParticleOptions result = createParticleFromMapping(mapping);
                if (result != null) {
                    System.out.println("Successfully created particle from config mapping");
                    return result;
                } else {
                    System.err.println("Failed to create particle from config mapping");
                }
            } else {
                System.out.println("No config mapping found for: " + itemIdString);
                System.out.println("Available mappings: " + ParticleConfig.particleMappings.keySet());
            }
        } else {
            System.err.println("ParticleConfig.particleMappings is null!");
        }

        // Check if this is a nichirin sword (fallback to legacy logic)
        boolean isKimetsunoyaibaSword = itemId.getNamespace().equals("kimetsunoyaiba") && itemId.getPath().startsWith("nichirinsword_");
        boolean isOurModSword = itemId.getNamespace().equals("kimetsunoyaibamultiplayer") && itemId.getPath().startsWith("nichirinsword_");

        if (!isKimetsunoyaibaSword && !isOurModSword) {
            return null;
        }

        // Extract the sword type (part after "nichirinsword_")
        String swordType = itemId.getPath();

        // Special handling for our mod's ice breathing swords - return light blue dust
        if (itemId.getNamespace().equals("kimetsunoyaibamultiplayer")) {
            if (swordType.equals("nichirinsword_ice") || swordType.equals("nichirinsword_hanazawa")) {
                // Light blue color (RGB: 0.5, 0.8, 1.0)
                Vector3f lightBlue = new Vector3f(0.5f, 0.8f, 1.0f);
                return new DustParticleOptions(lightBlue, 1.0f);
            } else if (swordType.equals("nichirinsword_frost") || swordType.equals("nichirinsword_hiori")) {
                return ParticleTypes.SNOWFLAKE;
            }
        }

        // Legacy fallback: Look up the particle mapping
        ResourceLocation particleId = SWORD_TO_PARTICLE_MAP.get(swordType);

        if (particleId == null) {
            // Fallback: try to create a particle name based on the sword type
            String typeSuffix = swordType.substring("nichirinsword_".length());
            particleId = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "particle_" + typeSuffix);
        }

        // Try to get the particle from the registry
        if (BuiltInRegistries.PARTICLE_TYPE.containsKey(particleId)) {
            var particleType = BuiltInRegistries.PARTICLE_TYPE.get(particleId);
            if (particleType instanceof ParticleOptions) {
                return (ParticleOptions) particleType;
            }
            // For simple particle types, we need to create the options
            return (ParticleOptions) particleType;
        }

        // Ultimate fallback: use a generic particle effect
        if (Config.logDebug)
        	Log.debug("No particle found for sword {}, using fallback particle", itemId);
        return ParticleTypes.CLOUD;
    }

    /**
     * Creates a ParticleOptions from a config-based particle mapping
     * @param mapping The particle mapping from config
     * @return ParticleOptions for the particle, or null if invalid
     */
    private static ParticleOptions createParticleFromMapping(ParticleConfig.ParticleMapping mapping) {
        try {
            System.out.println("Creating particle from mapping: " + mapping.particleType + " (isDust: " + mapping.isDust + ")");
            ResourceLocation particleId = ResourceLocation.parse(mapping.particleType);

            if (mapping.isDust) {
                // Create dust particle with custom size and color
                Vector3f color = new Vector3f(mapping.red, mapping.green, mapping.blue);
                System.out.println("Creating dust particle with color (" + mapping.red + ", " + mapping.green + ", " + mapping.blue + ") size " + mapping.size);
                DustParticleOptions dustOptions = new DustParticleOptions(color, mapping.size);
                System.out.println("Successfully created dust particle options");
                return dustOptions;
            } else {
                // Try to get the particle from the registry
                System.out.println("Looking for particle in registry: " + particleId);
                if (BuiltInRegistries.PARTICLE_TYPE.containsKey(particleId)) {
                    var particleType = BuiltInRegistries.PARTICLE_TYPE.get(particleId);
                    System.out.println("Found particle type in registry: " + particleType);
                    return (ParticleOptions) particleType;
                } else {
                    System.err.println("Particle not found in registry: " + particleId);
                }
            }
        } catch (Exception e) {
            Log.error("Failed to create particle from mapping: {}", mapping.particleType, e);
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Checks if an item should have particle effects
     * @param item The ItemStack to check
     * @return true if this item has particle effects configured
     */
    public static boolean isKimetsunoyaibaSword(ItemStack item) {
        if (item.isEmpty()) {
            return false;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item.getItem());
        String itemIdString = itemId.toString();

        // First check config-based mappings
        if (ParticleConfig.particleMappings != null && ParticleConfig.particleMappings.containsKey(itemIdString)) {
            return true;
        }

        // Check if this is a kimetsunoyaiba nichirin sword or our mod's breathing swords
        return (itemId.getNamespace().equals("kimetsunoyaiba") && itemId.getPath().startsWith("nichirinsword_")) ||
               (itemId.getNamespace().equals("kimetsunoyaibamultiplayer") && itemId.getPath().startsWith("nichirinsword_"));
    }

    /**
     * Gets the sword type name for debugging/logging purposes
     * @param swordItem The sword ItemStack
     * @return The sword type name, or "unknown" if not a nichirin sword
     */
    public static String getSwordTypeName(ItemStack swordItem) {
        if (!isKimetsunoyaibaSword(swordItem)) {
            return "unknown";
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(swordItem.getItem());
        return itemId.getPath();
    }

    /**
     * Registers a custom sword-to-particle mapping
     * @param swordType The sword type (e.g., "nichirinsword_custom")
     * @param particleId The particle ResourceLocation to use
     */
    public static void registerCustomMapping(String swordType, ResourceLocation particleId) {
        SWORD_TO_PARTICLE_MAP.put(swordType, particleId);
        Log.info("Registered custom sword particle mapping: {} -> {}", swordType, particleId);
    }
}