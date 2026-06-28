package com.lerdorf.kimetsunoyaibamultiplayer.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class VerticalSlabBlock extends Block {
    public enum VerticalSlabType implements StringRepresentable {
        NORTH("north"),
        EAST("east"),
        SOUTH("south"),
        WEST("west"),
        DOUBLE("double");

        private final String name;

        VerticalSlabType(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final EnumProperty<VerticalSlabType> TYPE = EnumProperty.create("type", VerticalSlabType.class);

    private static final VoxelShape NORTH_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 8.0D);
    private static final VoxelShape EAST_SHAPE = Block.box(8.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape SOUTH_SHAPE = Block.box(0.0D, 0.0D, 8.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape WEST_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 8.0D, 16.0D, 16.0D);
    private static final VoxelShape DOUBLE_SHAPE = Shapes.block();

    public VerticalSlabBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(TYPE, VerticalSlabType.NORTH));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState existing = context.getLevel().getBlockState(context.getClickedPos());
        if (existing.is(this) && existing.getValue(TYPE) != VerticalSlabType.DOUBLE) {
            return existing.setValue(TYPE, VerticalSlabType.DOUBLE);
        }

        Direction face = context.getClickedFace();
        return defaultBlockState().setValue(TYPE, switch (face) {
            case NORTH -> VerticalSlabType.SOUTH;
            case SOUTH -> VerticalSlabType.NORTH;
            case EAST -> VerticalSlabType.WEST;
            case WEST -> VerticalSlabType.EAST;
            default -> VerticalSlabType.NORTH;
        });
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return state.is(this) && state.getValue(TYPE) != VerticalSlabType.DOUBLE
            && context.getItemInHand().is(this.asItem());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TYPE);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state.getValue(TYPE));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state.getValue(TYPE));
    }

    @Override
    public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return shapeFor(state.getValue(TYPE));
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state.getValue(TYPE));
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(TYPE) == VerticalSlabType.DOUBLE ? Shapes.block() : Shapes.empty();
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return switch (state.getValue(TYPE)) {
            case NORTH -> state.setValue(TYPE, rotateType(VerticalSlabType.NORTH, rotation));
            case EAST -> state.setValue(TYPE, rotateType(VerticalSlabType.EAST, rotation));
            case SOUTH -> state.setValue(TYPE, rotateType(VerticalSlabType.SOUTH, rotation));
            case WEST -> state.setValue(TYPE, rotateType(VerticalSlabType.WEST, rotation));
            case DOUBLE -> state;
        };
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return switch (mirror) {
            case LEFT_RIGHT -> switch (state.getValue(TYPE)) {
                case NORTH -> state.setValue(TYPE, VerticalSlabType.NORTH);
                case EAST -> state.setValue(TYPE, VerticalSlabType.WEST);
                case SOUTH -> state.setValue(TYPE, VerticalSlabType.SOUTH);
                case WEST -> state.setValue(TYPE, VerticalSlabType.EAST);
                case DOUBLE -> state;
            };
            case FRONT_BACK -> switch (state.getValue(TYPE)) {
                case NORTH -> state.setValue(TYPE, VerticalSlabType.SOUTH);
                case EAST -> state.setValue(TYPE, VerticalSlabType.EAST);
                case SOUTH -> state.setValue(TYPE, VerticalSlabType.NORTH);
                case WEST -> state.setValue(TYPE, VerticalSlabType.WEST);
                case DOUBLE -> state;
            };
            default -> state;
        };
    }

    private static VerticalSlabType rotateType(VerticalSlabType type, Rotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_90 -> switch (type) {
                case NORTH -> VerticalSlabType.EAST;
                case EAST -> VerticalSlabType.SOUTH;
                case SOUTH -> VerticalSlabType.WEST;
                case WEST -> VerticalSlabType.NORTH;
                case DOUBLE -> VerticalSlabType.DOUBLE;
            };
            case CLOCKWISE_180 -> switch (type) {
                case NORTH -> VerticalSlabType.SOUTH;
                case EAST -> VerticalSlabType.WEST;
                case SOUTH -> VerticalSlabType.NORTH;
                case WEST -> VerticalSlabType.EAST;
                case DOUBLE -> VerticalSlabType.DOUBLE;
            };
            case COUNTERCLOCKWISE_90 -> switch (type) {
                case NORTH -> VerticalSlabType.WEST;
                case EAST -> VerticalSlabType.NORTH;
                case SOUTH -> VerticalSlabType.EAST;
                case WEST -> VerticalSlabType.SOUTH;
                case DOUBLE -> VerticalSlabType.DOUBLE;
            };
            default -> type;
        };
    }

    private static VoxelShape shapeFor(VerticalSlabType type) {
        return switch (type) {
            case NORTH -> NORTH_SHAPE;
            case EAST -> EAST_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            case DOUBLE -> DOUBLE_SHAPE;
        };
    }
}
