package com.lerdorf.kimetsunoyaibamultiplayer.alchemy;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.brewing.IBrewingRecipe;

public class AlchemyBrewingRecipe implements IBrewingRecipe {
    private final String ingredientId;
    private final String inputId;
    private final String outputId;
    private final int outputCount;

    public AlchemyBrewingRecipe(String ingredientId, String inputId, String outputId) {
        this(ingredientId, inputId, outputId, 1);
    }

    public AlchemyBrewingRecipe(String ingredientId, String inputId, String outputId, int outputCount) {
        this.ingredientId = ingredientId;
        this.inputId = inputId;
        this.outputId = outputId;
        this.outputCount = Math.max(1, outputCount);
    }

    @Override
    public boolean isInput(ItemStack input) {
        return BloodDemonArtAlchemyCatalog.matches(input, inputId);
    }

    @Override
    public boolean isIngredient(ItemStack ingredient) {
        return BloodDemonArtAlchemyCatalog.matches(ingredient, ingredientId);
    }

    @Override
    public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
        if (!isInput(input) || !isIngredient(ingredient)) {
            return ItemStack.EMPTY;
        }
        return BloodDemonArtAlchemyCatalog.stack(outputId, outputCount);
    }
}
