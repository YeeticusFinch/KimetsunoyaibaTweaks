package com.lerdorf.kimetsunoyaibamultiplayer.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class SidewaysFenceBlock extends Block implements SimpleWaterloggedBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty LOCAL_NORTH = BooleanProperty.create("local_north");
    public static final BooleanProperty LOCAL_SOUTH = BooleanProperty.create("local_south");
    public static final BooleanProperty LOCAL_EAST = BooleanProperty.create("local_east");
    public static final BooleanProperty LOCAL_WEST = BooleanProperty.create("local_west");
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private static final Direction[] LOCAL_DIRECTIONS = {
        Direction.NORTH,
        Direction.SOUTH,
        Direction.EAST,
        Direction.WEST
    };

    private static final VoxelShape CENTER_VERTICAL = Block.box(6.0D, 0.0D, 6.0D, 10.0D, 16.0D, 10.0D);
    private static final VoxelShape CENTER_NORTH_SOUTH = Block.box(6.0D, 6.0D, 0.0D, 10.0D, 10.0D, 16.0D);
    private static final VoxelShape CENTER_EAST_WEST = Block.box(0.0D, 6.0D, 6.0D, 16.0D, 10.0D, 10.0D);

    private static final VoxelShape ARM_NORTH = Block.box(6.0D, 6.0D, 0.0D, 10.0D, 10.0D, 6.0D);
    private static final VoxelShape ARM_SOUTH = Block.box(6.0D, 6.0D, 10.0D, 10.0D, 10.0D, 16.0D);
    private static final VoxelShape ARM_EAST = Block.box(10.0D, 6.0D, 6.0D, 16.0D, 10.0D, 10.0D);
    private static final VoxelShape ARM_WEST = Block.box(0.0D, 6.0D, 6.0D, 6.0D, 10.0D, 10.0D);
    private static final VoxelShape ARM_UP = Block.box(6.0D, 10.0D, 6.0D, 10.0D, 16.0D, 10.0D);
    private static final VoxelShape ARM_DOWN = Block.box(6.0D, 0.0D, 6.0D, 10.0D, 6.0D, 10.0D);

    public SidewaysFenceBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
            .setValue(FACING, Direction.UP)
            .setValue(LOCAL_NORTH, false)
            .setValue(LOCAL_SOUTH, false)
            .setValue(LOCAL_EAST, false)
            .setValue(LOCAL_WEST, false)
            .setValue(WATERLOGGED, false));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        BlockState state = defaultBlockState()
            .setValue(FACING, context.getNearestLookingDirection().getOpposite())
            .setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
        return updateConnections(state, context.getLevel(), context.getClickedPos());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LOCAL_NORTH, LOCAL_SOUTH, LOCAL_EAST, LOCAL_WEST, WATERLOGGED);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay((LevelReader) level));
        }
        return updateConnections(state, level, pos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state);
    }

    @Override
    public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return shapeFor(state);
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state);
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return remapState(state, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return mirror == Mirror.NONE ? state : state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    private BlockState updateConnections(BlockState state, BlockGetter level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        for (Direction localDirection : LOCAL_DIRECTIONS) {
            Direction worldDirection = worldDirectionForLocal(facing, localDirection);
            BlockPos neighborPos = pos.relative(worldDirection);
            BlockState neighbor = level.getBlockState(neighborPos);
            boolean connects = canConnectTo(state, level, neighborPos, neighbor, worldDirection);
            state = state.setValue(propertyFor(localDirection), connects);
        }
        return state;
    }

    private BlockState remapState(BlockState state, Direction newFacing) {
        Direction oldFacing = state.getValue(FACING);
        BlockState remapped = defaultBlockState()
            .setValue(FACING, newFacing)
            .setValue(WATERLOGGED, state.getValue(WATERLOGGED));

        for (Direction newLocalDirection : LOCAL_DIRECTIONS) {
            Direction newWorldDirection = worldDirectionForLocal(newFacing, newLocalDirection);
            boolean connects = false;

            for (Direction oldLocalDirection : LOCAL_DIRECTIONS) {
                if (!state.getValue(propertyFor(oldLocalDirection))) {
                    continue;
                }

                Direction oldWorldDirection = worldDirectionForLocal(oldFacing, oldLocalDirection);
                if (oldWorldDirection == newWorldDirection) {
                    connects = true;
                    break;
                }
            }

            remapped = remapped.setValue(propertyFor(newLocalDirection), connects);
        }

        return remapped;
    }

    private boolean canConnectTo(BlockState self, BlockGetter level, BlockPos neighborPos, BlockState neighbor, Direction dirToNeighbor) {
        if (neighbor.is(this)) {
            Direction selfFacing = self.getValue(FACING);
            Direction otherFacing = neighbor.getValue(FACING);
            return selfFacing.getAxis() == otherFacing.getAxis();
        }

        return neighbor.isFaceSturdy(level, neighborPos, dirToNeighbor.getOpposite());
    }

    private static BooleanProperty propertyFor(Direction direction) {
        return switch (direction) {
            case NORTH -> LOCAL_NORTH;
            case SOUTH -> LOCAL_SOUTH;
            case EAST -> LOCAL_EAST;
            case WEST -> LOCAL_WEST;
            default -> throw new IllegalArgumentException("Unsupported local direction: " + direction);
        };
    }

    private static Direction worldDirectionForLocal(Direction facing, Direction localDirection) {
        return switch (facing) {
            case UP -> switch (localDirection) {
                case NORTH -> Direction.NORTH;
                case SOUTH -> Direction.SOUTH;
                case EAST -> Direction.EAST;
                case WEST -> Direction.WEST;
                default -> throw new IllegalArgumentException("Unsupported local direction: " + localDirection);
            };
            case DOWN -> switch (localDirection) {
                case NORTH -> Direction.SOUTH;
                case SOUTH -> Direction.NORTH;
                case EAST -> Direction.WEST;
                case WEST -> Direction.EAST;
                default -> throw new IllegalArgumentException("Unsupported local direction: " + localDirection);
            };
            case NORTH -> switch (localDirection) {
                case NORTH -> Direction.UP;
                case SOUTH -> Direction.DOWN;
                case EAST -> Direction.EAST;
                case WEST -> Direction.WEST;
                default -> throw new IllegalArgumentException("Unsupported local direction: " + localDirection);
            };
            case SOUTH -> switch (localDirection) {
                case NORTH -> Direction.DOWN;
                case SOUTH -> Direction.UP;
                case EAST -> Direction.WEST;
                case WEST -> Direction.EAST;
                default -> throw new IllegalArgumentException("Unsupported local direction: " + localDirection);
            };
            case EAST -> switch (localDirection) {
                case NORTH -> Direction.UP;
                case SOUTH -> Direction.DOWN;
                case EAST -> Direction.SOUTH;
                case WEST -> Direction.NORTH;
                default -> throw new IllegalArgumentException("Unsupported local direction: " + localDirection);
            };
            case WEST -> switch (localDirection) {
                case NORTH -> Direction.UP;
                case SOUTH -> Direction.DOWN;
                case EAST -> Direction.NORTH;
                case WEST -> Direction.SOUTH;
                default -> throw new IllegalArgumentException("Unsupported local direction: " + localDirection);
            };
        };
    }

    private static VoxelShape shapeFor(BlockState state) {
        Direction facing = state.getValue(FACING);
        VoxelShape shape = centerPostShapeFor(facing);

        for (Direction localDirection : LOCAL_DIRECTIONS) {
            if (!state.getValue(propertyFor(localDirection))) {
                continue;
            }

            Direction worldDirection = worldDirectionForLocal(facing, localDirection);
            shape = Shapes.or(shape, armShapeFor(worldDirection));
        }

        return shape;
    }

    private static VoxelShape centerPostShapeFor(Direction facing) {
        return switch (facing.getAxis()) {
            case Y -> CENTER_VERTICAL;
            case Z -> CENTER_NORTH_SOUTH;
            case X -> CENTER_EAST_WEST;
        };
    }

    private static VoxelShape armShapeFor(Direction direction) {
        return switch (direction) {
            case NORTH -> ARM_NORTH;
            case SOUTH -> ARM_SOUTH;
            case EAST -> ARM_EAST;
            case WEST -> ARM_WEST;
            case UP -> ARM_UP;
            case DOWN -> ARM_DOWN;
        };
    }
}
