package com.lerdorf.kimetsunoyaibamultiplayer.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class SidewaysStairsBlock extends Block {
    public enum StairSide implements StringRepresentable {
        LEFT("left"),
        RIGHT("right");

        private final String name;

        StairSide(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public enum StairShape implements StringRepresentable {
        STRAIGHT("straight"),
        BOTTOM_INNER("bottom_inner"),
        BOTTOM_OUTER("bottom_outer"),
        TOP_INNER("top_inner"),
        TOP_OUTER("top_outer");

        private final String name;

        StairShape(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<StairSide> SIDE = EnumProperty.create("side", StairSide.class);
    public static final EnumProperty<StairShape> SHAPE = EnumProperty.create("shape", StairShape.class);

    private static final VoxelShape LEFT_STRAIGHT = Shapes.or(
        Block.box(0.0D, 0.0D, 8.0D, 16.0D, 16.0D, 16.0D),
        Block.box(8.0D, 0.0D, 0.0D, 16.0D, 16.0D, 8.0D)
    );
    private static final VoxelShape LEFT_BOTTOM_INNER = Shapes.or(
        Block.box(0.0D, 0.0D, 8.0D, 16.0D, 16.0D, 16.0D),
        Block.box(8.0D, 8.0D, 0.0D, 16.0D, 16.0D, 8.0D),
        Block.box(8.0D, 0.0D, 0.0D, 16.0D, 8.0D, 8.0D),
        Block.box(0.0D, 0.0D, 0.0D, 8.0D, 8.0D, 8.0D)
    );
    private static final VoxelShape LEFT_BOTTOM_OUTER = Shapes.or(
        Block.box(0.0D, 0.0D, 8.0D, 16.0D, 16.0D, 16.0D),
        Block.box(8.0D, 0.0D, 0.0D, 16.0D, 8.0D, 8.0D)
    );
    private static final VoxelShape LEFT_TOP_INNER = Shapes.or(
        Block.box(0.0D, 0.0D, 8.0D, 16.0D, 16.0D, 16.0D),
        Block.box(8.0D, 8.0D, 0.0D, 16.0D, 16.0D, 8.0D),
        Block.box(8.0D, 0.0D, 0.0D, 16.0D, 8.0D, 8.0D),
        Block.box(0.0D, 8.0D, 0.0D, 8.0D, 16.0D, 8.0D)
    );
    private static final VoxelShape LEFT_TOP_OUTER = Shapes.or(
        Block.box(0.0D, 0.0D, 8.0D, 16.0D, 16.0D, 16.0D),
        Block.box(8.0D, 8.0D, 0.0D, 16.0D, 16.0D, 8.0D)
    );

    private static final VoxelShape RIGHT_STRAIGHT = Shapes.or(
        Block.box(0.0D, 0.0D, 8.0D, 16.0D, 16.0D, 16.0D),
        Block.box(0.0D, 0.0D, 0.0D, 8.0D, 16.0D, 8.0D)
    );
    private static final VoxelShape RIGHT_BOTTOM = Shapes.or(
        Block.box(0.0D, 0.0D, 8.0D, 16.0D, 16.0D, 16.0D),
        Block.box(0.0D, 0.0D, 0.0D, 8.0D, 8.0D, 8.0D)
    );
    private static final VoxelShape RIGHT_BOTTOM_INNER = Shapes.or(
        Block.box(0.0D, 0.0D, 8.0D, 16.0D, 16.0D, 16.0D),
        Block.box(0.0D, 8.0D, 0.0D, 8.0D, 16.0D, 8.0D),
        Block.box(8.0D, 0.0D, 0.0D, 16.0D, 8.0D, 8.0D),
        Block.box(0.0D, 0.0D, 0.0D, 8.0D, 8.0D, 8.0D)
    );
    private static final VoxelShape RIGHT_BOTTOM_OUTER = Shapes.or(
        Block.box(0.0D, 0.0D, 8.0D, 16.0D, 16.0D, 16.0D),
        Block.box(0.0D, 0.0D, 0.0D, 8.0D, 8.0D, 8.0D)
    );
    private static final VoxelShape RIGHT_TOP_INNER = Shapes.or(
        Block.box(0.0D, 0.0D, 8.0D, 16.0D, 16.0D, 16.0D),
        Block.box(0.0D, 8.0D, 0.0D, 8.0D, 16.0D, 8.0D),
        Block.box(8.0D, 8.0D, 0.0D, 16.0D, 16.0D, 8.0D),
        Block.box(0.0D, 0.0D, 0.0D, 8.0D, 8.0D, 8.0D)
    );
    private static final VoxelShape RIGHT_TOP_OUTER = Shapes.or(
        Block.box(0.0D, 0.0D, 8.0D, 16.0D, 16.0D, 16.0D),
        Block.box(0.0D, 8.0D, 0.0D, 8.0D, 16.0D, 8.0D)
    );

    public SidewaysStairsBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(SIDE, StairSide.LEFT)
            .setValue(SHAPE, StairShape.STRAIGHT));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        StairSide side = sideFromHit(context);
        return defaultBlockState()
            .setValue(FACING, facing)
            .setValue(SIDE, side)
            .setValue(SHAPE, StairShape.STRAIGHT);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return rotateY(shapeFor(state.getValue(SIDE), state.getValue(SHAPE)), state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return rotateY(shapeFor(state.getValue(SIDE), state.getValue(SHAPE)), state.getValue(FACING));
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return rotateY(shapeFor(state.getValue(SIDE), state.getValue(SHAPE)), state.getValue(FACING));
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
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
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SIDE, SHAPE);
    }

    private static StairSide sideFromHit(BlockPlaceContext context) {
        Vec3 hit = context.getClickLocation();
        BlockPos pos = context.getClickedPos();
        double localX = hit.x - pos.getX();
        double localZ = hit.z - pos.getZ();

        return switch (context.getClickedFace()) {
            case NORTH -> localX < 0.5D ? StairSide.RIGHT : StairSide.LEFT;
            case SOUTH -> localX > 0.5D ? StairSide.RIGHT : StairSide.LEFT;
            case EAST -> localZ < 0.5D ? StairSide.RIGHT : StairSide.LEFT;
            case WEST -> localZ > 0.5D ? StairSide.RIGHT : StairSide.LEFT;
            default -> context.getHorizontalDirection() == Direction.EAST || context.getHorizontalDirection() == Direction.SOUTH
                ? StairSide.RIGHT
                : StairSide.LEFT;
        };
    }

    private static VoxelShape shapeFor(StairSide side, StairShape shape) {
        return switch (side) {
            case LEFT -> switch (shape) {
                case STRAIGHT -> LEFT_STRAIGHT;
                case BOTTOM_INNER -> LEFT_BOTTOM_INNER;
                case BOTTOM_OUTER -> LEFT_BOTTOM_OUTER;
                case TOP_INNER -> LEFT_TOP_INNER;
                case TOP_OUTER -> LEFT_TOP_OUTER;
            };
            case RIGHT -> switch (shape) {
                case STRAIGHT -> RIGHT_STRAIGHT;
                case BOTTOM_INNER -> RIGHT_BOTTOM_INNER;
                case BOTTOM_OUTER -> RIGHT_BOTTOM_OUTER;
                case TOP_INNER -> RIGHT_TOP_INNER;
                case TOP_OUTER -> RIGHT_TOP_OUTER;
            };
        };
    }

    private static VoxelShape rotateY(VoxelShape shape, Direction direction) {
        int turns = switch (direction) {
            case NORTH -> 0;
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };

        VoxelShape result = shape;
        for (int i = 0; i < turns; i++) {
            final VoxelShape current = result;
            VoxelShape[] buffer = new VoxelShape[] { Shapes.empty() };
            current.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
                buffer[0] = Shapes.or(buffer[0], Shapes.box(1.0D - maxZ, minY, minX, 1.0D - minZ, maxY, maxX)));
            result = buffer[0];
        }

        return result;
    }
}
