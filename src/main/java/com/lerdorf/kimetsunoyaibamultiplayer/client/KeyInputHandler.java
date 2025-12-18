package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
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

    // Cooldown to prevent double-triggering on mouse buttons (same approach as BreathingFormCycleHandler)
    private static long lastMouseCycleTime = 0;
    private static final long MOUSE_CYCLE_COOLDOWN_MS = 150; // 150ms cooldown

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null)
                return;
            if (event.getAction() != GLFW.GLFW_PRESS)
                return;
            if (mc.screen != null)
                return;

            // IMPORTANT: Skip this handler if the keybinding is bound to a mouse button!
            // Check if the base mod's key is bound to a mouse button
            if (net.mcreator.kimetsunoyaiba.init.KimetsunoyaibaModKeyMappings.CHANGE_BREATHES_AND_BLOOD_ART.getKey()
                    .getType() == com.mojang.blaze3d.platform.InputConstants.Type.MOUSE) {
                return; // Skip keyboard handler - onMouseInput will handle it
            }
            // Check if our dedicated reverse key is bound to a mouse button
            if (ModKeyBindings.CYCLE_BREATHING_FORM_BACKWARD != null &&
                    ModKeyBindings.CYCLE_BREATHING_FORM_BACKWARD.getKey()
                            .getType() == com.mojang.blaze3d.platform.InputConstants.Type.MOUSE) {
                return; // Skip keyboard handler - onMouseInput will handle it
            }
            // Check if variation cycle key is bound to a mouse button
            if (ModKeyBindings.CYCLE_FORM_VARIATION != null &&
                    ModKeyBindings.CYCLE_FORM_VARIATION.getKey()
                            .getType() == com.mojang.blaze3d.platform.InputConstants.Type.MOUSE) {
                return; // Skip keyboard handler - onMouseInput will handle it
            }

            // Check if variation cycle key was pressed
            boolean variationCycleKey = ModKeyBindings.CYCLE_FORM_VARIATION != null &&
                    ModKeyBindings.CYCLE_FORM_VARIATION.matches(event.getKey(), event.getScanCode());

            if (variationCycleKey) {
                ItemStack mainHandItem = mc.player.getItemInHand(InteractionHand.MAIN_HAND);

                // Handle custom breathing swords
                if (mainHandItem.getItem() instanceof BreathingSwordItem) {
                    // Send packet to server to cycle variation
                    // Direction: Shift+G = backward (-1), G = forward (1)
                    com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToServer(
                            new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.CycleFormVariationPacket(
                                    mc.options.keyShift.isDown() ? -1 : 1));
                    return;
                }
                // Handle base mod swords
                else if (com.lerdorf.kimetsunoyaibamultiplayer.util.BreathingInfoDetector.isNichirinSword(mainHandItem)) {
                    // Get client-side breathes value (from cache)
                    double breathes = com.lerdorf.kimetsunoyaibamultiplayer.client.BreathingFormTracker
                        .getCachedForm(mc.player.getUUID(), mainHandItem);
                    if (breathes == 0.0) {
                        breathes = mc.player.getPersistentData().getDouble("breathes");
                    }

                    // Send packet to server with breathes value
                    // Server will handle cycling and send chat message
                    // Direction: Shift+G = backward (-1), G = forward (1)
                    com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToServer(
                            new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.CycleFormVariationPacket(
                                    mc.options.keyShift.isDown() ? -1 : 1, breathes));
                    return;
                }
                return;
            }

            // Check if base mod's cycle key was pressed
            boolean baseCycleKey = net.mcreator.kimetsunoyaiba.init.KimetsunoyaibaModKeyMappings.CHANGE_BREATHES_AND_BLOOD_ART
                    .matches(event.getKey(), event.getScanCode());
            // Check if dedicated reverse cycle key was pressed (if bound)
            boolean dedicatedReverseKey = ModKeyBindings.CYCLE_BREATHING_FORM_BACKWARD != null &&
                    ModKeyBindings.CYCLE_BREATHING_FORM_BACKWARD.matches(event.getKey(), event.getScanCode());

            if (!baseCycleKey && !dedicatedReverseKey) {
                return;
            }

            ItemStack mainHandItem = mc.player.getItemInHand(InteractionHand.MAIN_HAND);

            // Handle custom breathing swords
            if (mainHandItem.getItem() instanceof BreathingSwordItem) {
                // Determine direction:
                // 1. If dedicated reverse key pressed → always reverse
                // 2. If base cycle key pressed + Shift held → reverse (mimics base mod behavior)
                // 3. If base cycle key pressed without Shift → forward
                boolean shouldReverse = dedicatedReverseKey || (baseCycleKey && mc.options.keyShift.isDown());

                // Consume base mod's keybind to prevent it from also cycling
                net.mcreator.kimetsunoyaiba.init.KimetsunoyaibaModKeyMappings.CHANGE_BREATHES_AND_BLOOD_ART
                        .consumeClick();

                // Send packet to server to cycle the form (server-authoritative)
                com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToServer(
                        new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.CycleBreathingFormPacket(
                                shouldReverse ? -1 : 1));
                return;
            }

            // Handle base mod swords with dedicated reverse key
            if (dedicatedReverseKey) {
                // Check if holding a nichirin sword
                if (com.lerdorf.kimetsunoyaibamultiplayer.util.BreathingInfoDetector.isNichirinSword(mainHandItem)) {
                    // Send packet to server to cycle backward
                    // Note: The base mod doesn't respond to our dedicated reverse key, so we just send -1
                    com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToServer(
                            new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.CycleBreathingFormPacket(-1));
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

            // Check if variation cycle key is bound to this mouse button
            boolean variationCycleButton = ModKeyBindings.CYCLE_FORM_VARIATION != null &&
                    ModKeyBindings.CYCLE_FORM_VARIATION.matchesMouse(event.getButton());

            if (variationCycleButton) {
                // COOLDOWN CHECK for variation cycling
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastMouseCycleTime < MOUSE_CYCLE_COOLDOWN_MS) {
                    return; // Too soon after last cycle - ignore
                }
                lastMouseCycleTime = currentTime;

                ItemStack mainHandItem = mc.player.getItemInHand(InteractionHand.MAIN_HAND);

                // Handle custom breathing swords
                if (mainHandItem.getItem() instanceof BreathingSwordItem) {
                    // Send packet to server to cycle variation
                    // Direction: Shift+G = backward (-1), G = forward (1)
                    com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToServer(
                            new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.CycleFormVariationPacket(
                                    mc.options.keyShift.isDown() ? -1 : 1));
                    return;
                }
                // Handle base mod swords
                else if (com.lerdorf.kimetsunoyaibamultiplayer.util.BreathingInfoDetector.isNichirinSword(mainHandItem)) {
                    // Get client-side breathes value (from cache or NBT)
                    double breathes = com.lerdorf.kimetsunoyaibamultiplayer.client.BreathingFormTracker
                        .getCachedForm(mc.player.getUUID(), mainHandItem);
                    if (breathes == 0.0) {
                        breathes = mc.player.getPersistentData().getDouble("breathes");
                    }

                    // Send packet to server with breathes value
                    // Direction: Shift+Mouse = backward (-1), Mouse = forward (1)
                    com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToServer(
                            new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.CycleFormVariationPacket(
                                    mc.options.keyShift.isDown() ? -1 : 1, breathes));
                    return;
                }
                return;
            }

            // Check if base mod's cycle key is bound to this mouse button
            boolean baseCycleButton = net.mcreator.kimetsunoyaiba.init.KimetsunoyaibaModKeyMappings.CHANGE_BREATHES_AND_BLOOD_ART.matchesMouse(event.getButton());
            // Check if dedicated reverse cycle key is bound to this mouse button (if bound)
            boolean dedicatedReverseButton = ModKeyBindings.CYCLE_BREATHING_FORM_BACKWARD != null &&
                                            ModKeyBindings.CYCLE_BREATHING_FORM_BACKWARD.matchesMouse(event.getButton());

            if (!baseCycleButton && !dedicatedReverseButton) {
                return;
            }

            // COOLDOWN CHECK: Prevent rapid double-cycling (same as BreathingFormCycleHandler)
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastMouseCycleTime < MOUSE_CYCLE_COOLDOWN_MS) {
                return; // Too soon after last cycle - ignore
            }
            // Update timestamp IMMEDIATELY to block race conditions
            // (both calls in same moment will see the updated timestamp)
            lastMouseCycleTime = currentTime;

            ItemStack mainHandItem = mc.player.getItemInHand(InteractionHand.MAIN_HAND);

            // Handle custom breathing swords
            if (mainHandItem.getItem() instanceof BreathingSwordItem) {
                // Determine direction (same logic as keyboard)
                boolean shouldReverse = dedicatedReverseButton || (baseCycleButton && mc.options.keyShift.isDown());

                // Consume base mod's keybind
                net.mcreator.kimetsunoyaiba.init.KimetsunoyaibaModKeyMappings.CHANGE_BREATHES_AND_BLOOD_ART
                        .consumeClick();
                

                // Send packet to server to cycle the form (server-authoritative)
                com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToServer(
                    new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.CycleBreathingFormPacket(shouldReverse ? -1 : 1)
                );
                return;
            }

            // Handle base mod swords with dedicated reverse button
            if (dedicatedReverseButton) {
                // Check if holding a nichirin sword
                if (com.lerdorf.kimetsunoyaibamultiplayer.util.BreathingInfoDetector.isNichirinSword(mainHandItem)) {
                    // Send packet to server to cycle backward
                    // Note: The base mod doesn't respond to our dedicated reverse key, so we just send -1
                    com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToServer(
                        new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.CycleBreathingFormPacket(-1)
                    );
                }
            }

        } catch (Exception e) {
            // Silently catch exceptions to prevent crashes
        }
    }
}