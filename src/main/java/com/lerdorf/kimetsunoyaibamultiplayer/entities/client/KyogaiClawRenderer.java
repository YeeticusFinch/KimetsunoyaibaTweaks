package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.client.models.KyogaiClawModel;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.KyogaiClawEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import net.minecraft.world.phys.Vec3;

public class KyogaiClawRenderer extends GeoEntityRenderer<KyogaiClawEntity> {
    public KyogaiClawRenderer(EntityRendererProvider.Context context) {
        super(context, new KyogaiClawModel());
        this.shadowRadius = 0.0F;
    }

    @Override
    public RenderType getRenderType(KyogaiClawEntity animatable, ResourceLocation texture,
                                    MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    public int getPackedOverlay(KyogaiClawEntity animatable, float u, float partialTick) {
        return OverlayTexture.NO_OVERLAY;
    }

    @Override
    protected void applyRotations(KyogaiClawEntity animatable, PoseStack poseStack,
                                  float ageInTicks, float rotationYaw, float partialTick) {
        Vec3 forward = animatable.getTravelDirection().normalize();
        Vec3 down = Vec3.atLowerCornerOf(animatable.getGravityDirection().getNormal());
        Vec3 up = down.scale(-1.0D);
        forward = forward.subtract(down.scale(forward.dot(down)));
        if (forward.lengthSqr() < 1.0E-6D) {
            return;
        }
        forward = forward.normalize();
        Vec3 right = forward.cross(up).normalize();
        Vec3 back = forward.scale(-1.0D);

        Matrix3f basis = new Matrix3f().set(
            (float) right.x, (float) right.y, (float) right.z,
            (float) up.x, (float) up.y, (float) up.z,
            (float) back.x, (float) back.y, (float) back.z);
        poseStack.mulPose(new Quaternionf().setFromUnnormalized(basis));
    }

    @Override
    public void preRender(PoseStack poseStack, KyogaiClawEntity animatable, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                           float partialTick, int packedLight, int packedOverlay, float red, float green,
                           float blue, float alpha) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
            partialTick, 0xF000F0, packedOverlay, 0.82F, 0.82F, 0.82F, 1.0F);
    }
}
