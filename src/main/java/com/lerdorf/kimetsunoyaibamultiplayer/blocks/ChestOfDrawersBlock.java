package com.lerdorf.kimetsunoyaibamultiplayer.blocks;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.ChestOfDrawersBlockEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.ModBlockEntities;
import com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.KNYGravity;
import com.lerdorf.kimetsunoyaibamultiplayer.gravity.field.GravityFieldManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class ChestOfDrawersBlock extends BaseEntityBlock implements SimpleWaterloggedBlock, EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final DirectionProperty GRAVITY_DIRECTION = DirectionProperty.create("gravity_direction");
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private static final double FRONT_INSET = 3.2D;
    private static final VoxelShape SHAPE_DOWN = box(0.0, FRONT_INSET, 0.0, 16.0, 16.0, 16.0);
    private static final VoxelShape SHAPE_UP = box(0.0, 0.0, 0.0, 16.0, 16.0 - FRONT_INSET, 16.0);
    private static final VoxelShape SHAPE_NORTH = box(0.0, 0.0, FRONT_INSET, 16.0, 16.0, 16.0);
    private static final VoxelShape SHAPE_SOUTH = box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0 - FRONT_INSET);
    private static final VoxelShape SHAPE_EAST = box(0.0, 0.0, 0.0, 16.0 - FRONT_INSET, 16.0, 16.0);
    private static final VoxelShape SHAPE_WEST = box(FRONT_INSET, 0.0, 0.0, 16.0, 16.0, 16.0);

    public ChestOfDrawersBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(GRAVITY_DIRECTION, Direction.DOWN)
            .setValue(WATERLOGGED, false));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case DOWN -> SHAPE_DOWN;
            case UP -> SHAPE_UP;
            case NORTH -> SHAPE_NORTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            case SOUTH -> SHAPE_SOUTH;
        };
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
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING, GRAVITY_DIRECTION, WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        Direction gravityDirection = sampleGravityDirection(context.getLevel(), context.getClickedPos());
        Direction facing = chooseFrontFacing(context, gravityDirection);
        return defaultBlockState()
            .setValue(FACING, facing)
            .setValue(GRAVITY_DIRECTION, gravityDirection)
            .setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state
            .setValue(FACING, rotation.rotate(state.getValue(FACING)))
            .setValue(GRAVITY_DIRECTION, rotation.rotate(state.getValue(GRAVITY_DIRECTION)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        if (mirror == Mirror.NONE) {
            return state;
        }
        return state
            .rotate(mirror.getRotation(state.getValue(FACING)))
            .setValue(GRAVITY_DIRECTION, mirror.mirror(state.getValue(GRAVITY_DIRECTION)));
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        Direction gravityDirection = sampleGravityDirection(level, currentPos);
        if (gravityDirection != state.getValue(GRAVITY_DIRECTION)) {
            Direction front = makePerpendicular(state.getValue(FACING), gravityDirection, state.getValue(GRAVITY_DIRECTION));
            state = state.setValue(GRAVITY_DIRECTION, gravityDirection).setValue(FACING, front);
        }
        return super.updateShape(state, facing, facingState, level, currentPos, facingPos);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof ChestOfDrawersBlockEntity drawers) {
            int slot = resolveDrawerFaceSlot(state, pos, hit, drawers);
            if (slot >= 0) {
                ChestOfDrawersBlockEntity.DrawerUseResult drawerResult = drawers.handleDrawerItemUse(player, slot);
                if (drawerResult == ChestOfDrawersBlockEntity.DrawerUseResult.CONSUMED) {
                    return InteractionResult.CONSUME;
                }
                if (drawerResult == ChestOfDrawersBlockEntity.DrawerUseResult.NONE) {
                    return InteractionResult.PASS;
                }
            }

            return drawers.handleBlockUse(player) ? InteractionResult.CONSUME : InteractionResult.PASS;
        }

        return InteractionResult.PASS;
    }

    private int resolveDrawerFaceSlot(BlockState state, BlockPos pos, BlockHitResult hit, ChestOfDrawersBlockEntity drawers) {
        if (!drawers.canInteractWithDrawerFace() || hit.getDirection() != state.getValue(FACING)) {
            return -1;
        }

        return drawers.getFrontInteractionSlot(hit.getLocation());
    }

    public static Direction getDownDirection(BlockState state) {
        return state.hasProperty(GRAVITY_DIRECTION) ? state.getValue(GRAVITY_DIRECTION) : Direction.DOWN;
    }

    public static Direction getUpDirection(BlockState state) {
        return getDownDirection(state).getOpposite();
    }

    public static Direction getFrontDirection(BlockState state) {
        return state.hasProperty(FACING) ? state.getValue(FACING) : Direction.NORTH;
    }

    public static Direction getRightDirection(BlockState state) {
        return cross(getFrontDirection(state), getUpDirection(state));
    }

    public static Vec3 worldVector(Direction direction) {
        return Vec3.atLowerCornerOf(direction.getNormal());
    }

    public static Vec3 localToWorld(BlockState state, double rightOffset, double upOffset, double frontOffset) {
        Vec3 right = worldVector(getRightDirection(state)).scale(rightOffset);
        Vec3 up = worldVector(getUpDirection(state)).scale(upOffset);
        Vec3 front = worldVector(getFrontDirection(state)).scale(frontOffset);
        return right.add(up).add(front);
    }

    public static BlockState withSampledGravity(BlockState state, LevelAccessor level, BlockPos pos) {
        Direction gravityDirection = sampleGravityDirection(level, pos);
        Direction facing = makePerpendicular(getFrontDirection(state), gravityDirection, getDownDirection(state));
        return state.setValue(GRAVITY_DIRECTION, gravityDirection).setValue(FACING, facing);
    }

    public static Direction sampleGravityDirection(LevelAccessor level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel && KNYGravity.isEnabled()) {
            return GravityFieldManager.resolveDirectionAt(serverLevel, pos).orElse(Direction.DOWN);
        }
        return Direction.DOWN;
    }

    private static Direction chooseFrontFacing(BlockPlaceContext context, Direction gravityDirection) {
        LivingEntity placer = context.getPlayer();
        if (placer != null) {
            Vec3 toPlacer = placer.getEyePosition().subtract(Vec3.atCenterOf(context.getClickedPos()));
            Direction best = nearestPerpendicularDirection(toPlacer, gravityDirection);
            if (best != null) {
                return best;
            }
        }

        return makePerpendicular(context.getHorizontalDirection().getOpposite(), gravityDirection, Direction.DOWN);
    }

    private static Direction nearestPerpendicularDirection(Vec3 vector, Direction gravityDirection) {
        Direction best = null;
        double bestDot = Double.NEGATIVE_INFINITY;
        for (Direction direction : Direction.values()) {
            if (direction.getAxis() == gravityDirection.getAxis()) {
                continue;
            }

            Vec3 directionVector = worldVector(direction);
            double dot = vector.dot(directionVector);
            if (dot > bestDot) {
                bestDot = dot;
                best = direction;
            }
        }
        return bestDot > 1.0E-4D ? best : null;
    }

    private static Direction makePerpendicular(Direction front, Direction gravityDirection, Direction oldGravityDirection) {
        if (front.getAxis() != gravityDirection.getAxis()) {
            return front;
        }

        Direction oldUp = oldGravityDirection.getOpposite();
        if (oldUp.getAxis() != gravityDirection.getAxis()) {
            return oldUp;
        }

        return switch (gravityDirection.getAxis()) {
            case X -> Direction.NORTH;
            case Y -> Direction.NORTH;
            case Z -> Direction.EAST;
        };
    }

    private static Direction cross(Direction a, Direction b) {
        int ax = a.getStepX();
        int ay = a.getStepY();
        int az = a.getStepZ();
        int bx = b.getStepX();
        int by = b.getStepY();
        int bz = b.getStepZ();
        return Direction.getNearest(ay * bz - az * by, az * bx - ax * bz, ax * by - ay * bx);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ChestOfDrawersBlockEntity drawers) {
                drawers.dropStoredItems();
                drawers.clearInteractionEntities();
            }
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChestOfDrawersBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, ModBlockEntities.CHEST_OF_DRAWERS.get(),
            level.isClientSide ? ChestOfDrawersBlockEntity::clientTick : ChestOfDrawersBlockEntity::serverTick);
    }
}
