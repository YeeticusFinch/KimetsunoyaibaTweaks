package com.lerdorf.kimetsunoyaibamultiplayer.items;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;

/**
 * Komorebi's Haori - Cosmetic chestplate slot item for Setsu Komorebi entity
 */
public class KomorebiHaoriItem extends ArmorItem {
    public KomorebiHaoriItem() {
        super(CosmeticArmorMaterial.COSMETIC, Type.CHESTPLATE,
            new Item.Properties().stacksTo(1).durability(0));
    }
}
