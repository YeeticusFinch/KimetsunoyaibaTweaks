package com.lerdorf.kimetsunoyaibamultiplayer.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/** Beneficial strength granted by the arachnid strength chestplate. */
public class ArachnidStrengthEffect extends MobEffect {
    private static final int COLOR = 0x6E2638;
    private static final UUID ATTACK_DAMAGE_MODIFIER_UUID =
        UUID.fromString("a6a41ed0-7bcb-4db2-9fcb-73d7e59d3ef1");

    public ArachnidStrengthEffect() {
        super(MobEffectCategory.BENEFICIAL, COLOR);
        addAttributeModifier(
            Attributes.ATTACK_DAMAGE,
            ATTACK_DAMAGE_MODIFIER_UUID.toString(),
            0.20D,
            AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }
}
