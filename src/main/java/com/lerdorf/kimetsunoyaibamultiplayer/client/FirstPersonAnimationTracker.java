package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tracks the currently playing animation on the local player for first-person rendering
 */
public class FirstPersonAnimationTracker {

    private static String currentAnimation = null;
    private static float currentAnimationProgress = 0.0f;
    private static int lastAnimationTick = 0;
    private static int lastAnimationLength = 0;

    /**
     * Update the current animation state for the local player
     * Should be called every render frame
     */
    public static void updateCurrentAnimation(AbstractClientPlayer player) {
        if (player == null) {
            currentAnimation = null;
            currentAnimationProgress = 0.0f;
            return;
        }

        try {
            AnimationStack animationStack = PlayerAnimationAccess.getPlayerAnimLayer(player);
            if (animationStack == null) {
                currentAnimation = null;
                currentAnimationProgress = 0.0f;
                return;
            }

            // Use reflection to get layers
            Field layersField = AnimationStack.class.getDeclaredField("layers");
            layersField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<dev.kosmx.playerAnim.core.util.Pair<Integer, IAnimation>> layers =
                (List<dev.kosmx.playerAnim.core.util.Pair<Integer, IAnimation>>) layersField.get(animationStack);

            // Find the highest priority active animation (skip sprint animations)
            // Use single pass instead of sorting for better performance
            int highestPriority = Integer.MIN_VALUE;
            String highestPriorityAnimName = null;
            int highestPriorityTick = 0;
            int highestPriorityLength = 0;

            for (dev.kosmx.playerAnim.core.util.Pair<Integer, IAnimation> pair : layers) {
                int priority = pair.getLeft();
                IAnimation anim = pair.getRight();

                if (anim != null && anim.isActive()) {
                    KeyframeAnimationPlayer animPlayer = extractKeyframePlayer(anim);
                    if (animPlayer != null) {
                        KeyframeAnimation data = animPlayer.getData();
                        if (data != null) {
                            String animName = extractAnimationName(data);

                            // Skip sprint animations
                            if (animName != null && (animName.equals("sprint") || animName.equals("sprint_senior"))) {
                                continue;
                            }

                            // Check if this is higher priority than what we've found so far
                            if (priority > highestPriority) {
                                highestPriority = priority;
                                highestPriorityAnimName = animName;
                                highestPriorityTick = animPlayer.getTick();
                                highestPriorityLength = data.getLength();
                            }
                        }
                    }
                }
            }

            // Apply the highest priority animation found
            if (highestPriorityAnimName != null) {
                currentAnimation = highestPriorityAnimName;
                lastAnimationTick = highestPriorityTick;
                lastAnimationLength = highestPriorityLength;

                // Calculate progress (0.0 to 1.0)
                if (lastAnimationLength > 0) {
                    currentAnimationProgress = (float) lastAnimationTick / (float) lastAnimationLength;
                    // Clamp to [0, 1]
                    currentAnimationProgress = Math.max(0.0f, Math.min(1.0f, currentAnimationProgress));
                } else {
                    currentAnimationProgress = 0.0f;
                }
            } else {
                currentAnimation = null;
                currentAnimationProgress = 0.0f;
            }

        } catch (Exception e) {
            // Silently fail
            currentAnimation = null;
            currentAnimationProgress = 0.0f;
        }
    }

    /**
     * Get the currently playing animation name (null if none)
     */
    public static String getCurrentAnimation() {
        return currentAnimation;
    }

    /**
     * Get the current animation progress (0.0 to 1.0)
     */
    public static float getCurrentAnimationProgress() {
        return currentAnimationProgress;
    }

    /**
     * Check if a specific animation is currently playing
     */
    public static boolean isPlayingAnimation(String animationName) {
        return currentAnimation != null && currentAnimation.equals(animationName);
    }

    /**
     * Check if the current animation should be rendered on the offhand in first person.
     */
    public static boolean isOffhandAnimation() {
        return isOffhandAnimation(currentAnimation);
    }

    /**
     * Check if a specific animation should be rendered on the offhand in first person.
     */
    public static boolean isOffhandAnimation(String animationName) {
        if (animationName == null) {
            return false;
        }
        return animationName.equals("left_sword_to_left")
                || animationName.equals("left_sword_to_right")
                || animationName.equals("left_sword_overhead");
    }

    /**
     * Check if any sword attack animation is playing
     */
    public static boolean isPlayingSwordAttack() {
        if (currentAnimation == null) return false;

        return currentAnimation.equals("sword_to_left") ||
               currentAnimation.equals("sword_to_right") ||
               currentAnimation.equals("left_sword_to_left") ||
               currentAnimation.equals("left_sword_to_right") ||
               currentAnimation.equals("sword_rotate") ||
               currentAnimation.equals("sword_overhead") ||
               currentAnimation.equals("left_sword_overhead") ||
               currentAnimation.equals("double_sword_overhead") ||
               currentAnimation.equals("sword_to_left_reverse") ||
               currentAnimation.equals("sword_to_right_reverse") ||
               currentAnimation.equals("sword_to_upper") ||
               currentAnimation.equals("beast2") ||
               currentAnimation.equals("breath_beast2");
    }

    // Helper methods to extract animation info

    private static KeyframeAnimationPlayer extractKeyframePlayer(IAnimation anim) {
        try {
            if (anim instanceof KeyframeAnimationPlayer) {
                return (KeyframeAnimationPlayer) anim;
            } else if (anim instanceof ModifierLayer) {
                ModifierLayer<?> modLayer = (ModifierLayer<?>) anim;
                IAnimation innerAnim = modLayer.getAnimation();
                if (innerAnim instanceof KeyframeAnimationPlayer) {
                    return (KeyframeAnimationPlayer) innerAnim;
                } else if (innerAnim != null) {
                    return extractKeyframePlayer(innerAnim);
                }
            }

            // Try reflection for nested structures
            Field[] fields = anim.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(anim);
                if (value instanceof KeyframeAnimationPlayer) {
                    return (KeyframeAnimationPlayer) value;
                } else if (value instanceof IAnimation) {
                    KeyframeAnimationPlayer nested = extractKeyframePlayer((IAnimation) value);
                    if (nested != null) return nested;
                }
            }
        } catch (Exception e) {
            // Silently fail
        }
        return null;
    }

    private static String extractAnimationName(KeyframeAnimation data) {
        // Try to get name from extraData first
        try {
            Field extraDataField = KeyframeAnimation.class.getDeclaredField("extraData");
            extraDataField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Object> extraData = (Map<String, Object>) extraDataField.get(data);
            if (extraData != null) {
                Object name = extraData.get("name");
                if (name instanceof String && !((String) name).isEmpty()) {
                    return (String) name;
                }
            }
        } catch (Exception e) {
            // Try next method
        }

        // Fallback to reflection search
        try {
            Field[] fields = KeyframeAnimation.class.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(data);

                if (value instanceof String) {
                    String strValue = (String) value;
                    if (isKnownAnimationName(strValue)) {
                        return strValue;
                    }
                }
            }
        } catch (Exception e) {
            // Silently fail
        }

        return null;
    }

    private static boolean isKnownAnimationName(String name) {
        String[] knownNames = {
            "sword_to_right", "sword_to_left", "sword_rotate", "sword_overhead",
            "left_sword_to_right", "left_sword_to_left", "left_sword_overhead", "double_sword_overhead",
            "sword_to_right_reverse", "sword_to_left_reverse", "sword_to_upper",
            "punch_right", "punch_left", "kick_right", "kick_left",
            "backstep", "guard", "iai1", "speed_attack_sword", "beast2", "breath_beast2"
        };

        for (String known : knownNames) {
            if (known.equals(name)) {
                return true;
            }
        }
        return false;
    }
}
