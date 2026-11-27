package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.client.CustomRenderTypes;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ArachnidMandiblesAndEyesItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/** Renderer for the combined helmet and its separate fullbright glow pass. */
public class ArachnidMandiblesAndEyesRenderer extends GeoArmorRenderer<ArachnidMandiblesAndEyesItem> {
    public ArachnidMandiblesAndEyesRenderer() {
        super(new Model());
        addRenderLayer(new GlowLayer(this));
    }

    @Override
    public RenderType getRenderType(ArachnidMandiblesAndEyesItem item, ResourceLocation texture,
                                    MultiBufferSource bufferSource, float partialTick) {
        return CustomRenderTypes.geoEntityTranslucent(texture);
    }

    private static class Model extends GeoModel<ArachnidMandiblesAndEyesItem> {
        @Override
        public ResourceLocation getModelResource(ArachnidMandiblesAndEyesItem item) {
            return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
                "geo/arachnid_mandibles_and_eyes.geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(ArachnidMandiblesAndEyesItem item) {
            return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
                "textures/armor/arachnid_mandibles_and_eyes.png");
        }

        @Override
        public ResourceLocation getAnimationResource(ArachnidMandiblesAndEyesItem item) {
            return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
                "animations/arachnid_mandibles.animation.json");
        }
    }

    private static final class GlowLayer extends GeoRenderLayer<ArachnidMandiblesAndEyesItem> {
        private static final ResourceLocation GLOW_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            KimetsunoyaibaMultiplayer.MODID, "textures/armor/arachnid_mandibles_and_eyes_glow.png");
        private final Model glowModel = new Model() {
            @Override
            public ResourceLocation getTextureResource(ArachnidMandiblesAndEyesItem item) {
                return GLOW_TEXTURE;
            }
        };

        private GlowLayer(GeoRenderer<ArachnidMandiblesAndEyesItem> renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack poseStack, ArachnidMandiblesAndEyesItem item, BakedGeoModel bakedModel,
                           RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                           float partialTick, int packedLight, int packedOverlay) {
            BakedGeoModel glowBakedModel = glowModel.getBakedModel(glowModel.getModelResource(item));
            RenderType glowRenderType = CustomRenderTypes.geoEntityTranslucentEmissive(GLOW_TEXTURE);
            getRenderer().reRender(glowBakedModel, poseStack, bufferSource, item, glowRenderType,
                bufferSource.getBuffer(glowRenderType), partialTick, 0xF000F0,
                OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
