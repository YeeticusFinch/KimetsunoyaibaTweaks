package com.lerdorf.kimetsunoyaibamultiplayer.alchemy;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

public class UnidentifiedRainbowLilyCultureRecipe extends CustomRecipe {
    public UnidentifiedRainbowLilyCultureRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        return !assemble(container, level.registryAccess()).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        int cultureBonus = -1;
        int immortalDaisy = 0;
        Map<Item, Integer> uniqueLilyScores = new HashMap<>();

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(ModAlchemyItems.NEUROTOXIC_CULTURE.get()) || stack.is(ModAlchemyItems.PROTEOLYTIC_CULTURE.get()) || stack.is(ModAlchemyItems.HERBAL_CULTURE.get())) {
                if (cultureBonus != -1) {
                    return ItemStack.EMPTY;
                }
                cultureBonus = stack.is(ModAlchemyItems.PROTEOLYTIC_CULTURE.get()) ? 2 : stack.is(ModAlchemyItems.NEUROTOXIC_CULTURE.get()) ? 1 : 0;
            } else if (stack.is(ModAlchemyBlocks.IMMORTAL_DAISY.get().asItem())) {
                immortalDaisy++;
            } else {
                Integer lilyScore = spiderLilyScore(stack);
                if (lilyScore == null) {
                    return ItemStack.EMPTY;
                }
                uniqueLilyScores.putIfAbsent(stack.getItem(), lilyScore);
            }
        }

        if (cultureBonus < 0 || immortalDaisy != 1 || uniqueLilyScores.isEmpty()) {
            return ItemStack.EMPTY;
        }

        int potency = cultureBonus;
        for (int score : uniqueLilyScores.values()) {
            potency += score;
        }

        return BloodDemonArtAlchemyCatalog.withPotency(
            new ItemStack(ModAlchemyItems.UNIDENTIFIED_RAINBOW_LILY_CULTURE.get()),
            Math.max(0, potency)
        );
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModAlchemyRecipes.UNIDENTIFIED_RAINBOW_LILY_CULTURE_SERIALIZER.get();
    }

    private static Integer spiderLilyScore(ItemStack stack) {
        if (stack.is(ModBlocks.PURPLE_SPIDER_LILY.get().asItem())) {
            return 2;
        }
        if (stack.is(ModBlocks.BLUE_SPIDER_LILY.get().asItem())) {
            return -2;
        }
        if (stack.is(ModBlocks.WHITE_SPIDER_LILY.get().asItem())
            || stack.is(ModBlocks.RED_SPIDER_LILY.get().asItem())
            || stack.is(ModBlocks.YELLOW_SPIDER_LILY.get().asItem())
            || stack.is(ModBlocks.LIME_SPIDER_LILY.get().asItem())
            || stack.is(ModBlocks.PINK_SPIDER_LILY.get().asItem())
            || stack.is(ModBlocks.ORANGE_SPIDER_LILY.get().asItem())) {
            return 1;
        }
        return null;
    }
}
