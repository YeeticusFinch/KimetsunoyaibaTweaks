package com.lerdorf.kimetsunoyaibamultiplayer.entities.ai;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.KanrojiEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Front flip ability for Kanroji - aggressive mobility to close distance with targets.
 * Occasionally performs a forward flip to get closer to distant targets.
 *
 * Behavior:
 * - Triggers when target is 15-25 blocks away (too far for optimal range)
 * - Leaps forward and upward with front_flip animation
 * - Plays kanroji_sword_overhead animation on held sword
 * - Used to close distance gaps quickly
 */
public class KanrojiFrontFlipGoal extends Goal {
    private final KanrojiEntity entity;
    private int nextAllowedTick = 0;

    // Distance range for front flip (outside optimal attack range)
    private static final double MIN_FLIP_DISTANCE = 15.0; // Outside attack range
    private static final double MAX_FLIP_DISTANCE = 25.0; // Not too far
    private static final double MIN_FLIP_DISTANCE_SQ = MIN_FLIP_DISTANCE * MIN_FLIP_DISTANCE;
    private static final double MAX_FLIP_DISTANCE_SQ = MAX_FLIP_DISTANCE * MAX_FLIP_DISTANCE;

    // Cooldown between front flips
    private static final int BASE_COOLDOWN_TICKS = 80; // 4 seconds

    public KanrojiFrontFlipGoal(KanrojiEntity entity) {
        this.entity = entity;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = entity.getTarget();
        if (target == null || !target.isAlive()) return false;
        if (entity.level().isClientSide) return false;

        // Don't flip during transformation
        if (entity.isTransforming()) return false;

        // Respect cooldown
        if (entity.tickCount < nextAllowedTick) return false;

        // Don't flip while already doing an animation
        if (entity.getAnimationTicks() > 0) return false;

        // Check if target is in the correct distance range
        double distSq = entity.distanceToSqr(target);
        if (distSq < MIN_FLIP_DISTANCE_SQ || distSq > MAX_FLIP_DISTANCE_SQ) return false;

        // Chance gate - occasional use (evaluate every 10 ticks)
        if (entity.tickCount % 10 != 0) return false;
        return entity.getRandom().nextFloat() < 0.25f; // 25% chance when in range
    }

    @Override
    public boolean canContinueToUse() {
        return false; // one-shot
    }

    @Override
    public void start() {
        LivingEntity target = entity.getTarget();
        if (target == null) return;

        // Compute forward direction toward target
        Vec3 toTarget = target.position().subtract(entity.position()).normalize();
        if (toTarget.lengthSqr() < 1.0e-4) {
            // Fallback to look direction
            toTarget = entity.getLookAngle();
        }

        // Propel forward and upward (athletic leap)
        double forwardSpeed = 1.5; // Strong leap forward
        double upBoost = 0.6;       // High jump
        Vec3 motion = new Vec3(toTarget.x * forwardSpeed, upBoost, toTarget.z * forwardSpeed);
        entity.setDeltaMovement(motion);
        entity.hurtMarked = true;

        // Play front flip animation
        // The entity animation triggers the sword animation via EntityKanrojiSwordAnimationSync
        // which watches for "front_flip" and plays "kanroji_sword_overhead" on the sword
        entity.playGeckoAnimation("front_flip", 15);

        Log.debug("[KanrojiFrontFlipGoal] Front flip toward target (distance: {} blocks)",
            Math.sqrt(entity.distanceToSqr(target)));

        // Next allowed time randomized (base ±20 ticks)
        int jitter = entity.getRandom().nextInt(41) - 20; // [-20, +20]
        nextAllowedTick = entity.tickCount + Math.max(40, BASE_COOLDOWN_TICKS + jitter);
    }
}
