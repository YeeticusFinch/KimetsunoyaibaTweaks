package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.RuiFatherEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Renderer for Spider Demon Father's oversized GeckoLib model. */
public class RuiFatherRenderer extends GeoEntityRenderer<RuiFatherEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID, "geo/rui_father.geo.json");
    private static final ResourceLocation NORMAL_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID, "textures/entity/rui_father.png");
    private static final ResourceLocation MOLTED_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID, "textures/entity/rui_father_molted.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID, "animations/biped.animation.json");
    private static final float BASE_RENDER_SCALE = 1.30F;

    private static final GeoModel<RuiFatherEntity> MODEL_INSTANCE = new GeoModel<>() {
        @Override
        public ResourceLocation getModelResource(RuiFatherEntity entity) {
            return MODEL;
        }

        @Override
        public ResourceLocation getTextureResource(RuiFatherEntity entity) {
            return entity.isMolted() ? MOLTED_TEXTURE : NORMAL_TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(RuiFatherEntity entity) {
            return ANIMATION;
        }
    };

    public RuiFatherRenderer(EntityRendererProvider.Context context) {
        super(context, MODEL_INSTANCE);
    }

    @Override
    public void preRender(PoseStack poseStack, RuiFatherEntity animatable, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay, float red, float green,
                          float blue, float alpha) {
        float scale = BASE_RENDER_SCALE * (animatable.isMolted() ? 1.15F : 1.0F);
        this.scaleWidth = scale;
        this.scaleHeight = scale;
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick,
            packedLight, packedOverlay, red, green, blue, alpha);
    }
}
