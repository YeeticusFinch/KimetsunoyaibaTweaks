package com.lerdorf.kimetsunoyaibamultiplayer.alchemy;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

final class AlchemyScreenSupport {
    private AlchemyScreenSupport() {
    }

    static void drawPanel(GuiGraphics guiGraphics, int left, int top, int width, int height, Component title,
                          net.minecraft.client.gui.Font font) {
        guiGraphics.fill(left - 4, top - 4, left + width + 4, top + height + 4, 0xAA09090C);
        guiGraphics.fill(left, top, left + width, top + height, 0xF1211B19);
        guiGraphics.fill(left + 1, top + 1, left + width - 1, top + height - 1, 0xF5382B28);
        guiGraphics.drawString(font, title, left + 12, top + 10, 0xF5D18A, false);
    }

    static Button buildActionButton(int x, int y, int width, Component label, Button.OnPress onPress) {
        return Button.builder(label, onPress)
            .bounds(x, y, width, 20)
            .build();
    }
}
