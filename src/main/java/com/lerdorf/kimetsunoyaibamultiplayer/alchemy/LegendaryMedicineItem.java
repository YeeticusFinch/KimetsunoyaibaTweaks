package com.lerdorf.kimetsunoyaibamultiplayer.alchemy;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class LegendaryMedicineItem extends AlchemyItem {
    public LegendaryMedicineItem(Properties properties, int tintColor) {
        super(properties.stacksTo(1), true, tintColor);
    }

    @Override
    public Component getName(ItemStack stack) {
        return super.getName(stack).copy().withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.kimetsunoyaibamultiplayer.legendary_medicine").withStyle(ChatFormatting.GOLD));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
