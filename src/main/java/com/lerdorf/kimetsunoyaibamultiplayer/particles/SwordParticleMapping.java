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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SwordParticleMapping {
    // Items exempt from sword sheath display (e.g., Himejima's axe and ball)
    private static final Set<String> SHEATH_EXEMPT_ITEMS = new HashSet<>();

    static {
        // Himejima's weapons should not render in sword sheath
        SHEATH_EXEMPT_ITEMS.add("nichirinsword_himejima_1");
        SHEATH_EXEMPT_ITEMS.add("nichirinsword_himejima_2");
    }
    //private static final Log Log = LogUtils.getLog();

    private static final Map<String, ResourceLocation> SWORD_TO_PARTICLE_MAP = new HashMap<>();

    static {
        // Initialize hardcoded mappings for specific sword types

        // Thunder Breathing
        ResourceLocation thunderParticle = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "particle_thunder");
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_thunder", thunderParticle);
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_zenitsu", thunderParticle);
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_kaigaku", thunderParticle);

        // Water Breathing
        ResourceLocation waterParticle = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "particle_blue_smoke");
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_water", waterParticle);
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_tomioka", waterParticle);

        // Flame Breathing
        ResourceLocation flameParticle = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "particle_flame");
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_flame", flameParticle);
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_rengoku", flameParticle);

        // Mist Breathing
        ResourceLocation mistParticle = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "particle_mist");
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_mist", mistParticle);
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_tokito", mistParticle);
        SWORD_TO_PARTICLE_MAP.put("nitirintou_tokitou", mistParticle); // Alternative spelling

        // Wind Breathing
        ResourceLocation windParticle = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "particle_wind");
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_wind", windParticle);
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_shinazugawa", windParticle);

        // Stone Breathing
        ResourceLocation stoneParticle = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "particle_stone");
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_stone", stoneParticle);
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_himejima_1", stoneParticle);
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_himejima_2", stoneParticle);

        // Insect Breathing
        ResourceLocation insectParticle = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "particle_insect");
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_insect", insectParticle);
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_kocho", insectParticle);

        // Serpent Breathing
        ResourceLocation serpentParticle = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "particle_serpent");
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_serpent", serpentParticle);
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_iguro", serpentParticle);

        // Sound Breathing
        ResourceLocation soundParticle = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "particle_sound");
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_sound", soundParticle);
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_uzui", soundParticle);

        // Love Breathing
        ResourceLocation loveParticle = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "particle_love");
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_love", loveParticle);
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_kanroji", loveParticle);

        // Flower Breathing
        ResourceLocation flowerParticle = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "particle_flower");
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_flower", flowerParticle);
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_kanae", flowerParticle);
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_kanawo", flowerParticle);

        // Beast Breathing
        ResourceLocation beastParticle = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "particle_beast");
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_beast", beastParticle);
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_inosuke", beastParticle);

        // Sun Breathing / Hinokami Kagura
        ResourceLocation sunParticle = ResourceLocation.fromNamespaceAndPath("minecraft", "flame");
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_sun", sunParticle);
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_yoriichi", sunParticle);
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_tanjiro_2", sunParticle);

        // Moon Breathing (moon is missing an underscore, not a typo!!!)
        ResourceLocation moonParticle = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "particle_blue_smoke");
        SWORD_TO_PARTICLE_MAP.put("nichirinswordmoon", moonParticle);

        // Generic/Basic swords
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_basic", ResourceLocation.fromNamespaceAndPath("minecraft", "crit"));
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_generic", ResourceLocation.fromNamespaceAndPath("minecraft", "cloud"));
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_black", ResourceLocation.fromNamespaceAndPath("minecraft", "cloud"));

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

        // First, check if this sword is registered in the SwordRegistry
        var registeredSword = com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry
            .getSword(swordItem.getItem());
        if (registeredSword != null) {
            ParticleOptions effectiveParticle = registeredSword.getEffectiveParticle();
            if (effectiveParticle != null) {
                Log.debug("Using registered particle for sword: " + itemIdString);
                return effectiveParticle;
            }
        }

        // Second, check config-based particle mappings
        Log.debug("Looking for particle mapping for item: " + itemIdString);
        if (ParticleConfig.particleMappings != null) {
            Log.debug("Config mappings available: " + ParticleConfig.particleMappings.size());
            if (ParticleConfig.particleMappings.containsKey(itemIdString)) {
                ParticleConfig.ParticleMapping mapping = ParticleConfig.particleMappings.get(itemIdString);
                Log.debug("Found config mapping: " + mapping.particleType);
                ParticleOptions result = createParticleFromMapping(mapping);
                if (result != null) {
                    Log.debug("Successfully created particle from config mapping");
                    return result;
                } else {
                    Log.warn("Failed to create particle from config mapping");
                }
            }
        } else {
            Log.warn("ParticleConfig.particleMappings is null!");
        }

        // Check if this is a nichirin sword (fallback to legacy logic)
        boolean isKimetsunoyaibaSword = itemId.getNamespace().equals("kimetsunoyaiba") && itemId.getPath().startsWith("nichirinsword_");
        boolean isOurModSword = itemId.getNamespace().equals("kimetsunoyaibamultiplayer") && itemId.getPath().startsWith("nichirinsword_");

        if (!isKimetsunoyaibaSword && !isOurModSword) {
            return null;
        }

        // Extract the sword type (part after "nichirinsword_")
        String swordType = itemId.getPath();

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
            Log.debug("Creating particle from mapping: " + mapping.particleType + " (isDust: " + mapping.isDust + ")");
            ResourceLocation particleId = ResourceLocation.parse(mapping.particleType);

            if (mapping.isDust) {
                // Create dust particle with custom size and color
                Vector3f color = new Vector3f(mapping.red, mapping.green, mapping.blue);
                Log.debug("Creating dust particle with color (" + mapping.red + ", " + mapping.green + ", " + mapping.blue + ") size " + mapping.size);
                DustParticleOptions dustOptions = new DustParticleOptions(color, mapping.size);
                Log.debug("Successfully created dust particle options");
                return dustOptions;
            } else {
                // Try to get the particle from the registry
                Log.debug("Looking for particle in registry: " + particleId);
                if (BuiltInRegistries.PARTICLE_TYPE.containsKey(particleId)) {
                    var particleType = BuiltInRegistries.PARTICLE_TYPE.get(particleId);
                    Log.debug("Found particle type in registry: " + particleType);
                    return (ParticleOptions) particleType;
                } else {
                    Log.warn("Particle not found in registry: " + particleId);
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

        // First check if this is a registered sword
        if (com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.isRegistered(item.getItem())) {
            return true;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item.getItem());
        String itemIdString = itemId.toString();

        // Check config-based mappings
        if (ParticleConfig.particleMappings != null && ParticleConfig.particleMappings.containsKey(itemIdString)) {
            return true;
        }

        // Check if this is a kimetsunoyaiba nichirin sword or our mod's breathing swords
        // Note: "nichirinsword" (base, no suffix) is also a valid sword
        String path = itemId.getPath();
        return (itemId.getNamespace().equals("kimetsunoyaiba") && (path.equals("nichirinsword") || path.startsWith("nichirinsword_"))) ||
               (itemId.getNamespace().equals("kimetsunoyaibamultiplayer") && path.startsWith("nichirinsword_"));
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

    /**
     * Checks if an item should be exempt from sword sheath/hip display
     * Some weapons like Himejima's axe and ball shouldn't render in the sheath
     * @param item The ItemStack to check
     * @return true if this item should NOT be displayed in the sword sheath
     */
    public static boolean isSheathExempt(ItemStack item) {
        if (item.isEmpty()) {
            return true;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item.getItem());
        String itemPath = itemId.getPath();

        return SHEATH_EXEMPT_ITEMS.contains(itemPath);
    }
}