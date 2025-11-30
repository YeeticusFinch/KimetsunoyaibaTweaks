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
 * Handles breathing form cycling with configurable keybinding (forward) and Shift+Key (backward).
 * This intercepts the kimetsunoyaiba mod's key to add backward cycling functionality.
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

        // Only handle key press, not release
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
            // Custom breathing sword - WE handle both directions
            // Consume the base mod's key to prevent it from processing
            try {
                net.mcreator.kimetsunoyaiba.init.KimetsunoyaibaModKeyMappings.CHANGE_BREATHES_AND_BLOOD_ART.consumeClick();
            } catch (Exception e) {
                // Ignore if base mod key not accessible
            }

            if (isBackward) {
                // Backward cycling
                if (Config.logDebug) {
                    Log.debug("Backward key pressed on custom sword - cycling backward");
                }
                ModNetworking.sendToServer(new CycleBreathingFormPacket(-1));
                handledShiftR = true;
            } else {
                // Forward cycling
                if (Config.logDebug) {
                    Log.debug("Forward key pressed on custom sword - cycling forward");
                }
                ModNetworking.sendToServer(new CycleBreathingFormPacket(1));
                handledShiftR = false;
            }
        } else {
            // Base mod sword - allow cycling if either breathes or demon_art is active
            double breathes = player.getPersistentData().getDouble("breathes");
            double demonArt = player.getPersistentData().getDouble("demon_art");
            if (breathes == 0.0 && demonArt == 0.0) {
                return;
            }

            if (isBackward) {
                // Backward cycling - consume base mod's key and send our backward packet
                if (Config.logDebug) {
                    Log.debug("Backward key pressed on base mod sword - cycling backward");
                }

                // Consume the base mod's key to prevent forward cycling
                try {
                    net.mcreator.kimetsunoyaiba.init.KimetsunoyaibaModKeyMappings.CHANGE_BREATHES_AND_BLOOD_ART.consumeClick();
                } catch (Exception e) {
                    // Ignore if base mod key not accessible
                }

                // Send backward cycle packet
                ModNetworking.sendToServer(new CycleBreathingFormPacket(-1));
                handledShiftR = true;
            } else {
                // Forward cycling - let the base mod handle it normally
                if (Config.logDebug) {
                    Log.debug("Forward key pressed on base mod sword - letting base mod handle it");
                }
                handledShiftR = false;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseInput(InputEvent.MouseButton event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (event.getAction() != GLFW.GLFW_PRESS) return;
        if (mc.screen != null) return;

        boolean isForward = ModKeyBindings.CYCLE_BREATHING_FORM.matchesMouse(event.getButton());
        boolean isBackward = ModKeyBindings.CYCLE_BREATHING_FORM_BACKWARD.matchesMouse(event.getButton());

        if (!isForward && !isBackward) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCycleTime < CYCLE_COOLDOWN_MS) {
            if (Config.logDebug) {
                Log.debug("Form cycling on cooldown, ignoring mouse press");
            }
            return;
        }
        lastCycleTime = currentTime;

        LocalPlayer player = mc.player;
        ItemStack heldItem = player.getMainHandItem();
        if (!BreathingInfoDetector.isNichirinSword(heldItem)) return;

        boolean isCustomBreathingSword = heldItem.getItem() instanceof
            com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem;

        if (isCustomBreathingSword) {
            try {
                net.mcreator.kimetsunoyaiba.init.KimetsunoyaibaModKeyMappings
                    .CHANGE_BREATHES_AND_BLOOD_ART.consumeClick();
            } catch (Exception e) {}

            if (isBackward) {
                if (Config.logDebug) {
                    Log.debug("Backward mouse button pressed on custom sword - cycling backward");
                }
                ModNetworking.sendToServer(new CycleBreathingFormPacket(-1));
                handledShiftR = true;
            } else {
                if (Config.logDebug) {
                    Log.debug("Forward mouse button pressed on custom sword - cycling forward");
                }
                ModNetworking.sendToServer(new CycleBreathingFormPacket(1));
                handledShiftR = false;
            }
        } else {
            double breathes = player.getPersistentData().getDouble("breathes");
            double demonArt = player.getPersistentData().getDouble("demon_art");
            if (breathes == 0.0 && demonArt == 0.0) return;

            if (isBackward) {
                if (Config.logDebug) {
                    Log.debug("Backward mouse button pressed on base mod sword - cycling backward");
                }

                try {
                    net.mcreator.kimetsunoyaiba.init.KimetsunoyaibaModKeyMappings
                        .CHANGE_BREATHES_AND_BLOOD_ART.consumeClick();
                } catch (Exception e) {}

                ModNetworking.sendToServer(new CycleBreathingFormPacket(-1));
                handledShiftR = true;
            } else {
                if (Config.logDebug) {
                    Log.debug("Forward mouse button pressed on base mod sword - letting base mod handle it");
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
