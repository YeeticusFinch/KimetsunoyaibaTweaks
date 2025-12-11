package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.CycleBreathingFormPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.util.BreathingInfoDetector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Handles breathing form cycling with R key (forward) and Shift+R (backward).
 * This intercepts the kimetsunoyaiba mod's R key to add backward cycling functionality.
 */
@OnlyIn(Dist.CLIENT)
public class BreathingFormCycleHandler {

    private static boolean handledShiftR = false;
    private static long lastCycleTime = 0;
    private static final long CYCLE_COOLDOWN_MS = 100; // 100ms cooldown between cycles

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        // Only handle key press, not release
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }

        // Don't process keybindings when a screen/GUI is open (chat, inventory, etc.)
        if (mc.screen != null) {
            return;
        }
        
        //boolean forwardKey = ModKeyBindings.CYCLE_BREATHING_FORM.matches(event.getKey(), event.getScanCode());
        boolean forwardKey = net.mcreator.kimetsunoyaiba.init.KimetsunoyaibaModKeyMappings.CHANGE_BREATHES_AND_BLOOD_ART.matches(event.getKey(), event.getScanCode());
        boolean reverseKey = ModKeyBindings.CYCLE_BREATHING_FORM_BACKWARD.matches(event.getKey(), event.getScanCode());

        // Only handle R key (GLFW.GLFW_KEY_R = 82)
        if (!forwardKey && !reverseKey) {
            return;
        }

        // Check cooldown to prevent rapid cycling
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCycleTime < CYCLE_COOLDOWN_MS) {
            if (Config.logDebug) {
                Log.debug("Form cycling on cooldown, ignoring key press");
            }
            return;
        }
        lastCycleTime = currentTime;

        LocalPlayer player = mc.player;
        ItemStack heldItem = player.getMainHandItem();

        // Only handle if holding a nichirin sword
        if (!BreathingInfoDetector.isNichirinSword(heldItem)) {
            return;
        }

        // Check if this is a custom breathing sword (our API)
        boolean isCustomBreathingSword = heldItem.getItem() instanceof com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem;

        // For custom breathing swords, we handle ALL cycling (both forward and backward)
        // For base mod swords, only handle backward cycling
        if (isCustomBreathingSword) {
            // Custom breathing sword - send packet to server for BOTH directions
        	// Currently handled in KeyInputHandler
/*
            if (reverseKey) {
                // Backward cycling
                if (Config.logDebug) {
                    Log.debug("Reverse cycle pressed on custom sword - cycling backward");
                }
                ModNetworking.sendToServer(new CycleBreathingFormPacket(-1));
                handledShiftR = true;
            } else {
                // Forward cycling
                if (Config.logDebug) {
                    Log.debug("Forward cycle pressed on custom sword - cycling forward");
                }
                ModNetworking.sendToServer(new CycleBreathingFormPacket(1));
                handledShiftR = false;
            }*/
        } else {
            // Base mod sword - check if player has a breathing form active
            if (player.getPersistentData().getDouble("breathes") == 0.0) {
                return;
            }

            // Check if shift is held for backward cycling

            if (reverseKey) {
                // Backward cycling with Shift+R
                if (Config.logDebug) {
                    Log.debug("Reverse cycle pressed on base mod sword - cycling backward");
                }

                // Send packet to server to cycle backward TWICE (once to go back, once to counteract kimetsunoyaiba's forward)
                ModNetworking.sendToServer(new CycleBreathingFormPacket(-2)); // -2 = backward twice

                handledShiftR = true;
            } else {
                // Forward cycling with R - let kimetsunoyaiba mod handle it normally
                if (Config.logDebug) {
                    Log.debug("Forward cycle pressed on base mod sword - letting base mod handle it");
                }
                handledShiftR = false;
            }
        }
    }

    /**
     * Returns true if we just handled a Shift+R press.
     * This is used to prevent double-cycling.
     */
    public static boolean justHandledShiftR() {
        boolean result = handledShiftR;
        handledShiftR = false; // Reset after checking
        return result;
    }
}