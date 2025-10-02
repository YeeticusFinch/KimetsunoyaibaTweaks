package com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * Utility class for calculating scaled damage based on potion effects
 */
public class DamageCalculator {

    /**
     * Calculate damage with potion effect scaling (Strength/Weakness)
     *
     * @param player The player dealing damage
     * @param baseDamage The base damage value
     * @return Scaled damage based on active potion effects
     */
    public static float calculateScaledDamage(Player player, float baseDamage) {
        float damage = baseDamage;

        // Strength effect: +3 damage per level
        if (player.hasEffect(MobEffects.DAMAGE_BOOST)) {
            int strengthLevel = player.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() + 1;
            damage += 3.0F * strengthLevel;
        }

        // Weakness effect: -4 damage per level
        if (player.hasEffect(MobEffects.WEAKNESS)) {
            int weaknessLevel = player.getEffect(MobEffects.WEAKNESS).getAmplifier() + 1;
            damage -= 4.0F * weaknessLevel;
        }

        // Ensure minimum damage of 0
        return Math.max(0.0F, damage);
    }

    /**
     * Calculate damage with custom multiplier and potion effect scaling
     *
     * @param player The player dealing damage
     * @param baseDamage The base damage value
     * @param multiplier Damage multiplier (e.g., 1.5 for 150% damage)
     * @return Scaled damage with multiplier and potion effects
     */
    public static float calculateScaledDamage(Player player, float baseDamage, float multiplier) {
        return calculateScaledDamage(player, baseDamage * multiplier);
    }
}
