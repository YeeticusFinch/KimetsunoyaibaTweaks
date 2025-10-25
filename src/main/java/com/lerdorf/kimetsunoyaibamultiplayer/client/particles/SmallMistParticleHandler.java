package com.lerdorf.kimetsunoyaibamultiplayer.client.particles;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.ModParticles;

/**
 * Handler for spawning small animated mist particles
 */
@OnlyIn(Dist.CLIENT)
public class SmallMistParticleHandler {

    /**
     * Spawns a single small mist particle at the given location with random slow movement
     */
    public static void spawnSmallMistParticle(double x, double y, double z) {
        if (net.minecraft.client.Minecraft.getInstance().level != null) {
            net.minecraft.client.Minecraft.getInstance().level.addParticle(
                (ParticleOptions) ModParticles.SMALL_MIST_PARTICLE.get(),
                x, y, z,
                (Math.random() - 0.5) * 0.02,  // x velocity
                (Math.random() - 0.5) * 0.02,  // y velocity  
                (Math.random() - 0.5) * 0.02   // z velocity
            );
        }
    }

    /**
     * Spawns multiple small mist particles in an area
     */
    public static void spawnSmallMistParticles(double x, double y, double z, int count, double spread) {
        for (int i = 0; i < count; i++) {
            double particleX = x + (Math.random() - 0.5) * spread;
            double particleY = y + (Math.random() - 0.5) * spread;
            double particleZ = z + (Math.random() - 0.5) * spread;
            spawnSmallMistParticle(particleX, particleY, particleZ);
        }
    }

    /**
     * Spawns a small mist particle with a specific velocity
     */
    public static void spawnSmallMistParticleWithVelocity(Vec3 position, Vec3 velocity) {
        if (net.minecraft.client.Minecraft.getInstance().level != null) {
            net.minecraft.client.Minecraft.getInstance().level.addParticle(
                (ParticleOptions) ModParticles.SMALL_MIST_PARTICLE.get(),
                position.x, position.y, position.z,
                velocity.x, velocity.y, velocity.z
            );
        }
    }
}