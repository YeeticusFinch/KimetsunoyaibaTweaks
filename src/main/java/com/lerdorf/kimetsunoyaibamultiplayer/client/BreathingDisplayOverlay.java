package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.util.BreathingInfoDetector;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Client-side overlay that displays the current breathing technique and form
 * when the player is holding a nichirin sword.
 */
@OnlyIn(Dist.CLIENT)
public class BreathingDisplayOverlay {

    private static final Minecraft mc = Minecraft.getInstance();

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        // Only render if config is enabled
        if (!Config.showBreathingDisplay) {
            return;
        }

        // Only render on the main HUD layer (not on other overlays)
        if (!event.getOverlay().id().toString().equals("minecraft:hotbar")) {
            return;
        }

        Player player = mc.player;
        if (player == null) {
            return;
        }

        // Get the item in the player's main hand
        ItemStack heldItem = player.getMainHandItem();

        // Check if it's a nichirin sword
        if (!BreathingInfoDetector.isNichirinSword(heldItem)) {
            return;
        }

        // Get breathing information
        BreathingInfoDetector.BreathingInfo info = BreathingInfoDetector.getBreathingInfo(player, heldItem);
        if (info == null) {
            return;
        }

        // Render the breathing info on screen
        renderBreathingInfo(event.getGuiGraphics().pose(), info, event.getGuiGraphics());
    }

    /**
     * Renders the breathing technique and form display on screen.
     */
    private static void renderBreathingInfo(PoseStack poseStack, BreathingInfoDetector.BreathingInfo info, net.minecraft.client.gui.GuiGraphics guiGraphics) {
        Font font = mc.font;

        // Create the display component with color codes
        Component displayText = Component.literal(info.getColoredDisplay());

        // Apply scaling
        float scale = (float) Config.breathingDisplayScale;
        poseStack.pushPose();
        poseStack.scale(scale, scale, 1.0f);

        // Calculate position based on config (adjusted for scale)
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int textWidth = (int)(font.width(displayText) * scale);
        int textHeight = (int)(font.lineHeight * scale);

        int x, y;
        int margin = 5; // Margin from screen edges

        switch (Config.breathingDisplayPosition) {
            case TOP_LEFT:
                x = (int)(margin / scale);
                y = (int)(margin / scale);
                break;
            case TOP_RIGHT:
                x = (int)((screenWidth - textWidth - margin) / scale);
                y = (int)(margin / scale);
                break;
            case BOTTOM_LEFT:
                x = (int)(margin / scale);
                y = (int)((screenHeight - textHeight - margin) / scale);
                break;
            case BOTTOM_RIGHT:
                x = (int)((screenWidth - textWidth - margin) / scale);
                y = (int)((screenHeight - textHeight - margin) / scale);
                break;
            case CENTER_BELOW_CROSSHAIR:
            default:
                x = (int)((screenWidth - textWidth) / 2 / scale);
                y = (int)((screenHeight / 2 + 20) / scale); // 20 pixels below center
                break;
        }

        // Draw the text with shadow for better visibility
        guiGraphics.drawString(font, displayText, x, y, 0xFFFFFF, true);

        poseStack.popPose();
    }
}
