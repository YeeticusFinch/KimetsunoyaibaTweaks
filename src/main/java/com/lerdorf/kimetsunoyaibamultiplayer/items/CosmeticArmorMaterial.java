package com.lerdorf.kimetsunoyaibamultiplayer.items;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Armor material for cosmetic items (no protection, purely visual)
 */
public enum CosmeticArmorMaterial implements ArmorMaterial {
    COSMETIC("cosmetic", 0, new int[]{0, 0, 0, 0}, 0, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F, () -> Ingredient.EMPTY),
    SLAYER_UNIFORM("slayer_uniform", 25, new int[]{0, 6, 0, 0}, 0, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F, () -> Ingredient.EMPTY),
    SLAYER_UNIFORM_2("slayer_uniform_2", 25, new int[]{0, 8, 6, 4}, 0, SoundEvents.ARMOR_EQUIP_LEATHER, 1.0F, 0.0F, () -> Ingredient.EMPTY),
    MUICHIRO_HAORI("muichiro_haori", 25, new int[]{0, 6, 0, 0}, 0, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F, () -> Ingredient.EMPTY),
    MUICHIRO_HAORI_UNIFORM("muichiro_haori_uniform", 25, new int[]{0, 8, 0, 0}, 0, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F, () -> Ingredient.EMPTY),
    MUICHIRO_FP_HAIR("muichiro_fp_hair", 25, new int[]{0, 0, 0, 0}, 0, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F, () -> Ingredient.EMPTY);

    private final String name;
    private final int durabilityMultiplier;
    private final int[] protectionAmounts;
    private final int enchantmentValue;
    private final SoundEvent equipSound;
    private final float toughness;
    private final float knockbackResistance;
    private final java.util.function.Supplier<Ingredient> repairIngredient;

    CosmeticArmorMaterial(String name, int durabilityMultiplier, int[] protectionAmounts, int enchantmentValue,
                          SoundEvent equipSound, float toughness, float knockbackResistance,
                          java.util.function.Supplier<Ingredient> repairIngredient) {
        this.name = name;
        this.durabilityMultiplier = durabilityMultiplier;
        this.protectionAmounts = protectionAmounts;
        this.enchantmentValue = enchantmentValue;
        this.equipSound = equipSound;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
        this.repairIngredient = repairIngredient;
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return 0; // Unbreakable cosmetic item
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return this.protectionAmounts[type.getSlot().getIndex()];
    }

    @Override
    public int getEnchantmentValue() {
        return this.enchantmentValue;
    }

    @Override
    public SoundEvent getEquipSound() {
        return this.equipSound;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return this.repairIngredient.get();
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public float getToughness() {
        return this.toughness;
    }

    @Override
    public float getKnockbackResistance() {
        return this.knockbackResistance;
    }
}
