package com.lerdorf.kimetsunoyaibamultiplayer.progression;

import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

public enum DemonSlayerRank {
    MIZUNOTO(1, "Mizunoto", "癸"),
    MIZUNOE(2, "Mizunoe", "壬"),
    KANOTO(3, "Kanoto", "辛"),
    KANOE(4, "Kanoe", "庚"),
    TSUCHINOTO(5, "Tsuchinoto", "己"),
    TSUCHINOE(6, "Tsuchinoe", "戊"),
    HINOTO(7, "Hinoto", "丁"),
    HINOE(8, "Hinoe", "丙"),
    KINOTO(9, "Kinoto", "乙"),
    KINOE(10, "Kinoe", "甲"),
    HASHIRA(11, "Hashira", "柱"),
    STRONGEST(12, "Strongest", "最強");

    private final int level;
    private final String displayName;
    private final String kanji;

    DemonSlayerRank(int level, String displayName, String kanji) {
        this.level = level;
        this.displayName = displayName;
        this.kanji = kanji;
    }

    public int level() {
        return level;
    }

    public String displayName() {
        return displayName;
    }

    public String kanji() {
        return kanji;
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
