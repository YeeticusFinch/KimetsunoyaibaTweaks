package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem;
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

            if (mainHand.getItem() instanceof BreathingSwordItem) {
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
                lastAttackTime = currentTime;

                String animationName;

                // Special handling for Kanroji sword - uses both sword_overhead and kanroji_sword_overhead
                if (mainHand.getItem() instanceof com.lerdorf.kimetsunoyaibamultiplayer.items.NichirinSwordKanrojiAnimated) {
                    int roll = RANDOM.nextInt(100);
                    if (roll < 10) {
                        // 10% chance for sword_overhead
                        animationName = "sword_overhead";
                    } else if (roll < 20) {
                        // 10% chance for kanroji_sword_overhead
                        animationName = "kanroji_sword_overhead";
                    } else {
                        // 80% chance for normal alternating attacks
                        animationName = lastWasLeft ? "sword_to_right" : "sword_to_left";
                        lastWasLeft = !lastWasLeft;
                    }
                } else {
                    // Default behavior for other breathing swords
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
                    com.lerdorf.kimetsunoyaibamultiplayer.Log.debug("[BreathingSwordAnimationHandler] onAttack -> requested player animation '{}' and triggered sword mapping", animationName);
                }

                return animationName;
            }
        } catch (Exception e) {
            // Silently catch all exceptions to prevent crashes
        }
        return null;
    }
}
