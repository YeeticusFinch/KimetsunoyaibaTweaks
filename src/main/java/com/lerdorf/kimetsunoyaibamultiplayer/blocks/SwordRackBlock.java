package com.lerdorf.kimetsunoyaibamultiplayer.blocks;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.SwordRackBlockEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class SwordRackBlock extends BaseEntityBlock {
    public static final BooleanProperty WALL = BooleanProperty.create("wall");
    public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.Plane.HORIZONTAL);
    public static final IntegerProperty ROTATION = IntegerProperty.create("rotation", 0, 15);

    private static final VoxelShape FLOOR_SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 15.0D, 15.0D);
    private static final VoxelShape NORTH_WALL_SHAPE = Block.box(1.0D, 1.0D, 0.0D, 15.0D, 15.0D, 2.0D);
    private static final VoxelShape SOUTH_WALL_SHAPE = Block.box(1.0D, 1.0D, 14.0D, 15.0D, 15.0D, 16.0D);
    private static final VoxelShape WEST_WALL_SHAPE = Block.box(0.0D, 1.0D, 1.0D, 2.0D, 15.0D, 15.0D);
    private static final VoxelShape EAST_WALL_SHAPE = Block.box(14.0D, 1.0D, 1.0D, 16.0D, 15.0D, 15.0D);

    public SwordRackBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
            .setValue(WALL, false)
            .setValue(FACING, Direction.NORTH)
            .setValue(ROTATION, 0));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        Direction clickedFace = context.getClickedFace();

        if (clickedFace.getAxis().isHorizontal()) {
            BlockState wallState = defaultBlockState()
                .setValue(WALL, true)
                .setValue(FACING, clickedFace);
            return wallState.canSurvive(level, pos) ? wallState : null;
        }

        BlockState floorState = defaultBlockState()
            .setValue(WALL, false)
            .setValue(FACING, Direction.NORTH)
            .setValue(ROTATION, rotationForYaw(context.getRotation()));
        return floorState.canSurvive(level, pos) ? floorState : null;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(WALL)) {
            Direction facing = state.getValue(FACING);
            BlockPos supportPos = pos.relative(facing.getOpposite());
            return level.getBlockState(supportPos).isFaceSturdy(level, supportPos, facing);
        }

        BlockPos supportPos = pos.below();
        return level.getBlockState(supportPos).isFaceSturdy(level, supportPos, Direction.UP);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof SwordRackBlockEntity rack) {
            rack.syncClient();
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof SwordRackBlockEntity rack) {
                rack.dropContents();
            }
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (player instanceof ServerPlayer serverPlayer && blockEntity instanceof SwordRackBlockEntity rack) {
            NetworkHooks.openScreen(serverPlayer, rack, buffer -> buffer.writeBlockPos(pos));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(WALL)) {
            return switch (state.getValue(FACING)) {
                case NORTH -> SOUTH_WALL_SHAPE;
                case SOUTH -> NORTH_WALL_SHAPE;
                case WEST -> EAST_WALL_SHAPE;
                case EAST -> WEST_WALL_SHAPE;
                default -> SOUTH_WALL_SHAPE;
            };
        }

        return FLOOR_SHAPE;
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
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos neighborPos, boolean movedByPiston) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
        super.neighborChanged(state, level, pos, block, neighborPos, movedByPiston);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        if (state.getValue(WALL)) {
            return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
        }

        int steps = switch (rotation) {
            case CLOCKWISE_90 -> 4;
            case CLOCKWISE_180 -> 8;
            case COUNTERCLOCKWISE_90 -> 12;
            default -> 0;
        };
        return state.setValue(ROTATION, (state.getValue(ROTATION) + steps) & 15);
    }

    @Override
    public BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        if (state.getValue(WALL)) {
            return state.rotate(mirror.getRotation(state.getValue(FACING)));
        }
        return mirror == net.minecraft.world.level.block.Mirror.NONE
            ? state
            : state.setValue(ROTATION, (state.getValue(ROTATION) + 8) & 15);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WALL, FACING, ROTATION);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SwordRackBlockEntity(pos, state);
    }

    private static int rotationForYaw(float yaw) {
        return Mth.floor((yaw + 180.0F + 11.25F) / 22.5F) & 15;
    }
}
