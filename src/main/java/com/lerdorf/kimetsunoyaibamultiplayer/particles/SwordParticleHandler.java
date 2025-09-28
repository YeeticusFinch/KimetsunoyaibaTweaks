package com.lerdorf.kimetsunoyaibamultiplayer.particles;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.*;

public class SwordParticleHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Track active particle effects to avoid spam
    private static final Map<UUID, ParticleEffectState> activeEffects = new HashMap<>();
    private static final int PARTICLE_COOLDOWN_TICKS = 1; // Spawn particles every tick during animation
    private static final int MAX_PARTICLES_PER_TICK = 2; // Maximum particles per tick
    private static final double RENDER_DISTANCE_SQ = 32.0 * 32.0; // 32 block render distance

    private static class ParticleEffectState {
        long lastSpawnTime;
        int particlesSpawned;
        ParticleOptions lastParticleType;

        ParticleEffectState(long time, ParticleOptions particle) {
            this.lastSpawnTime = time;
            this.particlesSpawned = 1;
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
        if (!shouldSpawnParticles(entity, swordItem)) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }

        // Check render distance
        if (mc.player != null && !isWithinRenderDistance(entity, mc.player)) {
            return;
        }

        // Get particle type for this sword
        ParticleOptions particleType = SwordParticleMapping.getParticleForSword(swordItem);
        if (particleType == null) {
            return;
        }

        // Check cooldown and particle limits
        UUID entityId = entity.getUUID();
        ParticleEffectState state = activeEffects.get(entityId);
        long currentTime = level.getGameTime();

        if (state != null) {
            if (currentTime - state.lastSpawnTime < PARTICLE_COOLDOWN_TICKS) {
                return; // Still in cooldown
            }
            // Reset particle count if it's a different particle type (new animation)
            if (!state.lastParticleType.equals(particleType)) {
                state.particlesSpawned = 0;
            }
        }

        // Calculate sword tip position using animation name and tick
        Vec3 swordTipPos = BonePositionTracker.getSwordTipPosition(entity, animationName, animationTick);
        if (swordTipPos == null || !BonePositionTracker.isValidParticlePosition(entity.position(), swordTipPos)) {
            if (Config.logDebug) {
                LOGGER.debug("Invalid sword tip position for entity {} with animation {}", entity.getName().getString(), animationName);
            }
            return;
        }

        // Spawn the particles (use MAX_PARTICLES_PER_TICK for consistent spawning)
        int particleCount = MAX_PARTICLES_PER_TICK;
        spawnParticlesAtPosition(level, swordTipPos, particleType, particleCount, entity);

        // Update state
        if (state == null || !state.lastParticleType.equals(particleType)) {
            activeEffects.put(entityId, new ParticleEffectState(currentTime, particleType));
        } else {
            state.lastSpawnTime = currentTime;
            state.particlesSpawned++;
        }

        if (Config.logDebug) {
            LOGGER.info("Spawned {} {} particles for {} at position ({}, {}, {})",
                particleCount, particleType.getType().toString(),
                SwordParticleMapping.getSwordTypeName(swordItem),
                swordTipPos.x, swordTipPos.y, swordTipPos.z);
        }
    }

    /**
     * Determines if particles should be spawned for a given entity and sword
     * @param entity The entity
     * @param swordItem The sword item
     * @return true if particles should be spawned
     */
    private static boolean shouldSpawnParticles(LivingEntity entity, ItemStack swordItem) {
        // Check config settings
        if (!Config.swordParticlesEnabled) {
            return false;
        }

        // Check if this is the local player or if other entities are allowed
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return false;
        }

        boolean isLocalPlayer = entity.getUUID().equals(mc.player.getUUID());
        if (!isLocalPlayer && !Config.swordParticlesForOtherEntities) {
            return false;
        }

        // Check if this is a valid kimetsunoyaiba sword
        if (!SwordParticleMapping.isKimetsunoyaibaSword(swordItem)) {
            return false;
        }

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
     * Spawns particles at a specific position with some randomization
     * @param level The client level
     * @param position The base position to spawn particles
     * @param particleType The type of particle to spawn
     * @param count The number of particles to spawn
     * @param entity The entity performing the animation (for velocity calculations)
     */
    private static void spawnParticlesAtPosition(ClientLevel level, Vec3 position, ParticleOptions particleType,
                                               int count, LivingEntity entity) {
        net.minecraft.util.RandomSource randomSource = level.getRandom();
        java.util.Random random = new java.util.Random(randomSource.nextLong());

        for (int i = 0; i < count; i++) {
            // Add some randomization to particle position
            double offsetX = (random.nextDouble() - 0.5) * 0.3;
            double offsetY = (random.nextDouble() - 0.5) * 0.3;
            double offsetZ = (random.nextDouble() - 0.5) * 0.3;

            Vec3 particlePos = position.add(offsetX, offsetY, offsetZ);

            // Calculate particle velocity based on entity movement and swing direction
            Vec3 velocity = calculateParticleVelocity(entity, random);

            // Spawn the particle
            level.addParticle(particleType,
                particlePos.x, particlePos.y, particlePos.z,
                velocity.x, velocity.y, velocity.z);
        }
    }

    /**
     * Calculates particle velocity based on entity movement and animation
     * @param entity The entity performing the animation
     * @param random Random instance for variation
     * @return Velocity vector for the particle
     */
    private static Vec3 calculateParticleVelocity(LivingEntity entity, Random random) {
        // Base velocity from entity movement
        Vec3 entityVelocity = entity.getDeltaMovement();

        // Add some randomization and swing direction
        double velocityX = entityVelocity.x + (random.nextDouble() - 0.5) * 0.2;
        double velocityY = Math.abs(entityVelocity.y) + random.nextDouble() * 0.1;
        double velocityZ = entityVelocity.z + (random.nextDouble() - 0.5) * 0.2;

        return new Vec3(velocityX, velocityY, velocityZ);
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
     * Updates particle effect states, removing old entries
     * @param currentTime The current game time
     */
    public static void updateParticleStates(long currentTime) {
        // Remove states older than 60 ticks (3 seconds)
        activeEffects.entrySet().removeIf(entry ->
            currentTime - entry.getValue().lastSpawnTime > 60);
    }

    /**
     * Forces particle spawn for testing purposes (ignores cooldowns and limits)
     * @param entity The entity
     * @param swordItem The sword item
     * @param animationName The animation name
     */
    public static void forceSpawnParticles(LivingEntity entity, ItemStack swordItem, String animationName) {
        if (!Config.swordParticlesEnabled) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }

        ParticleOptions particleType = SwordParticleMapping.getParticleForSword(swordItem);
        if (particleType == null) {
            return;
        }

        Vec3 swordTipPos = BonePositionTracker.getSwordTipPosition(entity, animationName);
        if (swordTipPos == null) {
            return;
        }

        int particleCount = MAX_PARTICLES_PER_TICK;
        spawnParticlesAtPosition(level, swordTipPos, particleType, particleCount, entity);

        LOGGER.info("Force spawned {} particles for testing", particleCount);
    }
}