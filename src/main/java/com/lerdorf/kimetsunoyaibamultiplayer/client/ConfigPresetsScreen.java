package com.lerdorf.kimetsunoyaibamultiplayer.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public final class ConfigPresetsScreen extends Screen {
    private static final int LIST_TOP = 62;
    private static final int FOOTER_HEIGHT = 30;
    private static final int CARD_HEIGHT = 132;
    private static final int CARD_GAP = 12;
    private static final int IMAGE_HEIGHT = 76;
    private static final List<ConfigPresetManager.Preset> PRESETS = ConfigPresetManager.presets();

    private final Screen parent;
    private final List<Button> presetButtons;
    private int scroll;
    private int maxScroll;
    private String status = "Applying a preset resets all known Tweaks and KnY Worlds values first.";
    private int statusColor = 0xFFC8B9AC;

    public ConfigPresetsScreen(Screen parent) {
        super(Component.literal("Config Presets"));
        this.parent = parent;
        this.presetButtons = PRESETS.stream().map(preset -> Button.builder(Component.literal("Apply Preset"),
            button -> confirmPreset(preset)).bounds(0, -100, 110, 20).build()).toList();
    }

    @Override
    protected void init() {
        for (Button button : presetButtons) {
            addRenderableWidget(button);
        }
        addRenderableWidget(Button.builder(Component.literal("Back"), button -> onClose())
            .bounds(width / 2 - 55, height - 25, 110, 20).build());
        updateScrollBounds();
        repositionWidgets();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        int left = panelLeft();
        int right = panelRight();
        int listBottom = height - FOOTER_HEIGHT;
        updateScrollBounds();
        repositionWidgets();

        guiGraphics.fill(left - 4, 23, right + 4, height - 4, 0xAA08090D);
        guiGraphics.fill(left, 27, right, height - 8, 0xF01C1720);
        guiGraphics.fill(left + 2, 29, right - 2, height - 10, 0xF52A2028);
        guiGraphics.drawCenteredString(font, title, width / 2, 33, 0xFFF2D5A0);
        guiGraphics.drawCenteredString(font, Component.literal("Presets affect both mods and overwrite current values"),
            width / 2, 48, 0xFFC8B9AC);

        guiGraphics.enableScissor(left + 8, LIST_TOP, right - 8, listBottom);
        for (int i = 0; i < PRESETS.size(); i++) {
            int cardY = LIST_TOP + i * (CARD_HEIGHT + CARD_GAP) - scroll;
            renderCard(guiGraphics, PRESETS.get(i), left + 10, cardY, right - left - 20);
        }
        guiGraphics.disableScissor();

        if (maxScroll > 0) {
            int trackTop = LIST_TOP + 2;
            int trackHeight = Math.max(20, listBottom - LIST_TOP - 4);
            int contentHeight = PRESETS.size() * CARD_HEIGHT + (PRESETS.size() - 1) * CARD_GAP;
            int thumbHeight = Math.max(16, trackHeight * trackHeight / contentHeight);
            int thumbY = trackTop + (trackHeight - thumbHeight) * scroll / maxScroll;
            guiGraphics.fill(right - 7, trackTop, right - 4, trackTop + trackHeight, 0xFF1A151B);
            guiGraphics.fill(right - 7, thumbY, right - 4, thumbY + thumbHeight, 0xFFD1A267);
        }
        guiGraphics.drawCenteredString(font, Component.literal(status), width / 2, height - 42, statusColor);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.signum(delta) * 30));
        repositionWidgets();
        return true;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    void applyPreset(ConfigPresetManager.Preset preset) {
        ConfigPresetManager.ApplyResult result = ConfigPresetManager.apply(preset);
        status = result.applied() + " values applied" + (result.skipped() == 0 ? "." : "; " + result.skipped() + " skipped.");
        statusColor = result.skipped() == 0 ? 0xFFB9E1B3 : 0xFFE6C887;
    }

    private void confirmPreset(ConfigPresetManager.Preset preset) {
        minecraft.setScreen(new ConfigPresetConfirmationScreen(this, preset));
    }

    private void renderCard(GuiGraphics guiGraphics, ConfigPresetManager.Preset preset, int x, int y, int cardWidth) {
        int right = x + cardWidth;
        guiGraphics.fill(x, y, right, y + CARD_HEIGHT, 0xE52A2028);
        guiGraphics.fill(x + 2, y + 2, right - 2, y + CARD_HEIGHT - 2, 0xFF342930);

        int imageWidth = Math.min(150, Math.max(88, cardWidth / 3));
        drawTextureFit(guiGraphics, preset.image(), x + 10, y + 10, imageWidth, IMAGE_HEIGHT,
            preset.imageWidth(), preset.imageHeight());

        int textX = x + imageWidth + 24;
        int textWidth = Math.max(80, right - textX - 12);
        guiGraphics.drawString(font, Component.literal(preset.name()), textX, y + 13, 0xFFF0D5A7, false);
        List<FormattedCharSequence> description = font.split(Component.literal(preset.description()), textWidth);
        for (int i = 0; i < Math.min(6, description.size()); i++) {
            guiGraphics.drawString(font, description.get(i), textX, y + 32 + i * 11, 0xFFB5A6A0);
        }
    }

    private void drawTextureFit(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y,
                                int targetWidth, int targetHeight, int textureWidth, int textureHeight) {
        float scale = Math.min(targetWidth / (float) textureWidth, targetHeight / (float) textureHeight);
        float drawWidth = textureWidth * scale;
        float drawHeight = textureHeight * scale;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x + (targetWidth - drawWidth) / 2.0F,
            y + (targetHeight - drawHeight) / 2.0F, 0.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        guiGraphics.blit(texture, 0, 0, 0, 0, textureWidth, textureHeight, textureWidth, textureHeight);
        guiGraphics.pose().popPose();
    }

    private void repositionWidgets() {
        int left = panelLeft();
        int right = panelRight();
        int listBottom = height - FOOTER_HEIGHT;
        int cardWidth = right - left - 20;
        int imageWidth = Math.min(150, Math.max(88, cardWidth / 3));
        int buttonX = left + 10 + imageWidth + 24;
        int buttonWidth = Math.max(80, Math.min(115, right - buttonX - 12));
        for (int i = 0; i < presetButtons.size(); i++) {
            int cardY = LIST_TOP + i * (CARD_HEIGHT + CARD_GAP) - scroll;
            int buttonY = cardY + CARD_HEIGHT - 26;
            boolean visible = buttonY + 20 >= LIST_TOP && buttonY <= listBottom;
            presetButtons.get(i).setX(buttonX);
            presetButtons.get(i).setY(visible ? buttonY : -100);
            presetButtons.get(i).setWidth(buttonWidth);
        }
    }

    private void updateScrollBounds() {
        int listHeight = Math.max(0, height - FOOTER_HEIGHT - LIST_TOP);
        int contentHeight = PRESETS.size() * CARD_HEIGHT + (PRESETS.size() - 1) * CARD_GAP;
        maxScroll = Math.max(0, contentHeight - listHeight);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
    }

    private int panelLeft() {
        return Math.max(8, (width - Math.min(820, width - 16)) / 2);
    }

    private int panelRight() {
        return Math.min(width - 8, panelLeft() + Math.min(820, width - 16));
    }
}
