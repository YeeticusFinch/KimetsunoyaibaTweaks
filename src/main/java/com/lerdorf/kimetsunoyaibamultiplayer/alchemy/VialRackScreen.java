package com.lerdorf.kimetsunoyaibamultiplayer.alchemy;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class VialRackScreen extends AbstractContainerScreen<VialRackMenu> {
    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/generic_54.png");

    public VialRackScreen(VialRackMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 133 + (menu.getRackCount() - 1) * 18;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (imageWidth - font.width(title)) / 2;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 39 + (menu.getRackCount() - 1) * 18;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int rackRows = menu.getRackCount();
        int inventoryY = imageHeight - 96;
        guiGraphics.blit(GUI_TEXTURE, leftPos, topPos, 0, 0, imageWidth, 17);
        guiGraphics.fill(leftPos, topPos + 17, leftPos + imageWidth, topPos + inventoryY, 0xFFC6C6C6);
        guiGraphics.fill(leftPos + 4, topPos + 17, leftPos + imageWidth - 4, topPos + inventoryY, 0xFF8B8B8B);
        for (int rack = 0; rack < rackRows; rack++) {
            for (int slot = 0; slot < VialRackContents.SLOT_COUNT; slot++) {
                guiGraphics.blit(GUI_TEXTURE, leftPos + 43 + slot * 18, topPos + 19 + rack * 18, 7, 17, 18, 18);
            }
        }
        guiGraphics.blit(GUI_TEXTURE, leftPos, topPos + inventoryY, 0, 126, imageWidth, 96);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
