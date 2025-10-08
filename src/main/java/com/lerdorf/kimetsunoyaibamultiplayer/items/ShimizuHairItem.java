package com.lerdorf.kimetsunoyaibamultiplayer.items;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;

/**
 * Shimizu's Hair - Cosmetic helmet slot item for Akira Shimizu entity
 */
public class ShimizuHairItem extends ArmorItem {
    public ShimizuHairItem() {
        super(CosmeticArmorMaterial.COSMETIC, Type.HELMET,
            new Item.Properties().stacksTo(1).durability(0));
    }
}
