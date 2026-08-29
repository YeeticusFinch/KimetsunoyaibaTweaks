package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingForm;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.util.BaseModFormExecutionHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Golden nichirin sword.
 * Uses the base mod's golden/senior breathing style abilities
 * (runtime breathes 1000-1004, executed through the base mod procedure bridge).
 */
public class NichirinSwordGold extends BreathingSwordItem {
    private static final BreathingTechnique GOLD_BREATHING = createGoldBreathing();

    public NichirinSwordGold(Properties properties) {
        super(properties);
    }

    @Override
    public BreathingTechnique getBreathingTechnique() {
        return GOLD_BREATHING;
    }

    private static BreathingTechnique createGoldBreathing() {
        List<BreathingForm> forms = new ArrayList<>();
        // Base mod senior/golden forms: 1000 Hikkondero, 1001 Oraa,
        // 1002 Tyodoiikurainoonigairuzyaneka, 1004 Syusse
        int[] formIds = {1000, 1001, 1002, 1004};
        for (int id : formIds) {
            String name;
            switch (id) {
                case 1000 -> name = "Hikkondero";
                case 1001 -> name = "Oraa";
                case 1002 -> name = "Tyodoiikurainoonigairuzyaneka";
                default -> name = "Syusse";
            }
            forms.add(new BreathingForm(id, name, "", 5,
                BaseModFormExecutionHelper::executeBaseModForm));
        }
        return new BreathingTechnique("Golden Breathing", forms, "\u00A76", "\u00A76");
    }
}
