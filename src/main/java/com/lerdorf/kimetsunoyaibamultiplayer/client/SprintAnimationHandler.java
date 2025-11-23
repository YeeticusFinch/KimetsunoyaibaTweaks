package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem;
import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles sprint animation for players holding nichirin swords
 */
public class SprintAnimationHandler {
    private static final int SPRINT_LAYER_PRIORITY = 200;
    private static final Map<UUID, ModifierLayer<IAnimation>> activeSprintLayers = new HashMap<>();
    private static final Map<UUID, Long> sprintAnimationStartTime = new HashMap<>();
    private static KeyframeAnimation cachedSprintAnimation = null;
    private static int cachedSprintDurationMs = 2000; // Default 2 seconds, will be updated from actual animation

    /**
     * Called every client tick to check if player is sprinting with a nichirin sword
     */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        // Check if sprint animation is enabled in config
        if (!com.lerdorf.kimetsunoyaibamultiplayer.Config.enableNichirinSprintAnimation) {
            // Remove any active sprint animations if config is disabled
            if (activeSprintLayers.containsKey(mc.player.getUUID())) {
                removeSprintAnimation(mc.player);
                sprintAnimationStartTime.remove(mc.player.getUUID());
            }
            return;
        }

        checkPlayerSprint(mc.player);
    }

    private static void checkPlayerSprint(AbstractClientPlayer player) {
        UUID playerUUID = player.getUUID();
        boolean isSprinting = player.isSprinting();

        // Check if player is holding a nichirin sword
        ItemStack mainHandItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        boolean holdingNichirinSword = mainHandItem.getItem() instanceof BreathingSwordItem ||
                                      (SwordRegistry.getSword(mainHandItem.getItem()) != null);

        // Only process if holding a nichirin sword
        if (!holdingNichirinSword || !isSprinting) {
            // Remove sprint animation if not holding sword or not sprinting
            if (activeSprintLayers.containsKey(playerUUID)) {
                removeSprintAnimation(player);
            }
            sprintAnimationStartTime.remove(playerUUID);
            return;
        }

        // Player is sprinting with a nichirin sword - manage the looping animation
        long currentTime = System.currentTimeMillis();
        Long animStartTime = sprintAnimationStartTime.get(playerUUID);

        if (animStartTime == null) {
            // Start the sprint animation
            applySprintAnimation(player);
            sprintAnimationStartTime.put(playerUUID, currentTime);
        } else {
            long elapsed = currentTime - animStartTime;
            // Trigger 4 times per second (every 250ms / 5 ticks) to force sprint animation
            int refreshInterval = 250;
            if (elapsed >= refreshInterval) {
                // Force refresh the sprint animation to prevent walk from overriding
                applySprintAnimation(player);
                sprintAnimationStartTime.put(playerUUID, currentTime);
            }
            // Otherwise animation is still playing - do nothing
        }
    }

    private static void applySprintAnimation(AbstractClientPlayer player) {
        // Remove existing sprint layer if any
        removeSprintAnimation(player);

        AnimationStack animationStack = PlayerAnimationAccess.getPlayerAnimLayer(player);
        if (animationStack == null) {
            return;
        }

        // Find sprint animation from PlayerAnimationRegistry (cache it)
        if (cachedSprintAnimation == null) {
            // The base mod has the sprint animation at "player_animation/sprint2.json"
            cachedSprintAnimation = PlayerAnimationRegistry.getAnimation(
                ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "sprint2"));

            if (cachedSprintAnimation == null) {
                // Fallback to try "sprint" without the 2
                cachedSprintAnimation = PlayerAnimationRegistry.getAnimation(
                    ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "sprint"));
            }

            if (cachedSprintAnimation == null) {
                System.err.println("[SprintAnimationHandler] Could not find sprint animation (tried sprint2 and sprint)");
                return;
            }

            // Get actual animation duration in milliseconds
            int lengthTicks = cachedSprintAnimation.getLength();
            cachedSprintDurationMs = lengthTicks * 50; // 20 tps = 50ms per tick
            com.lerdorf.kimetsunoyaibamultiplayer.Log.info(
                "[SprintAnimationHandler] Sprint animation duration: {} ticks = {} ms",
                lengthTicks, cachedSprintDurationMs);
        }

        KeyframeAnimation sprintAnim = cachedSprintAnimation;

        // Create a new layer for sprint animation
        ModifierLayer<IAnimation> sprintLayer = new ModifierLayer<>();
        // Play once - we'll manually loop by re-triggering in tick()
        KeyframeAnimationPlayer sprintPlayer = new KeyframeAnimationPlayer(sprintAnim);
        sprintLayer.setAnimation(sprintPlayer);

        // Add the layer to the stack at the specified priority
        animationStack.addAnimLayer(SPRINT_LAYER_PRIORITY, sprintLayer);

        // Track the active layer
        activeSprintLayers.put(player.getUUID(), sprintLayer);
    }

    private static void removeSprintAnimation(AbstractClientPlayer player) {
        UUID playerUUID = player.getUUID();
        ModifierLayer<IAnimation> sprintLayer = activeSprintLayers.remove(playerUUID);

        if (sprintLayer != null) {
            AnimationStack animationStack = PlayerAnimationAccess.getPlayerAnimLayer(player);
            if (animationStack != null) {
                // Remove by setting animation to null
                sprintLayer.setAnimation(null);
            }
        }
    }

    /**
     * Clean up when a player disconnects
     */
    public static void cleanup(UUID playerUUID) {
        activeSprintLayers.remove(playerUUID);
        sprintAnimationStartTime.remove(playerUUID);
    }
}
