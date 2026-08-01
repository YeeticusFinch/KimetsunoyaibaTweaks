package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.EnhancedBeastForms;

/**
 * Enhanced Beast Breathing sword for Inosuke (level 1 named sword).
 */
public class NichirinSwordInosuke extends BreathingSwordItem {
    private static final BreathingTechnique BEAST_BREATHING = EnhancedBeastForms.createBeastBreathing();
    private static final double ATTACK_DAMAGE_MODIFIER = 4.0;

    public NichirinSwordInosuke(Properties properties) {
        super(properties);
    }

    @Override
    public BreathingTechnique getBreathingTechnique() {
        return BEAST_BREATHING;
    }

    @Override
    protected double getAttackDamageModifier() {
        return ATTACK_DAMAGE_MODIFIER;
    }
}
