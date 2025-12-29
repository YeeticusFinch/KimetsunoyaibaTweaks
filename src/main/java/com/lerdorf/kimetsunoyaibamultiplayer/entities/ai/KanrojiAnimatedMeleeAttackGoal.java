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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/**
 * Custom melee attack goal for Kanroji Entity with long-range whip attacks.
 *
 * Features:
 * - Extended attack range (5 blocks) for whip-style combat
 * - Multi-target damage using WhipDamageHandler
 * - Synchronized sword animations via KanrojiSwordAnimationTrigger
 * - Attack animations match the GeckoLib sword animations
 */
public class KanrojiAnimatedMeleeAttackGoal extends MeleeAttackGoal {
    private final KanrojiEntity entity;
    private int attackAnimationTick = 0;

    // Kanroji-specific attack animations (mix of basic and special overhead attacks)
    private static final String[] ATTACK_ANIMATIONS = {
        "sword_to_left",
        "sword_to_right",
        "sword_overhead",
        "kanroji_sword_overhead" // Special Kanroji overhead attack
    };

    // Whip attack configuration
    private static final double WHIP_RANGE = 5.0; // 5 block range (vs normal 2-3 blocks)
    private static final double WHIP_DAMAGE_RADIUS = 1.5; // Hitbox radius for whip hits

    public KanrojiAnimatedMeleeAttackGoal(KanrojiEntity entity, double speedModifier, boolean followingTargetEvenIfNotSeen) {
        super(entity, speedModifier, followingTargetEvenIfNotSeen);
        this.entity = entity;
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity target, double distToTarget) {
        // Check if we can attack (within whip range)
        if (this.canPerformWhipAttack(target)) {
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

            // Perform whip-based attack (multi-target damage in arc)
            ItemStack heldItem = entity.getItemInHand(InteractionHand.MAIN_HAND);
            if (heldItem.getItem() instanceof BreathingSwordItem) {
                // Use WhipDamageHandler for radial multi-target damage
                float damage = (float) entity.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
                int hitCount = WhipDamageHandler.applyWhipDamage(entity, damage, WHIP_DAMAGE_RADIUS);

                Log.debug("[KanrojiAnimatedMeleeAttackGoal] Whip attack hit {} targets with {} damage", hitCount, damage);

                // Trigger sword slash rendering for visual effect
                if (!entity.level().isClientSide) {
                    triggerSwordSlash(animIndex, animation);
                }
            }
        }
    }

    /**
     * Check if entity can perform a whip attack (extended range check).
     */
    private boolean canPerformWhipAttack(LivingEntity target) {
        if (!this.isTimeToAttack()) {
            return false;
        }

        // Use extended whip range instead of normal melee range
        double distanceSq = this.mob.distanceToSqr(target);
        double maxRangeSq = WHIP_RANGE * WHIP_RANGE;

        return distanceSq <= maxRangeSq;
    }

    /**
     * Override to use whip range instead of normal melee range.
     */
    @Override
    protected double getAttackReachSqr(LivingEntity target) {
        return WHIP_RANGE * WHIP_RANGE;
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
}
