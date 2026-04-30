package com.lerdorf.kimetsunoyaibamultiplayer.alchemy.jei;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.AlchemyTableRecipe;
import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.MicroscopeRecipe;
import mezz.jei.api.recipe.RecipeType;

public final class AlchemyJeiRecipeTypes {
    public static final RecipeType<AlchemyTableRecipe> ALCHEMY_TABLE = RecipeType.create(
        KimetsunoyaibaMultiplayer.MODID, "alchemy_table", AlchemyTableRecipe.class);

    public static final RecipeType<MicroscopeRecipe> MICROSCOPE = RecipeType.create(
        KimetsunoyaibaMultiplayer.MODID, "microscope", MicroscopeRecipe.class);

    public static final RecipeType<AlchemyBrewingJeiRecipe> ALCHEMY_BREWING = RecipeType.create(
        KimetsunoyaibaMultiplayer.MODID, "alchemy_brewing", AlchemyBrewingJeiRecipe.class);

    private AlchemyJeiRecipeTypes() {
    }
}
