package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.client.ShimmerCompat;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.BloodLanceEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Renders the item-model Blood Lance aligned to its current velocity. */
public final class BloodLanceRenderer extends EntityRenderer<BloodLanceEntity> {
    private static final ItemStack LANCE_STACK = new ItemStack(ModItems.BLOOD_LANCE.get());
    private final ItemRenderer itemRenderer;

    public BloodLanceRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        shadowRadius = 0.0F;
    }

    @Override
    public void render(BloodLanceEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        renderModel(entity, poseStack, bufferSource, 0xF000F0);
        ShimmerCompat.updateTornadoLight(entity, partialTick, 0xD41424, 8.0F);
        ShimmerCompat.postEntityBloom(poseStack, (bloomPoseStack, bloomBufferSource) ->
            renderModel(entity, bloomPoseStack, bloomBufferSource, 0xF000F0));
    }

    private void renderModel(BloodLanceEntity entity, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight) {
        Vec3 velocity = entity.getDeltaMovement();
        if (velocity.lengthSqr() > 1.0E-8D) {
            Vector3f target = new Vector3f((float) velocity.x, (float) velocity.y,
                (float) velocity.z).normalize();
            poseStack.pushPose();
            poseStack.mulPose(new Quaternionf().rotationTo(new Vector3f(0.0F, 0.0F, 1.0F), target));
            itemRenderer.renderStatic(LANCE_STACK, ItemDisplayContext.NONE, packedLight,
                OverlayTexture.NO_OVERLAY, poseStack, bufferSource, entity.level(), entity.getId());
            poseStack.popPose();
            return;
        }

        itemRenderer.renderStatic(LANCE_STACK, ItemDisplayContext.NONE, packedLight,
            OverlayTexture.NO_OVERLAY, poseStack, bufferSource, entity.level(), entity.getId());
    }

    @Override
    public ResourceLocation getTextureLocation(BloodLanceEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(
            KimetsunoyaibaMultiplayer.MODID, "textures/entity/blood_lance.png");
    }
}
