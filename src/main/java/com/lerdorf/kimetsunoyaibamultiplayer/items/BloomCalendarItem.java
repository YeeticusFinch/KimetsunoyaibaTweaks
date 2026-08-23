package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.SpiderLilyBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class BloomCalendarItem extends Item {
    public BloomCalendarItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.getDescriptionId(stack))
            .withStyle(ChatFormatting.LIGHT_PURPLE);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.kimetsunoyaibamultiplayer.bloom_calendar")
            .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            if (SpiderLilyBlock.isBloomSeason(level)) {
                if (SpiderLilyBlock.isBloomWindow(level)) {
                    player.sendSystemMessage(Component.translatable("message.kimetsunoyaibamultiplayer.bloom_calendar.blooming")
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
                } else {
                    player.sendSystemMessage(Component.translatable("message.kimetsunoyaibamultiplayer.bloom_calendar.season")
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            } else {
                long days = SpiderLilyBlock.daysUntilNextBloomSeason(level);
                String translationKey = days == 1L
                    ? "message.kimetsunoyaibamultiplayer.bloom_calendar.day"
                    : "message.kimetsunoyaibamultiplayer.bloom_calendar.days";
                player.sendSystemMessage(Component.translatable(translationKey, days)
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            }

            player.getCooldowns().addCooldown(this, 20);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
