package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.ThunderBreathingForms;

/** Dark-blue/light-blue enhanced Thunder Breathing sword. */
public class NichirinSwordThunderBlue extends BreathingSwordItem {
    private static final BreathingTechnique THUNDER_BREATHING =
        ThunderBreathingForms.createThunderBreathing(true);

    public NichirinSwordThunderBlue(Properties properties) {
        super(properties);
    }

    @Override
    public BreathingTechnique getBreathingTechnique() {
        return THUNDER_BREATHING;
    }
}
