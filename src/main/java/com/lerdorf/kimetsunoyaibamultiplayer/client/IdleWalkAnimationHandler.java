package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
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

    // Layer priorities for idle/walk animations (lower than action animations at 3000)
    private static final int IDLE_LAYER_PRIORITY = 100;
    private static final int WALK_LAYER_PRIORITY = 101;

    // Track the last held item for each player to detect equipment changes
    private static final Map<UUID, Item> lastHeldItem = new HashMap<>();

    // Track which players currently have idle/walk replacements active
    private static final Map<UUID, ActiveReplacements> activeReplacements = new HashMap<>();

    private static class ActiveReplacements {
        String idleAnimationName;
        String walkAnimationName;
        ModifierLayer<IAnimation> idleLayer;
        ModifierLayer<IAnimation> walkLayer;

        ActiveReplacements(String idleAnimationName, String walkAnimationName,
                          ModifierLayer<IAnimation> idleLayer, ModifierLayer<IAnimation> walkLayer) {
            this.idleAnimationName = idleAnimationName;
            this.walkAnimationName = walkAnimationName;
            this.idleLayer = idleLayer;
            this.walkLayer = walkLayer;
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

        ModifierLayer<IAnimation> idleLayer = null;
        ModifierLayer<IAnimation> walkLayer = null;

        // Apply idle replacement if specified
        if (idleReplacement != null) {
            KeyframeAnimation idleAnim = findAnimation(idleReplacement);
            if (idleAnim != null) {
                idleLayer = applyAnimationToLayer(animationStack, idleAnim, IDLE_LAYER_PRIORITY);
                if (Config.logDebug && idleLayer != null) {
                    Log.info("Applied idle replacement: {} on layer {}", idleReplacement, IDLE_LAYER_PRIORITY);
                }
            } else {
                Log.warn("Idle animation not found: {}", idleReplacement);
            }
        }

        // Apply walk replacement if specified
        if (walkReplacement != null) {
            KeyframeAnimation walkAnim = findAnimation(walkReplacement);
            if (walkAnim != null) {
                walkLayer = applyAnimationToLayer(animationStack, walkAnim, WALK_LAYER_PRIORITY);
                if (Config.logDebug && walkLayer != null) {
                    Log.info("Applied walk replacement: {} on layer {}", walkReplacement, WALK_LAYER_PRIORITY);
                }
            } else {
                Log.warn("Walk animation not found: {}", walkReplacement);
            }
        }

        // Track the active replacements
        if (idleLayer != null || walkLayer != null) {
            activeReplacements.put(player.getUUID(),
                new ActiveReplacements(idleReplacement, walkReplacement, idleLayer, walkLayer));
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
}
