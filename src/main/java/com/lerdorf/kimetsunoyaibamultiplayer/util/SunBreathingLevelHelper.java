package com.lerdorf.kimetsunoyaibamultiplayer.util;

import com.lerdorf.kimetsunoyaibamultiplayer.BaseKnYForms;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingForm;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles persistent sun breathing progression (0-12) on entity/player NBT.
 */
public final class SunBreathingLevelHelper {
    public static final int MIN_LEVEL = 0;
    public static final int MAX_LEVEL = 12;
    public static final String SUN_BREATHING_LEVEL_TAG = "KnYMPSunBreathingLevel";

    private SunBreathingLevelHelper() {
    }

    public static int getSunBreathingLevel(Entity entity) {
        if (entity == null) {
            return 0;
        }
        return clampLevel(entity.getPersistentData().getInt(SUN_BREATHING_LEVEL_TAG));
    }

    public static void setSunBreathingLevel(Entity entity, int level) {
        if (entity == null) {
            return;
        }
        entity.getPersistentData().putInt(SUN_BREATHING_LEVEL_TAG, clampLevel(level));
    }

    public static int clampLevel(int level) {
        if (level < MIN_LEVEL) {
            return MIN_LEVEL;
        }
        return Math.min(level, MAX_LEVEL);
    }

    public static List<BreathingForm> createUnlockedSunForms(int level) {
        int clamped = clampLevel(level);
        List<BreathingForm> forms = new ArrayList<>();
        if (clamped <= 0) {
            return forms;
        }

        int[] sunFormIds = BaseModStyleMapping.getFormsForStyle(1200);
        int limit = Math.min(clamped, sunFormIds.length);
        for (int i = 0; i < limit; i++) {
            int formId = sunFormIds[i];
            BaseKnYForms.BaseForm base = BaseKnYForms.forms.get(formId);
            if (base != null) {
                forms.add(new BreathingForm(formId, base.name, "", 5, BaseModFormExecutionHelper::executeBaseModForm));
            }
        }
        return forms;
    }
}
