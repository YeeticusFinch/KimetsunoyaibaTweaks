package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.client.CustomRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class EyesGlowLayer<T extends LivingEntity & GeoEntity> extends GeoRenderLayer<T> {
    private static final float PULSE_PERIOD_TICKS = 40.0F;
    private static final float BASE_BRIGHTNESS = 1.35F;
    private static final float PEAK_BRIGHTNESS = 2.35F;
    private final GeoModel<T> overlayModel;

    public EyesGlowLayer(GeoRenderer<T> renderer, String modelPath, String texturePath, String animationPath) {
        super(renderer);
        this.overlayModel = new GeoModel<>() {
            @Override
            public ResourceLocation getModelResource(T animatable) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, modelPath);
            }

            @Override
            public ResourceLocation getTextureResource(T animatable) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, texturePath);
            }

            @Override
            public ResourceLocation getAnimationResource(T animatable) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, animationPath);
            }
        };
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, RenderType renderType,
                       MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight,
                       int packedOverlay) {
        BakedGeoModel overlayBakedModel = this.getGeoModel().getBakedModel(overlayModel.getModelResource(animatable));
        RenderType overlayRenderType = CustomRenderTypes.geoEntityTranslucentEmissive(
            overlayModel.getTextureResource(animatable)
        );
        float brightness = getPulsingBrightness(animatable, partialTick);

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
            brightness,
            brightness,
            brightness,
            1.0F
        );
    }

    private static float getPulsingBrightness(LivingEntity animatable, float partialTick) {
        float cycleProgress = ((animatable.tickCount + partialTick) % PULSE_PERIOD_TICKS) / PULSE_PERIOD_TICKS;
        float pulse = 0.5F + 0.5F * Mth.sin((cycleProgress * ((float) Math.PI * 2.0F)) - ((float) Math.PI / 2.0F));
        return Mth.lerp(pulse, BASE_BRIGHTNESS, PEAK_BRIGHTNESS);
    }
}
