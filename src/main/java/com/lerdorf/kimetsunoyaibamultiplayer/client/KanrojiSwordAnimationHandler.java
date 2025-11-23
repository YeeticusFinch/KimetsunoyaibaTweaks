package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.items.NichirinSwordKanrojiAnimated;
import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles animation synchronization between players/entities and the Kanroji sword item.
 *
 * This handler:
 * 1. Tracks the current animation playing on the player/entity
 * 2. Applies the corresponding animation to the Kanroji sword item
 * 3. Supports both playeranimator animations (for players) and GeckoLib animations (for entities)
 *
 * Animation priorities (lowest to highest):
 * - idle: Plays when standing still (lowest priority, easily overridden)
 * - walk: Plays when walking
 * - sprint: Plays when sprinting
 * - Action animations (sword_to_left, sword_to_right, etc.): Highest priority
 */
public class KanrojiSwordAnimationHandler {

    /**
     * Tracks the last known animation state for each entity/player.
     */
    private static class AnimationState {
        String currentAnimation = "idle";
        boolean isWalking = false;
        boolean isSprinting = false;
        double lastX = 0;
        double lastY = 0;
        double lastZ = 0;
        long lastAnimationStartTime = 0;
        String lastActionAnimation = null; // Track last action animation (for transitions)
        String lastTransitionAnimation = null; // Track last transition animation

        // Movement averaging to prevent flickering
        double[] recentSpeeds = new double[5]; // Last 5 ticks of speed
        int speedIndex = 0;
    }

    private static final Map<UUID, AnimationState> entityStates = new HashMap<>();

    private static int tickCounter = 0;

    /**
     * Called every client tick to update sword animations based on player/entity state.
     */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        // Log every 100 ticks (5 seconds) to verify tick() is being called
        if (++tickCounter % 100 == 0) {
            Log.debug("[KanrojiSwordAnimationHandler] tick() called (tick {})", tickCounter);
        }

        // Check local player
        updatePlayerSwordAnimation(mc.player);

        // Check other players and entities in the level
        mc.level.players().forEach(player -> {
            if (player != mc.player) {
                updatePlayerSwordAnimation((AbstractClientPlayer) player);
            }
        });
    }

    /**
     * Update the sword animation based on the player's current state and active animations.
     */
    private static void updatePlayerSwordAnimation(AbstractClientPlayer player) {
        UUID playerUUID = player.getUUID();
        AnimationState state = entityStates.computeIfAbsent(playerUUID, id -> new AnimationState());

        // Check if player is holding Kanroji sword
        ItemStack mainHandItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        boolean isHoldingKanrojiSword = mainHandItem.getItem() instanceof NichirinSwordKanrojiAnimated;

        // Check if Kanroji sword is displayed on hip/back
        SwordDisplayTracker.SwordDisplayState displayState = SwordDisplayTracker.getDisplayState(playerUUID);
        boolean kanrojiOnLeftHip = displayState.hasLeftSword() &&
                                  displayState.getLeftHipSword().getItem() instanceof NichirinSwordKanrojiAnimated;
        boolean kanrojiOnRightHip = displayState.hasRightSword() &&
                                   displayState.getRightHipSword().getItem() instanceof NichirinSwordKanrojiAnimated;

        // If not holding AND not displayed, nothing to do
        if (!isHoldingKanrojiSword && !kanrojiOnLeftHip && !kanrojiOnRightHip) {
            return;
        }

        Log.debug("[KanrojiSwordAnimationHandler] Player {} - holding={}, leftHip={}, rightHip={}",
                  player.getName().getString(), isHoldingKanrojiSword, kanrojiOnLeftHip, kanrojiOnRightHip);

        // If sword is displayed on hip/back, play sheath animation
        if ((kanrojiOnLeftHip || kanrojiOnRightHip) && !isHoldingKanrojiSword) {
            String targetAnimation = "sheath";
            boolean animationChanged = !targetAnimation.equals(state.currentAnimation);

            if (animationChanged) {
                Log.info("[KanrojiSwordAnimationHandler] Kanroji sword on hip/back - triggering sheath animation");
                state.currentAnimation = targetAnimation;
                // Trigger sheath animation on the displayed sword(s)
                if (kanrojiOnLeftHip) {
                    KanrojiSwordAnimationTrigger.triggerAnimationOnItemStack(player, displayState.getLeftHipSword(), targetAnimation);
                }
                if (kanrojiOnRightHip) {
                    KanrojiSwordAnimationTrigger.triggerAnimationOnItemStack(player, displayState.getRightHipSword(), targetAnimation);
                }
            }
            return;
        }

        // Sword is held in hand - update movement-based animations
        if (!isHoldingKanrojiSword) {
            return;
        }

        // Detect movement (use horizontal velocity only, ignore vertical/jumping)
        double dx = player.getX() - state.lastX;
        double dz = player.getZ() - state.lastZ;
        double horizontalSpeed = Math.sqrt(dx * dx + dz * dz);

        state.lastX = player.getX();
        state.lastY = player.getY();
        state.lastZ = player.getZ();

        // Average horizontal speed over last 5 ticks to prevent flickering
        state.recentSpeeds[state.speedIndex] = horizontalSpeed;
        state.speedIndex = (state.speedIndex + 1) % state.recentSpeeds.length;

        double avgSpeed = 0;
        for (double speed : state.recentSpeeds) {
            avgSpeed += speed;
        }
        avgSpeed /= state.recentSpeeds.length;

        // Check if sprinting or walking (use averaged horizontal movement)
        boolean isSprinting = player.isSprinting();
        boolean isWalking = !isSprinting && avgSpeed > 0.003; // Use averaged speed

        Log.debug("[KanrojiSwordAnimationHandler] Movement: instantSpeed={}, avgSpeed={}, sprinting={}, walking={}",
                  horizontalSpeed, avgSpeed, isSprinting, isWalking);

        // Check for active playeranimator animations (sword swings have TOP PRIORITY)
        String activeAnimation = getActivePlayerAnimation(player);

        // Determine which animation should play on the sword
        String targetAnimation;
        long currentTime = System.currentTimeMillis();

        // PRIORITY 1: Active action animation (sword swings) - ALWAYS override everything
        if (activeAnimation != null && isActionAnimation(activeAnimation)) {
            targetAnimation = activeAnimation;
            Log.info("[KanrojiSwordAnimationHandler] PRIORITY: Active sword swing - {}", activeAnimation);
        }
        // PRIORITY 2: Check if we should play a transition after a sword swing finished
        else if (state.lastActionAnimation != null) {
            // Check if enough time has passed for the action animation to finish
            long timeSinceAction = currentTime - state.lastAnimationStartTime;
            int actionDuration = getAnimationDuration(state.lastActionAnimation);

            if (timeSinceAction >= actionDuration) {
                // Action animation finished, return to movement animations
                state.lastActionAnimation = null; // Clear the action
                targetAnimation = determineTargetAnimation(activeAnimation, isSprinting, isWalking);
                Log.info("[KanrojiSwordAnimationHandler] Action finished, returning to: {}", targetAnimation);
            } else {
                // Action still playing - keep current
                targetAnimation = state.currentAnimation;
            }
        }
        // PRIORITY 3: Check if transition animation finished
        else if (state.lastTransitionAnimation != null) {
            long timeSinceTransition = currentTime - state.lastAnimationStartTime;
            int transitionDuration = getAnimationDuration(state.lastTransitionAnimation);

            if (timeSinceTransition >= transitionDuration) {
                // Transition finished - return to movement animations
                state.lastTransitionAnimation = null;
                targetAnimation = determineTargetAnimation(activeAnimation, isSprinting, isWalking);
                Log.info("[KanrojiSwordAnimationHandler] Transition finished, returning to movement: {}", targetAnimation);
            } else {
                // Transition still playing
                targetAnimation = state.currentAnimation;
            }
        }
        // PRIORITY 4: Normal movement-based animations (walk/sprint/idle)
        else {
            targetAnimation = determineTargetAnimation(activeAnimation, isSprinting, isWalking);
        }

        Log.debug("[KanrojiSwordAnimationHandler] Target animation: {}", targetAnimation);

        // Only trigger if the animation has changed to avoid re-triggering every tick
        boolean animationChanged = !targetAnimation.equals(state.currentAnimation);

        if (animationChanged) {
            Log.info("[KanrojiSwordAnimationHandler] Animation changed: {} -> {}", state.currentAnimation, targetAnimation);

            // Track action animations for transition system
            if (isActionAnimation(targetAnimation)) {
                state.lastActionAnimation = targetAnimation;
                state.lastAnimationStartTime = currentTime;
            }
            // Track transition animations
            else if (targetAnimation.equals("random") || targetAnimation.equals("random2")) {
                state.lastAnimationStartTime = currentTime;
            }
            // Clear transition tracking when returning to movement animations
            else if (targetAnimation.equals("idle") || targetAnimation.equals("walk") || targetAnimation.equals("sprint")) {
                state.lastTransitionAnimation = null;
            }
        }

        // Update state
        state.currentAnimation = targetAnimation;
        state.isSprinting = isSprinting;
        state.isWalking = isWalking;

        // Only trigger the animation if it changed (don't spam triggers every tick)
        if (animationChanged) {
            KanrojiSwordAnimationTrigger.triggerAnimation(player, targetAnimation);
        }
    }

    /**
     * Get the currently active animation from the player's animation stack.
     * Returns null if no action animation is playing.
     *
     * This checks the PlayerAnimator animation stack for active animations.
     */
    private static String getActivePlayerAnimation(AbstractClientPlayer player) {
        // Note: Detecting active player animations from the PlayerAnimator stack is complex
        // because it requires reflection to access the layers field.
        // For now, we rely on AnimationHelper to trigger sword swing animations directly,
        // and only use the tick-based handler for walk/sprint/idle state animations.
        // This is sufficient because:
        // 1. AnimationHelper.playAnimation() triggers sword swings via triggerKanrojiSwordAnimation()
        // 2. The animation-changed check prevents walk/sprint/idle from re-triggering every tick
        // 3. Sprint already works correctly using this approach
        return null;
    }

    /**
     * Extract the animation name from an IAnimation instance.
     * This is a heuristic approach since IAnimation doesn't directly expose the name.
     */
    private static String extractAnimationName(IAnimation animation) {
        try {
            String animationString = animation.toString();
            // Common animation names we're looking for
            if (animationString.contains("sword_to_left")) return "sword_to_left";
            if (animationString.contains("sword_to_right")) return "sword_to_right";
            if (animationString.contains("speed_attack_sword")) return "speed_attack_sword";
            if (animationString.contains("sword_overhead")) return "sword_overhead";
            if (animationString.contains("sword_rotate")) return "sword_rotate";
        } catch (Exception e) {
            // Silently ignore errors
        }
        return null;
    }

    /**
     * Determine which animation should play on the sword based on current state.
     * Priority: Action animations > sprint > walk > idle
     */
    private static String determineTargetAnimation(String activeAnimation, boolean isSprinting, boolean isWalking) {
        // If there's an active action animation, use that (highest priority)
        if (activeAnimation != null) {
            return activeAnimation;
        }

        // Otherwise, use movement-based animations
        if (isSprinting) {
            return "sprint";
        } else if (isWalking) {
            return "walk";
        } else {
            return "idle";
        }
    }

    /**
     * Trigger the specified animation on the Kanroji sword item.
     *
     * Note: GeckoLib item animations are controlled by the renderer based on
     * the item's AnimatableInstanceCache. The animation will be set when the
     * item is rendered.
     */
    private static void triggerSwordAnimation(ItemStack itemStack, String animationName) {
        // Animation triggering is handled by the GeckoLib renderer
        // The renderer will query the current animation state when rendering
        // For now, we just track the state change
    }

    /**
     * Check if an animation is an action animation (non-looping sword swing)
     */
    private static boolean isActionAnimation(String animationName) {
        if (animationName == null) return false;
        switch (animationName) {
            case "sword_to_left":
            case "sword_to_right":
            case "speed_attack_sword":
            case "sword_overhead":
            case "kanroji_sword_overhead":
            case "sword_rotate":
            case "love_first_form":
                return true;
            default:
                return false;
        }
    }

    /**
     * Get the actual duration in milliseconds for an animation by reading from the animation file.
     * Falls back to estimated durations if animation cannot be loaded.
     */
    private static int getAnimationDuration(String animationName) {
        if (animationName == null) return 0;

        try {
            // Try to load the actual animation to get its real duration
            net.minecraft.resources.ResourceLocation animLoc = parseAnimationResourceLocation(animationName);
            dev.kosmx.playerAnim.core.data.KeyframeAnimation anim =
                dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry.getAnimation(animLoc);

            if (anim != null) {
                // Get actual animation length in ticks and convert to milliseconds
                int lengthTicks = anim.getLength();
                int lengthMs = lengthTicks * 50; // 20 tps = 50ms per tick
                com.lerdorf.kimetsunoyaibamultiplayer.Log.debug(
                    "[KanrojiSwordAnimationHandler] Animation '{}' actual duration: {} ticks = {} ms",
                    animationName, lengthTicks, lengthMs);
                return lengthMs;
            }
        } catch (Exception e) {
            // Fall through to estimates
        }

        // Fallback: Approximate durations based on typical sword swing animations
        switch (animationName) {
            case "sword_to_left":
            case "sword_to_right":
            case "sword_overhead":
                return 650; // ~13 ticks at 20 tps
            case "kanroji_sword_overhead":
                return 1000; // ~20 ticks (1 second)
            case "sword_rotate":
                return 650;
            case "speed_attack_sword":
                return 400; // Faster attack
            case "random":
                return 2000; // 2 seconds
            case "random2":
                return 1500; // 1.5 seconds
            case "love_first_form":
                return 4600; // 4.6 seconds (92 ticks)
            default:
                return 1000; // Default 1 second
        }
    }

    /**
     * Parse animation name to ResourceLocation for loading
     */
    private static net.minecraft.resources.ResourceLocation parseAnimationResourceLocation(String animationName) {
        if (animationName.contains(":")) {
            String[] parts = animationName.split(":", 2);
            return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]);
        }

        // Try kimetsunoyaibamultiplayer namespace first for our custom animations
        if (animationName.equals("kanroji_sword_overhead") || animationName.equals("random") || animationName.equals("random2")) {
            return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", animationName);
        }

        // Default to kimetsunoyaiba namespace
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", animationName);
    }

    /**
     * Notify the handler that an action animation has been triggered externally.
     * This prevents the tick handler from overriding the animation with movement animations.
     */
    public static void notifyActionAnimation(UUID playerUUID, String animationName) {
        if (!isActionAnimation(animationName)) {
            return;
        }

        AnimationState state = entityStates.computeIfAbsent(playerUUID, id -> new AnimationState());
        state.lastActionAnimation = animationName;
        state.lastAnimationStartTime = System.currentTimeMillis();
        state.currentAnimation = animationName;
        state.lastTransitionAnimation = null; // Clear any pending transitions

        Log.info("[KanrojiSwordAnimationHandler] External action animation notified: {} for player {}",
            animationName, playerUUID);
    }

    /**
     * Clean up state when a player disconnects.
     */
    public static void cleanup(UUID playerUUID) {
        entityStates.remove(playerUUID);
    }

    /**
     * Custom animation predicate for the Kanroji sword that responds to entity state.
     * This should be used in the registerControllers method of NichirinSwordKanrojiAnimated.
     */
    public static <T extends GeoAnimatable> PlayState animationPredicate(software.bernie.geckolib.core.animation.AnimationState<T> event) {
        // Default to idle animation
        event.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));
        return PlayState.CONTINUE;
    }
}
