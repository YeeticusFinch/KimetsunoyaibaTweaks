package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.BloodDemonArtAlchemyCatalog;
import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.ModAlchemyRecipes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class TippedKunaiRecipe extends CustomRecipe {
    public TippedKunaiRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        return findResult(container) != ItemStack.EMPTY;
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        return findResult(container);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer container) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (TippedKunaiUtil.isEffectCarrier(stack)) {
                remaining.set(i, BloodDemonArtAlchemyCatalog.containerReturn(stack));
                break;
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
        return ModAlchemyRecipes.TIPPED_KUNAI_SERIALIZER.get();
    }

    private static ItemStack findResult(CraftingContainer container) {
        ItemStack carrier = ItemStack.EMPTY;
        int carrierSlot = -1;
        int kunaiSlots = 0;
        int nonEmptySlots = 0;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            nonEmptySlots++;

            if (TippedKunaiUtil.isBaseKunai(stack)) {
                kunaiSlots++;
                continue;
            }
            if (TippedKunaiUtil.isEffectCarrier(stack) && carrier.isEmpty()) {
                carrier = stack;
                carrierSlot = i;
                continue;
            }
            return ItemStack.EMPTY;
        }

        if (carrier.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (kunaiSlots == 1 && nonEmptySlots == 2) {
            return TippedKunaiUtil.createTippedKunai(carrier, 1);
        }
        if (kunaiSlots == 8 && nonEmptySlots == 9 && isSurroundedPattern(container, carrierSlot)) {
            return TippedKunaiUtil.createTippedKunai(carrier, 8);
        }
        return ItemStack.EMPTY;
    }

    private static boolean isSurroundedPattern(CraftingContainer container, int carrierSlot) {
        if (container.getWidth() != 3 || container.getHeight() != 3 || carrierSlot != 4) {
            return false;
        }
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (i == 4) {
                continue;
            }
            if (!TippedKunaiUtil.isBaseKunai(container.getItem(i))) {
                return false;
            }
        }
        return true;
    }
}
