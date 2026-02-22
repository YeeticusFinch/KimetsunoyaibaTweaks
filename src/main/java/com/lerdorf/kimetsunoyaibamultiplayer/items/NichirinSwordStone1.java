package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.BaseKnYForms;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingForm;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.util.BaseModFormExecutionHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Stone Breathing nichirin sword (variant 1).
 * Wraps base mod forms 601-604 and executes them through the base mod procedure bridge.
 */
public class NichirinSwordStone1 extends BreathingSwordItem {
    private static final BreathingTechnique STONE_BREATHING = createStoneBreathing();

    public NichirinSwordStone1(Properties properties) {
        super(properties);
    }

    @Override
    public BreathingTechnique getBreathingTechnique() {
        return STONE_BREATHING;
    }

    private static BreathingTechnique createStoneBreathing() {
        List<BreathingForm> forms = new ArrayList<>();
        int[] formIds = {601, 602, 603, 604};
        for (int id : formIds) {
            BaseKnYForms.BaseForm base = BaseKnYForms.forms.get(id);
            if (base != null) {
                forms.add(new BreathingForm(id, base.name, "", 5,
                    BaseModFormExecutionHelper::executeBaseModForm));
            }
        }
        return new BreathingTechnique("Stone Breathing", forms, "§7", "§7");
    }
}
