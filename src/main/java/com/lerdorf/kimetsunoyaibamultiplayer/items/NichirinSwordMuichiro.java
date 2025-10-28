package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.EnhancedMistForms;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Muichiro Tokito's Nichirin Sword
 * Features all 7 Mist Breathing forms including the signature 7th Form: Obscuring Clouds
 */
public class NichirinSwordMuichiro extends BreathingSwordItem {
    private static final BreathingTechnique MUICHIRO_MIST_BREATHING = EnhancedMistForms.createMuichiroMistBreathing();

    private static final double ATTACK_SPEED = -2.4F;
    
    public NichirinSwordMuichiro(Properties properties) {
        super(properties);
    }

    @Override
    public BreathingTechnique getBreathingTechnique() {
        return MUICHIRO_MIST_BREATHING;
    }
    
    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND) {
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();

            // Attack damage: base entity damage is 1, we add 4.5 to make the tooltip show "+4.5 Attack Damage"
            builder.put(Attributes.ATTACK_DAMAGE,
                new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", 4, AttributeModifier.Operation.ADDITION));
            builder.put(Attributes.ATTACK_SPEED,
                new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", ATTACK_SPEED, AttributeModifier.Operation.ADDITION));

            return builder.build();
        }
        return super.getDefaultAttributeModifiers(slot);
    }
}
