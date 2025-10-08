package com.lerdorf.kimetsunoyaibamultiplayer.items;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;

/**
 * Komorebi's Hair - Cosmetic helmet slot item for Setsu Komorebi entity
 */
public class KomorebiHairItem extends ArmorItem {
    public KomorebiHairItem() {
        super(CosmeticArmorMaterial.COSMETIC, Type.HELMET,
            new Item.Properties().stacksTo(1).durability(0));
    }
}
