package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.compat.GravityApiCompat;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class GravityControlScreen extends Screen {
    private static final String MOD_URL = "https://www.curseforge.com/minecraft/mc-mods/gravity-control";

    private final Screen parent;
    private final boolean gravityControlInstalled;

    public GravityControlScreen(Screen parent) {
        super(Component.literal("Gravity Control"));
        this.parent = parent;
        this.gravityControlInstalled = GravityApiCompat.isLoaded();
    }

    @Override
    protected void init() {
        int left = panelLeft();
        int top = panelTop();
        int panelWidth = panelWidth();
        addRenderableWidget(Button.builder(Component.literal("Open Gravity Control Page"), button ->
                Util.getPlatform().openUri(MOD_URL))
            .bounds(left + 24, top + 112, panelWidth - 48, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), button -> onClose())
            .bounds(left + 24, top + 146, panelWidth - 48, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        int left = panelLeft();
        int top = panelTop();
        int right = left + panelWidth();
        int bottom = top + 180;

        guiGraphics.fill(left - 4, top - 4, right + 4, bottom + 4, 0xAA08090D);
        guiGraphics.fill(left, top, right, bottom, 0xF01C1720);
        guiGraphics.fill(left + 2, top + 2, right - 2, bottom - 2, 0xF52A2028);
        guiGraphics.drawCenteredString(font, title, width / 2, top + 22, 0xFFF2D5A0);
        guiGraphics.drawCenteredString(font, Component.literal("Gravity integration status"),
            width / 2, top + 46, 0xFFC8B9AC);

        guiGraphics.fill(left + 24, top + 64, right - 24, top + 96, 0xFF211B22);
        guiGraphics.drawCenteredString(font, Component.literal(statusText()), width / 2, top + 76,
            gravityControlInstalled ? 0xFFB9E1B3 : 0xFFE6C887);
        guiGraphics.drawCenteredString(font, Component.literal("Supports Gravity API and Gravity Changer."),
            width / 2, top + 100, 0xFF9B8E88);
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

    private String statusText() {
        return gravityControlInstalled ? "gravity control installed" : "gravity control not installed";
    }

    private int panelWidth() {
        return Math.min(430, width - 24);
    }

    private int panelLeft() {
        return (width - panelWidth()) / 2;
    }

    private int panelTop() {
        return Math.max(24, (height - 180) / 2);
    }
}
