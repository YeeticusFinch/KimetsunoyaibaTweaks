package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.TrainingGourdBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraftforge.client.model.data.ModelData;

public class TrainingGourdRenderer implements BlockEntityRenderer<TrainingGourdBlockEntity> {
    public TrainingGourdRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TrainingGourdBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.0D, blockEntity.getRenderYOffset(), 0.0D);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
            blockEntity.getBlockState(), poseStack, buffer, packedLight, packedOverlay, ModelData.EMPTY, null);
        poseStack.popPose();
    }
}
