package com.lerdorf.kimetsunoyaibamultiplayer.blocks;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.LargeDoorBlockEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** A three-block-tall, two-block-wide animated door. */
public class LargeDoorBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final IntegerProperty ANIMATION = IntegerProperty.create("animation", 0, 3);
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 5);

    private static final ThreadLocal<Boolean> REMOVING_STRUCTURE = ThreadLocal.withInitial(() -> false);

    public LargeDoorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(OPEN, false)
            .setValue(ANIMATION, 0)
            .setValue(PART, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN, ANIMATION, PART);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos anchor = context.getClickedPos();
        if (!canPlaceStructure(context, anchor, facing)) {
            return null;
        }

        return defaultBlockState()
            .setValue(FACING, facing)
            .setValue(OPEN, false)
            .setValue(ANIMATION, 0)
            .setValue(PART, 0);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        Direction facing = state.getValue(FACING);
        Direction widthDirection = widthDirection(facing);
        for (int row = 0; row < 3; row++) {
            for (int side = 0; side < 2; side++) {
                if (row == 0 && side == 0) {
                    continue;
                }
                BlockPos partPos = pos.above(row).relative(widthDirection, side);
                level.setBlock(partPos, state
                    .setValue(PART, row * 2 + side), 3);
            }
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        BlockPos anchor = anchorPosition(pos, state);
        BlockEntity blockEntity = level.getBlockEntity(anchor);
        if (!(blockEntity instanceof LargeDoorBlockEntity door)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            door.toggle();
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return canSurvive(state, level, pos) ? state : Blocks.AIR.defaultBlockState();
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos anchor = anchorPosition(pos, state);
        return Block.canSupportCenter(level, anchor.below(), Direction.UP);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return closedShape(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(OPEN) ? Shapes.empty() : closedShape(state);
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == 0 ? ModBlockEntities.LARGE_DOOR.get().create(pos, state) : null;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
        Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return state.getValue(PART) == 0
            ? createTickerHelper(blockEntityType, ModBlockEntities.LARGE_DOOR.get(),
                level.isClientSide ? LargeDoorBlockEntity::clientTick : LargeDoorBlockEntity::serverTick)
            : null;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return mirror == Mirror.NONE ? state : state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !REMOVING_STRUCTURE.get()) {
            REMOVING_STRUCTURE.set(true);
            try {
                removeOtherParts(level, pos, state);
            } finally {
                REMOVING_STRUCTURE.set(false);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    public static BlockPos anchorPosition(BlockPos pos, BlockState state) {
        int part = state.getValue(PART);
        Direction widthDirection = widthDirection(state.getValue(FACING));
        return pos.relative(widthDirection, -(part % 2)).below(part / 2);
    }

    public static void setPhase(Level level, BlockPos anchor, BlockState anchorState, int animation) {
        Direction widthDirection = widthDirection(anchorState.getValue(FACING));
        boolean open = animation == 2;
        for (int row = 0; row < 3; row++) {
            for (int side = 0; side < 2; side++) {
                BlockPos partPos = anchor.above(row).relative(widthDirection, side);
                BlockState partState = level.getBlockState(partPos);
                if (partState.is(anchorState.getBlock()) && partState.getValue(PART) == row * 2 + side) {
                    level.setBlock(partPos, partState
                        .setValue(OPEN, open)
                        .setValue(ANIMATION, animation), 3);
                }
            }
        }
    }

    private static boolean canPlaceStructure(BlockPlaceContext context, BlockPos anchor, Direction facing) {
        if (!Block.canSupportCenter(context.getLevel(), anchor.below(), Direction.UP)) {
            return false;
        }

        Direction widthDirection = widthDirection(facing);
        for (int row = 0; row < 3; row++) {
            for (int side = 0; side < 2; side++) {
                BlockPos partPos = anchor.above(row).relative(widthDirection, side);
                if (!context.getLevel().getBlockState(partPos).canBeReplaced(context)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void removeOtherParts(Level level, BlockPos removedPos, BlockState removedState) {
        BlockPos anchor = anchorPosition(removedPos, removedState);
        Direction widthDirection = widthDirection(removedState.getValue(FACING));
        for (int row = 0; row < 3; row++) {
            for (int side = 0; side < 2; side++) {
                BlockPos partPos = anchor.above(row).relative(widthDirection, side);
                if (!partPos.equals(removedPos) && level.getBlockState(partPos).is(removedState.getBlock())) {
                    level.setBlock(partPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                }
            }
        }
    }

    private static VoxelShape closedShape(BlockState state) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z
            ? Block.box(0.0D, 0.0D, 7.0D, 16.0D, 16.0D, 9.0D)
            : Block.box(7.0D, 0.0D, 0.0D, 9.0D, 16.0D, 16.0D);
    }

    private static Direction widthDirection(Direction facing) {
        return facing.getCounterClockWise();
    }
}
