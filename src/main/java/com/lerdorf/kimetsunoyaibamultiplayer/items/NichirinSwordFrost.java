package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.FrostBreathingForms;

/**
 * Frost Breathing nichirin sword (6 forms)
 */
public class NichirinSwordFrost extends BreathingSwordItem {
    private static final BreathingTechnique FROST_BREATHING = FrostBreathingForms.createFrostBreathing();

    public NichirinSwordFrost(Properties properties) {
        super(properties);
    }

    @Override
    public BreathingTechnique getBreathingTechnique() {
        return FROST_BREATHING;
    }
}
