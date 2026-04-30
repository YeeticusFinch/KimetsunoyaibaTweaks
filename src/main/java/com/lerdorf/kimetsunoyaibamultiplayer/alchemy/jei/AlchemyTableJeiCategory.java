package com.lerdorf.kimetsunoyaibamultiplayer.alchemy.jei;

import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.AlchemyTableRecipe;
import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.ModAlchemyBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class AlchemyTableJeiCategory implements IRecipeCategory<AlchemyTableRecipe> {
    private final IDrawable background;
    private final IDrawable icon;

    public AlchemyTableJeiCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(150, 64);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModAlchemyBlocks.ALCHEMY_TABLE.get()));
    }

    @Override
    public RecipeType<AlchemyTableRecipe> getRecipeType() {
        return AlchemyJeiRecipeTypes.ALCHEMY_TABLE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("menu.kimetsunoyaibamultiplayer.alchemy_table");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AlchemyTableRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 24, 14)
            .addIngredients(recipe.firstIngredient());
        builder.addSlot(RecipeIngredientRole.INPUT, 24, 42)
            .addIngredients(recipe.secondIngredient());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 118, 28)
            .addItemStack(recipe.result());
    }

    @Override
    public void draw(AlchemyTableRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        guiGraphics.drawString(minecraft.font, "+", 50, 30, 0x8A8A8A, false);
        guiGraphics.drawString(minecraft.font, "->", 87, 30, 0x8A8A8A, false);
    }
}
