package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.SpeedControlledAnimation;
import com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles replacing idle and walk animations when a player is wielding a sword
 * that has custom idle/walk animation replacements configured.
 *
 * This runs on the client side and monitors the player's main hand item.
 * When a sword with idle/walk replacements is equipped, it applies those
 * animations to low-priority layers. When unequipped, it removes them.
 */
public class IdleWalkAnimationHandler {

    // Layer priorities for idle/walk/sprint animations (lower than action animations at 3000)
    private static final int IDLE_LAYER_PRIORITY = 100;
    private static final int WALK_LAYER_PRIORITY = 101;
    private static final int SPRINT_LAYER_PRIORITY = 102;

    // Track the last held item for each player to detect equipment changes
    private static final Map<UUID, Item> lastHeldItem = new HashMap<>();

    // Track which players currently have idle/walk replacements active
    private static final Map<UUID, ActiveReplacements> activeReplacements = new HashMap<>();

    private static class ActiveReplacements {
        String idleAnimationName;
        String walkAnimationName;
        String sprintAnimationName;
        ModifierLayer<IAnimation> idleLayer;
        ModifierLayer<IAnimation> walkLayer;
        ModifierLayer<IAnimation> sprintLayer;
        float lastWalkSpeed = 1.0f;
        boolean wasMoving = false;

        ActiveReplacements(String idleAnimationName, String walkAnimationName, String sprintAnimationName,
                          ModifierLayer<IAnimation> idleLayer, ModifierLayer<IAnimation> walkLayer,
                          ModifierLayer<IAnimation> sprintLayer) {
            this.idleAnimationName = idleAnimationName;
            this.walkAnimationName = walkAnimationName;
            this.sprintAnimationName = sprintAnimationName;
            this.idleLayer = idleLayer;
            this.walkLayer = walkLayer;
            this.sprintLayer = sprintLayer;
        }
    }

    /**
     * Called every client tick to check for equipment changes and update animations.
     */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        checkPlayerEquipment(mc.player);
        updateAnimationStates(mc.player);
    }

    /**
     * Check if the player's equipment has changed and update animations accordingly.
     */
    private static void checkPlayerEquipment(AbstractClientPlayer player) {
        UUID playerUUID = player.getUUID();
        ItemStack mainHandItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        Item currentItem = mainHandItem.getItem();

        // Get the last held item for this player
        Item previousItem = lastHeldItem.get(playerUUID);

        // Check if the item has changed
        if (currentItem != previousItem) {
            if (Config.logDebug) {
                Log.debug("Equipment changed for player {}: {} -> {}",
                    player.getName().getString(),
                    previousItem != null ? previousItem.toString() : "null",
                    currentItem.toString());
            }

            // Update the tracked item
            lastHeldItem.put(playerUUID, currentItem);

            // Remove any existing idle/walk replacements
            removeIdleWalkReplacements(player);

            // Check if the new item is a registered sword with replacements
            if (!mainHandItem.isEmpty()) {
                SwordRegistry.RegisteredSword sword = SwordRegistry.getSword(currentItem);
                if (sword != null && sword.getReplaceAnimations() != null) {
                    Map<String, String> replacements = sword.getReplaceAnimations();

                    // Check for idle and walk replacements
                    String idleReplacement = getAnimationReplacement(replacements, "idle");
                    String walkReplacement = getAnimationReplacement(replacements, "walk");

                    if (idleReplacement != null || walkReplacement != null) {
                        if (Config.logDebug) {
                            Log.info("Applying idle/walk replacements for sword {}: idle={}, walk={}",
                                sword.getSwordId(), idleReplacement, walkReplacement);
                        }
                        applyIdleWalkReplacements(player, idleReplacement, walkReplacement);
                    }
                }
            }
        }
    }

    /**
     * Get the replacement animation name for a given original animation.
     * Handles both with and without namespace variants.
     */
    private static String getAnimationReplacement(Map<String, String> replacements, String animName) {
        if (replacements == null || replacements.isEmpty()) {
            return null;
        }

        // Try exact match
        if (replacements.containsKey(animName)) {
            return replacements.get(animName);
        }

        // Try with kimetsunoyaiba namespace
        String withNamespace = "kimetsunoyaiba:" + animName;
        if (replacements.containsKey(withNamespace)) {
            return replacements.get(withNamespace);
        }

        return null;
    }

    /**
     * Apply idle and/or walk animation replacements to the player.
     */
    private static void applyIdleWalkReplacements(AbstractClientPlayer player,
                                                  String idleReplacement,
                                                  String walkReplacement) {
        AnimationStack animationStack = PlayerAnimationAccess.getPlayerAnimLayer(player);
        if (animationStack == null) {
            if (Config.logDebug) {
                Log.warn("Cannot apply idle/walk replacements: animation stack is null for player {}",
                    player.getName().getString());
            }
            return;
        }

        // Check if player is currently moving to determine initial animation
        boolean isMoving = player.getDeltaMovement().horizontalDistanceSqr() > 0.001;

        ModifierLayer<IAnimation> idleLayer = null;
        ModifierLayer<IAnimation> walkLayer = null;

        // Only apply the appropriate animation based on current state
        if (isMoving) {
            // Apply walk replacement if specified
            if (walkReplacement != null) {
                KeyframeAnimation walkAnim = findAnimation(walkReplacement);
                if (walkAnim != null) {
                    // Calculate initial walk speed
                    double horizontalSpeed = Math.sqrt(player.getDeltaMovement().horizontalDistanceSqr());
                    float animationSpeed = (float) Math.max(0.5, Math.min(2.0, horizontalSpeed * 10.0));

                    try {
                        // Create walk animation with speed control
                        SpeedControlledAnimation animPlayer = new SpeedControlledAnimation(walkAnim, animationSpeed);
                        ModifierLayer<IAnimation> modifierLayer = new ModifierLayer<>();
                        modifierLayer.setAnimation(animPlayer);
                        animationStack.addAnimLayer(WALK_LAYER_PRIORITY, modifierLayer);
                        walkLayer = modifierLayer;

                        if (Config.logDebug) {
                            Log.info("Applied walk replacement (moving): {} on layer {} with speed {}",
                                walkReplacement, WALK_LAYER_PRIORITY, animationSpeed);
                        }
                    } catch (Exception e) {
                        Log.error("Failed to apply walk animation: {}", e.getMessage());
                    }
                } else {
                    Log.warn("Walk animation not found: {}", walkReplacement);
                }
            }
        } else {
            // Apply idle replacement if specified
            if (idleReplacement != null) {
                KeyframeAnimation idleAnim = findAnimation(idleReplacement);
                if (idleAnim != null) {
                    idleLayer = applyAnimationToLayer(animationStack, idleAnim, IDLE_LAYER_PRIORITY);
                    if (Config.logDebug && idleLayer != null) {
                        Log.info("Applied idle replacement (stationary): {} on layer {}",
                            idleReplacement, IDLE_LAYER_PRIORITY);
                    }
                } else {
                    Log.warn("Idle animation not found: {}", idleReplacement);
                }
            }
        }

        // Track the active replacements (store both names even if only one layer is active)
        activeReplacements.put(player.getUUID(),
            new ActiveReplacements(idleReplacement, walkReplacement, null, idleLayer, walkLayer, null));

        // Set initial state for tracking
        ActiveReplacements replacements = activeReplacements.get(player.getUUID());
        if (replacements != null) {
            replacements.wasMoving = isMoving;
            if (isMoving) {
                replacements.lastWalkSpeed = (float) Math.max(0.5, Math.min(2.0,
                    Math.sqrt(player.getDeltaMovement().horizontalDistanceSqr()) * 10.0));
            }
        }
    }

    /**
     * Apply an animation to a specific layer.
     */
    private static ModifierLayer<IAnimation> applyAnimationToLayer(AnimationStack animationStack,
                                                                   KeyframeAnimation animation,
                                                                   int layerPriority) {
        try {
            // Remove any existing animation on this layer
            animationStack.removeLayer(layerPriority);

            // Create and add the new animation
            KeyframeAnimationPlayer animPlayer = new KeyframeAnimationPlayer(animation);
            ModifierLayer<IAnimation> modifierLayer = new ModifierLayer<>();
            modifierLayer.setAnimation(animPlayer);
            animationStack.addAnimLayer(layerPriority, modifierLayer);

            return modifierLayer;
        } catch (Exception e) {
            Log.error("Failed to apply animation to layer {}: {}", layerPriority, e.getMessage());
            return null;
        }
    }

    /**
     * Remove any active idle/walk replacements for the player.
     */
    private static void removeIdleWalkReplacements(AbstractClientPlayer player) {
        UUID playerUUID = player.getUUID();
        ActiveReplacements replacements = activeReplacements.get(playerUUID);

        if (replacements != null) {
            if (Config.logDebug) {
                Log.info("Removing idle/walk replacements for player {}", player.getName().getString());
            }

            AnimationStack animationStack = PlayerAnimationAccess.getPlayerAnimLayer(player);
            if (animationStack != null) {
                // Remove the layers
                try {
                    animationStack.removeLayer(IDLE_LAYER_PRIORITY);
                    animationStack.removeLayer(WALK_LAYER_PRIORITY);

                    if (Config.logDebug) {
                        Log.debug("Removed animation layers {} and {}", IDLE_LAYER_PRIORITY, WALK_LAYER_PRIORITY);
                    }
                } catch (Exception e) {
                    Log.error("Failed to remove idle/walk animation layers: {}", e.getMessage());
                }
            }

            // Clear the tracking
            activeReplacements.remove(playerUUID);
        }
    }

    /**
     * Find a KeyframeAnimation by name, checking multiple namespaces.
     */
    private static KeyframeAnimation findAnimation(String animationName) {
        ResourceLocation animationLocation = parseAnimationName(animationName);
        KeyframeAnimation anim = PlayerAnimationRegistry.getAnimation(animationLocation);
        if (anim != null) {
            return anim;
        }

        // Try alternative namespaces
        String path = animationLocation.getPath();
        ResourceLocation[] alternativeLocations = {
            ResourceLocation.fromNamespaceAndPath("playeranimator", path),
            ResourceLocation.fromNamespaceAndPath("minecraft", path),
            ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", path),
            ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "animations/" + path)
        };

        for (ResourceLocation loc : alternativeLocations) {
            anim = PlayerAnimationRegistry.getAnimation(loc);
            if (anim != null) {
                return anim;
            }
        }

        return null;
    }

    /**
     * Parse an animation name string into a ResourceLocation.
     */
    private static ResourceLocation parseAnimationName(String animationName) {
        if (animationName.contains(":")) {
            String[] parts = animationName.split(":", 2);
            return ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]);
        }
        return ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", animationName);
    }

    /**
     * Clear all tracked data (e.g., when player logs out).
     */
    public static void clear() {
        lastHeldItem.clear();
        activeReplacements.clear();
    }

    /**
     * Clear data for a specific player.
     */
    public static void clearPlayer(UUID playerUUID) {
        lastHeldItem.remove(playerUUID);
        activeReplacements.remove(playerUUID);
    }

    /**
     * Update animation states based on player movement.
     * Switches between idle/walk animations and adjusts walk speed.
     */
    private static void updateAnimationStates(AbstractClientPlayer player) {
        UUID playerUUID = player.getUUID();
        ActiveReplacements replacements = activeReplacements.get(playerUUID);

        if (replacements == null) {
            return; // No active replacements for this player
        }

        AnimationStack animationStack = PlayerAnimationAccess.getPlayerAnimLayer(player);
        if (animationStack == null) {
            return;
        }

        // Determine if player is moving
        boolean isMoving = player.getDeltaMovement().horizontalDistanceSqr() > 0.001;

        // Calculate movement speed for animation speed scaling
        double horizontalSpeed = Math.sqrt(player.getDeltaMovement().horizontalDistanceSqr());
        // Normal walk speed is about 0.1, sprint is about 0.13
        // Scale animation to match: 1.0 at walk speed, 1.3 at sprint speed
        float animationSpeed = (float) Math.max(0.5, Math.min(2.0, horizontalSpeed * 10.0));

        // Check if state changed or speed changed significantly
        boolean stateChanged = isMoving != replacements.wasMoving;
        boolean speedChanged = Math.abs(animationSpeed - replacements.lastWalkSpeed) > 0.15f;

        if (isMoving) {
            // Player is moving - show walk animation
            // Remove idle layer if transitioning from idle to walk
            if (stateChanged && replacements.idleLayer != null) {
                try {
                    animationStack.removeLayer(IDLE_LAYER_PRIORITY);
                    replacements.idleLayer = null;
                } catch (Exception ignored) {}
            }

            // Add/update walk layer when state changes or speed changes significantly
            if ((stateChanged || speedChanged) && replacements.walkAnimationName != null) {
                KeyframeAnimation walkAnim = findAnimation(replacements.walkAnimationName);
                if (walkAnim != null) {
                    try {
                        // Remove old walk layer if exists
                        animationStack.removeLayer(WALK_LAYER_PRIORITY);

                        // Create new walk animation with current speed using SpeedControlledAnimation
                        SpeedControlledAnimation animPlayer = new SpeedControlledAnimation(walkAnim, animationSpeed);
                        ModifierLayer<IAnimation> modifierLayer = new ModifierLayer<>();
                        modifierLayer.setAnimation(animPlayer);
                        animationStack.addAnimLayer(WALK_LAYER_PRIORITY, modifierLayer);
                        replacements.walkLayer = modifierLayer;
                        replacements.lastWalkSpeed = animationSpeed;
                    } catch (Exception e) {
                        if (Config.logDebug) {
                            Log.error("Failed to update walk animation: {}", e.getMessage());
                        }
                    }
                }
            }
        } else {
            // Player is stationary - show idle animation
            // Remove walk layer if transitioning from walk to idle
            if (stateChanged && replacements.walkLayer != null) {
                try {
                    animationStack.removeLayer(WALK_LAYER_PRIORITY);
                    replacements.walkLayer = null;
                } catch (Exception ignored) {}
            }

            // Add idle layer when transitioning to idle
            if (stateChanged && replacements.idleLayer == null && replacements.idleAnimationName != null) {
                KeyframeAnimation idleAnim = findAnimation(replacements.idleAnimationName);
                if (idleAnim != null) {
                    replacements.idleLayer = applyAnimationToLayer(animationStack, idleAnim, IDLE_LAYER_PRIORITY);
                }
            }
        }

        // Update state tracking
        replacements.wasMoving = isMoving;
    }
}
