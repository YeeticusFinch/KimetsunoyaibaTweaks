package com.lerdorf.kimetsunoyaibamultiplayer.particles;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

public class SwordParticleMapping {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<String, ResourceLocation> SWORD_TO_PARTICLE_MAP = new HashMap<>();

    static {
        // Initialize hardcoded mappings for specific sword types
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_thunder", ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "particle_thunder"));
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_water", ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "particle_water"));
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_flame", ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "particle_flame"));
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_stone", ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "particle_stone"));
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_wind", ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "particle_wind"));
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_sun", ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "particle_sun"));
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_moon", ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "particle_moon"));
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
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_generic", ResourceLocation.fromNamespaceAndPath("minecraft", "enchanted_hit"));
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

        // Check if this is a kimetsunoyaiba nichirin sword
        if (!itemId.getNamespace().equals("kimetsunoyaiba") || !itemId.getPath().startsWith("nichirinsword_")) {
            return null;
        }

        // Extract the sword type (part after "nichirinsword_")
        String swordType = itemId.getPath();

        // Special case for wind sword - use green dust particles
        if (swordType.equals("nichirinsword_wind")) {
            // Create green dust particles (RGB: 0.0, 1.0, 0.2 for bright green)
            return new DustParticleOptions(new Vector3f(0.0f, 1.0f, 0.2f), 1.0f);
        }
        
     // Special case for thunder sword - use yellow dust particles
        if (swordType.equals("nichirinsword_thunder")) {
            // Create green dust particles (RGB: 1.0, 1.0, 0.2 for bright yellow)
            return new DustParticleOptions(new Vector3f(1.0f, 1.0f, 0.2f), 1.0f);
        }

        // Look up the particle mapping
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
        LOGGER.debug("No particle found for sword {}, using fallback particle", itemId);
        return ParticleTypes.ENCHANTED_HIT;
    }

    /**
     * Checks if an item is a kimetsunoyaiba nichirin sword
     * @param item The ItemStack to check
     * @return true if this is a nichirin sword that should have particle effects
     */
    public static boolean isKimetsunoyaibaSword(ItemStack item) {
        if (item.isEmpty()) {
            return false;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item.getItem());
        return itemId.getNamespace().equals("kimetsunoyaiba") && itemId.getPath().startsWith("nichirinsword_");
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
        LOGGER.info("Registered custom sword particle mapping: {} -> {}", swordType, particleId);
    }
}