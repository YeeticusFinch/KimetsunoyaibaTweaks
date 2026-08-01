package com.lerdorf.kimetsunoyaibamultiplayer.blocks;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.GravityBlockEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class GravityBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = DirectionProperty.create("facing");
    private static final VoxelShape FULL_SHAPE = Shapes.block();

    public GravityBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof GravityBlockEntity gravityBlock) {
            gravityBlock.updateField();
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!GravityBlockEntity.isHoldingGravityBlock(player)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!player.isCreative()) {
            return InteractionResult.CONSUME;
        }
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof GravityBlockEntity gravityBlock) {
            NetworkHooks.openScreen(serverPlayer, gravityBlock, buffer -> gravityBlock.writeMenuData(buffer));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, neighborPos, movedByPiston);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof GravityBlockEntity gravityBlock) {
            gravityBlock.updateField();
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof GravityBlockEntity gravityBlock) {
            gravityBlock.clearFieldEffects();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!(context instanceof EntityCollisionContext)) {
            return FULL_SHAPE;
        }
        EntityCollisionContext entityContext = (EntityCollisionContext) context;
        if (entityContext.getEntity() == null) {
            return FULL_SHAPE;
        }
        if (entityContext.getEntity() instanceof Player player && GravityBlockEntity.isHoldingGravityBlock(player)) {
            return FULL_SHAPE;
        }
        return Shapes.empty();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.GRAVITY_BLOCK.get().create(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(blockEntityType, ModBlockEntities.GRAVITY_BLOCK.get(),
            (serverLevel, pos, blockState, gravityBlock) -> {
                if (serverLevel instanceof ServerLevel typedServerLevel) {
                    gravityBlock.tickServer(typedServerLevel);
                }
            });
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
