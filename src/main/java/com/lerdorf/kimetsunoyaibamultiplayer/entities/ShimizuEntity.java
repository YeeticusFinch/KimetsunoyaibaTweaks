package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.IceBreathingForms;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Akira Shimizu - Wields nichirinsword_shimizu, uses Ice Breathing (all 7 forms)
 * Wears custom armor: Shimizu's Hair, Shimizu's Haori (no leggings/boots)
 */
public class ShimizuEntity extends BreathingSlayerEntity {

    public ShimizuEntity(EntityType<? extends BreathingSlayerEntity> entityType, Level level) {
        super(entityType, level);
        this.setCustomName(Component.literal("Akira Shimizu"));
    }

    @Override
    public BreathingTechnique getBreathingTechnique() {
        return IceBreathingForms.createIceBreathingWithSeventh(); // All 7 forms including ultimate
    }

    @Override
    public ItemStack getEquippedSword() {
        return new ItemStack(ModItems.NICHIRINSWORD_SHIMIZU.get());
    }

    @Override
    public ItemStack[] getArmorEquipment() {
        return new ItemStack[]{
            new ItemStack(ModItems.SHIMIZU_HAIR.get()),   // Head: Shimizu's Hair
            new ItemStack(ModItems.SHIMIZU_HAORI.get()),  // Chest: Shimizu's Haori
            ItemStack.EMPTY,                               // Legs: None
            ItemStack.EMPTY                                // Feet: None
        };
    }
}
