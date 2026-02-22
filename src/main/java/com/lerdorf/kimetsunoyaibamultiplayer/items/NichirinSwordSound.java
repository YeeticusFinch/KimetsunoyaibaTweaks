package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.BaseKnYForms;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingForm;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.util.BaseModFormExecutionHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Sound Breathing nichirin sword.
 * Wraps base mod forms while skipping Sound Breathing form 5.
 */
public class NichirinSwordSound extends BreathingSwordItem {
    private static final BreathingTechnique SOUND_BREATHING = createSoundBreathing();

    public NichirinSwordSound(Properties properties) {
        super(properties);
    }

    @Override
    public BreathingTechnique getBreathingTechnique() {
        return SOUND_BREATHING;
    }

    private static BreathingTechnique createSoundBreathing() {
        List<BreathingForm> forms = new ArrayList<>();
        int[] formIds = {901, 902, 904};
        for (int id : formIds) {
            BaseKnYForms.BaseForm base = BaseKnYForms.forms.get(id);
            if (base != null) {
                forms.add(new BreathingForm(id, base.name, "", 5,
                    BaseModFormExecutionHelper::executeBaseModForm));
            }
        }
        return new BreathingTechnique("Sound Breathing", forms, "§6", "§6");
    }
}
