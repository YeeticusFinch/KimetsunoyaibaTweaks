package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.ModAlchemyBlocks;
import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.ModAlchemyRecipes;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class SakeRecipe extends CustomRecipe {
    public SakeRecipe(ResourceLocation id, CraftingBookCategory category) {
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
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModAlchemyRecipes.SAKE_SERIALIZER.get();
    }

    private static ItemStack findOutput(CraftingContainer container) {
        int fermentedOrchids = 0;
        int brownMushrooms = 0;
        int wheat = 0;
        int bowls = 0;
        int redSpiderLilies = 0;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }

            if (stack.is(ModAlchemyBlocks.FERMENTED_ORCHID.get().asItem())) {
                fermentedOrchids++;
            } else if (stack.is(Items.BROWN_MUSHROOM)) {
                brownMushrooms++;
            } else if (stack.is(Items.WHEAT)) {
                wheat++;
            } else if (stack.is(Items.BOWL)) {
                bowls++;
            } else if (stack.is(ModBlocks.RED_SPIDER_LILY.get().asItem())) {
                redSpiderLilies++;
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (fermentedOrchids != 1 || brownMushrooms != 1 || wheat != 1 || bowls != 1 || redSpiderLilies > 5) {
            return ItemStack.EMPTY;
        }

        return SakeItem.withPotency(new ItemStack(ModItems.SAKE.get()), 1 + redSpiderLilies);
    }
}
