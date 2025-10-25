package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.EnhancedMistForms;

/**
 * Muichiro Tokito's Nichirin Sword
 * Features all 7 Mist Breathing forms including the signature 7th Form: Obscuring Clouds
 */
public class NichirinSwordMuichiro extends BreathingSwordItem {
    private static final BreathingTechnique MUICHIRO_MIST_BREATHING = EnhancedMistForms.createMuichiroMistBreathing();

    public NichirinSwordMuichiro(Properties properties) {
        super(properties);
    }

    @Override
    public BreathingTechnique getBreathingTechnique() {
        return MUICHIRO_MIST_BREATHING;
    }
}
