package com.lerdorf.kimetsunoyaibamultiplayer.entities.ai;

import com.lerdorf.kimetsunoyaibamultiplayer.entities.DemonSlayerEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Dodge/backstep behavior for higher-level demon slayers.
 * Active only for power level 3+ and when recently damaged.
 */
public class DemonSlayerBackstepGoal extends Goal {
    private final DemonSlayerEntity entity;
    private int nextAllowedTick = 0;

    public DemonSlayerBackstepGoal(DemonSlayerEntity entity) {
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
        if (!entity.wasRecentlyDamaged(12)) return false;

        if (entity.tickCount % 3 != 0) return false;
        return entity.getRandom().nextFloat() < 0.30f;
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public void start() {
        LivingEntity target = entity.getTarget();
        if (target == null) return;

        Vec3 away = entity.position().subtract(target.position()).normalize();
        if (away.lengthSqr() < 1.0e-4) {
            away = entity.getLookAngle().scale(-1.0);
        }

        double backSpeed = 1.0;
        double upBoost = 0.45;
        entity.setDeltaMovement(new Vec3(away.x * backSpeed, upBoost, away.z * backSpeed));
        entity.hurtMarked = true;
        entity.playGeckoAnimation("backstep", 10);

        int jitter = entity.getRandom().nextInt(31) - 15;
        nextAllowedTick = entity.tickCount + Math.max(30, 60 + jitter);
    }
}
