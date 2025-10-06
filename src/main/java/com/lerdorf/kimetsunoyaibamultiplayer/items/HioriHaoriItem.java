package com.lerdorf.kimetsunoyaibamultiplayer.items;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;

/**
 * Hiori's Haori - Cosmetic chestplate slot item for Kakushima Hiori entity
 */
public class HioriHaoriItem extends ArmorItem {
    public HioriHaoriItem() {
        super(CosmeticArmorMaterial.COSMETIC, Type.CHESTPLATE,
            new Item.Properties().stacksTo(1).durability(0));
    }
}
