package com.lerdorf.kimetsunoyaibamultiplayer.client;

import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Auto-triggers whip animations based on player movement state.
 *
 * Handles transitions between:
 * - idle (standing still)
 * - walk (walking)
 * - sprint (sprinting)
 *
 * Called from ClientTickEvents every tick for players holding Mitsuri's sword.
 */
public class WhipAnimationTriggers {

    /**
     * Track previous player state for change detection.
     */
    private static class PlayerState {
        double prevX;
        double prevY;
        double prevZ;
        boolean wasSprinting;
        String currentAnimation;

        PlayerState(Player player) {
            this.prevX = player.getX();
            this.prevY = player.getY();
            this.prevZ = player.getZ();
            this.wasSprinting = player.isSprinting();
            this.currentAnimation = "idle";
        }
    }

    private static final Map<UUID, PlayerState> playerStates = new HashMap<>();

    /**
     * Update whip animation based on player movement.
     * Called every client tick.
     */
    public static void update(Player player) {
        PlayerState state = playerStates.computeIfAbsent(player.getUUID(), id -> new PlayerState(player));

        // Get current player position
        double currentX = player.getX();
        double currentY = player.getY();
        double currentZ = player.getZ();

        // Calculate movement
        double dx = currentX - state.prevX;
        double dy = currentY - state.prevY;
        double dz = currentZ - state.prevZ;
        double horizontalMovement = Math.sqrt(dx * dx + dz * dz);

        // Determine animation based on movement
        String targetAnimation;
        double targetExtension;

        if (player.isSprinting() && horizontalMovement > 0.01) {
            // Sprinting
            targetAnimation = "sprint";
            targetExtension = 0.3; // Slight extension when sprinting
        } else if (horizontalMovement > 0.01) {
            // Walking (any movement)
            targetAnimation = "walk";
            targetExtension = 0.1; // Minimal extension when walking
        } else {
            // Idle (no movement)
            targetAnimation = "idle";
            targetExtension = 0.0; // No extension when idle
        }

        // Only trigger animation change if state actually changed
        if (!targetAnimation.equals(state.currentAnimation)) {
            WhipPhysics.playAnimation(player, targetAnimation, targetExtension, 10);
            state.currentAnimation = targetAnimation;
        }

        // Update state for next tick
        state.prevX = currentX;
        state.prevY = currentY;
        state.prevZ = currentZ;
        state.wasSprinting = player.isSprinting();
    }

    /**
     * Clean up state for a player (call when player disconnects or stops holding sword).
     */
    public static void cleanup(UUID playerUUID) {
        playerStates.remove(playerUUID);
    }

    /**
     * Clear all tracked states (for world reload).
     */
    public static void clearAll() {
        playerStates.clear();
    }
}
