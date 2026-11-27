package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.CrimsonbladeNezukoEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CrimsonbladeNezukoRenderer extends GeoEntityRenderer<CrimsonbladeNezukoEntity> {
    private static final ResourceLocation MODEL_NORMAL = ResourceLocation.fromNamespaceAndPath(
        "crimsonblade", "geo/nezuko.geo.json");
    private static final ResourceLocation MODEL_AWAKENED = ResourceLocation.fromNamespaceAndPath(
        "crimsonblade", "geo/nezuko_awakened.geo.json");
    private static final ResourceLocation TEXTURE_NORMAL = ResourceLocation.fromNamespaceAndPath(
        "crimsonblade", "textures/entity/nezuko.png");
    private static final ResourceLocation TEXTURE_AWAKENED = ResourceLocation.fromNamespaceAndPath(
        "crimsonblade", "textures/entity/nezuko_demon.png");
    private static final ResourceLocation ANIMATIONS_NORMAL = ResourceLocation.fromNamespaceAndPath(
        "crimsonblade", "animations/nezuko.animation.json");
    private static final ResourceLocation ANIMATIONS_AWAKENED = ResourceLocation.fromNamespaceAndPath(
        "crimsonblade", "animations/nezuko_demon.animation.json");
    private static final ResourceLocation BIPED_ANIMATIONS = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID, "animations/biped.animation.json");

    public CrimsonbladeNezukoRenderer(EntityRendererProvider.Context context) {
        super(context, new GeoModel<CrimsonbladeNezukoEntity>() {
            @Override
            public ResourceLocation getModelResource(CrimsonbladeNezukoEntity entity) {
                return entity.isAwakenedStage() ? MODEL_AWAKENED : MODEL_NORMAL;
            }

            @Override
            public ResourceLocation getTextureResource(CrimsonbladeNezukoEntity entity) {
                return entity.isAwakenedStage() ? TEXTURE_AWAKENED : TEXTURE_NORMAL;
            }

            @Override
            public ResourceLocation getAnimationResource(CrimsonbladeNezukoEntity entity) {
                if (entity.isDeadOrDying()) {
                    return BIPED_ANIMATIONS;
                }

                String animation = entity.getCurrentAnimation();
                boolean activeAction = entity.getAnimationTicks() > 0
                    && animation != null
                    && !animation.equals("idle")
                    && !animation.equals("walk")
                    && !animation.equals("run")
                    && !animation.equals("standing");
                if (activeAction && !entity.hasCrimsonbladeAnimation(animation)) {
                    return BIPED_ANIMATIONS;
                }

                return entity.isAwakenedStage() ? ANIMATIONS_AWAKENED : ANIMATIONS_NORMAL;
            }
        });
        this.addRenderLayer(new GeoEquipmentLayer<>(this));
    }

    @Override
    public void preRender(PoseStack poseStack, CrimsonbladeNezukoEntity animatable, BakedGeoModel model,
                          net.minecraft.client.renderer.MultiBufferSource bufferSource,
                          com.mojang.blaze3d.vertex.VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay, float red, float green,
                          float blue, float alpha) {
        this.scaleWidth = animatable.getStageScale();
        this.scaleHeight = animatable.getStageScale();
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick,
            packedLight, packedOverlay, red, green, blue, alpha);
    }
}
