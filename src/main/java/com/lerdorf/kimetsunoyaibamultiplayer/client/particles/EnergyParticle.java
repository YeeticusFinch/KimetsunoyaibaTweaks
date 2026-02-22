package com.lerdorf.kimetsunoyaibamultiplayer.client.particles;

import com.lerdorf.kimetsunoyaibamultiplayer.particles.EnergyParticleOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

/**
 * Energy particle that drifts in a random direction, collides with blocks,
 * and shrinks over its final 10 ticks before despawning.
 *
 * Works as a drop-in replacement for DustParticleOptions with the same
 * color (Vector3f) and size (float) constructor signature.
 *
 * - Lifetime: 30 ticks total
 * - First 20 ticks: stays same size
 * - Last 10 ticks: shrinks linearly to 0
 * - Has physics (collides with blocks)
 * - Drifts in random direction with gentle movement
 */
@OnlyIn(Dist.CLIENT)
public class EnergyParticle extends TextureSheetParticle {

    private final float initialSize;
    private static final int TOTAL_LIFETIME = 30;
    private static final int SHRINK_START_TICK = 20;
    private static final float DRIFT_SPEED = 0.0001f;

    protected EnergyParticle(ClientLevel level, double x, double y, double z,
                             double xSpeed, double ySpeed, double zSpeed,
                             Vector3f color, float size, SpriteSet spriteSet) {
        super(level, x, y, z, 0, 0, 0);

        // Set sprite
        this.pickSprite(spriteSet);

        // Set color from options
        this.rCol = color.x();
        this.gCol = color.y();
        this.bCol = color.z();

        // Set size
        this.initialSize = size * 0.1f;
        this.quadSize = size * 0.1f;

        // Set lifetime to 30 ticks
        this.lifetime = TOTAL_LIFETIME;

        // Enable physics (collide with blocks)
        this.hasPhysics = true;

        // Check if velocities were provided (non-zero speed means use as base for random drift)
        double speedMagnitude = Math.sqrt(xSpeed * xSpeed + ySpeed * ySpeed + zSpeed * zSpeed);
        float driftSpeed = speedMagnitude > 0.001 ? (float) speedMagnitude : DRIFT_SPEED;

        // Random direction (uniform on unit sphere) scaled by drift speed
        double theta = random.nextDouble() * 2 * Math.PI;
        double phi = Math.acos(2 * random.nextDouble() - 1);
        this.xd = driftSpeed * Math.sin(phi) * Math.cos(theta);
        this.yd = driftSpeed * Math.sin(phi) * Math.sin(theta);
        this.zd = driftSpeed * Math.cos(phi);

        // Full opacity
        this.alpha = 1.0f;
    }

    @Override
    public void tick() {
        super.tick();

        // Calculate age (how many ticks have passed)
        int ticksAlive = this.age;

        // After tick 20, start shrinking over 10 ticks
        if (ticksAlive >= SHRINK_START_TICK) {
            int shrinkTicks = ticksAlive - SHRINK_START_TICK;
            int remainingTicks = TOTAL_LIFETIME - SHRINK_START_TICK;
            // Linear interpolation from initialSize to 0
            float shrinkProgress = (float) shrinkTicks / remainingTicks;
            this.quadSize = initialSize * (1.0f - shrinkProgress);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<EnergyParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet spriteSet) {
            this.sprites = spriteSet;
        }

        @Override
        public Particle createParticle(EnergyParticleOptions options, ClientLevel level,
                                        double x, double y, double z,
                                        double xSpeed, double ySpeed, double zSpeed) {
            return new EnergyParticle(level, x, y, z, xSpeed, ySpeed, zSpeed,
                options.getColor(), options.getSize(), this.sprites);
        }
    }
}
