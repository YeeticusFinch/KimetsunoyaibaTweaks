package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
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
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles spawning GeckolibCrowEntity mirrors when kasugai_crow entities spawn,
 * and manages damage/death event forwarding
 */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
public class CrowMirrorHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Map from original crow UUID to mirror crow
    private static final Map<UUID, GeckolibCrowEntity> CROW_MIRRORS = new HashMap<>();

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

        // Check if we already have a mirror for this crow
        if (CROW_MIRRORS.containsKey(entity.getUUID())) {
            // Check if the mirror still exists
            GeckolibCrowEntity existingMirror = CROW_MIRRORS.get(entity.getUUID());
            if (existingMirror != null && existingMirror.isAlive() && !existingMirror.isRemoved()) {
                LOGGER.info("Crow {} already has a valid mirror, skipping", entity.getUUID());
                return; // Already has a valid mirror
            } else {
                // Mirror is gone or invalid, remove from map and create new one
                LOGGER.warn("Crow {} had invalid mirror, removing and recreating", entity.getUUID());
                if (existingMirror != null && !existingMirror.isRemoved()) {
                    existingMirror.discard();
                }
                CROW_MIRRORS.remove(entity.getUUID());
            }
        }

        LOGGER.info("Kasugai crow spawned! Creating GeckoLib mirror entity...");

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
                LOGGER.info("Original crow hurt - triggered animation on mirror");
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
            LOGGER.info("Original crow died - triggered death animation on mirror");

            // Schedule removal of mirror after death animation (about 2 seconds)
            // The mirror will auto-remove when it detects original is dead in its tick()
        }

        // Clean up from map
        CROW_MIRRORS.remove(entity.getUUID());
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
     * Scan for existing kasugai_crow entities that don't have mirrors
     * Should be called periodically (e.g., every second)
     */
    public static void scanForUnmirroredCrows(ServerLevel level) {
        for (Entity entity : level.getAllEntities()) {
            // Check if this is a kasugai_crow
            if (!isKasugaiCrow(entity)) {
                continue;
            }

            // Check if we already have a mirror
            if (CROW_MIRRORS.containsKey(entity.getUUID())) {
                // Verify mirror still exists
                GeckolibCrowEntity mirror = CROW_MIRRORS.get(entity.getUUID());
                if (mirror == null || mirror.isRemoved() || !mirror.isAlive()) {
                    // Mirror is gone, remove from map and re-create
                    LOGGER.warn("Mirror crow was removed, recreating...");
                    CROW_MIRRORS.remove(entity.getUUID());
                } else {
                    continue; // Mirror exists and is valid
                }
            }

            // No mirror exists - create one
            LOGGER.info("Found unmirror kasugai crow, creating GeckoLib mirror...");
            createMirrorForCrow(entity, level);
        }
    }

    /**
     * Create a mirror entity for a given kasugai_crow
     */
    private static void createMirrorForCrow(Entity originalCrow, ServerLevel serverLevel) {
        LOGGER.info("Creating GeckoLib mirror for crow: {} at {}, {}, {}",
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
            LOGGER.info("  Made original crow invisible");
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

        LOGGER.info("  Created GeckoLib mirror crow: {}", mirrorCrow.getUUID());
    }

    /**
     * Clear all mirrors (on server shutdown/world unload)
     * Should only be called from server side
     */
    public static void clearAllMirrors() {
        if (!CROW_MIRRORS.isEmpty()) {
            LOGGER.info("Clearing {} crow mirrors", CROW_MIRRORS.size());
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
            LOGGER.info("Cleared all crow mirrors");
        }
    }

    /**
     * Handle server stopping event to clean up all mirrors
     */
    @SubscribeEvent
    public static void onServerStopping(net.minecraftforge.event.server.ServerStoppingEvent event) {
        clearAllMirrors();
    }
}
