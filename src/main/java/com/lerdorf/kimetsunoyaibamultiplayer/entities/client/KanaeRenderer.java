package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.KanaeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer for Kanae using base mod assets with resourcepack-aware model fallback.
 */
public class KanaeRenderer extends GeoEntityRenderer<KanaeEntity> {
    private static ResourceLocation cachedModelResource = null;

    public KanaeRenderer(EntityRendererProvider.Context context) {
        super(context, new GeoModel<KanaeEntity>() {
            @Override
            public ResourceLocation getModelResource(KanaeEntity entity) {
                if (cachedModelResource != null) {
                    return cachedModelResource;
                }

                ResourceLocation defaultModel = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "geo/biped.geo.json");
                ResourceLocation specificModel = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "geo/biped_kanae.geo.json");

                if (resourceExists(specificModel)) {
                    cachedModelResource = specificModel;
                    return specificModel;
                }

                cachedModelResource = defaultModel;
                return defaultModel;
            }

            @Override
            public ResourceLocation getTextureResource(KanaeEntity entity) {
                return ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "textures/entities/kanae.png");
            }

            @Override
            public ResourceLocation getAnimationResource(KanaeEntity entity) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "animations/biped.animation.json");
            }

            private boolean resourceExists(ResourceLocation location) {
                try {
                    ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
                    return resourceManager.getResource(location).isPresent();
                } catch (Exception e) {
                    return false;
                }
            }
        });

        this.addRenderLayer(new GeoArmorLayer<>(this));
        this.addRenderLayer(new GeoEquipmentLayer<>(this));
        this.addRenderLayer(new GeoSwordDisplayLayer<>(this));
        this.addRenderLayer(new DemonSlayerDemonEyesLayer<>(this, "geo/biped.geo.json", "animations/biped.animation.json"));
        this.scaleHeight = 1.0F;
        this.scaleWidth = 1.0F;
    }

    @Override
    public void render(KanaeEntity entity, float entityYaw, float partialTick,
                       com.mojang.blaze3d.vertex.PoseStack poseStack,
                       net.minecraft.client.renderer.MultiBufferSource bufferSource,
                       int packedLight) {
        com.lerdorf.kimetsunoyaibamultiplayer.client.EntityRenderContext.setCurrentEntity(entity);
        try {
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        } finally {
            com.lerdorf.kimetsunoyaibamultiplayer.client.EntityRenderContext.clearCurrentEntity();
        }
    }

    @Override
    protected float getDeathMaxRotation(KanaeEntity entityLivingBaseIn) {
        return 0.0F;
    }

    public static void clearModelCache() {
        cachedModelResource = null;
    }
}
