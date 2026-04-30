package com.lerdorf.kimetsunoyaibamultiplayer.alchemy;

import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;

public final class AlchemyBrewingRecipes {
    private AlchemyBrewingRecipes() {
    }

    public static void register() {
        BrewingRecipeRegistry.addRecipe(new AlchemyBrewingRecipe(
            BloodDemonArtAlchemyCatalog.id(ModItems.HEMOLITH_DUST.get()),
            "kimetsunoyaibamultiplayer:blood_sample",
            "kimetsunoyaibamultiplayer:crude_vial"
        ));
        BrewingRecipeRegistry.addRecipe(new AlchemyBrewingRecipe(
            "kimetsunoyaibamultiplayer:fermented_orchid",
            "kimetsunoyaibamultiplayer:crude_vial",
            "kimetsunoyaibamultiplayer:refined_vial"
        ));
        BrewingRecipeRegistry.addRecipe(new AlchemyBrewingRecipe(
            "kimetsunoyaibamultiplayer:immortal_daisy",
            "kimetsunoyaibamultiplayer:crude_vial",
            "kimetsunoyaibamultiplayer:cruel_vial"
        ));
        BrewingRecipeRegistry.addRecipe(new AlchemyBrewingRecipe(
            "minecraft:slime_ball",
            "kimetsunoyaibamultiplayer:refined_vial",
            "kimetsunoyaibamultiplayer:potion_effect_binder"
        ));

        registerInfusion("kimetsunoyaibamultiplayer:azure_extract", "kimetsunoyaibamultiplayer:blindness_infusion");
        registerInfusion("kimetsunoyaibamultiplayer:wither_extract", "kimetsunoyaibamultiplayer:wither_infusion");
        registerInfusion("kimetsunoyaibamultiplayer:sculk_extract", "kimetsunoyaibamultiplayer:darkness_infusion");
        registerInfusion("kimetsunoyaibamultiplayer:noxious_extract", "kimetsunoyaibamultiplayer:nausea_infusion");
        registerInfusion("kimetsunoyaibamultiplayer:guardian_extract", "kimetsunoyaibamultiplayer:mining_fatigue_infusion");
        registerInfusion("kimetsunoyaibamultiplayer:golden_extract", "kimetsunoyaibamultiplayer:haste_infusion");
        registerInfusion("kimetsunoyaibamultiplayer:rotten_blood_sample", "kimetsunoyaibamultiplayer:hunger_infusion");
        registerInfusion("kimetsunoyaibamultiplayer:scute_extract", "kimetsunoyaibamultiplayer:resistance_infusion");
        registerInfusion("kimetsunoyaibamultiplayer:illagers_extract", "kimetsunoyaibamultiplayer:bleeding_infusion");
        registerInfusion("kimetsunoyaibamultiplayer:blaze_extract", "kimetsunoyaibamultiplayer:fire_infusion");
        registerInfusion("kimetsunoyaibamultiplayer:powdered_snow_extract", "kimetsunoyaibamultiplayer:frozen_infusion");
    }

    private static void registerInfusion(String ingredientId, String outputId) {
        BrewingRecipeRegistry.addRecipe(new AlchemyBrewingRecipe(
            ingredientId,
            "kimetsunoyaibamultiplayer:crude_vial",
            outputId
        ));
    }
}
