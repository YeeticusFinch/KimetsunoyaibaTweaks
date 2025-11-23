package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.EnhancedLoveForms;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Mitsuri Kanroji's Nichirin Sword (Love Breathing)
 *
 * Features:
 * - Flexible whip-like blade that extends during combat
 * - 6 Love Breathing forms with whip animations
 * - Dual-layer emissive rendering with heart particle trails
 * - Physics-based whip motion for realistic movement
 * - Faster attack speed but slightly lower damage (whip-style combat)
 *
 * Attack Stats:
 * - Attack Damage: 6.0 (vs normal 7.0) - Compensated by multi-target whip hits
 * - Attack Speed: -1.8 (vs normal -2.4) - Faster whip attacks
 */
public class NichirinSwordKanroji extends BreathingSwordItem {
    private static final BreathingTechnique LOVE_BREATHING = EnhancedLoveForms.createLoveBreathing();

    private static final double ATTACK_DAMAGE = 6.0; // Slightly lower than normal swords
    private static final double ATTACK_SPEED = -1.8; // Faster than normal swords (-2.4)

    public NichirinSwordKanroji(Properties properties) {
        super(properties);
    }

    @Override
    public BreathingTechnique getBreathingTechnique() {
        return LOVE_BREATHING;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND) {
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();

            // Attack damage: base entity damage is 1, we add 5.0 to make total 6.0
            // Slightly lower than normal nichirin swords (which are 7.0)
            // This is balanced by the whip's ability to hit multiple targets
            builder.put(Attributes.ATTACK_DAMAGE,
                new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier",
                    ATTACK_DAMAGE - 1.0, AttributeModifier.Operation.ADDITION));

            // Attack speed: -1.8 (faster than normal -2.4)
            // Represents the whip's faster, more fluid combat style
            builder.put(Attributes.ATTACK_SPEED,
                new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier",
                    ATTACK_SPEED, AttributeModifier.Operation.ADDITION));

            return builder.build();
        }
        return super.getDefaultAttributeModifiers(slot);
    }

    /**
     * Check if this sword should render a whip.
     * This method can be used by client-side rendering code to identify Love Breathing swords.
     */
    public boolean hasWhipRendering() {
        return true;
    }
}
