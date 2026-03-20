package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "kimetsunoyaibamultiplayer", value = Dist.CLIENT)
public final class BlindfoldOverlay {

    private static final Minecraft MC = Minecraft.getInstance();

    private BlindfoldOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Pre event) {
        if (!event.getOverlay().id().toString().equals("minecraft:hotbar")) {
            return;
        }
        if (MC.screen != null) {
            return;
        }

        LocalPlayer player = MC.player;
        if (player == null) {
            return;
        }

        ItemStack headItem = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!headItem.is(ModItems.BLINDFOLD.get())) {
            return;
        }

        renderBlackout(event.getGuiGraphics());
    }

    private static void renderBlackout(GuiGraphics guiGraphics) {
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
        bufferBuilder.vertex(matrix, 0, screenHeight, 0).color(0.0F, 0.0F, 0.0F, 1.0F).endVertex();
        bufferBuilder.vertex(matrix, screenWidth, screenHeight, 0).color(0.0F, 0.0F, 0.0F, 1.0F).endVertex();
        bufferBuilder.vertex(matrix, screenWidth, 0, 0).color(0.0F, 0.0F, 0.0F, 1.0F).endVertex();
        bufferBuilder.vertex(matrix, 0, 0, 0).color(0.0F, 0.0F, 0.0F, 1.0F).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());

        poseStack.popPose();
        RenderSystem.disableBlend();
    }
}
