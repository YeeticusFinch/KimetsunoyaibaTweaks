package com.lerdorf.kimetsunoyaibamultiplayer.client.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class BloodParticle extends TextureSheetParticle {
    private final SpriteSet spriteSet;
    private boolean splattered = false;

    protected BloodParticle(ClientLevel level, double x, double y, double z,
                            double xSpeed, double ySpeed, double zSpeed,
                            SpriteSet spriteSet) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.spriteSet = spriteSet;
        this.setSprite(spriteSet.get(0, 1));

        this.xd = xSpeed + (this.random.nextDouble() - 0.5D) * 0.08D;
        this.yd = ySpeed + this.random.nextDouble() * 0.08D;
        this.zd = zSpeed + (this.random.nextDouble() - 0.5D) * 0.08D;
        this.gravity = 1.2F;
        this.friction = 0.92F;
        this.quadSize = 0.08F + this.random.nextFloat() * 0.14F;
        this.lifetime = 25 + this.random.nextInt(20);
        this.hasPhysics = true;
        this.rCol = 1.0F;
        this.gCol = 1.0F;
        this.bCol = 1.0F;
        this.alpha = 1.0F;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.oRoll = this.roll;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        if (this.splattered) {
            return;
        }

        double prevX = this.x;
        double prevY = this.y;
        double prevZ = this.z;
        double prevXd = this.xd;
        double prevYd = this.yd;
        double prevZd = this.zd;

        this.yd -= 0.04D * this.gravity;
        this.move(this.xd, this.yd, this.zd);
        this.xd *= this.friction;
        this.yd *= this.friction;
        this.zd *= this.friction;

        boolean collided =
            this.onGround
                || (Math.abs(this.x - prevX) < 1.0E-4D && Math.abs(prevXd) > 1.0E-3D)
                || (Math.abs(this.y - prevY) < 1.0E-4D && Math.abs(prevYd) > 1.0E-3D)
                || (Math.abs(this.z - prevZ) < 1.0E-4D && Math.abs(prevZd) > 1.0E-3D);

        if (collided) {
            this.splattered = true;
            this.xd = 0.0D;
            this.yd = 0.0D;
            this.zd = 0.0D;
            this.gravity = 0.0F;
            this.quadSize *= 1.6F;
            this.roll = this.random.nextFloat() * ((float) Math.PI * 2.0F);
            this.oRoll = this.roll;
            this.setSprite(this.spriteSet.get(1, 1));
            this.lifetime = Math.max(this.lifetime, this.age + 40 + this.random.nextInt(20));
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
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
            return new BloodParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }
}
