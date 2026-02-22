package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.BaseKnYForms;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingForm;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.util.BaseModFormExecutionHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Insect Breathing nichirin sword.
 * Wraps base mod forms 1401, 1402, 1404, 1405 and executes them through the base mod procedure bridge.
 */
public class NichirinSwordInsect extends BreathingSwordItem {
    private static final BreathingTechnique INSECT_BREATHING = createInsectBreathing();

    public NichirinSwordInsect(Properties properties) {
        super(properties);
    }

    @Override
    public BreathingTechnique getBreathingTechnique() {
        return INSECT_BREATHING;
    }

    private static BreathingTechnique createInsectBreathing() {
        List<BreathingForm> forms = new ArrayList<>();
        int[] formIds = {1401, 1402, 1404, 1405};
        for (int id : formIds) {
            BaseKnYForms.BaseForm base = BaseKnYForms.forms.get(id);
            if (base != null) {
                forms.add(new BreathingForm(id, base.name, "", 5,
                    BaseModFormExecutionHelper::executeBaseModForm));
            }
        }
        return new BreathingTechnique("Insect Breathing", forms, "§5", "§5");
    }
}
