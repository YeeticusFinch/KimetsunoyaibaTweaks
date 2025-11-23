package com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique;

import net.minecraft.world.entity.LivingEntity;

/**
 * Helper class for managing KnY sword clashing mechanics via NBT data.
 *
 * Based on the original KnY mod's guard system:
 * - "Damage" field: Dual purpose - offensive damage when attacking, defensive power when being attacked
 * - "guard" flag: Indicates entity is actively guarding (set during abilities)
 * - "skill" field: Enables guard mechanics even when Damage = 0 (set during animations/abilities)
 * - "attack" flag: Indicates entity is performing an attack
 *
 * See KnY_Sword_Combat.md for full documentation of the clashing system.
 */
public class GuardStateHelper {

    // NBT field names (matching original KnY mod)
    private static final String NBT_DAMAGE = "Damage";
    private static final String NBT_GUARD = "guard";
    private static final String NBT_SKILL = "skill";
    private static final String NBT_ATTACK = "attack";
    private static final String NBT_BREATHES = "breathes";

    /**
     * Set entity to guard/defensive state with specified defensive power.
     * Call this when a breathing technique or ability starts.
     *
     * @param entity The entity using the ability
     * @param defensivePower Amount of incoming damage that can be negated
     * @param breathingId Unique ID for the breathing technique (e.g., 320.0 for Flame Breathing)
     */
    public static void setGuardState(LivingEntity entity, double defensivePower, double breathingId) {
        entity.getPersistentData().putDouble(NBT_DAMAGE, defensivePower);
        entity.getPersistentData().putBoolean(NBT_GUARD, true);
        // Don't set skill or breathes - they interfere with base mod's movement and form cycling
        // Only set Damage and guard which are needed for sword clashing
    }
    
    /**
     * Set entity to guard/defensive state with specified defensive power.
     * Simplified version without breathing ID (defaults to 0).
     *
     * @param entity The entity using the ability
     * @param defensivePower Amount of incoming damage that can be negated
     */
    public static void setGuardState(LivingEntity entity, double defensivePower) {
        setGuardState(entity, defensivePower, 0.0);
    }

    /**
     * Set entity to attack state with specified offensive damage.
     * Call this when performing an attack within a breathing technique.
     *
     * @param entity The entity attacking
     * @param offensiveDamage Amount of damage to deal (also acts as defensive power)
     */
    public static void setAttackState(LivingEntity entity, double offensiveDamage) {
        entity.getPersistentData().putDouble(NBT_DAMAGE, offensiveDamage);
        entity.getPersistentData().putBoolean(NBT_ATTACK, true);
        // Note: skill and guard flags should already be set from setGuardState
    }

    /**
     * Enable defensive state without setting Damage value.
     * Useful for maintaining defense between attacks in a technique sequence.
     *
     * @param entity The entity to enable defense for
     */
    public static void enableContinuousDefense(LivingEntity entity) {
        // Only set guard flag - don't set skill as it interferes with base mod
        entity.getPersistentData().putBoolean(NBT_GUARD, true);
    }

    /**
     * Clear all guard/attack state.
     * Call this when a breathing technique or ability ends.
     *
     * @param entity The entity to clear state from
     */
    public static void clearGuardState(LivingEntity entity) {
        entity.getPersistentData().putDouble(NBT_DAMAGE, 0.0);
        entity.getPersistentData().putBoolean(NBT_GUARD, false);
        entity.getPersistentData().putBoolean(NBT_ATTACK, false);
        // Don't touch skill or breathes tags - let base mod manage them
    }

    /**
     * Clear only the attack flag (keep guard state active).
     * Call this after an individual attack completes within a technique.
     *
     * @param entity The entity to clear attack flag from
     */
    public static void clearAttackFlag(LivingEntity entity) {
        entity.getPersistentData().putBoolean(NBT_ATTACK, false);
    }

    /**
     * Clear the Damage field but keep skill/guard active.
     * Used between attacks in a technique to maintain continuous defense.
     *
     * @param entity The entity to clear damage from
     */
    public static void clearDamageValue(LivingEntity entity) {
        entity.getPersistentData().putDouble(NBT_DAMAGE, 0.0);
    }

    /**
     * Check if an entity is in guard state (can defend against attacks).
     * Simplified version that only checks guard flag and damage.
     *
     * @param entity The entity to check
     * @param attacker The entity attacking (to prevent self-damage)
     * @return true if entity can successfully guard
     */
    public static boolean isGuarding(LivingEntity entity, LivingEntity attacker) {
        // Not self-damage
        if (entity == attacker) {
            return false;
        }

        // Has guard flag
        boolean guard = entity.getPersistentData().getBoolean(NBT_GUARD);
        if (!guard) {
            return false;
        }

        // Has defensive power (Damage value > 0)
        double damage = entity.getPersistentData().getDouble(NBT_DAMAGE);
        return damage > 0.0;
    }

    /**
     * Get the defensive power of an entity (Damage field value).
     *
     * @param entity The entity to check
     * @return The amount of incoming damage that can be negated
     */
    public static double getDefensivePower(LivingEntity entity) {
        return entity.getPersistentData().getDouble(NBT_DAMAGE);
    }

    /**
     * Set a weak defensive state for basic weapon swings.
     * Call this when an entity performs a normal left-click attack.
     *
     * @param entity The entity swinging weapon
     * @param weakDefense Weak defensive power (typically 2.0-3.0)
     */
    public static void setWeakAttackState(LivingEntity entity, double weakDefense) {
        entity.getPersistentData().putDouble(NBT_DAMAGE, weakDefense);
        entity.getPersistentData().putBoolean(NBT_ATTACK, true);
    }
}
