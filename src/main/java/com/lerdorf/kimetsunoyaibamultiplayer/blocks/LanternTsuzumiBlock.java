package com.lerdorf.kimetsunoyaibamultiplayer.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Tsuzumi lantern that reverses the supporting lantern when stacked directly.
 */
public class LanternTsuzumiBlock extends SidewaysLanternBlock {
    public static final BooleanProperty PAIRED = BooleanProperty.create("paired");

    public LanternTsuzumiBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(PAIRED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PAIRED);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (!level.isClientSide && placer instanceof Player
                && level.getBlockState(supportPosition(pos, state)).is(this)) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockState placedState = level.getBlockState(pos);
        if (!placedState.is(this)) {
            return;
        }

        Direction facing = placedState.getValue(FACING);
        BlockPos supportPos = supportPosition(pos, placedState);
        BlockState supportState = level.getBlockState(supportPos);
        if (!supportState.is(this)) {
            return;
        }

        BlockState rotatedSupport = supportState.setValue(FACING, facing.getOpposite()).setValue(PAIRED, true);
        if (rotatedSupport != supportState) {
            level.setBlock(supportPos, rotatedSupport, Block.UPDATE_ALL);
        }

        BlockState currentState = level.getBlockState(pos);
        if (currentState.is(this)) {
            level.setBlock(pos, currentState.setValue(PAIRED, true), Block.UPDATE_ALL);
        }
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BlockState updatedState = super.updateShape(state, direction, neighborState, level, pos, neighborPos);
        if (updatedState.isAir()) {
            return updatedState;
        }
        return updatedState.setValue(PAIRED, isPaired(level, pos, updatedState));
    }

    private boolean isPaired(LevelReader level, BlockPos pos, BlockState state) {
        BlockState supportState = level.getBlockState(supportPosition(pos, state));
        return supportState.is(this)
            && supportState.getValue(FACING) == state.getValue(FACING).getOpposite();
    }

    private static BlockPos supportPosition(BlockPos pos, BlockState state) {
        return pos.relative(state.getValue(FACING).getOpposite());
    }
}
