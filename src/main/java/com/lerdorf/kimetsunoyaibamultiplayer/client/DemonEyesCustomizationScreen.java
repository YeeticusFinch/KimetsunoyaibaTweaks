package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SetDemonEyesPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.util.DemonEyesHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public class DemonEyesCustomizationScreen extends Screen {
    private final Screen parent;
    private final MeditationMenuScreen meditationScreen;
    private List<Integer> availableIndices = List.of(DemonEyesHelper.DEFAULT_DEMON_EYES_INDEX);
    private int currentIndex = DemonEyesHelper.DEFAULT_DEMON_EYES_INDEX;
    private int currentHue = DemonEyesHelper.DEFAULT_DEMON_EYES_HUE;

    public DemonEyesCustomizationScreen(MeditationMenuScreen parent, int initialIndex, int initialHue) {
        super(Component.literal("Demon Eyes"));
        this.parent = parent;
        this.meditationScreen = parent;
        this.currentIndex = initialIndex;
        this.currentHue = DemonEyesHelper.normalizeHue(initialHue);
    }

    @Override
    protected void init() {
        availableIndices = DemonEyesResourceHelper.getAvailableIndices();
        currentIndex = normalizeIndex(currentIndex);

        int centerX = this.width / 2;
        int buttonY = this.height / 2 + 58;
        addRenderableWidget(Button.builder(Component.literal("<"), button -> cycle(-1))
            .bounds(centerX - 92, buttonY, 20, 20)
            .build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> cycle(1))
            .bounds(centerX + 72, buttonY, 20, 20)
            .build());
        addRenderableWidget(new HueSlider(centerX - 80, buttonY + 26, 160, 20));
        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
            .bounds(centerX - 40, buttonY + 56, 80, 20)
            .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);

        int left = this.width / 2 - 120;
        int top = this.height / 2 - 96;
        int right = this.width / 2 + 120;
        int bottom = this.height / 2 + 150;

        guiGraphics.fill(left - 3, top - 3, right + 3, bottom + 3, 0xAA050505);
        guiGraphics.fill(left, top, right, bottom, 0xF11B1412);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, top + 12, 0xFFE8C8);
        guiGraphics.drawCenteredString(this.font, Component.literal(DemonEyesResourceHelper.getLabel(currentIndex)), this.width / 2, top + 28, 0xFFF3E3);
        guiGraphics.drawCenteredString(this.font, Component.literal("Hue " + currentHue), this.width / 2, top + 42, 0xFFC9B7A5);

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player != null) {
            guiGraphics.fill(this.width / 2 - 54, top + 56, this.width / 2 + 54, top + 162, 0x55110C0A);
            InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, this.width / 2, top + 156, 48,
                this.width / 2 - mouseX, top + 116 - mouseY, player);
            guiGraphics.drawCenteredString(this.font, Component.literal("Live Preview"), this.width / 2, top + 168, 0xFFD4B58D);
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
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            DemonEyesClientState.setPlayerState(player.getUUID(), true, currentIndex, currentHue);
        }
        ModNetworking.sendToServer(new SetDemonEyesPacket(currentIndex, currentHue));
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
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            DemonEyesClientState.setPlayerState(player.getUUID(), true, currentIndex, currentHue);
        }
        ModNetworking.sendToServer(new SetDemonEyesPacket(currentIndex, currentHue));
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
