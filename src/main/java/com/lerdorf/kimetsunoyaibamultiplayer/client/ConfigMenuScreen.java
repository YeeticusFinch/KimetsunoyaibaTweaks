package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModList;

public final class ConfigMenuScreen extends Screen {
    private static final String CONFIGURED_MOD_ID = "configured";
    private static final String TWEAKS_MOD_ID = "kimetsunoyaibamultiplayer";
    private static final String KNY_WORLDS_MOD_ID = "kny_worlds";
    private static final String CONFIGURED_URL =
        "https://www.curseforge.com/minecraft/mc-mods/configured/files/all?page=1&pageSize=20&version=1.20.1&gameVersionTypeId=1&showAlphaFiles=hide";

    private final Screen parent;
    private final boolean configuredInstalled;
    private Button enhancedCycleButton;

    public ConfigMenuScreen(Screen parent) {
        super(Component.literal("Config"));
        this.parent = parent;
        this.configuredInstalled = ModList.get().isLoaded(CONFIGURED_MOD_ID);
    }

    @Override
    protected void init() {
        int left = panelLeft();
        int top = panelTop();
        int buttonY = top + 76;

        addRenderableWidget(Button.builder(Component.literal("Config Presets"), button ->
                minecraft.setScreen(new ConfigPresetsScreen(this)))
            .bounds(left + 35, buttonY, panelWidth() - 70, 24).build());
        buttonY += 30;

        enhancedCycleButton = addRenderableWidget(Button.builder(enhancedCycleButtonText(), button -> toggleEnhancedCycle())
            .bounds(left + 35, buttonY, panelWidth() - 70, 24).build());
        buttonY += 30;

        Button tweaksButton = Button.builder(Component.literal("Edit Tweaks Config"), button ->
                openConfiguredConfig(TWEAKS_MOD_ID))
            .bounds(left + 35, buttonY, panelWidth() - 70, 24).build();
        tweaksButton.active = configuredInstalled;
        addRenderableWidget(tweaksButton);
        buttonY += 30;

        Button worldsButton = Button.builder(Component.literal("Edit KnY Worlds Config"), button ->
                openConfiguredConfig(KNY_WORLDS_MOD_ID))
            .bounds(left + 35, buttonY, panelWidth() - 70, 24).build();
        worldsButton.active = configuredInstalled && ModList.get().isLoaded(KNY_WORLDS_MOD_ID);
        addRenderableWidget(worldsButton);
        buttonY += 30;

        if (!configuredInstalled) {
            addRenderableWidget(Button.builder(Component.literal("Get Configured"), button -> Util.getPlatform().openUri(CONFIGURED_URL))
                .bounds(left + 35, buttonY, panelWidth() - 70, 24).build());
            buttonY += 30;
        }

        addRenderableWidget(Button.builder(Component.literal("Back"), button -> onClose())
            .bounds(left + 35, buttonY, panelWidth() - 70, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        int left = panelLeft();
        int top = panelTop();
        int panelHeight = panelHeight();
        guiGraphics.fill(left - 4, top - 4, left + panelWidth() + 4, top + panelHeight + 4, 0xAA08090D);
        guiGraphics.fill(left, top, left + panelWidth(), top + panelHeight, 0xF01C1720);
        guiGraphics.fill(left + 2, top + 2, left + panelWidth() - 2, top + panelHeight - 2, 0xF52A2028);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(width / 2.0F, top + 22.0F, 0.0F);
        guiGraphics.pose().scale(1.55F, 1.55F, 1.0F);
        guiGraphics.drawCenteredString(font, title, 0, 0, 0xFFF2D5A0);
        guiGraphics.pose().popPose();
        guiGraphics.drawCenteredString(font, Component.literal(configuredInstalled
                ? "Choose a preset or edit a mod through Configured"
                : "Install Configured to edit mod settings in-game"),
            width / 2, top + 44, 0xFFC8B9AC);
        if (enhancedCycleButton != null) {
            enhancedCycleButton.setMessage(enhancedCycleButtonText());
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

    private void openConfiguredConfig(String modId) {
        ModList.get().getModContainerById(modId)
            .flatMap(container -> ConfigScreenHandler.getScreenFactoryFor(container.getModInfo()))
            .ifPresentOrElse(factory -> minecraft.setScreen(factory.apply(minecraft, this)),
                () -> System.err.println("[Kimetsunoyaiba Tweaks] Configured has no screen for " + modId));
    }

    private int panelWidth() {
        return Math.min(430, width - 24);
    }

    private int panelHeight() {
        int buttonCount = configuredInstalled ? 5 : 6;
        return 80 + buttonCount * 30;
    }

    private int panelLeft() {
        return (width - panelWidth()) / 2;
    }

    private int panelTop() {
        return Math.max(8, (height - panelHeight()) / 2);
    }

    private Component enhancedCycleButtonText() {
        boolean enabled = Config.isEnhancedFormCycleEnabled();
        return Component.literal("Ability Cycle Wheel HUD: ")
            .append(Component.literal(enabled ? "ON" : "OFF")
                .withStyle(style -> style.withColor(enabled ? 0x55FF55 : 0xFF5555)));
    }

    private void toggleEnhancedCycle() {
        Config.setEnhancedFormCycle(!Config.isEnhancedFormCycleEnabled());
    }
}
