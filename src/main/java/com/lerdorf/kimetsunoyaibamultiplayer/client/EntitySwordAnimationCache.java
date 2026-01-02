package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.items.NichirinSwordKanrojiAnimated;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side cache for entity sword animations.
 *
 * This cache stores ONE AnimationController per entity (not per ItemStack).
 * This ensures that all renders of the same entity's sword use the same
 * controller instance, preventing the flickering issue where different
 * ItemStack instances have different animation states.
 */
public class EntitySwordAnimationCache {

    private static class EntityAnimationController {
        String currentAnimation = "idle";
        String lastSetAnimation = "idle";
        AnimationController<NichirinSwordKanrojiAnimated> controller;
        long lastUpdateTime = 0;

        EntityAnimationController(NichirinSwordKanrojiAnimated swordItem) {
            // Create a shared controller for this entity
            controller = new AnimationController<>(swordItem, "entity_sword", 0, state -> {
                // Check if animation needs to change
                if (state.getController().getCurrentAnimation() != null) {
                    String currentAnim = state.getController().getCurrentAnimation().animation().name();
                    if (currentAnim != null && currentAnim.equals(currentAnimation)) {
                        // Same animation - continue playing
                        return software.bernie.geckolib.core.object.PlayState.CONTINUE;
                    }
                }

                // Animation changed - start new animation
                boolean isLooping = currentAnimation.equals("idle") || currentAnimation.equals("walk") ||
                                   currentAnimation.equals("sprint") || currentAnimation.equals("sheath");

                if (isLooping) {
                    return state.setAndContinue(RawAnimation.begin().thenLoop(currentAnimation));
                } else {
                    return state.setAndContinue(RawAnimation.begin().thenPlay(currentAnimation).thenLoop("idle"));
                }
            });
        }

        void setAnimation(String animName) {
            if (!animName.equals(lastSetAnimation)) {
                currentAnimation = animName;
                lastSetAnimation = animName;
                lastUpdateTime = System.currentTimeMillis();
            }
        }
    }

    private static final Map<UUID, EntityAnimationController> entityControllers = new ConcurrentHashMap<>();

    /**
     * Set the animation for an entity's sword.
     * Note: This only sets the animation string. The controller must be created
     * separately by calling getController() first.
     */
    public static void setAnimation(UUID entityUUID, String animationName) {
        EntityAnimationController ctrl = entityControllers.get(entityUUID);
        if (ctrl != null) {
            ctrl.setAnimation(animationName);
            Log.debug("[EntitySwordAnimationCache] Set animation for entity {} to: {}", entityUUID, animationName);
        } else {
            // Controller doesn't exist yet - store the animation for when it's created
            Log.debug("[EntitySwordAnimationCache] WARNING: Controller for entity {} doesn't exist yet, cannot set animation to {}",
                      entityUUID, animationName);
        }
    }

    /**
     * Set the animation and ensure the controller exists.
     */
    public static void setAnimationAndEnsureController(UUID entityUUID, String animationName, NichirinSwordKanrojiAnimated swordItem) {
        EntityAnimationController ctrl = entityControllers.computeIfAbsent(entityUUID,
            id -> new EntityAnimationController(swordItem));
        ctrl.setAnimation(animationName);
        Log.debug("[EntitySwordAnimationCache] Set animation for entity {} to: {}", entityUUID, animationName);
    }

    /**
     * Set the animation and ensure the controller exists.
     */
    public static void setAnimationAndEnsureController(LivingEntity entity, String animationName, NichirinSwordKanrojiAnimated swordItem) {
        setAnimationAndEnsureController(entity.getUUID(), animationName, swordItem);
    }

    /**
     * Set the animation for an entity's sword.
     */
    public static void setAnimation(LivingEntity entity, String animationName) {
        setAnimation(entity.getUUID(), animationName);
    }

    /**
     * Get the animation for an entity's sword.
     * Returns "idle" if no animation is set.
     */
    public static String getAnimation(UUID entityUUID) {
        EntityAnimationController ctrl = entityControllers.get(entityUUID);
        String result = ctrl != null ? ctrl.currentAnimation : "idle";
        Log.debug("[EntitySwordAnimationCache] getAnimation({}) returning: {}", entityUUID, result);
        return result;
    }

    /**
     * Get the animation for an entity's sword.
     * Returns "idle" if no animation is set.
     */
    public static String getAnimation(LivingEntity entity) {
        return getAnimation(entity.getUUID());
    }

    /**
     * Get the shared AnimationController for an entity.
     * This is the ONE controller that all ItemStack renders should use.
     * Creates the controller if it doesn't exist.
     */
    public static AnimationController<NichirinSwordKanrojiAnimated> getController(UUID entityUUID, NichirinSwordKanrojiAnimated swordItem) {
        EntityAnimationController ctrl = entityControllers.computeIfAbsent(entityUUID,
            id -> {
                Log.debug("[EntitySwordAnimationCache] Creating new controller for entity {}", entityUUID);
                return new EntityAnimationController(swordItem);
            });
        Log.debug("[EntitySwordAnimationCache] getController() for entity {} - currentAnimation field: {}",
                  entityUUID, ctrl.currentAnimation);
        return ctrl.controller;
    }

    /**
     * Get the shared AnimationController for an entity.
     */
    public static AnimationController<NichirinSwordKanrojiAnimated> getController(LivingEntity entity, NichirinSwordKanrojiAnimated swordItem) {
        return getController(entity.getUUID(), swordItem);
    }

    /**
     * Clear the animation for an entity (when entity is removed).
     */
    public static void clearAnimation(UUID entityUUID) {
        entityControllers.remove(entityUUID);
    }

    /**
     * Clear the animation for an entity (when entity is removed).
     */
    public static void clearAnimation(LivingEntity entity) {
        clearAnimation(entity.getUUID());
    }
}
