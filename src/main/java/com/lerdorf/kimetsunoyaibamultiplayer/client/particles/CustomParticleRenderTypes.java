package com.lerdorf.kimetsunoyaibamultiplayer.client.particles;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;

/**
 * Custom particle render types for better translucent blending without depth writing.
 * This allows translucent particles (like mist) to not occlude other particles behind them.
 */
public class CustomParticleRenderTypes {

    /**
     * Translucent sheet that DOES NOT write depth (depth mask disabled) so particles behind remain visible.
     * Keep depth test enabled so particles still sort reasonably. Particle engine sorts quads by distance.
     */
    public static final ParticleRenderType TRANSLUCENT_NO_DEPTH = new ParticleRenderType() {
        @Override
        public void begin(BufferBuilder bufferbuilder, TextureManager textureManager) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(false); // don't write depth
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);

            bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public void end(Tesselator tesselator) {
            tesselator.end();
            // Re-enable depth writes for subsequent render types
            RenderSystem.depthMask(true);
        }

        @Override
        public String toString() {
            return "TRANSLUCENT_NO_DEPTH";
        }
    };

    /**
     * Additive blending for bright, glowing particles (like love_impact and love_slash).
     * Uses additive blend mode (src + dst) for maximum brightness.
     * Does not write depth so other particles remain visible behind it.
     */
    public static final ParticleRenderType ADDITIVE_TRANSLUCENT = new ParticleRenderType() {
        @Override
        public void begin(BufferBuilder bufferbuilder, TextureManager textureManager) {
            RenderSystem.enableBlend();
            // Additive blending: GL_SRC_ALPHA, GL_ONE (add source to destination)
            RenderSystem.blendFunc(
                com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE
            );
            RenderSystem.depthMask(false); // don't write depth
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);

            bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public void end(Tesselator tesselator) {
            tesselator.end();
            // Reset to default blend func
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(true);
        }

        @Override
        public String toString() {
            return "ADDITIVE_TRANSLUCENT";
        }
    };
}
