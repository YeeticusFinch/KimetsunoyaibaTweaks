package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.ModAlchemyBlocks;
import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.VialRackBlock;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.VialRackBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import com.mojang.math.Axis;

public class VialRackRenderer implements BlockEntityRenderer<VialRackBlockEntity> {
    private static final double MODEL_UNIT_OFFSET = 8.0D / 16.0D;
    private static final double[] SLOT_X_OFFSETS = {
        (11.35D - 7.35D) / 16.0D,
        (9.35D - 7.35D) / 16.0D,
        0.0D,
        (5.35D - 7.35D) / 16.0D,
        (3.35D - 7.35D) / 16.0D
    };

    public VialRackRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(VialRackBlockEntity rack, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyRackRotation(rack.getBlockState(), poseStack);
        renderRackModel(poseStack, bufferSource, packedLight, packedOverlay);

        NonNullList<ItemStack> stacks = NonNullList.withSize(SLOT_X_OFFSETS.length, ItemStack.EMPTY);
        for (int slot = 0; slot < stacks.size(); slot++) {
            stacks.set(slot, rack.getRenderItem(slot));
        }
        renderVials(stacks, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }

    public static void renderVials(Iterable<ItemStack> stacks, PoseStack poseStack,
                                   MultiBufferSource bufferSource, int packedLight) {
        int slot = 0;
        for (ItemStack stack : stacks) {
            if (slot >= SLOT_X_OFFSETS.length) {
                break;
            }
            renderVial(slot, stack, poseStack, bufferSource, packedLight);
            slot++;
        }
    }

    private static void renderVial(int slot, ItemStack stack, PoseStack poseStack,
                                   MultiBufferSource bufferSource, int packedLight) {
        if (stack.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(SLOT_X_OFFSETS[slot] + MODEL_UNIT_OFFSET, MODEL_UNIT_OFFSET, MODEL_UNIT_OFFSET);
        Minecraft.getInstance().getItemRenderer().renderStatic(
            stack,
            ItemDisplayContext.NONE,
            packedLight,
            OverlayTexture.NO_OVERLAY,
            poseStack,
            bufferSource,
            Minecraft.getInstance().level,
            stack.hashCode() + slot
        );
        poseStack.popPose();
    }

    private static void applyRackRotation(BlockState state, PoseStack poseStack) {
        if (!state.hasProperty(VialRackBlock.ROTATION)) {
            return;
        }

        poseStack.translate(0.5D, 0.5D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationDegrees(state.getValue(VialRackBlock.ROTATION))));
        poseStack.translate(-0.5D, -0.5D, -0.5D);
    }

    private static float rotationDegrees(int rotation) {
        float degrees = rotation * 45.0F;
        return (rotation & 1) == 1 ? degrees + 90.0F : degrees;
    }

    public static void renderRackModel(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Minecraft minecraft = Minecraft.getInstance();
        BlockState state = ModAlchemyBlocks.VIAL_RACK.get().defaultBlockState();
        minecraft.getBlockRenderer().getModelRenderer().renderModel(
            poseStack.last(),
            bufferSource.getBuffer(net.minecraft.client.renderer.RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS)),
            state,
            minecraft.getBlockRenderer().getBlockModel(state),
            1.0F,
            1.0F,
            1.0F,
            packedLight,
            packedOverlay
        );
    }
}
