package com.lerdorf.kimetsunoyaibamultiplayer.util;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.KnYEffects;
import com.lerdorf.kimetsunoyaibamultiplayer.effects.ModEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

public final class WisteriaResistanceHelper {
    public static final int DEFAULT_DURATION_SECONDS = 90;
    public static final int EXTENDED_DURATION_SECONDS = 180;
    public static final int DEFAULT_AMPLIFIER = 1;
    public static final int MAX_AMPLIFIER = 3;

    private WisteriaResistanceHelper() {
    }

    public static boolean hasResistance(LivingEntity entity) {
        return entity != null && entity.hasEffect(ModEffects.WISTERIA_RESISTANCE.get());
    }

    public static int resistanceAmplifier(LivingEntity entity) {
        if (entity == null) {
            return 0;
        }
        MobEffectInstance effect = entity.getEffect(ModEffects.WISTERIA_RESISTANCE.get());
        return effect == null ? 0 : Math.min(MAX_AMPLIFIER, Math.max(0, effect.getAmplifier()));
    }

    public static float damageMultiplier(LivingEntity entity) {
        return Math.max(0.0F, 1.0F - 0.2F * resistanceAmplifier(entity));
    }

    public static float reduceWisteriaDamage(LivingEntity entity, float amount) {
        return amount * damageMultiplier(entity);
    }

    public static void addMovementSlowdownUnlessResistant(LivingEntity entity, int durationTicks, int amplifier, boolean ambient, boolean visible) {
        if (!hasResistance(entity)) {
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, durationTicks, amplifier, ambient, visible));
        }
    }

    public static boolean addWisteriaPoisonEffect(LivingEntity entity, int durationTicks, int amplifier, boolean ambient, boolean visible, boolean showIcon) {
        MobEffect wisteriaPoison = KnYEffects.getWisteriaPoisonEffect();
        if (entity == null || wisteriaPoison == null) {
            return false;
        }
        int adjustedDuration = Math.max(1, Math.round(durationTicks * damageMultiplier(entity)));
        int adjustedAmplifier = adjustedPoisonAmplifier(entity, amplifier);
        entity.addEffect(new MobEffectInstance(wisteriaPoison, adjustedDuration, adjustedAmplifier, ambient, visible, showIcon));
        return true;
    }

    public static int adjustedPoisonAmplifier(LivingEntity entity, int amplifier) {
        float multiplier = damageMultiplier(entity);
        int levels = Math.max(1, amplifier + 1);
        return Math.max(0, (int) Math.ceil(levels * multiplier) - 1);
    }
}
