package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.client.WhipPhysics;
import com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedBreathingConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Particle trail renderer for Mitsuri's whip sword.
 *
 * Spawns heart particles along the whip curve for a glowing trail effect.
 */
public class WhipParticleRenderer {

    private static int tickCounter = 0;

    /**
     * Spawn particles along the whip curve.
     * Called from ClientTickEvents.
     */
    public static void spawnParticles(Player player, ClientLevel level) {
        // Check config
        int density = EnhancedBreathingConfig.whipParticleDensity;
        if (density <= 0) return;

        // Check if we should spawn particles when idle
        String currentAnim = WhipPhysics.getCurrentAnimation(player);
        boolean isIdle = currentAnim.equals("idle") || currentAnim.equals("walk");

        if (isIdle && !EnhancedBreathingConfig.whipParticleIdle) {
            return; // Don't spawn particles during idle unless configured
        }

        // Throttle particle spawning
        tickCounter++;
        if (tickCounter % (6 - Math.min(density, 5)) != 0) {
            return;
        }

        List<Vec3> points = WhipPhysics.getPoints(player);
        if (points.isEmpty()) return;

        // Spawn particles along curve
        int totalPoints = points.size();
        for (int i = 0; i < totalPoints; i++) {
            // Skip some points based on density
            if (i % (11 - Math.min(density, 10)) != 0) continue;

            Vec3 pos = points.get(i);

            // Determine particle type and color based on position along whip
            float t = (float) i / totalPoints;

            // Add slight random offset
            double offsetX = (level.random.nextDouble() - 0.5) * 0.1;
            double offsetY = (level.random.nextDouble() - 0.5) * 0.1;
            double offsetZ = (level.random.nextDouble() - 0.5) * 0.1;

            if (t < 0.5) {
                // Back half: Less particles, smaller
                if (level.random.nextFloat() < 0.3f) {
                    level.addParticle(ParticleTypes.ASH,
                        pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                        0, 0.01, 0);
                }
            } else {
                // Front half: More particles, hearts
                if (level.random.nextFloat() < 0.6f) {
                    level.addParticle(ParticleTypes.HEART,
                        pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                        0, 0.02, 0);
                }

                // Additional pink particle for extra glow
                if (level.random.nextFloat() < 0.3f) {
                    level.addParticle(ParticleTypes.CHERRY_LEAVES,
                        pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                        0, 0.01, 0);
                }
            }
        }
    }

    /**
     * Reset tick counter (for world reload).
     */
    public static void reset() {
        tickCounter = 0;
    }
}
