package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.effects.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client HUD and camera feedback for the Electrocuted effect. */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, value = Dist.CLIENT)
public final class ElectrocutedClientHandler {
    private static final ResourceLocation PLAYER_HEALTH_OVERLAY =
        ResourceLocation.fromNamespaceAndPath("minecraft", "player_health");
    private static final ResourceLocation FULL_ELECTROCUTED_HEART = heartTexture("full_electrocuted");
    private static final ResourceLocation HALF_ELECTROCUTED_HEART = heartTexture("half_electrocuted");
    private static final ResourceLocation EMPTY_ELECTROCUTED_HEART = heartTexture("empty_electrocuted");

    private ElectrocutedClientHandler() {
    }

    @SubscribeEvent
    public static void onRenderHealth(RenderGuiOverlayEvent.Post event) {
        if (!PLAYER_HEALTH_OVERLAY.equals(event.getOverlay().id())) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !player.hasEffect(ModEffects.ELECTROCUTED.get())) {
            return;
        }

        renderElectrocutedHearts(event.getGuiGraphics(), player);
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }

        var effect = player.getEffect(ModEffects.ELECTROCUTED.get());
        if (effect == null) {
            return;
        }

        float strength = 0.65F + Math.min(3, effect.getAmplifier()) * 0.2F;
        float time = player.tickCount + (float) event.getPartialTick();
        event.setYaw(event.getYaw() + strength * Mth.sin(time * 4.8F));
        event.setPitch(event.getPitch() + strength * Mth.cos(time * 5.7F));
        event.setRoll(event.getRoll() + strength * 1.4F * Mth.sin(time * 7.2F + 0.8F));
    }

    private static ResourceLocation heartTexture(String name) {
        return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
            "textures/gui/sprites/hud/heart/" + name + ".png");
    }

    private static void renderElectrocutedHearts(GuiGraphics guiGraphics, LocalPlayer player) {
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int heartCount = Mth.ceil(player.getMaxHealth() / 2.0F);
        int rows = Math.max(1, (heartCount + 9) / 10);
        int extraRows = Math.max(0, rows - 10);
        double health = player.getHealth();

        for (int heart = 0; heart < heartCount; heart++) {
            double heartStart = heart * 2.0D;
            int row = heart / 10;
            int x = screenWidth / 2 - 91 + (heart % 10) * 8;
            int y = screenHeight - 39 - row * 10 - extraRows * 10;
            ResourceLocation texture = health >= heartStart + 2.0D
                ? FULL_ELECTROCUTED_HEART
                : health > heartStart ? HALF_ELECTROCUTED_HEART : EMPTY_ELECTROCUTED_HEART;
            drawHeart(guiGraphics, texture, x, y);
        }
    }

    private static void drawHeart(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y) {
        guiGraphics.blit(texture, x, y, 9, 9, 0.0F, 0.0F, 36, 36, 36, 36);
    }
}
