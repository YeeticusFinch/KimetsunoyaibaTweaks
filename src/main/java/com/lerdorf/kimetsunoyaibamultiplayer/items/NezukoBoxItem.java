package com.lerdorf.kimetsunoyaibamultiplayer.items;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;

public class NezukoBoxItem extends Item {
    public static final String OPEN_TAG = "Open";

    public NezukoBoxItem(Properties properties) {
        super(properties);
    }

    public static boolean isOpen(ItemStack stack) {
        return stack.hasTag() && stack.getOrCreateTag().getBoolean(OPEN_TAG);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            stack.getOrCreateTag().putBoolean(OPEN_TAG, !isOpen(stack));
            player.getCooldowns().addCooldown(this, 5);
            if (player instanceof ServerPlayer serverPlayer) {
                NezukoBoxHotbarSyncHandler.syncNow(serverPlayer);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
