package com.lerdorf.kimetsunoyaibamultiplayer.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Harmful fear effect. Gameplay behavior is enforced by FearEffectHandler.
 */
public class FearEffect extends MobEffect {
    public static final int FEAR_COLOR = 0x1B0B24;

    public FearEffect() {
        super(MobEffectCategory.HARMFUL, FEAR_COLOR);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        super.applyEffectTick(entity, amplifier);
    }
}
