package com.lerdorf.kimetsunoyaibamultiplayer.alchemy;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class AlchemyTableScreen extends AbstractContainerScreen<AlchemyTableMenu> {
    private static final int TEXTURE_WIDTH = 352;
    private static final int TEXTURE_HEIGHT = 332;
    private static final int ARROW_X = 97;
    private static final int ARROW_Y = 16;
    private static final int ARROW_WIDTH = 9;
    private static final int ARROW_HEIGHT = 28;
    private static final int FLAME_X = 62;
    private static final int FLAME_Y = 32;
    private static final int FLAME_SIZE = 14;
    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/gui/alchemy_table_gui.png");
    private static final ResourceLocation BREWING_STAND_TEXTURE =
        ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/brewing_stand.png");
    private static final ResourceLocation FURNACE_TEXTURE =
        ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/furnace.png");

    public AlchemyTableScreen(AlchemyTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (imageWidth - font.width(title)) / 2;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(GUI_TEXTURE, leftPos, topPos, imageWidth, imageHeight,
            0.0F, 0.0F, TEXTURE_WIDTH, TEXTURE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        int flame = menu.getScaledBurnProgress(FLAME_SIZE);
        if (flame > 0) {
            guiGraphics.blit(FURNACE_TEXTURE, leftPos + FLAME_X, topPos + FLAME_Y + FLAME_SIZE - flame,
                176, FLAME_SIZE - flame, FLAME_SIZE, flame);
        }

        int progress = menu.getScaledCookProgress(ARROW_HEIGHT);
        if (progress > 0) {
            guiGraphics.blit(BREWING_STAND_TEXTURE, leftPos + ARROW_X, topPos + ARROW_Y,
                176, 0, ARROW_WIDTH, progress);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
