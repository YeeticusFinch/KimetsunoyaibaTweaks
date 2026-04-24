package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.items.BloodDemonArtAxeItem;
import com.lerdorf.kimetsunoyaibamultiplayer.items.BloodDemonArtItem;
import com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem;
import com.lerdorf.kimetsunoyaibamultiplayer.items.CustomDemonArtItem;
import com.lerdorf.kimetsunoyaibamultiplayer.util.TrainingSwordHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * Handles key input events for breathing techniques
 */
@Mod.EventBusSubscriber(modid = com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer.MODID, value = Dist.CLIENT)
public class KeyInputHandler {

    // Cooldown to prevent double-triggering on mouse buttons (same approach as BreathingFormCycleHandler)
    private static long lastMouseCycleTime = 0;
    private static final long MOUSE_CYCLE_COOLDOWN_MS = 150; // 150ms cooldown

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onKeyInput(InputEvent.Key event) {
      //  System.out.println("[KeyInputHandler] onKeyInput() ENTERED - key=" + event.getKey());
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null)
                return;
            if (event.getAction() != GLFW.GLFW_PRESS)
                return;
            if (mc.screen != null)
                return;

            // DEBUG: Log that we're in the key handler
            /*
            if (event.getKey() == GLFW.GLFW_KEY_G) {
                System.out.println("[KeyInputHandler] onKeyInput() called for G key!");
                if (Config.logDebug) {
                    Log.debug("[KeyInputHandler] onKeyInput() called for G key!");
                }
            }*/

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

            // DEBUG: Log all G key presses to diagnose keybind issues
            if (event.getKey() == GLFW.GLFW_KEY_G) {
                Log.debug("[KeyInputHandler] G key pressed! variationCycleKey={}, keybinding={}",
                    variationCycleKey,
                    ModKeyBindings.CYCLE_FORM_VARIATION != null
                        ? ModKeyBindings.CYCLE_FORM_VARIATION.getKey().getName()
                        : "null");
            }

            if (variationCycleKey) {
                ItemStack mainHandItem = mc.player.getItemInHand(InteractionHand.MAIN_HAND);

                Log.debug("[KeyInputHandler] Variation cycle key pressed (keyboard)");

                // Handle custom breathing swords
                if (mainHandItem.getItem() instanceof BreathingSwordItem) {
                    Log.debug("[KeyInputHandler] Detected CUSTOM breathing sword");
                    // Send packet to server to cycle variation
                    // Direction: Shift+G = backward (-1), G = forward (1)
                    int direction = mc.options.keyShift.isDown() ? -1 : 1;
                    Log.debug("[KeyInputHandler] Sending CycleFormVariationPacket for custom sword, direction: {}", direction);
                    com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToServer(
                            new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.CycleFormVariationPacket(direction));
                    return;
                }
                // Handle base mod swords
                else if (com.lerdorf.kimetsunoyaibamultiplayer.util.BreathingInfoDetector.isNichirinSword(mainHandItem)) {
                    Log.debug("[KeyInputHandler] Detected BASE MOD nichirin sword");

                    // Read form name from chat cache (populated when user cycled with R key)
                    // Get cached breathes value first
                    double breathes = com.lerdorf.kimetsunoyaibamultiplayer.client.BreathingFormTracker
                        .getCachedForm(mc.player.getUUID(), mainHandItem);
                    if (breathes == 0.0) {
                        breathes = mc.player.getPersistentData().getDouble("breathes");
                    }

                    Log.debug("[KeyInputHandler] Current breathes value: {}", breathes);

                    // Get form name from chat cache (source of truth - independent of breathes NBT corruption)
                    String formName = null;
                    if (breathes > 0) {
                        int formId = (int) breathes;
                        formName = com.lerdorf.kimetsunoyaibamultiplayer.client.BreathingFormTracker
                            .getDisplayTextForForm(mc.player.getUUID(), formId);

                        if (formName != null) {
                            // Strip color codes for server-side matching
                            formName = formName.replaceAll("§.", "");
                            Log.debug("[KeyInputHandler] Using form name from chat cache (formId={}): {}", formId, formName);
                        } else {
                            // Fallback: use BaseKnYForms map
                            com.lerdorf.kimetsunoyaibamultiplayer.BaseKnYForms.BaseForm form =
                                com.lerdorf.kimetsunoyaibamultiplayer.BaseKnYForms.forms.get(formId);
                            if (form != null) {
                                formName = form.name;
                                Log.debug("[KeyInputHandler] Form name from BaseKnYForms (formId={}): {}", formId, formName);
                            } else {
                                Log.warn("[KeyInputHandler] Form ID {} not found in chat cache or BaseKnYForms!", formId);
                            }
                        }
                    } else {
                        Log.debug("[KeyInputHandler] No breathes value found, cannot determine form");
                    }

                    // Send packet to server with form name
                    // Server will handle cycling and send chat message
                    // Direction: Shift+G = backward (-1), G = forward (1)
                    int direction = mc.options.keyShift.isDown() ? -1 : 1;
                    Log.debug("[KeyInputHandler] Sending CycleFormVariationPacket for base sword, direction: {}, formName: {}", direction, formName);
                    com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToServer(
                            new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.CycleFormVariationPacket(direction, formName));
                    return;
                }
                else {
                    Log.debug("[KeyInputHandler] Not a breathing sword, item: {}", mainHandItem.getItem());
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

            // TRAINING SWORD CHECK: Intercept cycling for base mod training swords
            // This must be checked before letting the base mod handle cycling
            if (TrainingSwordHelper.isTrainingSword(mainHandItem) &&
                    !(mainHandItem.getItem() instanceof BreathingSwordItem) &&
                    com.lerdorf.kimetsunoyaibamultiplayer.util.BreathingInfoDetector.isNichirinSword(mainHandItem)) {
                // This is a base mod training sword - block the cycle and show message
                // Consume ALL pending clicks from the base mod's keybind to prevent it from cycling
                // Note: InputEvent.Key is not cancelable, so we drain the click queue instead
                var keyMapping = net.mcreator.kimetsunoyaiba.init.KimetsunoyaibaModKeyMappings.CHANGE_BREATHES_AND_BLOOD_ART;
                while (keyMapping.consumeClick()) {
                    // Drain all pending clicks
                }
                // Also set the key as not down to prevent isDown() checks
                keyMapping.setDown(false);

                // Show training sword message
                mc.player.displayClientMessage(
                    Component.literal("\u00A7e[Training Sword] \u00A7fOnly the 1st Form is available on a training sword."),
                    true
                );

                // Send packet to server to ensure first form is set (server will handle reset)
                com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToServer(
                        new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.CycleBreathingFormPacket(1));
                return;
            }

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

            if (mainHandItem.getItem() instanceof BloodDemonArtItem
                    || mainHandItem.getItem() instanceof BloodDemonArtAxeItem
                    || mainHandItem.getItem() instanceof CustomDemonArtItem) {
                boolean shouldReverse = dedicatedReverseKey || (baseCycleKey && mc.options.keyShift.isDown());
                net.mcreator.kimetsunoyaiba.init.KimetsunoyaibaModKeyMappings.CHANGE_BREATHES_AND_BLOOD_ART.consumeClick();
                com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToServer(
                    new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.CycleBloodDemonArtFormPacket(shouldReverse ? -1 : 1));
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
            System.err.println("[KeyInputHandler] EXCEPTION: " + e.getMessage());
            e.printStackTrace();
        }
    }


    @SubscribeEvent(priority = EventPriority.HIGHEST)
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

                if (Config.logDebug) {
                    Log.debug("[KeyInputHandler] Variation cycle mouse button pressed: " + event.getButton());
                }

                // Handle custom breathing swords
                if (mainHandItem.getItem() instanceof BreathingSwordItem) {
                    // Send packet to server to cycle variation
                    // Direction: Shift+Mouse = backward (-1), Mouse = forward (1)
                    int direction = mc.options.keyShift.isDown() ? -1 : 1;
                    if (Config.logDebug) {
                        Log.debug("[KeyInputHandler] Sending CycleFormVariationPacket for custom sword, direction: " + direction);
                    }
                    com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToServer(
                            new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.CycleFormVariationPacket(direction));
                    return;
                }
                // Handle base mod swords
                else if (com.lerdorf.kimetsunoyaibamultiplayer.util.BreathingInfoDetector.isNichirinSword(mainHandItem)) {
                    // ALWAYS resolve form name fresh from breathes value (don't use cache)
                    // This ensures we always have the current form, even if the cache is stale
                    String formName = null;
                    double breathes = com.lerdorf.kimetsunoyaibamultiplayer.client.BreathingFormTracker
                        .getCachedForm(mc.player.getUUID(), mainHandItem);
                    if (breathes == 0.0) {
                        breathes = mc.player.getPersistentData().getDouble("breathes");
                    }

                    if (breathes > 0) {
                        int formId = (int) breathes;
                        com.lerdorf.kimetsunoyaibamultiplayer.BaseKnYForms.BaseForm form =
                            com.lerdorf.kimetsunoyaibamultiplayer.BaseKnYForms.forms.get(formId);
                        if (form != null) {
                            formName = form.name;
                        }
                    }

                    // Send packet to server with form name
                    // Direction: Shift+Mouse = backward (-1), Mouse = forward (1)
                    int direction = mc.options.keyShift.isDown() ? -1 : 1;
                    if (Config.logDebug) {
                        Log.debug("[KeyInputHandler] Sending CycleFormVariationPacket for base sword, direction: " + direction + ", formName: " + formName);
                    }
                    com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToServer(
                            new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.CycleFormVariationPacket(direction, formName));
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

            // TRAINING SWORD CHECK: Intercept cycling for base mod training swords (mouse input)
            // This must be checked before letting the base mod handle cycling
            if (TrainingSwordHelper.isTrainingSword(mainHandItem) &&
                    !(mainHandItem.getItem() instanceof BreathingSwordItem) &&
                    com.lerdorf.kimetsunoyaibamultiplayer.util.BreathingInfoDetector.isNichirinSword(mainHandItem)) {
                // This is a base mod training sword - block the cycle and show message
                // Consume ALL pending clicks from the base mod's keybind to prevent it from cycling
                // Note: InputEvent.MouseButton is not cancelable, so we drain the click queue instead
                var keyMapping = net.mcreator.kimetsunoyaiba.init.KimetsunoyaibaModKeyMappings.CHANGE_BREATHES_AND_BLOOD_ART;
                while (keyMapping.consumeClick()) {
                    // Drain all pending clicks
                }
                // Also set the key as not down to prevent isDown() checks
                keyMapping.setDown(false);

                // Show training sword message
                mc.player.displayClientMessage(
                    Component.literal("\u00A7e[Training Sword] \u00A7fOnly the 1st Form is available on a training sword."),
                    true
                );

                // Send packet to server to ensure first form is set (server will handle reset)
                com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToServer(
                        new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.CycleBreathingFormPacket(1));
                return;
            }

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

            if (mainHandItem.getItem() instanceof BloodDemonArtItem
                    || mainHandItem.getItem() instanceof BloodDemonArtAxeItem
                    || mainHandItem.getItem() instanceof CustomDemonArtItem) {
                boolean shouldReverse = dedicatedReverseButton || (baseCycleButton && mc.options.keyShift.isDown());
                net.mcreator.kimetsunoyaiba.init.KimetsunoyaibaModKeyMappings.CHANGE_BREATHES_AND_BLOOD_ART.consumeClick();
                com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToServer(
                    new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.CycleBloodDemonArtFormPacket(shouldReverse ? -1 : 1));
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
            System.err.println("[KeyInputHandler] EXCEPTION: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
