package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.SpineEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class SpineRenderer extends EntityRenderer<SpineEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID,
        "textures/entity/solid_color.png"
    );

    private final SpineModel<SpineEntity> model;

    public SpineRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new SpineModel<>(context.bakeLayer(SpineModel.LAYER_LOCATION));
    }

    @Override
    public void render(SpineEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = Mth.rotLerp(partialTick, entity.xRotO, entity.getXRot());

        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

        // Keep model scale consistent with standard Java entity model coordinates.
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0D, -1.5D, 0.0D);

        int color = entity.getTintColor();
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        this.model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, red, green, blue, 1.0F);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(SpineEntity entity) {
        return TEXTURE;
    }
}
