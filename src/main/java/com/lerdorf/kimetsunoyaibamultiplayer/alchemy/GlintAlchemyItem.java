package com.lerdorf.kimetsunoyaibamultiplayer.alchemy;

import net.minecraft.world.item.ItemStack;

public class GlintAlchemyItem extends AlchemyItem {
    public GlintAlchemyItem(Properties properties, int tintColor) {
        super(properties, false, tintColor);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
