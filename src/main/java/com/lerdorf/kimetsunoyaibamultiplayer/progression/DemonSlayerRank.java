package com.lerdorf.kimetsunoyaibamultiplayer.progression;

import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

public enum DemonSlayerRank {
    MIZUNOTO(1, "Mizunoto"),
    MIZUNOE(2, "Mizunoe"),
    KANOTO(3, "Kanoto"),
    KANOE(4, "Kanoe"),
    TSUCHINOTO(5, "Tsuchinoto"),
    TSUCHINOE(6, "Tsuchinoe"),
    HINOTO(7, "Hinoto"),
    HINOE(8, "Hinoe"),
    KINOTO(9, "Kinoto"),
    KINOE(10, "Kinoe"),
    HASHIRA(11, "Hashira"),
    STRONGEST(12, "Strongest");

    private final int level;
    private final String displayName;

    DemonSlayerRank(int level, String displayName) {
        this.level = level;
        this.displayName = displayName;
    }

    public int level() {
        return level;
    }

    public String displayName() {
        return displayName;
    }

    public ResourceLocation advancementId() {
        return ResourceLocation.fromNamespaceAndPath(
            "kimetsunoyaibamultiplayer", name().toLowerCase(Locale.ROOT));
    }

    public static DemonSlayerRank fromLevel(int level) {
        for (DemonSlayerRank rank : values()) {
            if (rank.level == level) {
                return rank;
            }
        }
        return null;
    }

    public static DemonSlayerRank parse(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.toLowerCase(Locale.ROOT).replace('-', '_');
        if ("super_senior".equals(normalized)) {
            normalized = "strongest";
        }

        for (DemonSlayerRank rank : values()) {
            if (rank.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return rank;
            }
        }
        return null;
    }
}
