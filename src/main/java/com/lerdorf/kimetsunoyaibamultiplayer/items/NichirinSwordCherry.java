package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingForm;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.util.BaseModFormExecutionHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Sakura (Cherry Blossom) Breathing nichirin sword.
 * Wraps base mod sakura forms (runtime 1801-1810) and executes them through
 * the base mod procedure bridge.
 */
public class NichirinSwordCherry extends BreathingSwordItem {
    private static final BreathingTechnique SAKURA_BREATHING = createSakuraBreathing();

    public NichirinSwordCherry(Properties properties) {
        super(properties);
    }

    @Override
    public BreathingTechnique getBreathingTechnique() {
        return SAKURA_BREATHING;
    }

    private static BreathingTechnique createSakuraBreathing() {
        List<BreathingForm> forms = new ArrayList<>();
        // Base mod sakura runtime IDs: 1801-1806, 1808, 1809, 1810 (no 7th form).
        int[][] formsData = {
            {1801, 1}, {1802, 2}, {1803, 3}, {1804, 4}, {1805, 5},
            {1806, 6}, {1808, 8}, {1809, 9}, {1810, 10}
        };
        for (int[] fd : formsData) {
            int id = fd[0];
            int formNumber = fd[1];
            forms.add(new BreathingForm(id, sakuraFormName(formNumber), "", 5,
                BaseModFormExecutionHelper::executeBaseModForm));
        }
        return new BreathingTechnique("Sakura Breathing", forms, "\u00A7d", "\u00A7d");
    }

    private static String sakuraFormName(int formNumber) {
        return switch (formNumber) {
            case 1 -> "1st Form: Sakura Petals Dance";
            case 2 -> "2nd Form: Sakura Flowing";
            case 3 -> "3rd Form: Falling Full-Blown Sakura Petals";
            case 4 -> "4th Form: Sakura Rain";
            case 5 -> "5th Form: Phantom Water Surface Sakura";
            case 6 -> "6th Form: Sakura Storm Flash";
            case 8 -> "8th Form: Thousands Sakura";
            case 9 -> "9th Form: Ephemeral Spring Dream";
            case 10 -> "10th Form: Blooming in Profusion - Turbulence Sakura";
            default -> formNumber + "th Form";
        };
    }
}
