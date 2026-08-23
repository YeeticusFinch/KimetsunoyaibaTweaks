package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.client.CustomRenderTypes;
import com.lerdorf.kimetsunoyaibamultiplayer.client.DemonEyesResourceHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.BreathingSlayerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class DemonSlayerDemonEyesLayer<T extends BreathingSlayerEntity> extends GeoRenderLayer<T> {
    private final GeoModel<T> overlayModel;

    public DemonSlayerDemonEyesLayer(GeoRenderer<T> renderer, String modelPath, String animationPath) {
        super(renderer);
        this.overlayModel = new GeoModel<>() {
            @Override
            public ResourceLocation getModelResource(T animatable) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, modelPath);
            }

            @Override
            public ResourceLocation getTextureResource(T animatable) {
                ResourceLocation override = animatable.getDemonizedEyesTextureOverride();
                if (override != null) {
                    return override;
                }
                return DemonEyesResourceHelper.getTexture(animatable.getDemonEyesIndex());
            }

            @Override
            public ResourceLocation getAnimationResource(T animatable) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, animationPath);
            }
        };
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel,
                       RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                       float partialTick, int packedLight, int packedOverlay) {
        if (!animatable.isDemonized() || animatable.isInvisible()) {
            return;
        }

        BakedGeoModel overlayBakedModel = this.getGeoModel().getBakedModel(overlayModel.getModelResource(animatable));
        RenderType overlayRenderType = CustomRenderTypes.geoEntityTranslucentEmissive(
            overlayModel.getTextureResource(animatable)
        );
        float[] tint = DemonEyesResourceHelper.getHueTint(animatable.getDemonEyesHue());
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
            tint[0],
            tint[1],
            tint[2],
            1.0F
        );
    }
}
