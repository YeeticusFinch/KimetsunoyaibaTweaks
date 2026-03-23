package com.lerdorf.kimetsunoyaibamultiplayer.util;

import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class LocalizationHelper {
    private static final String BREATHING_FORM_PREFIX = "breathing_form.kimetsunoyaibamultiplayer.";
    private static final String BREATHING_VARIATION_PREFIX = "breathing_form_variation.kimetsunoyaibamultiplayer.";
    private static final String BREATHING_STYLE_PREFIX = "breathing_style.kimetsunoyaibamultiplayer.";
    private static final String BLOOD_DEMON_ART_PREFIX = "blood_demon_art.kimetsunoyaibamultiplayer.";
    private static final String BLOOD_DEMON_ART_FORM_PREFIX = "blood_demon_art_form.kimetsunoyaibamultiplayer.";

    private LocalizationHelper() {
    }

    public static String breathingFormKey(int formId) {
        return BREATHING_FORM_PREFIX + formId;
    }

    public static Component breathingForm(int formId) {
        return Component.translatable(breathingFormKey(formId));
    }

    public static String breathingVariationKey(String name) {
        return BREATHING_VARIATION_PREFIX + slugify(name);
    }

    public static Component breathingVariation(String name) {
        return Component.translatable(breathingVariationKey(name));
    }

    public static String breathingStyleKey(String styleId) {
        return BREATHING_STYLE_PREFIX + styleId;
    }

    public static Component breathingStyle(String styleId) {
        return Component.translatable(breathingStyleKey(styleId));
    }

    public static Component breathingStyleFromFormId(int formId, String fallback) {
        String styleId = breathingStyleIdFromFormId(formId);
        if (styleId == null) {
            return Component.literal(fallback == null ? "" : fallback);
        }
        return breathingStyle(styleId);
    }

    private static String breathingStyleIdFromFormId(int formId) {
        if (formId >= 25000 && formId < 26000) return "beast";
        if (formId >= 24000 && formId < 25000) return "flower";
        if (formId >= 22000 && formId < 23000) return "love";
        if (formId >= 20000 && formId < 21000) return "mist";
        if (formId >= 1800 && formId < 1900) return "sakura";
        if (formId >= 1500 && formId < 1600) return "love";
        if (formId >= 1400 && formId < 1500) return "insect";
        if (formId >= 1300 && formId < 1400) return "flower";
        if (formId >= 1200 && formId < 1300) return "sun";
        if (formId >= 1100 && formId < 1200) return "moon";
        if (formId >= 900 && formId < 1000) return "sound";
        if (formId >= 800 && formId < 900) return "serpent";
        if (formId >= 700 && formId < 800) return "mist";
        if (formId >= 600 && formId < 700) return "stone";
        if (formId >= 500 && formId < 600) return "wind";
        if (formId >= 400 && formId < 500) return "flame";
        if (formId >= 300 && formId < 400) return "thunder";
        if (formId >= 200 && formId < 300) return "beast";
        if (formId >= 100 && formId < 200) return "water";
        if (formId >= 1 && formId < 100) return "bamboo";
        return null;
    }

    public static String bloodDemonArtKey(String artId) {
        return BLOOD_DEMON_ART_PREFIX + artId;
    }

    public static Component bloodDemonArt(String artId) {
        return Component.translatable(bloodDemonArtKey(artId));
    }

    public static String bloodDemonArtFormKey(int formId) {
        return BLOOD_DEMON_ART_FORM_PREFIX + formId;
    }

    public static Component bloodDemonArtForm(int formId) {
        return Component.translatable(bloodDemonArtFormKey(formId));
    }

    private static String slugify(String value) {
        StringBuilder builder = new StringBuilder();
        String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT);
        boolean previousUnderscore = false;

        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            if ((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9')) {
                builder.append(ch);
                previousUnderscore = false;
            } else if (!previousUnderscore) {
                builder.append('_');
                previousUnderscore = true;
            }
        }

        int length = builder.length();
        while (length > 0 && builder.charAt(length - 1) == '_') {
            builder.deleteCharAt(length - 1);
            length--;
        }
        return builder.toString();
    }
}
