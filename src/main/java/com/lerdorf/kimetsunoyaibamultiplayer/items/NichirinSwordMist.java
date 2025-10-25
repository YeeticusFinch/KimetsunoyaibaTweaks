package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.EnhancedMistForms;

/**
 * Generic Mist Breathing nichirin sword
 * Features 6 Mist Breathing forms (excludes 7th Form: Obscuring Clouds)
 */
public class NichirinSwordMist extends BreathingSwordItem {
    private static final BreathingTechnique MIST_BREATHING = EnhancedMistForms.createGenericMistBreathing();

    public NichirinSwordMist(Properties properties) {
        super(properties);
    }

    @Override
    public BreathingTechnique getBreathingTechnique() {
        return MIST_BREATHING;
    }
}
