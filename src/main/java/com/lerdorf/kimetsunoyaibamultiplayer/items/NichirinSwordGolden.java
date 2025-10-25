package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.FrostBreathingForms;

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
}
