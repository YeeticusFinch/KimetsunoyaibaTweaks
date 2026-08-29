package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingForm;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.util.BaseModFormExecutionHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Bamboo Breathing nichirin sword.
 * Wraps base mod bamboo forms (runtime 1701-1712) and executes them through
 * the base mod procedure bridge.
 */
public class NichirinSwordBamboo extends BreathingSwordItem {
    private static final BreathingTechnique BAMBOO_BREATHING = createBambooBreathing();

    public NichirinSwordBamboo(Properties properties) {
        super(properties);
    }

    @Override
    public BreathingTechnique getBreathingTechnique() {
        return BAMBOO_BREATHING;
    }

    private static BreathingTechnique createBambooBreathing() {
        List<BreathingForm> forms = new ArrayList<>();
        // Base mod bamboo runtime IDs: 1701-1712 (12 forms).
        int[][] formsData = {
            {1701, 1}, {1702, 2}, {1703, 3}, {1704, 4}, {1705, 5}, {1706, 6},
            {1707, 7}, {1708, 8}, {1709, 9}, {1710, 10}, {1711, 11}, {1712, 12}
        };
        for (int[] fd : formsData) {
            int id = fd[0];
            int formNumber = fd[1];
            forms.add(new BreathingForm(id, bambooFormName(formNumber), "", 5,
                BaseModFormExecutionHelper::executeBaseModForm));
        }
        return new BreathingTechnique("Bamboo Breathing", forms, "\u00A7a", "\u00A7a");
    }

    private static String bambooFormName(int formNumber) {
        return switch (formNumber) {
            case 1 -> "1st Form: Bamboo Blade";
            case 2 -> "2nd Form: Bamboo Way";
            case 3 -> "3rd Form: Bamboo Basket";
            case 4 -> "4th Form: Bamboo-Copter";
            case 5 -> "5th Form: Stilt";
            case 6 -> "6th Form: Bamboo Impact";
            case 7 -> "7th Form: Bamboo Fang Demon";
            case 8 -> "8th Form: Bamboo Grass Enumeration";
            case 9 -> "9th Form: Firecracker";
            case 10 -> "10th Form: Takemikazuchi no Kami";
            case 11 -> "11th Form: Shishi-Odoshi";
            case 12 -> "12th Form: BAMBOO";
            default -> formNumber + "th Form";
        };
    }
}
