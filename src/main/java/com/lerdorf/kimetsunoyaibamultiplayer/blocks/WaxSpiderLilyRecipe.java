package com.lerdorf.kimetsunoyaibamultiplayer.blocks;

import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.ModAlchemyRecipes;
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

public class WaxSpiderLilyRecipe extends CustomRecipe {
    public WaxSpiderLilyRecipe(ResourceLocation id, CraftingBookCategory category) {
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
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModAlchemyRecipes.WAX_SPIDER_LILY_SERIALIZER.get();
    }

    private static ItemStack findOutput(CraftingContainer container) {
        Block lilyBlock = null;
        int honeycombCount = 0;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(Items.HONEYCOMB)) {
                honeycombCount++;
                continue;
            }

            Block candidate = Block.byItem(stack.getItem());
            if (candidate != Blocks.AIR && SpiderLilyBlock.waxedBlockFor(candidate) != null && lilyBlock == null) {
                lilyBlock = candidate;
                continue;
            }

            return ItemStack.EMPTY;
        }

        if (lilyBlock == null || honeycombCount != 1) {
            return ItemStack.EMPTY;
        }

        Block waxedBlock = SpiderLilyBlock.waxedBlockFor(lilyBlock);
        return waxedBlock == null ? ItemStack.EMPTY : new ItemStack(waxedBlock);
    }
}
