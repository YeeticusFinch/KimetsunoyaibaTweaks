package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.client.CustomRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/** Demon-eye overlay shared by the base-mod named slayers. */
public class BaseNamedDemonEyesLayer<T extends GeoAnimatable> extends GeoRenderLayer<T> {
    private final ResourceLocation eyesTexture;
    private final GeoModel<T> overlayModel;

    public BaseNamedDemonEyesLayer(GeoRenderer<T> renderer, ResourceLocation eyesTexture) {
        super(renderer);
        this.eyesTexture = eyesTexture;
        this.overlayModel = new GeoModel<>() {
            @Override
            public ResourceLocation getModelResource(T entity) {
                return ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "geo/biped.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(T entity) {
                return BaseNamedDemonEyesLayer.this.eyesTexture;
            }

            @Override
            public ResourceLocation getAnimationResource(T entity) {
                return ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "animations/biped.animation.json");
            }
        };
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, RenderType renderType,
                       MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight,
                       int packedOverlay) {
        if (!(animatable instanceof Entity entity)
            || entity.isInvisible()
            || !entity.getPersistentData().getBoolean("oni")) {
            return;
        }

        BakedGeoModel overlayBakedModel = this.getGeoModel().getBakedModel(overlayModel.getModelResource(animatable));
        RenderType overlayRenderType = CustomRenderTypes.geoEntityTranslucentEmissive(eyesTexture);
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
