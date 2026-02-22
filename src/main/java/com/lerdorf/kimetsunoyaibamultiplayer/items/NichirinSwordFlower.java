package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.EnhancedFlowerForms;

/**
 * Generic Flower Breathing nichirin sword
 * Features 6 Flower Breathing forms (1st, 3rd, plus base mod 2nd, 4th, 5th, 6th) plus Final Form
 *
 * Flower Breathing is derived from Water Breathing and emphasizes graceful, flowing attacks
 * with beautiful petal effects. This sword is suitable for demon slayers who have mastered
 * the Flower Breathing style.
 *
 * Available Forms:
 * - 1st Form: Flowing Tiger Lily Petals
 * - 2nd Form: (Base mod implementation)
 * - 3rd Form: Scattering Rose-Peach Thorns
 * - 4th Form: (Base mod implementation)
 * - 5th Form: (Base mod implementation)
 * - 6th Form: (Base mod implementation)
 * - Final Form: Equinoctial Vermillion Eye
 *
 * Hashira-exclusive forms (7th-9th) are NOT available on this sword.
 */
public class NichirinSwordFlower extends BreathingSwordItem {
    private static final BreathingTechnique FLOWER_BREATHING = EnhancedFlowerForms.createGenericFlowerBreathing();

    public NichirinSwordFlower(Properties properties) {
        super(properties);
    }

    @Override
    public BreathingTechnique getBreathingTechnique() {
        return FLOWER_BREATHING;
    }
}
