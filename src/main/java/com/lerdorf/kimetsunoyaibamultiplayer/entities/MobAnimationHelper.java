package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;

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

        // Parse animation name into ResourceLocation and resolve from registry
        ResourceLocation animLocation = parseAnimationName(animationName);
        KeyframeAnimation animation = findAnimation(animLocation);
        if (animation == null) {
            System.err.println("[MobAnimationHelper] Animation not found: " + animLocation);
            return;
        }

        // Prefer PlayerAnimator's entity-layer access if available (MobPlayerAnimator exposes this)
        // Fallback to legacy getAnimationStack mixin if present on the entity class.
        try {
            Object animationStackObj = null;

            // Attempt: PlayerAnimationAccess.getPlayerAnimLayer(LivingEntity)
            boolean usedEntityLayerApi = false;
            try {
                java.lang.reflect.Method entityLayerMethod =
                        dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess.class
                                .getMethod("getPlayerAnimLayer", net.minecraft.world.entity.LivingEntity.class);
                animationStackObj = entityLayerMethod.invoke(null, entity);
                usedEntityLayerApi = (animationStackObj != null);
                if (usedEntityLayerApi) {
                    Log.debug("[MobAnimationHelper] [DEBUG] Found animation stack via entity-layer API");
                }
            } catch (Throwable e) {
                Log.debug("[MobAnimationHelper] [DEBUG] Entity-layer API not available: " + e.getClass().getSimpleName());
            }

            if (animationStackObj == null) {
                // Fallback: entity.getAnimationStack() provided by MobPlayerAnimator mixin
                try {
                    java.lang.reflect.Method getAnimStackMethod = entity.getClass().getMethod("getAnimationStack");
                    animationStackObj = getAnimStackMethod.invoke(entity);
                    if (animationStackObj != null) {
                        Log.debug("[MobAnimationHelper] Using mixin getAnimationStack() for entity: " + entity.getClass().getName());
                    } else {
                        System.err.println("[MobAnimationHelper] [DEBUG] getAnimationStack() returned null for: " + entity.getClass().getName());
                    }
                } catch (NoSuchMethodException e) {
                    System.err.println("[MobAnimationHelper] [DEBUG] No getAnimationStack() method on: " + entity.getClass().getName());
                } catch (Throwable e) {
                    System.err.println("[MobAnimationHelper] [DEBUG] Error invoking getAnimationStack(): " + e.getMessage());
                }
            }

            if (animationStackObj instanceof dev.kosmx.playerAnim.api.layered.AnimationStack animationStack) {
                // Remove any existing animation on our mob layer (1000)
                animationStack.removeLayer(1000);

                // Create and add new animation
                KeyframeAnimationPlayer animPlayer = new KeyframeAnimationPlayer(animation);
                ModifierLayer<IAnimation> modifierLayer = new ModifierLayer<>();
                modifierLayer.setAnimation(animPlayer);
                animationStack.addAnimLayer(1000, modifierLayer);

                if (usedEntityLayerApi) {
                    Log.debug("[MobAnimationHelper] Applied animation via entity-layer API to: " + entity.getName().getString());
                }
                Log.debug("[MobAnimationHelper] [DEBUG] Successfully added animation to stack");
            } else {
                System.err.println("[MobAnimationHelper] No compatible animation layer on entity: " + entity.getClass().getName());
                System.err.println("[MobAnimationHelper] [DEBUG] animationStackObj type: " +
                    (animationStackObj != null ? animationStackObj.getClass().getName() : "null"));
            }
        } catch (Exception e) {
            System.err.println("[MobAnimationHelper] Failed to play animation on mob: " + e.getMessage());
            e.printStackTrace();
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
        // Default without namespace; the finder will try multiple namespaces
        return ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", animationName);
    }

    private static KeyframeAnimation findAnimation(ResourceLocation animationLocation) {
        // Try to get from registry
        KeyframeAnimation anim = PlayerAnimationRegistry.getAnimation(animationLocation);
        if (anim != null) {
            return anim;
        }

        // Try alternative namespaces (order matters)
        String path = animationLocation.getPath();
        ResourceLocation[] alternativeLocations = {
            // Mod’s own namespace first, then base mod, then playeranimator/minecraft, then base mod subpath
            ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", path),
            ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", path),
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
