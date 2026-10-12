package com.lerdorf.kimetsunoyaibamultiplayer.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/** A near-total movement lock that keeps the victim suspended in webbing. */
public class SpiderSnareEffect extends MobEffect {
    private static final int COLOR = 0xD8D8D8;
    private static final UUID MOVEMENT_MODIFIER_UUID =
        UUID.fromString("b7b2b3e4-63d4-4b18-8a14-fc38db4fc3d2");

    public SpiderSnareEffect() {
        super(MobEffectCategory.HARMFUL, COLOR);
        this.addAttributeModifier(
            Attributes.MOVEMENT_SPEED,
            MOVEMENT_MODIFIER_UUID.toString(),
            -0.99D,
            AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
}
