package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SetDemonEyesPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.util.DemonEyesHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public class DemonEyesCustomizationScreen extends Screen {
    private static final int PANEL_HEIGHT = 260;
    private static final int PANEL_MARGIN = 8;
    private final Screen parent;
    private final MeditationMenuScreen meditationScreen;
    private List<Integer> availableIndices = List.of(DemonEyesHelper.DEFAULT_DEMON_EYES_INDEX);
    private int currentIndex = DemonEyesHelper.DEFAULT_DEMON_EYES_INDEX;
    private int currentHue = DemonEyesHelper.DEFAULT_DEMON_EYES_HUE;
    private float currentOffsetX = DemonEyesHelper.DEFAULT_DEMON_EYES_OFFSET;
    private float currentOffsetY = DemonEyesHelper.DEFAULT_DEMON_EYES_OFFSET;
    private EditBox offsetXBox;
    private EditBox offsetYBox;

    public DemonEyesCustomizationScreen(MeditationMenuScreen parent, int initialIndex, int initialHue) {
        super(Component.literal("Demon Eyes"));
        this.parent = parent;
        this.meditationScreen = parent;
        this.currentIndex = initialIndex;
        this.currentHue = DemonEyesHelper.normalizeHue(initialHue);
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            DemonEyesClientState.PlayerDemonEyesState state = DemonEyesClientState.getPlayerState(player.getUUID());
            if (state != null) {
                this.currentOffsetX = state.offsetX();
                this.currentOffsetY = state.offsetY();
            }
        }
    }

    @Override
    protected void init() {
        availableIndices = DemonEyesResourceHelper.getAvailableIndices();
        currentIndex = normalizeIndex(currentIndex);

        int centerX = this.width / 2;
        int panelWidth = getPanelWidth();
        int previewCenterX = getPreviewCenterX();
        int panelLeft = centerX - panelWidth / 2;
        int controlLeft = panelLeft + 12;
        int buttonY = getPanelTop() + 178;

        offsetXBox = new EditBox(this.font, controlLeft, buttonY - 108, 116, 20, Component.literal("X offset"));
        offsetXBox.setMaxLength(32);
        offsetXBox.setValue(Float.toString(currentOffsetX));
        offsetXBox.setResponder(value -> updateOffsetsFromInput());
        addRenderableWidget(offsetXBox);

        offsetYBox = new EditBox(this.font, controlLeft, buttonY - 62, 116, 20, Component.literal("Y offset"));
        offsetYBox.setMaxLength(32);
        offsetYBox.setValue(Float.toString(currentOffsetY));
        offsetYBox.setResponder(value -> updateOffsetsFromInput());
        addRenderableWidget(offsetYBox);

        addRenderableWidget(Button.builder(Component.literal("<"), button -> cycle(-1))
            .bounds(previewCenterX - 92, buttonY, 20, 20)
            .build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> cycle(1))
            .bounds(previewCenterX + 72, buttonY, 20, 20)
            .build());
        addRenderableWidget(new HueSlider(previewCenterX - 80, buttonY + 26, 160, 20));
        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
            .bounds(controlLeft + 18, getPanelTop() + 34, 80, 20)
            .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);

        int centerX = this.width / 2;
        int panelWidth = getPanelWidth();
        int left = centerX - panelWidth / 2;
        int top = getPanelTop();
        int right = left + panelWidth;
        int bottom = top + getPanelHeight();
        int previewCenterX = getPreviewCenterX();
        int controlLeft = left + 12;

        guiGraphics.fill(left - 3, top - 3, right + 3, bottom + 3, 0xAA050505);
        guiGraphics.fill(left, top, right, bottom, 0xF11B1412);
        guiGraphics.drawCenteredString(this.font, this.title, centerX, top + 12, 0xFFE8C8);
        guiGraphics.drawCenteredString(this.font, Component.literal(DemonEyesResourceHelper.getLabel(currentIndex)), previewCenterX, top + 28, 0xFFF3E3);
        guiGraphics.drawCenteredString(this.font, Component.literal("Hue " + currentHue), previewCenterX, top + 42, 0xFFC9B7A5);

        guiGraphics.drawString(this.font, Component.literal("X Offset"), controlLeft, top + 58, 0xFFC9B7A5, false);
        guiGraphics.drawString(this.font, Component.literal("Y Offset"), controlLeft, top + 104, 0xFFC9B7A5, false);

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player != null) {
            guiGraphics.fill(previewCenterX - 54, top + 56, previewCenterX + 54, top + 162, 0x55110C0A);
            InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, previewCenterX, top + 156, 48,
                previewCenterX - mouseX, top + 116 - mouseY, player);
            guiGraphics.drawCenteredString(this.font, Component.literal("Live Preview"), previewCenterX, top + 168, 0xFFD4B58D);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void cycle(int direction) {
        if (availableIndices.isEmpty()) {
            return;
        }
        int currentPosition = availableIndices.indexOf(normalizeIndex(currentIndex));
        if (currentPosition < 0) {
            currentPosition = 0;
        }
        int next = Math.floorMod(currentPosition + direction, availableIndices.size());
        currentIndex = availableIndices.get(next);
        currentHue = DemonEyesHelper.DEFAULT_DEMON_EYES_HUE;
        meditationScreen.updateLocalDemonEyesStyle(currentIndex, currentHue);
        sendCurrentStyle();
    }

    private int normalizeIndex(int index) {
        if (availableIndices.contains(index)) {
            return index;
        }
        if (availableIndices.contains(DemonEyesHelper.DEFAULT_DEMON_EYES_INDEX)) {
            return DemonEyesHelper.DEFAULT_DEMON_EYES_INDEX;
        }
        return availableIndices.get(0);
    }

    private void setHue(int hue) {
        currentHue = DemonEyesHelper.normalizeHue(hue);
        meditationScreen.updateLocalDemonEyesStyle(currentIndex, currentHue);
        sendCurrentStyle();
    }

    private void updateOffsetsFromInput() {
        if (offsetXBox == null || offsetYBox == null) {
            return;
        }
        Float offsetX = parseFloat(offsetXBox.getValue());
        Float offsetY = parseFloat(offsetYBox.getValue());
        if (offsetX == null || offsetY == null) {
            return;
        }
        currentOffsetX = DemonEyesHelper.normalizeOffset(offsetX);
        currentOffsetY = DemonEyesHelper.normalizeOffset(offsetY);
        sendCurrentStyle();
    }

    private Float parseFloat(String value) {
        try {
            float parsed = Float.parseFloat(value);
            return Float.isFinite(parsed) ? parsed : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void sendCurrentStyle() {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            DemonEyesClientState.PlayerDemonEyesState state = DemonEyesClientState.getPlayerState(player.getUUID());
            int rankTier = state == null ? -1 : state.rankTier();
            DemonEyesClientState.setPlayerState(player.getUUID(), true, currentIndex, currentHue, rankTier,
                currentOffsetX, currentOffsetY);
        }
        ModNetworking.sendToServer(new SetDemonEyesPacket(currentIndex, currentHue, currentOffsetX, currentOffsetY));
    }

    private int getPanelTop() {
        return (this.height - getPanelHeight()) / 2;
    }

    private int getPanelHeight() {
        return Math.min(PANEL_HEIGHT, Math.max(1, this.height - PANEL_MARGIN * 2));
    }

    private int getPanelWidth() {
        return Math.min(360, this.width - 16);
    }

    private int getPreviewCenterX() {
        return this.width / 2 + Math.min(65, getPanelWidth() / 2 - 92);
    }

    private class HueSlider extends AbstractSliderButton {
        HueSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(), currentHue / 359.0D);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal("Hue: " + currentHue));
        }

        @Override
        protected void applyValue() {
            setHue((int) Math.round(this.value * 359.0D));
            updateMessage();
        }
    }
}
