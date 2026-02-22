package com.lerdorf.kimetsunoyaibamultiplayer.entities.ai;

import com.lerdorf.kimetsunoyaibamultiplayer.entities.MuichiroFullPotentialEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Occasional evasive backstep for Muichiro Full Potential.
 */
public class MuichiroFPBackstepGoal extends Goal {
    private final MuichiroFullPotentialEntity entity;
    private int nextAllowedTick = 0;
    private static final int BASE_COOLDOWN_TICKS = 60;

    public MuichiroFPBackstepGoal(MuichiroFullPotentialEntity entity) {
        this.entity = entity;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = entity.getTarget();
        if (target == null || !target.isAlive()) return false;
        if (entity.level().isClientSide) return false;
        if (entity.tickCount < nextAllowedTick) return false;
        if (entity.getAnimationTicks() > 0) return false;
        if (!entity.wasRecentlyDamaged(20)) return false;
        if (entity.tickCount % 5 != 0) return false;
        return entity.getRandom().nextFloat() < 0.35f;
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

        Vec3 motion = new Vec3(away.x * 1.0, 0.5, away.z * 1.0);
        entity.setDeltaMovement(motion);
        entity.hurtMarked = true;
        entity.playGeckoAnimation("backstep", 8);

        int jitter = entity.getRandom().nextInt(41) - 20;
        nextAllowedTick = entity.tickCount + Math.max(20, BASE_COOLDOWN_TICKS + jitter);
    }
}
