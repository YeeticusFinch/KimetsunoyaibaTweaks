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

/** Animated dark red, fullbright slash particle for Black Blood Brambles. */
public class BlackRedSlashParticle extends TextureSheetParticle {
    private static final double DRIFT_SPEED = 0.04D;
    private static final int SMALL_SLICE_FRAME_COUNT = 7;
    private static final int SLICE_FRAME_COUNT = 8;
    private static final int SLASH_FRAME_COUNT = 8;
    private static final int KNOT_FRAME_COUNT = 5;
    private static final int SLICE_FRAME_OFFSET = SMALL_SLICE_FRAME_COUNT;
    private static final int SLASH_FRAME_OFFSET = SLICE_FRAME_OFFSET + SLICE_FRAME_COUNT;
    private static final int KNOT_FRAME_OFFSET = SLASH_FRAME_OFFSET + SLASH_FRAME_COUNT;
    private static final int TOTAL_FRAME_COUNT = KNOT_FRAME_OFFSET + KNOT_FRAME_COUNT;
    private final SpriteSet sprites;
    private final int frameOffset;
    private final int frameCount;
    private final int startFrame;

    protected BlackRedSlashParticle(ClientLevel level, double x, double y, double z,
                                    double xSpeed, double ySpeed, double zSpeed,
                                    SpriteSet sprites) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.sprites = sprites;
        if (level.random.nextInt(4) < 3) {
            boolean slice = level.random.nextBoolean();
            if (slice) {
                frameOffset = SLICE_FRAME_OFFSET;
                frameCount = SLICE_FRAME_COUNT;
            } else {
                frameOffset = 0;
                frameCount = SMALL_SLICE_FRAME_COUNT;
            }
        } else if (level.random.nextBoolean()) {
            frameOffset = SLASH_FRAME_OFFSET;
            frameCount = SLASH_FRAME_COUNT;
        } else {
            frameOffset = KNOT_FRAME_OFFSET;
            frameCount = KNOT_FRAME_COUNT;
        }
        startFrame = level.random.nextInt(frameCount);
        roll = level.random.nextFloat() * ((float) Math.PI * 2.0F);
        oRoll = roll;
        setSprite(sprites.get(frameOffset + startFrame, TOTAL_FRAME_COUNT));
        lifetime = 8;
        quadSize = 1.0F + (float) (Math.random() * 2.0D);
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

        setSprite(sprites.get(frameOffset + (startFrame + age) % frameCount, TOTAL_FRAME_COUNT));
        if (age >= lifetime - 2) {
            alpha = 1.0F - (age - (lifetime - 2.0F)) / 2.0F;
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
            return new BlackRedSlashParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
        }
    }
}
