package com.lerdorf.kimetsunoyaibamultiplayer.client.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class BloodFlameParticle extends TextureSheetParticle {
    private static final int SPRITE_COUNT = 6;

    protected BloodFlameParticle(ClientLevel level, double x, double y, double z,
                                 double xSpeed, double ySpeed, double zSpeed, SpriteSet spriteSet) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);

        int spriteIndex = this.random.nextInt(SPRITE_COUNT);
        this.setSprite(spriteSet.get(spriteIndex, SPRITE_COUNT - 1));

        double baseSpeed = 0.015D + (this.random.nextDouble() * 0.03D);
        double directionLength = Math.sqrt((xSpeed * xSpeed) + (ySpeed * ySpeed) + (zSpeed * zSpeed));

        if (directionLength > 1.0E-6D) {
            double speed = baseSpeed + Mth.clamp(directionLength * 0.28D, 0.0D, 0.16D);
            this.xd = (xSpeed / directionLength) * speed;
            this.yd = (ySpeed / directionLength) * speed;
            this.zd = (zSpeed / directionLength) * speed;
        } else {
            double yaw = this.random.nextDouble() * (Math.PI * 2.0D);
            double pitch = (this.random.nextDouble() - 0.5D) * 0.9D;
            double speed = baseSpeed + (this.random.nextDouble() * 0.02D);
            this.xd = Math.cos(yaw) * Math.cos(pitch) * speed;
            this.yd = Math.sin(pitch) * speed;
            this.zd = Math.sin(yaw) * Math.cos(pitch) * speed;
        }

        this.gravity = -0.005F;
        this.friction = 0.93F;
        this.quadSize = 0.15F + (this.random.nextFloat() * 0.18F);
        this.lifetime = 18 + this.random.nextInt(18);
        this.alpha = 0.95F;
        this.rCol = 1.0F;
        this.gCol = 1.0F;
        this.bCol = 1.0F;
        this.hasPhysics = false;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.age > this.lifetime - 6) {
            float remaining = (float) (this.lifetime - this.age) / 6.0F;
            this.alpha = Math.max(0.0F, remaining);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
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
            return new BloodFlameParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }
}