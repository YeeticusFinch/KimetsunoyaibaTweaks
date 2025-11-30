package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * Handles key input events for breathing techniques
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class KeyInputHandler {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                return;
            }

            // Only process on key press, not release or repeat
            if (event.getAction() != GLFW.GLFW_PRESS) {
                return;
            }

            // Don't process keybindings when a screen/GUI is open (chat, inventory, etc.)
            if (mc.screen != null) {
                return;
            }

            // Check if either keybinding was pressed
            boolean isForward = ModKeyBindings.CYCLE_BREATHING_FORM.matches(event.getKey(), event.getScanCode());
            boolean isBackward = ModKeyBindings.CYCLE_BREATHING_FORM_BACKWARD.matches(event.getKey(), event.getScanCode());

            if (!isForward && !isBackward) {
                return;
            }

            // Consume the click and process
            if ((isForward && ModKeyBindings.CYCLE_BREATHING_FORM.consumeClick()) ||
                (isBackward && ModKeyBindings.CYCLE_BREATHING_FORM_BACKWARD.consumeClick())) {

                ItemStack mainHandItem = mc.player.getItemInHand(InteractionHand.MAIN_HAND);

                // Check if holding a breathing sword
                if (mainHandItem.getItem() instanceof BreathingSwordItem breathingSword) {
                    breathingSword.cycleForm(mc.player, isBackward);
                }
            }
        } catch (Exception e) {
            // Silently catch exceptions to prevent crashes
        }
    }

    @SubscribeEvent
    public static void onMouseInput(InputEvent.MouseButton event) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            if (event.getAction() != GLFW.GLFW_PRESS) return;
            if (mc.screen != null) return;

            boolean isForward = ModKeyBindings.CYCLE_BREATHING_FORM.matchesMouse(event.getButton());
            boolean isBackward = ModKeyBindings.CYCLE_BREATHING_FORM_BACKWARD.matchesMouse(event.getButton());

            if (!isForward && !isBackward) return;

            ItemStack mainHandItem = mc.player.getItemInHand(InteractionHand.MAIN_HAND);
            if (mainHandItem.getItem() instanceof BreathingSwordItem breathingSword) {
                breathingSword.cycleForm(mc.player, isBackward);
            }
        } catch (Exception e) {
            // Silently catch exceptions to prevent crashes
        }
    }
}
