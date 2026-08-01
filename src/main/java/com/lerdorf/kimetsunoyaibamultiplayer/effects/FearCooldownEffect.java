package com.lerdorf.kimetsunoyaibamultiplayer.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Hidden-gating effect used to prevent repeated low-level fear applications.
 */
public class FearCooldownEffect extends MobEffect {
    public static final int FEAR_COOLDOWN_COLOR = 0x100914;

    public FearCooldownEffect() {
        super(MobEffectCategory.HARMFUL, FEAR_COOLDOWN_COLOR);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }
}
