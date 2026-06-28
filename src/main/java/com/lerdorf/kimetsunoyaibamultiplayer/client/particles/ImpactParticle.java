package com.lerdorf.kimetsunoyaibamultiplayer.client.particles;

import com.lerdorf.kimetsunoyaibamultiplayer.particles.ImpactParticleOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class ImpactParticle extends TextureSheetParticle {
    private static final int LAST_FRAME_INDEX = 5;
    private static final int TOTAL_LIFETIME = LAST_FRAME_INDEX + 1;
    private static final float BASE_DRIFT = 0.015F;
    private static final float SPEED_DRIFT_MULTIPLIER = 0.35F;

    private final SpriteSet spriteSet;

    protected ImpactParticle(ClientLevel level, double x, double y, double z,
                             double xSpeed, double ySpeed, double zSpeed,
                             Vector3f color, float size, SpriteSet spriteSet) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        this.spriteSet = spriteSet;
        this.lifetime = TOTAL_LIFETIME;
        this.hasPhysics = false;
        this.friction = 0.92F;
        this.gravity = 0.0F;

        this.rCol = color.x();
        this.gCol = color.y();
        this.bCol = color.z();
        this.alpha = 1.0F;
        this.quadSize = Math.max(0.01F, size * 0.4F);
        this.setSprite(spriteSet.get(0, LAST_FRAME_INDEX));

        double speedMagnitude = Math.sqrt(xSpeed * xSpeed + ySpeed * ySpeed + zSpeed * zSpeed);
        double drift = BASE_DRIFT + speedMagnitude * SPEED_DRIFT_MULTIPLIER;
        this.xd = xSpeed * 0.25D + (this.random.nextDouble() - 0.5D) * drift;
        this.yd = ySpeed * 0.25D + (this.random.nextDouble() - 0.5D) * drift;
        this.zd = zSpeed * 0.25D + (this.random.nextDouble() - 0.5D) * drift;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed) {
            this.setSprite(spriteSet.get(Math.min(this.age, LAST_FRAME_INDEX), LAST_FRAME_INDEX));
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        // Use additive blending for bright, glowing effect
        return CustomParticleRenderTypes.ADDITIVE_TRANSLUCENT;
    }

    @Override
    public int getLightColor(float partialTick) {
        float brightness = Math.max(rCol, Math.max(gCol, bCol));

        if (brightness > 0.5F) {
            return 0xF000F0; // fullbright
        }

        return super.getLightColor(partialTick);
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<ImpactParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet spriteSet) {
            this.sprites = spriteSet;
        }

        @Override
        public Particle createParticle(ImpactParticleOptions options, ClientLevel level, double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new ImpactParticle(level, x, y, z, xSpeed, ySpeed, zSpeed,
                options.getColor(), options.getSize(), this.sprites);
        }
    }
}
