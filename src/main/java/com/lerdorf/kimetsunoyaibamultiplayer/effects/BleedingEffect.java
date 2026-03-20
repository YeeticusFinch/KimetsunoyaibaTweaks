package com.lerdorf.kimetsunoyaibamultiplayer.effects;

import com.lerdorf.kimetsunoyaibamultiplayer.events.BleedingHandler;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class BleedingEffect extends MobEffect {
    private static final int BLEEDING_COLOR = 0x9B111E;

    public BleedingEffect() {
        super(MobEffectCategory.HARMFUL, BLEEDING_COLOR);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        BleedingHandler.tickBleedingMovement(entity, amplifier + 1);
    }
}
