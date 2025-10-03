package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.SwordParticleHandler;
import com.mojang.logging.LogUtils;
import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import java.util.HashMap;
import java.util.UUID;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AnimationSyncHandler {
    private static final Map<UUID, ActiveAnimation> syncedAnimations = new HashMap<>();

    private static class ActiveAnimation {
        ModifierLayer<IAnimation> modifierLayer;
        ResourceLocation animationId;
        int lastUpdateTick;

        ActiveAnimation(ModifierLayer<IAnimation> layer, ResourceLocation id) {
            this.modifierLayer = layer;
            this.animationId = id;
            this.lastUpdateTick = 0;
        }
    }

    public static void handleAnimationSync(UUID playerUUID, ResourceLocation animationId, int currentTick,
                                          int animationLength, boolean isLooping, boolean stopAnimation, KeyframeAnimation animationData) {
        handleAnimationSync(playerUUID, animationId, currentTick, animationLength, isLooping, stopAnimation, animationData, ItemStack.EMPTY, null);
    }

    public static void handleAnimationSync(UUID playerUUID, ResourceLocation animationId, int currentTick,
                                          int animationLength, boolean isLooping, boolean stopAnimation, KeyframeAnimation animationData,
                                          ItemStack swordItem, ResourceLocation particleType) {
        if (Config.logDebug) {
            Log.info("handleAnimationSync called: player={}, animation={}, tick={}, stop={}",
                playerUUID, animationId, currentTick, stopAnimation);
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            if (Config.logDebug) {
                Log.warn("Cannot handle animation sync - no level or player available");
            }
            return;
        }

        if (mc.player.getUUID().equals(playerUUID)) {
            if (Config.logDebug) {
                Log.debug("Ignoring animation sync for local player");
            }
            return;
        }

        Player targetPlayer = mc.level.getPlayerByUUID(playerUUID);
        if (targetPlayer == null) {
            Log.warn("Could not find player with UUID {} in level", playerUUID);
            return;
        }
        if (!(targetPlayer instanceof AbstractClientPlayer)) {
            Log.warn("Player {} is not an AbstractClientPlayer, type: {}",
                targetPlayer.getName().getString(), targetPlayer.getClass().getSimpleName());
            return;
        }

        if (Config.logDebug) {
            Log.info("Found target player: {}", targetPlayer.getName().getString());
        }

        AbstractClientPlayer clientPlayer = (AbstractClientPlayer) targetPlayer;

        // Send debug chat message about receiving animation
        if (Config.onScreenDebug && mc.player != null) {
            if (stopAnimation || animationId == null) {
                mc.player.displayClientMessage(
                    Component.literal("§e[AnimSync] " + targetPlayer.getName().getString() + " stopped animation"),
                    true
                );
            } else {
                mc.player.displayClientMessage(
                    Component.literal("§b[AnimSync] " + targetPlayer.getName().getString() + " playing: " + animationId.getPath()),
                    true
                );
            }
        }

        if (stopAnimation || animationId == null) {
            stopPlayerAnimation(playerUUID, clientPlayer);
        } else {
            playAnimation(playerUUID, clientPlayer, animationId, currentTick, animationLength, isLooping, animationData);

            // Trigger sword particles for synchronized animations if sword data is available
            if (!swordItem.isEmpty()) {
                String animationName = animationId.getPath();
                SwordParticleHandler.spawnSwordParticles(clientPlayer, swordItem, animationName);

                if (Config.logDebug) {
                    Log.debug("Triggered synchronized sword particles for player {} with animation {}",
                        clientPlayer.getName().getString(), animationName);
                }
            }
        }
    }

    private static void playAnimation(UUID playerUUID, AbstractClientPlayer player, ResourceLocation animationId,
                                     int currentTick, int animationLength, boolean isLooping, KeyframeAnimation animationData) {
        try {
            if (Config.logDebug) {
                Log.info("Attempting to play animation {} for player {} at tick {} (data present: {})",
                    animationId, player.getName().getString(), currentTick, animationData != null);
            }

            // Show a chat message to confirm sync is working
            if (Config.onScreenDebug) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    mc.player.displayClientMessage(
                        Component.literal("§a[AnimSync] ✓ Received: " + player.getName().getString() + " -> " + animationId.getPath()),
                        true
                    );
                }
            }

            // With Mob Player Animator, we can now apply animations to other players!
            if (animationData != null) {
                if (Config.logDebug) {
                    Log.info("Using transmitted animation data for {}", animationId);
                }
                applyAnimationToPlayer(player, animationData, currentTick, playerUUID, animationId);
            } else {
                // Try to find the animation from the registry
                KeyframeAnimation foundAnimation = findAnimationAlternative(animationId);
                if (foundAnimation != null) {
                    if (Config.logDebug) {
                        Log.info("Found animation {} via registry lookup", animationId);
                    }
                    applyAnimationToPlayer(player, foundAnimation, currentTick, playerUUID, animationId);
                } else {
                    // Create a fallback animation using kimetsunoyaiba animations
                    if (Config.logDebug) {
                        Log.info("Creating fallback animation for {} on player {}", animationId, player.getName().getString());
                    }
                    createFallbackAnimation(player, animationId, playerUUID);
                }
            }

        } catch (Exception e) {
            Log.error("Failed to process animation for player {}", player.getName().getString(), e);
        }
    }

    private static void stopPlayerAnimation(UUID playerUUID, AbstractClientPlayer player) {
        try {
            ActiveAnimation activeAnim = syncedAnimations.remove(playerUUID);
            if (activeAnim != null && activeAnim.modifierLayer != null) {
                AnimationStack animationStack = PlayerAnimationAccess.getPlayerAnimLayer(player);
                if (animationStack != null) {
                    activeAnim.modifierLayer.setAnimation(null);
                }

                if (Config.logDebug) {
                    Log.info("Successfully stopped animation for player {}", player.getName().getString());
                }
            }
        } catch (Exception e) {
            Log.error("Failed to stop animation for player {}", player.getName().getString(), e);
        }
    }

    private static void applyAnimationToPlayer(AbstractClientPlayer player, KeyframeAnimation animation, int currentTick, UUID playerUUID, ResourceLocation animationId) {
        try {
            AnimationStack animationStack = PlayerAnimationAccess.getPlayerAnimLayer(player);
            if (animationStack == null) {
                if (Config.logDebug) {
                    Log.warn("Animation stack is null for player {}", player.getName().getString());
                }
                return;
            }

            // Remove any existing animation for this player
            ActiveAnimation activeAnim = syncedAnimations.get(playerUUID);
            if (activeAnim != null && activeAnim.modifierLayer != null) {
                activeAnim.modifierLayer.setAnimation(null);
            }

            // Create new animation player with the received animation
            KeyframeAnimationPlayer newAnimation = new KeyframeAnimationPlayer(animation);

            // Create modifier layer and set the animation
            ModifierLayer<IAnimation> modifierLayer = new ModifierLayer<>();
            modifierLayer.setAnimation(newAnimation);

            // Add to animation stack
            animationStack.addAnimLayer(1000, modifierLayer);

            // Track the animation
            syncedAnimations.put(playerUUID, new ActiveAnimation(modifierLayer, animationId));

            if (Config.logDebug) {
                Log.info("Successfully applied animation {} to player {}", animationId, player.getName().getString());
            }

        } catch (Exception e) {
            Log.error("Failed to apply animation to player {}", player.getName().getString(), e);
        }
    }

    private static KeyframeAnimation findAnimationAlternative(ResourceLocation animationId) {
        try {
            String animationName = animationId.getPath();

            if (Config.logDebug) {
                Log.info("Searching for animation with name: '{}'", animationName);
            }

            // Try multiple namespace combinations
            ResourceLocation[] possibleLocations = {
                // Original ID
                animationId,
                // Common namespaces for kimetsunoyaiba animations
                ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", animationName),
                ResourceLocation.fromNamespaceAndPath("playeranimator", animationName),
                ResourceLocation.fromNamespaceAndPath("minecraft", animationName),
                ResourceLocation.fromNamespaceAndPath("forge", animationName),
                // Try with animations/ prefix (common in some mods)
                ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "animations/" + animationName),
                ResourceLocation.fromNamespaceAndPath("playeranimator", "animations/" + animationName),
                // Try with biped/ prefix (since the JSON is biped.animation.json)
                ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "biped/" + animationName),
                ResourceLocation.fromNamespaceAndPath("playeranimator", "biped/" + animationName),
                // Try without namespace (bare name)
                ResourceLocation.fromNamespaceAndPath("", animationName)
            };

            for (ResourceLocation loc : possibleLocations) {
                try {
                    KeyframeAnimation anim = PlayerAnimationRegistry.getAnimation(loc);
                    if (anim != null) {
                        if (Config.logDebug) {
                            Log.info("Found animation '{}' at ResourceLocation: {}", animationName, loc);
                        }
                        return anim;
                    }
                } catch (Exception e) {
                    // Ignore individual lookup failures, try next one
                }
            }

            if (Config.logDebug) {
                Log.warn("Could not find animation '{}' in registry with any namespace combination", animationName);
                logAvailableAnimations();
            }

        } catch (Exception e) {
            if (Config.logDebug) {
                Log.error("Error searching for animation", e);
            }
        }
        return null;
    }

    private static void logAvailableAnimations() {
        try {
            // Try to introspect the PlayerAnimationRegistry to see what's available
            if (Config.logDebug) {
                Log.info("Attempting to log available animations in registry...");
                // This might require additional reflection to access registry contents
                // For now, just log that we tried
                Log.info("Registry introspection not implemented yet");
            }
        } catch (Exception e) {
            Log.debug("Failed to log available animations: {}", e.getMessage());
        }
    }

    private static void createFallbackAnimation(AbstractClientPlayer player, ResourceLocation animationId, UUID playerUUID) {
        try {
            // Try multiple fallback animations
            String[] fallbackNames = {"sword_to_left", "punch_right", "walk", "idle"};

            for (String fallbackName : fallbackNames) {
                KeyframeAnimation fallbackAnim = findAnimationAlternative(
                    ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", fallbackName)
                );

                if (fallbackAnim != null) {
                    if (Config.logDebug) {
                        Log.info("Using fallback animation '{}' for {}", fallbackName, animationId);
                    }
                    applyAnimationToPlayer(player, fallbackAnim, 0, playerUUID,
                        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", fallbackName));
                    return;
                }
            }

            // Last resort: create a simple test animation to show system is working
            AnimationStack animationStack = PlayerAnimationAccess.getPlayerAnimLayer(player);
            if (animationStack != null) {
                ModifierLayer<IAnimation> modifierLayer = new ModifierLayer<>();

                // Track this as an active animation
                syncedAnimations.put(playerUUID, new ActiveAnimation(modifierLayer, animationId));

                // Add the empty modifier layer (Mob Player Animator should allow this to work on other players)
                animationStack.addAnimLayer(1000, modifierLayer);

                if (Config.logDebug) {
                    Log.info("Applied fallback empty animation layer for {} to {} (testing Mob Player Animator)", animationId, player.getName().getString());
                }

                // Show success message
                if (Config.onScreenDebug) {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player != null) {
                        mc.player.displayClientMessage(
                            Component.literal("§e[AnimSync] Applied fallback animation to " + player.getName().getString()),
                            true
                        );
                    }
                }

                // Remove it after a short time
                java.util.Timer timer = new java.util.Timer();
                timer.schedule(new java.util.TimerTask() {
                    @Override
                    public void run() {
                        try {
                            modifierLayer.setAnimation(null);
                            syncedAnimations.remove(playerUUID);
                            if (Config.logDebug) {
                                Log.info("Removed fallback animation for {}", player.getName().getString());
                            }
                        } catch (Exception e) {
                            // Ignore
                        }
                    }
                }, 3000); // Remove after 3 seconds
            }

        } catch (Exception e) {
            if (Config.logDebug) {
                Log.error("Failed to create fallback animation", e);
            }
        }
    }

    public static void clearAllAnimations() {
        for (Map.Entry<UUID, ActiveAnimation> entry : syncedAnimations.entrySet()) {
            ActiveAnimation activeAnim = entry.getValue();
            if (activeAnim != null && activeAnim.modifierLayer != null) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.level != null) {
                    Player player = mc.level.getPlayerByUUID(entry.getKey());
                    if (player instanceof AbstractClientPlayer) {
                        AnimationStack stack = PlayerAnimationAccess.getPlayerAnimLayer((AbstractClientPlayer) player);
                        if (stack != null) {
                            activeAnim.modifierLayer.setAnimation(null);
                        }
                    }
                }
            }
        }
        syncedAnimations.clear();
    }
}