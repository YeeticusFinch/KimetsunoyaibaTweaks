package com.lerdorf.kimetsunoyaibamultiplayer.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/** Harmful electrical shock that damages and slows its victim continuously. */
public class ElectrocutedEffect extends MobEffect {
    public static final int COLOR = 0x123A9A;
    private static final UUID MOVEMENT_MODIFIER_UUID =
        UUID.fromString("6fddde0e-2a4e-4cf7-aab7-4ac7a23c5a53");

    public ElectrocutedEffect() {
        super(MobEffectCategory.HARMFUL, COLOR);
        addAttributeModifier(
            Attributes.MOVEMENT_SPEED,
            MOVEMENT_MODIFIER_UUID.toString(),
            -0.25D,
            AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide && entity.isAlive()) {
            entity.hurt(entity.damageSources().magic(), 1.0F + amplifier);
        }
    }
}
