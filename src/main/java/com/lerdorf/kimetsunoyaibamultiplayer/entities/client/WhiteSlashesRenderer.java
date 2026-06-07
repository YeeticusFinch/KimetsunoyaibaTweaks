package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.client.CustomRenderTypes;
import com.lerdorf.kimetsunoyaibamultiplayer.client.models.WhiteSlashesModel;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.WhiteSlashesEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class WhiteSlashesRenderer extends GeoEntityRenderer<WhiteSlashesEntity> {
    public WhiteSlashesRenderer(EntityRendererProvider.Context context) {
        super(context, new WhiteSlashesModel());
    }

    @Override
    public RenderType getRenderType(WhiteSlashesEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return CustomRenderTypes.geoEntityTranslucentEmissive(texture);
    }

    @Override
    public int getPackedOverlay(WhiteSlashesEntity animatable, float u, float partialTick) {
        return net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
    }

    @Override
    public void preRender(PoseStack poseStack, WhiteSlashesEntity animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, 0xF000F0, packedOverlay, red, green, blue, alpha);
    }

    @Override
    protected void applyRotations(WhiteSlashesEntity animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick) {
        float yaw = net.minecraft.util.Mth.rotLerp(partialTick, animatable.yRotO, animatable.getYRot());
        float pitch = net.minecraft.util.Mth.rotLerp(partialTick, animatable.xRotO, animatable.getXRot());
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F - yaw));
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(pitch));
    }
}
