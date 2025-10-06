package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.IceBreathingForms;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Yukire Hanazawa - Wields nichirinsword_hanazawa, uses Ice Breathing (all 7 forms)
 * Wears custom armor: Hanazawa's Hair, Hanazawa's Haori (no leggings/boots)
 */
public class HanazawaEntity extends BreathingSlayerEntity {

    public HanazawaEntity(EntityType<? extends BreathingSlayerEntity> entityType, Level level) {
        super(entityType, level);
        this.setCustomName(Component.literal("Yukire Hanazawa"));
    }

    @Override
    public BreathingTechnique getBreathingTechnique() {
        return IceBreathingForms.createIceBreathingWithSeventh(); // All 7 forms including ultimate
    }

    @Override
    public ItemStack getEquippedSword() {
        return new ItemStack(ModItems.NICHIRINSWORD_HANAZAWA.get());
    }

    @Override
    public ItemStack[] getArmorEquipment() {
        return new ItemStack[]{
            new ItemStack(ModItems.HANAZAWA_HAIR.get()),   // Head: Hanazawa's Hair
            new ItemStack(ModItems.HANAZAWA_HAORI.get()),  // Chest: Hanazawa's Haori
            ItemStack.EMPTY,                               // Legs: None
            ItemStack.EMPTY                                // Feet: None
        };
    }
}
