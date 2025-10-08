package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.IceBreathingForms;

/**
 * Shimizu's ice breathing sword (6 forms + 7th form: Icicle Claws)
 */
public class NichirinSwordShimizu extends BreathingSwordItem {
    private static final BreathingTechnique ICE_BREATHING_WITH_SEVENTH = IceBreathingForms.createIceBreathingWithSeventh();

    public NichirinSwordShimizu(Properties properties) {
        super(properties);
    }

    @Override
    public BreathingTechnique getBreathingTechnique() {
        return ICE_BREATHING_WITH_SEVENTH;
    }
}
