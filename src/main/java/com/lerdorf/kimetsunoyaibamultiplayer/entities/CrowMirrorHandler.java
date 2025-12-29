package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Iterator;

/**
 * Handles spawning GeckolibCrowEntity mirrors when kasugai_crow entities spawn,
 * and manages damage/death event forwarding
 */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
public class CrowMirrorHandler {

    // Map from original crow UUID to mirror crow
    private static final Map<UUID, GeckolibCrowEntity> CROW_MIRRORS = new HashMap<>();

    // Cache of all known original crow UUIDs (for faster lookups)
    private static final Set<UUID> KNOWN_CROW_UUIDS = new HashSet<>();

    // Batching state for scanForUnmirroredCrows
    private static Iterator<Entity> currentScanIterator = null;
    private static int entitiesProcessedThisTick = 0;
    private static final int MAX_ENTITIES_PER_TICK = 50; // Process max 50 entities per scan tick

    @SubscribeEvent
    public static void onEntitySpawn(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();

        // Only process on server side
        if (entity.level().isClientSide()) {
            return;
        }

        // Check if this is a kasugai_crow
        if (!isKasugaiCrow(entity)) {
            return;
        }

        // Add to known crow UUIDs cache
        KNOWN_CROW_UUIDS.add(entity.getUUID());

        // Check if we already have a mirror for this crow
        if (CROW_MIRRORS.containsKey(entity.getUUID())) {
            // Check if the mirror still exists
            GeckolibCrowEntity existingMirror = CROW_MIRRORS.get(entity.getUUID());
            if (existingMirror != null && existingMirror.isAlive() && !existingMirror.isRemoved()) {
            	if (Config.logDebug)
                Log.info("Crow {} already has a valid mirror, skipping", entity.getUUID());
                return; // Already has a valid mirror
            } else {
                // Mirror is gone or invalid, remove from map and create new one
            	if (Config.logDebug)
                Log.warn("Crow {} had invalid mirror, removing and recreating", entity.getUUID());
                if (existingMirror != null && !existingMirror.isRemoved()) {
                    existingMirror.discard();
                }
                CROW_MIRRORS.remove(entity.getUUID());
            }
        }
        if (Config.logDebug)
        Log.info("Kasugai crow spawned! Creating GeckoLib mirror entity...");

        // Use the helper method to create the mirror
        createMirrorForCrow(entity, (ServerLevel) entity.level());
    }

    @SubscribeEvent
    public static void onEntityHurt(LivingHurtEvent event) {
        Entity entity = event.getEntity();

        // Check if this is a kasugai_crow
        if (!isKasugaiCrow(entity)) {
            return;
        }

        // Find the mirror crow
        GeckolibCrowEntity mirrorCrow = CROW_MIRRORS.get(entity.getUUID());
        if (mirrorCrow != null) {
            // Trigger hurt animation on mirror
            mirrorCrow.triggerHurt();
            if (Config.logDebug) {
                Log.info("Original crow hurt - triggered animation on mirror");
            }
        }
    }

    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        Entity entity = event.getEntity();

        // Check if this is a kasugai_crow
        if (!isKasugaiCrow(entity)) {
            return;
        }

        // Find the mirror crow
        GeckolibCrowEntity mirrorCrow = CROW_MIRRORS.get(entity.getUUID());
        if (mirrorCrow != null) {
            // Trigger death animation on mirror
            mirrorCrow.triggerDeath();
            if (Config.logDebug)
            Log.info("Original crow died - triggered death animation on mirror");

            // Schedule removal of mirror after death animation (about 2 seconds)
            // The mirror will auto-remove when it detects original is dead in its tick()
        }

        // Clean up from maps and cache
        CROW_MIRRORS.remove(entity.getUUID());
        KNOWN_CROW_UUIDS.remove(entity.getUUID());
    }

    /**
     * Check if an entity is a kasugai_crow from the kimetsunoyaiba mod
     */
    private static boolean isKasugaiCrow(Entity entity) {
        String entityType = entity.getType().toString();
        return entityType.contains("kasugai_crow");
    }

    /**
     * Get the mirror entity for a given original crow UUID
     */
    public static GeckolibCrowEntity getMirror(UUID originalCrowUUID) {
        return CROW_MIRRORS.get(originalCrowUUID);
    }

    /**
     * Get all mirror crows
     */
    public static java.util.Collection<GeckolibCrowEntity> getAllMirrors() {
        return CROW_MIRRORS.values();
    }

    /**
     * Scan for existing kasugai_crow entities that don't have mirrors.
     * Uses batching to avoid iterating all entities in a single tick.
     * Should be called periodically (e.g., every 5 seconds).
     *
     * PERFORMANCE FIX: Instead of calling level.getAllEntities() which can iterate
     * over 10,000+ entities and freeze the server, this method processes entities
     * in batches of MAX_ENTITIES_PER_TICK across multiple ticks.
     */
    public static void scanForUnmirroredCrows(ServerLevel level) {
        // Initialize iterator if we're starting a new scan
        if (currentScanIterator == null || !currentScanIterator.hasNext()) {
            // Start a new scan - get fresh iterator
            // Note: We still need to get all entities, but we'll process them in batches
            Iterable<Entity> allEntities = level.getAllEntities();
            currentScanIterator = allEntities.iterator();
            entitiesProcessedThisTick = 0;
            if (Config.logDebug) {
                Log.info("Starting new crow scan batch");
            }
        }

        // Process up to MAX_ENTITIES_PER_TICK entities this tick
        int processedThisTick = 0;
        while (currentScanIterator.hasNext() && processedThisTick < MAX_ENTITIES_PER_TICK) {
            try {
                Entity entity = currentScanIterator.next();
                processedThisTick++;
                entitiesProcessedThisTick++;

                // Skip null entities
                if (entity == null) {
                    continue;
                }

                // Check if this is a kasugai_crow
                if (!isKasugaiCrow(entity)) {
                    continue;
                }

                // Add to cache if not already tracked
                KNOWN_CROW_UUIDS.add(entity.getUUID());

                // Check if we already have a mirror
                if (CROW_MIRRORS.containsKey(entity.getUUID())) {
                    // Verify mirror still exists
                    GeckolibCrowEntity mirror = CROW_MIRRORS.get(entity.getUUID());
                    if (mirror == null || mirror.isRemoved() || !mirror.isAlive()) {
                        // Mirror is gone, remove from map and re-create
                        if (Config.logDebug)
                            Log.warn("Mirror crow was removed, recreating...");
                        CROW_MIRRORS.remove(entity.getUUID());
                    } else {
                        // Mirror exists and is valid - ensure original crow is still invisible
                        ensureCrowInvisibility(entity);
                        continue; // Mirror exists and is valid
                    }
                }

                // No mirror exists - create one
                if (Config.logDebug)
                    Log.info("Found unmirrored kasugai crow, creating GeckoLib mirror...");
                createMirrorForCrow(entity, level);
            } catch (Exception e) {
                // Catch any errors during iteration to prevent complete server freeze
                System.err.println("Error scanning crow entity: " + e.getMessage());
                if (Config.logDebug) {
                    e.printStackTrace();
                }
            }
        }

        // Log when scan completes
        if (!currentScanIterator.hasNext()) {
            if (Config.logDebug) {
                Log.info("Completed crow scan batch - processed {} entities total", entitiesProcessedThisTick);
            }
            currentScanIterator = null; // Reset for next scan
        }
    }

    /**
     * Ensure a crow has the invisibility effect
     * Re-applies it if missing (e.g., if cleared by commands)
     */
    private static void ensureCrowInvisibility(Entity crow) {
        if (crow instanceof LivingEntity livingCrow) {
            // Check if invisibility effect is present
            if (!livingCrow.hasEffect(MobEffects.INVISIBILITY)) {
            	if (Config.logDebug)
                Log.info("Re-applying invisibility to crow {} (was removed)", crow.getUUID());
                livingCrow.addEffect(new MobEffectInstance(
                    MobEffects.INVISIBILITY,
                    Integer.MAX_VALUE,
                    0,
                    false,
                    false
                ));
            }
        }
    }

    /**
     * Create a mirror entity for a given kasugai_crow
     */
    private static void createMirrorForCrow(Entity originalCrow, ServerLevel serverLevel) {
    	if (Config.logDebug)
        Log.info("Creating GeckoLib mirror for crow: {} at {}, {}, {}",
            originalCrow.getName().getString(), originalCrow.getX(), originalCrow.getY(), originalCrow.getZ());

        // Make the original crow invisible
        if (originalCrow instanceof LivingEntity livingCrow) {
            livingCrow.addEffect(new MobEffectInstance(
                MobEffects.INVISIBILITY,
                Integer.MAX_VALUE,
                0,
                false,
                false
            ));
            if (Config.logDebug)
            Log.info("  Made original crow invisible");
        }

        // Create our GeckoLib mirror entity
        GeckolibCrowEntity mirrorCrow = new GeckolibCrowEntity(
            ModEntities.GECKOLIB_CROW.get(),
            serverLevel
        );

        // Set position to match original
        mirrorCrow.setPos(originalCrow.getX(), originalCrow.getY(), originalCrow.getZ());
        mirrorCrow.setYRot(originalCrow.getYRot());
        mirrorCrow.setXRot(originalCrow.getXRot());

        // Link to original crow
        mirrorCrow.setOriginalCrow(originalCrow.getUUID());

        // Add to world
        serverLevel.addFreshEntity(mirrorCrow);

        // Store in map
        CROW_MIRRORS.put(originalCrow.getUUID(), mirrorCrow);
        if (Config.logDebug)
        Log.info("  Created GeckoLib mirror crow: {}", mirrorCrow.getUUID());
    }

    /**
     * Clear all mirrors (on server shutdown/world unload)
     * Should only be called from server side
     */
    public static void clearAllMirrors() {
        if (!CROW_MIRRORS.isEmpty()) {
        	if (Config.logDebug)
            Log.info("Clearing {} crow mirrors", CROW_MIRRORS.size());
            // Remove all mirror entities from the world
            for (GeckolibCrowEntity mirror : CROW_MIRRORS.values()) {
                if (mirror != null && !mirror.isRemoved()) {
                    // Only discard if we're on the logical server
                    if (mirror.level() != null && !mirror.level().isClientSide()) {
                        mirror.discard();
                    }
                }
            }
            CROW_MIRRORS.clear();
            if (Config.logDebug)
            Log.info("Cleared all crow mirrors");
        }

        // Clear the UUID cache and scan state
        KNOWN_CROW_UUIDS.clear();
        currentScanIterator = null;
        entitiesProcessedThisTick = 0;
    }

    /**
     * Handle server stopping event to clean up all mirrors
     */
    @SubscribeEvent
    public static void onServerStopping(net.minecraftforge.event.server.ServerStoppingEvent event) {
        clearAllMirrors();
    }
}
