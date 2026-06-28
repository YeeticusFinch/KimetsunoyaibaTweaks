package com.lerdorf.kimetsunoyaibamultiplayer.entities.ai;

import com.lerdorf.kimetsunoyaibamultiplayer.combat.KanrojiSwordAttackHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.BreathingSlayerEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.MuichiroEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.MuichiroFullPotentialEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.SwordParticleMapping;
import com.lerdorf.kimetsunoyaibamultiplayer.util.AttackDamageHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.util.DualWieldHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
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
    private boolean useOffhandDamageNext = false;

    public AnimatedMeleeAttackGoal(BreathingSlayerEntity entity, double speedModifier, boolean followingTargetEvenIfNotSeen) {
        super(entity, speedModifier, followingTargetEvenIfNotSeen);
        this.entity = entity;
    }

    @Override
    public boolean canUse() {
        if (entity instanceof com.lerdorf.kimetsunoyaibamultiplayer.entities.DemonSlayerEntity demonSlayer
            && (demonSlayer.isActionLocked() || demonSlayer.isDisarmed())) {
            return false;
        }
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        if (entity instanceof com.lerdorf.kimetsunoyaibamultiplayer.entities.DemonSlayerEntity demonSlayer
            && (demonSlayer.isActionLocked() || demonSlayer.isDisarmed())) {
            return false;
        }
        return super.canContinueToUse();
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity target, double distToTarget) {
        if (this.canPerformAttack(target)) {
            // Reset attack cooldown
            this.resetAttackCooldown();

            boolean dualWield = DualWieldHelper.isDualWielding(entity);
            InteractionHand attackHand = InteractionHand.MAIN_HAND;
            if (dualWield) {
                attackHand = useOffhandDamageNext ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
                useOffhandDamageNext = !useOffhandDamageNext;
            }

            // Play random attack animation
            String animation;
            if (dualWield) {
                animation = entity.getRandom().nextBoolean() ? "sword_to_left" : "sword_to_right";
            } else {
                int animIndex = entity.getRandom().nextInt(ATTACK_ANIMATIONS.length);
                animation = ATTACK_ANIMATIONS[animIndex];
            }
            int duration = 10;
            // For Muichiro, play faster (double/triple speed effect via shorter duration)
            if (entity instanceof MuichiroEntity) {
                duration = 7; // slightly faster attacks
            }
            if (entity instanceof MuichiroFullPotentialEntity) {
                duration = 5; // ~2x speed for visibility while still fast
            }
            entity.playGeckoAnimation(animation, duration);

            // Trigger sword slash rendering if holding a supported nichirin sword (server-side -> clients)
            if (!entity.level().isClientSide) {
                ItemStack heldItem = entity.getItemInHand(InteractionHand.MAIN_HAND);
                if (SwordParticleMapping.isKimetsunoyaibaSword(heldItem)) {
                    triggerSwordSlash(animation);
                }
            }

            // Check if holding a whip sword (love/kanroji) — use AOE whip attack instead
            ItemStack heldItem = entity.getItemInHand(InteractionHand.MAIN_HAND);
            if (KanrojiSwordAttackHandler.isWhipSword(heldItem) && heldItem.getItem() instanceof BreathingSwordItem) {
                float damage = AttackDamageHelper.getAttackDamageForHand(entity, attackHand);
                float boxSize = KanrojiSwordAttackHandler.getBoxSizeForSword(heldItem);
                GuardStateHelper.setAttackState(entity, damage);
                KanrojiSwordAttackHandler.performWhipAttack(entity, damage, animation, boxSize);
                AbilityScheduler.scheduleOnce(entity, () -> GuardStateHelper.clearGuardState(entity), 6);
            } else {
                // For dual-wield beast attacks, use hand-specific damage instead of main-hand-only damage.
                if (dualWield) {
                    float handDamage = AttackDamageHelper.getAttackDamageForHand(entity, attackHand);
                    Damager.hurt(entity, target, handDamage);
                } else {
                    // Perform the standard melee attack
                    this.mob.doHurtTarget(target);
                }
            }

            // Basic sword clash window for Muichiro's regular swings
            if (!entity.level().isClientSide && (entity instanceof MuichiroEntity) && Config.enableSwordClashing) {
                double weakDefense = 2.0; // brief clash/defense power
                GuardStateHelper.setWeakAttackState(entity, weakDefense);
                AbilityScheduler.scheduleOnce(entity, () -> GuardStateHelper.clearGuardState(entity), 15);
            }

            if (!entity.level().isClientSide && (entity instanceof MuichiroFullPotentialEntity) && Config.enableSwordClashing) {
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
    private void triggerSwordSlash(String animName) {
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
    protected int getAttackInterval() {
        int base = super.getAttackInterval();
        if (DualWieldHelper.isDualWielding(entity)) {
            return Math.max(1, base / 2);
        }
        return base;
    }

    @Override
    protected boolean isTimeToAttack() {
        return super.isTimeToAttack();
    }

    protected boolean canPerformAttack(LivingEntity target) {
        if (entity instanceof com.lerdorf.kimetsunoyaibamultiplayer.entities.DemonSlayerEntity demonSlayer
            && (demonSlayer.isActionLocked() || demonSlayer.isDisarmed())) {
            return false;
        }
        return this.isTimeToAttack() && this.mob.distanceToSqr(target) <= this.getAttackReachSqr(target);
    }

    /**
     * Check if this entity currently holds a whip sword with extended reach.
     */
    private boolean hasExtendedReach() {
        ItemStack heldItem = entity.getItemInHand(InteractionHand.MAIN_HAND);
        return KanrojiSwordAttackHandler.isWhipSword(heldItem);
    }

    /**
     * Get the entity's reach value from ENTITY_REACH attribute.
     * Returns > 3 when holding a whip sword.
     */
    private double getEntityReach() {
        try {
            return entity.getAttributeValue(ForgeMod.ENTITY_REACH.get());
        } catch (Exception e) {
            return 3.0;
        }
    }

    @Override
    protected double getAttackReachSqr(LivingEntity target) {
        if (hasExtendedReach()) {
            double reach = getEntityReach();
            return reach * reach;
        }
        return super.getAttackReachSqr(target);
    }

    /**
     * Override tick for optimal range management when holding whip swords.
     * Stops pathfinding when within range instead of running right up to the target.
     */
    @Override
    public void tick() {
        if (!hasExtendedReach()) {
            super.tick();
            return;
        }

        LivingEntity target = this.mob.getTarget();
        if (target == null) {
            super.tick();
            return;
        }

        double distSq = this.mob.distanceToSqr(target);
        double reach = getEntityReach();
        // Optimal range is slightly less than max reach to avoid edge-of-range issues
        double optimalRangeSq = (reach - 1.0) * (reach - 1.0);

        if (distSq <= optimalRangeSq) {
            // In range — stop pathfinding and face target
            this.mob.getNavigation().stop();
            this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

            // Face the target for accurate whip attacks
            Vec3 targetPos = target.position();
            Vec3 entityPos = entity.position();
            double dx = targetPos.x - entityPos.x;
            double dz = targetPos.z - entityPos.z;
            float targetYaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
            entity.setYRot(targetYaw);
            entity.yBodyRot = targetYaw;
            entity.yHeadRot = targetYaw;

            this.checkAndPerformAttack(target, distSq);
        } else {
            // Too far — continue normal pathfinding
            super.tick();
        }
    }
}
