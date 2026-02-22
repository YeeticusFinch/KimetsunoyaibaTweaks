package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.SwordParticleMapping;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import java.util.Random;

/**
 * Handles attack animations for breathing swords
 */
public class BreathingSwordAnimationHandler {
    private static final Random RANDOM = new Random();
    private static long lastAttackTime = 0;
    private static boolean lastWasLeft = false;

    /**
     * Play attack animation when player attacks with breathing sword
     * @return The animation name that was played, or null if no animation was played
     */
    public static String onAttack(AbstractClientPlayer player) {
        try {
            ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);

            // Check if this is our mod's sword or a base mod sword
            boolean isOurSword = mainHand.getItem() instanceof BreathingSwordItem;
            boolean isBaseModSword = !isOurSword && SwordParticleMapping.isKimetsunoyaibaSword(mainHand);

            if (isOurSword || isBaseModSword) {
                long currentTime = System.currentTimeMillis();

                try {
                    if (player.getCapability(KimetsunoyaibaMultiplayer.SWORD_WIELDER_DATA).map(data -> data.cancelAttackSwing()).orElse(false)) {
                        // We are canceling attack swings
                        return null;
                    }
                } catch (Exception e) {
                    // Capability might not be available, continue with animation
                }

                // Prevent animation spam
                if (currentTime - lastAttackTime < 100) {
                    return null;
                }

                // For base mod swords, check if the base mod is already playing an animation
                // Only intervene if no sword animation is currently playing
                if (isBaseModSword && isPlayerPlayingSwordAnimation(player)) {
                    // Base mod animation is working, don't interfere
                    return null;
                }

                lastAttackTime = currentTime;

                String animationName;

                // Special handling for Kanroji/Love sword - uses whip-style attack animation set
                if (mainHand.getItem() instanceof com.lerdorf.kimetsunoyaibamultiplayer.items.NichirinSwordKanrojiAnimated ||
                    mainHand.getItem() instanceof com.lerdorf.kimetsunoyaibamultiplayer.items.NichirinSwordLoveAnimated) {
                    int roll = RANDOM.nextInt(100);
                    if (roll < 10) {
                        // 10% chance for sword_overhead
                        animationName = "sword_overhead";
                    } else if (roll < 20) {
                        // 10% chance for kanroji_sword_overhead
                        animationName = "kanroji_sword_overhead";
                    } else if (roll < 30) {
                        animationName = "sword_rotate";
                    } else {
                        // 80% chance for normal alternating attacks
                        animationName = lastWasLeft ? "sword_to_right" : "sword_to_left";
                        lastWasLeft = !lastWasLeft;
                    }
                } else {
                    // Default behavior for other breathing swords (and base mod swords as fallback)
                    // 8% chance for overhead animation, otherwise alternate left/right
                    if (RANDOM.nextInt(100) < 8) {
                        animationName = "sword_overhead";
                    } else {
                        animationName = lastWasLeft ? "sword_to_right" : "sword_to_left";
                        lastWasLeft = !lastWasLeft;
                    }
                }

                // Determine max duration based on animation type
                int maxDurationTicks;
                if (animationName.equals("kanroji_sword_overhead")) {
                    // Kanroji overhead is ~1 second (20 ticks)
                    maxDurationTicks = 20;
                } else if (animationName.equals("sprint") || animationName.equals("sprint2")) {
                    // Sprint animations should play fully (don't cut off)
                    maxDurationTicks = -1;
                } else {
                    // Other attacks: 10 ticks (0.5 seconds)
                    maxDurationTicks = 10;
                }

                // Play the animation
                com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.AnimationHelper.playAnimation(
                    player, animationName, maxDurationTicks
                );

                if (com.lerdorf.kimetsunoyaibamultiplayer.Config.logDebug) {
                    String swordType = isBaseModSword ? "base mod sword (fallback)" : "our sword";
                    com.lerdorf.kimetsunoyaibamultiplayer.Log.debug("[BreathingSwordAnimationHandler] onAttack -> requested player animation '{}' for {} and triggered sword mapping", animationName, swordType);
                }

                return animationName;
            }
        } catch (Exception e) {
            // Silently catch all exceptions to prevent crashes
        }
        return null;
    }

    /**
     * Check if the player is currently playing a sword-related animation.
     * Used to detect if the base mod's animation system is working.
     *
     * @param player The player to check
     * @return true if a sword animation is currently playing
     */
    private static boolean isPlayerPlayingSwordAnimation(AbstractClientPlayer player) {
        return true; // HACK
        /*
        try {
            dev.kosmx.playerAnim.api.layered.AnimationStack animationStack =
                dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess.getPlayerAnimLayer(player);

            if (animationStack == null) {
                return false;
            }

            // Use reflection to get layers
            java.lang.reflect.Field layersField = dev.kosmx.playerAnim.api.layered.AnimationStack.class.getDeclaredField("layers");
            layersField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.List<dev.kosmx.playerAnim.core.util.Pair<Integer, dev.kosmx.playerAnim.api.layered.IAnimation>> layers =
                (java.util.List<dev.kosmx.playerAnim.core.util.Pair<Integer, dev.kosmx.playerAnim.api.layered.IAnimation>>) layersField.get(animationStack);

            for (dev.kosmx.playerAnim.core.util.Pair<Integer, dev.kosmx.playerAnim.api.layered.IAnimation> pair : layers) {
                dev.kosmx.playerAnim.api.layered.IAnimation anim = pair.getRight();

                if (anim != null && anim.isActive()) {
                    // Try to extract animation name
                    String animName = extractAnimationName(anim);
                    if (animName != null && animName.toLowerCase().contains("sword")) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // Silently fail - assume no animation is playing
        }
        return false;
        */
    }

    /**
     * Extract the animation name from an IAnimation.
     */
    private static String extractAnimationName(dev.kosmx.playerAnim.api.layered.IAnimation anim) {
        try {
            // Check if it's a ModifierLayer wrapping another animation
            if (anim instanceof dev.kosmx.playerAnim.api.layered.ModifierLayer) {
                dev.kosmx.playerAnim.api.layered.ModifierLayer<?> modLayer = (dev.kosmx.playerAnim.api.layered.ModifierLayer<?>) anim;
                dev.kosmx.playerAnim.api.layered.IAnimation innerAnim = modLayer.getAnimation();
                if (innerAnim != null) {
                    return extractAnimationName(innerAnim);
                }
            }

            // Check if it's a KeyframeAnimationPlayer
            if (anim instanceof dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer) {
                dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer animPlayer =
                    (dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer) anim;
                dev.kosmx.playerAnim.core.data.KeyframeAnimation data = animPlayer.getData();

                if (data != null) {
                    // Try to get name from extraData
                    java.lang.reflect.Field extraDataField = dev.kosmx.playerAnim.core.data.KeyframeAnimation.class.getDeclaredField("extraData");
                    extraDataField.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> extraData = (java.util.Map<String, Object>) extraDataField.get(data);
                    if (extraData != null) {
                        Object name = extraData.get("name");
                        if (name instanceof String) {
                            return (String) name;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Silently fail
        }
        return null;
    }
}
