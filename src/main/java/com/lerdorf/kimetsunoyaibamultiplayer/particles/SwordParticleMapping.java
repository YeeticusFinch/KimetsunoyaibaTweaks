package com.lerdorf.kimetsunoyaibamultiplayer.particles;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.BreathingSlayerEntity;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.api.SwordMetadataRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.config.ParticleConfig;
import com.mojang.logging.LogUtils;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class SwordParticleMapping {
    // Items exempt from sword sheath display (e.g., Himejima's axe and ball)
    private static final Set<String> SHEATH_EXEMPT_ITEMS = new HashSet<>();

    static {
        // Himejima's weapons should not render in sword sheath
        SHEATH_EXEMPT_ITEMS.add("nichirinsword_himejima_1");
        SHEATH_EXEMPT_ITEMS.add("nichirinsword_himejima_2");
        // Our stone variants should behave the same way (no sheath / no hip-back display)
        SHEATH_EXEMPT_ITEMS.add("nichirinsword_stone1");
        SHEATH_EXEMPT_ITEMS.add("nichirinsword_stone2");
    }
    //private static final Log Log = LogUtils.getLog();

    private static final Map<String, ResourceLocation> SWORD_TO_PARTICLE_MAP = new HashMap<>();
    private static final Map<String, List<ResourceLocation>> STYLE_TO_SECONDARY_PARTICLES = Map.of(
        "flame_breathing", List.of(
            ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "particle_flame"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "flame")),
        "thunder_breathing", List.of(
            ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "particle_lightning"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "electric_spark")),
        "sound_breathing", List.of(
            ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "particle_spark_fire")),
        "water_breathing", List.of(
            ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "water")),
        "flower_breathing", List.of(
            ResourceLocation.fromNamespaceAndPath("minecraft", "cherry_leaves")),
        "moon_breathing", List.of(
            ResourceLocation.fromNamespaceAndPath("minecraft", "end_rod")));
    private static final ParticleOptions MIST_COLORED_DUST_PARTICLE = new DustParticleOptions(
        new Vector3f(138.0F / 255.0F, 195.0F / 255.0F, 194.0F / 255.0F), 0.5F);
    private static final ParticleOptions MOON_ENERGY_PARTICLE = new EnergyParticleOptions(255, 169, 250, 1.0F);
    private static final ParticleOptions DEMONIZED_SERPENT_PARTICLE =
        new DustParticleOptions(new Vector3f(0.0F, 0.0F, 0.0F), 1.0F);

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
        ResourceLocation beastParticle = ResourceLocation.fromNamespaceAndPath("minecraft", "crit");
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

        // Our mod's variant swords (map to same particles as base mod equivalents)
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_snake", serpentParticle);
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_stone1", stoneParticle);
        SWORD_TO_PARTICLE_MAP.put("nichirinsword_stone2", stoneParticle);

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
        return getParticleForSword(swordItem, false);
    }

    public static ParticleOptions getParticleForSword(ItemStack swordItem, LivingEntity wielder) {
        return getParticleForSword(swordItem, isDemonizedWielder(wielder));
    }

    /**
     * Selects a particle for a slash trail. Two out of three outcomes use the
     * sword's primary particle; the remaining outcome randomly selects one of
     * the secondary particles registered for its breathing style.
     */
    public static ParticleOptions getParticleForSwordTrail(ItemStack swordItem, LivingEntity wielder) {
        String styleId = getBreathingStyleId(swordItem);
        ParticleOptions primary = "moon_breathing".equals(styleId)
            ? MOON_ENERGY_PARTICLE
            : getParticleForSword(swordItem, wielder);
        if (primary == null) {
            return null;
        }

        if ("mist_breathing".equals(styleId)) {
            primary = (ParticleOptions) ModParticles.SMALL_MIST_PARTICLE.get();
            if (ThreadLocalRandom.current().nextInt(3) < 2) {
                return primary;
            }
            return MIST_COLORED_DUST_PARTICLE;
        }

        List<ResourceLocation> secondaryIds = "moon_breathing".equals(styleId)
            ? List.of(getMoonSecondaryParticleId(swordItem))
            : getSecondaryParticleIds(swordItem);
        if (secondaryIds.isEmpty() || ThreadLocalRandom.current().nextInt(3) < 2) {
            return primary;
        }

        ResourceLocation secondaryId = secondaryIds.get(ThreadLocalRandom.current().nextInt(secondaryIds.size()));
        ParticleOptions secondary = getParticleOption(secondaryId);
        return secondary != null ? secondary : primary;
    }

    private static ResourceLocation getMoonSecondaryParticleId(ItemStack swordItem) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(swordItem.getItem());
        if (itemId != null) {
            if ("sword_kokushibo_1".equals(itemId.getPath())) {
                return ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "particle_moon_1");
            }
            if ("sword_kokushibo_2".equals(itemId.getPath())) {
                return ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "particle_moon_2");
            }
        }
        return ResourceLocation.fromNamespaceAndPath("minecraft", "end_rod");
    }

    public static ParticleOptions getParticleForSword(ItemStack swordItem, boolean demonizedWielder) {
        if (swordItem.isEmpty()) {
            return null;
        }

        // Moon-style base-mod swords do not use the nichirinsword_ naming
        // convention, so resolve their shared primary particle first.
        if ("moon_breathing".equals(getBreathingStyleId(swordItem))) {
            return MOON_ENERGY_PARTICLE;
        }

        if (demonizedWielder && shouldUseDemonizedSerpentParticle(swordItem)) {
            Log.debug("Using demonized serpent particle for sword: " + BuiltInRegistries.ITEM.getKey(swordItem.getItem()));
            return DEMONIZED_SERPENT_PARTICLE;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(swordItem.getItem());
        String itemIdString = itemId.toString();

        // First, check if this sword is registered in the SwordRegistry
        var registeredSword = SwordRegistry.getSword(swordItem.getItem());
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

    private static List<ResourceLocation> getSecondaryParticleIds(ItemStack swordItem) {
        String styleId = getBreathingStyleId(swordItem);
        return styleId == null ? List.of() : STYLE_TO_SECONDARY_PARTICLES.getOrDefault(styleId, List.of());
    }

    private static String getBreathingStyleId(ItemStack swordItem) {
        if (swordItem == null || swordItem.isEmpty()) {
            return null;
        }

        SwordRegistry.RegisteredSword registeredSword = SwordRegistry.getSword(swordItem.getItem());
        if (registeredSword != null && registeredSword.getStyleId() != null) {
            return registeredSword.getStyleId();
        }

        SwordMetadataRegistry.SwordMetadata metadata = SwordMetadataRegistry.getMetadata(swordItem.getItem());
        if (metadata != null && metadata.getStyleId() != null) {
            return metadata.getStyleId();
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(swordItem.getItem());
        if (itemId == null) {
            return null;
        }

        String path = itemId.getPath().toLowerCase(Locale.ROOT);
        if (path.contains("flame") || path.contains("rengoku")) return "flame_breathing";
        if (path.contains("thunder") || path.contains("zenitsu") || path.contains("kaigaku")) return "thunder_breathing";
        if (path.contains("water") || path.contains("tomioka") || path.contains("tanjiro")) return "water_breathing";
        if (path.contains("flower") || path.contains("kanae") || path.contains("kanawo")) return "flower_breathing";
        if (path.contains("moon") || path.contains("kokushibo")) return "moon_breathing";
        return null;
    }

    private static ParticleOptions getParticleOption(ResourceLocation particleId) {
        if (!BuiltInRegistries.PARTICLE_TYPE.containsKey(particleId)) {
            return null;
        }
        return (ParticleOptions) BuiltInRegistries.PARTICLE_TYPE.get(particleId);
    }

    public static boolean isSerpentBreathingSword(ItemStack swordItem) {
        if (swordItem == null || swordItem.isEmpty()) {
            return false;
        }

        SwordRegistry.RegisteredSword registeredSword = SwordRegistry.getSword(swordItem.getItem());
        if (registeredSword != null && "serpent_breathing".equals(registeredSword.getStyleId())) {
            return true;
        }

        SwordMetadataRegistry.SwordMetadata metadata = SwordMetadataRegistry.getMetadata(swordItem.getItem());
        if (metadata != null && "serpent_breathing".equals(metadata.getStyleId())) {
            return true;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(swordItem.getItem());
        if (itemId == null) {
            return false;
        }

        String path = itemId.getPath().toLowerCase(Locale.ROOT);
        if (!path.startsWith("nichirinsword")) {
            return false;
        }

        return path.contains("serpent") || path.contains("iguro") || path.contains("snake");
    }

    private static boolean shouldUseDemonizedSerpentParticle(ItemStack swordItem) {
        return Config.demonizedBreathingStyles && isSerpentBreathingSword(swordItem);
    }

    private static boolean isDemonizedWielder(LivingEntity wielder) {
        if (wielder == null) {
            return false;
        }
        return Damager.isDemon(wielder)
            || (wielder instanceof BreathingSlayerEntity slayer && slayer.isDemonized());
    }

    /**
     * Creates a ParticleOptions from a config-based particle mapping
     * @param mapping The particle mapping from config
     * @return ParticleOptions for the particle, or null if invalid
     */
    private static ParticleOptions createParticleFromMapping(ParticleConfig.ParticleMapping mapping) {
        try {
            Log.debug("Creating particle from mapping: " + mapping.particleType + " (isDust: " + mapping.isDust + ", isEnergy: " + mapping.isEnergy + ")");
            ResourceLocation particleId = ResourceLocation.parse(mapping.particleType);

            if (mapping.isDust) {
                // Create dust particle with custom size and color
                Vector3f color = new Vector3f(mapping.red, mapping.green, mapping.blue);
                Log.debug("Creating dust particle with color (" + mapping.red + ", " + mapping.green + ", " + mapping.blue + ") size " + mapping.size);
                DustParticleOptions dustOptions = new DustParticleOptions(color, mapping.size);
                Log.debug("Successfully created dust particle options");
                return dustOptions;
            } else if (mapping.isEnergy) {
                // Create energy particle with custom size and color
                Vector3f color = new Vector3f(mapping.red, mapping.green, mapping.blue);
                Log.debug("Creating energy particle with color (" + mapping.red + ", " + mapping.green + ", " + mapping.blue + ") size " + mapping.size);
                EnergyParticleOptions energyOptions = new EnergyParticleOptions(color, mapping.size);
                Log.debug("Successfully created energy particle options");
                return energyOptions;
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
        return (itemId.getNamespace().equals("kimetsunoyaiba") && (path.equals("nichirinsword") || path.startsWith("nichirinsword_") || path.startsWith("sword_kokushibo") || path.equals("sword_hairo") || path.equals("saber"))) ||
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
