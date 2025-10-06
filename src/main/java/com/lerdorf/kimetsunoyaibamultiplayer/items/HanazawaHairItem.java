package com.lerdorf.kimetsunoyaibamultiplayer.items;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;

/**
 * Hanazawa's Hair - Cosmetic helmet slot item for Yukire Hanazawa entity
 */
public class HanazawaHairItem extends ArmorItem {
    public HanazawaHairItem() {
        super(CosmeticArmorMaterial.COSMETIC, Type.HELMET,
            new Item.Properties().stacksTo(1).durability(0));
    }
}
