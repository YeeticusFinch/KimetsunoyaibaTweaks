package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.KanrojiEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer for Mitsuri Kanroji entity using GeckoLib
 *
 * Uses the same texture and model as the base mod's Kanroji entity:
 * - Texture: kimetsunoyaiba:textures/entities/kanroji.png
 * - Model: kimetsunoyaiba:geo/biped.geo.json (or biped_kanroji.geo.json if in resourcepack)
 *
 * Supports resourcepack overrides:
 * - If a resourcepack provides biped_kanroji.geo.json, it will be used automatically
 * - If a resourcepack modifies kanroji.png, it will apply to this entity
 *
 * Uses the same model resolution logic as KimetsunoyaibaModels mod to ensure
 * our custom Kanroji entity uses the same resourcepack models as the base mod's Kanroji.
 */
public class KanrojiRenderer extends GeoEntityRenderer<KanrojiEntity> {
    private static ResourceLocation cachedModelResource = null;

    public KanrojiRenderer(EntityRendererProvider.Context context) {
        super(context, new GeoModel<KanrojiEntity>() {
            @Override
            public ResourceLocation getModelResource(KanrojiEntity entity) {
                // Check cache first
                if (cachedModelResource != null) {
                    return cachedModelResource;
                }

                // Default model
                ResourceLocation defaultModel = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "geo/biped.geo.json");

                // Try to find entity-specific model (biped_kanroji.geo.json)
                ResourceLocation specificModel = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "geo/biped_kanroji.geo.json");

                // Check if specific model exists in resourcepacks
                if (resourceExists(specificModel)) {
                    com.lerdorf.kimetsunoyaibamultiplayer.Log.info("[KanrojiRenderer] Using resourcepack model: {}", specificModel);
                    cachedModelResource = specificModel;
                    return specificModel;
                }

                // Fall back to default biped model
                com.lerdorf.kimetsunoyaibamultiplayer.Log.debug("[KanrojiRenderer] Using default biped model");
                cachedModelResource = defaultModel;
                return defaultModel;
            }

            @Override
            public ResourceLocation getTextureResource(KanrojiEntity entity) {
                // Use kanroji texture from base mod's namespace
                // This ensures resourcepack texture overrides work automatically
                return ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "textures/entities/kanroji.png");
            }

            @Override
            public ResourceLocation getAnimationResource(KanrojiEntity entity) {
                // Use biped animations from our mod (fallback to base mod if not found)
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "animations/biped.animation.json");
            }

            /**
             * Check if a resource exists in the resource manager
             */
            private boolean resourceExists(ResourceLocation location) {
                try {
                    ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
                    return resourceManager.getResource(location).isPresent();
                } catch (Exception e) {
                    return false;
                }
            }
        });

        // Add armor rendering layer (though Kanroji doesn't wear armor by default)
        this.addRenderLayer(new GeoArmorLayer<>(this));

        // Add equipment rendering layer - renders held items (nichirinsword_kanroji)
        this.addRenderLayer(new GeoEquipmentLayer<>(this));

        // Sword display on back/hip (GeoRenderLayer, matches player positioning)
        this.addRenderLayer(new GeoSwordDisplayLayer<>(this));

        // Use 1.1 scale to match base mod's Kanroji (slightly larger than normal)
        this.scaleHeight = 1.1F;
        this.scaleWidth = 1.1F;
    }

    @Override
    public void render(KanrojiEntity entity, float entityYaw, float partialTick,
                      com.mojang.blaze3d.vertex.PoseStack poseStack,
                      net.minecraft.client.renderer.MultiBufferSource bufferSource,
                      int packedLight) {
        // Set entity context for item rendering (so KanrojiSwordRenderer can detect the entity)
        com.lerdorf.kimetsunoyaibamultiplayer.client.EntityRenderContext.setCurrentEntity(entity);

        try {
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        } finally {
            // Clear entity context after rendering
            com.lerdorf.kimetsunoyaibamultiplayer.client.EntityRenderContext.clearCurrentEntity();
        }
    }

    @Override
    protected float getDeathMaxRotation(KanrojiEntity entityLivingBaseIn) {
        return 0.0F; // Match base mod (no rotation on death)
    }

    /**
     * Clear the cached model resource when resourcepacks are reloaded
     * This ensures we pick up new resourcepack models when F3+T is pressed
     */
    public static void clearModelCache() {
        cachedModelResource = null;
        com.lerdorf.kimetsunoyaibamultiplayer.Log.info("[KanrojiRenderer] Model cache cleared");
    }
}
