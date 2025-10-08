package com.lerdorf.kimetsunoyaibamultiplayer.items;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;

/**
 * Shimizu's Haori - Cosmetic chestplate slot item for Akira Shimizu entity
 */
public class ShimizuHaoriItem extends ArmorItem {
    public ShimizuHaoriItem() {
        super(CosmeticArmorMaterial.COSMETIC, Type.CHESTPLATE,
            new Item.Properties().stacksTo(1).durability(0));
    }
}
