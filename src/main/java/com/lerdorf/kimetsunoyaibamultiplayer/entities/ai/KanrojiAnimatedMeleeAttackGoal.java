package com.lerdorf.kimetsunoyaibamultiplayer.entities.ai;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.client.KanrojiSwordAnimationTrigger;
import com.lerdorf.kimetsunoyaibamultiplayer.combat.WhipDamageHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.KanrojiEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fml.DistExecutor;

/**
 * Custom melee attack goal for Kanroji Entity with long-range whip attacks.
 *
 * Features:
 * - Extended attack range (15 blocks, using entity's ENTITY_REACH attribute) for whip-style combat
 * - Optimal range maintenance (stops pathfinding when within 15 blocks)
 * - Multi-target damage using WhipDamageHandler
 * - Synchronized sword animations via KanrojiSwordAnimationTrigger
 * - Attack animations match the GeckoLib sword animations
 */
public class KanrojiAnimatedMeleeAttackGoal extends MeleeAttackGoal {
    private final KanrojiEntity entity;
    private int attackAnimationTick = 0;
    private int guardStateClearTick = -1; // When to clear guard state

    // Kanroji-specific attack animations (mix of basic and special overhead attacks)
    private static final String[] ATTACK_ANIMATIONS = {
        "sword_to_left",
        "sword_to_right",
        "sword_overhead",
        "kanroji_sword_overhead" // Special Kanroji overhead attack
    };

    // Whip attack configuration
    private static final double WHIP_DAMAGE_RADIUS = 1.5; // Hitbox radius for whip hits

    // Range management - stop pathfinding when within optimal range
    // This prevents Kanroji from running up to melee range
    private static final double OPTIMAL_RANGE = 14.0; // Stop pathfinding at this distance

    public KanrojiAnimatedMeleeAttackGoal(KanrojiEntity entity, double speedModifier, boolean followingTargetEvenIfNotSeen) {
        super(entity, speedModifier, followingTargetEvenIfNotSeen);
        this.entity = entity;
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity target, double distToTarget) {
        // Check if we can attack (within whip range)
        if (this.canPerformWhipAttack(target)) {
            // CRITICAL: Face the target before attacking
            // This ensures particles spawn in the right direction
            entity.getLookControl().setLookAt(target, 30.0F, 30.0F);

            // Force immediate rotation to target
            Vec3 targetPos = target.position();
            Vec3 entityPos = entity.position();
            double dx = targetPos.x - entityPos.x;
            double dz = targetPos.z - entityPos.z;
            float targetYaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
            entity.setYRot(targetYaw);
            entity.yBodyRot = targetYaw;
            entity.yHeadRot = targetYaw;

            // Reset attack cooldown
            this.resetAttackCooldown();

            // Select random attack animation
            int animIndex = entity.getRandom().nextInt(ATTACK_ANIMATIONS.length);
            String animation = ATTACK_ANIMATIONS[animIndex];
            int duration = 10; // Standard animation duration

            // Play entity animation (server -> synced to clients via entity data)
            entity.playGeckoAnimation(animation, duration);

            Log.debug("[KanrojiAnimatedMeleeAttackGoal] Playing animation: {} on entity", animation);

            // Note: Sword animation sync is handled by client-side tick handler
            // that watches the entity's current animation and triggers matching sword animations

            // Perform whip-based attack using shared handler (handles damage + particles)
            ItemStack heldItem = entity.getItemInHand(InteractionHand.MAIN_HAND);
            if (heldItem.getItem() instanceof BreathingSwordItem) {
                float damage = (float) entity.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);

                // Set sword clashing attack state (don't set guard state for basic attacks)
                // Love breathing uses ID 400 (consistent with player forms)
                com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.GuardStateHelper.setAttackState(entity, damage);

                // Use shared KanrojiSwordAttackHandler for consistent behavior with players
                int hitCount = com.lerdorf.kimetsunoyaibamultiplayer.combat.KanrojiSwordAttackHandler.performWhipAttack(
                    entity,
                    damage,
                    animation
                );

                Log.debug("[KanrojiAnimatedMeleeAttackGoal] Whip attack hit {} targets with {} damage", hitCount, damage);

                // Schedule guard state clear after 6 ticks
                guardStateClearTick = entity.tickCount + 6;

                // Trigger sword slash rendering for visual effect
                if (!entity.level().isClientSide) {
                    triggerSwordSlash(animIndex, animation);
                }
            }
        }
    }

    /**
     * Check if entity can perform a whip attack (extended range check).
     * Uses the entity's ENTITY_REACH attribute for dynamic range.
     */
    private boolean canPerformWhipAttack(LivingEntity target) {
        if (!this.isTimeToAttack()) {
            return false;
        }

        // Use entity's reach attribute (15 blocks for Kanroji)
        double whipRange = entity.getAttributeValue(ForgeMod.ENTITY_REACH.get());
        double distanceSq = this.mob.distanceToSqr(target);
        double maxRangeSq = whipRange * whipRange;

        return distanceSq <= maxRangeSq;
    }

    /**
     * Override to use whip range from entity's ENTITY_REACH attribute.
     */
    @Override
    protected double getAttackReachSqr(LivingEntity target) {
        double whipRange = entity.getAttributeValue(ForgeMod.ENTITY_REACH.get());
        return whipRange * whipRange;
    }

    /**
     * Trigger sword slash visual based on animation type.
     * Sends packet to nearby clients for slash model rendering.
     */
    private void triggerSwordSlash(int animIndex, String animName) {
        // Send slash spawn packet to nearby players
        if (entity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            double broadcastRange = com.lerdorf.kimetsunoyaibamultiplayer.Config.mobSlashBroadcastRange;

            com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToNearby(
                new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.MobSwordSlashPacket(
                    entity.getUUID(),
                    animName,
                    0 // formIndex (not used for basic attacks)
                ),
                serverLevel,
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                broadcastRange
            );

            Log.debug("[KanrojiAnimatedMeleeAttackGoal] Sent sword slash packet for animation: {}", animName);
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

    /**
     * Override tick to manage range - stop pathfinding when within optimal range.
     * This prevents Kanroji from running right up to the target.
     */
    @Override
    public void tick() {
        // Clear guard state if scheduled
        if (guardStateClearTick >= 0 && entity.tickCount >= guardStateClearTick) {
            com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.GuardStateHelper.clearGuardState(entity);
            guardStateClearTick = -1;
        }

        LivingEntity target = this.mob.getTarget();
        if (target == null) {
            super.tick();
            return;
        }

        double distSq = this.mob.distanceToSqr(target);
        double optimalRangeSq = OPTIMAL_RANGE * OPTIMAL_RANGE;

        // If we're within optimal range, stop pathfinding and just rotate to face target
        if (distSq <= optimalRangeSq) {
            // Stop movement
            this.mob.getNavigation().stop();

            // Look at target
            this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

            // Check if we can attack
            this.checkAndPerformAttack(target, distSq);
        } else {
            // Too far - continue normal pathfinding behavior
            super.tick();
        }
    }
}
