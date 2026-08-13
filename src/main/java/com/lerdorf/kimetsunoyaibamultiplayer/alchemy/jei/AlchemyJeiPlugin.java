package com.lerdorf.kimetsunoyaibamultiplayer.alchemy.jei;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.AlchemyTableRecipe;
import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.BloodDemonArtAlchemyCatalog;
import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.MicroscopeRecipe;
import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.ModAlchemyBlocks;
import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.ModAlchemyItems;
import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.ModAlchemyRecipes;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class AlchemyJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID, "jei_alchemy");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new AlchemyTableJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new MicroscopeJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new AlchemyBrewingJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        List<AlchemyTableRecipe> alchemyRecipes = minecraft.level.getRecipeManager()
            .getAllRecipesFor(ModAlchemyRecipes.ALCHEMY_TABLE_TYPE)
            .stream()
            .toList();
        registration.addRecipes(AlchemyJeiRecipeTypes.ALCHEMY_TABLE, alchemyRecipes);

        List<MicroscopeRecipe> microscopeRecipes = minecraft.level.getRecipeManager()
            .getAllRecipesFor(ModAlchemyRecipes.MICROSCOPE_TYPE)
            .stream()
            .toList();
        registration.addRecipes(AlchemyJeiRecipeTypes.MICROSCOPE, microscopeRecipes);

        registration.addRecipes(AlchemyJeiRecipeTypes.ALCHEMY_BREWING, brewingRecipes());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModAlchemyBlocks.ALCHEMY_TABLE.get()), AlchemyJeiRecipeTypes.ALCHEMY_TABLE);

        ResourceLocation microscopeId = ResourceLocation.tryParse(BloodDemonArtAlchemyCatalog.MICROSCOPE_BLOCK_ID);
        Block microscopeBlock = microscopeId == null ? Blocks.AIR : ForgeRegistries.BLOCKS.getValue(microscopeId);
        if (microscopeBlock != null && microscopeBlock != Blocks.AIR) {
            registration.addRecipeCatalyst(new ItemStack(microscopeBlock), AlchemyJeiRecipeTypes.MICROSCOPE);
        } else {
            registration.addRecipeCatalyst(new ItemStack(ModAlchemyItems.AMETHYST_LENS.get()), AlchemyJeiRecipeTypes.MICROSCOPE);
        }

        registration.addRecipeCatalyst(new ItemStack(Blocks.BREWING_STAND), AlchemyJeiRecipeTypes.ALCHEMY_BREWING);
    }

    private static List<AlchemyBrewingJeiRecipe> brewingRecipes() {
        List<AlchemyBrewingJeiRecipe> recipes = new ArrayList<>();

        recipes.add(new AlchemyBrewingJeiRecipe(
            new ItemStack(ModItems.HEMOLITH_DUST.get()),
            BloodDemonArtAlchemyCatalog.stack("kimetsunoyaibamultiplayer:blood_sample"),
            BloodDemonArtAlchemyCatalog.stack("kimetsunoyaibamultiplayer:crude_vial")
        ));
        recipes.add(new AlchemyBrewingJeiRecipe(
            BloodDemonArtAlchemyCatalog.stack("kimetsunoyaibamultiplayer:calcite_powder"),
            BloodDemonArtAlchemyCatalog.stack("kimetsunoyaibamultiplayer:herbal_extract"),
            BloodDemonArtAlchemyCatalog.stack("kimetsunoyaibamultiplayer:botanical_vial")
        ));
        recipes.add(new AlchemyBrewingJeiRecipe(
            BloodDemonArtAlchemyCatalog.stack("kimetsunoyaibamultiplayer:fermented_orchid"),
            BloodDemonArtAlchemyCatalog.stack("kimetsunoyaibamultiplayer:crude_vial"),
            BloodDemonArtAlchemyCatalog.stack("kimetsunoyaibamultiplayer:refined_vial")
        ));
        recipes.add(new AlchemyBrewingJeiRecipe(
            BloodDemonArtAlchemyCatalog.stack("kimetsunoyaibamultiplayer:fermented_orchid"),
            BloodDemonArtAlchemyCatalog.stack("kimetsunoyaibamultiplayer:botanical_vial"),
            BloodDemonArtAlchemyCatalog.stack("kimetsunoyaibamultiplayer:distilled_vial")
        ));
        recipes.add(new AlchemyBrewingJeiRecipe(
            BloodDemonArtAlchemyCatalog.stack("kimetsunoyaibamultiplayer:immortal_daisy"),
            BloodDemonArtAlchemyCatalog.stack("kimetsunoyaibamultiplayer:crude_vial"),
            BloodDemonArtAlchemyCatalog.stack("kimetsunoyaibamultiplayer:cruel_vial")
        ));
        recipes.add(new AlchemyBrewingJeiRecipe(
            BloodDemonArtAlchemyCatalog.stack("kimetsunoyaibamultiplayer:hemomimetic_culture"),
            BloodDemonArtAlchemyCatalog.stack("kimetsunoyaibamultiplayer:cruel_vial"),
            BloodDemonArtAlchemyCatalog.stack("kimetsunoyaibamultiplayer:catalytic_reagent")
        ));
        recipes.add(new AlchemyBrewingJeiRecipe(
            BloodDemonArtAlchemyCatalog.stack("kimetsunoyaibamultiplayer:herbal_culture"),
            BloodDemonArtAlchemyCatalog.stack("kimetsunoyaibamultiplayer:crude_vial"),
            BloodDemonArtAlchemyCatalog.stack("kimetsunoyaibamultiplayer:familiar_tonic")
        ));
        recipes.add(new AlchemyBrewingJeiRecipe(
            BloodDemonArtAlchemyCatalog.stack("minecraft:slime_ball"),
            BloodDemonArtAlchemyCatalog.stack("kimetsunoyaibamultiplayer:refined_vial"),
            BloodDemonArtAlchemyCatalog.stack("kimetsunoyaibamultiplayer:potion_effect_binder")
        ));

        addInfusion(recipes, "kimetsunoyaibamultiplayer:azure_extract", "kimetsunoyaibamultiplayer:blindness_infusion");
        addInfusion(recipes, "kimetsunoyaibamultiplayer:wither_extract", "kimetsunoyaibamultiplayer:wither_infusion");
        addInfusion(recipes, "kimetsunoyaibamultiplayer:sculk_extract", "kimetsunoyaibamultiplayer:darkness_infusion");
        addInfusion(recipes, "kimetsunoyaibamultiplayer:noxious_extract", "kimetsunoyaibamultiplayer:nausea_infusion");
        addInfusion(recipes, "kimetsunoyaibamultiplayer:guardian_extract", "kimetsunoyaibamultiplayer:mining_fatigue_infusion");
        addInfusion(recipes, "kimetsunoyaibamultiplayer:golden_extract", "kimetsunoyaibamultiplayer:haste_infusion");
        addInfusion(recipes, "kimetsunoyaibamultiplayer:rotten_blood_sample", "kimetsunoyaibamultiplayer:hunger_infusion");
        addInfusion(recipes, "kimetsunoyaibamultiplayer:scute_extract", "kimetsunoyaibamultiplayer:resistance_infusion");
        addInfusion(recipes, "kimetsunoyaibamultiplayer:illagers_extract", "kimetsunoyaibamultiplayer:bleeding_infusion");
        addInfusion(recipes, "kimetsunoyaibamultiplayer:blaze_extract", "kimetsunoyaibamultiplayer:fire_infusion");
        addInfusion(recipes, "kimetsunoyaibamultiplayer:powdered_snow_extract", "kimetsunoyaibamultiplayer:frozen_infusion");
        addInfusion(recipes, "kimetsunoyaibamultiplayer:wisteria_extract", "kimetsunoyaibamultiplayer:wisteria_infusion");
        addInfusion(recipes, "kimetsunoyaibamultiplayer:exorcistic_culture", "kimetsunoyaibamultiplayer:division_inhibition_infusion");
        addInfusion(recipes, "kimetsunoyaibamultiplayer:proteolytic_culture", "kimetsunoyaibamultiplayer:cell_destruction_infusion");
        addInfusion(recipes, "kimetsunoyaibamultiplayer:neurotoxic_culture", "kimetsunoyaibamultiplayer:immovable_infusion");

        for (BloodDemonArtAlchemyCatalog.CatalystDefinition definition : BloodDemonArtAlchemyCatalog.baseCatalystDefinitions()) {
            recipes.add(new AlchemyBrewingJeiRecipe(
                BloodDemonArtAlchemyCatalog.stack(definition.inputId()),
                BloodDemonArtAlchemyCatalog.stack("kimetsunoyaibamultiplayer:catalytic_reagent"),
                BloodDemonArtAlchemyCatalog.stack(definition.outputId(), 3)
            ));
        }

        return recipes;
    }

    private static void addInfusion(List<AlchemyBrewingJeiRecipe> recipes, String ingredientId, String outputId) {
        recipes.add(new AlchemyBrewingJeiRecipe(
            BloodDemonArtAlchemyCatalog.stack(ingredientId),
            BloodDemonArtAlchemyCatalog.stack("kimetsunoyaibamultiplayer:crude_vial"),
            BloodDemonArtAlchemyCatalog.stack(outputId)
        ));
        recipes.add(new AlchemyBrewingJeiRecipe(
            BloodDemonArtAlchemyCatalog.stack(ingredientId),
            BloodDemonArtAlchemyCatalog.stack("kimetsunoyaibamultiplayer:botanical_vial"),
            BloodDemonArtAlchemyCatalog.stack(outputId)
        ));
    }
}
