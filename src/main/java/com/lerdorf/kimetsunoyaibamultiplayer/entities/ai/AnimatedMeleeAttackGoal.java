package com.lerdorf.kimetsunoyaibamultiplayer.entities.ai;

import com.lerdorf.kimetsunoyaibamultiplayer.entities.BreathingSlayerEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.MuichiroEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem;
import com.lerdorf.kimetsunoyaibamultiplayer.client.particles.BonePositionTracker;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.item.ItemStack;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.GuardStateHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.AbilityScheduler;
import com.lerdorf.kimetsunoyaibamultiplayer.Config;

/**
 * Custom melee attack goal that plays attack animations via PlayerAnimator/MobPlayerAnimator
 * Also triggers sword slash rendering when entity has a nichirin sword
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

            // Play random attack animation
            int animIndex = entity.getRandom().nextInt(ATTACK_ANIMATIONS.length);
            String animation = ATTACK_ANIMATIONS[animIndex];
            int duration = 10;
            // For Muichiro, play faster (double/triple speed effect via shorter duration)
            if (entity instanceof MuichiroEntity) {
                duration = 5; // ~2x speed for visibility while still fast
            }
            entity.playGeckoAnimation(animation, duration);

            // Trigger sword slash rendering if holding a nichirin sword (server-side -> clients)
            if (!entity.level().isClientSide) {
                ItemStack heldItem = entity.getItemInHand(InteractionHand.MAIN_HAND);
                if (heldItem.getItem() instanceof BreathingSwordItem) {
                    triggerSwordSlash(animIndex);
                }
            }

            // Perform the actual attack
            this.mob.doHurtTarget(target);

            // Basic sword clash window for Muichiro's regular swings
            if (!entity.level().isClientSide && entity instanceof MuichiroEntity && Config.enableSwordClashing) {
                double weakDefense = 5.0; // stronger brief clash/defense power
                GuardStateHelper.setWeakAttackState(entity, weakDefense);
                AbilityScheduler.scheduleOnce(entity, () -> GuardStateHelper.clearGuardState(entity), 15);
            }
        }
    }

    /**
     * Trigger sword slash visual based on animation type
     * Uses BonePositionTracker to spawn slash models directly
     */
    private void triggerSwordSlash(int animIndex) {
        // Map animation index to animation name
        // 0 = sword_to_left (horizontal right->left), 1 = sword_to_right (horizontal left->right), 2 = sword_overhead (vertical down)
        String animName = "sword_to_left";
        if (animIndex == 1) animName = "sword_to_right";
        else if (animIndex == 2) animName = "sword_overhead";

        // Only execute on server: send a simple slash spawn instruction; clients will choose model/particles automatically
        if (!entity.level().isClientSide) {
            // Send only to nearby players within configured range
            if (entity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                double r = com.lerdorf.kimetsunoyaibamultiplayer.Config.mobSlashBroadcastRange;
                com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToNearby(
                    new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.MobSwordSlashPacket(
                        entity.getUUID(), animName, 0
                    ),
                    serverLevel,
                    entity.getX(), entity.getY(), entity.getZ(),
                    r
                );
            }
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
