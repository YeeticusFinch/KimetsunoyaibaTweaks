package com.lerdorf.kimetsunoyaibamultiplayer.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Locale;
import java.util.Map;

public final class LocalizationHelper {
    private static final String BREATHING_FORM_PREFIX = "breathing_form.kimetsunoyaibamultiplayer.";
    private static final String BREATHING_VARIATION_PREFIX = "breathing_form_variation.kimetsunoyaibamultiplayer.";
    private static final String BREATHING_STYLE_PREFIX = "breathing_style.kimetsunoyaibamultiplayer.";
    private static final String ADDON_BREATHING_FORM_PREFIX = "form.knyextraadditions.";
    private static final String ADDON_BREATHING_STYLE_PREFIX = "breathing.knyextraadditions.";
    private static final String BLOOD_DEMON_ART_PREFIX = "blood_demon_art.kimetsunoyaibamultiplayer.";
    private static final String BLOOD_DEMON_ART_FORM_PREFIX = "blood_demon_art_form.kimetsunoyaibamultiplayer.";
    private static final Map<Integer, String> ADDON_FORM_KEYS = Map.ofEntries(
        Map.entry(4001, "4001"),
        Map.entry(4002, "4002"),
        Map.entry(4003, "4003"),
        Map.entry(4004, "4004"),
        Map.entry(4005, "4005"),
        Map.entry(4006, "4006"),
        Map.entry(4007, "4007"),
        Map.entry(4101, "4101"),
        Map.entry(4102, "4102"),
        Map.entry(4103, "4103"),
        Map.entry(4104, "4104"),
        Map.entry(4105, "4105"),
        Map.entry(4106, "4106"),
        Map.entry(4107, "4107"),
        Map.entry(4201, "4201"),
        Map.entry(4202, "4202"),
        Map.entry(4203, "4203"),
        Map.entry(4204, "4204"),
        Map.entry(4205, "4205"),
        Map.entry(4206, "4206"),
        Map.entry(4207, "4207"),
        Map.entry(4301, "4301"),
        Map.entry(4302, "4302"),
        Map.entry(4303, "4303"),
        Map.entry(4304, "4304"),
        Map.entry(4305, "4305"),
        Map.entry(4306, "4306"),
        Map.entry(4307, "4307"),
        Map.entry(6002, "6002"),
        Map.entry(6003, "6003"),
        Map.entry(6004, "6004"),
        Map.entry(6005, "6005"),
        Map.entry(6006, "6006"),
        Map.entry(6007, "6007"),
        Map.entry(6008, "6008"),
        Map.entry(60001, "60001")
    );

    private LocalizationHelper() {
    }

    public static String breathingFormKey(int formId) {
        String addonKey = ADDON_FORM_KEYS.get(formId);
        if (addonKey != null) {
            return ADDON_BREATHING_FORM_PREFIX + addonKey;
        }
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
        if (styleId == null || styleId.isBlank()) {
            return BREATHING_STYLE_PREFIX;
        }

        if (isAddonStyle(styleId)) {
            return ADDON_BREATHING_STYLE_PREFIX + styleId;
        }

        return BREATHING_STYLE_PREFIX + normalizeBaseStyleId(styleId);
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

    public static MutableComponent coloredBreathingSelection(int formId, String fallbackStyleName,
                                                             Component formName, String techniqueColor,
                                                             String formColor) {
        String safeTechniqueColor = sanitizeLegacyColor(techniqueColor);
        String safeFormColor = sanitizeLegacyColor(formColor);
        ChatFormatting techniqueFormatting = parseLegacyFormatting(safeTechniqueColor);
        ChatFormatting formFormatting = parseLegacyFormatting(safeFormColor);
        MutableComponent styleComponent = breathingStyleFromFormId(formId, fallbackStyleName).copy();
        MutableComponent formComponent = formName == null ? Component.empty() : formName.copy();
        if (techniqueFormatting != null) {
            styleComponent.withStyle(techniqueFormatting);
        }
        if (formFormatting != null) {
            formComponent.withStyle(formFormatting);
        }
        return Component.literal(safeTechniqueColor)
            .append(styleComponent)
            .append(Component.literal(" - " + safeFormColor))
            .append(formComponent);
    }

    private static String sanitizeLegacyColor(String color) {
        if (color == null || color.length() < 2 || color.charAt(0) != '§') {
            return "";
        }
        return color.substring(0, 2);
    }

    private static ChatFormatting parseLegacyFormatting(String color) {
        if (color == null || color.length() < 2 || color.charAt(0) != '§') {
            return null;
        }
        return ChatFormatting.getByCode(color.charAt(1));
    }

    private static String breathingStyleIdFromFormId(int formId) {
        if (formId >= 60000 && formId < 60100) return "star_breathing";
        if (formId >= 6000 && formId < 6100) return "star_breathing";
        if (formId >= 4300 && formId < 4400) return "ice_breathing";
        if (formId >= 4200 && formId < 4300) return "frost_breathing";
        if (formId >= 4100 && formId < 4200) return "forest_breathing";
        if (formId >= 4000 && formId < 4100) return "alcohol_breathing";
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

    private static boolean isAddonStyle(String styleId) {
        return "alcohol_breathing".equals(styleId)
            || "forest_breathing".equals(styleId)
            || "frost_breathing".equals(styleId)
            || "ice_breathing".equals(styleId)
            || "star_breathing".equals(styleId);
    }

    private static String normalizeBaseStyleId(String styleId) {
        if (styleId.endsWith("_breathing")) {
            return styleId.substring(0, styleId.length() - "_breathing".length());
        }
        return styleId;
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
