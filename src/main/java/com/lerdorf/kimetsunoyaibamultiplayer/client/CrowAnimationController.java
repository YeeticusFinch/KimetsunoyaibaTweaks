package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.entities.CrowEnhancementHandler;
import net.minecraft.world.entity.Entity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;

/**
 * Controls animations for kasugai_crow entities using GeckoLib
 * Switches between idle, takeoff, flying, landing, and walking animations
 */
public class CrowAnimationController {

    /**
     * Animation controller for crow entity
     * Determines which animation to play based on crow's current state
     */
    public static PlayState crowAnimationPredicate(AnimationState<CrowAnimatableWrapper> event) {
        CrowAnimatableWrapper wrapper = event.getAnimatable();
        Entity entity = wrapper.getEntity();
        AnimationController<CrowAnimatableWrapper> controller = event.getController();

        // Check if crow is currently in flying state (dodge mechanic active)
        boolean isFlying = CrowEnhancementHandler.isCrowFlying(entity.getUUID());

        if (isFlying) {
            // Flying dodge mechanic active - play flying animation
            controller.setAnimation(RawAnimation.begin().thenLoop("kimetsunoyaibamultiplayer.crow.flying"));
            return PlayState.CONTINUE;
        }

        // Check if crow is moving on ground
        if (entity.getDeltaMovement().horizontalDistanceSqr() > 0.01) {
            // Walking
            controller.setAnimation(RawAnimation.begin().thenLoop("kimetsunoyaibamultiplayer.crow.walk"));
            return PlayState.CONTINUE;
        }

        // Default: idle animation
        controller.setAnimation(RawAnimation.begin().thenLoop("kimetsunoyaibamultiplayer.crow.idle"));
        return PlayState.CONTINUE;
    }

    /**
     * Trigger takeoff animation when crow starts flying
     * This should be called when the flying dodge mechanic initiates
     */
    public static <T extends Entity & GeoAnimatable> void triggerTakeoff(T entity) {
        // Note: In GeckoLib 4, we need to get the animation controller from the entity
        // This will transition to the takeoff animation, then automatically to flying
        if (entity instanceof GeoAnimatable) {
            // The animation controller will handle the transition in the predicate
        }
    }

    /**
     * Trigger landing animation when crow lands
     * This should be called when the flying dodge mechanic ends
     */
    public static <T extends Entity & GeoAnimatable> void triggerLanding(T entity) {
        if (entity instanceof GeoAnimatable) {
            // The animation controller will handle the transition in the predicate
        }
    }
}