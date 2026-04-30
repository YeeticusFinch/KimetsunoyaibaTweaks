package com.lerdorf.kimetsunoyaibamultiplayer.alchemy;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.crafting.CraftingHelper;
import org.jetbrains.annotations.Nullable;

public class MicroscopeRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final Ingredient lensIngredient;
    private final Ingredient sampleIngredient;
    private final ItemStack result;

    public MicroscopeRecipe(ResourceLocation id, Ingredient lensIngredient, Ingredient sampleIngredient, ItemStack result) {
        this.id = id;
        this.lensIngredient = lensIngredient;
        this.sampleIngredient = sampleIngredient;
        this.result = result;
    }

    @Override
    public boolean matches(Container container, Level level) {
        ItemStack lens = container.getItem(0);
        ItemStack sample = container.getItem(1);
        return !lens.isEmpty()
            && !sample.isEmpty()
            && lensIngredient.test(lens)
            && sampleIngredient.test(sample);
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess registryAccess) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return result.copy();
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModAlchemyRecipes.MICROSCOPE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModAlchemyRecipes.MICROSCOPE_TYPE;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(lensIngredient);
        ingredients.add(sampleIngredient);
        return ingredients;
    }

    public Ingredient lensIngredient() {
        return lensIngredient;
    }

    public Ingredient sampleIngredient() {
        return sampleIngredient;
    }

    public ItemStack result() {
        return result;
    }

    public static class Serializer implements RecipeSerializer<MicroscopeRecipe> {
        @Override
        public MicroscopeRecipe fromJson(ResourceLocation id, JsonObject json) {
            if (!json.has("lens") || !json.has("sample")) {
                throw new JsonSyntaxException("Microscope recipe must define both 'lens' and 'sample' ingredients");
            }
            Ingredient lensIngredient = Ingredient.fromJson(json.get("lens"));
            Ingredient sampleIngredient = Ingredient.fromJson(json.get("sample"));
            ItemStack result = CraftingHelper.getItemStack(GsonHelper.getAsJsonObject(json, "result"), true, true);
            return new MicroscopeRecipe(id, lensIngredient, sampleIngredient, result);
        }

        @Override
        public @Nullable MicroscopeRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            Ingredient lensIngredient = Ingredient.fromNetwork(buffer);
            Ingredient sampleIngredient = Ingredient.fromNetwork(buffer);
            ItemStack result = buffer.readItem();
            return new MicroscopeRecipe(id, lensIngredient, sampleIngredient, result);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, MicroscopeRecipe recipe) {
            recipe.lensIngredient.toNetwork(buffer);
            recipe.sampleIngredient.toNetwork(buffer);
            buffer.writeItem(recipe.result);
        }
    }
}
