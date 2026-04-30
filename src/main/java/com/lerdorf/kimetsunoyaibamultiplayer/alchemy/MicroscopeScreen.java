package com.lerdorf.kimetsunoyaibamultiplayer.alchemy;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class MicroscopeScreen extends AbstractContainerScreen<MicroscopeMenu> {
    private static final int TEXTURE_WIDTH = 352;
    private static final int TEXTURE_HEIGHT = 332;
    private static final int CHAMBER_X = 22;
    private static final int CHAMBER_Y = 24;
    private static final int CHAMBER_WIDTH = 42;
    private static final int CHAMBER_HEIGHT = 42;
    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/gui/microscope_gui.png");
    private static final ResourceLocation GUI_OVERLAY_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/gui/microscope_gui_overlay.png");

    public MicroscopeScreen(MicroscopeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(GUI_TEXTURE, leftPos, topPos, imageWidth, imageHeight,
            0.0F, 0.0F, TEXTURE_WIDTH, TEXTURE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        if (menu.hasActiveSample()) {
            renderBubbles(guiGraphics, partialTick);
        }
        guiGraphics.blit(GUI_OVERLAY_TEXTURE, leftPos, topPos, imageWidth, imageHeight,
            0.0F, 0.0F, TEXTURE_WIDTH, TEXTURE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    private void renderBubbles(GuiGraphics guiGraphics, float partialTick) {
        float time = partialTick;
        if (minecraft != null && minecraft.level != null) {
            time += minecraft.level.getGameTime();
        }

        int sampleColor = menu.getSampleTint();
        int[] colors = new int[] {
            sampleColor,
            mix(sampleColor, 0xFFFFFF, 0.45F),
            mix(sampleColor, 0x65E6FF, 0.35F),
            mix(sampleColor, 0xF7A7C4, 0.25F)
        };

        for (int i = 0; i < 11; i++) {
            float speed = 0.28F + (i % 4) * 0.055F;
            float travel = (time * speed + i * 7.0F) % CHAMBER_HEIGHT;
            int radius = 1 + i % 3;
            int x = leftPos + CHAMBER_X + 3 + Math.floorMod(i * 11, CHAMBER_WIDTH - 8)
                + Math.round((float) Math.sin(time * 0.09F + i * 1.7F) * 3.0F);
            int y = topPos + CHAMBER_Y + CHAMBER_HEIGHT - 4 - Math.round(travel);
            x = Math.max(leftPos + CHAMBER_X + 1, Math.min(leftPos + CHAMBER_X + CHAMBER_WIDTH - radius * 2 - 1, x));
            y = Math.max(topPos + CHAMBER_Y + 1, Math.min(topPos + CHAMBER_Y + CHAMBER_HEIGHT - radius * 2 - 1, y));
            drawBubble(guiGraphics, x, y, radius, 0xB0000000 | colors[i % colors.length]);
        }
    }

    private void drawBubble(GuiGraphics guiGraphics, int x, int y, int radius, int color) {
        int size = radius * 2 + 1;
        guiGraphics.fill(x + radius, y, x + radius + 1, y + size, color);
        guiGraphics.fill(x, y + radius, x + size, y + radius + 1, color);
        if (radius > 1) {
            guiGraphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, color);
        }
        guiGraphics.fill(x + 1, y + 1, x + 2, y + 2, 0xD0FFFFFF);
    }

    private int mix(int first, int second, float amount) {
        int r = (int) (((first >> 16) & 0xFF) * (1.0F - amount) + ((second >> 16) & 0xFF) * amount);
        int g = (int) (((first >> 8) & 0xFF) * (1.0F - amount) + ((second >> 8) & 0xFF) * amount);
        int b = (int) ((first & 0xFF) * (1.0F - amount) + (second & 0xFF) * amount);
        return r << 16 | g << 8 | b;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
