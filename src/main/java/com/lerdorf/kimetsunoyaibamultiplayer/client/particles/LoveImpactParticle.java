package com.lerdorf.kimetsunoyaibamultiplayer.client.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Love Impact particle for Kanroji's Love Breathing.
 *
 * Behavior:
 * - Randomly selects one of 8 textures at spawn
 * - Starts super small and grows to full size over 5 ticks
 * - Stays at full size for 5 ticks
 * - Fades out over 2 ticks
 * - Total lifetime: 12 ticks
 *
 * Uses additive blending for bright, glowing effect.
 */
public class LoveImpactParticle extends TextureSheetParticle {
    private final SpriteSet spriteSet;
    private final float targetScale;

    // Animation phases
    private static final int GROW_TICKS = 5;
    private static final int STAY_TICKS = 5;
    private static final int FADE_TICKS = 2;
    private static final int TOTAL_LIFETIME = GROW_TICKS + STAY_TICKS + FADE_TICKS;

    protected LoveImpactParticle(ClientLevel level, double x, double y, double z,
                                  double xSpeed, double ySpeed, double zSpeed,
                                  SpriteSet spriteSet) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);

        this.spriteSet = spriteSet;

        // Randomly choose one of 8 textures (indices 0-7)
        int textureIndex = (int)(Math.random() * 8);
        this.setSprite(spriteSet.get(textureIndex, 7)); // 7 = max index

        // Set movement - mostly stationary with slight drift
        this.xd = xSpeed * 0.1;
        this.yd = ySpeed * 0.1;
        this.zd = zSpeed * 0.1;

        // Target scale (full size) - random 1x to 3x
        this.targetScale = 1.0f + (float)(Math.random() * 2.0f); // 1.0-3.0

        // Start at 0 size
        this.quadSize = 0.01f;

        // Set lifetime
        this.lifetime = TOTAL_LIFETIME;

        // No tint - use texture colors directly (white = no tint)
        this.rCol = 1.0f;
        this.gCol = 1.0f;
        this.bCol = 1.0f;

        // Start fully visible
        this.alpha = 1.0f;

        // No physics - particles stay in place
        this.hasPhysics = false;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        // Phase 1: Growing (ticks 0-4)
        if (this.age < GROW_TICKS) {
            float progress = (float) this.age / GROW_TICKS;
            this.quadSize = targetScale * progress;
            this.alpha = 1.0f;
        }
        // Phase 2: Staying at full size (ticks 5-9)
        else if (this.age < GROW_TICKS + STAY_TICKS) {
            this.quadSize = targetScale;
            this.alpha = 1.0f;
        }
        // Phase 3: Fading out (ticks 10-11)
        else {
            this.quadSize = targetScale;
            float fadeProgress = (float)(this.age - GROW_TICKS - STAY_TICKS) / FADE_TICKS;
            this.alpha = 1.0f - fadeProgress;
        }

        // Apply minimal movement
        this.move(this.xd, this.yd, this.zd);
    }

    @Override
    public ParticleRenderType getRenderType() {
        // Use additive blending for bright, glowing effect
        return CustomParticleRenderTypes.ADDITIVE_TRANSLUCENT;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet spriteSet) {
            this.sprites = spriteSet;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                     double xSpeed, double ySpeed, double zSpeed) {
            return new LoveImpactParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }
}
