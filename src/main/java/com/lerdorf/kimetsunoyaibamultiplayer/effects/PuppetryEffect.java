package com.lerdorf.kimetsunoyaibamultiplayer.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * The Puppetry effect (Spider Family quest).
 *
 * Gameplay behavior (anchor leash, puppet owner, hover, AI control,
 * damage-shortened duration) is enforced by {@link PuppetryHandler}.
 * Visual white bezier strings are rendered client-side by
 * {@link com.lerdorf.kimetsunoyaibamultiplayer.client.PuppetLineRenderer}.
 */
public class PuppetryEffect extends MobEffect {
    public static final int PUPPETRY_COLOR = 0xE8E8E8;

    public PuppetryEffect() {
        super(MobEffectCategory.NEUTRAL, PUPPETRY_COLOR);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // Tick constantly so the handler can enforce behavior every cycle.
        return true;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        super.applyEffectTick(entity, amplifier);
    }
}
