package com.lerdorf.kimetsunoyaibamultiplayer.effects;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.AbilityScheduler;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Vermilion Eye potion effect - grants enhanced perception abilities.
 *
 * Features:
 * - Red vermilion tint to vision (#E34234)
 * - See all entities within 200 blocks through walls
 * - Color-coded entity glowing based on threat level:
 *   - Red: Hostile entities and entities that are aggro towards the player
 *   - Green: Passive entities (animals, etc.)
 *   - Yellow: Neutral entities
 *   - Blue: Players (non-aggro)
 * - 40% faster breathing form ability cooldowns
 *
 * Effect ticks every tick to maintain the visual overlay.
 */
public class VermilionEyeEffect extends MobEffect {

    // Vermilion red color: #E34234
    public static final int VERMILION_COLOR = 0xE34234;

    // Range in blocks for entity visibility
    public static final double VISIBILITY_RANGE = 200.0;

    // Cooldown reduction multiplier (0.6 = 40% faster, meaning 60% of original cooldown)
    public static final float COOLDOWN_MULTIPLIER = 0.6f;
    private static final int EYE_STRAIN_BLINDNESS_DURATION = 20 * 600; // 10 minutes
    private static final String EYE_STRAIN_PENDING_KEY = "knymp_vermilion_eye_strain_pending";

    public VermilionEyeEffect() {
        super(MobEffectCategory.BENEFICIAL, VERMILION_COLOR);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // Effect should tick every tick for visual updates
        return true;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // The main visual effects are handled client-side by VermilionEyeOverlay
        // Server-side, we just need to ensure the effect is active
        // Cooldown reduction is applied when abilities are used (see integration in breathing forms)
        super.applyEffectTick(entity, amplifier);
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
        super.removeAttributeModifiers(entity, attributeMap, amplifier);

        if (entity.level().isClientSide || !entity.isAlive()) {
            return;
        }

        // Guard against duplicate scheduling when effect removal callbacks fire more than once.
        if (entity.getPersistentData().getBoolean(EYE_STRAIN_PENDING_KEY)) {
            return;
        }
        entity.getPersistentData().putBoolean(EYE_STRAIN_PENDING_KEY, true);

        // Defer by one tick so we don't mutate active effects during effect removal processing.
        AbilityScheduler.scheduleOnce(entity, () -> {
            entity.getPersistentData().remove(EYE_STRAIN_PENDING_KEY);

            if (!entity.isAlive() || entity.level().isClientSide) {
                return;
            }
            if (entity.hasEffect(ModEffects.VERMILION_EYE.get())) {
                return;
            }

            entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, EYE_STRAIN_BLINDNESS_DURATION, 0));
            entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0));
            entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 5));
            entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 80, 5));

            entity.level().playSound(null, entity.blockPosition(), SoundEvents.BEACON_DEACTIVATE,
                SoundSource.PLAYERS, 0.6F, 1.2F);
        }, 1);
    }

    /**
     * Check if an entity has the Vermilion Eye effect active.
     */
    public static boolean hasVermilionEye(LivingEntity entity) {
        if (entity == null) return false;
        return entity.hasEffect(ModEffects.VERMILION_EYE.get());
    }

    /**
     * Get the cooldown multiplier for breathing form abilities.
     * Returns 1.0 if the entity doesn't have the effect.
     *
     * @param entity The entity to check
     * @return Cooldown multiplier (0.6 if effect is active, 1.0 otherwise)
     */
    public static float getCooldownMultiplier(LivingEntity entity) {
        if (hasVermilionEye(entity)) {
            return COOLDOWN_MULTIPLIER;
        }
        return 1.0f;
    }

    /**
     * Apply cooldown reduction to a given cooldown value.
     *
     * @param entity The entity with potentially active Vermilion Eye
     * @param baseCooldown The base cooldown in seconds
     * @return The adjusted cooldown in seconds
     */
    public static float applyCooldownReduction(LivingEntity entity, float baseCooldown) {
        return baseCooldown * getCooldownMultiplier(entity);
    }

    /**
     * Apply cooldown reduction to a given cooldown value in ticks.
     *
     * @param entity The entity with potentially active Vermilion Eye
     * @param baseCooldownTicks The base cooldown in ticks
     * @return The adjusted cooldown in ticks
     */
    public static int applyCooldownReductionTicks(LivingEntity entity, int baseCooldownTicks) {
        return Math.round(baseCooldownTicks * getCooldownMultiplier(entity));
    }
}
