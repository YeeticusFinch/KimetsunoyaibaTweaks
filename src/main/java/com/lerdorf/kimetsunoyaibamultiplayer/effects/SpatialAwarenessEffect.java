package com.lerdorf.kimetsunoyaibamultiplayer.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Spatial Awareness potion effect.
 * Core gameplay behavior is implemented by tick handlers on client/server.
 */
public class SpatialAwarenessEffect extends MobEffect {

    public static final int SPATIAL_AWARENESS_COLOR = 0xC8C8C8;

    public SpatialAwarenessEffect() {
        super(MobEffectCategory.BENEFICIAL, SPATIAL_AWARENESS_COLOR);
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
