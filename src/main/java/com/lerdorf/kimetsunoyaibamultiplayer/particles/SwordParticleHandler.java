package com.lerdorf.kimetsunoyaibamultiplayer.particles;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.config.ParticleConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class SwordParticleHandler {

    // Track active particle effects to avoid spam
    private static final Map<UUID, ParticleEffectState> activeEffects = new HashMap<>();
    private static final double RENDER_DISTANCE_SQ = 32.0 * 32.0; // 32 block render distance

    private static class ParticleEffectState {
        long lastSpawnTime;
        ParticleOptions lastParticleType;

        ParticleEffectState(long time, ParticleOptions particle) {
            this.lastSpawnTime = time;
            this.lastParticleType = particle;
        }
    }

    /**
     * Spawns sword particles for a given entity during an animation
     * @param entity The entity performing the animation
     * @param swordItem The sword item being used
     * @param animationName The name of the animation being performed
     */
    public static void spawnSwordParticles(LivingEntity entity, ItemStack swordItem, String animationName) {
        spawnSwordParticles(entity, swordItem, animationName, -1);
    }

    /**
     * Spawns sword particles for a given entity during an animation with animation tick info
     * @param entity The entity performing the animation
     * @param swordItem The sword item being used
     * @param animationName The name of the animation being performed
     * @param animationTick The current tick of the animation (-1 if unknown)
     */
    public static void spawnSwordParticles(LivingEntity entity, ItemStack swordItem, String animationName, int animationTick) {
        System.out.println("spawnSwordParticles called: entity=" + entity.getName().getString() + ", item=" + swordItem.getItem() + ", anim=" + animationName);

        if (!shouldSpawnParticles(entity, swordItem)) {
            System.out.println("shouldSpawnParticles returned false");
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            System.out.println("Level is null");
            return;
        }

        // Check render distance
        if (mc.player != null && !isWithinRenderDistance(entity, mc.player)) {
            System.out.println("Entity too far from player");
            return;
        }

        // Get particle type for this sword
        ParticleOptions particleType = SwordParticleMapping.getParticleForSword(swordItem);
        if (particleType == null) {
            System.out.println("No particle type found for sword");
            return;
        }

        System.out.println("About to spawn particles with type: " + particleType.getType());

        // Spawn radial ribbon particles directly - no more Vec3 arrays!
        BonePositionTracker.spawnRadialRibbonParticles(entity, animationName, animationTick, particleType);

        // Simple state tracking
        UUID entityId = entity.getUUID();
        long currentTime = level.getGameTime();
        activeEffects.put(entityId, new ParticleEffectState(currentTime, particleType));

        if (Config.logDebug) {
            Log.info("Spawned radial ribbon particles for {} using {}",
                SwordParticleMapping.getSwordTypeName(swordItem),
                particleType.getType().toString());
        }
    }

    /**
     * Determines if particles should be spawned for a given entity and sword
     * @param entity The entity
     * @param swordItem The sword item
     * @return true if particles should be spawned
     */
    private static boolean shouldSpawnParticles(LivingEntity entity, ItemStack swordItem) {
        System.out.println("shouldSpawnParticles check: particlesEnabled=" + ParticleConfig.swordParticlesEnabled);

        // Check config settings
        if (!ParticleConfig.swordParticlesEnabled) {
            System.out.println("Particles disabled in config");
            return false;
        }

        // Check if this is the local player or if other entities are allowed
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            System.out.println("Player is null");
            return false;
        }

        boolean isLocalPlayer = entity.getUUID().equals(mc.player.getUUID());
        System.out.println("Is local player: " + isLocalPlayer + ", allow other entities: " + ParticleConfig.swordParticlesForOtherEntities);
        if (!isLocalPlayer && !ParticleConfig.swordParticlesForOtherEntities) {
            System.out.println("Not local player and other entities disabled");
            return false;
        }

        // Check if this is a valid kimetsunoyaiba sword
        boolean isKimetsunoyaibaSword = SwordParticleMapping.isKimetsunoyaibaSword(swordItem);
        System.out.println("Is kimetsunoyaiba sword: " + isKimetsunoyaibaSword);
        if (!isKimetsunoyaibaSword) {
            System.out.println("Not a kimetsunoyaiba sword");
            return false;
        }

        System.out.println("All checks passed, should spawn particles");
        return true;
    }

    /**
     * Checks if an entity is within render distance of the player
     * @param entity The entity to check
     * @param player The player
     * @return true if within render distance
     */
    private static boolean isWithinRenderDistance(LivingEntity entity, LivingEntity player) {
        double distanceSq = entity.distanceToSqr(player);
        return distanceSq <= RENDER_DISTANCE_SQ;
    }

    /**
     * Calculates the number of particles to spawn based on the animation type
     * @param animationName The name of the animation
     * @return Number of particles to spawn
     */
    private static int calculateParticleCount(String animationName) {
        if (animationName == null) {
            return 1;
        }

        // More particles for powerful/special attacks
        if (animationName.contains("overhead") || animationName.contains("ultimate") ||
            animationName.contains("breath") || animationName.contains("special")) {
            return 3;
        } else if (animationName.contains("combo") || animationName.contains("rush") ||
                  animationName.contains("rotate")) {
            return 2;
        } else {
            return 1;
        }
    }


    /**
     * Clears particle effect state for an entity (called when animation stops)
     * @param entityId The UUID of the entity
     */
    public static void clearParticleEffectState(UUID entityId) {
        activeEffects.remove(entityId);
    }

    /**
     * Clears all particle effect states (called on world unload)
     */
    public static void clearAllParticleEffectStates() {
        activeEffects.clear();
    }

    /**
     * Forces particle spawn for testing purposes
     * @param entity The entity
     * @param swordItem The sword item
     * @param animationName The animation name
     */
    public static void forceSpawnParticles(LivingEntity entity, ItemStack swordItem, String animationName) {
        spawnSwordParticles(entity, swordItem, animationName, -1);
    }
}