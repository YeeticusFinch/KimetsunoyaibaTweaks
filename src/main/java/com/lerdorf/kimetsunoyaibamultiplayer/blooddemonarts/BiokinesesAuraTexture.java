package com.lerdorf.kimetsunoyaibamultiplayer.blooddemonarts;

import java.util.Locale;

/** Client-resolved textures used by Biokineses aura projections. */
public enum BiokinesesAuraTexture {
    DARK("dark"),
    RED_CIRCLES("red"),
    CRACKED("cracked"),
    WHITE("white");

    private final String id;

    BiokinesesAuraTexture(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static BiokinesesAuraTexture fromId(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        for (BiokinesesAuraTexture texture : values()) {
            if (texture.id.equals(normalized) || texture.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return texture;
            }
        }
        return null;
    }
}
