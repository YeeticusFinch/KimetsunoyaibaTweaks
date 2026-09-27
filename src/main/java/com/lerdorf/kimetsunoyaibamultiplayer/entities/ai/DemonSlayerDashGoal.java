package com.lerdorf.kimetsunoyaibamultiplayer.entities.ai;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.MovementHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.DemonSlayerEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.meditation.PassiveSkillManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** AI equivalent of the demon slayer passive dash skill. */
public class DemonSlayerDashGoal extends Goal {
    private static final double MIN_DASH_DISTANCE = 4.0D;
    private static final double MAX_DASH_DISTANCE = 18.0D;

    private final DemonSlayerEntity entity;
    private int nextAllowedTick;

    public DemonSlayerDashGoal(DemonSlayerEntity entity) {
        this.entity = entity;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (entity.level().isClientSide || entity.isActionLocked() || entity.isDisarmed()) {
            return false;
        }
        int skillLevel = PassiveSkillManager.getSlayerPassiveSkillLevelForRank(entity.getPowerLevel());
        if (skillLevel <= 0 || entity.tickCount < nextAllowedTick || entity.getAnimationTicks() > 0) {
            return false;
        }

        LivingEntity target = entity.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        double distance = entity.distanceTo(target);
        return distance >= MIN_DASH_DISTANCE
            && distance <= MAX_DASH_DISTANCE
            && entity.tickCount % 8 == 0
            && entity.getRandom().nextFloat() < 0.16F;
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public void start() {
        LivingEntity target = entity.getTarget();
        if (target == null) {
            return;
        }

        int skillLevel = PassiveSkillManager.getSlayerPassiveSkillLevelForRank(entity.getPowerLevel());
        Vec3 direction = target.position().subtract(entity.position());
        Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);
        if (horizontal.lengthSqr() < 0.0001D) {
            horizontal = entity.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        }
        if (horizontal.lengthSqr() < 0.0001D) {
            return;
        }

        double power = PassiveSkillManager.getSlayerDashPower(skillLevel);
        MovementHelper.setVelocity(entity, horizontal.normalize().scale(power).add(0.0D, 0.12D, 0.0D));
        entity.playGeckoAnimation("sprint", 8);
        nextAllowedTick = entity.tickCount + PassiveSkillManager.getSlayerDashCooldown(skillLevel);
    }
}
