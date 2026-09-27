package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TweaksClientEvents {
    private static final int LOGO_SIZE = 12;
    private static final ResourceLocation LOGO = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID, "textures/gui/tweaks_logo.png");

    private TweaksClientEvents() {
    }

    @SubscribeEvent
    public static void addPauseMenuButton(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof PauseScreen pauseScreen)) {
            return;
        }

        LogoButton button = new LogoButton(pauseScreen.width - 28, 8, 20, 20,
            Component.empty(), ignored ->
                Minecraft.getInstance().setScreen(new TweaksMenuScreen(pauseScreen)));
        button.setTooltip(Tooltip.create(Component.literal("Kimetsunoyaiba Tweaks")));
        event.addListener(button);
    }

    private static final class LogoButton extends Button {
        private LogoButton(int x, int y, int width, int height, Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
            int offset = (width - LOGO_SIZE) / 2;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(getX() + offset, getY() + offset, 0.0F);
            guiGraphics.pose().scale(LOGO_SIZE / 128.0F, LOGO_SIZE / 128.0F, 1.0F);
            guiGraphics.blit(LOGO, 0, 0, 0, 0, 128, 128, 128, 128);
            guiGraphics.pose().popPose();
        }
    }
}
