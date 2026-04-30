package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.customdemonart.BloodDemonArtBuilderData;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.BloodDemonArtBuilderActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class BloodDemonArtFormsScreen extends Screen {
    private static final int PANEL_WIDTH = 376;
    private static final int PANEL_HEIGHT = 226;
    private static final int ROW_HEIGHT = 22;
    private static final int VISIBLE_ROWS = 7;

    private final BloodDemonArtBuilderData data;
    private final Screen parent;
    private final List<ActionHitbox> actionHitboxes = new ArrayList<>();
    private int scroll = 0;

    public BloodDemonArtFormsScreen(BloodDemonArtBuilderData data, Screen parent) {
        super(Component.literal("Forms"));
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
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scroll += delta > 0 ? -1 : 1;
        return true;
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

        guiGraphics.drawString(font, title, left + 12, top + 10, 0xF5D18A, false);
        guiGraphics.drawString(font, "Unlocked Slots: " + data.unlockedSlots() + "/10", left + 12, top + 26, 0xCBE7C8, false);

        scroll = clampScroll(scroll, data.slots().size());
        int start = scroll;
        int end = Math.min(data.slots().size(), start + VISIBLE_ROWS);
        int y = top + 44;

        for (int i = start; i < end; i++) {
            BloodDemonArtBuilderData.FormSlotView slot = data.slots().get(i);
            int fill = !slot.unlocked() ? 0xFF1F1B1A : (slot.index() == data.selectedSlot() ? 0xFF6E5536 : 0xFF322824);
            guiGraphics.fill(left + 10, y, left + PANEL_WIDTH - 16, y + 20, fill);

            String label = "Slot " + (slot.index() + 1);
            if (!slot.unlocked()) {
                guiGraphics.drawString(font, label + " - Locked", left + 16, y + 6, 0x7F736A, false);
            } else if (!slot.filled()) {
                guiGraphics.drawString(font, label + " - Empty", left + 16, y + 6, 0xD9C6A1, false);
                Rect2i createButton = new Rect2i(left + PANEL_WIDTH - 122, y + 2, 102, 16);
                guiGraphics.fill(createButton.getX(), createButton.getY(), createButton.getX() + createButton.getWidth(), createButton.getY() + createButton.getHeight(), 0xFF8A6A3E);
                guiGraphics.drawCenteredString(font, "Create (10 XP)", createButton.getX() + createButton.getWidth() / 2, createButton.getY() + 4, 0x1D1208);
                actionHitboxes.add(new ActionHitbox(createButton, "create_slot_and_edit", slot.index(), "", "form_editor", slot.index()));
            } else {
                String moveText = slot.moves().isEmpty() ? "No moves yet"
                    : slot.moves().stream().map(BloodDemonArtBuilderData.MoveView::name).reduce((a, b) -> a + ", " + b).orElse("No moves yet");
                guiGraphics.drawString(font, label + ": " + slot.name(), left + 16, y + 3, 0xF0E3C2, false);
                guiGraphics.drawString(font, font.plainSubstrByWidth(moveText, 176), left + 16, y + 12, 0xBFD1B3, false);

                Rect2i editButton = new Rect2i(left + PANEL_WIDTH - 76, y + 2, 56, 16);
                guiGraphics.fill(editButton.getX(), editButton.getY(), editButton.getX() + editButton.getWidth(), editButton.getY() + editButton.getHeight(), 0xFF705336);
                guiGraphics.drawCenteredString(font, "Edit", editButton.getX() + editButton.getWidth() / 2, editButton.getY() + 4, 0xF7EBDD);
                actionHitboxes.add(new ActionHitbox(editButton, "select_slot", slot.index(), "", "form_editor", slot.index()));
            }
            y += ROW_HEIGHT;
        }

        drawScrollBar(guiGraphics, left + PANEL_WIDTH - 12, top + 44, VISIBLE_ROWS * ROW_HEIGHT - 2, scroll, data.slots().size());
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
                    ModNetworking.sendToServer(new BloodDemonArtBuilderActionPacket(
                        action.action, action.slotIndex, action.value, action.nextView, action.editorSlot));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static int clampScroll(int value, int totalItems) {
        int max = Math.max(0, totalItems - VISIBLE_ROWS);
        return Math.min(max, Math.max(0, value));
    }

    private static void drawScrollBar(GuiGraphics guiGraphics, int x, int y, int height, int scroll, int totalItems) {
        guiGraphics.fill(x, y, x + 4, y + height, 0x66352A26);
        if (totalItems <= VISIBLE_ROWS) {
            guiGraphics.fill(x, y, x + 4, y + height, 0xAA7A6154);
            return;
        }
        int maxScroll = totalItems - VISIBLE_ROWS;
        int thumbHeight = Math.max(18, (int) (height * (VISIBLE_ROWS / (float) totalItems)));
        int travel = height - thumbHeight;
        int thumbY = y + (int) ((scroll / (float) maxScroll) * travel);
        guiGraphics.fill(x, thumbY, x + 4, thumbY + thumbHeight, 0xAA7A6154);
    }

    private static boolean contains(Rect2i rect, double mouseX, double mouseY) {
        return mouseX >= rect.getX() && mouseX <= rect.getX() + rect.getWidth()
            && mouseY >= rect.getY() && mouseY <= rect.getY() + rect.getHeight();
    }

    private record ActionHitbox(Rect2i rect, String action, int slotIndex, String value, String nextView, int editorSlot) {
    }
}
