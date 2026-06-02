package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.customdemonart.BloodDemonArtBuilderData;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.BloodDemonArtBuilderActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class BloodDemonArtBuilderScreen extends Screen {
    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 234;
    private static final int[] CHAT_COLOR_PALETTE = {
        0xAA1E2F, 0xE53935, 0xFB8C00, 0xFDD835, 0x43A047, 0x00ACC1, 0x1E88E5, 0x5E35B1, 0x8E24AA, 0xEC407A, 0x8D6E63, 0x90A4AE
    };

    private final BloodDemonArtBuilderData data;
    private final Screen parent;
    private final List<ActionHitbox> actionHitboxes = new ArrayList<>();
    private EditBox artNameBox;

    public BloodDemonArtBuilderScreen(BloodDemonArtBuilderData data, Screen parent) {
        super(Component.literal("Blood Demon Art Builder"));
        this.data = data;
        this.parent = parent;
    }

    public Screen parentScreen() {
        return parent;
    }

    public static void openFromNetwork(BloodDemonArtBuilderData data, String view, int editorSlot) {
        Minecraft minecraft = Minecraft.getInstance();
        Screen parent = rootParent(minecraft.screen);
        BloodDemonArtBuilderScreen hub = new BloodDemonArtBuilderScreen(data, parent);
        BloodDemonArtFormsScreen forms = new BloodDemonArtFormsScreen(data, hub);
        Screen screen = switch (view) {
            case "core" -> new BloodDemonArtCoreConfigScreen(data, hub);
            case "forms" -> forms;
            case "form_editor" -> new BloodDemonArtFormEditorScreen(data, forms, editorSlot);
            case "model_select" -> new BloodDemonArtModelSelectScreen(data, hub);
            default -> hub;
        };
        minecraft.setScreen(screen);
    }

    private static Screen rootParent(Screen screen) {
        if (screen instanceof BloodDemonArtBuilderScreen builderScreen) {
            return builderScreen.parentScreen();
        }
        if (screen instanceof BloodDemonArtCoreConfigScreen coreConfigScreen) {
            return rootParent(coreConfigScreen.parentScreen());
        }
        if (screen instanceof BloodDemonArtFormsScreen formsScreen) {
            return rootParent(formsScreen.parentScreen());
        }
        if (screen instanceof BloodDemonArtFormEditorScreen formEditorScreen) {
            return rootParent(formEditorScreen.parentScreen());
        }
        if (screen instanceof BloodDemonArtPotionPickerScreen potionPickerScreen) {
            return rootParent(potionPickerScreen.parentScreen());
        }
        if (screen instanceof BloodDemonArtParticleEditorScreen particleEditorScreen) {
            return rootParent(particleEditorScreen.parentScreen());
        }
        if (screen instanceof BloodDemonArtModelSelectScreen modelSelectScreen) {
            return rootParent(modelSelectScreen.parentScreen());
        }
        return screen;
    }

    @Override
    protected void init() {
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        artNameBox = new EditBox(font, left + 110, top + 48, 170, 16, Component.literal("Blood Demon Art Name"));
        artNameBox.setMaxLength(48);
        artNameBox.setValue(data.artName());
        addRenderableWidget(artNameBox);
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
        guiGraphics.drawString(font, "Art Name:", left + 14, top + 52, 0xEDE0C1, false);
        guiGraphics.drawString(font, data.artName() + ": Preview", left + 14, top + 70, data.chatColor(), false);

        Rect2i applyNameButton = new Rect2i(left + 286, top + 48, 60, 16);
        guiGraphics.fill(applyNameButton.getX(), applyNameButton.getY(), applyNameButton.getX() + applyNameButton.getWidth(), applyNameButton.getY() + applyNameButton.getHeight(), 0xFF705336);
        guiGraphics.drawCenteredString(font, "Apply", applyNameButton.getX() + applyNameButton.getWidth() / 2, applyNameButton.getY() + 4, 0xF7EBDD);
        actionHitboxes.add(new ActionHitbox(applyNameButton, "set_art_name", -1, "", "main", -1));

        Rect2i colorButton = new Rect2i(left + 286, top + 68, 60, 16);
        guiGraphics.fill(colorButton.getX(), colorButton.getY(), colorButton.getX() + colorButton.getWidth(), colorButton.getY() + colorButton.getHeight(), data.chatColor());
        guiGraphics.drawCenteredString(font, "Color", colorButton.getX() + colorButton.getWidth() / 2, colorButton.getY() + 4, 0xFFFFFF);
        actionHitboxes.add(new ActionHitbox(colorButton, "set_chat_color", -1, "", "main", -1));

        Rect2i itemButton = new Rect2i(left + 14, top + 88, 146, 20);
        if (!data.hasCustomItem()) {
            guiGraphics.fill(itemButton.getX(), itemButton.getY(), itemButton.getX() + itemButton.getWidth(), itemButton.getY() + itemButton.getHeight(), 0xFF8A6A3E);
            guiGraphics.drawCenteredString(font, "Get Item (Select Model)", itemButton.getX() + itemButton.getWidth() / 2, itemButton.getY() + 6, 0x1D1208);
            actionHitboxes.add(new ActionHitbox(itemButton, "open_model_select", -1, "", "main", -1));
        } else {
            guiGraphics.fill(itemButton.getX(), itemButton.getY(), itemButton.getX() + itemButton.getWidth(), itemButton.getY() + itemButton.getHeight(), 0xFF475F4E);
            guiGraphics.drawCenteredString(font, "Custom Item Owned", itemButton.getX() + itemButton.getWidth() / 2, itemButton.getY() + 6, 0xE5F2DD);
        }

        Rect2i coreButton = new Rect2i(left + 14, top + 124, PANEL_WIDTH - 28, 34);
        guiGraphics.fill(coreButton.getX(), coreButton.getY(), coreButton.getX() + coreButton.getWidth(), coreButton.getY() + coreButton.getHeight(), 0xFF3A2C27);
        guiGraphics.drawString(font, "Core Configuration", coreButton.getX() + 10, coreButton.getY() + 6, 0xF5D18A, false);
        guiGraphics.drawString(font, "Particles and Primary/Secondary Effects", coreButton.getX() + 10, coreButton.getY() + 19, 0xD6E3C5, false);
        actionHitboxes.add(new ActionHitbox(coreButton, "open_core", -1, "", "core", -1));

        Rect2i formsButton = new Rect2i(left + 14, top + 166, PANEL_WIDTH - 28, 34);
        guiGraphics.fill(formsButton.getX(), formsButton.getY(), formsButton.getX() + formsButton.getWidth(), formsButton.getY() + formsButton.getHeight(), 0xFF332824);
        guiGraphics.drawString(font, "Forms", formsButton.getX() + 10, formsButton.getY() + 6, 0xF5D18A, false);
        guiGraphics.drawString(font, "View form slots and open form editors", formsButton.getX() + 10, formsButton.getY() + 19, 0xD6E3C5, false);
        actionHitboxes.add(new ActionHitbox(formsButton, "open_forms", -1, "", "forms", -1));

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (artNameBox != null && artNameBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (keyCode == 257 || keyCode == 335) {
            sendArtName();
            return true;
        }
        if (keyCode == 256) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (artNameBox != null && artNameBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 0) {
            for (ActionHitbox action : actionHitboxes) {
                if (contains(action.rect, mouseX, mouseY)) {
                    if ("open_core".equals(action.action)) {
                        minecraft.setScreen(new BloodDemonArtCoreConfigScreen(data, this));
                    } else if ("open_forms".equals(action.action)) {
                        minecraft.setScreen(new BloodDemonArtFormsScreen(data, this));
                    } else if ("open_model_select".equals(action.action)) {
                        minecraft.setScreen(new BloodDemonArtModelSelectScreen(data, this));
                    } else if ("set_art_name".equals(action.action)) {
                        sendArtName();
                    } else if ("set_chat_color".equals(action.action)) {
                        int nextColor = nextChatColor(data.chatColor());
                        ModNetworking.sendToServer(new BloodDemonArtBuilderActionPacket("set_chat_color", -1, Integer.toString(nextColor), "main", -1));
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

    private void sendArtName() {
        if (artNameBox == null) {
            return;
        }
        String value = artNameBox.getValue() == null ? "" : artNameBox.getValue();
        ModNetworking.sendToServer(new BloodDemonArtBuilderActionPacket("set_art_name", -1, value, "main", -1));
    }

    private static int nextChatColor(int current) {
        for (int i = 0; i < CHAT_COLOR_PALETTE.length; i++) {
            if (CHAT_COLOR_PALETTE[i] == current) {
                return CHAT_COLOR_PALETTE[(i + 1) % CHAT_COLOR_PALETTE.length];
            }
        }
        return CHAT_COLOR_PALETTE[0];
    }

    private record ActionHitbox(Rect2i rect, String action, int slotIndex, String value, String nextView, int editorSlot) {
    }
}
