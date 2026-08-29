package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.client.CustomRenderTypes;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.DaughterEntity;
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

/**
 * Daughter entity renderer.
 *
 * Uses the shared biped_female_2 geckolib model and the shared biped
 * animation file (same animations as all other biped entities).
 *
 * Skins:
 * - Spider demon form: daughter.png + daughter eyes overlay
 * - Human disguise (ryoko): ryoko.png + ryoko eyes overlay
 */
public class DaughterRenderer extends GeoEntityRenderer<DaughterEntity> {
    private static final class DaughterModel extends GeoModel<DaughterEntity> {
        private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "geo/biped_female_2.geo.json");
        private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "animations/biped.animation.json");

        @Override
        public ResourceLocation getModelResource(DaughterEntity animatable) {
            return MODEL;
        }

        @Override
        public ResourceLocation getTextureResource(DaughterEntity animatable) {
            return animatable.isInHumanForm()
                ? ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/ryoko.png")
                : ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/daughter.png");
        }

        @Override
        public ResourceLocation getAnimationResource(DaughterEntity animatable) {
            return ANIMATION;
        }
    }

    public DaughterRenderer(EntityRendererProvider.Context context) {
        super(context, new DaughterModel());
        // The Daughter renders at 85% of her model's native size.
        this.withScale(0.85F);
        this.addRenderLayer(new GeoEquipmentLayer<>(this));
        this.addRenderLayer(new DaughterEyesLayer(this));
    }

    private static class DaughterEyesLayer extends GeoRenderLayer<DaughterEntity> {
        private DaughterEyesLayer(DaughterRenderer renderer) {
            super(renderer);
        }

        private static ResourceLocation getEyesTexture(DaughterEntity animatable) {
            return animatable.isInHumanForm()
                ? ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/ryoko_eyes.png")
                : ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/daughter_eyes.png");
        }

        @Override
        public void render(PoseStack poseStack, DaughterEntity animatable, BakedGeoModel bakedModel, RenderType renderType,
                           MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight,
                           int packedOverlay) {
            // Demon-form kanji eyes glow; human-disguise eyes render subtly.
            boolean humanForm = animatable.isInHumanForm();

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

            if (!humanForm) {
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
}
