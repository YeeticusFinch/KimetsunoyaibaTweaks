package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.items.BloodDemonArtAxeItem;
import com.lerdorf.kimetsunoyaibamultiplayer.items.BloodDemonArtItem;
import com.lerdorf.kimetsunoyaibamultiplayer.items.CustomDemonArtItem;
import com.lerdorf.kimetsunoyaibamultiplayer.util.BreathingInfoDetector;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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

        if (heldItem.getItem() instanceof CustomDemonArtItem) {
            MutableComponent customDisplay = Component.literal(CustomDemonArtItem.getDisplayText(heldItem))
                .withStyle(style -> style.withColor(CustomDemonArtItem.getDisplayColor(heldItem)));
            renderDisplayText(event.getGuiGraphics().pose(), customDisplay, event.getGuiGraphics());
            return;
        }

        if (heldItem.getItem() instanceof BloodDemonArtItem artItem) {
            renderBloodDemonArtItem(event, artItem.getDisplayText(heldItem), artItem.getArtId());
            return;
        }

        if (heldItem.getItem() instanceof BloodDemonArtAxeItem artItem) {
            renderBloodDemonArtItem(event, artItem.getDisplayText(heldItem), artItem.getArtId());
            return;
        }

        BreathingInfoDetector.BreathingInfo info = BreathingInfoDetector.getBreathingInfo(player, heldItem);
        if (info == null) {
            return;
        }

        // Render the breathing info on screen
        renderBreathingInfo(event.getGuiGraphics().pose(), info, event.getGuiGraphics());
    }

    private static void renderBloodDemonArtItem(RenderGuiOverlayEvent.Post event, String displayText, String artId) {
        if (displayText == null || displayText.isBlank()) {
            return;
        }

        MutableComponent display = Component.literal(displayText)
            .withStyle(style -> style.withColor(getBloodDemonArtDisplayColor(artId)));
        renderDisplayText(event.getGuiGraphics().pose(), display, event.getGuiGraphics());
    }

    private static int getBloodDemonArtDisplayColor(String artId) {
        BloodDemonArtRegistry.RegisteredBloodDemonArt art = BloodDemonArtRegistry.getArt(artId);
        if (art == null || art.getTechnique() == null) {
            return 0xAA1E2F;
        }
        return art.getTechnique().getDisplayColor();
    }

    /**
     * Renders the breathing technique and form display on screen.
     */
    private static void renderBreathingInfo(PoseStack poseStack, BreathingInfoDetector.BreathingInfo info, net.minecraft.client.gui.GuiGraphics guiGraphics) {
        // Create the display component with color codes
        String displayString = info.getColoredDisplay();

        // If variations are available, append the count: (current/total)
        // currentVariationIndex: 0 = base (shows as 1/N), 1 = first variation (shows as 2/N), etc.
        if (info.totalVariations > 0) {
            int currentPosition = info.currentVariationIndex + 1; // +1 because 0 = base form (position 1)
            int totalPositions = info.totalVariations + 1; // +1 to include base form in count
            displayString += " §8(" + currentPosition + "/" + totalPositions + ")";
        }

        // If config is enabled, show the raw breathes value for debugging
        if (Config.showBreathesValue) {
            displayString += " §7[Breathes: " + String.format("%.1f", info.fullBreathesValue) + "]";
        }

        Component displayText = Component.literal(displayString);
        renderDisplayText(poseStack, displayText, guiGraphics);
    }

    private static void renderDisplayText(PoseStack poseStack, Component displayText, net.minecraft.client.gui.GuiGraphics guiGraphics) {
        Font font = mc.font;
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
