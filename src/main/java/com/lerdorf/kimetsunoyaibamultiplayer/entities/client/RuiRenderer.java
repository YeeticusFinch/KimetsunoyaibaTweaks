package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.config.RuiDisplayConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.RuiEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Renderer for Rui's custom biped model. */
public class RuiRenderer extends GeoEntityRenderer<RuiEntity> {
    private static final float RENDER_SCALE = 0.87F;
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID, "geo/biped_rui.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID, "textures/entity/oni_rui.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID, "animations/biped.animation.json");

    public RuiRenderer(EntityRendererProvider.Context context) {
        super(context, new GeoModel<RuiEntity>() {
            @Override
            public ResourceLocation getModelResource(RuiEntity entity) {
                return MODEL;
            }

            @Override
            public ResourceLocation getTextureResource(RuiEntity entity) {
                return TEXTURE;
            }

            @Override
            public ResourceLocation getAnimationResource(RuiEntity entity) {
                return ANIMATION;
            }
        });
        this.scaleWidth = RENDER_SCALE;
        this.scaleHeight = RENDER_SCALE;
        this.addRenderLayer(new GeoEquipmentLayer<>(this));
        this.addRenderLayer(new RuiFaceOverlaysLayer(this));
    }

    @Override
    public void actuallyRender(PoseStack poseStack, RuiEntity animatable, BakedGeoModel model,
                               RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                               boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        if (!isReRender && !animatable.isHeadless()) {
            // Restore every bone because a hidden child is not covered by checking
            // only the root body's visibility flags.
            model.getBone("body").ifPresent(RuiRenderer::restoreVisibility);
        }

        if (!isReRender) {
            model.getBone("hair").ifPresent(hair ->
                    hair.setHidden(!RuiDisplayConfig.FANCY_RUI_HAIR.get()));
        }

        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    private static void restoreVisibility(GeoBone bone) {
        bone.setHidden(false);
        bone.setChildrenHidden(false);
        for (GeoBone child : bone.getChildBones()) {
            restoreVisibility(child);
        }
    }
}
