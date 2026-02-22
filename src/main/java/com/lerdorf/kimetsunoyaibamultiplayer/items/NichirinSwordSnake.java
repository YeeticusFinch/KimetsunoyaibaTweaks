package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.BaseKnYForms;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingForm;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.util.BaseModFormExecutionHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Serpent Breathing nichirin sword.
 * Wraps base mod forms 801-805 and executes them through the base mod procedure bridge.
 */
public class NichirinSwordSnake extends BreathingSwordItem {
    private static final BreathingTechnique SERPENT_BREATHING = createSerpentBreathing();

    public NichirinSwordSnake(Properties properties) {
        super(properties);
    }

    @Override
    public BreathingTechnique getBreathingTechnique() {
        return SERPENT_BREATHING;
    }

    private static BreathingTechnique createSerpentBreathing() {
        List<BreathingForm> forms = new ArrayList<>();
        int[] formIds = {801, 802, 803, 804, 805};
        for (int id : formIds) {
            BaseKnYForms.BaseForm base = BaseKnYForms.forms.get(id);
            if (base != null) {
                forms.add(new BreathingForm(id, base.name, "", 5,
                    BaseModFormExecutionHelper::executeBaseModForm));
            }
        }
        return new BreathingTechnique("Serpent Breathing", forms, "§5", "§5");
    }
}
