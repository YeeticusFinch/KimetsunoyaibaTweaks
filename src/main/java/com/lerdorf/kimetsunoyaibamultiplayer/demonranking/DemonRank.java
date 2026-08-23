package com.lerdorf.kimetsunoyaibamultiplayer.demonranking;

import net.mcreator.kimetsunoyaiba.init.KimetsunoyaibaModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

/**
 * The Twelve Kizuki ranking ladder plus Demon King, ordered by tier (0 = most prestigious).
 * Each rank falls back to one or more canon boss entities when no player holds it.
 */
public enum DemonRank {
    DEMON_KING(0, "Demon King", 19, 3, 4, KimetsunoyaibaModEntities.MUZAN, KimetsunoyaibaModEntities.TANJIRO_DEMON),
    UPPER_1(1, "Upper 1", 17, 3, 3, KimetsunoyaibaModEntities.KOKUSHIBO),
    UPPER_2(2, "Upper 2", 16, 3, 3, KimetsunoyaibaModEntities.DOMA),
    UPPER_3(3, "Upper 3", 15, 2, 2, KimetsunoyaibaModEntities.AKAZA),
    UPPER_4(4, "Upper 4", 14, 2, 2, KimetsunoyaibaModEntities.NAKIME, KimetsunoyaibaModEntities.HANTENGU),
    UPPER_5(5, "Upper 5", 13, 2, 2, KimetsunoyaibaModEntities.GYOKKO),
    UPPER_6(6, "Upper 6", 12, 2, 2, KimetsunoyaibaModEntities.KAIGAKU, KimetsunoyaibaModEntities.GYUTARO, KimetsunoyaibaModEntities.DAKI),
    // TODO: add an Ubume entity here alongside Enmu once one exists in the base mod.
    LOWER_1(7, "Lower 1", 10, 1, 1, KimetsunoyaibaModEntities.ENMU),
    LOWER_2(8, "Lower 2", 9, 1, 1, KimetsunoyaibaModEntities.ROKURO, KimetsunoyaibaModEntities.HAIRO),
    LOWER_3(9, "Lower 3", 8, 1, 1, KimetsunoyaibaModEntities.WAKURABA),
    LOWER_4(10, "Lower 4", 7, 1, 1, KimetsunoyaibaModEntities.MUKAGO),
    LOWER_5(11, "Lower 5", 6, 1, 1, KimetsunoyaibaModEntities.RUI),
    LOWER_6(12, "Lower 6", 5, 1, 1, KimetsunoyaibaModEntities.KAMANUE, KimetsunoyaibaModEntities.KYOGAI);

    /** Pseudo-tier representing "no rank" — one below Lower 6. */
    public static final int UNRANKED_TIER = LOWER_6.tier + 1;

    private final int tier;
    private final String displayName;
    private final int strengthLevel;
    private final int resistanceLevel;
    private final int regenLevel;
    private final List<RegistryObject<? extends EntityType<?>>> fallbackEntities;

    @SafeVarargs
    DemonRank(int tier, String displayName, int strengthLevel, int resistanceLevel, int regenLevel,
              RegistryObject<? extends EntityType<?>>... fallbackEntities) {
        this.tier = tier;
        this.displayName = displayName;
        this.strengthLevel = strengthLevel;
        this.resistanceLevel = resistanceLevel;
        this.regenLevel = regenLevel;
        this.fallbackEntities = List.of(fallbackEntities);
    }

    public int tier() {
        return tier;
    }

    public String displayName() {
        return displayName;
    }

    public int strengthLevel() {
        return strengthLevel;
    }

    public int resistanceLevel() {
        return resistanceLevel;
    }

    public int regenLevel() {
        return regenLevel;
    }

    public List<RegistryObject<? extends EntityType<?>>> fallbackEntities() {
        return fallbackEntities;
    }

    public boolean isFallbackEntity(EntityType<?> type) {
        for (RegistryObject<? extends EntityType<?>> entity : fallbackEntities) {
            if (entity.isPresent() && entity.get() == type) {
                return true;
            }
        }
        return false;
    }

    /** The rank directly above this one (smaller tier), or null if this is already Demon King. */
    public DemonRank above() {
        return byTier(tier - 1);
    }

    /** The rank directly below this one (larger tier), or null if this is already Lower 6. */
    public DemonRank below() {
        return byTier(tier + 1);
    }

    public static DemonRank byTier(int tier) {
        for (DemonRank rank : values()) {
            if (rank.tier == tier) {
                return rank;
            }
        }
        return null;
    }

    public static DemonRank fromFallbackEntity(EntityType<?> type) {
        if (type == null) {
            return null;
        }
        for (DemonRank rank : values()) {
            if (rank.isFallbackEntity(type)) {
                return rank;
            }
        }
        return null;
    }
}
