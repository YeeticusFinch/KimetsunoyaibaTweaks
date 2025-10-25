package com.lerdorf.kimetsunoyaibamultiplayer.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class FavorOfMuzanEffect extends MobEffect {

    public FavorOfMuzanEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x964B00); // Brown color
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // Effect should be reapplied every 5 seconds (100 ticks)
        return duration % 100 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Currently no ongoing effect, but can be extended in the future
        super.applyEffectTick(entity, amplifier);
    }
}