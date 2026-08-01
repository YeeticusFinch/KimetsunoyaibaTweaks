package com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique;

import com.lerdorf.kimetsunoyaibamultiplayer.effects.ModEffects;

import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Utility class for calculating scaled damage based on potion effects
 * Works with any LivingEntity (players, mobs, custom entities)
 */
public class DamageCalculator {

	/**
	 * Gets the damage multiplier based on world difficulty when the source is not a player.
	 *
	 * @param source The entity dealing the damage
	 * @return The damage multiplier (0.7 for peaceful, 1.0 for easy, 1.25 for normal, 1.5 for hard)
	 */
	private static float getDifficultyMultiplier(LivingEntity source) {
		// Players don't get difficulty scaling
		if (source instanceof Player) {
			return 1.0f;
		}

		// Get the world difficulty
		Difficulty difficulty = source.level().getDifficulty();

		// Apply difficulty-based scaling
		return switch (difficulty) {
			case PEACEFUL -> 0.7f;
			case EASY -> 1.0f;
			case NORMAL -> 1.25f;
			case HARD -> 1.5f;
		};
	}
	
    /**
     * Calculate damage with potion effect scaling (Strength/Weakness)
     *
     * @param entity The entity dealing damage
     * @param baseDamage The base damage value
     * @return Scaled damage based on active potion effects
     */
    public static float calculateScaledDamage(LivingEntity entity, float baseDamage) {
        float damage = baseDamage;
        float damageScaler = 1;
        boolean vanillaEffects = false;
        
        if (vanillaEffects) {// Vanilla Strength effect: +3 damage per level
            if (entity.hasEffect(MobEffects.DAMAGE_BOOST)) {
                int strengthLevel = entity.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() + 1;
                damage += 3.0F * strengthLevel;
            }
        }
        else {
            if (entity.hasEffect(MobEffects.DAMAGE_BOOST)) {
                int strengthLevel = entity.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() + 1;
                damageScaler = (float) Math.max(1, Math.ceil(strengthLevel / 3)); // Strength 0-3 --> 1x, Strength 4-6 --> 2x, Strength 7-9 --> 3x...
            }
        }

        // Vermilion Eye effect: +25% damage
        if (entity.hasEffect(ModEffects.VERMILION_EYE.get())) {
            damageScaler *= 1.25f;
        }

        // Weakness effect: -4 damage per level
        if (vanillaEffects) {
            if (entity.hasEffect(MobEffects.WEAKNESS)) {
                int weaknessLevel = entity.getEffect(MobEffects.WEAKNESS).getAmplifier() + 1;
                //damage -= 4.0F * weaknessLevel;
                damage *= Math.pow(0.8, weaknessLevel);
                damage += (Math.pow(0.8, weaknessLevel) - 1) / 0.4;
            }
        } else {
            if (entity.hasEffect(MobEffects.WEAKNESS)) {
                int weaknessLevel = entity.getEffect(MobEffects.WEAKNESS).getAmplifier() + 1;
                damageScaler *= 1 - 0.333f * weaknessLevel;
            }
        }

        MobEffectInstance killingIntent = entity.getEffect(ModEffects.KILLING_INTENT.get());
        if (killingIntent != null) {
            int level = killingIntent.getAmplifier() + 1;
            damageScaler *= 1.0f + (0.02f * level);
        }

        damageScaler = damageScaler * getDifficultyMultiplier(entity);

        // Ensure minimum damage of 0
        return Math.max(0.0F, damage * damageScaler);
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
