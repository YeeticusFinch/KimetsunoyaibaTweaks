package com.lerdorf.kimetsunoyaibamultiplayer.items;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Non-Gecko helmet item for Hanafuda.
 */
public class HanafudaItem extends ArmorItem {
    public HanafudaItem(ArmorMaterial material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public String getArmorTexture(ItemStack stack, net.minecraft.world.entity.Entity entity,
                                  EquipmentSlot slot, @Nullable String type) {
        return "kimetsunoyaibamultiplayer:textures/armor/hanafuda1.png";
    }
}
