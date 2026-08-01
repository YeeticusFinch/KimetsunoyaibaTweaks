package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.ChestOfDrawersBlock;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.ChestOfDrawersBlockEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedBlocksConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
public final class EnhancedChestOfDrawersHandler {
    private static final ResourceLocation BASE_CHEST_OF_DRAWER_ID =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "chest_of_drawer");

    private EnhancedChestOfDrawersHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Player player = event.player;
        if (player.level().isClientSide() || !EnhancedBlocksConfig.enhancedChestOfDrawers) {
            return;
        }

        Item replacementItem = ModBlocks.CHEST_OF_DRAWERS.get().asItem();

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) {
                continue;
            }

            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (!BASE_CHEST_OF_DRAWER_ID.equals(itemId)) {
                continue;
            }

            ItemStack replacement = new ItemStack(replacementItem, stack.getCount());
            if (stack.hasTag()) {
                replacement.setTag(stack.getTag().copy());
            }
            player.getInventory().setItem(i, replacement);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        Player player = event.getEntity();
        if (player.level().isClientSide() || !EnhancedBlocksConfig.enhancedChestOfDrawers) {
            return;
        }

        BlockState clickedState = event.getLevel().getBlockState(event.getPos());
        Block clickedBlock = clickedState.getBlock();
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(clickedBlock);
        if (!BASE_CHEST_OF_DRAWER_ID.equals(blockId)) {
            return;
        }

        BlockState replacementState = ModBlocks.CHEST_OF_DRAWERS.get().defaultBlockState();
        if (clickedState.hasProperty(ChestOfDrawersBlock.FACING)) {
            replacementState = replacementState.setValue(ChestOfDrawersBlock.FACING, clickedState.getValue(ChestOfDrawersBlock.FACING));
        }
        if (clickedState.hasProperty(ChestOfDrawersBlock.WATERLOGGED)) {
            replacementState = replacementState.setValue(ChestOfDrawersBlock.WATERLOGGED, clickedState.getValue(ChestOfDrawersBlock.WATERLOGGED));
        }
        replacementState = ChestOfDrawersBlock.withSampledGravity(replacementState, event.getLevel(), event.getPos());

        event.getLevel().setBlock(event.getPos(), replacementState, 3);

        if (event.getLevel().getBlockEntity(event.getPos()) instanceof ChestOfDrawersBlockEntity drawers) {
            drawers.handleBlockUse(player);
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
