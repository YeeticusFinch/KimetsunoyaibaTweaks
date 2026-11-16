package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.ThunderBreathingForms;

/** Kaigaku's human Thunder Breathing sword, using the blue lightning forms. */
public class NichirinSwordKaigakuHuman extends BreathingSwordItem {
    private static final BreathingTechnique THUNDER_BREATHING =
        ThunderBreathingForms.createKaigakuHumanBreathing();

    public NichirinSwordKaigakuHuman(Properties properties) {
        super(properties);
    }

    @Override
    public BreathingTechnique getBreathingTechnique() {
        return THUNDER_BREATHING;
    }
}
