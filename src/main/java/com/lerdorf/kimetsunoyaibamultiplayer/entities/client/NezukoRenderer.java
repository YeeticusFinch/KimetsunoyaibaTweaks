package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.client.CustomRenderTypes;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.NezukoEntity;
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

public class NezukoRenderer extends GeoEntityRenderer<NezukoEntity> {
    private static final class NezukoModel extends GeoModel<NezukoEntity> {
        private static final ResourceLocation MODEL_SMALL =
            ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "geo/nezuko_small.geo.json");
        private static final ResourceLocation MODEL_NORMAL =
            ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "geo/nezuko.geo.json");
        private static final ResourceLocation MODEL_AWAKENED =
            ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "geo/nezuko_awakened.geo.json");

        private static final ResourceLocation TEXTURE_SMALL =
            ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/nezuko_small.png");
        private static final ResourceLocation TEXTURE_NORMAL =
            ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/nezuko.png");
        private static final ResourceLocation TEXTURE_AWAKENED =
            ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/nezuko_awakened.png");

        private static final ResourceLocation BIPED_ANIMATION =
            ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "animations/biped.animation.json");

        @Override
        public ResourceLocation getModelResource(NezukoEntity animatable) {
            return switch (animatable.getStage()) {
                case SMALL -> MODEL_SMALL;
                case AWAKENED -> MODEL_AWAKENED;
                case NORMAL -> MODEL_NORMAL;
            };
        }

        @Override
        public ResourceLocation getTextureResource(NezukoEntity animatable) {
            return switch (animatable.getStage()) {
                case SMALL -> TEXTURE_SMALL;
                case AWAKENED -> TEXTURE_AWAKENED;
                case NORMAL -> TEXTURE_NORMAL;
            };
        }

        @Override
        public ResourceLocation getAnimationResource(NezukoEntity animatable) {
            return BIPED_ANIMATION;
        }
    }

    public NezukoRenderer(EntityRendererProvider.Context context) {
        super(context, new NezukoModel());
        this.addRenderLayer(new GeoEquipmentLayer<>(this));
        this.addRenderLayer(new NezukoEyesLayer(this));
    }

    @Override
    public void preRender(PoseStack poseStack, NezukoEntity animatable, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay, float red, float green,
                          float blue, float alpha) {
        this.scaleWidth = animatable.getStageScale();
        this.scaleHeight = animatable.getStageScale();
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick,
            packedLight, packedOverlay, red, green, blue, alpha);
    }

    private static class NezukoEyesLayer extends GeoRenderLayer<NezukoEntity> {
        private static final ResourceLocation EYES_SMALL =
            ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/nezuko_small_eyes.png");
        private static final ResourceLocation EYES_NORMAL =
            ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/nezuko_eyes.png");
        private static final ResourceLocation EYES_AWAKENED =
            ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/nezuko_awakened_eyes.png");

        private NezukoEyesLayer(NezukoRenderer renderer) {
            super(renderer);
        }

        private static ResourceLocation getEyesTexture(NezukoEntity animatable) {
            return switch (animatable.getStage()) {
                case SMALL -> EYES_SMALL;
                case AWAKENED -> EYES_AWAKENED;
                case NORMAL -> EYES_NORMAL;
            };
        }

        @Override
        public void render(PoseStack poseStack, NezukoEntity animatable, BakedGeoModel bakedModel, RenderType renderType,
                           MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight,
                           int packedOverlay) {
            ResourceLocation eyesTexture = getEyesTexture(animatable);
            RenderType eyesRenderType = RenderType.entityTranslucentEmissive(eyesTexture);
            getRenderer().reRender(
                bakedModel,
                poseStack,
                bufferSource,
                animatable,
                eyesRenderType,
                bufferSource.getBuffer(eyesRenderType),
                partialTick,
                0xF000F0,
                OverlayTexture.NO_OVERLAY,
                1.0F,
                1.0F,
                1.0F,
                1.0F
            );

            RenderType boostedEyesRenderType = CustomRenderTypes.geoEntityAdditive(eyesTexture);
            getRenderer().reRender(
                bakedModel,
                poseStack,
                bufferSource,
                animatable,
                boostedEyesRenderType,
                bufferSource.getBuffer(boostedEyesRenderType),
                partialTick,
                0xF000F0,
                OverlayTexture.NO_OVERLAY,
                1.65F,
                1.65F,
                1.65F,
                0.85F
            );
        }
    }
}
