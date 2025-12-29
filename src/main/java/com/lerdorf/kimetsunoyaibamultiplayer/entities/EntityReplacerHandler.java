package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedSpawnConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles entity replacement system.
 *
 * Replaces kimetsunoyaiba mod entities with kimetsu mod entities when configured.
 * This allows users to use higher-quality entity models from the kimetsu mod
 * while maintaining compatibility with the kimetsunoyaiba mod's spawning system.
 *
 * Replacements are configurable and only occur if the kimetsu mod is loaded.
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EntityReplacerHandler {

    private static boolean kimetsuModLoaded = false;

    // Standard replacements (kimetsu_replacer config)
    private static final Map<ResourceLocation, ResourceLocation> STANDARD_REPLACEMENTS = new HashMap<>();

    // Movie replacements (kimetsu_movie_replacer config)
    private static final Map<ResourceLocation, ResourceLocation> MOVIE_REPLACEMENTS = new HashMap<>();

    static {
        // Check if kimetsu mod is loaded
        kimetsuModLoaded = ModList.get().isLoaded("kimetsu");

        if (kimetsuModLoaded) {
            // Standard replacements
            STANDARD_REPLACEMENTS.put(
                ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "nakime"),
                ResourceLocation.fromNamespaceAndPath("kimetsu", "nakime")
            );

            // Movie replacements
            MOVIE_REPLACEMENTS.put(
                ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "nakime"),
                ResourceLocation.fromNamespaceAndPath("kimetsu", "nakime_real")
            );
            MOVIE_REPLACEMENTS.put(
                ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "daki"),
                ResourceLocation.fromNamespaceAndPath("kimetsu", "daki")
            );
            MOVIE_REPLACEMENTS.put(
                ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "gyutaro"),
                ResourceLocation.fromNamespaceAndPath("kimetsu", "gyutaro")
            );
            MOVIE_REPLACEMENTS.put(
                ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "kaigaku"),
                ResourceLocation.fromNamespaceAndPath("kimetsu", "kaigaku")
            );
            MOVIE_REPLACEMENTS.put(
                ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "gyokko"),
                ResourceLocation.fromNamespaceAndPath("kimetsu", "gyokko")
            );
            MOVIE_REPLACEMENTS.put(
                ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "zohakuten"),
                ResourceLocation.fromNamespaceAndPath("kimetsu", "zohakuten")
            );
            MOVIE_REPLACEMENTS.put(
                ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "akaza"),
                ResourceLocation.fromNamespaceAndPath("kimetsu", "akaza")
            );
            MOVIE_REPLACEMENTS.put(
                ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "doma"),
                ResourceLocation.fromNamespaceAndPath("kimetsu", "doma")
            );
            MOVIE_REPLACEMENTS.put(
                ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "kokushibo"),
                ResourceLocation.fromNamespaceAndPath("kimetsu", "kokushibo")
            );
            MOVIE_REPLACEMENTS.put(
                ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "himejima"),
                ResourceLocation.fromNamespaceAndPath("kimetsu", "himejima")
            );
            MOVIE_REPLACEMENTS.put(
                ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "kocho"),
                ResourceLocation.fromNamespaceAndPath("kimetsu", "slayer_kocho")
            );
        }
    }

    /**
     * Handle entity spawning and replace if configured
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        // Only run on server side
        if (event.getLevel().isClientSide()) {
            return;
        }

        // Check if kimetsu mod is loaded
        if (!kimetsuModLoaded) {
            return;
        }

        Entity entity = event.getEntity();
        if (entity == null) {
            return;
        }

        // Get entity type ID
        ResourceLocation entityTypeId = EntityType.getKey(entity.getType());
        if (entityTypeId == null || !entityTypeId.getNamespace().equals("kimetsunoyaiba")) {
            return;
        }

        // Don't replace entities spawned by commands or spawn eggs
        // We check this by seeing if the entity has a custom spawn reason
        if (entity.getPersistentData().contains("SpawnReason")) {
            String spawnReason = entity.getPersistentData().getString("SpawnReason");
            if (spawnReason.equals("COMMAND") || spawnReason.equals("SPAWN_EGG")) {
                return;
            }
        }

        // Determine which replacement map to use
        ResourceLocation replacementId = null;

        // Check movie replacements first (if enabled and entity-specific toggle is on)
        if (EnhancedSpawnConfig.enableKimetsuMovieReplacer && MOVIE_REPLACEMENTS.containsKey(entityTypeId)) {
            String entityName = entityTypeId.getPath();
            if (EnhancedSpawnConfig.isEntityReplacementEnabled(entityName)) {
                replacementId = MOVIE_REPLACEMENTS.get(entityTypeId);
            }
        }

        // Check standard replacements (if enabled and entity-specific toggle is on)
        if (replacementId == null && EnhancedSpawnConfig.enableKimetsuReplacer && STANDARD_REPLACEMENTS.containsKey(entityTypeId)) {
            String entityName = entityTypeId.getPath();
            if (EnhancedSpawnConfig.isEntityReplacementEnabled(entityName)) {
                replacementId = STANDARD_REPLACEMENTS.get(entityTypeId);
            }
        }

        // No replacement needed
        if (replacementId == null) {
            return;
        }

        // Get the replacement entity type
        EntityType<?> replacementType = BuiltInRegistries.ENTITY_TYPE.get(replacementId);
        if (replacementType == null) {
            System.err.println("[Entity Replacer] Could not find replacement entity type: " + replacementId);
            return;
        }

        try {
            // Create the replacement entity
            Entity replacementEntity = replacementType.create(event.getLevel());
            if (replacementEntity == null) {
                System.err.println("[Entity Replacer] Failed to create replacement entity: " + replacementId);
                return;
            }

            // Copy position and rotation
            replacementEntity.moveTo(
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                entity.getYRot(),
                entity.getXRot()
            );

            // Copy velocity
            replacementEntity.setDeltaMovement(entity.getDeltaMovement());

            // Copy some basic NBT data (health, attributes, etc.)
            CompoundTag oldNbt = new CompoundTag();
            entity.saveWithoutId(oldNbt);

            CompoundTag newNbt = new CompoundTag();
            replacementEntity.saveWithoutId(newNbt);

            // Copy health if available
            if (oldNbt.contains("Health")) {
                newNbt.putFloat("Health", oldNbt.getFloat("Health"));
            }

            // Load the copied data
            replacementEntity.load(newNbt);

            // Transfer raid metadata if this is a raid entity
            CompoundTag oldPersistentData = entity.getPersistentData();
            if (oldPersistentData.contains("RaidId")) {
                UUID raidId = oldPersistentData.getUUID("RaidId");
                String raidType = oldPersistentData.getString("RaidType");

                // Tag the replacement entity with the same raid metadata
                CompoundTag newPersistentData = replacementEntity.getPersistentData();
                newPersistentData.putUUID("RaidId", raidId);
                newPersistentData.putString("RaidType", raidType);

                // Notify raid system to transfer tracking
                if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    // Get the replacement entity's max health (if it's a LivingEntity)
                    float newMaxHealth = replacementEntity instanceof net.minecraft.world.entity.LivingEntity living
                        ? living.getMaxHealth()
                        : 100f; // Fallback value

                    // Transfer raid tracking from old UUID to new UUID
                    com.lerdorf.kimetsunoyaibamultiplayer.raids.RaidRegistry registry =
                        com.lerdorf.kimetsunoyaibamultiplayer.raids.RaidRegistry.get(serverLevel);
                    registry.replaceRaidEntity(raidId, entity.getUUID(), replacementEntity.getUUID(), newMaxHealth);

                    if (com.lerdorf.kimetsunoyaibamultiplayer.Config.logDebug) {
                        Log.debug("[Entity Replacer] Transferred raid tracking from " +
                                  entity.getUUID() + " to " + replacementEntity.getUUID());
                    }
                }
            }

            // Spawn the replacement entity
            event.getLevel().addFreshEntity(replacementEntity);

            // Remove the original entity
            entity.discard();

            // Cancel the original spawn event
            event.setCanceled(true);

            if (com.lerdorf.kimetsunoyaibamultiplayer.Config.logDebug) {
                Log.debug("[Entity Replacer] Replaced " + entityTypeId + " with " + replacementId);
            }

        } catch (Exception e) {
            System.err.println("[Entity Replacer] Error replacing entity: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get the replacement entity type ID for a given entity type ID (if any)
     */
    public static ResourceLocation getReplacementId(ResourceLocation entityTypeId) {
        if (!kimetsuModLoaded) {
            return null;
        }

        // Check movie replacements first
        if (EnhancedSpawnConfig.enableKimetsuMovieReplacer && MOVIE_REPLACEMENTS.containsKey(entityTypeId)) {
            String entityName = entityTypeId.getPath();
            if (EnhancedSpawnConfig.isEntityReplacementEnabled(entityName)) {
                return MOVIE_REPLACEMENTS.get(entityTypeId);
            }
        }

        // Check standard replacements
        if (EnhancedSpawnConfig.enableKimetsuReplacer && STANDARD_REPLACEMENTS.containsKey(entityTypeId)) {
            String entityName = entityTypeId.getPath();
            if (EnhancedSpawnConfig.isEntityReplacementEnabled(entityName)) {
                return STANDARD_REPLACEMENTS.get(entityTypeId);
            }
        }

        return null;
    }

    /**
     * Check if an entity type has a replacement configured
     */
    public static boolean hasReplacement(ResourceLocation entityTypeId) {
        return getReplacementId(entityTypeId) != null;
    }
}
