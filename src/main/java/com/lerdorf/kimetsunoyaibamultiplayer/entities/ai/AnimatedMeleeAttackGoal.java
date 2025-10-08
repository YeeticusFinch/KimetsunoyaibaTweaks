package com.lerdorf.kimetsunoyaibamultiplayer.entities.ai;

import com.lerdorf.kimetsunoyaibamultiplayer.entities.BreathingSlayerEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

/**
 * Custom melee attack goal that plays attack animations via PlayerAnimator/MobPlayerAnimator
 */
public class AnimatedMeleeAttackGoal extends MeleeAttackGoal {
    private final BreathingSlayerEntity entity;
    private int attackAnimationTick = 0;
    private static final String[] ATTACK_ANIMATIONS = {
        "sword_to_left",
        "sword_to_right",
        "sword_overhead"
    };

    public AnimatedMeleeAttackGoal(BreathingSlayerEntity entity, double speedModifier, boolean followingTargetEvenIfNotSeen) {
        super(entity, speedModifier, followingTargetEvenIfNotSeen);
        this.entity = entity;
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity target, double distToTarget) {
        if (this.canPerformAttack(target)) {
            // Reset attack cooldown
            this.resetAttackCooldown();

            // Play random attack animation (10 tick duration, same as player attacks)
            String animation = ATTACK_ANIMATIONS[entity.getRandom().nextInt(ATTACK_ANIMATIONS.length)];
            entity.playGeckoAnimation(animation, 10);

            // Perform the actual attack
            this.mob.doHurtTarget(target);
        }
    }

    @Override
    protected void resetAttackCooldown() {
        super.resetAttackCooldown();
        this.attackAnimationTick = 0;
    }

    @Override
    protected boolean isTimeToAttack() {
        return super.isTimeToAttack();
    }

    protected boolean canPerformAttack(LivingEntity target) {
        return this.isTimeToAttack() && this.mob.distanceToSqr(target) <= this.getAttackReachSqr(target);
    }
}
