package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.FrostBreathingForms;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Golden nichirin sword - temporary form for Seventh Form: Golden Senses
 * Not intended for creative menu or normal gameplay
 */
public class NichirinSwordGolden extends BreathingSwordItem {
    private static final BreathingTechnique GOLDEN_FROST_BREATHING = FrostBreathingForms.createGoldenFrostBreathing();

    public NichirinSwordGolden(Properties properties) {
        super(properties);
    }

    @Override
    public BreathingTechnique getBreathingTechnique() {
        return GOLDEN_FROST_BREATHING;
    }
    
    private static final double ATTACK_SPEED = -2.4F;
    
    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND) {
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();

            // Attack damage: base entity damage is 1, we add 4 to make the tooltip show "+4.5 Attack Damage"
            builder.put(Attributes.ATTACK_DAMAGE,
                new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", 4, AttributeModifier.Operation.ADDITION));
            builder.put(Attributes.ATTACK_SPEED,
                new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", ATTACK_SPEED, AttributeModifier.Operation.ADDITION));

            return builder.build();
        }
        return super.getDefaultAttributeModifiers(slot);
    }
}
