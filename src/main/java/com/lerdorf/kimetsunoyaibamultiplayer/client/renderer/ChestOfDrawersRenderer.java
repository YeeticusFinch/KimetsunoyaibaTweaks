package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.ChestOfDrawersBlockEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.client.models.ChestOfDrawersModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class ChestOfDrawersRenderer extends GeoBlockRenderer<ChestOfDrawersBlockEntity> {
    public ChestOfDrawersRenderer(BlockEntityRendererProvider.Context context) {
        super(new ChestOfDrawersModel());
        addRenderLayer(new DrawerItemsLayer(this));
    }

    private static class DrawerItemsLayer extends GeoRenderLayer<ChestOfDrawersBlockEntity> {
        private DrawerItemsLayer(ChestOfDrawersRenderer renderer) {
            super(renderer);
        }

        @Override
        public void renderForBone(PoseStack poseStack, ChestOfDrawersBlockEntity animatable, GeoBone bone,
                                  net.minecraft.client.renderer.RenderType renderType, MultiBufferSource bufferSource,
                                  com.mojang.blaze3d.vertex.VertexConsumer buffer, float partialTick, int packedLight,
                                  int packedOverlay) {
            switch (bone.getName()) {
                case "top" -> renderDrawerItem(poseStack, renderType, bufferSource, packedLight, animatable, ChestOfDrawersBlockEntity.SLOT_TOP, 0.0f, 0.80f, 0.10f, 0.52f);
                case "topmiddle" -> renderDrawerItem(poseStack, renderType, bufferSource, packedLight, animatable, ChestOfDrawersBlockEntity.SLOT_TOP_MIDDLE, 0.0f, 0.6f, 0.10f, 0.62f);
                case "middlebottom" -> renderDrawerItem(poseStack, renderType, bufferSource, packedLight, animatable, ChestOfDrawersBlockEntity.SLOT_MIDDLE_BOTTOM, 0.0f, 0.38f, 0.10f, 0.62f);
                case "bottom" -> {
                    renderDrawerItem(poseStack, renderType, bufferSource, packedLight, animatable, ChestOfDrawersBlockEntity.SLOT_BOTTOM_LEFT, -0.25f, 0.094f, 0.10f, 0.31f);
                    renderDrawerItem(poseStack, renderType, bufferSource, packedLight, animatable, ChestOfDrawersBlockEntity.SLOT_BOTTOM_RIGHT, 0.25f, 0.094f, 0.10f, 0.31f);
                }
                default -> {
                }
            }
        }

        private void renderDrawerItem(PoseStack poseStack, net.minecraft.client.renderer.RenderType renderType,
                                      MultiBufferSource bufferSource, int packedLight,
                                      ChestOfDrawersBlockEntity animatable, int slot,
                                      float sideOffset, float verticalOffset, float depthOffset, float scale) {
            if (!animatable.shouldRenderSlotItem(slot)) {
                return;
            }

            ItemStack stack = animatable.getItemForSlot(slot);
            if (stack.isEmpty()) {
                return;
            }

            poseStack.pushPose();
            poseStack.translate(sideOffset, verticalOffset, depthOffset);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
            poseStack.scale(scale, scale, scale);
            Minecraft.getInstance().getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.FIXED,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                bufferSource,
                Minecraft.getInstance().level,
                stack.hashCode()
            );
            bufferSource.getBuffer(renderType);
            poseStack.popPose();
        }
    }
}
