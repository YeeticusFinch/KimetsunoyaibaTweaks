package com.lerdorf.kimetsunoyaibamultiplayer.customdemonart;

import java.util.List;

public final class BloodDemonArtBuilderOptions {
    private static final List<ParticlePreset> PARTICLE_PRESETS = List.of(
        new ParticlePreset("crimson_dust", "Crimson Dust",
            new CustomBloodDemonArtSavedData.ParticleStyle("minecraft:dust", 0xD12F4B, 1.0F, "")),
        new ParticlePreset("smoke", "Smoke",
            new CustomBloodDemonArtSavedData.ParticleStyle("minecraft:smoke", 0xFFFFFF, 0.8F, "")),
        new ParticlePreset("flame", "Flame",
            new CustomBloodDemonArtSavedData.ParticleStyle("minecraft:flame", 0xFFFFFF, 1.0F, "")),
        new ParticlePreset("soul_flame", "Soul Flame",
            new CustomBloodDemonArtSavedData.ParticleStyle("minecraft:soul_fire_flame", 0xFFFFFF, 1.0F, "")),
        new ParticlePreset("witch", "Witch",
            new CustomBloodDemonArtSavedData.ParticleStyle("minecraft:witch", 0xFFFFFF, 1.0F, "")),
        new ParticlePreset("ash", "Ash",
            new CustomBloodDemonArtSavedData.ParticleStyle("minecraft:ash", 0xFFFFFF, 1.0F, ""))
    );

    private BloodDemonArtBuilderOptions() {
    }

    public static List<ParticlePreset> particlePresets() {
        return PARTICLE_PRESETS;
    }

    public static ParticlePreset presetById(String presetId) {
        for (ParticlePreset preset : PARTICLE_PRESETS) {
            if (preset.id().equals(presetId)) {
                return preset;
            }
        }
        return PARTICLE_PRESETS.get(0);
    }

    public static ParticlePreset nextPresetForParticle(String particleId) {
        int currentIndex = 0;
        for (int i = 0; i < PARTICLE_PRESETS.size(); i++) {
            if (PARTICLE_PRESETS.get(i).style().particleId().equals(particleId)) {
                currentIndex = i;
                break;
            }
        }
        return PARTICLE_PRESETS.get((currentIndex + 1) % PARTICLE_PRESETS.size());
    }

    public static String particleLabel(String particleId) {
        for (ParticlePreset preset : PARTICLE_PRESETS) {
            if (preset.style().particleId().equals(particleId)) {
                return preset.label();
            }
        }
        return particleId == null || particleId.isBlank() ? "None" : particleId;
    }

    public record ParticlePreset(String id, String label, CustomBloodDemonArtSavedData.ParticleStyle style) {
    }
}
