package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.IceBreathingForms;

/**
 * Ice Breathing nichirin sword (6 forms)
 */
public class NichirinSwordIce extends BreathingSwordItem {
    private static final BreathingTechnique ICE_BREATHING = IceBreathingForms.createIceBreathing();

    public NichirinSwordIce(Properties properties) {
        super(properties);
    }

    @Override
    public BreathingTechnique getBreathingTechnique() {
        return ICE_BREATHING;
    }
}
