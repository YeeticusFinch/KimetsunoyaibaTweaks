package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.ThunderBreathingForms;

/** Hashira-level Thunder Breathing sword with all seven forms. */
public class NichirinSwordHashiraThunder extends BreathingSwordItem {
    private static final BreathingTechnique THUNDER_BREATHING =
        ThunderBreathingForms.createHashiraBreathing();
    private static final double ATTACK_DAMAGE_MODIFIER = 4.0D;

    public NichirinSwordHashiraThunder(Properties properties) {
        super(properties);
    }

    @Override
    public BreathingTechnique getBreathingTechnique() {
        return THUNDER_BREATHING;
    }

    @Override
    protected double getAttackDamageModifier() {
        return ATTACK_DAMAGE_MODIFIER;
    }
}
