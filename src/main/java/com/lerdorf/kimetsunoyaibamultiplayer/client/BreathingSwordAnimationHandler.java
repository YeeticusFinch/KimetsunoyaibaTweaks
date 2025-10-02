package com.lerdorf.kimetsunoyaibamultiplayer.client;

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
     */
    public static void onAttack(AbstractClientPlayer player) {
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);

        if (mainHand.getItem() instanceof BreathingSwordItem) {
            long currentTime = System.currentTimeMillis();

            // Prevent animation spam
            if (currentTime - lastAttackTime < 100) {
                return;
            }
            lastAttackTime = currentTime;

            // 5% chance for overhead animation, otherwise alternate left/right
            String animationName;
            if (RANDOM.nextInt(100) < 5) {
                animationName = "sword_overhead";
            } else {
                animationName = lastWasLeft ? "sword_to_right" : "sword_to_left";
                lastWasLeft = !lastWasLeft;
            }

            // Play the animation with 10 tick max duration (cancel after 10 ticks)
            com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.AnimationHelper.playAnimation(
                player, animationName, 10
            );
        }
    }
}
