package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.DemonVillagerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class DemonVillagerRenderer extends GeoEntityRenderer<DemonVillagerEntity> {
    private static final GeoModel<DemonVillagerEntity> BASE_MODEL = new GeoModel<>() {
        @Override
        public ResourceLocation getModelResource(DemonVillagerEntity entity) {
            return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "geo/biped_villager.geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(DemonVillagerEntity entity) {
            return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/demon_villager.png");
        }

        @Override
        public ResourceLocation getAnimationResource(DemonVillagerEntity entity) {
            return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "animations/biped.animation.json");
        }
    };

    public DemonVillagerRenderer(EntityRendererProvider.Context context) {
        super(context, BASE_MODEL);
        this.addRenderLayer(new EyesGlowLayer<>(this, "geo/biped_villager.geo.json",
            "textures/entity/demon_villager_eyes.png", "animations/biped.animation.json"));
        this.addRenderLayer(new ClothesLayer(this));
    }

    private static class ClothesLayer extends GeoRenderLayer<DemonVillagerEntity> {
        private final GeoModel<DemonVillagerEntity> clothesModel = new GeoModel<>() {
            @Override
            public ResourceLocation getModelResource(DemonVillagerEntity entity) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "geo/biped_villager.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(DemonVillagerEntity entity) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/demon_villager_clothes.png");
            }

            @Override
            public ResourceLocation getAnimationResource(DemonVillagerEntity entity) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "animations/biped.animation.json");
            }
        };

        private ClothesLayer(DemonVillagerRenderer renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack poseStack, DemonVillagerEntity animatable, BakedGeoModel bakedModel, RenderType renderType,
                           MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
            BakedGeoModel overlayBakedModel = this.getGeoModel().getBakedModel(clothesModel.getModelResource(animatable));
            RenderType overlayRenderType = RenderType.entityTranslucent(clothesModel.getTextureResource(animatable));
            getRenderer().reRender(
                overlayBakedModel,
                poseStack,
                bufferSource,
                animatable,
                overlayRenderType,
                bufferSource.getBuffer(overlayRenderType),
                partialTick,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                1.0F,
                1.0F,
                1.0F,
                1.0F
            );
        }
    }
}
