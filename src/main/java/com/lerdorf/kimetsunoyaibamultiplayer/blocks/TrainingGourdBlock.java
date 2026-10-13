package com.lerdorf.kimetsunoyaibamultiplayer.blocks;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.TrainingGourdBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;


public class TrainingGourdBlock extends BaseEntityBlock {
    public static final IntegerProperty DIFFICULTY = IntegerProperty.create("difficulty", 0, 2);
    private static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D);
    private final TrainingGourdSize size;

    public TrainingGourdBlock(Properties properties, TrainingGourdSize size) {
        super(properties);
        this.size = size;
        registerDefaultState(stateDefinition.any().setValue(DIFFICULTY, TrainingGourdDifficulty.EASY.ordinal()));
    }

    public TrainingGourdSize getGourdSize() {
        return size;
    }

    public TrainingGourdDifficulty getDifficulty(BlockState state) {
        return TrainingGourdDifficulty.fromId(state.getValue(DIFFICULTY));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        TrainingGourdDifficulty difficulty = TrainingGourdDifficulty.EASY;
        if (context.getItemInHand().getItem() instanceof com.lerdorf.kimetsunoyaibamultiplayer.items.TrainingGourdItem item
            && item.getGourdSize() == size) {
            difficulty = item.getDifficulty();
        }
        return defaultBlockState().setValue(DIFFICULTY, difficulty.ordinal());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer,
                new TrainingGourdMenu.Provider(pos, size,
                    getDifficulty(state)),
                buffer -> {
                    buffer.writeBoolean(false);
                    buffer.writeBlockPos(pos);
                    buffer.writeVarInt(size.ordinal());
                    buffer.writeVarInt(getDifficulty(state).ordinal());
                });
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return usesOffsetRenderer() ? RenderShape.ENTITYBLOCK_ANIMATED : RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return usesOffsetRenderer() ? new TrainingGourdBlockEntity(pos, state) : null;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems
            .getTrainingGourdItem(size, getDifficulty(state)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DIFFICULTY);
    }

    private boolean usesOffsetRenderer() {
        return size == TrainingGourdSize.HUGE || size == TrainingGourdSize.GARGANTUAN;
    }
}
