package com.lerdorf.kimetsunoyaibamultiplayer.entities.ai;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.AbilityScheduler;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.GuardStateHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.DemonSlayerEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Random defensive guard behavior for DemonSlayerEntity.
 * Active only for power level 2+ while in combat.
 */
public class DemonSlayerGuardGoal extends Goal {
    private final DemonSlayerEntity entity;
    private int nextAllowedTick = 0;

    public DemonSlayerGuardGoal(DemonSlayerEntity entity) {
        this.entity = entity;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (entity.level().isClientSide) return false;
        if (entity.getPowerLevel() < 2) return false;
        if (entity.isActionLocked() || entity.isDisarmed()) return false;
        if (entity.tickCount < nextAllowedTick) return false;
        if (entity.getAnimationTicks() > 0) return false;

        LivingEntity target = entity.getTarget();
        if (target == null || !target.isAlive()) return false;

        if (entity.tickCount % 5 != 0) return false;
        return entity.getRandom().nextFloat() < 0.18f;
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public void start() {
        double defensivePower = entity.getPowerLevel() * 5.0 + 5.0;
        double formId = entity.getPersistentData().getDouble("breathes");

        entity.playGeckoAnimation("guard", 20);
        GuardStateHelper.setGuardState(entity, defensivePower, formId);
        AbilityScheduler.scheduleOnce(entity, () -> GuardStateHelper.clearGuardState(entity), 20);

        int jitter = entity.getRandom().nextInt(41) - 20;
        nextAllowedTick = entity.tickCount + Math.max(30, 80 + jitter);
    }
}
