package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * Helper class for playing PlayerAnimator animations on mobs
 * Uses Mob Player Animator library to enable player animations on PathfinderMob entities
 */
public class MobAnimationHelper {

    /**
     * Play a PlayerAnimator animation on a mob entity
     * This uses the Mob Player Animator library to apply player animations to mobs
     *
     * @param entity The mob to animate (must be a PathfinderMob or compatible)
     * @param animationName The animation name (e.g., "sword_to_left", "ragnaraku1")
     */
    public static void playAnimationOnMob(LivingEntity entity, String animationName) {
        if (!entity.level().isClientSide) {
            return; // Only run on client
        }

        // Parse animation name into ResourceLocation
        ResourceLocation animLocation = parseAnimationName(animationName);

        // Get animation from registry
        KeyframeAnimation animation = findAnimation(animLocation);
        if (animation == null) {
            System.err.println("[MobAnimationHelper] Animation not found: " + animLocation);
            return;
        }

        try {
            // Mob Player Animator makes PlayerAnimationAccess work with any LivingEntity
            // We need to cast to Object first to bypass Java's type checking,
            // then Mob Player Animator's runtime magic makes it work
            @SuppressWarnings("unchecked")
            var animationContainer = PlayerAnimationAccess.getPlayerAnimLayer((net.minecraft.client.player.AbstractClientPlayer)(Object)entity);

            if (animationContainer != null) {
                // Remove any existing animation on default layer
                animationContainer.removeLayer(3000);

                // Create and add new animation
                KeyframeAnimationPlayer animPlayer = new KeyframeAnimationPlayer(animation);
                ModifierLayer<IAnimation> modifierLayer = new ModifierLayer<>();
                modifierLayer.setAnimation(animPlayer);
                animationContainer.addAnimLayer(3000, modifierLayer);
            }
        } catch (Exception e) {
            System.err.println("[MobAnimationHelper] Failed to play animation on mob: " + e.getMessage());
        }
    }

    /**
     * Overload for compatibility with breathing forms
     */
    public static void playAnimation(LivingEntity entity, String animationName) {
        playAnimationOnMob(entity, animationName);
    }

    /**
     * Overload with layer priority support (not fully implemented for mobs yet)
     */
    public static void playAnimationOnLayer(LivingEntity entity, String animationName, int maxTicks, float speed, int layerPriority) {
        // For now, just play the animation with default settings
        // TODO: Implement speed and layer priority for mob animations
        playAnimationOnMob(entity, animationName);
    }

    private static ResourceLocation parseAnimationName(String animationName) {
        if (animationName.contains(":")) {
            String[] parts = animationName.split(":", 2);
            return ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]);
        }
        return ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", animationName);
    }

    private static KeyframeAnimation findAnimation(ResourceLocation animationLocation) {
        // Try to get from registry
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
}
