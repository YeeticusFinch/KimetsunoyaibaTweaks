package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.IceBreathingForms;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Ice Slayer - Wields nichirinsword_ice, uses Ice Breathing (forms 1-6)
 * Wears kimetsunoyaiba:uniform armor
 */
public class IceSlayerEntity extends BreathingSlayerEntity {

    public IceSlayerEntity(EntityType<? extends BreathingSlayerEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public BreathingTechnique getBreathingTechnique() {
        return IceBreathingForms.createIceBreathing(); // Forms 1-6 only
    }

    @Override
    public ItemStack getEquippedSword() {
        return new ItemStack(ModItems.NICHIRINSWORD_ICE.get());
    }

    @Override
    public ItemStack[] getArmorEquipment() {
        // Load kimetsunoyaiba mod armor
        Item uniformChest = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryBuild("kimetsunoyaiba", "uniform_chestplate"));
        Item uniformLegs = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryBuild("kimetsunoyaiba", "uniform_leggings"));
        Item uniformBoots = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryBuild("kimetsunoyaiba", "uniform_boots"));

        return new ItemStack[]{
            ItemStack.EMPTY, // No helmet
            uniformChest != null ? new ItemStack(uniformChest) : ItemStack.EMPTY,
            uniformLegs != null ? new ItemStack(uniformLegs) : ItemStack.EMPTY,
            uniformBoots != null ? new ItemStack(uniformBoots) : ItemStack.EMPTY
        };
    }
}
