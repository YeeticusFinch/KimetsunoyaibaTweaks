package com.lerdorf.kimetsunoyaibamultiplayer.items;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;

/**
 * Hanazawa's Haori - Cosmetic chestplate slot item for Yukire Hanazawa entity
 */
public class HanazawaHaoriItem extends ArmorItem {
    public HanazawaHaoriItem() {
        super(CosmeticArmorMaterial.COSMETIC, Type.CHESTPLATE,
            new Item.Properties().stacksTo(1).durability(0));
    }
}
