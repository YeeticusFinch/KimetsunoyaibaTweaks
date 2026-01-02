package com.lerdorf.kimetsunoyaibamultiplayer.entities.ai;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.EnhancedLoveForms;
import com.lerdorf.kimetsunoyaibamultiplayer.combat.KanrojiSwordAttackHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.KanrojiEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * Side flip dodge for Kanroji - defensive counter-attack when taking damage.
 *
 * Similar to Muichiro's dodge but with an offensive twist:
 * - 30% chance to trigger when taking damage
 * - Reduces incoming damage by 50%
 * - Leaps sideways (left or right randomly) with side_flip animation
 * - Plays sword_rotate animation on sword with particle trail
 * - Deals AOE damage and knockback to all targetable entities within 10 blocks
 *
 * This creates a "damage reversal" playstyle where Kanroji can turn enemy attacks
 * into opportunities for devastating counter-attacks.
 */
public class KanrojiSideFlipDodgeGoal extends Goal {
    private final KanrojiEntity entity;
    private int nextAllowedTick = 0;

    // Minimum delay between side flips
    private static final int BASE_COOLDOWN_TICKS = 60; // 3 seconds

    // AOE counter-attack range
    private static final double COUNTER_RANGE = 10.0;

    public KanrojiSideFlipDodgeGoal(KanrojiEntity entity) {
        this.entity = entity;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = entity.getTarget();
        if (target == null || !target.isAlive()) return false; // Only when fighting
        if (entity.level().isClientSide) return false;

        // Don't dodge during transformation
        if (entity.isTransforming()) return false;

        // Respect cooldown
        if (entity.tickCount < nextAllowedTick) return false;

        // Don't dodge while already doing an animation
        if (entity.getAnimationTicks() > 0) return false;

        // Only consider dodge if recently took damage (last 10 ticks)
        if (!entity.wasRecentlyDamaged(10)) return false;

        // Chance gate - 30% chance when recently damaged
        if (entity.tickCount % 3 != 0) return false; // Evaluate every 3 ticks for responsiveness
        return entity.getRandom().nextFloat() < 0.30f;
    }

    @Override
    public boolean canContinueToUse() {
        return false; // one-shot
    }

    @Override
    public void start() {
        LivingEntity target = entity.getTarget();
        if (target == null) return;

        // Choose random side direction (left or right)
        boolean flipLeft = entity.getRandom().nextBoolean();

        // Compute sideways direction (perpendicular to forward)
        Vec3 forward = entity.getLookAngle().normalize();
        Vec3 sideways = new Vec3(-forward.z, 0, forward.x); // 90 degrees counter-clockwise
        if (!flipLeft) {
            sideways = sideways.scale(-1.0); // Flip to right side
        }

        // Propel sideways and upward (acrobatic dodge)
        double sideSpeed = 1.3; // Strong sideways leap
        double upBoost = 0.5;   // Jump up
        Vec3 motion = new Vec3(sideways.x * sideSpeed, upBoost, sideways.z * sideSpeed);
        entity.setDeltaMovement(motion);
        entity.hurtMarked = true;

        // Play side flip animation
        entity.playGeckoAnimation("side_flip", 12);

        Log.debug("[KanrojiSideFlipDodgeGoal] Side flip dodge {} (distance from target: {} blocks)",
            flipLeft ? "LEFT" : "RIGHT", Math.sqrt(entity.distanceToSqr(target)));

        // Perform AOE counter-attack with knockback
        performCounterAttack();

        // Next allowed time randomized (base ±15 ticks)
        int jitter = entity.getRandom().nextInt(31) - 15; // [-15, +15]
        nextAllowedTick = entity.tickCount + Math.max(30, BASE_COOLDOWN_TICKS + jitter);
    }

    /**
     * Perform AOE counter-attack during side flip.
     * Damages and knocks back all targetable entities within 10 blocks.
     */
    private void performCounterAttack() {
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        // Calculate damage (half of normal attack damage)
        float baseDamage = (float) entity.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float counterDamage = baseDamage * 0.5f; // 50% damage for counter

        // Use shared Kanroji attack handler for consistent AOE damage + particles
        // This will spawn sword_rotate particle trail automatically
        int hitCount = KanrojiSwordAttackHandler.performWhipAttack(
            entity,
            counterDamage,
            "sword_rotate" // Animation name for particles
        );

        // Apply knockback to all hit entities
        AABB counterBox = entity.getBoundingBox().inflate(COUNTER_RANGE);
        List<LivingEntity> targets = entity.level().getEntitiesOfClass(
            LivingEntity.class,
            counterBox,
            e -> e != entity && e.isAlive() && EnhancedLoveForms.isTargetable(entity, e)
        );

        for (LivingEntity target : targets) {
            // Calculate knockback direction (away from Kanroji)
            Vec3 knockbackDir = target.position().subtract(entity.position()).normalize();
            double knockbackStrength = 0.8; // Strong knockback for counter

            // Apply knockback
            target.setDeltaMovement(
                target.getDeltaMovement().add(
                    knockbackDir.x * knockbackStrength,
                    0.3, // Upward component
                    knockbackDir.z * knockbackStrength
                )
            );
            target.hurtMarked = true;
        }

        Log.debug("[KanrojiSideFlipDodgeGoal] Counter-attack hit {} targets with {} damage + knockback",
            hitCount, counterDamage);
    }
}
