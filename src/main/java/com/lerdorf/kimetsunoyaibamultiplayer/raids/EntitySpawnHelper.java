package com.lerdorf.kimetsunoyaibamultiplayer.raids;

import com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedBreathingConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.config.RaidConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.fml.ModList;

import javax.annotation.Nullable;

/**
 * Helper class for entity spawning in raids, civilian protection, and favor effects.
 *
 * Handles:
 * - Enhanced breathing entity replacement (kanroji, muichiro)
 * - Kimetsu addon entity filtering based on config
 * - Entity namespace detection
 * - Spawning entities with automatic kimetsu spawn egg fallback
 */
public class EntitySpawnHelper {

    private static Boolean kimetsuModLoaded = null;

    /**
     * Check if the kimetsu addon mod is loaded.
     */
    public static boolean isKimetsuModLoaded() {
        if (kimetsuModLoaded == null) {
            kimetsuModLoaded = ModList.get().isLoaded("kimetsu");
        }
        return kimetsuModLoaded;
    }

    /**
     * Replace entity IDs based on enhanced breathing configuration.
     *
     * Returns the appropriate entity ID based on enhanced breathing settings:
     * - If enhanced mist breathing is enabled: muichirou -> muichiro (multiplayer)
     * - If enhanced love breathing is enabled: kanroji -> kanroji (multiplayer)
     *
     * @param entityId The original entity ID
     * @return The potentially replaced entity ID
     */
    public static ResourceLocation applyEnhancedBreathingReplacement(ResourceLocation entityId) {
        if (entityId == null) {
            return null;
        }

        String namespace = entityId.getNamespace();
        String path = entityId.getPath();

        // Only replace kimetsunoyaiba namespace entities
        if (!namespace.equals("kimetsunoyaiba")) {
            return entityId;
        }

        // Enhanced Mist Breathing: muichirou -> muichiro (multiplayer)
        if (path.equals("muichirou") && EnhancedBreathingConfig.enhancedMistBreathing) {
            return ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "muichiro");
        }

        // Enhanced Love Breathing: kanroji -> kanroji (multiplayer)
        if (path.equals("kanroji") && EnhancedBreathingConfig.enhancedLoveBreathing) {
            return ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "kanroji");
        }

        return entityId;
    }

    /**
     * Check if an entity should be allowed based on raid configuration.
     *
     * Filters entities based on:
     * - Kimetsu addon config (if entity is from kimetsu namespace)
     * - Yoriichi restrictions
     *
     * @param entityId The entity ID to check
     * @return true if the entity should be allowed to spawn
     */
    public static boolean isEntityAllowedInRaid(ResourceLocation entityId) {
        if (entityId == null) {
            return false;
        }

        String namespace = entityId.getNamespace();
        String path = entityId.getPath();

        // Check kimetsu addon entities
        if (namespace.equals("kimetsu")) {
            // Kimetsu entities are only allowed if:
            // 1. The kimetsu mod is loaded
            // 2. The config option is enabled
            if (!isKimetsuModLoaded() || !RaidConfig.enableKimetsuAddonEntities.get()) {
                return false;
            }
        }

        // Check Yoriichi restrictions
        if (namespace.equals("kimetsunoyaiba") && path.equals("yoriichi")) {
            if (RaidConfig.disableYoriichiInRaids.get()) {
                return false;
            }
        }

        if (namespace.equals("kimetsunoyaiba") && path.equals("yoriichi_old")) {
            if (RaidConfig.disableYoriichiOldInRaids.get()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Get a spawn-ready entity ID, applying both enhanced breathing replacement
     * and raid configuration filtering.
     *
     * @param entityId The original entity ID
     * @return The spawn-ready entity ID, or null if the entity should not spawn
     */
    public static ResourceLocation getSpawnReadyEntityId(ResourceLocation entityId) {
        if (entityId == null) {
            return null;
        }

        // Apply enhanced breathing replacement first
        ResourceLocation replacedId = applyEnhancedBreathingReplacement(entityId);

        // Check if the entity is allowed
        if (!isEntityAllowedInRaid(replacedId)) {
            return null;
        }

        return replacedId;
    }

    /**
     * Filter a resource location to only allow spawning if the entity is allowed.
     *
     * This is useful for civilian protection and favor effects where we want
     * to apply the same filtering logic as raids.
     *
     * @param entityId The entity ID
     * @return The entity ID if allowed, null otherwise
     */
    public static ResourceLocation filterForProtectiveSpawning(ResourceLocation entityId) {
        if (entityId == null) {
            return null;
        }

        // Apply enhanced breathing replacement
        ResourceLocation replacedId = applyEnhancedBreathingReplacement(entityId);

        // For civilian protection and favor effects, we don't apply the Yoriichi restrictions
        // but we do check kimetsu addon availability
        String namespace = replacedId.getNamespace();
        if (namespace.equals("kimetsu") && !isKimetsuModLoaded()) {
            return null;
        }

        return replacedId;
    }

    /**
     * Spawn an entity at the given position, using spawn eggs for kimetsu entities if needed.
     *
     * This method automatically handles:
     * - Enhanced breathing replacement
     * - Raid config filtering
     * - Kimetsu spawn egg fallback
     *
     * @param level Server level
     * @param pos Position to spawn at
     * @param entityId Original entity ID
     * @return The spawned mob, or null if spawning failed or entity is not allowed
     */
    @Nullable
    public static Mob spawnEntity(ServerLevel level, BlockPos pos, ResourceLocation entityId) {
        // Apply enhanced breathing replacement and filtering
        ResourceLocation spawnReadyId = getSpawnReadyEntityId(entityId);
        if (spawnReadyId == null) {
            return null;
        }

        // Check if this is a kimetsu entity - use spawn egg spawner for reliability
        if (spawnReadyId.getNamespace().equals("kimetsu")) {
            return KimetsuEntitySpawner.spawnKimetsuEntity(level, pos, spawnReadyId);
        }

        // For non-kimetsu entities, use direct spawning
        try {
            EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(spawnReadyId);
            if (entityType == null) {
                return null;
            }

            Entity entity = entityType.create(level);
            if (!(entity instanceof Mob mob)) {
                return null;
            }

            mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                       level.random.nextFloat() * 360F, 0);
            mob.setPersistenceRequired();

            if (level.addFreshEntity(mob)) {
                return mob;
            }

            return null;
        } catch (Exception e) {
            System.err.println("[Entity Spawn Helper] Failed to spawn " + spawnReadyId + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Spawn an entity for protective purposes (civilian protection, favor effects).
     *
     * Same as spawnEntity, but uses filterForProtectiveSpawning instead of getSpawnReadyEntityId.
     *
     * @param level Server level
     * @param pos Position to spawn at
     * @param entityId Original entity ID
     * @return The spawned mob, or null if spawning failed
     */
    @Nullable
    public static Mob spawnProtectiveEntity(ServerLevel level, BlockPos pos, ResourceLocation entityId) {
        // Apply enhanced breathing replacement for protective spawning
        ResourceLocation replacedId = filterForProtectiveSpawning(entityId);
        if (replacedId == null) {
            return null;
        }

        // Check if this is a kimetsu entity - use spawn egg spawner for reliability
        if (replacedId.getNamespace().equals("kimetsu")) {
            return KimetsuEntitySpawner.spawnKimetsuEntity(level, pos, replacedId);
        }

        // For non-kimetsu entities, use direct spawning
        try {
            EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(replacedId);
            if (entityType == null) {
                return null;
            }

            Entity entity = entityType.create(level);
            if (!(entity instanceof Mob mob)) {
                return null;
            }

            mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                       level.random.nextFloat() * 360F, 0);
            mob.setPersistenceRequired();

            if (level.addFreshEntity(mob)) {
                return mob;
            }

            return null;
        } catch (Exception e) {
            System.err.println("[Entity Spawn Helper] Failed to spawn protective entity " + replacedId + ": " + e.getMessage());
            return null;
        }
    }
}
