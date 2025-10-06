package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.FrostBreathingForms;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Kakushima Hiori - Wields nichirinsword_hiori, uses Frost Breathing (all 7 forms)
 * Wears custom armor: Hiori's Hair, Hiori's Haori, Hiori's Hakama, uniform boots
 */
public class HioriEntity extends BreathingSlayerEntity {

    public HioriEntity(EntityType<? extends BreathingSlayerEntity> entityType, Level level) {
        super(entityType, level);
        this.setCustomName(Component.literal("Kakushima Hiori"));
    }

    @Override
    public BreathingTechnique getBreathingTechnique() {
        return FrostBreathingForms.createFrostBreathingWithSeventh(); // All 7 forms including ultimate
    }

    @Override
    public ItemStack getEquippedSword() {
        return new ItemStack(ModItems.NICHIRINSWORD_HIORI.get());
    }

    @Override
    public ItemStack[] getArmorEquipment() {
        // Load kimetsunoyaiba mod uniform boots
        net.minecraft.world.item.Item uniformBoots = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
            net.minecraft.resources.ResourceLocation.tryBuild("kimetsunoyaiba", "uniform_boots"));

        return new ItemStack[]{
            new ItemStack(ModItems.HIORI_HAIR.get()),      // Head: Hiori's Hair
            new ItemStack(ModItems.HIORI_HAORI.get()),     // Chest: Hiori's Haori
            new ItemStack(ModItems.HIORI_HAKAMA.get()),    // Legs: Hiori's Hakama
            uniformBoots != null ? new ItemStack(uniformBoots) : ItemStack.EMPTY
        };
    }
}
