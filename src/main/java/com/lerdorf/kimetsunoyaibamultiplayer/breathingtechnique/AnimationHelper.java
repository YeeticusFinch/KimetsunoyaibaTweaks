package com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique;

import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.AnimationSyncPacket;
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
        playAnimation(player, animationName, -1);
    }

    /**
     * Play an animation on a player with a maximum duration in ticks
     * @param player The player to play the animation on
     * @param animationName The name of the animation
     * @param maxDurationTicks Maximum duration in ticks (-1 for full animation)
     */
    public static void playAnimation(Player player, String animationName, int maxDurationTicks) {
        ResourceLocation animationLocation = parseAnimationName(animationName);

        // If on client, play locally and sync to server
        if (player.level().isClientSide && player instanceof AbstractClientPlayer clientPlayer) {
            KeyframeAnimation animation = findAnimation(animationLocation);
            if (animation != null) {
                ModifierLayer<IAnimation> layer = playAnimationOnPlayer(clientPlayer, animation);

                // Schedule animation cancellation if maxDurationTicks is specified
                if (maxDurationTicks > 0 && layer != null) {
                    scheduleAnimationCancellation(clientPlayer, layer, maxDurationTicks);
                }

                // Send to server for other players
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

    private static ModifierLayer<IAnimation> playAnimationOnPlayer(AbstractClientPlayer player, KeyframeAnimation animation) {
        try {
            AnimationStack animationStack = PlayerAnimationAccess.getPlayerAnimLayer(player);
            if (animationStack != null) {
                KeyframeAnimationPlayer animPlayer = new KeyframeAnimationPlayer(animation);
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
