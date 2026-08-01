package com.lerdorf.kimetsunoyaibamultiplayer.alchemy;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.PetriDishBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class AlchemyItem extends Item {
    private final boolean specialPresentation;
    private final int tintColor;

    public AlchemyItem(Properties properties, boolean specialPresentation, int tintColor) {
        super(properties);
        this.specialPresentation = specialPresentation;
        this.tintColor = tintColor;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return specialPresentation || super.isFoil(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        Component name = super.getName(stack);
        if (!specialPresentation) {
            return name;
        }
        return name.copy().withStyle(ChatFormatting.BOLD);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        if (!BloodDemonArtAlchemyCatalog.isPetriDishDisplayItem(stack)) {
            return super.useOn(context);
        }

        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        if (level.getBlockState(clickedPos).is(ModAlchemyBlocks.PETRI_DISH.get())) {
            return addToPetriDishBlock(context, clickedPos);
        }

        if (context.getClickedFace() != Direction.UP) {
            return super.useOn(context);
        }

        BlockPlaceContext placeContext = new BlockPlaceContext(context);
        BlockPos placePos = placeContext.getClickedPos();
        BlockState placeState = ModAlchemyBlocks.PETRI_DISH.get().defaultBlockState();
        if (!level.getBlockState(placePos).canBeReplaced(placeContext) || !placeState.canSurvive(level, placePos)) {
            return super.useOn(context);
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!level.setBlock(placePos, placeState, 3)) {
            return InteractionResult.FAIL;
        }

        BlockEntity blockEntity = level.getBlockEntity(placePos);
        if (blockEntity instanceof PetriDishBlockEntity petriDish) {
            petriDish.loadFromItem(stack);
        }
        shrinkPlacedStack(context.getPlayer(), stack);
        return InteractionResult.CONSUME;
    }

    private InteractionResult addToPetriDishBlock(UseOnContext context, BlockPos pos) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = context.getItemInHand();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof PetriDishBlockEntity petriDish && petriDish.addDish(stack)) {
            shrinkPlacedStack(context.getPlayer(), stack);
        }
        return InteractionResult.CONSUME;
    }

    private static void shrinkPlacedStack(Player player, ItemStack stack) {
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    public int tintColor() {
        return tintColor;
    }
}
