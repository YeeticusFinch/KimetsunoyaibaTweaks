package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.config.ConfigEditorRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.ConfigEditPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.config.ModConfig;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ConfigPresetManager {
    private static final String MOD_ID = KimetsunoyaibaMultiplayer.MODID;
    private static final ResourceLocation DEFAULT_ICON = texture("config_preset_icons/default_logo.png");
    private static final ResourceLocation PARTICLE_ICON = texture("config_preset_icons/particle_logo.png");
    private static final ResourceLocation ORCA_ICON = texture("config_preset_icons/orca_logo.png");
    private static final ResourceLocation ORCA_SWORD_ICON = texture("config_preset_icons/orca_sword_logo.png");

    private ConfigPresetManager() {
    }

    static List<Preset> presets() {
        return List.of(
            new Preset("Default", "Reset every Tweaks and KnY Worlds setting to the defaults shipped by the mods.",
                DEFAULT_ICON, 256, 256, Map.of()),
            new Preset("Old Progression",
                "Restores a legacy progression-focused setup: the base Orca progression and quest flow stays in charge, while custom progression, custom demon initiation, and custom demon progression overrides are disabled.",
                ORCA_ICON, 256, 256, mapOf(
                    "custom_progression", "demon_slayer_initiation.disable_base_mod_demon_slayer_initiation", "false",
                    "custom_progression", "demon_slayer_initiation.grant_training_sword", "false",
                    "custom_progression", "demon_progression.custom_demon_initiation", "false",
                    "custom_progression", "demon_progression.suppress_non_kizuki_muzan_blood_drops", "false",
                    "custom_progression", "demon_progression.persistent_demonhood", "false",
                    "custom_progression", "sword_transformation.replace_color_changing_procedure", "false"
                )),
            new Preset("No New Stuff",
                "A minimal compatibility setup with the new Tweaks additions turned off: custom progression, enhanced breathing, enhanced spawning, replacement entities, custom NPC abilities, extra blocks, new demon systems, and optional KnY Worlds dimensions are disabled.",
                ORCA_SWORD_ICON, 256, 256, mapOf(
                    "custom_progression", "demon_slayer_initiation.disable_base_mod_demon_slayer_initiation", "false",
                    "custom_progression", "demon_progression.custom_demon_initiation", "false",
                    "custom_progression", "demon_progression.persistent_demonhood", "false",
                    "enhanced_spawning", "enhanced_spawning_rules", "false",
                    "enhanced_breathing", "enhancedMistBreathing", "false",
                    "enhanced_breathing", "enhancedFlowerBreathing", "false",
                    "enhanced_breathing", "enhancedBeastBreathing", "false",
                    "enhanced_breathing", "enhancedLoveBreathing", "false",
                    "customnpcs", "enableCustomNPCCompat", "false",
                    "enhanced_blocks", "enhancedChestOfDrawers", "false",
                    "enhanced_blocks", "enhancedVialRack", "false",
                    "enhanced_mount_biomes", "enhanced-mount-natagumo-enabled", "false",
                    "enhanced_mount_biomes", "enhanced-mount-yoko-enabled", "false",
                    "kny_worlds_common", "dimensions.infinityCastleEnabled", "false",
                    "kny_worlds_common", "dimensions.mugenTrainEnabled", "false",
                    "kny_worlds_common", "dimensions.swordsmithVillageEnabled", "false",
                    "kny_worlds_common", "dimensions.mtFujikasaneEnabled", "false"
                )),
            new Preset("Potato Mode",
                "The lowest-cost profile for older computers: particle rendering, expensive spawning logic, custom NPC abilities, heavy breathing visuals, protective responses, smoke, and high-detail KnY Worlds scenery are reduced while core gameplay remains available.",
                PARTICLE_ICON, 256, 256, mapOf(
                    "client_particles", "ability-particle-render-percent", "10",
                    "particles", "sword-particles-enabled", "false",
                    "particles", "appearance.radial-layers", "3",
                    "particles", "appearance.max-particles-per-tick", "20",
                    "enhanced_spawning", "enhanced_spawning_rules", "false",
                    "enhanced_spawning", "protective_spawning.enable_protective_spawning", "false",
                    "customnpcs", "enableCustomNPCCompat", "false",
                    "enhanced_breathing", "emissive", "false",
                    "enhanced_breathing", "particleDensity", "0",
                    "enhanced_mount_biomes", "enhanced-mount-natagumo-enabled", "false",
                    "enhanced_mount_biomes", "enhanced-mount-yoko-enabled", "false",
                    "kny_worlds_client", "rendering.maxCachedSections", "256",
                    "kny_worlds_client", "rendering.sectionBuildsPerFrame", "1",
                    "kny_worlds_client", "rendering.gpuUploadsPerFrame", "1",
                    "kny_worlds_client", "rendering.viewRadiusCap", "128",
                    "kny_worlds_client", "smoke.chimneySmokeEnabled", "false"
                ))
        );
    }

    static ApplyResult apply(Preset preset) {
        int applied = 0;
        int skipped = 0;
        for (ConfigEditorRegistry.EditorTab tab : ConfigEditorRegistry.EditorTab.values()) {
            for (ConfigEditorRegistry.SpecEntry spec : ConfigEditorRegistry.specs(tab)) {
                for (ConfigEditorRegistry.ValueEntry value : ConfigEditorRegistry.values(spec)) {
                    if (applyValue(value, ConfigEditorRegistry.format(value.value().getDefault()))) {
                        applied++;
                    } else {
                        skipped++;
                    }
                }
            }
        }
        for (Override override : preset.overrides().values()) {
            ConfigEditorRegistry.ValueEntry value = findValue(override.specId(), override.path());
            if (value == null || !applyValue(value, override.rawValue())) {
                skipped++;
            } else {
                applied++;
            }
        }
        return new ApplyResult(applied, skipped);
    }

    private static boolean applyValue(ConfigEditorRegistry.ValueEntry value, String rawValue) {
        if (value.specEntry().type() == ModConfig.Type.SERVER && Minecraft.getInstance().player != null) {
            if (!Minecraft.getInstance().player.hasPermissions(2)) {
                return false;
            }
            ModNetworking.sendToServer(new ConfigEditPacket(value.specEntry().id(), value.path(), rawValue));
            return true;
        }
        try {
            return ConfigEditorRegistry.apply(value, ConfigEditorRegistry.parse(value, rawValue));
        } catch (RuntimeException exception) {
            System.err.println("[Kimetsunoyaiba Tweaks] Preset value failed for " + value.path() + ": " + exception.getMessage());
            return false;
        }
    }

    private static ConfigEditorRegistry.ValueEntry findValue(String specId, String path) {
        ConfigEditorRegistry.ValueEntry exact = ConfigEditorRegistry.findValue(specId, path);
        if (exact != null) {
            return exact;
        }
        for (ConfigEditorRegistry.EditorTab tab : ConfigEditorRegistry.EditorTab.values()) {
            for (ConfigEditorRegistry.SpecEntry spec : ConfigEditorRegistry.specs(tab)) {
                if (!spec.id().equals(specId)) {
                    continue;
                }
                for (ConfigEditorRegistry.ValueEntry value : ConfigEditorRegistry.values(spec)) {
                    if (value.path().equals(path) || value.path().endsWith("." + path)) {
                        return value;
                    }
                }
            }
        }
        return null;
    }

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/" + path);
    }

    private static Map<String, Override> mapOf(String... values) {
        Map<String, Override> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 3) {
            Override override = new Override(values[i], values[i + 1], values[i + 2]);
            result.put(values[i] + ":" + values[i + 1], override);
        }
        return result;
    }

    record Preset(String name, String description, ResourceLocation image, int imageWidth, int imageHeight,
                  Map<String, Override> overrides) {
    }

    record Override(String specId, String path, String rawValue) {
    }

    record ApplyResult(int applied, int skipped) {
    }
}
