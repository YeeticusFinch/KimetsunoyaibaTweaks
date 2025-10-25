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
    private static final long CYCLE_COOLDOWN_MS = 200; // 200ms cooldown between cycles

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        // Only handle R key (GLFW.GLFW_KEY_R = 82)
        if (event.getKey() != GLFW.GLFW_KEY_R) {
            return;
        }

        // Only handle key press, not release
        if (event.getAction() != GLFW.GLFW_PRESS) {
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

        // Check if player has a breathing form active
        if (player.getPersistentData().getDouble("breathes") == 0.0) {
            return;
        }

        // Check if shift is held for backward cycling
        boolean shiftHeld = mc.options.keyShift.isDown();

        if (shiftHeld) {
            // Backward cycling with Shift+R
            if (Config.logDebug) {
                Log.debug("Shift+R pressed - cycling backward");
            }

            // Send packet to server to cycle backward TWICE (once to go back, once to counteract kimetsunoyaiba's forward)
            ModNetworking.sendToServer(new CycleBreathingFormPacket(-2)); // -2 = backward twice

            handledShiftR = true;
        } else {
            // Forward cycling with R - let kimetsunoyaiba mod handle it normally
            if (Config.logDebug) {
                Log.debug("R pressed - cycling forward (handled by kimetsunoyaiba mod)");
            }
            handledShiftR = false;
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
