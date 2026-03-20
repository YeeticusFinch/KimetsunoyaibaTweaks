package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.effects.ModEffects;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "kimetsunoyaibamultiplayer", value = Dist.CLIENT)
public class BleedingOverlay {
    private static final Minecraft MC = Minecraft.getInstance();
    private static int flashTicksRemaining = 0;
    private static float flashStrength = 0.0F;

    private BleedingOverlay() {
    }

    public static void triggerFlash(int bleedingLevel) {
        flashTicksRemaining = Math.max(flashTicksRemaining, 6);
        flashStrength = Math.max(flashStrength, Math.min(0.14F + bleedingLevel * 0.05F, 0.42F));
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || flashTicksRemaining <= 0) {
            return;
        }

        flashTicksRemaining--;
        if (flashTicksRemaining <= 0) {
            flashStrength = 0.0F;
        }
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().toString().equals("minecraft:hotbar")) {
            return;
        }

        if (flashTicksRemaining <= 0) {
            return;
        }

        LocalPlayer player = MC.player;
        if (player == null || !player.hasEffect(ModEffects.BLEEDING.get())) {
            return;
        }

        renderFlash(event.getGuiGraphics(), flashStrength * (flashTicksRemaining / 6.0F));
    }

    private static void renderFlash(GuiGraphics guiGraphics, float alpha) {
        int screenWidth = MC.getWindow().getGuiScaledWidth();
        int screenHeight = MC.getWindow().getGuiScaledHeight();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferBuilder.vertex(matrix, 0, screenHeight, 0).color(0.75F, 0.0F, 0.0F, alpha).endVertex();
        bufferBuilder.vertex(matrix, screenWidth, screenHeight, 0).color(0.75F, 0.0F, 0.0F, alpha).endVertex();
        bufferBuilder.vertex(matrix, screenWidth, 0, 0).color(0.75F, 0.0F, 0.0F, alpha).endVertex();
        bufferBuilder.vertex(matrix, 0, 0, 0).color(0.75F, 0.0F, 0.0F, alpha).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());
        poseStack.popPose();

        RenderSystem.disableBlend();
    }
}
