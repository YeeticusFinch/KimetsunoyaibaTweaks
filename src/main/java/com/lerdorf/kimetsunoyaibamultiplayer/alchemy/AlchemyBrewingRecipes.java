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
            "kimetsunoyaibamultiplayer:calcite_powder",
            "kimetsunoyaibamultiplayer:herbal_extract",
            "kimetsunoyaibamultiplayer:botanical_vial"
        ));
        BrewingRecipeRegistry.addRecipe(new AlchemyBrewingRecipe(
            "kimetsunoyaibamultiplayer:fermented_orchid",
            "kimetsunoyaibamultiplayer:crude_vial",
            "kimetsunoyaibamultiplayer:refined_vial"
        ));
        BrewingRecipeRegistry.addRecipe(new AlchemyBrewingRecipe(
            "kimetsunoyaibamultiplayer:fermented_orchid",
            "kimetsunoyaibamultiplayer:botanical_vial",
            "kimetsunoyaibamultiplayer:distilled_vial"
        ));
        BrewingRecipeRegistry.addRecipe(new AlchemyBrewingRecipe(
            "kimetsunoyaibamultiplayer:immortal_daisy",
            "kimetsunoyaibamultiplayer:crude_vial",
            "kimetsunoyaibamultiplayer:cruel_vial"
        ));
        BrewingRecipeRegistry.addRecipe(new AlchemyBrewingRecipe(
            "kimetsunoyaibamultiplayer:hemomimetic_culture",
            "kimetsunoyaibamultiplayer:cruel_vial",
            "kimetsunoyaibamultiplayer:catalytic_reagent"
        ));
        BrewingRecipeRegistry.addRecipe(new AlchemyBrewingRecipe(
            "kimetsunoyaibamultiplayer:herbal_culture",
            "kimetsunoyaibamultiplayer:crude_vial",
            "kimetsunoyaibamultiplayer:familiar_tonic"
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
        registerInfusion("kimetsunoyaibamultiplayer:wisteria_extract", "kimetsunoyaibamultiplayer:wisteria_infusion");
        registerInfusion("kimetsunoyaibamultiplayer:exorcistic_culture", "kimetsunoyaibamultiplayer:division_inhibition_infusion");
        registerInfusion("kimetsunoyaibamultiplayer:proteolytic_culture", "kimetsunoyaibamultiplayer:cell_destruction_infusion");
        registerInfusion("kimetsunoyaibamultiplayer:neurotoxic_culture", "kimetsunoyaibamultiplayer:immovable_infusion");

        for (BloodDemonArtAlchemyCatalog.CatalystDefinition definition : BloodDemonArtAlchemyCatalog.baseCatalystDefinitions()) {
            BrewingRecipeRegistry.addRecipe(new AlchemyBrewingRecipe(
                definition.inputId(),
                "kimetsunoyaibamultiplayer:catalytic_reagent",
                definition.outputId(),
                3
            ));
        }
    }

    private static void registerInfusion(String ingredientId, String outputId) {
        BrewingRecipeRegistry.addRecipe(new AlchemyBrewingRecipe(
            ingredientId,
            "kimetsunoyaibamultiplayer:crude_vial",
            outputId
        ));
        BrewingRecipeRegistry.addRecipe(new AlchemyBrewingRecipe(
            ingredientId,
            "kimetsunoyaibamultiplayer:botanical_vial",
            outputId
        ));
    }
}
