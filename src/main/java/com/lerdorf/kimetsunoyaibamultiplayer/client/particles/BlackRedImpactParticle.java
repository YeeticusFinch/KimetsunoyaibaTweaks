package com.lerdorf.kimetsunoyaibamultiplayer.client.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Dark red, fullbright impact burst for Black Blood Brambles. */
public class BlackRedImpactParticle extends TextureSheetParticle {
    private static final double DRIFT_SPEED = 0.04D;
    private final float targetScale;

    protected BlackRedImpactParticle(ClientLevel level, double x, double y, double z,
                                     double xSpeed, double ySpeed, double zSpeed,
                                     SpriteSet sprites) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        setSprite(sprites.get((int) (Math.random() * 8.0D), 7));
        targetScale = 1.0F + (float) (Math.random() * 2.0D);
        quadSize = 0.01F;
        lifetime = 12;
        alpha = 1.0F;
        hasPhysics = false;
        double dx = level.random.nextDouble() * 2.0D - 1.0D;
        double dy = level.random.nextDouble() * 2.0D - 1.0D;
        double dz = level.random.nextDouble() * 2.0D - 1.0D;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0E-8D) {
            dx = 1.0D;
            dy = 0.0D;
            dz = 0.0D;
            length = 1.0D;
        }
        xd = dx / length * DRIFT_SPEED;
        yd = dy / length * DRIFT_SPEED;
        zd = dz / length * DRIFT_SPEED;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (age++ >= lifetime) {
            remove();
            return;
        }

        if (age < 5) {
            quadSize = targetScale * age / 5.0F;
        } else if (age < 10) {
            quadSize = targetScale;
        } else {
            quadSize = targetScale;
            alpha = 1.0F - (age - 10.0F) / 2.0F;
        }
        move(xd, yd, zd);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return CustomParticleRenderTypes.TRANSLUCENT_NO_DEPTH;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new BlackRedImpactParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
        }
    }
}
