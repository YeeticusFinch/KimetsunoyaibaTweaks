package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.config.CustomProgressionConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.util.DemonEyesHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@OnlyIn(Dist.CLIENT)
public final class DemonEyesResourceHelper {
    private static final Pattern DEMON_EYES_PATTERN = Pattern.compile("textures/entity/demon_eyes_(\\d+)\\.png");
    public static final int KANROJI_EYES_INDEX = 1000;
    public static final int TOKITO_EYES_INDEX = 1001;
    public static final int INOSUKE_EYES_INDEX = 1002;
    public static final int DAUGHTER_EYES_INDEX = 1005;
    public static final int RYOKO_EYES_INDEX = 1006;
    public static final int KANROJI_EYES_1_INDEX = 1007;
    public static final int TOKITO_EYES_1_INDEX = 1008;
    public static final int INOSUKE_EYES_1_INDEX = 1009;
    public static final int DAUGHTER_EYES_1_INDEX = 1010;
    public static final int EMPTY_DEMON_EYES_INDEX = DemonEyesHelper.EMPTY_DEMON_EYES_INDEX;
    public static final int JUSTIN_EYES_0_INDEX = 1012;
    public static final int JUSTIN_EYES_1_INDEX = 1013;
    public static final int JUSTIN_EYES_2_INDEX = 1014;
    public static final int MOTHER_EYES_INDEX = 1015;
    private static final List<NamedEyesStyle> NAMED_STYLES = List.of(
        new NamedEyesStyle(KANROJI_EYES_INDEX, "Kanroji", "textures/entity/oni_kanroji_eyes.png", "textures/entity/oni_kanroji_eyes_kanji.png", "textures/entity/oni_kanroji_eyes_double_kanji.png"),
        new NamedEyesStyle(KANROJI_EYES_1_INDEX, "Kanroji 1", "textures/entity/oni_kanroji_eyes_1.png", "textures/entity/oni_kanroji_eyes_kanji_1.png", "textures/entity/oni_kanroji_eyes_double_kanji_1.png"),
        new NamedEyesStyle(TOKITO_EYES_INDEX, "Tokito", "textures/entity/oni_tokito_eyes.png", "textures/entity/oni_tokito_eyes_kanji.png", "textures/entity/oni_tokito_eyes_double_kanji.png"),
        new NamedEyesStyle(TOKITO_EYES_1_INDEX, "Tokito 1", "textures/entity/oni_tokito_eyes_1.png", "textures/entity/oni_tokito_eyes_kanji_1.png", "textures/entity/oni_tokito_eyes_double_kanji_1.png"),
        new NamedEyesStyle(INOSUKE_EYES_INDEX, "Inosuke", "textures/entity/oni_inosuke_eyes.png", "textures/entity/oni_inosuke_eyes_kanji.png", "textures/entity/oni_inosuke_eyes_double_kanji.png"),
        new NamedEyesStyle(INOSUKE_EYES_1_INDEX, "Inosuke 1", "textures/entity/oni_inosuke_eyes_1.png", "textures/entity/oni_inosuke_eyes_kanji_1.png", "textures/entity/oni_inosuke_eyes_double_kanji_1.png"),
        new NamedEyesStyle(DAUGHTER_EYES_INDEX, "Daughter", "textures/entity/daughter_eyes.png", "textures/entity/daughter_eyes_kanji.png", "textures/entity/daughter_eyes_double_kanji.png"),
        new NamedEyesStyle(DAUGHTER_EYES_1_INDEX, "Daughter 1", "textures/entity/daughter_eyes_1.png", "textures/entity/daughter_eyes_kanji_1.png", "textures/entity/daughter_eyes_double_kanji_1.png"),
        new NamedEyesStyle(RYOKO_EYES_INDEX, "Ryoko", "textures/entity/ryoko_eyes.png", "textures/entity/ryoko_eyes_kanji.png", "textures/entity/ryoko_eyes_double_kanji.png"),
        new NamedEyesStyle(EMPTY_DEMON_EYES_INDEX, "Empty", "textures/entity/demon_eyes_empty.png", null, null),
        new NamedEyesStyle(JUSTIN_EYES_0_INDEX, "Justin 0", "textures/entity/demon_eyes_justin_0.png", null, null),
        new NamedEyesStyle(JUSTIN_EYES_1_INDEX, "Justin 1", "textures/entity/demon_eyes_justin_1.png", null, null),
        new NamedEyesStyle(JUSTIN_EYES_2_INDEX, "Justin 2", "textures/entity/demon_eyes_justin_2.png", null, null),
        new NamedEyesStyle(MOTHER_EYES_INDEX, "Mother", "textures/entity/demon_eyes_mother.png", null, null)
    );

    private DemonEyesResourceHelper() {
    }

    public static List<Integer> getAvailableIndices() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return List.of(DemonEyesHelper.DEFAULT_DEMON_EYES_INDEX);
        }

        List<Integer> indices = new ArrayList<>();
        minecraft.getResourceManager().listResources("textures/entity", location ->
            KimetsunoyaibaMultiplayer.MODID.equals(location.getNamespace()) &&
                DEMON_EYES_PATTERN.matcher(location.getPath()).matches()
        ).keySet().forEach(location -> {
            Matcher matcher = DEMON_EYES_PATTERN.matcher(location.getPath());
            if (matcher.matches()) {
                indices.add(Integer.parseInt(matcher.group(1)));
            }
        });

        if (indices.isEmpty()) {
            indices.add(DemonEyesHelper.DEFAULT_DEMON_EYES_INDEX);
        }
        indices.sort(Comparator.naturalOrder());
        for (NamedEyesStyle style : NAMED_STYLES) {
            if (resourceExists(style.texturePath())
                && (style.index() != EMPTY_DEMON_EYES_INDEX
                    || CustomProgressionConfig.isEmptyDemonEyesAllowed())) {
                indices.add(style.index());
            }
        }
        return indices;
    }

    public static ResourceLocation getTexture(int index) {
        for (NamedEyesStyle style : NAMED_STYLES) {
            if (style.index() == index) {
                return texture(style.texturePath());
            }
        }
        return ResourceLocation.fromNamespaceAndPath(
            KimetsunoyaibaMultiplayer.MODID,
            "textures/entity/demon_eyes_" + Math.max(0, index) + ".png"
        );
    }

    public static ResourceLocation getKanjiTexture(int index) {
        for (NamedEyesStyle style : NAMED_STYLES) {
            if (style.index() == index && resourceExists(style.kanjiTexturePath())) {
                return texture(style.kanjiTexturePath());
            }
        }

        String numericKanjiPath = "textures/entity/demon_eyes_kanji_" + Math.max(0, index) + ".png";
        if (index >= 0 && index < KANROJI_EYES_INDEX && resourceExists(numericKanjiPath)) {
            return texture(numericKanjiPath);
        }
        return null;
    }

    public static ResourceLocation getDoubleKanjiTexture(int index) {
        for (NamedEyesStyle style : NAMED_STYLES) {
            if (style.index() == index && resourceExists(style.doubleKanjiTexturePath())) {
                return texture(style.doubleKanjiTexturePath());
            }
        }

        String numericDoubleKanjiPath = "textures/entity/demon_eyes_double_kanji_" + Math.max(0, index) + ".png";
        if (index >= 0 && index < KANROJI_EYES_INDEX && resourceExists(numericDoubleKanjiPath)) {
            return texture(numericDoubleKanjiPath);
        }
        return null;
    }

    public static String getLabel(int index) {
        for (NamedEyesStyle style : NAMED_STYLES) {
            if (style.index() == index) {
                return style.label();
            }
        }
        return "Style " + index;
    }

    public static float[] getHueTint(int hue) {
        int normalizedHue = DemonEyesHelper.normalizeHue(hue);
        if (normalizedHue == 0) {
            return new float[] {1.0F, 1.0F, 1.0F};
        }

        float h = normalizedHue / 60.0F;
        float c = 1.0F;
        float x = c * (1.0F - Math.abs((h % 2.0F) - 1.0F));
        float r = 0.0F;
        float g = 0.0F;
        float b = 0.0F;
        if (h < 1.0F) {
            r = c;
            g = x;
        } else if (h < 2.0F) {
            r = x;
            g = c;
        } else if (h < 3.0F) {
            g = c;
            b = x;
        } else if (h < 4.0F) {
            g = x;
            b = c;
        } else if (h < 5.0F) {
            r = x;
            b = c;
        } else {
            r = c;
            b = x;
        }
        return new float[] {r, g, b};
    }

    private static boolean resourceExists(String texturePath) {
        Minecraft minecraft = Minecraft.getInstance();
        return texturePath != null
            && minecraft != null
            && minecraft.getResourceManager().getResource(texture(texturePath)).isPresent();
    }

    private static ResourceLocation texture(String texturePath) {
        return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, texturePath);
    }

    private record NamedEyesStyle(int index, String label, String texturePath, String kanjiTexturePath, String doubleKanjiTexturePath) {
    }
}
