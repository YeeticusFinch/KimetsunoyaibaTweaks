package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.FrostBreathingForms;

/**
 * Komorebi's frost breathing sword (6 forms + 7th form: Golden Senses)
 */
public class NichirinSwordKomorebi extends BreathingSwordItem {
    private static final BreathingTechnique FROST_BREATHING_WITH_SEVENTH = FrostBreathingForms.createFrostBreathingWithSeventh();

    public NichirinSwordKomorebi(Properties properties) {
        super(properties);
    }

    @Override
    public BreathingTechnique getBreathingTechnique() {
        return FROST_BREATHING_WITH_SEVENTH;
    }
}
