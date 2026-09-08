package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.SixEyeDemonHeadBlock;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.SixEyeDemonHeadBlockEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.client.models.SixEyeDemonHeadModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class SixEyeDemonHeadRenderer extends GeoBlockRenderer<SixEyeDemonHeadBlockEntity> {
    public SixEyeDemonHeadRenderer(BlockEntityRendererProvider.Context context) {
        super(new SixEyeDemonHeadModel<>());
        addRenderLayer(new SixEyeDemonHeadEyeLayer<>(this));
    }

    @Override
    protected void rotateBlock(Direction facing, PoseStack poseStack) {
        if (animatable == null) {
            super.rotateBlock(facing, poseStack);
            return;
        }

        BlockState state = animatable.getBlockState();
        Direction horizontalFacing = state.getValue(SixEyeDemonHeadBlock.FACING);
        float yaw = switch (horizontalFacing) {
            case NORTH -> 0.0F;
            case EAST -> 270.0F;
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        };
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        if (state.getValue(SixEyeDemonHeadBlock.FACE) == net.minecraft.world.level.block.state.properties.AttachFace.WALL) {
            poseStack.translate(0.0D, 0.0D, -0.25D);
        }
    }
}
