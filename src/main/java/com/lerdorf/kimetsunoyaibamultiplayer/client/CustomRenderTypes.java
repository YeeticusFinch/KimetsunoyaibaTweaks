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

    /**
     * GeckoLib-compatible additive render type for love sword slashes
     * Uses additive blending to avoid depth sorting issues with clouds/water
     * while maintaining compatibility with GeckoLib's rendering system
     */
    public static RenderType geoEntityAdditive(ResourceLocation texture) {
        CompositeState renderState = CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER) // Entity shader for GeckoLib
                .setTextureState(new TextureStateShard(texture, false, false)) // Texture
                .setTransparencyState(ADDITIVE_TRANSPARENCY) // ADDITIVE BLENDING - avoids depth issues
                .setLightmapState(LIGHTMAP) // Use lightmap
                .setOverlayState(OVERLAY) // Required for entity format
                .setCullState(NO_CULL) // Don't cull faces
                .setWriteMaskState(COLOR_WRITE) // Write colors only (not depth)
                .setOutputState(TRANSLUCENT_TARGET) // Render to translucent target
                .createCompositeState(true); // Affects outline

        return create(
                "geo_entity_additive",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                256,
                true,
                true,
                renderState
        );
    }

    /**
     * GeckoLib-compatible translucent emissive render type with proper alpha blending
     * Uses translucent transparency for proper partial alpha/translucency support
     * Combined with emissive shader for glowing appearance
     *
     * This is the correct render type for models with alpha gradients/partial transparency.
     * Use this instead of geoEntityAdditive when you need smooth alpha blending.
     */
    public static RenderType geoEntityTranslucentEmissive(ResourceLocation texture) {
            CompositeState renderState = CompositeState.builder()
                            .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER) // Emissive shader for glow
                            .setTextureState(new TextureStateShard(texture, false, false)) // Texture
                            .setTransparencyState(TRANSLUCENT_TRANSPARENCY) // TRANSLUCENT BLENDING - proper alpha support
                            .setLightmapState(LIGHTMAP) // Use lightmap
                            .setOverlayState(OVERLAY) // Required for entity format
                            .setCullState(NO_CULL) // Don't cull faces
                            .setWriteMaskState(COLOR_DEPTH_WRITE) // Write both color and depth for proper sorting
                            .setOutputState(TRANSLUCENT_TARGET) // Render to translucent target
                            .createCompositeState(true); // Affects outline

            return create(
                            "geo_entity_translucent_emissive",
                            DefaultVertexFormat.NEW_ENTITY,
                            VertexFormat.Mode.QUADS,
                            256,
                            true,
                            true,
                            renderState);
    }
    
    public static RenderType geoEntityTranslucentFullbright(ResourceLocation texture) {
        CompositeState renderState = CompositeState.builder()
                // IMPORTANT:
                // Use the ordinary entity translucent shader.
                // Iris/Oculus knows how to substitute this path.
                .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)

                .setTextureState(new TextureStateShard(texture, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setCullState(NO_CULL)

                // Keep this if you need the overlay itself to write depth.
                .setWriteMaskState(COLOR_DEPTH_WRITE)

                .setOutputState(TRANSLUCENT_TARGET)
                .createCompositeState(true);

        return create(
                "geo_entity_translucent_fullbright",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                256,
                true,
                true,
                renderState
        );
        }
}
