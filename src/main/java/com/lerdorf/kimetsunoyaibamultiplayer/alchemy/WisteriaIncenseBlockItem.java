package com.lerdorf.kimetsunoyaibamultiplayer.alchemy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class WisteriaIncenseBlockItem extends BlockItem {
    public WisteriaIncenseBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        BlockState clickedState = level.getBlockState(clickedPos);

        if (clickedState.is(Blocks.FLOWER_POT)) {
            return placeInEmptyPot(context, clickedPos);
        }
        if (clickedState.getBlock() instanceof WisteriaIncenseBlock && clickedState.getValue(WisteriaIncenseBlock.COUNT) < 4) {
            return addToIncense(context, clickedPos, clickedState);
        }

        return super.useOn(context);
    }

    private InteractionResult placeInEmptyPot(UseOnContext context, BlockPos pos) {
        Level level = context.getLevel();
        if (!level.isClientSide) {
            level.setBlock(pos, ModAlchemyBlocks.POTTED_WISTERIA_INCENSE.get().defaultBlockState(), 3);
            shrinkStack(context.getPlayer(), context.getItemInHand());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private InteractionResult addToIncense(UseOnContext context, BlockPos pos, BlockState state) {
        Level level = context.getLevel();
        if (!level.isClientSide) {
            WisteriaIncenseBlock.addIncense(level, pos, state, context.getPlayer(), context.getItemInHand());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void shrinkStack(Player player, ItemStack stack) {
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    @Override
    protected boolean canPlace(BlockPlaceContext context, BlockState state) {
        return super.canPlace(context, state.setValue(WisteriaIncenseBlock.COUNT, 1));
    }
}
