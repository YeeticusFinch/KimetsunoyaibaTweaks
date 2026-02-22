package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.EnhancedBeastForms;

/**
 * Enhanced Beast Breathing nichirin sword.
 * Uses custom EnhancedBeastForms with placeholder effects for future implementation.
 */
public class NichirinSwordBeast extends BreathingSwordItem {
    private static final BreathingTechnique BEAST_BREATHING = EnhancedBeastForms.createBeastBreathing();

    public NichirinSwordBeast(Properties properties) {
        super(properties);
    }

    @Override
    public BreathingTechnique getBreathingTechnique() {
        return BEAST_BREATHING;
    }
}
