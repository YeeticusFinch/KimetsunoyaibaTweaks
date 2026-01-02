package com.lerdorf.kimetsunoyaibamultiplayer.entities.ai;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.KanrojiEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Range management backstep for Kanroji.
 * Keeps Kanroji at optimal whip range (10-15 blocks) by backstepping when too close.
 *
 * Unlike Muichiro's dodge (which triggers on damage), this triggers on proximity:
 * - If target is within 10 blocks, chance to backstep away
 * - Goal is to maintain 10-15 block optimal whip attack range
 */
public class KanrojiBackstepGoal extends Goal {
    private final KanrojiEntity entity;
    private int nextAllowedTick = 0;

    // Range management
    private static final double MIN_PREFERRED_RANGE = 10.0; // Start backstepping when within this range
    private static final double MIN_PREFERRED_RANGE_SQ = MIN_PREFERRED_RANGE * MIN_PREFERRED_RANGE;

    // Minimum delay between backsteps (randomized around this base)
    private static final int BASE_COOLDOWN_TICKS = 40; // 2 seconds (faster than Muichiro's combat dodge)

    public KanrojiBackstepGoal(KanrojiEntity entity) {
        this.entity = entity;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = entity.getTarget();
        if (target == null || !target.isAlive()) return false; // only when fighting
        if (entity.level().isClientSide) return false;

        // Don't backstep during transformation
        if (entity.isTransforming()) return false;

        // Respect cooldown
        if (entity.tickCount < nextAllowedTick) return false;

        // Don't backstep while already doing a special animation (attack or breathing form)
        if (entity.getAnimationTicks() > 0) return false;

        // Check if target is too close (within 10 blocks)
        double distSq = entity.distanceToSqr(target);
        if (distSq >= MIN_PREFERRED_RANGE_SQ) return false;

        // Chance gate to make it occasional but responsive
        if (entity.tickCount % 3 != 0) return false; // evaluate every 3 ticks
        return entity.getRandom().nextFloat() < 0.45f; // 45% chance when too close
    }

    @Override
    public boolean canContinueToUse() {
        return false; // one-shot
    }

    @Override
    public void start() {
        LivingEntity target = entity.getTarget();
        if (target == null) return;

        // Compute backward direction away from target
        Vec3 away = entity.position().subtract(target.position()).normalize();
        if (away.lengthSqr() < 1.0e-4) {
            // fallback to opposite of look direction
            away = entity.getLookAngle().scale(-1.0);
        }

        // Propel backwards and slightly upward (graceful leap back)
        double backSpeed = 1.2; // strong leap back to create distance
        double upBoost = 0.4;   // graceful jump
        Vec3 motion = new Vec3(away.x * backSpeed, upBoost, away.z * backSpeed);
        entity.setDeltaMovement(motion);
        entity.hurtMarked = true; // ensure motion sync

        // Play backstep animation (short and elegant)
        entity.playGeckoAnimation("backstep", 10);

        Log.debug("[KanrojiBackstepGoal] Backstepping away from target (distance: {} blocks)",
            Math.sqrt(entity.distanceToSqr(target)));

        // Next allowed time randomized (base ±15 ticks)
        int jitter = entity.getRandom().nextInt(31) - 15; // [-15, +15]
        nextAllowedTick = entity.tickCount + Math.max(20, BASE_COOLDOWN_TICKS + jitter);
    }
}
