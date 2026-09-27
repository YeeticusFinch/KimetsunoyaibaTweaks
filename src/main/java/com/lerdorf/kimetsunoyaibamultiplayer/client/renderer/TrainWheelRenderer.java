package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.TrainWheelBlock;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.TrainWheelBlockEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.client.models.TrainWheelModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class TrainWheelRenderer extends GeoBlockRenderer<TrainWheelBlockEntity> {
    public TrainWheelRenderer(BlockEntityRendererProvider.Context context) {
        super(new TrainWheelModel());
    }

    @Override
    protected void rotateBlock(Direction facing, PoseStack poseStack) {
        if (animatable == null) {
            super.rotateBlock(facing, poseStack);
            return;
        }

        Direction wheelFacing = animatable.getBlockState().getValue(TrainWheelBlock.FACING);
        float yaw = switch (wheelFacing) {
            case NORTH -> 0.0F;
            case EAST -> 270.0F;
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        };
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
    }
}
