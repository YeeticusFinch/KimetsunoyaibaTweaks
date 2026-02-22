package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.EnhancedFlowerForms;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Kanao Tsuyuri's Nichirin Sword
 * Features Flower Breathing forms 1-6 plus Final Form: Equinoctial Vermilion Eye.
 */
public class NichirinSwordKanawo extends BreathingSwordItem {
    private static final BreathingTechnique KANAWO_FLOWER_BREATHING =
        EnhancedFlowerForms.createKanawoFlowerBreathing();

    private static final double ATTACK_SPEED = -2.4F;

    public NichirinSwordKanawo(Properties properties) {
        super(properties);
    }

    @Override
    public BreathingTechnique getBreathingTechnique() {
        return KANAWO_FLOWER_BREATHING;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND) {
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();

            // Attack damage: base entity damage is 1, we add 4 to make the tooltip show "+5 Attack Damage"
            builder.put(Attributes.ATTACK_DAMAGE,
                new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", 4, AttributeModifier.Operation.ADDITION));
            builder.put(Attributes.ATTACK_SPEED,
                new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", ATTACK_SPEED, AttributeModifier.Operation.ADDITION));

            return builder.build();
        }
        return super.getDefaultAttributeModifiers(slot);
    }
}
