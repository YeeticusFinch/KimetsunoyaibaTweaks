package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.LotusBlockEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.client.models.LilypadModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class LotusLilypadLayer extends GeoRenderLayer<LotusBlockEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID, "textures/block/lilypad.png");
    private final LilypadModel<LotusBlockEntity> model = new LilypadModel<>();

    public LotusLilypadLayer(GeoRenderer<LotusBlockEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, LotusBlockEntity animatable, BakedGeoModel bakedModel,
                       RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                       float partialTick, int packedLight, int packedOverlay) {
        if (!animatable.hasLilypad()) {
            return;
        }

        BakedGeoModel lilypadModel = model.getBakedModel(model.getModelResource(animatable));
        RenderType lilypadRenderType = RenderType.entityCutoutNoCull(TEXTURE);
        getRenderer().reRender(lilypadModel, poseStack, bufferSource, animatable, lilypadRenderType,
            bufferSource.getBuffer(lilypadRenderType), partialTick, packedLight, packedOverlay,
            1.0F, 1.0F, 1.0F, 1.0F);
    }
}
