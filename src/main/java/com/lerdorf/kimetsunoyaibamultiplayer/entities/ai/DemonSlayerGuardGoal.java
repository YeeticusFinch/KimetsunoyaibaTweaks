package com.lerdorf.kimetsunoyaibamultiplayer.entities.ai;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.AbilityScheduler;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.GuardStateHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.DemonSlayerEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.meditation.PassiveSkillManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Random defensive guard behavior for DemonSlayerEntity.
 * Active only for rank 4+ while in combat. The skill level follows the
 * demon slayer passive skill progression and uses the same random guard
 * animation set as the player ability.
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
        int skillLevel = PassiveSkillManager.getSlayerPassiveSkillLevelForRank(entity.getPowerLevel());
        if (skillLevel <= 0) return false;
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
        int skillLevel = PassiveSkillManager.getSlayerPassiveSkillLevelForRank(entity.getPowerLevel());
        int defensivePower = PassiveSkillManager.getSlayerGuardPower(skillLevel);
        String animation = "guard_" + entity.getRandom().nextInt(6);

        entity.playGeckoAnimation(animation, defensivePower);
        GuardStateHelper.setGuardState(entity, defensivePower, 0.0D, false);
        AbilityScheduler.scheduleOnce(entity, () -> GuardStateHelper.clearGuardState(entity), defensivePower);

        int jitter = entity.getRandom().nextInt(41) - 20;
        nextAllowedTick = entity.tickCount + Math.max(30, 80 + jitter);
    }
}
