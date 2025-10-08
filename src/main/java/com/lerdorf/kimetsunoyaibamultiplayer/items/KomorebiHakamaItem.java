package com.lerdorf.kimetsunoyaibamultiplayer.items;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;

/**
 * Komorebi's Hakama - Cosmetic leggings slot item for Setsu Komorebi entity
 */
public class KomorebiHakamaItem extends ArmorItem {
    public KomorebiHakamaItem() {
        super(CosmeticArmorMaterial.COSMETIC, Type.LEGGINGS,
            new Item.Properties().stacksTo(1).durability(0));
    }
}
