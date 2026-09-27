package com.lerdorf.kimetsunoyaibamultiplayer.blooddemonarts;

import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtForm;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.api.KnYAPI;

import java.util.List;

/** Placeholder form registry for Kyogai's future drum blood demon art abilities. */
public final class KyogaiDrumsArt {
    public static final String ART_ID = "kyogai_drums";

    public static final int FORM_RIGHT_SHOULDER_DRUM = 3700;
    public static final int FORM_LEFT_SHOULDER_DRUM = 3701;
    public static final int FORM_RIGHT_LEG_DRUM = 3702;
    public static final int FORM_LEFT_LEG_DRUM = 3703;
    public static final int FORM_NAVEL_DRUM = 3704;
    public static final int FORM_BACK_DRUM_SELF = 3705;
    public static final int FORM_BACK_DRUM_TARGET = 3706;

    private KyogaiDrumsArt() {
    }

    public static void register() {
        if (!BloodDemonArtRegistry.isRegistered(ART_ID)) {
            KnYAPI.registerBloodDemonArt(ART_ID, "Blood Demon Art: Kyogai's Drums", createTechnique());
        }
    }

    public static BloodDemonArtTechnique createTechnique() {
        return new BloodDemonArtTechnique(
            "Blood Demon Art: Kyogai's Drums",
            List.of(
                placeholder(FORM_RIGHT_SHOULDER_DRUM, "Right Shoulder Drum"),
                placeholder(FORM_LEFT_SHOULDER_DRUM, "Left Shoulder Drum"),
                placeholder(FORM_RIGHT_LEG_DRUM, "Right Leg Drum"),
                placeholder(FORM_LEFT_LEG_DRUM, "Left Leg Drum"),
                placeholder(FORM_NAVEL_DRUM, "Navel Drum"),
                placeholder(FORM_BACK_DRUM_SELF, "Back Drum (self)"),
                placeholder(FORM_BACK_DRUM_TARGET, "Back Drum (target)")
            ),
            0xAA1E2F
        );
    }

    private static BloodDemonArtForm placeholder(int formId, String name) {
        return new BloodDemonArtForm(
            formId,
            name,
            "Placeholder ability; no effect yet.",
            1,
            (entity, level, ignoredFormId) -> {
                // Intentionally empty until the drum abilities are implemented.
            }
        );
    }
}
