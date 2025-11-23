package com.lerdorf.kimetsunoyaibamultiplayer.raids;

import com.lerdorf.kimetsunoyaibamultiplayer.config.RaidConfig;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Wave generator with full support for 5 difficulty levels.
 * Wave compositions match the specifications in docs/raids.md.
 */
public class WaveGenerator {

    /**
     * Generate entities for a specific wave based on raid type, difficulty, and wave number.
     */
    public static List<ResourceLocation> generateWave(KnYRaid.RaidType type, int difficultyLevel, int waveNumber) {
        return switch (type) {
            case DEMON -> generateDemonWave(difficultyLevel, waveNumber);
            case SLAYER -> generateSlayerWave(difficultyLevel, waveNumber);
        };
    }

    // ============================================
    // DEMON RAID WAVES (from docs/raids.md)
    // ============================================

    private static List<ResourceLocation> generateDemonWave(int difficulty, int wave) {
        List<ResourceLocation> out = new ArrayList<>();

        switch (difficulty) {
            case 1 -> {
                // omen_of_muzan I: 3 waves
                switch (wave) {
                    case 1 -> out.addAll(pick(EntityPowerScale.EASY_DEMON, 6));
                    case 2 -> {
                        out.addAll(pick(EntityPowerScale.EASY_DEMON, 3));
                        out.addAll(pick(EntityPowerScale.MEDIUM_DEMON, 3));
                    }
                    case 3 -> {
                        // 1 hard demon as boss, 2 medium, 2 easy
                        out.addAll(pick(EntityPowerScale.HARD_DEMON, 1));
                        out.addAll(pick(EntityPowerScale.MEDIUM_DEMON, 2));
                        out.addAll(pick(EntityPowerScale.EASY_DEMON, 2));
                    }
                }
            }
            case 2 -> {
                // omen_of_muzan II: 4 waves
                switch (wave) {
                    case 1 -> out.addAll(pick(EntityPowerScale.EASY_DEMON, 6));
                    case 2 -> {
                        out.addAll(pick(EntityPowerScale.EASY_DEMON, 5));
                        out.addAll(pick(EntityPowerScale.MEDIUM_DEMON, 5));
                    }
                    case 3 -> {
                        out.addAll(pick(EntityPowerScale.EASY_DEMON, 6));
                        out.addAll(pick(EntityPowerScale.MEDIUM_DEMON, 6));
                    }
                    case 4 -> {
                        // 2 hard demons as bosses, 4 medium, 4 easy
                        out.addAll(pick(EntityPowerScale.HARD_DEMON, 2));
                        out.addAll(pick(EntityPowerScale.MEDIUM_DEMON, 4));
                        out.addAll(pick(EntityPowerScale.EASY_DEMON, 4));
                    }
                }
            }
            case 3 -> {
                // omen_of_muzan III: 5 waves (inferred)
                switch (wave) {
                    case 1 -> {
                        out.addAll(pick(EntityPowerScale.EASY_DEMON, 8));
                    }
                    case 2 -> {
                        out.addAll(pick(EntityPowerScale.EASY_DEMON, 6));
                        out.addAll(pick(EntityPowerScale.MEDIUM_DEMON, 6));
                    }
                    case 3 -> {
                        out.addAll(pick(EntityPowerScale.MEDIUM_DEMON, 10));
                        out.addAll(pick(EntityPowerScale.EASY_DEMON, 10));
                    }
                    case 4 -> {
                        out.addAll(pick(EntityPowerScale.HARD_DEMON, 4));
                        out.addAll(pick(EntityPowerScale.MEDIUM_DEMON, 8));
                    }
                    case 5 -> {
                        // 1 medium boss, 3 hard, 6 medium
                        out.addAll(pick(EntityPowerScale.MEDIUM_BOSS_DEMON, 1));
                        out.addAll(pick(EntityPowerScale.HARD_DEMON, 3));
                        out.addAll(pick(EntityPowerScale.MEDIUM_DEMON, 6));
                    }
                }
            }
            case 4 -> {
                // omen_of_muzan IV: 6 waves (inferred)
                switch (wave) {
                    case 1 -> {
                        out.addAll(pick(EntityPowerScale.HARD_DEMON, 1));
                        out.addAll(pick(EntityPowerScale.MEDIUM_DEMON, 6));
                        out.addAll(pick(EntityPowerScale.EASY_DEMON, 8));
                    }
                    case 2 -> {
                        out.addAll(pick(EntityPowerScale.HARD_DEMON, 3));
                        out.addAll(pick(EntityPowerScale.MEDIUM_DEMON, 8));
                        out.addAll(pick(EntityPowerScale.EASY_DEMON, 8));
                    }
                    case 3 -> {
                        out.addAll(pick(EntityPowerScale.MEDIUM_DEMON, 15));
                        out.addAll(pick(EntityPowerScale.EASY_DEMON, 20));
                    }
                    case 4 -> {
                        out.addAll(pick(EntityPowerScale.HARD_DEMON, 6));
                        out.addAll(pick(EntityPowerScale.MEDIUM_DEMON, 8));
                    }
                    case 5 -> {
                        out.addAll(pick(EntityPowerScale.MEDIUM_BOSS_DEMON, 1));
                        out.addAll(pick(EntityPowerScale.HARD_DEMON, 4));
                        out.addAll(pick(EntityPowerScale.MEDIUM_DEMON, 8));
                    }
                    case 6 -> {
                        // 1 hard boss, 3 hard, 6 medium
                        out.addAll(pick(EntityPowerScale.HARD_BOSS_DEMON, 1));
                        out.addAll(pick(EntityPowerScale.HARD_DEMON, 3));
                        out.addAll(pick(EntityPowerScale.MEDIUM_DEMON, 6));
                    }
                }
            }
            case 5 -> {
                // omen_of_muzan V: 7 waves (from docs/raids.md)
                switch (wave) {
                    case 1 -> {
                        out.addAll(pick(EntityPowerScale.HARD_DEMON, 2));
                        out.addAll(pick(EntityPowerScale.MEDIUM_DEMON, 8));
                        out.addAll(pick(EntityPowerScale.EASY_DEMON, 10));
                    }
                    case 2 -> {
                        out.addAll(pick(EntityPowerScale.HARD_DEMON, 5));
                        out.addAll(pick(EntityPowerScale.MEDIUM_DEMON, 10));
                        out.addAll(pick(EntityPowerScale.EASY_DEMON, 10));
                    }
                    case 3 -> {
                        out.addAll(pick(EntityPowerScale.MEDIUM_DEMON, 20));
                        out.addAll(pick(EntityPowerScale.EASY_DEMON, 30));
                    }
                    case 4 -> {
                        out.addAll(pick(EntityPowerScale.HARD_DEMON, 10));
                        out.addAll(pick(EntityPowerScale.MEDIUM_DEMON, 10));
                    }
                    case 5 -> {
                        // 2 medium boss (lower ranks), 5 hard, 10 medium
                        out.addAll(pick(EntityPowerScale.MEDIUM_BOSS_DEMON, 2));
                        out.addAll(pick(EntityPowerScale.HARD_DEMON, 5));
                        out.addAll(pick(EntityPowerScale.MEDIUM_DEMON, 10));
                    }
                    case 6 -> {
                        // 2 hard boss (upper ranks), 5 hard, 10 medium
                        out.addAll(pick(EntityPowerScale.HARD_BOSS_DEMON, 2));
                        out.addAll(pick(EntityPowerScale.HARD_DEMON, 5));
                        out.addAll(pick(EntityPowerScale.MEDIUM_DEMON, 10));
                    }
                    case 7 -> {
                        // 1 demon king, 10 hard
                        out.addAll(pick(EntityPowerScale.DEMON_KING, 1));
                        out.addAll(pick(EntityPowerScale.HARD_DEMON, 10));
                    }
                }
            }
        }

        return out;
    }

    // ============================================
    // DEMON SLAYER RAID WAVES (from docs/raids.md)
    // ============================================

    private static List<ResourceLocation> generateSlayerWave(int difficulty, int wave) {
        List<ResourceLocation> out = new ArrayList<>();

        switch (difficulty) {
            case 1 -> {
                // omen_of_ubuyashiki I: 3 waves
                switch (wave) {
                    case 1 -> out.addAll(pick(EntityPowerScale.GENERIC_SLAYER, 3));
                    case 2 -> {
                        out.addAll(pick(EntityPowerScale.HARD_SLAYER, 1));
                        out.addAll(pick(EntityPowerScale.GENERIC_SLAYER, 3));
                    }
                    case 3 -> {
                        // 1 named slayer as boss
                        out.addAll(pick(EntityPowerScale.NAMED_SLAYER, 1));
                    }
                }
            }
            case 2 -> {
                // omen_of_ubuyashiki II: 4 waves
                switch (wave) {
                    case 1 -> out.addAll(pick(EntityPowerScale.GENERIC_SLAYER, 4));
                    case 2 -> {
                        out.addAll(pick(EntityPowerScale.HARD_SLAYER, 1));
                        out.addAll(pick(EntityPowerScale.GENERIC_SLAYER, 4));
                    }
                    case 3 -> {
                        out.addAll(pick(EntityPowerScale.HARD_SLAYER, 2));
                        out.addAll(pick(EntityPowerScale.GENERIC_SLAYER, 5));
                    }
                    case 4 -> {
                        // 1 named slayer as boss with 3 generic
                        out.addAll(pick(EntityPowerScale.NAMED_SLAYER, 1));
                        out.addAll(pick(EntityPowerScale.GENERIC_SLAYER, 3));
                    }
                }
            }
            case 3 -> {
                // omen_of_ubuyashiki III: 5 waves
                switch (wave) {
                    case 1 -> out.addAll(pick(EntityPowerScale.GENERIC_SLAYER, 6));
                    case 2 -> {
                        out.addAll(pick(EntityPowerScale.HARD_SLAYER, 2));
                        out.addAll(pick(EntityPowerScale.GENERIC_SLAYER, 5));
                    }
                    case 3 -> {
                        out.addAll(pick(EntityPowerScale.HARD_SLAYER, 3));
                        out.addAll(pick(EntityPowerScale.GENERIC_SLAYER, 6));
                    }
                    case 4 -> {
                        // 2 named slayers as boss with 3 hard
                        out.addAll(pick(EntityPowerScale.NAMED_SLAYER, 2));
                        out.addAll(pick(EntityPowerScale.HARD_SLAYER, 3));
                    }
                    case 5 -> {
                        // 1 hashira as boss with 5 generic
                        out.addAll(pickHashira(1));
                        out.addAll(pick(EntityPowerScale.GENERIC_SLAYER, 5));
                    }
                }
            }
            case 4 -> {
                // omen_of_ubuyashiki IV: 6 waves
                switch (wave) {
                    case 1 -> {
                        out.addAll(pick(EntityPowerScale.GENERIC_SLAYER, 6));
                        out.addAll(pick(EntityPowerScale.HARD_SLAYER, 3));
                    }
                    case 2 -> {
                        out.addAll(pick(EntityPowerScale.HARD_SLAYER, 5));
                        out.addAll(pick(EntityPowerScale.GENERIC_SLAYER, 10));
                    }
                    case 3 -> {
                        out.addAll(pick(EntityPowerScale.HARD_SLAYER, 6));
                        out.addAll(pick(EntityPowerScale.GENERIC_SLAYER, 10));
                    }
                    case 4 -> {
                        // 3 named with 3 hard and 5 generic
                        out.addAll(pick(EntityPowerScale.NAMED_SLAYER, 3));
                        out.addAll(pick(EntityPowerScale.HARD_SLAYER, 3));
                        out.addAll(pick(EntityPowerScale.GENERIC_SLAYER, 5));
                    }
                    case 5 -> {
                        // 1 hashira with 1 super hard and 5 hard
                        out.addAll(pickHashira(1));
                        out.addAll(pickSuperHard(1));
                        out.addAll(pick(EntityPowerScale.HARD_SLAYER, 5));
                    }
                    case 6 -> {
                        // 1 stronger hashira with 2 named and 3 hard
                        out.addAll(pickHashira(1));
                        out.addAll(pick(EntityPowerScale.NAMED_SLAYER, 2));
                        out.addAll(pick(EntityPowerScale.HARD_SLAYER, 3));
                    }
                }
            }
            case 5 -> {
                // omen_of_ubuyashiki V: 7 waves
                switch (wave) {
                    case 1 -> {
                        out.addAll(pick(EntityPowerScale.GENERIC_SLAYER, 8));
                        out.addAll(pick(EntityPowerScale.HARD_SLAYER, 6));
                    }
                    case 2 -> {
                        out.addAll(pick(EntityPowerScale.HARD_SLAYER, 10));
                        out.addAll(pick(EntityPowerScale.GENERIC_SLAYER, 15));
                    }
                    case 3 -> {
                        out.addAll(pickSuperHard(6));
                        out.addAll(pick(EntityPowerScale.HARD_SLAYER, 10));
                    }
                    case 4 -> {
                        // 3 named with 10 hard and 10 generic
                        out.addAll(pick(EntityPowerScale.NAMED_SLAYER, 3));
                        out.addAll(pick(EntityPowerScale.HARD_SLAYER, 10));
                        out.addAll(pick(EntityPowerScale.GENERIC_SLAYER, 10));
                    }
                    case 5 -> {
                        // 2 hashira with 6 super hard and 10 hard
                        out.addAll(pickHashira(2));
                        out.addAll(pickSuperHard(6));
                        out.addAll(pick(EntityPowerScale.HARD_SLAYER, 10));
                    }
                    case 6 -> {
                        // 2 stronger hashira with 3 named and 8 hard
                        out.addAll(pickHashira(2));
                        out.addAll(pick(EntityPowerScale.NAMED_SLAYER, 3));
                        out.addAll(pick(EntityPowerScale.HARD_SLAYER, 8));
                    }
                    case 7 -> {
                        // 1 super hashira with 5 super hard
                        out.addAll(pickSuperHashira(1));
                        out.addAll(pickSuperHard(5));
                    }
                }
            }
        }

        return out;
    }

    // ============================================
    // ENTITY SELECTION HELPERS
    // ============================================

    private static List<ResourceLocation> pick(EntityPowerScale scale, int count) {
        List<ResourceLocation> pool = EntityCategorization.getEntitiesForScale(scale);
        List<ResourceLocation> out = new ArrayList<>();
        if (pool.isEmpty()) return out;

        for (int i = 0; i < count; i++) {
            out.add(pool.get(ThreadLocalRandom.current().nextInt(pool.size())));
        }
        return out;
    }

    /**
     * Pick hashira, respecting config disable settings.
     */
    private static List<ResourceLocation> pickHashira(int count) {
        List<ResourceLocation> pool = new ArrayList<>(EntityCategorization.getEntitiesForScale(EntityPowerScale.HASHIRA));

        // Remove disabled entities from pool
        if (RaidConfig.disableYoriichiInRaids != null && RaidConfig.disableYoriichiInRaids.get()) {
            pool.removeIf(id -> id.getPath().equals("yoriichi"));
        }
        if (RaidConfig.disableYoriichiOldInRaids != null && RaidConfig.disableYoriichiOldInRaids.get()) {
            pool.removeIf(id -> id.getPath().equals("yoriichi_old"));
        }

        List<ResourceLocation> out = new ArrayList<>();
        if (pool.isEmpty()) return out;

        for (int i = 0; i < count; i++) {
            out.add(pool.get(ThreadLocalRandom.current().nextInt(pool.size())));
        }
        return out;
    }

    /**
     * Pick super hashira (michikatsu, yoriichi, yoriichi_old), respecting config.
     */
    private static List<ResourceLocation> pickSuperHashira(int count) {
        List<ResourceLocation> pool = new ArrayList<>(EntityCategorization.getEntitiesForScale(EntityPowerScale.SUPER_HASHIRA));

        // Remove disabled entities
        if (RaidConfig.disableYoriichiInRaids != null && RaidConfig.disableYoriichiInRaids.get()) {
            pool.removeIf(id -> id.getPath().equals("yoriichi"));
        }
        if (RaidConfig.disableYoriichiOldInRaids != null && RaidConfig.disableYoriichiOldInRaids.get()) {
            pool.removeIf(id -> id.getPath().equals("yoriichi_old"));
        }

        List<ResourceLocation> out = new ArrayList<>();
        if (pool.isEmpty()) {
            // Fallback to regular hashira
            return pickHashira(count);
        }

        for (int i = 0; i < count; i++) {
            out.add(pool.get(ThreadLocalRandom.current().nextInt(pool.size())));
        }
        return out;
    }

    /**
     * Pick super hard slayers (dice_steak_senior_super).
     */
    private static List<ResourceLocation> pickSuperHard(int count) {
        // Get only dice_steak_senior_super from hard slayers
        ResourceLocation superHard = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "dice_steak_senior_super");
        List<ResourceLocation> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            out.add(superHard);
        }
        return out;
    }
}
