package com.lerdorf.kimetsunoyaibamultiplayer.blocks;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.GravityFieldProjectorBlockEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class GravityFieldProjectorBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = DirectionProperty.create("facing");

    public GravityFieldProjectorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer.isGravityIntegrationEnabled()
            && level.getBlockEntity(pos) instanceof GravityFieldProjectorBlockEntity projector) {
            projector.setGravityDirection(state.getValue(FACING));
            projector.updateField();
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer.isGravityIntegrationEnabled()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof GravityFieldProjectorBlockEntity projector) {
            if (player.isShiftKeyDown()) {
                projector.cycleGravityDirection();
            } else {
                projector.setEnabled(!projector.isEnabled());
            }
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, neighborPos, movedByPiston);
        if (!level.isClientSide && com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer.isGravityIntegrationEnabled()
            && level.getBlockEntity(pos) instanceof GravityFieldProjectorBlockEntity projector) {
            projector.updateField();
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof GravityFieldProjectorBlockEntity projector) {
            projector.unregisterField();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.GRAVITY_FIELD_PROJECTOR.get().create(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
