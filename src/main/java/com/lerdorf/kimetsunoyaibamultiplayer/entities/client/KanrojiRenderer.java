package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.KanrojiEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer for Mitsuri Kanroji entity using GeckoLib
 *
 * Uses the Kanroji-specific model and textures bundled with this mod.
 */
public class KanrojiRenderer extends GeoEntityRenderer<KanrojiEntity> {
    public KanrojiRenderer(EntityRendererProvider.Context context) {
        super(context, new GeoModel<KanrojiEntity>() {
            @Override
            public ResourceLocation getModelResource(KanrojiEntity entity) {
                return ResourceLocation.fromNamespaceAndPath(
                    KimetsunoyaibaMultiplayer.MODID, "geo/biped_kanroji.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(KanrojiEntity entity) {
                if (entity != null && entity.isDemonized() && entity.getDemonizedTextureOverride() != null) {
                    return entity.getDemonizedTextureOverride();
                }
                return ResourceLocation.fromNamespaceAndPath(
                    KimetsunoyaibaMultiplayer.MODID, "textures/entity/kanroji.png");
            }

            @Override
            public ResourceLocation getAnimationResource(KanrojiEntity entity) {
                // Use biped animations from our mod (fallback to base mod if not found)
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "animations/biped.animation.json");
            }

        });

        // Add armor rendering layer (though Kanroji doesn't wear armor by default)
        this.addRenderLayer(new GeoArmorLayer<>(this));

        // Add equipment rendering layer - renders held items (nichirinsword_kanroji)
        this.addRenderLayer(new GeoEquipmentLayer<>(this));

        // Sword display on back/hip (GeoRenderLayer, matches player positioning)
        this.addRenderLayer(new GeoSwordDisplayLayer<>(this));
        this.addRenderLayer(new SkinLayersGeoLayer<>(this));
        this.addRenderLayer(new DemonSlayerDemonEyesLayer<>(this, "geo/biped_kanroji.geo.json", "animations/biped.animation.json"));

        // Add demon slayer mark rendering layer - renders Love mark on torso when activated
        this.addRenderLayer(new com.lerdorf.kimetsunoyaibamultiplayer.client.DemonSlayerMarkLayer<>(
            this,
            com.lerdorf.kimetsunoyaibamultiplayer.client.DemonSlayerMarkRegistry.LOVE_MARK
        ));

        // Use 0.95 scale
        this.scaleHeight = 0.95F;
        this.scaleWidth = 0.95F;
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
     * Kept for the shared resource reload hook. The model is now a fixed bundled resource.
     */
    public static void clearModelCache() {
    }
}
