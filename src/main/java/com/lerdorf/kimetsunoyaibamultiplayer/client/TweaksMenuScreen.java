package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.concurrent.ThreadLocalRandom;

public final class TweaksMenuScreen extends Screen {
    private static final int PHOTO_WIDTH = 2000;
    private static final int PHOTO_HEIGHT = 1125;
    private static final int PHOTO_COUNT = 5;
    private static final int MENU_LEFT = 28;
    private static final int MENU_TOP = 70;
    private static final int MENU_WIDTH = 250;
    private static final int BUTTON_HEIGHT = 24;
    private static final int BUTTON_GAP = 8;

    private final Screen parent;
    private final ResourceLocation background;

    public TweaksMenuScreen(Screen parent) {
        super(Component.literal("Kimetsunoyaiba Tweaks"));
        this.parent = parent;
        int photoIndex = ThreadLocalRandom.current().nextInt(PHOTO_COUNT);
        this.background = ResourceLocation.fromNamespaceAndPath(
            KimetsunoyaibaMultiplayer.MODID, "textures/gui/menu_photos/photo_" + photoIndex + ".png");
    }

    @Override
    protected void init() {
        int buttonY = MENU_TOP;
        addMenuButton("Config", button -> minecraft.setScreen(new ConfigMenuScreen(this)), buttonY);
        buttonY += BUTTON_HEIGHT + BUTTON_GAP;
        addMenuButton("Recommended Mods", button -> minecraft.setScreen(new RecommendedModsScreen(this)), buttonY);
        buttonY += BUTTON_HEIGHT + BUTTON_GAP;
        addMenuButton("KnY Servers", button -> minecraft.setScreen(new KnyServersScreen(this)), buttonY);
        buttonY += BUTTON_HEIGHT + BUTTON_GAP;
        addMenuButton("Wiki", button -> Util.getPlatform().openUri(
            "https://github.com/YeeticusFinch/KimetsunoyaibaTweaks/wiki"), buttonY);
        buttonY += BUTTON_HEIGHT + BUTTON_GAP + 8;
        addMenuButton("Back", button -> onClose(), buttonY, MENU_WIDTH / 4);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderPhotoBackground(guiGraphics);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(MENU_LEFT, 34.0F, 0.0F);
        guiGraphics.pose().scale(1.45F, 1.45F, 1.0F);
        guiGraphics.drawString(font, title, 0, 0, 0xFFF2D5A0, false);
        guiGraphics.pose().popPose();
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

    private void renderPhotoBackground(GuiGraphics guiGraphics) {
        guiGraphics.fill(0, 0, width, height, 0xFF08090D);
        float scale = Math.max(width / (float) PHOTO_WIDTH, height / (float) PHOTO_HEIGHT);
        float drawWidth = PHOTO_WIDTH * scale;
        float drawHeight = PHOTO_HEIGHT * scale;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate((width - drawWidth) / 2.0F, (height - drawHeight) / 2.0F, 0.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        guiGraphics.blit(background, 0, 0, 0, 0, PHOTO_WIDTH, PHOTO_HEIGHT, PHOTO_WIDTH, PHOTO_HEIGHT);
        guiGraphics.pose().popPose();
    }

    private void addMenuButton(String label, Button.OnPress onPress, int y) {
        addMenuButton(label, onPress, y, MENU_WIDTH / 2);
    }

    private void addMenuButton(String label, Button.OnPress onPress, int y, int buttonWidth) {
        addRenderableWidget(Button.builder(Component.literal(label), onPress)
            .bounds(MENU_LEFT, y, Math.min(buttonWidth, width - MENU_LEFT - 24), BUTTON_HEIGHT).build());
    }

}
