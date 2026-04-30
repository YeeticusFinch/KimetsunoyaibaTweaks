package com.lerdorf.kimetsunoyaibamultiplayer.alchemy;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

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

    public int tintColor() {
        return tintColor;
    }
}
