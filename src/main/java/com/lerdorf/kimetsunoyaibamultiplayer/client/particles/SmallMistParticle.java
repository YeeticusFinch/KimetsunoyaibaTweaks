package com.lerdorf.kimetsunoyaibamultiplayer.client.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class SmallMistParticle extends TextureSheetParticle {
    private final double initialX;
    private final double initialY;
    private final double initialZ;
    private final SpriteSet spriteSet;

    protected SmallMistParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet spriteSet) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        
        this.spriteSet = spriteSet;
        
        // Set particle properties
        this.initialX = x;
        this.initialY = y;
        this.initialZ = z;
        
        // Start with the first frame of the animation (mistsmall)
        this.setSpriteFromTexture(0); // Will animate through available frames (0-5, but will cycle through available ones)
        
        // Set movement parameters - smaller particles should be lighter
        this.xd = xSpeed * 0.3; // Slower horizontal movement
        this.yd = Math.abs(ySpeed) < 0.01 ? 0.001 : ySpeed * 0.1; // Very slow upward drift
        this.zd = zSpeed * 0.3; // Slower horizontal movement
        
        // Set particle scale (smaller than regular mist)
        this.quadSize = 0.4f + (float)(Math.random() * 0.3f); // Smaller size between 0.4 and 0.7
        
        // Set particle lifetime (shorter than regular mist)
        this.lifetime = 100 + (int)(Math.random() * 60); // 5-8 seconds at 20tps
        
        // Set particle color to white/light gray
        this.rCol = 0.9f; // Slightly less bright than regular mist
        this.gCol = 0.9f;
        this.bCol = 0.9f;
        
        // Make the particle slightly transparent
        this.alpha = 0.7f + (float)(Math.random() * 0.2f);
        
        // Make particle ignore light settings (emit light)
        this.hasPhysics = false;
    }

    @Override
    public void tick() {
        super.tick();

        // Animate through the frames - update sprite every 10 ticks
        if (this.age % 10 == 0 && this.age < this.lifetime) {
            // Play through frames 0-4 once (5 frames total), then stay on the last frame
            int frame = Math.min((this.age / 10), 4); // Max frame is 4 (frames 0-4)
            this.setSpriteFromTexture(frame);
        }
        
        // Add some gentle, random drift
        this.xd += (random.nextGaussian() * 0.001);
        this.zd += (random.nextGaussian() * 0.001);
        
        // Very slow movement - almost floating
        this.yd += 0.0005; // Gentle upward movement
        
        // Limit how fast it moves vertically
        if (this.yd > 0.01) {
            this.yd = 0.01; // Prevent too fast upward movement
        } else if (this.yd < -0.01) {
            this.yd = -0.01; // Prevent too fast downward movement
        }
        
        // Slow down horizontal movement gradually
        this.xd *= 0.98;
        this.zd *= 0.98;
        
        // Fade out as the particle ages
        float lifeFraction = (float) this.age / (float) this.lifetime;
        this.alpha = 0.7f * (1.0f - lifeFraction); // Fade out toward the end of lifetime
        
        // Disappear completely when lifetime is reached
        if (this.age >= this.lifetime) {
            this.remove(); // Remove the particle when its lifetime is over
        }
    }

    private void setSpriteFromTexture(int frame) {
        if (frame >= 0 && frame <= 4) {
            try {
                // Get the specific frame (0-4 for 5 total frames)
                this.setSprite(this.spriteSet.get(frame, 4));
            } catch (Exception e) {
                // If the specific frame doesn't exist, use the base frame as fallback
                this.setSprite(this.spriteSet.get(0, 4));
            }
        } else {
            this.setSprite(this.spriteSet.get(0, 4));
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return CustomParticleRenderTypes.TRANSLUCENT_NO_DEPTH;
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
            return new SmallMistParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }
}
