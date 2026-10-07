package com.lerdorf.kimetsunoyaibamultiplayer.client;

import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = "kimetsunoyaibamultiplayer",
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public final class StartupAnimationScreenEvents {
    private StartupAnimationScreenEvents() {
    }

    @SubscribeEvent
    public static void onLoadingScreenRender(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof LevelLoadingScreen
                || event.getScreen() instanceof ReceivingLevelScreen
                || event.getScreen() instanceof ProgressScreen
                || event.getScreen() instanceof GenericDirtMessageScreen) {
            StartupAnimationRenderer.render(event.getGuiGraphics());
        }
    }
}
