package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.client.CustomRenderTypes;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.DemonCreeperEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class DemonCreeperRenderer extends GeoEntityRenderer<DemonCreeperEntity> {
    private static final GeoModel<DemonCreeperEntity> BASE_MODEL = new GeoModel<>() {
        @Override
        public ResourceLocation getModelResource(DemonCreeperEntity animatable) {
            return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "geo/demon_creeper.geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(DemonCreeperEntity animatable) {
            if (animatable.isDetonationFlickerWhite()) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/demon_creeper_white.png");
            }
            return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/demon_creeper.png");
        }

        @Override
        public ResourceLocation getAnimationResource(DemonCreeperEntity animatable) {
            return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "animations/demon_creeper.animation.json");
        }
    };

    public DemonCreeperRenderer(EntityRendererProvider.Context context) {
        super(context, BASE_MODEL);
        this.shadowRadius = 0.35F;
        this.addRenderLayer(new EyesGlowLayer<>(this, "geo/demon_creeper.geo.json",
            "textures/entity/demon_creeper_eyes.png", "animations/demon_creeper.animation.json"));
        this.addRenderLayer(new ChargedOverlayLayer(this));
    }

    private static class ChargedOverlayLayer extends GeoRenderLayer<DemonCreeperEntity> {
        private final GeoModel<DemonCreeperEntity> overlayModel = new GeoModel<>() {
            @Override
            public ResourceLocation getModelResource(DemonCreeperEntity animatable) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "geo/demon_creeper_charged.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(DemonCreeperEntity animatable) {
                return ResourceLocation.fromNamespaceAndPath(
                    KimetsunoyaibaMultiplayer.MODID,
                    "textures/entity/creeper_charge" + animatable.getChargedFrame() + ".png"
                );
            }

            @Override
            public ResourceLocation getAnimationResource(DemonCreeperEntity animatable) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "animations/demon_creeper.animation.json");
            }
        };

        private ChargedOverlayLayer(DemonCreeperRenderer renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack poseStack, DemonCreeperEntity animatable, BakedGeoModel bakedModel, RenderType renderType,
                           MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
            if (!animatable.isChargedState()) {
                return;
            }

            BakedGeoModel overlayBakedModel = overlayModel.getBakedModel(overlayModel.getModelResource(animatable));
            syncOverlayBones(bakedModel, overlayBakedModel);
            RenderType overlayRenderType = CustomRenderTypes.geoEntityTranslucentEmissive(overlayModel.getTextureResource(animatable));
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
                0.9F
            );
        }

        private void syncOverlayBones(BakedGeoModel baseModel, BakedGeoModel overlayModel) {
            for (GeoBone overlayBone : overlayModel.topLevelBones()) {
                syncOverlayBone(baseModel, overlayBone);
            }
        }

        private void syncOverlayBone(BakedGeoModel baseModel, GeoBone overlayBone) {
            GeoBone baseBone = baseModel.getBone(overlayBone.getName()).orElse(null);
            if (baseBone != null) {
                overlayBone.setRotX(baseBone.getRotX());
                overlayBone.setRotY(baseBone.getRotY());
                overlayBone.setRotZ(baseBone.getRotZ());
                overlayBone.setPosX(baseBone.getPosX());
                overlayBone.setPosY(baseBone.getPosY());
                overlayBone.setPosZ(baseBone.getPosZ());
                overlayBone.setScaleX(baseBone.getScaleX());
                overlayBone.setScaleY(baseBone.getScaleY());
                overlayBone.setScaleZ(baseBone.getScaleZ());
                overlayBone.setHidden(baseBone.isHidden());
                overlayBone.setModelSpaceMatrix(baseBone.getModelSpaceMatrix());
                overlayBone.setLocalSpaceMatrix(baseBone.getLocalSpaceMatrix());
                overlayBone.setWorldSpaceMatrix(baseBone.getWorldSpaceMatrix());
                overlayBone.setWorldSpaceNormal(baseBone.getWorldSpaceNormal());
            }

            for (GeoBone child : overlayBone.getChildBones()) {
                syncOverlayBone(baseModel, child);
            }
        }
    }
}
