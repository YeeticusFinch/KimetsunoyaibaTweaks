package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Custom render types for ultra-bright sword slash models with bloom
 */
@OnlyIn(Dist.CLIENT)
public class CustomRenderTypes extends RenderType {

    // Dummy constructor (never called, just needed for extending RenderType)
    private CustomRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                              boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    /**
     * Ultra-bright render type with additive blending for sword slashes
     * This creates an additive blend mode where colors ADD together instead of replacing
     * Result: Bright areas become MUCH brighter (perfect for bloom with shaders)
     *
     * FIXED: Uses NEW_ENTITY format to match vertex calls with overlay and normal
     */
    public static RenderType swordSlashAdditive(ResourceLocation texture) {
        CompositeState renderState = CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER) // FIXED: Use entity shader for NEW_ENTITY format
                .setTextureState(new TextureStateShard(texture, false, false)) // Texture
                .setTransparencyState(ADDITIVE_TRANSPARENCY) // ADDITIVE BLENDING - colors add together!
                .setLightmapState(LIGHTMAP) // Use lightmap (for full bright)
                .setOverlayState(OVERLAY) // RESTORED: Overlay required for NEW_ENTITY format
                .setCullState(NO_CULL) // Don't cull faces
                .setWriteMaskState(COLOR_WRITE) // Write colors
                .setOutputState(TRANSLUCENT_TARGET) // Render to translucent target for proper blending
                .createCompositeState(true); // Affects outline

        return create(
                "sword_slash_additive",
                DefaultVertexFormat.NEW_ENTITY, // FIXED: Use NEW_ENTITY format to match vertex calls
                VertexFormat.Mode.QUADS,
                256,
                true,
                true,
                renderState
        );
    }

    /**
     * Alternative: Multiply the brightness using a custom shader state
     * This version uses standard translucent emissive but with enhanced settings
     */
    public static RenderType swordSlashBright(ResourceLocation texture) {
        CompositeState renderState = CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER) // Emissive shader
                .setTextureState(new TextureStateShard(texture, false, false)) // Texture
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY) // Translucent transparency
                .setLightmapState(LIGHTMAP) // Use lightmap
                .setOverlayState(OVERLAY) // Use overlay
                .setCullState(NO_CULL) // Don't cull faces
                .setWriteMaskState(COLOR_WRITE) // Write colors
                .setOutputState(TRANSLUCENT_TARGET) // Render to translucent target
                .createCompositeState(true); // Affects outline

        return create(
                "sword_slash_bright",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                256,
                true,
                true,
                renderState
        );
    }
}
