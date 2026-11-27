package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ArachnidMandiblesAndEyesItem;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ArachnidEyesItem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Applies the same post-processing effect used by vanilla spider spectator vision. */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, value = Dist.CLIENT)
public final class ArachnidEyesClientHandler {
    private static final ResourceLocation SPIDER_SHADER =
        ResourceLocation.fromNamespaceAndPath("minecraft", "shaders/post/spider.json");

    private static boolean shaderLoaded;

    private ArachnidEyesClientHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        boolean active = minecraft.player != null
            && minecraft.level != null
            && (ArachnidEyesItem.isEquipped(minecraft.player)
                || ArachnidMandiblesAndEyesItem.isEquipped(minecraft.player));

        if (active && !shaderLoaded) {
            try {
                minecraft.gameRenderer.loadEffect(SPIDER_SHADER);
                shaderLoaded = true;
            } catch (Exception exception) {
                System.err.println("Failed to load vanilla spider vision shader: " + exception.getMessage());
            }
        } else if (!active && shaderLoaded) {
            minecraft.gameRenderer.shutdownEffect();
            shaderLoaded = false;
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (shaderLoaded) {
            minecraft.gameRenderer.shutdownEffect();
            shaderLoaded = false;
        }
    }
}
