package com.lerdorf.kimetsunoyaibamultiplayer.items;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;

/**
 * Hiori's Hakama - Cosmetic leggings slot item for Kakushima Hiori entity
 */
public class HioriHakamaItem extends ArmorItem {
    public HioriHakamaItem() {
        super(CosmeticArmorMaterial.COSMETIC, Type.LEGGINGS,
            new Item.Properties().stacksTo(1).durability(0));
    }
}
