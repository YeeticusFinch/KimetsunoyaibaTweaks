package com.lerdorf.kimetsunoyaibamultiplayer.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public class CourageEffect extends MobEffect {
    public static final int COURAGE_COLOR = 0xB06A2A;

    public CourageEffect() {
        super(MobEffectCategory.BENEFICIAL, COURAGE_COLOR);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        MobEffectInstance fear = entity.getEffect(ModEffects.FEAR.get());
        if (fear != null && fear.getAmplifier() + 1 <= amplifier + 1) {
            entity.removeEffect(ModEffects.FEAR.get());
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
}
