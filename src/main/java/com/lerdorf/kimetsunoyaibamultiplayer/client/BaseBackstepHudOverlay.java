package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.mcreator.kimetsunoyaiba.procedures.HaveTechniqueProcedure;
import net.mcreator.kimetsunoyaiba.procedures.OBackstepProcedure;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderGuiEvent;

public final class BaseBackstepHudOverlay {
    private static final ResourceLocation BACKSTEP_ICON =
            ResourceLocation.parse("kimetsunoyaiba:textures/screens/icon_backstep.png");

    private BaseBackstepHudOverlay() {
    }

    public static boolean shouldReplaceBaseOverlay() {
        return !Config.showBackstepHud || Config.backstepHudOffsetX != 0.0D || Config.backstepHudOffsetY != 0.0D;
    }

    public static void render(RenderGuiEvent.Pre event) {
        if (!Config.showBackstepHud) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || !HaveTechniqueProcedure.execute(player)) {
            return;
        }

        int screenWidth = event.getWindow().getGuiScaledWidth();
        int screenHeight = event.getWindow().getGuiScaledHeight();
        int x = screenWidth / 2 + (int) Math.round(screenWidth * Config.backstepHudOffsetX / 100.0D);
        int y = screenHeight / 2 + 73 + (int) Math.round(screenHeight * Config.backstepHudOffsetY / 100.0D);
        GuiGraphics guiGraphics = event.getGuiGraphics();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        guiGraphics.blit(BACKSTEP_ICON, x, y, 0.0F, 0.0F, 16, 16, 16, 16);
        guiGraphics.drawString(minecraft.font, OBackstepProcedure.execute(player), x + 14, y + 4, -1, false);

        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
