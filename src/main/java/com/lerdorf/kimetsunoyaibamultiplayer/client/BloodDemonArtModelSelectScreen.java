package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.customdemonart.BloodDemonArtBuilderData;
import com.lerdorf.kimetsunoyaibamultiplayer.items.CustomDemonArtItem;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.BloodDemonArtBuilderActionPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class BloodDemonArtModelSelectScreen extends Screen {
    private static final int PANEL_WIDTH = 260;
    private static final int PANEL_HEIGHT = 186;

    private final BloodDemonArtBuilderData data;
    private final BloodDemonArtBuilderScreen parent;
    private final List<ActionHitbox> actionHitboxes = new ArrayList<>();
    private int selectedVariant = CustomDemonArtItem.minModelVariant();
    private ItemStack previewStack = ItemStack.EMPTY;

    public BloodDemonArtModelSelectScreen(BloodDemonArtBuilderData data, BloodDemonArtBuilderScreen parent) {
        super(Component.literal("Select Custom Demon Art Model"));
        this.data = data;
        this.parent = parent;
    }

    public Screen parentScreen() {
        return parent;
    }

    @Override
    protected void init() {
        rebuildPreviewStack();
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
        int centerX = left + PANEL_WIDTH / 2;

        guiGraphics.fill(left - 4, top - 4, left + PANEL_WIDTH + 4, top + PANEL_HEIGHT + 4, 0xAA09090C);
        guiGraphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xF1211B19);
        guiGraphics.fill(left + 1, top + 1, left + PANEL_WIDTH - 1, top + PANEL_HEIGHT - 1, 0xF5382B28);

        guiGraphics.drawCenteredString(font, title, centerX, top + 12, 0xF5D18A);
        guiGraphics.drawCenteredString(font, "Choose a model for your item", centerX, top + 28, 0xD6E3C5);
        guiGraphics.drawCenteredString(font, "Model " + selectedVariant + " / " + CustomDemonArtItem.maxModelVariant(), centerX, top + 44, 0xEDE0C1);

        guiGraphics.fill(left + 72, top + 58, left + PANEL_WIDTH - 72, top + 136, 0x88251F1D);
        if (!previewStack.isEmpty()) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(centerX - 24, top + 76, 0);
            guiGraphics.pose().scale(3.0F, 3.0F, 1.0F);
            guiGraphics.renderItem(previewStack, 0, 0);
            guiGraphics.pose().popPose();
        }

        Rect2i prevButton = new Rect2i(left + 26, top + 89, 30, 16);
        guiGraphics.fill(prevButton.getX(), prevButton.getY(), prevButton.getX() + prevButton.getWidth(), prevButton.getY() + prevButton.getHeight(), 0xFF705336);
        guiGraphics.drawCenteredString(font, "<", prevButton.getX() + prevButton.getWidth() / 2, prevButton.getY() + 4, 0xF7EBDD);
        actionHitboxes.add(new ActionHitbox(prevButton, "prev"));

        Rect2i nextButton = new Rect2i(left + PANEL_WIDTH - 56, top + 89, 30, 16);
        guiGraphics.fill(nextButton.getX(), nextButton.getY(), nextButton.getX() + nextButton.getWidth(), nextButton.getY() + nextButton.getHeight(), 0xFF705336);
        guiGraphics.drawCenteredString(font, ">", nextButton.getX() + nextButton.getWidth() / 2, nextButton.getY() + 4, 0xF7EBDD);
        actionHitboxes.add(new ActionHitbox(nextButton, "next"));

        Rect2i confirmButton = new Rect2i(left + 34, top + 150, 88, 18);
        guiGraphics.fill(confirmButton.getX(), confirmButton.getY(), confirmButton.getX() + confirmButton.getWidth(), confirmButton.getY() + confirmButton.getHeight(), 0xFF8A6A3E);
        guiGraphics.drawCenteredString(font, "Confirm", confirmButton.getX() + confirmButton.getWidth() / 2, confirmButton.getY() + 5, 0x1D1208);
        actionHitboxes.add(new ActionHitbox(confirmButton, "confirm"));

        Rect2i cancelButton = new Rect2i(left + PANEL_WIDTH - 122, top + 150, 88, 18);
        guiGraphics.fill(cancelButton.getX(), cancelButton.getY(), cancelButton.getX() + cancelButton.getWidth(), cancelButton.getY() + cancelButton.getHeight(), 0xFF3A2C27);
        guiGraphics.drawCenteredString(font, "Back", cancelButton.getX() + cancelButton.getWidth() / 2, cancelButton.getY() + 5, 0xF0E3C2);
        actionHitboxes.add(new ActionHitbox(cancelButton, "back"));

        guiGraphics.drawCenteredString(font, "Cost: 5 XP", centerX, top + 171, 0xCBE7C8);
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
                if (contains(action.rect(), mouseX, mouseY)) {
                    switch (action.action()) {
                        case "prev" -> cycleVariant(-1);
                        case "next" -> cycleVariant(1);
                        case "confirm" -> ModNetworking.sendToServer(
                            new BloodDemonArtBuilderActionPacket("grant_item", -1, Integer.toString(selectedVariant), "main", -1));
                        case "back" -> onClose();
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void cycleVariant(int direction) {
        int min = CustomDemonArtItem.minModelVariant();
        int max = CustomDemonArtItem.maxModelVariant();
        int size = max - min + 1;
        int normalized = selectedVariant - min;
        selectedVariant = min + Math.floorMod(normalized + direction, size);
        rebuildPreviewStack();
    }

    private void rebuildPreviewStack() {
        previewStack = new ItemStack(ModItems.CUSTOM_DEMON_ART.get());
        CustomDemonArtItem.setModelVariant(previewStack, selectedVariant);
        if (minecraft != null && minecraft.player != null) {
            CustomDemonArtItem.setPlayerSkin(previewStack, minecraft.player.getUUID(), minecraft.player.getGameProfile().getName());
        }
        CustomDemonArtItem.setDisplayInfo(previewStack, data.artName(), "Preview", data.chatColor());
    }

    private static boolean contains(Rect2i rect, double mouseX, double mouseY) {
        return mouseX >= rect.getX() && mouseX <= rect.getX() + rect.getWidth()
            && mouseY >= rect.getY() && mouseY <= rect.getY() + rect.getHeight();
    }

    private record ActionHitbox(Rect2i rect, String action) {
    }
}
