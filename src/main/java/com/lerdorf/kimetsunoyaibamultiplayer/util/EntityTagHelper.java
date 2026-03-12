package com.lerdorf.kimetsunoyaibamultiplayer.util;

import com.lerdorf.kimetsunoyaibamultiplayer.raids.EntityCategorization;
import com.lerdorf.kimetsunoyaibamultiplayer.raids.EntityPowerScale;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for checking entity tags efficiently.
 *
 * Provides cached tag keys and convenient methods for entity tag checking.
 * Used throughout the spawning rules system to identify entity types.
 */
public class EntityTagHelper {

    // Cache tag keys for performance
    private static final Map<String, TagKey<EntityType<?>>> TAG_CACHE = new HashMap<>();

    // Kimetsunoyaiba mod tags
    public static final TagKey<EntityType<?>> DEMON = getOrCreateTag("kimetsunoyaiba", "demon");
    public static final TagKey<EntityType<?>> TWELVE_KIZUKI = getOrCreateTag("kimetsunoyaiba", "twelve_kizuki");
    public static final TagKey<EntityType<?>> TAG_HASHIRA = getOrCreateTag("kimetsunoyaiba", "tag_hashira");
    public static final TagKey<EntityType<?>> KAMABOKO = getOrCreateTag("kimetsunoyaiba", "kamaboko");

    // Custom tags added by this mod
    public static final TagKey<EntityType<?>> DEMON_SLAYER = getOrCreateTag("kimetsunoyaibamultiplayer", "demon_slayer");
    public static final TagKey<EntityType<?>> FORMER_HASHIRA = getOrCreateTag("kimetsunoyaibamultiplayer", "former_hashira");
    public static final TagKey<EntityType<?>> SWORD_SMITH = getOrCreateTag("kimetsunoyaibamultiplayer", "sword_smith");
    public static final TagKey<EntityType<?>> CIVILIAN = getOrCreateTag("kimetsunoyaibamultiplayer", "civilian");
    public static final TagKey<EntityType<?>> ANIMAL = getOrCreateTag("kimetsunoyaibamultiplayer", "animal");

    /**
     * Get or create a tag key and cache it
     */
    private static TagKey<EntityType<?>> getOrCreateTag(String namespace, String path) {
        String key = namespace + ":" + path;
        if (!TAG_CACHE.containsKey(key)) {
            TAG_CACHE.put(key, TagKey.create(Registries.ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(namespace, path)));
        }
        return TAG_CACHE.get(key);
    }

    /**
     * Check if an entity is a demon
     */
    public static boolean isDemon(Entity entity) {
        if (entity == null) return false;
        return entity.getType().is(DEMON);
    }

    /**
     * Check if an entity is a twelve kizuki demon
     */
    public static boolean isTwelveKizuki(Entity entity) {
        if (entity == null) return false;
        return entity.getType().is(TWELVE_KIZUKI);
    }

    /**
     * Check if an entity is a demon slayer (includes hashira, kamaboko, and regular demon slayers)
     */
    public static boolean isDemonSlayer(Entity entity) {
        if (entity == null) return false;
        if (entity.getType().is(DEMON_SLAYER)) {
            return true;
        }

        if (entity.getType().is(FORMER_HASHIRA)) {
            return true;
        }

        EntityPowerScale scale = getPowerScale(entity);
        return scale != null && scale.isSlayerScale();
    }

    /**
     * Check if an entity is a hashira
     */
    public static boolean isHashira(Entity entity) {
        if (entity == null) return false;
        if (entity.getType().is(TAG_HASHIRA)) {
            return true;
        }

        EntityPowerScale scale = getPowerScale(entity);
        return scale == EntityPowerScale.HASHIRA || scale == EntityPowerScale.SUPER_HASHIRA;
    }

    /**
     * Check if an entity is a former hashira (retired)
     */
    public static boolean isFormerHashira(Entity entity) {
        if (entity == null) return false;
        return entity.getType().is(FORMER_HASHIRA);
    }

    /**
     * Check if an entity is a kamaboko squad member
     */
    public static boolean isKamaboko(Entity entity) {
        if (entity == null) return false;
        if (entity.getType().is(KAMABOKO)) {
            return true;
        }

        ResourceLocation entityId = getEntityTypeId(entity);
        if (entityId == null) {
            return false;
        }

        String namespace = entityId.getNamespace();
        String path = entityId.getPath();
        return ("kimetsunoyaiba".equals(namespace) || "kimetsunoyaibamultiplayer".equals(namespace)) &&
            (path.equals("genya") || path.equals("tanjiro") || path.equals("zennitsu") ||
                path.equals("kanawo") || path.equals("inosuke"));
    }

    /**
     * Check if an entity is a named demon slayer that should not naturally spawn
     * outside of its designated biome/structure rules.
     */
    public static boolean isNamedDemonSlayer(Entity entity) {
        if (entity == null) return false;

        EntityPowerScale scale = getPowerScale(entity);
        return scale == EntityPowerScale.NAMED_SLAYER;
    }

    /**
     * Check if an entity is a civilian
     */
    public static boolean isCivilian(Entity entity) {
        if (entity == null) return false;
        return entity.getType().is(CIVILIAN);
    }

    /**
     * Check if an entity is a sword smith
     */
    public static boolean isSwordSmith(Entity entity) {
        if (entity == null) return false;
        return entity.getType().is(SWORD_SMITH);
    }

    /**
     * Check if an entity is an animal from the KnY mod
     */
    public static boolean isKnYAnimal(Entity entity) {
        if (entity == null) return false;
        return entity.getType().is(ANIMAL);
    }

    /**
     * Check if an entity is in a specific biome
     */
    public static boolean isInBiome(Entity entity, ResourceLocation biomeId) {
        if (entity == null || entity.level() == null) return false;

        Holder<Biome> biome = entity.level().getBiome(entity.blockPosition());
        return biome.is(ResourceLocation.fromNamespaceAndPath(biomeId.getNamespace(), biomeId.getPath()));
    }

    /**
     * Check if an entity is in a specific biome (by namespace and path)
     */
    public static boolean isInBiome(Entity entity, String namespace, String path) {
        return isInBiome(entity, ResourceLocation.fromNamespaceAndPath(namespace, path));
    }

    /**
     * Get the biome ID of the entity's current location
     */
    public static ResourceLocation getCurrentBiome(Entity entity) {
        if (entity == null || entity.level() == null) return null;

        Holder<Biome> biome = entity.level().getBiome(entity.blockPosition());
        return biome.unwrapKey().map(key -> key.location()).orElse(null);
    }

    /**
     * Check if an entity has a specific entity type
     */
    public static boolean isEntityType(Entity entity, ResourceLocation entityTypeId) {
        if (entity == null) return false;

        ResourceLocation entityId = EntityType.getKey(entity.getType());
        return entityId.equals(entityTypeId);
    }

    /**
     * Check if an entity has a specific entity type (by namespace and path)
     */
    public static boolean isEntityType(Entity entity, String namespace, String path) {
        return isEntityType(entity, ResourceLocation.fromNamespaceAndPath(namespace, path));
    }

    /**
     * Get the entity type ID as a ResourceLocation
     */
    public static ResourceLocation getEntityTypeId(Entity entity) {
        if (entity == null) return null;
        return EntityType.getKey(entity.getType());
    }

    /**
     * Check if an entity is from the kimetsunoyaiba mod
     */
    public static boolean isFromKnYMod(Entity entity) {
        if (entity == null) return false;
        ResourceLocation entityId = getEntityTypeId(entity);
        return entityId != null && entityId.getNamespace().equals("kimetsunoyaiba");
    }

    /**
     * Check if an entity is from the kimetsu mod
     */
    public static boolean isFromKimetsuMod(Entity entity) {
        if (entity == null) return false;
        ResourceLocation entityId = getEntityTypeId(entity);
        return entityId != null && entityId.getNamespace().equals("kimetsu");
    }

    /**
     * Check if an entity matches a specific entity type by checking both the entity
     * and its potential replacer entities
     */
    public static boolean isEntityOrReplacer(Entity entity, String... entityPaths) {
        if (entity == null) return false;

        ResourceLocation entityId = getEntityTypeId(entity);
        if (entityId == null) return false;

        String path = entityId.getPath();
        for (String checkPath : entityPaths) {
            if (path.equals(checkPath)) {
                return true;
            }
        }

        return false;
    }

    private static EntityPowerScale getPowerScale(Entity entity) {
        ResourceLocation entityId = getEntityTypeId(entity);
        if (entityId == null) {
            return null;
        }

        return EntityCategorization.getEntityPowerScale(entityId);
    }
}
