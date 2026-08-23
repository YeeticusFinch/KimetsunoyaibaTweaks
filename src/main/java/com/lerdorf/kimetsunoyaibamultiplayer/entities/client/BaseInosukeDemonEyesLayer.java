package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.client.CustomRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mcreator.kimetsunoyaiba.entity.InosukeEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class BaseInosukeDemonEyesLayer extends GeoRenderLayer<InosukeEntity> {
    private static final ResourceLocation EYES_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/oni_inosuke_eyes.png");
    private final GeoModel<InosukeEntity> overlayModel = new GeoModel<>() {
        @Override
        public ResourceLocation getModelResource(InosukeEntity entity) {
            return ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "geo/biped.geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(InosukeEntity entity) {
            return EYES_TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(InosukeEntity entity) {
            return ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "animations/biped.animation.json");
        }
    };

    public BaseInosukeDemonEyesLayer(GeoRenderer<InosukeEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, InosukeEntity animatable, BakedGeoModel bakedModel, RenderType renderType,
                       MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight,
                       int packedOverlay) {
        if (animatable == null || animatable.isInvisible() || !animatable.getPersistentData().getBoolean("oni")) {
            return;
        }

        BakedGeoModel overlayBakedModel = this.getGeoModel().getBakedModel(overlayModel.getModelResource(animatable));
        RenderType overlayRenderType = CustomRenderTypes.geoEntityTranslucentEmissive(EYES_TEXTURE);
        getRenderer().reRender(
            overlayBakedModel,
            poseStack,
            bufferSource,
            animatable,
            overlayRenderType,
            bufferSource.getBuffer(overlayRenderType),
            partialTick,
            0xF000F0,
            OverlayTexture.NO_OVERLAY,
            1.0F,
            1.0F,
            1.0F,
            1.0F
        );
    }
}
