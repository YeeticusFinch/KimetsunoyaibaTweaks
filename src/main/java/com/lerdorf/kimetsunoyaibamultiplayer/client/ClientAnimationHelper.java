package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.SpeedControlledAnimation;
import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Client-only animation helper methods
 * This class is only loaded on the client side
 */
public class ClientAnimationHelper {

    /**
     * Play an animation on a player (CLIENT SIDE ONLY)
     * @return The animation layer, or null if failed
     */
    public static ModifierLayer<IAnimation> playAnimationOnPlayer(Player player, KeyframeAnimation animation, float speed, int layerPriority) {
        if (!(player instanceof AbstractClientPlayer clientPlayer)) {
            return null;
        }

        try {
            AnimationStack animationStack = PlayerAnimationAccess.getPlayerAnimLayer(clientPlayer);
            if (animationStack != null) {
                // Remove old layer first (only for the same priority)
                animationStack.removeLayer(layerPriority);

                KeyframeAnimationPlayer animPlayer = Math.abs(speed-1) > 0.01
                    ? new SpeedControlledAnimation(animation, speed)
                    : new KeyframeAnimationPlayer(animation);
                ModifierLayer<IAnimation> modifierLayer = new ModifierLayer<>();
                modifierLayer.setAnimation(animPlayer);
                animationStack.addAnimLayer(layerPriority, modifierLayer);
                return modifierLayer;
            }
        } catch (Exception e) {
            // Animation failed, continue without it
        }
        return null;
    }

    /**
     * Schedule animation cancellation after a delay (CLIENT SIDE ONLY)
     */
    public static void scheduleAnimationCancellation(ModifierLayer<IAnimation> layer, int delayTicks) {
        new Thread(() -> {
            try {
                Thread.sleep(delayTicks * 50); // 50ms per tick
                // Stop the animation by setting it to null
                layer.setAnimation(null);
            } catch (InterruptedException e) {
                // Cancellation interrupted
            }
        }).start();
    }
}
