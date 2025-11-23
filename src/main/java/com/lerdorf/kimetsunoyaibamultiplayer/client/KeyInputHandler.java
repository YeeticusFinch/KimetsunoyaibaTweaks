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

            // Check if the pressed key matches our keybinding
            if (!ModKeyBindings.CYCLE_BREATHING_FORM.matches(event.getKey(), event.getScanCode())) {
                return;
            }

            // Consume the click and process
            if (ModKeyBindings.CYCLE_BREATHING_FORM.consumeClick()) {
                ItemStack mainHandItem = mc.player.getItemInHand(InteractionHand.MAIN_HAND);

                // Check if holding a breathing sword
                if (mainHandItem.getItem() instanceof BreathingSwordItem breathingSword) {
                    // Check if shift is held for backward cycling
                    boolean shiftHeld = mc.options.keyShift.isDown();
                    breathingSword.cycleForm(mc.player, shiftHeld);
                }
            }
        } catch (Exception e) {
            // Silently catch exceptions to prevent crashes
        }
    }
}
