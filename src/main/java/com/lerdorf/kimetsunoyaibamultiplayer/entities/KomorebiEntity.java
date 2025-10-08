package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.FrostBreathingForms;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Setsu Komorebi - Wields nichirinsword_komorebi, uses Frost Breathing (all 7 forms)
 * Wears custom armor: Komorebi's Hair, Komorebi's Haori, Komorebi's Hakama, uniform boots
 */
public class KomorebiEntity extends BreathingSlayerEntity {

    public KomorebiEntity(EntityType<? extends BreathingSlayerEntity> entityType, Level level) {
        super(entityType, level);
        this.setCustomName(Component.literal("Setsu Komorebi"));
    }

    @Override
    public BreathingTechnique getBreathingTechnique() {
        return FrostBreathingForms.createFrostBreathingWithSeventh(); // All 7 forms including ultimate
    }

    @Override
    public ItemStack getEquippedSword() {
        return new ItemStack(ModItems.NICHIRINSWORD_KOMOREBI.get());
    }

    @Override
    public ItemStack[] getArmorEquipment() {
        // Load kimetsunoyaiba mod uniform boots
        net.minecraft.world.item.Item uniformBoots = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
            net.minecraft.resources.ResourceLocation.tryBuild("kimetsunoyaiba", "uniform_boots"));

        return new ItemStack[]{
            new ItemStack(ModItems.KOMOREBI_HAIR.get()),      // Head: Komorebi's Hair
            new ItemStack(ModItems.KOMOREBI_HAORI.get()),     // Chest: Komorebi's Haori
            new ItemStack(ModItems.KOMOREBI_HAKAMA.get()),    // Legs: Komorebi's Hakama
            uniformBoots != null ? new ItemStack(uniformBoots) : ItemStack.EMPTY
        };
    }
}
