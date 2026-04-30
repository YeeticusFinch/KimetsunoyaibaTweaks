package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.events.DemonTransformationHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import javax.annotation.Nullable;
import java.util.List;

public class BloodOfMuzanItem extends Item {
    public BloodOfMuzanItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }

        if (DemonTransformationHandler.isCustomDemonInitiationEnabled()) {
            boolean consumed = DemonTransformationHandler.consumeCustomBlood(serverPlayer, stack);
            return consumed ? InteractionResultHolder.consume(stack) : InteractionResultHolder.fail(stack);
        }

        DemonTransformationHandler.consumeBaseMuzanBlood(serverPlayer, 1);
        if (!serverPlayer.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Begins demon transformation").withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("Longer for ranked demon slayers").withStyle(ChatFormatting.GRAY));
    }
}
