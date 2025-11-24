package com.lerdorf.kimetsunoyaibamultiplayer.client.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Love Slash particle for Kanroji's Love Breathing.
 *
 * Behavior:
 * - Starts at a random texture (0-7) and cycles through all 8
 * - Changes frame once per tick (e.g., 3 -> 4 -> 5 -> 6 -> 7 -> 0 -> 1 -> 2)
 * - Lives for exactly 8 ticks (one full cycle)
 *
 * Uses additive blending for bright, glowing effect.
 */
public class LoveSlashParticle extends TextureSheetParticle {
    private final SpriteSet spriteSet;
    private final int startFrame;
    private final int frameCount; // Random 4-6 frames
    private static final int TOTAL_FRAMES = 20;
    private final short type;

    protected LoveSlashParticle(ClientLevel level, double x, double y, double z,
                                 double xSpeed, double ySpeed, double zSpeed,
                                 SpriteSet spriteSet) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        
        type = (short)(Math.random()*5);

        this.spriteSet = spriteSet;

        switch (type) {
        	case 2: // love_slash_heart
        		// Random lifetime between 4-6 ticks
                this.frameCount = 6 + (int)(Math.random()); // 6 or 7
                this.lifetime = this.frameCount;

                // Start at a random frame
                this.startFrame = 8;
                
             // Set initial sprite
                this.setSprite(spriteSet.get(startFrame, TOTAL_FRAMES - 1));
        		break;
        	case 3: // love_slash_knot
                this.frameCount = 5;
                this.lifetime = this.frameCount;

                // Start at first knot frame
                this.startFrame = 15;

             // Set initial sprite
                this.setSprite(spriteSet.get(startFrame, TOTAL_FRAMES - 1));
        		break;
        	case 4: // love_slash_knot (backwards)
        		 this.frameCount = 5;
                 this.lifetime = this.frameCount;

                 // Start at last knot frame
                 this.startFrame = 19;

              // Set initial sprite
                 this.setSprite(spriteSet.get(startFrame, TOTAL_FRAMES - 1));
        		break;
        	default:
        	case 0:
        	case 1:
        		// love_slash (the circular one)
        		// Random lifetime between 4-6 ticks
                this.frameCount = 4 + (int)(Math.random() * 3); // 4, 5, or 6
                this.lifetime = this.frameCount;

                // Start at a random frame
                this.startFrame = (int)(Math.random() * 8); // 8 frames in this type
                
             // Set initial sprite
                this.setSprite(spriteSet.get(startFrame, TOTAL_FRAMES - 1));
        		break;
        }
        
        

        // Set movement - slight random drift
        this.xd = (Math.random() - 0.5) * 0.08;
        this.yd = (Math.random() - 0.5) * 0.08;
        this.zd = (Math.random() - 0.5) * 0.08;

        // Set scale - random 1x to 3x
        this.quadSize = 1.0f + (float)(Math.random() * 2.0f); // 1.0-3.0

        // No tint - use texture colors directly (white = no tint)
        this.rCol = 1.0f;
        this.gCol = 1.0f;
        this.bCol = 1.0f;

        // Fully visible
        this.alpha = 1.0f;

        // No physics
        this.hasPhysics = false;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        this.age++;
        if (this.age >= this.lifetime) {
            this.remove();
            return;
        }

        // Calculate current frame with modulo cycling (only plays frameCount frames)
        int currentFrame = startFrame + (type == 4 ? -this.age : this.age);
        if (type <= 1)
        	currentFrame %= 8; // modulo cycling is just for type 0 and type 1
        this.setSprite(spriteSet.get(currentFrame, TOTAL_FRAMES - 1));

        // Slight fade towards end for smoother transition
        if (this.age >= this.frameCount - 2) {
            this.alpha = 1.0f - ((float)(this.age - (this.frameCount - 2)) / 2.0f);
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
            return new LoveSlashParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }
}
