package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.client.CustomRenderTypes;
import com.lerdorf.kimetsunoyaibamultiplayer.client.models.SixEyeDemonHeadModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class SixEyeDemonHeadEyeLayer<T extends GeoAnimatable> extends GeoRenderLayer<T> {
    private static final ResourceLocation EYES_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID, "textures/entity/six_eye_demon_eyes.png");
    private static final ResourceLocation EYES_KANJI_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID, "textures/entity/six_eye_demon_eyes_kanji.png");
    private final SixEyeDemonHeadModel<T> overlayModel = new SixEyeDemonHeadModel<>() {
        @Override
        public ResourceLocation getTextureResource(T animatable) {
            return EYES_TEXTURE;
        }
    };

    public SixEyeDemonHeadEyeLayer(GeoRenderer<T> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, RenderType renderType,
                       MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick,
                       int packedLight, int packedOverlay) {
        BakedGeoModel overlayBakedModel = overlayModel.getBakedModel(overlayModel.getModelResource(animatable));
        ResourceLocation texture = EYES_TEXTURE;
        if (getRenderer() instanceof software.bernie.geckolib.renderer.GeoItemRenderer<?> itemRenderer) {
            ItemStack stack = itemRenderer.getCurrentItemStack();
            if (stack != null && stack.hasTag() && stack.getTag().getBoolean("SixEyeDemonHeadKanji")) {
                texture = EYES_KANJI_TEXTURE;
            }
        }
        RenderType overlayRenderType = CustomRenderTypes.geoEntityTranslucentEmissive(texture);
        getRenderer().reRender(overlayBakedModel, poseStack, bufferSource, animatable, overlayRenderType,
            bufferSource.getBuffer(overlayRenderType), partialTick, 0xF000F0, OverlayTexture.NO_OVERLAY,
            1.0F, 1.0F, 1.0F, 1.0F);
    }
}
