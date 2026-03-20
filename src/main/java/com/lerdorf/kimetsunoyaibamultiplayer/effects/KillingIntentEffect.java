package com.lerdorf.kimetsunoyaibamultiplayer.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

public class KillingIntentEffect extends MobEffect {
    private static final int KILLING_INTENT_COLOR = 0xD8D8D8;
    private static final UUID ATTACK_DAMAGE_MODIFIER_UUID = UUID.fromString("1ad71d8f-6541-4a54-9ea8-9d47be8e5a7c");

    public KillingIntentEffect() {
        super(MobEffectCategory.BENEFICIAL, KILLING_INTENT_COLOR);
        // +2% attack damage per effect level (amplifier + 1).
        this.addAttributeModifier(
            Attributes.ATTACK_DAMAGE,
            ATTACK_DAMAGE_MODIFIER_UUID.toString(),
            0.02D,
            AttributeModifier.Operation.MULTIPLY_TOTAL
        );
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }
}
