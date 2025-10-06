package com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Utility class for calculating scaled damage based on potion effects
 * Works with any LivingEntity (players, mobs, custom entities)
 */
public class DamageCalculator {

    /**
     * Calculate damage with potion effect scaling (Strength/Weakness)
     *
     * @param entity The entity dealing damage
     * @param baseDamage The base damage value
     * @return Scaled damage based on active potion effects
     */
    public static float calculateScaledDamage(LivingEntity entity, float baseDamage) {
        float damage = baseDamage;

        // Strength effect: +3 damage per level
        if (entity.hasEffect(MobEffects.DAMAGE_BOOST)) {
            int strengthLevel = entity.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() + 1;
            damage += 3.0F * strengthLevel;
        }

        // Weakness effect: -4 damage per level
        if (entity.hasEffect(MobEffects.WEAKNESS)) {
            int weaknessLevel = entity.getEffect(MobEffects.WEAKNESS).getAmplifier() + 1;
            damage -= 4.0F * weaknessLevel;
        }

        // Ensure minimum damage of 0
        return Math.max(0.0F, damage);
    }

    /**
     * Calculate damage with custom multiplier and potion effect scaling
     *
     * @param entity The entity dealing damage
     * @param baseDamage The base damage value
     * @param multiplier Damage multiplier (e.g., 1.5 for 150% damage)
     * @return Scaled damage with multiplier and potion effects
     */
    public static float calculateScaledDamage(LivingEntity entity, float baseDamage, float multiplier) {
        return calculateScaledDamage(entity, baseDamage * multiplier);
    }

    /**
     * Get appropriate damage source for an entity
     * Uses playerAttack for players, mobAttack for other entities
     *
     * @param attacker The attacking entity
     * @return DamageSource appropriate for the attacker type
     */
    public static DamageSource getDamageSource(LivingEntity attacker) {
        if (attacker instanceof Player player) {
            return attacker.level().damageSources().playerAttack(player);
        } else {
            return attacker.level().damageSources().mobAttack(attacker);
        }
    }
}
