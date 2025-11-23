package com.lerdorf.kimetsunoyaibamultiplayer.entities.ai;

import com.lerdorf.kimetsunoyaibamultiplayer.entities.MuichiroEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Occasional evasive backstep for Muichiro.
 * Plays the "backstep" GeckoLib animation and propels the entity backward/upward
 * to dodge attacks. Triggers only occasionally when in combat.
 */
public class MuichiroBackstepGoal extends Goal {
    private final MuichiroEntity entity;
    private int nextAllowedTick = 0;

    // Minimum delay between backsteps (randomized around this base)
    private static final int BASE_COOLDOWN_TICKS = 60; // 3 seconds

    public MuichiroBackstepGoal(MuichiroEntity entity) {
        this.entity = entity;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = entity.getTarget();
        if (target == null || !target.isAlive()) return false; // only when fighting
        if (entity.level().isClientSide) return false;

        // Respect cooldown
        if (entity.tickCount < nextAllowedTick) return false;

        // Don't backstep while already doing a special animation
        if (entity.getAnimationTicks() > 0) return false;

        // Only consider backstep if recently took damage (last 20 ticks)
        if (!entity.wasRecentlyDamaged(20)) return false;

        // Chance gate to make it occasional
        if (entity.tickCount % 5 != 0) return false; // evaluate every 5 ticks for responsiveness
        return entity.getRandom().nextFloat() < 0.35f; // 35% chance when recently damaged
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

        // Propel backwards and slightly upward
        double backSpeed = 1.0; // strong hop back
        double upBoost = 0.5;   // jump up
        Vec3 motion = new Vec3(away.x * backSpeed, upBoost, away.z * backSpeed);
        entity.setDeltaMovement(motion);
        entity.hurtMarked = true; // ensure motion sync

        // Play backstep animation (short and snappy)
        entity.playGeckoAnimation("backstep", 8);

        // Next allowed time randomized (base ±20 ticks)
        int jitter = entity.getRandom().nextInt(41) - 20; // [-20, +20]
        nextAllowedTick = entity.tickCount + Math.max(20, BASE_COOLDOWN_TICKS + jitter);
    }
}
