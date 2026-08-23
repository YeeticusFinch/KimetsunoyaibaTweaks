package com.lerdorf.kimetsunoyaibamultiplayer.alchemy;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class UnidentifiedBlueSpiderLilyCultureRecipe extends CustomRecipe {
    private static final int MIN_BLUE_LILIES = 1;
    private static final int MAX_BLUE_LILIES = 6;

    public UnidentifiedBlueSpiderLilyCultureRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        return !assemble(container, level.registryAccess()).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        int cultureBonus = -1;
        int hemolithDust = 0;
        int immortalDaisy = 0;
        int blueLilies = 0;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(ModAlchemyItems.VIRAL_CULTURE.get()) || stack.is(ModAlchemyItems.HEMOMIMETIC_CULTURE.get()) || stack.is(ModAlchemyItems.VITALITY_CULTURE.get())) {
                if (cultureBonus != -1) {
                    return ItemStack.EMPTY;
                }
                cultureBonus = stack.is(ModAlchemyItems.HEMOMIMETIC_CULTURE.get()) ? 2 : stack.is(ModAlchemyItems.VITALITY_CULTURE.get()) ? 1 : 0;
            } else if (stack.is(ModItems.HEMOLITH_DUST.get())) {
                hemolithDust++;
            } else if (stack.is(ModAlchemyBlocks.IMMORTAL_DAISY.get().asItem())) {
                immortalDaisy++;
            } else if (stack.is(ModBlocks.BLUE_SPIDER_LILY.get().asItem())) {
                blueLilies++;
                if (blueLilies > MAX_BLUE_LILIES) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (cultureBonus < 0 || hemolithDust != 1 || immortalDaisy != 1 || blueLilies < MIN_BLUE_LILIES || blueLilies > MAX_BLUE_LILIES) {
            return ItemStack.EMPTY;
        }

        return BloodDemonArtAlchemyCatalog.withPotency(
            new ItemStack(ModAlchemyItems.UNIDENTIFIED_BLUE_SPIDER_LILY_CULTURE.get()),
            Math.min(8, cultureBonus + blueLilies)
        );
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 4;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModAlchemyRecipes.UNIDENTIFIED_BLUE_SPIDER_LILY_CULTURE_SERIALIZER.get();
    }
}
