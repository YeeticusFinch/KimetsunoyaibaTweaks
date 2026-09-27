package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.ModAlchemyRecipes;
import com.lerdorf.kimetsunoyaibamultiplayer.api.SwordMetadataRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Crafts a generic nichirin sword, or the level-0 sword for two matching ore styles.
 */
public class NichirinSwordOreRecipe extends CustomRecipe {
    private static final ResourceLocation BASE_SCARLET_ORE =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "scarlet_ore");
    private static final ResourceLocation BASE_SCARLET_IRONSAND =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "scarlet_ironsand");
    private static final ResourceLocation BASE_NICHIRIN_SWORD =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "nichirinsword");

    public NichirinSwordOreRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        return findResult(container).getItem() != Items.AIR;
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        return findResult(container);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer container) {
        return NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModAlchemyRecipes.NICHIRIN_SWORD_ORE_SERIALIZER.get();
    }

    private static ItemStack findResult(CraftingContainer container) {
        if (container.getWidth() < 3 || container.getHeight() < 2) {
            return ItemStack.EMPTY;
        }

        for (int row = 0; row <= container.getHeight() - 2; row++) {
            for (int column = 0; column <= container.getWidth() - 3; column++) {
                if (!matchesPattern(container, column, row)) {
                    continue;
                }

                ItemStack firstOre = container.getItem(index(container, column, row));
                ItemStack secondOre = container.getItem(index(container, column + 2, row));
                return createSword(firstOre, secondOre);
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean matchesPattern(CraftingContainer container, int column, int row) {
        for (int currentRow = 0; currentRow < container.getHeight(); currentRow++) {
            for (int currentColumn = 0; currentColumn < container.getWidth(); currentColumn++) {
                boolean inPattern = currentRow == row && (currentColumn == column || currentColumn == column + 2)
                    || currentRow == row && currentColumn == column + 1
                    || currentRow == row + 1 && currentColumn == column
                    || currentRow == row + 1 && currentColumn == column + 1
                    || currentRow == row + 1 && currentColumn == column + 2;
                ItemStack stack = container.getItem(index(container, currentColumn, currentRow));
                boolean expected = switch (patternCharacter(currentColumn - column, currentRow - row)) {
                    case 'O' -> isScarletOre(stack);
                    case 'S' -> isScarletIronSand(stack);
                    case 'I' -> stack.is(Items.IRON_INGOT);
                    default -> stack.isEmpty();
                };
                if (!expected || (!inPattern && !stack.isEmpty())) {
                    return false;
                }
            }
        }
        return true;
    }

    private static char patternCharacter(int column, int row) {
        if (row == 0 && column == 0 || row == 0 && column == 2) {
            return 'O';
        }
        if (row == 0 && column == 1 || row == 1 && column == 0) {
            return 'S';
        }
        if (row == 1 && column == 1) {
            return 'I';
        }
        return ' ';
    }

    private static int index(CraftingContainer container, int column, int row) {
        return row * container.getWidth() + column;
    }

    private static boolean isScarletOre(ItemStack stack) {
        return stack.is(ForgeRegistries.ITEMS.getValue(BASE_SCARLET_ORE))
            || stack.is(ModItems.NICHIRIN_ORE.get());
    }

    private static boolean isScarletIronSand(ItemStack stack) {
        return stack.is(ForgeRegistries.ITEMS.getValue(BASE_SCARLET_IRONSAND));
    }

    private static ItemStack createSword(ItemStack firstOre, ItemStack secondOre) {
        String firstStyle = NichirinOreItem.getStyleId(firstOre);
        String secondStyle = NichirinOreItem.getStyleId(secondOre);
        if (firstStyle != null && !firstStyle.isEmpty() && firstStyle.equals(secondStyle)) {
            Item matchingSword = resolveLevelZeroSword(firstStyle);
            if (matchingSword != null) {
                return new ItemStack(matchingSword);
            }
        }

        Item fallback = ForgeRegistries.ITEMS.getValue(BASE_NICHIRIN_SWORD);
        return fallback == null ? ItemStack.EMPTY : new ItemStack(fallback);
    }

    private static Item resolveLevelZeroSword(String styleId) {
        List<Item> swords = new ArrayList<>();
        for (SwordRegistry.RegisteredSword sword : SwordRegistry.getSwordsByStyleAndLevel(styleId, 0)) {
            if (sword.getSwordItem() != null) {
                swords.add(sword.getSwordItem());
            }
        }
        for (SwordMetadataRegistry.SwordMetadata sword : SwordMetadataRegistry.getSwordsByStyleAndLevel(styleId, 0)) {
            if (sword.getSwordItem() != null) {
                swords.add(sword.getSwordItem());
            }
        }
        return swords.stream()
            .filter(item -> item != Items.AIR)
            .sorted(Comparator.comparing(item -> ForgeRegistries.ITEMS.getKey(item).toString()))
            .findFirst()
            .orElse(null);
    }
}
