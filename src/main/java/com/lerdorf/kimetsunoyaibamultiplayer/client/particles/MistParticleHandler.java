package com.lerdorf.kimetsunoyaibamultiplayer.client.particles;

import com.lerdorf.kimetsunoyaibamultiplayer.particles.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

public class MistParticleHandler {
    
    /**
     * Spawns a single mist particle at the given location with random slow movement
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     */
    public static void spawnMistParticle(double x, double y, double z) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        
        if (level != null) {
            // Generate random slow movement (between -0.01 and 0.01 for x and z, slightly downward for y)
            double xSpeed = (Math.random() - 0.5) * 0.02; // Random between -0.01 and 0.01
            double ySpeed = -0.01 + (Math.random() * 0.005); // Slightly downward movement (-0.01 to -0.005)
            double zSpeed = (Math.random() - 0.5) * 0.02; // Random between -0.01 and 0.01
            
            level.addParticle(
                ModParticles.MIST_PARTICLE.get(),
                x, y, z,
                xSpeed, ySpeed, zSpeed
            );
        }
    }
    
    /**
     * Spawns multiple mist particles in an area
     * @param x X coordinate center
     * @param y Y coordinate center
     * @param z Z coordinate center
     * @param count Number of particles to spawn
     * @param spread How far from the center to spread particles
     */
    public static void spawnMistParticles(double x, double y, double z, int count, double spread) {
        for (int i = 0; i < count; i++) {
            double particleX = x + (Math.random() - 0.5) * spread;
            double particleY = y + (Math.random() - 0.5) * spread;
            double particleZ = z + (Math.random() - 0.5) * spread;
            
            spawnMistParticle(particleX, particleY, particleZ);
        }
    }
    
    /**
     * Spawns a mist particle with a specific velocity
     * @param position Position vector
     * @param velocity Velocity vector
     */
    public static void spawnMistParticleWithVelocity(Vec3 position, Vec3 velocity) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        
        if (level != null) {
            level.addParticle(
                ModParticles.MIST_PARTICLE.get(),
                position.x, position.y, position.z,
                velocity.x, velocity.y, velocity.z
            );
        }
    }
}