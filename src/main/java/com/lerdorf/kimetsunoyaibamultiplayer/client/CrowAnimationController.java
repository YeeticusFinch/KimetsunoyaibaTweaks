package com.lerdorf.kimetsunoyaibamultiplayer.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.CrowEnhancementHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * Controls animations for kasugai_crow entities using GeckoLib
 * Switches between idle, takeoff, flying, landing, and walking animations
 */
public class CrowAnimationController {

	// Track previous state to detect transitions
	private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<UUID, Boolean> previousFlyingState = new HashMap<>();
    private static final Map<UUID, String> lastAnimationState = new HashMap<>();
    private static boolean hasLoggedAnimations = false;
    private static int tickCounter = 0;
    
    private enum CrowState {
        IDLE, WALKING, TAKEOFF, FLYING, LANDING
    }
	
    /**
     * Animation controller for crow entity
     * Determines which animation to play based on crow's current state
     */
    public static PlayState crowAnimationPredicate(AnimationState<CrowAnimatableWrapper> event) {
        CrowAnimatableWrapper wrapper = event.getAnimatable();
        Entity entity = wrapper.getEntity();
        AnimationController<CrowAnimatableWrapper> controller = event.getController();

        tickCounter++;

        // Log first call always
        if (!hasLoggedAnimations) {
            LOGGER.info("=== CROW ANIMATION PREDICATE CALLED ===");
            LOGGER.info("Entity: {}", entity.getName().getString());
            LOGGER.info("Controller: {}", controller.getName());
            hasLoggedAnimations = true;
        }
        
        // CHECK FOR DEATH FIRST - must be LivingEntity to have isDeadOrDying()
        if (entity instanceof net.minecraft.world.entity.LivingEntity livingEntity) {
            if (livingEntity.isDeadOrDying()) {
                String currentState = "DEATH";
                RawAnimation animation = RawAnimation.begin()
                    .then("kimetsunoyaibamultiplayer.crow.death", Animation.LoopType.HOLD_ON_LAST_FRAME);
                
                controller.setAnimation(animation);
                return PlayState.CONTINUE;
            }
        }

        // Get current flying state
        boolean isFlying = CrowEnhancementHandler.isCrowFlying(entity.getUUID());
        
        // Get previous flying state
        UUID entityId = entity.getUUID();
        boolean prevFlying = previousFlyingState.getOrDefault(entityId, false);
        
        // Detect state transitions
        boolean justStartedFlying = isFlying && !prevFlying;
        boolean justStoppedFlying = !isFlying && prevFlying;
        
        // Update previous state
        previousFlyingState.put(entityId, isFlying);
        
        // Get crow's movement
        Vec3 deltaMovement = entity.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(deltaMovement.x * deltaMovement.x + deltaMovement.z * deltaMovement.z);
        boolean isMovingOnGround = horizontalSpeed > 0.01 && entity.onGround();
        
        String currentState = "UNKNOWN";
        RawAnimation animation = null;
        
        // Determine current state and play appropriate animation
        if (justStartedFlying) {
            currentState = "TAKEOFF";
            animation = RawAnimation.begin()
                .then("kimetsunoyaibamultiplayer.crow.takeoff", Animation.LoopType.PLAY_ONCE)
                .thenLoop("kimetsunoyaibamultiplayer.crow.flying");
            if (Config.logDebug)
            	LOGGER.info("Crow {} TAKING OFF", entityId);
        } else if (justStoppedFlying) {
            currentState = "LANDING";
            animation = RawAnimation.begin()
                .then("kimetsunoyaibamultiplayer.crow.landing", Animation.LoopType.PLAY_ONCE)
                .thenLoop("kimetsunoyaibamultiplayer.crow.idle");
            if (Config.logDebug)
            	LOGGER.info("Crow {} LANDING", entityId);
        } else if (isFlying) {
            currentState = "FLYING";
            animation = RawAnimation.begin()
                .thenLoop("kimetsunoyaibamultiplayer.crow.flying");
        } else if (isMovingOnGround) {
            currentState = "WALKING";
            animation = RawAnimation.begin()
                .thenLoop("kimetsunoyaibamultiplayer.crow.walk");
        } else {
            currentState = "IDLE";
            animation = RawAnimation.begin()
                .thenLoop("kimetsunoyaibamultiplayer.crow.idle");
        }
        
        // Log state changes
        String lastState = lastAnimationState.get(entityId);
        if (!currentState.equals(lastState)) {
            if (Config.logDebug) {
                LOGGER.info("Crow {} animation state changed: {} -> {}",
                    entityId, lastState != null ? lastState : "NONE", currentState);
                LOGGER.info("  Flying: {}, OnGround: {}, Speed: {}",
                    isFlying, entity.onGround(), horizontalSpeed);
            }
            lastAnimationState.put(entityId, currentState);
        }
        
        // Set the animation
        try {
            controller.setAnimation(animation);
        } catch (Exception e) {
            LOGGER.error("Failed to set animation for state {}: {}", currentState, e.getMessage());
            // Try to set a simple idle animation as fallback
            controller.setAnimation(RawAnimation.begin().thenLoop("kimetsunoyaibamultiplayer.crow.idle"));
        }
        
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