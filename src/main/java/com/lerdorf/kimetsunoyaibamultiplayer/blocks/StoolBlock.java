package com.lerdorf.kimetsunoyaibamultiplayer.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A small, dyeable seat with a wool cushion. */
public class StoolBlock extends Block {
    private static final VoxelShape SHAPE = Shapes.or(
        box(0.25D, 0.0D, 0.25D, 2.0D, 5.5D, 2.0D),
        box(14.0D, 0.0D, 0.25D, 15.75D, 5.5D, 2.0D),
        box(0.25D, 0.0D, 14.0D, 2.0D, 5.5D, 15.75D),
        box(14.0D, 0.0D, 14.0D, 15.75D, 5.5D, 15.75D),
        box(2.4D, 8.0D, 2.4D, 13.6D, 13.0D, 13.6D)
    );

    public StoolBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            BenchSeatHandler.seatEntity(level, pos, player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
