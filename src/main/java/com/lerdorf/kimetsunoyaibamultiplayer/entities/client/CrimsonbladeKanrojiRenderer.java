package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.CrimsonbladeKanrojiEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CrimsonbladeKanrojiRenderer extends GeoEntityRenderer<CrimsonbladeKanrojiEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
        "crimsonblade", "geo/kanroji.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        "crimsonblade", "textures/entity/kanroji.png");
    private static final ResourceLocation CRIMSONBLADE_ANIMATIONS = ResourceLocation.fromNamespaceAndPath(
        "crimsonblade", "animations/kanroji.animation.json");
    private static final ResourceLocation BIPED_ANIMATIONS = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID, "animations/biped.animation.json");

    public CrimsonbladeKanrojiRenderer(EntityRendererProvider.Context context) {
        super(context, new GeoModel<CrimsonbladeKanrojiEntity>() {
            @Override
            public ResourceLocation getModelResource(CrimsonbladeKanrojiEntity entity) {
                return MODEL;
            }

            @Override
            public ResourceLocation getTextureResource(CrimsonbladeKanrojiEntity entity) {
                return TEXTURE;
            }

            @Override
            public ResourceLocation getAnimationResource(CrimsonbladeKanrojiEntity entity) {
                if (entity.isDeadOrDying()) {
                    return BIPED_ANIMATIONS;
                }
                if ("crimsonblade".equals(entity.getCurrentAnimationNamespace())
                    || (entity.getAnimationTicks() == 0 && "idle".equals(entity.getCurrentAnimation()))) {
                    return CRIMSONBLADE_ANIMATIONS;
                }
                return BIPED_ANIMATIONS;
            }
        });

        this.addRenderLayer(new GeoArmorLayer<>(this));
        this.addRenderLayer(new GeoEquipmentLayer<>(this));
        this.addRenderLayer(new GeoSwordDisplayLayer<>(this));
        this.addRenderLayer(new SkinLayersGeoLayer<>(this));
        this.addRenderLayer(new com.lerdorf.kimetsunoyaibamultiplayer.client.BiokinesesGeoAnchorLayer<>(this));
        this.addRenderLayer(new DemonSlayerDemonEyesLayer<>(this, "geo/kanroji.geo.json",
            "animations/biped.animation.json"));
        this.addRenderLayer(new com.lerdorf.kimetsunoyaibamultiplayer.client.DemonSlayerMarkLayer<>(
            this, com.lerdorf.kimetsunoyaibamultiplayer.client.DemonSlayerMarkRegistry.LOVE_MARK));

        this.scaleHeight = 0.95F;
        this.scaleWidth = 0.95F;
    }

    @Override
    public void render(CrimsonbladeKanrojiEntity entity, float entityYaw, float partialTick,
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
}
