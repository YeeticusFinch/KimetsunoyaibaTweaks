package com.lerdorf.kimetsunoyaibamultiplayer.compat;

import com.lerdorf.kimetsunoyaibamultiplayer.config.CustomProgressionConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;

/**
 * Resolves the preferred infinity castle dimension for this mod.
 */
public final class InfinityCastleCompat {
    private static final String BASE_MODID = "kimetsunoyaiba";
    private static final String ENHANCED_MODID = "kny_worlds";
    private static final String MIN_ENHANCED_VERSION = "1.0.1";

    public static final ResourceLocation BASE_INFINITY_CASTLE_ID =
        ResourceLocation.fromNamespaceAndPath(BASE_MODID, "mugen_castle_dimension");
    public static final ResourceLocation LEGACY_INFINITY_CASTLE_ID =
        ResourceLocation.fromNamespaceAndPath(BASE_MODID, "infinity_castle_dimension");
    public static final ResourceLocation ENHANCED_INFINITY_CASTLE_ID =
        ResourceLocation.fromNamespaceAndPath(ENHANCED_MODID, "infinity_castle");

    public static final ResourceKey<Level> BASE_INFINITY_CASTLE =
        ResourceKey.create(Registries.DIMENSION, BASE_INFINITY_CASTLE_ID);
    public static final ResourceKey<Level> LEGACY_INFINITY_CASTLE =
        ResourceKey.create(Registries.DIMENSION, LEGACY_INFINITY_CASTLE_ID);
    public static final ResourceKey<Level> ENHANCED_INFINITY_CASTLE =
        ResourceKey.create(Registries.DIMENSION, ENHANCED_INFINITY_CASTLE_ID);

    private InfinityCastleCompat() {
    }

    public static boolean isCastleDimension(ResourceKey<Level> dimension) {
        return BASE_INFINITY_CASTLE.equals(dimension)
            || LEGACY_INFINITY_CASTLE.equals(dimension)
            || ENHANCED_INFINITY_CASTLE.equals(dimension);
    }

    public static boolean canUseEnhancedInfinityCastle() {
        return CustomProgressionConfig.isEnhancedInfinityCastleEnabled()
            && isInstalledVersionAtLeast(MIN_ENHANCED_VERSION);
    }

    public static ResourceKey<Level> resolveCastleEntryDimension(MinecraftServer server) {
        if (server == null) {
            return BASE_INFINITY_CASTLE;
        }

        if (canUseEnhancedInfinityCastle() && server.getLevel(ENHANCED_INFINITY_CASTLE) != null) {
            return ENHANCED_INFINITY_CASTLE;
        }

        if (server.getLevel(BASE_INFINITY_CASTLE) != null) {
            return BASE_INFINITY_CASTLE;
        }

        if (server.getLevel(LEGACY_INFINITY_CASTLE) != null) {
            return LEGACY_INFINITY_CASTLE;
        }

        return BASE_INFINITY_CASTLE;
    }

    public static ServerLevel resolveCastleEntryLevel(MinecraftServer server) {
        if (server == null) {
            return null;
        }

        ResourceKey<Level> dimension = resolveCastleEntryDimension(server);
        ServerLevel level = server.getLevel(dimension);
        if (level != null) {
            return level;
        }

        if (!BASE_INFINITY_CASTLE.equals(dimension)) {
            ServerLevel fallback = server.getLevel(BASE_INFINITY_CASTLE);
            if (fallback != null) {
                return fallback;
            }
        }

        if (!LEGACY_INFINITY_CASTLE.equals(dimension)) {
            ServerLevel fallback = server.getLevel(LEGACY_INFINITY_CASTLE);
            if (fallback != null) {
                return fallback;
            }
        }

        return server.getLevel(ENHANCED_INFINITY_CASTLE);
    }

    private static boolean isInstalledVersionAtLeast(String minimumVersion) {
        return ModList.get().getModContainerById(ENHANCED_MODID)
            .map(container -> compareVersions(normalizeVersion(container.getModInfo().getVersion().toString()), minimumVersion) >= 0)
            .orElse(false);
    }

    private static String normalizeVersion(String version) {
        return version == null ? "" : version.trim();
    }

    private static int compareVersions(String actualVersion, String minimumVersion) {
        ParsedVersion actual = ParsedVersion.parse(actualVersion);
        ParsedVersion minimum = ParsedVersion.parse(minimumVersion);

        int maxLength = Math.max(actual.numbers.length, minimum.numbers.length);
        for (int i = 0; i < maxLength; i++) {
            int actualPart = i < actual.numbers.length ? actual.numbers[i] : 0;
            int minimumPart = i < minimum.numbers.length ? minimum.numbers[i] : 0;
            if (actualPart != minimumPart) {
                return Integer.compare(actualPart, minimumPart);
            }
        }

        if (actual.preRelease != minimum.preRelease) {
            return actual.preRelease ? -1 : 1;
        }

        return 0;
    }

    private static final class ParsedVersion {
        private final int[] numbers;
        private final boolean preRelease;

        private ParsedVersion(int[] numbers, boolean preRelease) {
            this.numbers = numbers;
            this.preRelease = preRelease;
        }

        private static ParsedVersion parse(String version) {
            if (version == null || version.isBlank()) {
                return new ParsedVersion(new int[0], true);
            }

            String[] parts = version.split("\\.");
            int[] numbers = new int[parts.length];
            boolean preRelease = false;

            for (int i = 0; i < parts.length; i++) {
                String part = parts[i].trim();
                int hyphenIndex = part.indexOf('-');
                if (hyphenIndex >= 0) {
                    preRelease = true;
                    part = part.substring(0, hyphenIndex);
                }

                int value = 0;
                int cursor = 0;
                while (cursor < part.length() && Character.isDigit(part.charAt(cursor))) {
                    value = value * 10 + (part.charAt(cursor) - '0');
                    cursor++;
                }

                numbers[i] = value;
            }

            return new ParsedVersion(numbers, preRelease);
        }
    }
}
