package com.lerdorf.kimetsunoyaibamultiplayer.alchemy.jei;

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
import net.minecraft.world.item.Items;

public class AlchemyBrewingJeiCategory implements IRecipeCategory<AlchemyBrewingJeiRecipe> {
    private final IDrawable background;
    private final IDrawable icon;

    public AlchemyBrewingJeiCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(150, 64);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(Items.BREWING_STAND));
    }

    @Override
    public RecipeType<AlchemyBrewingJeiRecipe> getRecipeType() {
        return AlchemyJeiRecipeTypes.ALCHEMY_BREWING;
    }

    @Override
    public Component getTitle() {
        return Component.literal("Alchemy Brewing");
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
    public void setRecipe(IRecipeLayoutBuilder builder, AlchemyBrewingJeiRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 24, 28)
            .addItemStack(recipe.ingredient());
        builder.addSlot(RecipeIngredientRole.INPUT, 59, 28)
            .addItemStack(recipe.input());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 118, 28)
            .addItemStack(recipe.output());
    }

    @Override
    public void draw(AlchemyBrewingJeiRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        guiGraphics.drawString(minecraft.font, "+", 46, 30, 0x8A8A8A, false);
        guiGraphics.drawString(minecraft.font, "->", 87, 30, 0x8A8A8A, false);
    }
}
