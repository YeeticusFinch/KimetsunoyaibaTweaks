package com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.TrainingGourdBlock;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.TrainingGourdSize;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class TrainingGourdBlockEntity extends BlockEntity {
    public TrainingGourdBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRAINING_GOURD.get(), pos, state);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(1.0D, 2.0D, 1.0D);
    }

    public double getRenderYOffset() {
        return getBlockState().getBlock() instanceof TrainingGourdBlock gourd
            && gourd.getGourdSize() == TrainingGourdSize.GARGANTUAN ? 0.94D : 0.25D;
    }
}
