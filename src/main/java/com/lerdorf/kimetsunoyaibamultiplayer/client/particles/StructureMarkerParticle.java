package com.lerdorf.kimetsunoyaibamultiplayer.client.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** Short-lived marker icon refreshed while a player holds the marker item. */
public class StructureMarkerParticle extends TextureSheetParticle {
    private StructureMarkerParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        setSprite(sprites.get(0, 1));
        quadSize = 0.75F;
        lifetime = 3;
        hasPhysics = false;
        alpha = 1.0F;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new StructureMarkerParticle(level, x, y, z, sprites);
        }
    }
}
