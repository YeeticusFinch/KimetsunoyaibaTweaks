package com.lerdorf.kimetsunoyaibamultiplayer.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

final class ConfigPresetConfirmationScreen extends Screen {
    private final ConfigPresetsScreen parent;
    private final ConfigPresetManager.Preset preset;

    ConfigPresetConfirmationScreen(ConfigPresetsScreen parent, ConfigPresetManager.Preset preset) {
        super(Component.literal("Apply Config Preset"));
        this.parent = parent;
        this.preset = preset;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal("Apply"), button -> {
            parent.applyPreset(preset);
            minecraft.setScreen(parent);
        }).bounds(width / 2 - 110, height / 2 + 38, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> minecraft.setScreen(parent))
            .bounds(width / 2 + 10, height / 2 + 38, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        int left = Math.max(18, width / 2 - 215);
        int right = Math.min(width - 18, width / 2 + 215);
        int top = Math.max(30, height / 2 - 72);
        int bottom = Math.min(height - 30, height / 2 + 74);
        guiGraphics.fill(left - 4, top - 4, right + 4, bottom + 4, 0xDD08090D);
        guiGraphics.fill(left, top, right, bottom, 0xF01C1720);
        guiGraphics.fill(left + 2, top + 2, right - 2, bottom - 2, 0xF52A2028);
        guiGraphics.drawCenteredString(font, Component.literal("Apply " + preset.name() + "?"), width / 2, top + 18, 0xFFF2D5A0);
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(
            Component.literal("This will overwrite your current config for Tweaks and KnY Worlds. Are you sure you want to continue?"),
            right - left - 30);
        for (int i = 0; i < lines.size(); i++) {
            guiGraphics.drawCenteredString(font, lines.get(i), width / 2, top + 42 + i * 12, 0xFFE0CFC0);
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
