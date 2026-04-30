package com.lerdorf.kimetsunoyaibamultiplayer.alchemy.jei;

import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.MicroscopeRecipe;
import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.ModAlchemyItems;
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

public class MicroscopeJeiCategory implements IRecipeCategory<MicroscopeRecipe> {
    private final IDrawable background;
    private final IDrawable icon;

    public MicroscopeJeiCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(150, 64);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModAlchemyItems.AMETHYST_LENS.get()));
    }

    @Override
    public RecipeType<MicroscopeRecipe> getRecipeType() {
        return AlchemyJeiRecipeTypes.MICROSCOPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("menu.kimetsunoyaibamultiplayer.microscope");
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
    public void setRecipe(IRecipeLayoutBuilder builder, MicroscopeRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 24, 14)
            .addIngredients(recipe.lensIngredient());
        builder.addSlot(RecipeIngredientRole.INPUT, 24, 42)
            .addIngredients(recipe.sampleIngredient());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 118, 28)
            .addItemStack(recipe.result());
    }

    @Override
    public void draw(MicroscopeRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        guiGraphics.drawString(minecraft.font, "+", 50, 30, 0x8A8A8A, false);
        guiGraphics.drawString(minecraft.font, "->", 87, 30, 0x8A8A8A, false);
    }
}
