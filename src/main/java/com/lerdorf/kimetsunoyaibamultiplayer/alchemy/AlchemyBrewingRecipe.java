package com.lerdorf.kimetsunoyaibamultiplayer.alchemy;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.brewing.IBrewingRecipe;

public class AlchemyBrewingRecipe implements IBrewingRecipe {
    private final String ingredientId;
    private final String inputId;
    private final String outputId;

    public AlchemyBrewingRecipe(String ingredientId, String inputId, String outputId) {
        this.ingredientId = ingredientId;
        this.inputId = inputId;
        this.outputId = outputId;
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
        return BloodDemonArtAlchemyCatalog.stack(outputId);
    }
}
