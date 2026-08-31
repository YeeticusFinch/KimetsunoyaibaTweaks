package com.lerdorf.kimetsunoyaibamultiplayer.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** Marker effect for the Demonweb Puppetry Web Traversal Anchor form. */
public class WebTraversalEffect extends MobEffect {
    public WebTraversalEffect() {
        super(MobEffectCategory.NEUTRAL, 0xE8E8E8);
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
