package com.lerdorf.kimetsunoyaibamultiplayer.blocks;

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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class UnwaxSpiderLilyRecipe extends CustomRecipe {
    public UnwaxSpiderLilyRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        return findOutput(container) != ItemStack.EMPTY;
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
        return ModAlchemyRecipes.UNWAX_SPIDER_LILY_SERIALIZER.get();
    }

    private static ItemStack findOutput(CraftingContainer container) {
        Block lilyBlock = null;
        int shearsCount = 0;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(Items.SHEARS)) {
                shearsCount++;
                continue;
            }

            Block candidate = Block.byItem(stack.getItem());
            if (candidate != Blocks.AIR && SpiderLilyBlock.unwaxedBlockFor(candidate) != null && lilyBlock == null) {
                lilyBlock = candidate;
                continue;
            }

            return ItemStack.EMPTY;
        }

        if (lilyBlock == null || shearsCount != 1) {
            return ItemStack.EMPTY;
        }

        Block unwaxedBlock = SpiderLilyBlock.unwaxedBlockFor(lilyBlock);
        return unwaxedBlock == null ? ItemStack.EMPTY : new ItemStack(unwaxedBlock);
    }
}
