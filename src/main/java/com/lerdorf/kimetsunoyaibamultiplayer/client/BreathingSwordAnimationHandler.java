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

                // Check if golden sword is equipped
                boolean goldenMode = mainHand.getItem() instanceof
                    com.lerdorf.kimetsunoyaibamultiplayer.items.NichirinSwordGolden;

                String animationName;
                if (goldenMode) {
                    // Golden mode: cycle through all attack animations
                    String[] goldenAnimations = {
                        "sword_to_left",
                        "sword_to_right",
                        "sword_rotate",
                        "sword_to_upper",
                        "sword_overhead",
                        "speed_attack_sword"
                    };

                    // Use time-based cycling for variety
                    int index = (int)((currentTime / 300) % goldenAnimations.length);
                    animationName = goldenAnimations[index];
                } else {
                    // Normal mode: 5% chance for overhead animation, otherwise alternate left/right
                    if (RANDOM.nextInt(100) < 8) {
                        animationName = "sword_overhead";
                    } else {
                        animationName = lastWasLeft ? "sword_to_right" : "sword_to_left";
                        lastWasLeft = !lastWasLeft;
                    }
                }

                // Play the animation with 10 tick max duration (cancel after 10 ticks)
                com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.AnimationHelper.playAnimation(
                    player, animationName, 10
                );

                return animationName;
            }
        } catch (Exception e) {
            // Silently catch all exceptions to prevent crashes
        }
        return null;
    }
}
