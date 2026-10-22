package com.lerdorf.kimetsunoyaibamultiplayer.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** A lamp attached to a block face, with its local down pointing away from that face. */
public class HangingLampBlock extends Block {
    public static final DirectionProperty DOWN_DIRECTION = DirectionProperty.create("down_direction");

    private static final VoxelShape DOWN_SHAPE = box(5.75D, 0.0D, 5.75D, 10.25D, 16.0D, 10.25D);
    private static final VoxelShape UP_SHAPE = box(5.75D, 0.0D, 5.75D, 10.25D, 16.0D, 10.25D);
    private static final VoxelShape NORTH_SHAPE = box(5.75D, 5.75D, 0.0D, 10.25D, 10.25D, 16.0D);
    private static final VoxelShape SOUTH_SHAPE = box(5.75D, 5.75D, 0.0D, 10.25D, 10.25D, 16.0D);
    private static final VoxelShape EAST_SHAPE = box(0.0D, 5.75D, 5.75D, 16.0D, 10.25D, 10.25D);
    private static final VoxelShape WEST_SHAPE = box(0.0D, 5.75D, 5.75D, 16.0D, 10.25D, 10.25D);

    public HangingLampBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(DOWN_DIRECTION, Direction.DOWN));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DOWN_DIRECTION);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(DOWN_DIRECTION, context.getClickedFace());
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos supportPos = pos.relative(state.getValue(DOWN_DIRECTION).getOpposite());
        return !level.getBlockState(supportPos).isAir();
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state.canSurvive(level, pos) ? super.updateShape(state, direction, neighborState, level, pos, neighborPos)
            : Blocks.AIR.defaultBlockState();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state.getValue(DOWN_DIRECTION));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state.getValue(DOWN_DIRECTION));
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(DOWN_DIRECTION, rotation.rotate(state.getValue(DOWN_DIRECTION)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return mirror == Mirror.NONE ? state : state.setValue(DOWN_DIRECTION,
            mirror.mirror(state.getValue(DOWN_DIRECTION)));
    }

    private static VoxelShape shapeFor(Direction direction) {
        return switch (direction) {
            case DOWN -> DOWN_SHAPE;
            case UP -> UP_SHAPE;
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case EAST -> EAST_SHAPE;
            case WEST -> WEST_SHAPE;
        };
    }
}
