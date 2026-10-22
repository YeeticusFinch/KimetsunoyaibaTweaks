package com.lerdorf.kimetsunoyaibamultiplayer.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/** Dark bamboo with gravity-aligned down and a player-facing north side. */
public class DarkBambooBlock extends Block {
    public static final DirectionProperty DOWN_DIRECTION = DirectionProperty.create("down_direction");
    public static final DirectionProperty NORTH_DIRECTION = DirectionProperty.create("north_direction");
    public static final BooleanProperty FULL = BooleanProperty.create("full");

    public DarkBambooBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
            .setValue(DOWN_DIRECTION, Direction.DOWN)
            .setValue(NORTH_DIRECTION, Direction.NORTH)
            .setValue(FULL, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DOWN_DIRECTION, NORTH_DIRECTION, FULL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction down = ChestOfDrawersBlock.sampleGravityDirection(context.getLevel(), context.getClickedPos());
        Direction north = playerFacing(context, down);
        return defaultBlockState()
            .setValue(DOWN_DIRECTION, down)
            .setValue(NORTH_DIRECTION, north)
            .setValue(FULL, needsFullModel(context.getLevel(), context.getClickedPos(), down));
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Direction down = ChestOfDrawersBlock.sampleGravityDirection(level, pos);
        Direction north = state.getValue(NORTH_DIRECTION);
        if (north.getAxis() == down.getAxis()) {
            north = fallbackNorth(down);
        }

        boolean full = needsFullModel(level, pos, down);
        state = state.setValue(DOWN_DIRECTION, down)
            .setValue(NORTH_DIRECTION, north)
            .setValue(FULL, full);
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state
            .setValue(DOWN_DIRECTION, rotation.rotate(state.getValue(DOWN_DIRECTION)))
            .setValue(NORTH_DIRECTION, rotation.rotate(state.getValue(NORTH_DIRECTION)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        if (mirror == Mirror.NONE) {
            return state;
        }
        return state
            .setValue(DOWN_DIRECTION, mirror.mirror(state.getValue(DOWN_DIRECTION)))
            .setValue(NORTH_DIRECTION, mirror.mirror(state.getValue(NORTH_DIRECTION)));
    }

    private static boolean needsFullModel(LevelAccessor level, BlockPos pos, Direction down) {
        BlockPos below = pos.relative(down);
        return !level.getBlockState(below).isCollisionShapeFullBlock(level, below);
    }

    private static Direction playerFacing(BlockPlaceContext context, Direction down) {
        Direction north = context.getHorizontalDirection().getOpposite();
        return north.getAxis() == down.getAxis() ? fallbackNorth(down) : north;
    }

    private static Direction fallbackNorth(Direction down) {
        return switch (down.getAxis()) {
            case X -> Direction.NORTH;
            case Y -> Direction.NORTH;
            case Z -> Direction.EAST;
        };
    }
}
