package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class DemonEyeKanjiHelper {
    private static final double CENTERED_KANJI_EPSILON = 1.0E-6D;
    private static final Map<Integer, EyeKanjiPlacement> PLACEMENTS = createPlacements();

    private DemonEyeKanjiHelper() {
    }

    public static EyeKanjiPlacement getPlacement(int demonEyesIndex) {
        return PLACEMENTS.getOrDefault(demonEyesIndex, EyeKanjiPlacement.DEFAULT);
    }

    public static ResourceLocation getEyeOverlayTexture(int demonEyesIndex, int rankTier) {
        if (getTexture(rankTier) != null) {
            if (shouldRenderMirroredUpperKanji(rankTier, getPlacement(demonEyesIndex))) {
                ResourceLocation doubleKanjiOverlay = DemonEyesResourceHelper.getDoubleKanjiTexture(demonEyesIndex);
                if (doubleKanjiOverlay != null) {
                    return doubleKanjiOverlay;
                }
            }
            ResourceLocation kanjiOverlay = DemonEyesResourceHelper.getKanjiTexture(demonEyesIndex);
            if (kanjiOverlay != null) {
                return kanjiOverlay;
            }
        }
        return DemonEyesResourceHelper.getTexture(demonEyesIndex);
    }

    public static ResourceLocation getTexture(int rankTier) {
        String path = getTexturePath(rankTier);
        if (path == null || !resourceExists(path)) {
            return null;
        }
        return texture(path);
    }

    public static ResourceLocation getRightTexture(int rankTier) {
        return getUpperTexture(rankTier, "_r");
    }

    public static ResourceLocation getLeftTexture(int rankTier) {
        return getUpperTexture(rankTier, "_l");
    }

    public static boolean shouldRenderMirroredUpperKanji(int rankTier, EyeKanjiPlacement placement) {
        return isUpperRank(rankTier)
            && Math.abs(placement.xOffset()) > CENTERED_KANJI_EPSILON
            && getRightTexture(rankTier) != null
            && getLeftTexture(rankTier) != null;
    }

    public static boolean isUpperRank(int rankTier) {
        return rankTier >= 1 && rankTier <= 6;
    }

    private static String getTexturePath(int rankTier) {
        if (rankTier >= 1 && rankTier <= 6) {
            return "textures/entity/upper" + rankTier + ".png";
        }
        if (rankTier >= 7 && rankTier <= 12) {
            return "textures/entity/lower" + (rankTier - 6) + ".png";
        }
        if (rankTier == 0) {
            return "textures/entity/demon_king.png";
        }
        return null;
    }

    private static ResourceLocation getUpperTexture(int rankTier, String suffix) {
        if (!isUpperRank(rankTier)) {
            return null;
        }
        String path = "textures/entity/upper" + rankTier + suffix + ".png";
        return resourceExists(path) ? texture(path) : null;
    }

    private static Map<Integer, EyeKanjiPlacement> createPlacements() {
        Map<Integer, EyeKanjiPlacement> placements = new HashMap<>();

        placements.put(0, new EyeKanjiPlacement(-1.60D, 1.65D, 1.05D, 1.05D, 0.00D));
        placements.put(1, new EyeKanjiPlacement(-1.62D, 1.72D, 0.95D, 0.95D, 0.00D));
        placements.put(2, new EyeKanjiPlacement(-1.85D, 0.50D, 1.00D, 1.00D, 0.00D));
        placements.put(3, new EyeKanjiPlacement(-1.90D, 2.00D, 1.70D, 1.70D, 0.00D));
        placements.put(4, new EyeKanjiPlacement(-1.90D, 1.05D, 1.70D, 1.70D, 0.00D));
        placements.put(5, new EyeKanjiPlacement(-2.00D, 1.52D, 1.10D, 1.10D, 0.00D));
        placements.put(6, new EyeKanjiPlacement(-2.00D, 2.52D, 1.10D, 1.10D, 0.00D));
        placements.put(7, new EyeKanjiPlacement(-2.20D, 1.62D, 1.30D, 1.30D, 0.00D));
        placements.put(8, new EyeKanjiPlacement(-1.95D, 0.02D, 1.70D, 1.70D, 0.00D));
        placements.put(9, new EyeKanjiPlacement(-1.95D, -0.48D, 1.00D, 1.00D, 0.00D));
        placements.put(10, new EyeKanjiPlacement(-1.45D, 0.62D, 0.80D, 0.80D, 0.00D));
        placements.put(11, new EyeKanjiPlacement(0.00D, 1.47D, 1.30D, 1.30D, 0.00D));

        placements.put(DemonEyesResourceHelper.KANROJI_EYES_INDEX, new EyeKanjiPlacement(-2.00D, 1.75D, 1.30D, 1.30D, 0.00D));
        placements.put(DemonEyesResourceHelper.KANROJI_EYES_1_INDEX, new EyeKanjiPlacement(-2.00D, 0.50D, 1.30D, 1.30D, 0.00D));
        placements.put(DemonEyesResourceHelper.TOKITO_EYES_INDEX, new EyeKanjiPlacement(-1.95D, 1.55D, 0.85D, 0.85D, -5.00D));
        placements.put(DemonEyesResourceHelper.TOKITO_EYES_1_INDEX, new EyeKanjiPlacement(-1.95D, 0.85D, 0.85D, 0.85D, 0.00D));
        placements.put(DemonEyesResourceHelper.INOSUKE_EYES_INDEX, new EyeKanjiPlacement(-1.80D, 2.00D, 1.30D, 1.30D, 0.00D));
        placements.put(DemonEyesResourceHelper.INOSUKE_EYES_1_INDEX, new EyeKanjiPlacement(-1.80D, 0.80D, 1.30D, 1.30D, 0.00D));
        placements.put(DemonEyesResourceHelper.DAUGHTER_EYES_INDEX, new EyeKanjiPlacement(-1.65D, 1.72D, 1.30D, 1.30D, 0.00D));
        placements.put(DemonEyesResourceHelper.DAUGHTER_EYES_1_INDEX, new EyeKanjiPlacement(-1.65D, 0.25D, 1.50D, 1.30D, 0.00D));
        placements.put(DemonEyesResourceHelper.RYOKO_EYES_INDEX, new EyeKanjiPlacement(-1.05D, 1.50D, 0.90D, 0.90D, 0.00D));
        EyeKanjiPlacement justinPlacement = new EyeKanjiPlacement(-1.50D, 1.00D, 1.00D, 1.00D, 0.00D);
        placements.put(DemonEyesResourceHelper.JUSTIN_EYES_0_INDEX, justinPlacement);
        placements.put(DemonEyesResourceHelper.JUSTIN_EYES_1_INDEX, justinPlacement);
        placements.put(DemonEyesResourceHelper.JUSTIN_EYES_2_INDEX, justinPlacement);
        placements.put(DemonEyesResourceHelper.MOTHER_EYES_INDEX,
            new EyeKanjiPlacement(-1.49D, 1.50D, 1.00D, 1.00D, 0.00D));

        return Map.copyOf(placements);
    }

    private static boolean resourceExists(String texturePath) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null && minecraft.getResourceManager().getResource(texture(texturePath)).isPresent();
    }

    private static ResourceLocation texture(String texturePath) {
        return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, texturePath);
    }

    public record EyeKanjiPlacement(double xOffset, double yOffset, double width, double height, double rotation) {
        public static final EyeKanjiPlacement DEFAULT = new EyeKanjiPlacement(-1.55D, -1.00D, 1.00D, 1.00D, 0.0D);
    }
}
