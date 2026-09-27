package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.config.ConfigEditorRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class TweaksMenuScreen extends Screen {
    private final Screen parent;

    public TweaksMenuScreen(Screen parent) {
        super(Component.literal("Kimetsunoyaiba Tweaks"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(430, width - 24);
        int left = (width - panelWidth) / 2;
        int top = panelTop();
        int buttonY = top + 72;

        addRenderableWidget(Button.builder(Component.literal("Client Config"), button ->
                minecraft.setScreen(new TweaksConfigScreen(this, ConfigEditorRegistry.EditorTab.CLIENT)))
            .bounds(left + 35, buttonY, panelWidth - 70, 24).build());
        buttonY += 34;

        if (hasServerAccess()) {
            addRenderableWidget(Button.builder(Component.literal("Server Config"), button ->
                    minecraft.setScreen(new TweaksConfigScreen(this, ConfigEditorRegistry.EditorTab.SERVER)))
                .bounds(left + 35, buttonY, panelWidth - 70, 24).build());
            buttonY += 34;
        }

        if (hasKnyWorldsConfig()) {
            addRenderableWidget(Button.builder(Component.literal("KnY Worlds Config"), button ->
                    minecraft.setScreen(new TweaksConfigScreen(this, ConfigEditorRegistry.EditorTab.KNY_WORLDS)))
                .bounds(left + 35, buttonY, panelWidth - 70, 24).build());
            buttonY += 34;
        }

        addRenderableWidget(Button.builder(Component.literal("Shimmer"), button ->
                minecraft.setScreen(new ShimmerScreen(this)))
            .bounds(left + 35, buttonY, panelWidth - 70, 24).build());
        buttonY += 34;

        addRenderableWidget(Button.builder(Component.literal("Back"), button -> onClose())
            .bounds(left + 35, buttonY, panelWidth - 70, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        int panelWidth = Math.min(430, width - 24);
        int left = (width - panelWidth) / 2;
        int top = panelTop();
        int panelHeight = panelHeight();
        guiGraphics.fill(left - 4, top - 4, left + panelWidth + 4, top + panelHeight + 4, 0xAA08090D);
        guiGraphics.fill(left, top, left + panelWidth, top + panelHeight, 0xF01C1720);
        guiGraphics.fill(left + 2, top + 2, left + panelWidth - 2, top + panelHeight - 2, 0xF52A2028);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(width / 2.0F, top + 22.0F, 0.0F);
        guiGraphics.pose().scale(1.55F, 1.55F, 1.0F);
        guiGraphics.drawCenteredString(font, title, 0, 0, 0xFFF2D5A0);
        guiGraphics.pose().popPose();
        guiGraphics.drawCenteredString(font, Component.literal("Configure your Kimetsunoyaiba experience"),
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

    private boolean hasServerAccess() {
        return minecraft != null && minecraft.player != null && minecraft.player.hasPermissions(2)
            && !ConfigEditorRegistry.specs(ConfigEditorRegistry.EditorTab.SERVER).isEmpty();
    }

    private boolean hasKnyWorldsConfig() {
        return !ConfigEditorRegistry.specs(ConfigEditorRegistry.EditorTab.KNY_WORLDS).isEmpty();
    }

    private int panelHeight() {
        return hasServerAccess() ? 234 : 200;
    }

    private int panelTop() {
        return Math.max(24, (height - panelHeight() - 60) / 2);
    }
}
