package com.lerdorf.kimetsunoyaibamultiplayer.blocks;

import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.ModAlchemyItems;
import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.ModAlchemyRecipes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class BlueSpiderLilyShearingRecipe extends CustomRecipe {
    public BlueSpiderLilyShearingRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        return !findOutput(container).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        return findOutput(container);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer container) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.is(Items.SHEARS)) {
                ItemStack shears = stack.copy();
                shears.setCount(1);
                shears.setDamageValue(shears.getDamageValue() + 1);
                if (shears.getDamageValue() < shears.getMaxDamage()) {
                    remaining.set(i, shears);
                }
            } else if (stack.is(ModBlocks.BLUE_SPIDER_LILY.get().asItem())) {
                remaining.set(i, new ItemStack(ModBlocks.SPIDER_LILY.get()));
            }
        }
        return remaining;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModAlchemyRecipes.BLUE_SPIDER_LILY_SHEARING_SERIALIZER.get();
    }

    private static ItemStack findOutput(CraftingContainer container) {
        int blueLilies = 0;
        int shears = 0;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(ModBlocks.BLUE_SPIDER_LILY.get().asItem())) {
                blueLilies++;
            } else if (stack.is(Items.SHEARS)) {
                shears++;
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (blueLilies != 1 || shears != 1) {
            return ItemStack.EMPTY;
        }

        return new ItemStack(ModAlchemyItems.BLUE_SPIDER_LILY_PETALS.get(), 2);
    }
}
