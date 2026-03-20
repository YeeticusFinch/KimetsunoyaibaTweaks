package com.lerdorf.kimetsunoyaibamultiplayer.raids;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Wave generator for survival raids.
 *
 * The cycle length is (difficulty + 2), and compositions scale by player count.
 */
public class SurvivalWaveGenerator {

    public enum WaveType {
        EASY,
        MIXED,
        HARD,
        BOSS
    }

    public record WaveBundle(List<ResourceLocation> entities, int bossCount, List<ResourceLocation> reinforcementEntities) {}

    public static WaveBundle generateWave(int difficulty, int playerCount, int cyclePhase) {
        int d = clampDifficulty(difficulty);
        int players = Math.max(1, playerCount);
        int phase = normalizePhase(cyclePhase, getCycleLength(d));

        WaveType type = getWaveType(d, phase);
        List<ResourceLocation> wave = new ArrayList<>();

        switch (d) {
            case 1 -> fillDifficulty1(type, wave, players);
            case 2 -> fillDifficulty2(phase, wave, players);
            case 3 -> fillDifficulty3(phase, wave, players);
            case 4 -> fillDifficulty4(phase, wave, players);
            default -> fillDifficulty5(phase, wave, players);
        }

        int bossCount = getBossCount(d, phase, type);
        List<ResourceLocation> reinforcements = bossCount > 0 ? stripLeadingBosses(wave, bossCount) : List.of();
        return new WaveBundle(wave, bossCount, reinforcements);
    }

    public static List<ResourceLocation> generateWave(WaveType type, int difficulty, int playerCount) {
        List<ResourceLocation> wave = new ArrayList<>();
        int d = clampDifficulty(difficulty);
        int players = Math.max(1, playerCount);

        if (type == WaveType.EASY) {
            wave.addAll(pick(EntityPowerScale.EASY_DEMON, 6 * players));
            return wave;
        }

        if (type == WaveType.MIXED) {
            wave.addAll(pick(EntityPowerScale.EASY_DEMON, 3 * players));
            wave.addAll(pick(EntityPowerScale.MEDIUM_DEMON, 3 * players));
            return wave;
        }

        if (type == WaveType.HARD) {
            wave.addAll(pick(EntityPowerScale.HARD_DEMON, 4 * players));
            wave.addAll(pick(EntityPowerScale.MEDIUM_DEMON, 6 * players));
            return wave;
        }

        wave.addAll(pick(bossScaleForDifficulty(d), 1));
        wave.addAll(pick(EntityPowerScale.MEDIUM_DEMON, 2 * players));
        wave.addAll(pick(EntityPowerScale.EASY_DEMON, 2 * players));
        return wave;
    }

    public static int getCycleLength(int difficulty) {
        return clampDifficulty(difficulty) + 2;
    }

    public static WaveType getWaveType(int difficulty, int cyclePhase) {
        int d = clampDifficulty(difficulty);
        int phase = normalizePhase(cyclePhase, getCycleLength(d));

        return switch (d) {
            case 1 -> switch (phase) {
                case 1 -> WaveType.EASY;
                case 2 -> WaveType.MIXED;
                default -> WaveType.BOSS;
            };
            case 2 -> switch (phase) {
                case 1 -> WaveType.EASY;
                case 2, 3 -> WaveType.MIXED;
                default -> WaveType.BOSS;
            };
            case 3 -> switch (phase) {
                case 1 -> WaveType.EASY;
                case 2, 3 -> WaveType.MIXED;
                case 4 -> WaveType.HARD;
                default -> WaveType.BOSS;
            };
            case 4 -> switch (phase) {
                case 1, 2, 3 -> WaveType.MIXED;
                case 4 -> WaveType.HARD;
                default -> WaveType.BOSS;
            };
            default -> switch (phase) {
                case 1, 2, 3 -> WaveType.MIXED;
                case 4 -> WaveType.HARD;
                default -> WaveType.BOSS;
            };
        };
    }

    private static void fillDifficulty1(WaveType type, List<ResourceLocation> out, int players) {
        switch (type) {
            case EASY -> out.addAll(pick(EntityPowerScale.EASY_DEMON, 6 * players));
            case MIXED -> {
                out.addAll(pick(EntityPowerScale.EASY_DEMON, 3 * players));
                out.addAll(pick(EntityPowerScale.MEDIUM_DEMON, 3 * players));
            }
            case BOSS -> {
                out.addAll(pick(EntityPowerScale.HARD_DEMON, 1));
                out.addAll(pick(EntityPowerScale.MEDIUM_DEMON, 2 * players));
                out.addAll(pick(EntityPowerScale.EASY_DEMON, 2 * players));
            }
            case HARD -> {
                out.addAll(pick(EntityPowerScale.HARD_DEMON, 2 * players));
                out.addAll(pick(EntityPowerScale.MEDIUM_DEMON, 4 * players));
            }
        }
    }

    private static void fillDifficulty2(int phase, List<ResourceLocation> out, int players) {
        switch (phase) {
            case 1 -> out.addAll(pick(EntityPowerScale.EASY_DEMON, 6 * players));
            case 2 -> {
                out.addAll(pick(EntityPowerScale.EASY_DEMON, 5 * players));
                out.addAll(pick(EntityPowerScale.MEDIUM_DEMON, 5 * players));
            }
            case 3 -> {
                out.addAll(pick(EntityPowerScale.EASY_DEMON, 6 * players));
                out.addAll(pick(EntityPowerScale.MEDIUM_DEMON, 6 * players));
            }
            default -> {
                out.addAll(pick(EntityPowerScale.MEDIUM_BOSS_DEMON, 2));
                out.addAll(pick(EntityPowerScale.MEDIUM_DEMON, 4 * players));
                out.addAll(pick(EntityPowerScale.EASY_DEMON, 4 * players));
            }
        }
    }

    private static void fillDifficulty3(int phase, List<ResourceLocation> out, int players) {
        switch (phase) {
            case 1 -> out.addAll(pick(EntityPowerScale.EASY_DEMON, 8 * players));
            case 2 -> {
                out.addAll(pick(EntityPowerScale.EASY_DEMON, 6 * players));
                out.addAll(pick(EntityPowerScale.MEDIUM_DEMON, 6 * players));
            }
            case 3 -> {
                out.addAll(pick(EntityPowerScale.MEDIUM_DEMON, 10 * players));
                out.addAll(pick(EntityPowerScale.EASY_DEMON, 10 * players));
            }
            case 4 -> {
                out.addAll(pick(EntityPowerScale.HARD_DEMON, 4 * players));
                out.addAll(pick(EntityPowerScale.MEDIUM_DEMON, 8 * players));
            }
            default -> {
                out.addAll(pick(EntityPowerScale.MEDIUM_BOSS_DEMON, 1));
                out.addAll(pick(EntityPowerScale.HARD_DEMON, 3 * players));
                out.addAll(pick(EntityPowerScale.MEDIUM_DEMON, 6 * players));
            }
        }
    }

    private static void fillDifficulty4(int phase, List<ResourceLocation> out, int players) {
        switch (phase) {
            case 1 -> {
                out.addAll(pick(EntityPowerScale.HARD_DEMON, 1 * players));
                out.addAll(pick(EntityPowerScale.MEDIUM_DEMON, 6 * players));
                out.addAll(pick(EntityPowerScale.EASY_DEMON, 8 * players));
            }
            case 2 -> {
                out.addAll(pick(EntityPowerScale.HARD_DEMON, 3 * players));
                out.addAll(pick(EntityPowerScale.MEDIUM_DEMON, 8 * players));
                out.addAll(pick(EntityPowerScale.EASY_DEMON, 8 * players));
            }
            case 3 -> {
                out.addAll(pick(EntityPowerScale.MEDIUM_DEMON, 15 * players));
                out.addAll(pick(EntityPowerScale.EASY_DEMON, 20 * players));
            }
            case 4 -> {
                out.addAll(pick(EntityPowerScale.HARD_DEMON, 6 * players));
                out.addAll(pick(EntityPowerScale.MEDIUM_DEMON, 8 * players));
            }
            case 5 -> {
                out.addAll(pick(EntityPowerScale.HARD_BOSS_DEMON, 1));
                out.addAll(pick(EntityPowerScale.HARD_DEMON, 4 * players));
                out.addAll(pick(EntityPowerScale.MEDIUM_DEMON, 8 * players));
            }
            default -> {
                out.addAll(pick(EntityPowerScale.HARD_BOSS_DEMON, 1));
                out.addAll(pick(EntityPowerScale.HARD_DEMON, 3 * players));
                out.addAll(pick(EntityPowerScale.MEDIUM_DEMON, 6 * players));
            }
        }
    }

    private static void fillDifficulty5(int phase, List<ResourceLocation> out, int players) {
        switch (phase) {
            case 1 -> {
                out.addAll(pick(EntityPowerScale.HARD_DEMON, 2 * players));
                out.addAll(pick(EntityPowerScale.MEDIUM_DEMON, 8 * players));
                out.addAll(pick(EntityPowerScale.EASY_DEMON, 10 * players));
            }
            case 2 -> {
                out.addAll(pick(EntityPowerScale.HARD_DEMON, 5 * players));
                out.addAll(pick(EntityPowerScale.MEDIUM_DEMON, 10 * players));
                out.addAll(pick(EntityPowerScale.EASY_DEMON, 10 * players));
            }
            case 3 -> {
                out.addAll(pick(EntityPowerScale.MEDIUM_DEMON, 20 * players));
                out.addAll(pick(EntityPowerScale.EASY_DEMON, 30 * players));
            }
            case 4 -> {
                out.addAll(pick(EntityPowerScale.HARD_DEMON, 10 * players));
                out.addAll(pick(EntityPowerScale.MEDIUM_DEMON, 10 * players));
            }
            case 5 -> {
                out.addAll(pick(EntityPowerScale.MEDIUM_BOSS_DEMON, 2));
                out.addAll(pick(EntityPowerScale.HARD_DEMON, 5 * players));
                out.addAll(pick(EntityPowerScale.MEDIUM_DEMON, 10 * players));
            }
            case 6 -> {
                out.addAll(pick(EntityPowerScale.HARD_BOSS_DEMON, 2));
                out.addAll(pick(EntityPowerScale.HARD_DEMON, 5 * players));
                out.addAll(pick(EntityPowerScale.MEDIUM_DEMON, 10 * players));
            }
            default -> {
                out.addAll(pick(EntityPowerScale.DEMON_KING, 1));
                out.addAll(pick(EntityPowerScale.HARD_DEMON, 10 * players));
            }
        }
    }

    private static int getBossCount(int difficulty, int phase, WaveType type) {
        if (type != WaveType.BOSS) return 0;
        int d = clampDifficulty(difficulty);

        if (d == 1) return 1;
        if (d == 2) return 2;
        if (d == 5 && phase == 5) return 2;
        if (d == 5 && phase == 6) return 2;
        return 1;
    }

    private static List<ResourceLocation> stripLeadingBosses(List<ResourceLocation> wave, int bossCount) {
        if (bossCount <= 0 || wave.isEmpty()) return List.of();
        int start = Math.min(bossCount, wave.size());
        return new ArrayList<>(wave.subList(start, wave.size()));
    }

    private static EntityPowerScale bossScaleForDifficulty(int difficulty) {
        int d = clampDifficulty(difficulty);
        if (d == 1) return EntityPowerScale.HARD_DEMON;
        if (d <= 3) return EntityPowerScale.MEDIUM_BOSS_DEMON;
        return EntityPowerScale.HARD_BOSS_DEMON;
    }

    private static int normalizePhase(int phase, int cycleLength) {
        int p = phase % cycleLength;
        if (p <= 0) p += cycleLength;
        return p;
    }

    private static int clampDifficulty(int difficulty) {
        return Math.max(1, Math.min(5, difficulty));
    }

    private static List<ResourceLocation> pick(EntityPowerScale scale, int count) {
        List<ResourceLocation> pool = EntityCategorization.getEntitiesForScale(scale);
        List<ResourceLocation> out = new ArrayList<>();

        if (pool.isEmpty() || count <= 0) {
            return out;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < count; i++) {
            out.add(pool.get(random.nextInt(pool.size())));
        }
        return out;
    }
}
