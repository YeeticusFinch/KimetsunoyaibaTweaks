package com.lerdorf.kimetsunoyaibamultiplayer.client.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class MistParticle extends TextureSheetParticle {
    private final double initialX;
    private final double initialY;
    private final double initialZ;
    private final SpriteSet spriteSet;

    protected MistParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet spriteSet) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        
        this.spriteSet = spriteSet;
        
        // Set particle properties
        this.initialX = x;
        this.initialY = y;
        this.initialZ = z;
        
        // Randomly choose between two textures
        int textureIndex = (int)(Math.random() * 2); // This will use either 0 or 1
        this.setSprite(spriteSet.get(textureIndex, 1)); // Use the appropriate sprite
        
        // Set movement parameters
        this.xd = xSpeed; // Random slow horizontal movement
        this.yd = ySpeed; // Slow downward movement (settling)
        this.zd = zSpeed; // Random slow horizontal movement
        
        // Set particle scale (size)
        this.quadSize = 0.9f + (float)(Math.random() * 1.1f); // Random size between 0.9 and 2.0
        
        // Set particle lifetime (10-15 seconds at 20tps = 200-300 ticks)
        this.lifetime = 200 + (int)(Math.random() * 100);
        
        // Set particle color to white/light gray (no random variation)
        this.rCol = 0.95f; // White
        this.gCol = 0.95f; // White
        this.bCol = 0.95f; // White
        
        // Make the particle slightly transparent
        this.alpha = 0.9f + (float)(Math.random() * 0.1f);
        
        // Make particle ignore light settings (emit light)
        this.hasPhysics = false;
    }

    @Override
    public void tick() {
        super.tick();
        
        // Add some gentle, random drift
        this.xd += (random.nextGaussian() * 0.002);
        this.zd += (random.nextGaussian() * 0.002);
        
        // Very slow descent - only slightly downward
        this.yd -= 0.0009; // Gentle gravity
        
        // Add some buoyancy so it doesn't fall too quickly
        if (this.yd < -0.01) {
            this.yd = -0.01; // Limit how fast it falls
        }
        
        // Occasionally move upward slightly to simulate mist floating behavior
        if (random.nextDouble() < 0.02) { // 2% chance per tick
            this.yd += 0.002;
        }
        
        // Slow down horizontal movement gradually
        this.xd *= 0.99;
        this.zd *= 0.99;
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
            return new MistParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }
}