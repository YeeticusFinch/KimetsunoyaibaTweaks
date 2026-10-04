package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.client.models.DirectionArrowModel;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.DirectionArrowEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Renders the arrow model with its local North (-Z) aligned to its direction. */
public class DirectionArrowRenderer extends GeoEntityRenderer<DirectionArrowEntity> {
    public DirectionArrowRenderer(EntityRendererProvider.Context context) {
        super(context, new DirectionArrowModel());
        this.shadowRadius = 0.0F;
    }

    @Override
    public RenderType getRenderType(DirectionArrowEntity animatable, ResourceLocation texture,
                                    MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    public int getPackedOverlay(DirectionArrowEntity animatable, float u, float partialTick) {
        return OverlayTexture.NO_OVERLAY;
    }

    @Override
    protected void applyRotations(DirectionArrowEntity animatable, PoseStack poseStack,
                                  float ageInTicks, float rotationYaw, float partialTick) {
        Vec3 direction = animatable.getArrowDirection();
        if (direction.lengthSqr() < 1.0E-6D) {
            return;
        }
        Vector3f localNorth = new Vector3f(0.0F, 0.0F, -1.0F);
        Vector3f target = new Vector3f((float) direction.x, (float) direction.y, (float) direction.z).normalize();
        poseStack.mulPose(new Quaternionf().rotationTo(localNorth, target));
    }

    @Override
    public void preRender(PoseStack poseStack, DirectionArrowEntity animatable, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay, float red, float green,
                          float blue, float alpha) {
        int color = animatable.getColor();
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
            partialTick, 0xF000F0, packedOverlay,
            ((color >> 16) & 0xFF) / 255.0F,
            ((color >> 8) & 0xFF) / 255.0F,
            (color & 0xFF) / 255.0F,
            1.0F);
    }
}
