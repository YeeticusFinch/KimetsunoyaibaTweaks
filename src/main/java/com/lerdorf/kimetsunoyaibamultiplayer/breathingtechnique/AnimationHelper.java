package com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique;

import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.AnimationSyncPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.SpeedControlledAnimation;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Helper class for playing animations on players
 */
public class AnimationHelper {

    /**
     * Play an animation on a player (both client and server)
     */
    public static void playAnimation(Player player, String animationName) {
        playAnimation(player, animationName, -1, 1.0f);
    }

    /**
     * Play an animation on a player with a maximum duration in ticks
     * @param player The player to play the animation on
     * @param animationName The name of the animation
     * @param maxDurationTicks Maximum duration in ticks (-1 for full animation)
     */
    public static void playAnimation(Player player, String animationName, int maxDurationTicks) {
        playAnimation(player, animationName, maxDurationTicks, 1.0f);
    }
    
    /**
     * Play an animation with optional max duration and playback speed.
     * @param player The player to animate
     * @param animationName The animation name or path
     * @param maxDurationTicks Maximum duration in ticks (-1 for full animation)
     * @param speed Playback speed multiplier (1.0 = normal, 2.0 = double speed, 0.5 = half speed)
     */
    public static void playAnimation(Player player, String animationName, int maxDurationTicks, float speed) {
        ResourceLocation animationLocation = parseAnimationName(animationName);
        KeyframeAnimation animation = findAnimation(animationLocation);

        if (animation == null) {
            return; // Animation not found
        }

        // CLIENT SIDE: Play locally
        if (player.level().isClientSide && player instanceof AbstractClientPlayer clientPlayer) {
            ModifierLayer<IAnimation> layer = playAnimationOnPlayer(clientPlayer, animation, speed);

            if (maxDurationTicks > 0 && layer != null) {
                scheduleAnimationCancellation(clientPlayer, layer, maxDurationTicks);
            }

            // Send to server so other players see it
            AnimationSyncPacket packet = new AnimationSyncPacket(
                player.getUUID(),
                animationLocation,
                0,
                30,
                false,
                false,
                animation
            );
            ModNetworking.sendToServer(packet);
        }
        // SERVER SIDE: Send to all clients (including the player themselves)
        else if (!player.level().isClientSide) {
            AnimationSyncPacket packet = new AnimationSyncPacket(
                player.getUUID(),
                animationLocation,
                0,
                30,
                false,
                false,
                animation
            );
            ModNetworking.sendToAllClients(packet);
        }
    }


    private static ResourceLocation parseAnimationName(String animationName) {
        if (animationName.contains(":")) {
            String[] parts = animationName.split(":", 2);
            return ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]);
        }
        return ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", animationName);
    }

    private static KeyframeAnimation findAnimation(ResourceLocation animationLocation) {
        KeyframeAnimation anim = PlayerAnimationRegistry.getAnimation(animationLocation);
        if (anim != null) {
            return anim;
        }

        // Try alternative namespaces
        String path = animationLocation.getPath();
        ResourceLocation[] alternativeLocations = {
            ResourceLocation.fromNamespaceAndPath("playeranimator", path),
            ResourceLocation.fromNamespaceAndPath("minecraft", path),
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

    private static ModifierLayer<IAnimation> playAnimationOnPlayer(AbstractClientPlayer player, KeyframeAnimation animation, float speed) {
        try {
            AnimationStack animationStack = PlayerAnimationAccess.getPlayerAnimLayer(player);
            if (animationStack != null) {
            	
            	// Remove old layer first
                animationStack.removeLayer(3000);

                KeyframeAnimationPlayer animPlayer = Math.abs(speed-1) > 0.01 ? new SpeedControlledAnimation(animation, speed) : new KeyframeAnimationPlayer(animation);
                ModifierLayer<IAnimation> modifierLayer = new ModifierLayer<>();
                modifierLayer.setAnimation(animPlayer);
                animationStack.addAnimLayer(3000, modifierLayer);
                return modifierLayer;
            }
        } catch (Exception e) {
            // Animation failed, continue without it
        }
        return null;
    }

    /**
     * Schedule animation cancellation after a delay
     * @param player The player
     * @param layer The animation layer to cancel
     * @param delayTicks Delay in ticks before cancellation
     */
    private static void scheduleAnimationCancellation(AbstractClientPlayer player, ModifierLayer<IAnimation> layer, int delayTicks) {
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
