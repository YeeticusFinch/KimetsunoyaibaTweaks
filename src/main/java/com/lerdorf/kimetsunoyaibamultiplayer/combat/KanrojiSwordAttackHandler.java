package com.lerdorf.kimetsunoyaibamultiplayer.combat;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.EnhancedLoveForms;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.ModParticles;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;

/**
 * Shared handler for Kanroji whip sword attacks.
 * Used by both players (via BreathingSwordSwingPacket) and entities (Kanroji Entity, CustomNPCs).
 *
 * Handles:
 * - Extended range AOE damage (10 block box)
 * - Particle trail spawning for sword animations
 * - Love slash hit particles
 */
public class KanrojiSwordAttackHandler {

    private static final float KANROJI_BOX_SIZE = 10f; // Extended range for whip sword

    /**
     * Perform a Kanroji whip AOE attack.
     *
     * @param attacker The entity performing the attack
     * @param damage Damage to deal
     * @param animationName Animation being played (for particles)
     * @return Number of entities hit
     */
    public static int performWhipAttack(LivingEntity attacker, float damage, String animationName) {
        if (!(attacker.level() instanceof ServerLevel serverLevel)) {
            return 0;
        }

        // Determine attack box based on attacker's look direction
        Vec3 attackerPos = attacker.position().add(0, attacker.getEyeHeight(), 0);
        Vec3 lookVec = attacker.getLookAngle().normalize();
        Vec3 frontPos = attackerPos.add(lookVec.scale(KANROJI_BOX_SIZE / 1.5f));

        AABB attackBox = new AABB(
            frontPos.add(-KANROJI_BOX_SIZE / 2, -KANROJI_BOX_SIZE / 2, -KANROJI_BOX_SIZE / 2),
            frontPos.add(KANROJI_BOX_SIZE / 2, KANROJI_BOX_SIZE / 2, KANROJI_BOX_SIZE / 2)
        );

        // Find all living entities in attack box
        List<LivingEntity> targets = attacker.level().getEntitiesOfClass(
            LivingEntity.class,
            attackBox,
            e -> e != attacker && e.isAlive()
        );

        // Apply damage to all targets
        int hitCount = 0;
        for (LivingEntity target : targets) {
            // Use smart targeting to prevent friendly fire
            if (!EnhancedLoveForms.isTargetable(attacker, target)) {
                continue;
            }

            Damager.hurt(attacker, target, damage);
            hitCount++;

            // Spawn love_slash particle on hit
            spawnHitParticle(serverLevel, target);
        }

        // Spawn particle trail for the animation
        if (animationName != null && !animationName.isEmpty()) {
            spawnParticleTrail(serverLevel, attacker, animationName);
        }

        if (Config.logDebug && hitCount > 0) {
            Log.debug("[KanrojiSwordAttackHandler] Whip attack hit {} targets with {} damage",
                hitCount, damage);
        }

        return hitCount;
    }

    /**
     * Spawn a love_slash particle on a hit target.
     */
    private static void spawnHitParticle(ServerLevel serverLevel, LivingEntity target) {
        double particleX = target.getX() + target.getBbHeight() * (Math.random() - 0.5);
        double particleY = target.getY() + target.getBbHeight() * Math.random();
        double particleZ = target.getZ() + target.getBbHeight() * (Math.random() - 0.5);

        serverLevel.sendParticles(
            ModParticles.LOVE_SLASH.get(),
            particleX, particleY, particleZ,
            1, 0, 0, 0, 0
        );
    }

    /**
     * Spawn particle trail for Kanroji sword animations.
     * Supports: sword_to_left, sword_to_right, sword_overhead, kanroji_sword_overhead, sword_rotate
     */
    public static void spawnParticleTrail(ServerLevel serverLevel, LivingEntity attacker, String animName) {
        // Only spawn particles for supported animations
        if (!animName.equals("sword_to_left") &&
            !animName.equals("sword_to_right") &&
            !animName.equals("sword_overhead") &&
            !animName.equals("kanroji_sword_overhead") &&
            !animName.equals("sword_overhead_kanroji") &&
            !animName.equals("sword_rotate")) {
            return;
        }

        // Get particle position data from ParticlePositions
        Map<String, float[][][]> particleMap = getParticleMap(animName);
        if (particleMap == null) {
            if (Config.logDebug) {
                Log.debug("No ParticlePositions map found for animation: {}", animName);
            }
            return;
        }

        float[][][] particlePoints = particleMap.get("point_a");
        if (particlePoints == null || particlePoints.length == 0) {
            if (Config.logDebug) {
                Log.debug("No particle points found in map for animation: {}", animName);
            }
            return;
        }

        // Spawn all particles immediately (entity attacks are instant, not scheduled like players)
        spawnParticlesForEntity(serverLevel, attacker, particlePoints);

        if (Config.logDebug) {
            Log.debug("Spawned Kanroji sword trail particles for animation: {} ({} frames)",
                animName, particlePoints.length);
        }
    }

    /**
     * Get the particle map for a given animation name.
     */
    private static Map<String, float[][][]> getParticleMap(String animName) {
        switch (animName) {
            case "sword_to_left":
                return com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.ParticlePositions.sword_to_left;
            case "sword_to_right":
                return com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.ParticlePositions.sword_to_right;
            case "sword_rotate":
                return com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.ParticlePositions.sword_rotate;
            case "sword_overhead":
            case "sword_overheaed": // typo in original code
                return com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.ParticlePositions.sword_overhead;
            case "kanroji_sword_overhead":
            case "sword_overhead_kanroji":
            case "kanroji_sword_overheaed": // typo variants
            case "sword_overheaed_kanroji":
                return com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.ParticlePositions.kanroji_sword_overhead;
            default:
                return null;
        }
    }

    /**
     * Spawn particle trail for an entity attack (instant, all frames at once with slight delay).
     * Entities don't use the scheduler like players, so we spawn particles across multiple frames rapidly.
     */
    private static void spawnParticlesForEntity(ServerLevel serverLevel, LivingEntity attacker, float[][][] particlePoints) {
        // Get forward and right vectors based on entity's yaw
        float yawRad = (float) Math.toRadians(-attacker.getYRot());
        Vec3 forward = new Vec3(Math.sin(yawRad), 0, Math.cos(yawRad)).normalize();
        Vec3 right = new Vec3(-forward.z, 0, forward.x); // 90 degrees counter-clockwise

        // Spawn particles from a few key frames to show the trail
        // Instead of all frames, sample every 2-3 frames for performance
        for (int tick = 0; tick < particlePoints.length; tick += 2) {
            if (particlePoints[tick].length == 0) continue;

            for (int i = 0; i < particlePoints[tick].length; i++) {
                float x = particlePoints[tick][i][0];
                float y = particlePoints[tick][i][1];
                float z = particlePoints[tick][i][2];

                Vec3 pos = attacker.position().add(
                    forward.scale(z).add(right.scale(x)).add(0, y, 0)
                );

                // Spawn pink dust particle
                serverLevel.sendParticles(
                    new DustParticleOptions(
                        new Vector3f(1.0f, 0.4f, 0.7f), // PINK
                        1.2f                             // scale
                    ),
                    pos.x, pos.y, pos.z,
                    2, 0.05, 0.05, 0.05, 0
                );

                // Spawn white dust particle
                serverLevel.sendParticles(
                    new DustParticleOptions(
                        new Vector3f(1.0f, 1.0f, 1.0f), // WHITE
                        1f                               // scale
                    ),
                    pos.x, pos.y, pos.z,
                    1, 0, 0, 0, 0
                );
            }
        }
    }

    /**
     * Get the attack box size for Kanroji whip sword.
     */
    public static float getAttackBoxSize() {
        return KANROJI_BOX_SIZE;
    }
}
