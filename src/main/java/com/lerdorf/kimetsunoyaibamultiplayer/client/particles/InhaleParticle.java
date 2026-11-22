package com.lerdorf.kimetsunoyaibamultiplayer.client.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Transparent one-shot inhale animation. */
public final class InhaleParticle extends TextureSheetParticle {
    private static final int FRAME_COUNT = 10;
    private final SpriteSet sprites;

    private InhaleParticle(ClientLevel level, double x, double y, double z,
                           double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.sprites = sprites;
        setSprite(sprites.get(0, FRAME_COUNT));
        lifetime = FRAME_COUNT;
        quadSize = 0.8F;
        alpha = 0.65F;
        hasPhysics = false;
        xd = xSpeed;
        yd = ySpeed;
        zd = zSpeed;
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

        setSprite(sprites.get(age, FRAME_COUNT));
        move(xd, yd, zd);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return CustomParticleRenderTypes.TRANSLUCENT_NO_DEPTH;
    }

    @OnlyIn(Dist.CLIENT)
    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public InhaleParticle createParticle(SimpleParticleType type, ClientLevel level, double x, double y,
                                             double z, double xSpeed, double ySpeed, double zSpeed) {
            return new InhaleParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
        }
    }
}
