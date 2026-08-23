package com.lerdorf.kimetsunoyaibamultiplayer.effects;

import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.AlchemyMedicineHandler;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class DemonRestorationEffect extends MobEffect {
    public DemonRestorationEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xE89AA6);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration == 1;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        AlchemyMedicineHandler.completeDemonRestoration(entity);
    }
}
