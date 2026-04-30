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

public class AlchemyTableRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final Ingredient firstIngredient;
    private final Ingredient secondIngredient;
    private final ItemStack result;
    private final boolean unordered;

    public AlchemyTableRecipe(ResourceLocation id, Ingredient firstIngredient, Ingredient secondIngredient, ItemStack result, boolean unordered) {
        this.id = id;
        this.firstIngredient = firstIngredient;
        this.secondIngredient = secondIngredient;
        this.result = result;
        this.unordered = unordered;
    }

    @Override
    public boolean matches(Container container, Level level) {
        ItemStack first = container.getItem(0);
        ItemStack second = container.getItem(1);
        if (first.isEmpty() || second.isEmpty()) {
            return false;
        }

        if (unordered) {
            return (firstIngredient.test(first) && secondIngredient.test(second))
                || (firstIngredient.test(second) && secondIngredient.test(first));
        }
        return firstIngredient.test(first) && secondIngredient.test(second);
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
        return ModAlchemyRecipes.ALCHEMY_TABLE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModAlchemyRecipes.ALCHEMY_TABLE_TYPE;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(firstIngredient);
        ingredients.add(secondIngredient);
        return ingredients;
    }

    public Ingredient firstIngredient() {
        return firstIngredient;
    }

    public Ingredient secondIngredient() {
        return secondIngredient;
    }

    public ItemStack result() {
        return result;
    }

    public static class Serializer implements RecipeSerializer<AlchemyTableRecipe> {
        @Override
        public AlchemyTableRecipe fromJson(ResourceLocation id, JsonObject json) {
            if (!json.has("first") || !json.has("second")) {
                throw new JsonSyntaxException("Alchemy table recipe must define both 'first' and 'second' ingredients");
            }
            Ingredient firstIngredient = Ingredient.fromJson(json.get("first"));
            Ingredient secondIngredient = Ingredient.fromJson(json.get("second"));
            ItemStack result = CraftingHelper.getItemStack(GsonHelper.getAsJsonObject(json, "result"), true, true);
            boolean unordered = GsonHelper.getAsBoolean(json, "unordered", true);
            return new AlchemyTableRecipe(id, firstIngredient, secondIngredient, result, unordered);
        }

        @Override
        public @Nullable AlchemyTableRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            Ingredient firstIngredient = Ingredient.fromNetwork(buffer);
            Ingredient secondIngredient = Ingredient.fromNetwork(buffer);
            ItemStack result = buffer.readItem();
            boolean unordered = buffer.readBoolean();
            return new AlchemyTableRecipe(id, firstIngredient, secondIngredient, result, unordered);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, AlchemyTableRecipe recipe) {
            recipe.firstIngredient.toNetwork(buffer);
            recipe.secondIngredient.toNetwork(buffer);
            buffer.writeItem(recipe.result);
            buffer.writeBoolean(recipe.unordered);
        }
    }
}
