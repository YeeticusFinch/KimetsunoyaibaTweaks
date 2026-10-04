package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.KyogaiEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/** Renders Kyogai's drum armor directly on his GeckoLib model. */
public class KyogaiDrumsGeoLayer extends GeoRenderLayer<KyogaiEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID, "geo/kyogai_drums.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID, "textures/armor/kyogai_drums.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID, "animations/biped.animation.json");

    private final GeoModel<KyogaiEntity> drumModel = new GeoModel<>() {
        @Override
        public ResourceLocation getModelResource(KyogaiEntity entity) {
            return MODEL;
        }

        @Override
        public ResourceLocation getTextureResource(KyogaiEntity entity) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(KyogaiEntity entity) {
            return ANIMATION;
        }
    };

    public KyogaiDrumsGeoLayer(GeoRenderer<KyogaiEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, KyogaiEntity animatable, BakedGeoModel bakedModel,
                       RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                       float partialTick, int packedLight, int packedOverlay) {
        if (animatable.isInvisible()) {
            return;
        }

        BakedGeoModel drumBakedModel = this.drumModel.getBakedModel(MODEL);
        resetBoneVisibility(drumBakedModel);
        RenderType drumRenderType = RenderType.entityCutoutNoCull(TEXTURE);
        getRenderer().reRender(
            drumBakedModel,
            poseStack,
            bufferSource,
            animatable,
            drumRenderType,
            bufferSource.getBuffer(drumRenderType),
            partialTick,
            packedLight,
            packedOverlay,
            1.0F,
            1.0F,
            1.0F,
            1.0F
        );
    }

    private static void resetBoneVisibility(BakedGeoModel model) {
        for (GeoBone bone : model.topLevelBones()) {
            resetBoneVisibility(bone);
        }
    }

    private static void resetBoneVisibility(GeoBone bone) {
        bone.setHidden(false);
        bone.setChildrenHidden(false);
        for (GeoBone child : bone.getChildBones()) {
            resetBoneVisibility(child);
        }
    }
}
