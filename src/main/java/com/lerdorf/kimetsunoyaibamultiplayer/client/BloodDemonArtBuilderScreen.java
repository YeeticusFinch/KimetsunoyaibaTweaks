package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.customdemonart.BloodDemonArtBuilderData;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.BloodDemonArtBuilderActionPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class BloodDemonArtBuilderScreen extends Screen {
    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 210;

    private final BloodDemonArtBuilderData data;
    private final Screen parent;
    private final List<ActionHitbox> actionHitboxes = new ArrayList<>();

    public BloodDemonArtBuilderScreen(BloodDemonArtBuilderData data, Screen parent) {
        super(Component.literal("Blood Demon Art Builder"));
        this.data = data;
        this.parent = parent;
    }

    public Screen parentScreen() {
        return parent;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        actionHitboxes.clear();

        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        guiGraphics.fill(left - 4, top - 4, left + PANEL_WIDTH + 4, top + PANEL_HEIGHT + 4, 0xAA09090C);
        guiGraphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xF1211B19);
        guiGraphics.fill(left + 1, top + 1, left + PANEL_WIDTH - 1, top + PANEL_HEIGHT - 1, 0xF5382B28);

        guiGraphics.drawString(font, title, left + 14, top + 12, 0xF5D18A, false);
        guiGraphics.drawString(font, "XP Level: " + data.xpLevel(), left + 14, top + 32, 0xEDE0C1, false);
        guiGraphics.drawString(font, "Muzan Blood: " + data.muzanBlood(), left + 130, top + 32, 0xEDE0C1, false);
        guiGraphics.drawString(font, "Unlocked Slots: " + data.unlockedSlots() + "/10", left + 250, top + 32, 0xCBE7C8, false);

        Rect2i itemButton = new Rect2i(left + 14, top + 54, 146, 20);
        if (!data.hasCustomItem()) {
            guiGraphics.fill(itemButton.getX(), itemButton.getY(), itemButton.getX() + itemButton.getWidth(), itemButton.getY() + itemButton.getHeight(), 0xFF8A6A3E);
            guiGraphics.drawCenteredString(font, "Get Item (5 XP)", itemButton.getX() + itemButton.getWidth() / 2, itemButton.getY() + 6, 0x1D1208);
            actionHitboxes.add(new ActionHitbox(itemButton, "grant_item", -1, "", "main", -1));
        } else {
            guiGraphics.fill(itemButton.getX(), itemButton.getY(), itemButton.getX() + itemButton.getWidth(), itemButton.getY() + itemButton.getHeight(), 0xFF475F4E);
            guiGraphics.drawCenteredString(font, "Custom Item Owned", itemButton.getX() + itemButton.getWidth() / 2, itemButton.getY() + 6, 0xE5F2DD);
        }

        Rect2i coreButton = new Rect2i(left + 14, top + 90, PANEL_WIDTH - 28, 34);
        guiGraphics.fill(coreButton.getX(), coreButton.getY(), coreButton.getX() + coreButton.getWidth(), coreButton.getY() + coreButton.getHeight(), 0xFF3A2C27);
        guiGraphics.drawString(font, "Core Configuration", coreButton.getX() + 10, coreButton.getY() + 6, 0xF5D18A, false);
        guiGraphics.drawString(font, "Particles and Primary/Secondary Effects", coreButton.getX() + 10, coreButton.getY() + 19, 0xD6E3C5, false);
        actionHitboxes.add(new ActionHitbox(coreButton, "open_core", -1, "", "core", -1));

        Rect2i formsButton = new Rect2i(left + 14, top + 132, PANEL_WIDTH - 28, 34);
        guiGraphics.fill(formsButton.getX(), formsButton.getY(), formsButton.getX() + formsButton.getWidth(), formsButton.getY() + formsButton.getHeight(), 0xFF332824);
        guiGraphics.drawString(font, "Forms", formsButton.getX() + 10, formsButton.getY() + 6, 0xF5D18A, false);
        guiGraphics.drawString(font, "View form slots and open form editors", formsButton.getX() + 10, formsButton.getY() + 19, 0xD6E3C5, false);
        actionHitboxes.add(new ActionHitbox(formsButton, "open_forms", -1, "", "forms", -1));

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (ActionHitbox action : actionHitboxes) {
                if (contains(action.rect, mouseX, mouseY)) {
                    if ("open_core".equals(action.action)) {
                        minecraft.setScreen(new BloodDemonArtCoreConfigScreen(data, this));
                    } else if ("open_forms".equals(action.action)) {
                        minecraft.setScreen(new BloodDemonArtFormsScreen(data, this));
                    } else {
                        ModNetworking.sendToServer(new BloodDemonArtBuilderActionPacket(
                            action.action, action.slotIndex, action.value, action.nextView, action.editorSlot));
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static boolean contains(Rect2i rect, double mouseX, double mouseY) {
        return mouseX >= rect.getX() && mouseX <= rect.getX() + rect.getWidth()
            && mouseY >= rect.getY() && mouseY <= rect.getY() + rect.getHeight();
    }

    private record ActionHitbox(Rect2i rect, String action, int slotIndex, String value, String nextView, int editorSlot) {
    }
}
