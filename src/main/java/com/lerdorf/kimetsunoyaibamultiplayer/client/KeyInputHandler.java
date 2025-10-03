package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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

            // Check if R key was pressed
            if (ModKeyBindings.CYCLE_BREATHING_FORM.consumeClick()) {
                ItemStack mainHandItem = mc.player.getItemInHand(InteractionHand.MAIN_HAND);

                // Check if holding a breathing sword
                if (mainHandItem.getItem() instanceof BreathingSwordItem breathingSword) {
                    breathingSword.cycleForm(mc.player);
                }
            }
        } catch (Exception e) {
            // Silently catch exceptions to prevent crashes
        }
    }
}
