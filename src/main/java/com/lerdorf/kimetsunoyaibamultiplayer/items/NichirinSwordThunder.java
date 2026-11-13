package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.ThunderBreathingForms;

/** Standard yellow/light-blue enhanced Thunder Breathing sword. */
public class NichirinSwordThunder extends BreathingSwordItem {
    private static final BreathingTechnique THUNDER_BREATHING =
        ThunderBreathingForms.createThunderBreathing(false);

    public NichirinSwordThunder(Properties properties) {
        super(properties);
    }

    @Override
    public BreathingTechnique getBreathingTechnique() {
        return THUNDER_BREATHING;
    }
}
