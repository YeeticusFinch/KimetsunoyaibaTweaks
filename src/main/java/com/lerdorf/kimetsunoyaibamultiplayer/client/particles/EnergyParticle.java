package com.lerdorf.kimetsunoyaibamultiplayer.client.particles;

import com.lerdorf.kimetsunoyaibamultiplayer.particles.EnergyParticleOptions;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

/**
 * Energy particle that drifts in a random direction, collides with blocks,
 * and fades into a progressively softer halo before despawning.
 *
 * Works as a drop-in replacement for DustParticleOptions with the same
 * color (Vector3f) and size (float) constructor signature.
 *
 * - Lifetime: 30 ticks total
 * - First 20 ticks: stays fully opaque at the configured size
 * - Last 10 ticks: fades out without shrinking and gains a soft halo
 * - Has physics (collides with blocks)
 * - Drifts in random direction with gentle movement
 */
@OnlyIn(Dist.CLIENT)
public class EnergyParticle extends TextureSheetParticle {

    private static final int TOTAL_LIFETIME = 30;
    private static final int FADE_START_TICK = 20;
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

        // Full opacity until the fade phase begins.
        this.alpha = 1.0f;
    }

    @Override
    public void tick() {
        super.tick();

        // Fade during the final ten ticks while keeping the particle's base size.
        float fadeProgress = (float) (this.age - FADE_START_TICK)
                / (TOTAL_LIFETIME - FADE_START_TICK);
        fadeProgress = Math.max(0.0F, Math.min(1.0F, fadeProgress));
        this.alpha = 1.0F - fadeProgress;
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        float fadeProgress = (float) (this.age - FADE_START_TICK + partialTick)
                / (TOTAL_LIFETIME - FADE_START_TICK);
        fadeProgress = Math.max(0.0F, Math.min(1.0F, fadeProgress));

        if (fadeProgress > 0.0F && this.alpha > 0.0F) {
            float baseAlpha = this.alpha;
            float baseSize = this.quadSize;

            // A larger, low-opacity pass creates a soft halo as the particle fades.
            this.alpha = baseAlpha * fadeProgress * 0.35F;
            this.quadSize = baseSize * (1.0F + fadeProgress * 1.5F);
            super.render(buffer, camera, partialTick);

            this.alpha = baseAlpha;
            this.quadSize = baseSize;
        }

        super.render(buffer, camera, partialTick);
    }

    @Override
    public int getLightColor(float partialTick) {
        float brightness = Math.max(rCol, Math.max(gCol, bCol));

        if (brightness > 0.5F) {
            return 0xF000F0; // fullbright
        }

        return super.getLightColor(partialTick);
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
