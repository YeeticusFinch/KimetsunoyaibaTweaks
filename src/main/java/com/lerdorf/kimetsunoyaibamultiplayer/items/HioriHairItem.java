package com.lerdorf.kimetsunoyaibamultiplayer.items;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;

/**
 * Hiori's Hair - Cosmetic helmet slot item for Kakushima Hiori entity
 */
public class HioriHairItem extends ArmorItem {
    public HioriHairItem() {
        super(CosmeticArmorMaterial.COSMETIC, Type.HELMET,
            new Item.Properties().stacksTo(1).durability(0));
    }
}
