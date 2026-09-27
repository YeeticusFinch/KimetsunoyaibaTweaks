package com.lerdorf.kimetsunoyaibamultiplayer.blocks;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.ModBlockEntities;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.TrainWheelBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class TrainWheelBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape SMALL_NORTH_SOUTH_SHAPE = Block.box(-8.0D, 0.0D, 0.0D, 24.0D, 32.0D, 16.0D);
    private static final VoxelShape SMALL_EAST_WEST_SHAPE = Block.box(0.0D, 0.0D, -8.0D, 16.0D, 32.0D, 24.0D);
    private static final VoxelShape LARGE_NORTH_SOUTH_SHAPE = Block.box(-16.0D, 0.0D, 0.0D, 32.0D, 48.0D, 16.0D);
    private static final VoxelShape LARGE_EAST_WEST_SHAPE = Block.box(0.0D, 0.0D, -16.0D, 16.0D, 48.0D, 32.0D);
    private static final VoxelShape BAR_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 48.0D, 16.0D);

    private final boolean large;
    private final boolean bar;

    public TrainWheelBlock(Properties properties, boolean large) {
        this(properties, large, false);
    }

    public TrainWheelBlock(Properties properties, boolean large, boolean bar) {
        super(properties);
        this.large = large;
        this.bar = bar;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (bar) {
            return BAR_SHAPE;
        }
        boolean northSouth = state.getValue(FACING).getAxis() == Direction.Axis.Z;
        if (large) {
            return northSouth ? LARGE_NORTH_SOUTH_SHAPE : LARGE_EAST_WEST_SHAPE;
        }
        return northSouth ? SMALL_NORTH_SOUTH_SHAPE : SMALL_EAST_WEST_SHAPE;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TrainWheelBlockEntity(pos, state);
    }

    @Override
    protected void spawnDestroyParticles(Level level, net.minecraft.world.entity.player.Player player,
                                         BlockPos pos, BlockState state) {
        DeepslateParticleBlock.spawnDeepslateDestroyParticles(level, player, pos);
    }
}
