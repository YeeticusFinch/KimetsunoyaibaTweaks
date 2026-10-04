package com.lerdorf.kimetsunoyaibamultiplayer.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class RecommendedModsScreen extends Screen {
    private final Screen parent;

    public RecommendedModsScreen(Screen parent) {
        super(Component.literal("Recommended Mods"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = panelLeft();
        int top = panelTop();
        addRenderableWidget(Button.builder(Component.literal("Shimmer"), button ->
                minecraft.setScreen(new ShimmerScreen(this)))
            .bounds(left + 35, top + 72, panelWidth() - 70, 24).build());
        addRenderableWidget(Button.builder(Component.literal("Gravity Control"), button ->
                minecraft.setScreen(new GravityControlScreen(this)))
            .bounds(left + 35, top + 106, panelWidth() - 70, 24).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), button -> onClose())
            .bounds(left + 35, top + 150, panelWidth() - 70, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        int left = panelLeft();
        int top = panelTop();
        int right = left + panelWidth();
        int bottom = top + 176;
        guiGraphics.fill(left - 4, top - 4, right + 4, bottom + 4, 0xAA08090D);
        guiGraphics.fill(left, top, right, bottom, 0xF01C1720);
        guiGraphics.fill(left + 2, top + 2, right - 2, bottom - 2, 0xF52A2028);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(width / 2.0F, top + 22.0F, 0.0F);
        guiGraphics.pose().scale(1.55F, 1.55F, 1.0F);
        guiGraphics.drawCenteredString(font, title, 0, 0, 0xFFF2D5A0);
        guiGraphics.pose().popPose();
        guiGraphics.drawCenteredString(font, Component.literal("Mods that complement Kimetsunoyaiba"),
            width / 2, top + 44, 0xFFC8B9AC);
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

    private int panelWidth() {
        return Math.min(430, width - 24);
    }

    private int panelLeft() {
        return (width - panelWidth()) / 2;
    }

    private int panelTop() {
        return Math.max(24, (height - 176) / 2);
    }
}
