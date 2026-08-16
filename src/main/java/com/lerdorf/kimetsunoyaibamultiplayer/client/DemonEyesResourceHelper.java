package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
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
    private static final List<NamedEyesStyle> NAMED_STYLES = List.of(
        new NamedEyesStyle(KANROJI_EYES_INDEX, "Kanroji", "textures/entity/oni_kanroji_eyes.png"),
        new NamedEyesStyle(TOKITO_EYES_INDEX, "Tokito", "textures/entity/oni_tokito_eyes.png"),
        new NamedEyesStyle(INOSUKE_EYES_INDEX, "Inosuke", "textures/entity/oni_inosuke_eyes.png")
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
            if (resourceExists(style.texturePath())) {
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
        return minecraft != null && minecraft.getResourceManager().getResource(texture(texturePath)).isPresent();
    }

    private static ResourceLocation texture(String texturePath) {
        return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, texturePath);
    }

    private record NamedEyesStyle(int index, String label, String texturePath) {
    }
}
