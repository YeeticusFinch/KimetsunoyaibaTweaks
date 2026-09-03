package com.lerdorf.kimetsunoyaibamultiplayer.entities.ai;

import com.lerdorf.kimetsunoyaibamultiplayer.entities.DaughterEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** Backsteps away from a nearby combat enemy while keeping the enemy in view. */
public class DaughterBackstepGoal extends Goal {
    private static final int BACKSTEP_COOLDOWN_TICKS = 20;
    private final DaughterEntity daughter;
    private int nextAllowedTick;

    public DaughterBackstepGoal(DaughterEntity daughter) {
        this.daughter = daughter;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (daughter.level().isClientSide || daughter.isInHumanForm()
            || daughter.tickCount < nextAllowedTick || daughter.getAnimationTicks() > 0) {
            return false;
        }
        LivingEntity target = daughter.getNearbyCombatEnemy();
        return target != null && daughter.distanceToSqr(target) <= 15.0D * 15.0D;
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public void start() {
        LivingEntity target = daughter.getNearbyCombatEnemy();
        if (target == null) {
            return;
        }
        daughter.startBackstep(target);
        nextAllowedTick = daughter.tickCount + BACKSTEP_COOLDOWN_TICKS;
    }
}
