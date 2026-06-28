package com.lerdorf.kimetsunoyaibamultiplayer.entities.ai;

import com.lerdorf.kimetsunoyaibamultiplayer.entities.DemonSlayerEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Front-flip mobility for higher-level demon slayers.
 * Used occasionally to close distance to a target.
 */
public class DemonSlayerFrontFlipGoal extends Goal {
    private final DemonSlayerEntity entity;
    private int nextAllowedTick = 0;

    private static final double MIN_FLIP_DISTANCE = 6.0D;
    private static final double MAX_FLIP_DISTANCE = 20.0D;
    private static final double MIN_FLIP_DISTANCE_SQ = MIN_FLIP_DISTANCE * MIN_FLIP_DISTANCE;
    private static final double MAX_FLIP_DISTANCE_SQ = MAX_FLIP_DISTANCE * MAX_FLIP_DISTANCE;
    private static final int BASE_COOLDOWN_TICKS = 75;

    public DemonSlayerFrontFlipGoal(DemonSlayerEntity entity) {
        this.entity = entity;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (entity.level().isClientSide) return false;
        if (entity.getPowerLevel() < 3) return false;
        if (entity.isActionLocked() || entity.isDisarmed()) return false;
        if (entity.tickCount < nextAllowedTick) return false;
        if (entity.getAnimationTicks() > 0) return false;

        LivingEntity target = entity.getTarget();
        if (target == null || !target.isAlive()) return false;

        double distSq = entity.distanceToSqr(target);
        if (distSq < MIN_FLIP_DISTANCE_SQ || distSq > MAX_FLIP_DISTANCE_SQ) return false;

        if (entity.tickCount % 8 != 0) return false;
        return entity.getRandom().nextFloat() < 0.20f;
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public void start() {
        LivingEntity target = entity.getTarget();
        if (target == null) return;

        Vec3 toTarget = target.position().subtract(entity.position()).normalize();
        if (toTarget.lengthSqr() < 1.0e-4) {
            toTarget = entity.getLookAngle();
        }

        double distance = Math.sqrt(entity.distanceToSqr(target));
        double forwardSpeed = Math.min(1.75D, 1.1D + distance * 0.03D);
        double upBoost = 0.58D;
        entity.setDeltaMovement(new Vec3(toTarget.x * forwardSpeed, upBoost, toTarget.z * forwardSpeed));
        entity.hurtMarked = true;
        entity.playGeckoAnimation("front_flip", 15);

        int jitter = entity.getRandom().nextInt(31) - 15;
        nextAllowedTick = entity.tickCount + Math.max(40, BASE_COOLDOWN_TICKS + jitter);
    }
}
