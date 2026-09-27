package com.lerdorf.kimetsunoyaibamultiplayer.config;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Registry and safe access layer used by the in-game config editor. */
public final class ConfigEditorRegistry {
    public enum EditorTab {
        CLIENT,
        SERVER,
        KNY_WORLDS
    }

    public record SpecEntry(String modId, String id, String label, ModConfig.Type type, ForgeConfigSpec spec,
                            List<Class<?>> owners) {
    }

    public record ValueEntry(SpecEntry specEntry, ForgeConfigSpec.ConfigValue<?> value,
                             ForgeConfigSpec.ValueSpec valueSpec) {
        public String path() {
            return String.join(".", value.getPath());
        }

        public String label() {
            String path = value.getPath().isEmpty() ? "Value" : value.getPath().get(value.getPath().size() - 1);
            return humanize(path);
        }
    }

    private static final List<SpecEntry> OWN_SPECS = List.of(
        entry("common", "General", ModConfig.Type.COMMON, Config.SPEC, Config.class,
            com.lerdorf.kimetsunoyaibamultiplayer.config.FirstPersonSwordSwingConfig.class),
        entry("sword_slash", "Sword Slash", ModConfig.Type.COMMON,
            com.lerdorf.kimetsunoyaibamultiplayer.config.SwordSwingConfig.SPEC,
            com.lerdorf.kimetsunoyaibamultiplayer.config.SwordSwingConfig.class),
        entry("particles", "Particles", ModConfig.Type.COMMON,
            com.lerdorf.kimetsunoyaibamultiplayer.config.ParticleConfig.SPEC,
            com.lerdorf.kimetsunoyaibamultiplayer.config.ParticleConfig.class),
        entry("entities", "Entities", ModConfig.Type.COMMON,
            com.lerdorf.kimetsunoyaibamultiplayer.config.EntityConfig.SPEC,
            com.lerdorf.kimetsunoyaibamultiplayer.config.EntityConfig.class),
        entry("sword_display", "Sword Display", ModConfig.Type.COMMON,
            com.lerdorf.kimetsunoyaibamultiplayer.config.SwordDisplayConfig.SPEC,
            com.lerdorf.kimetsunoyaibamultiplayer.config.SwordDisplayConfig.class),
        entry("sword_rack", "Sword Rack", ModConfig.Type.COMMON,
            com.lerdorf.kimetsunoyaibamultiplayer.config.SwordRackConfig.SPEC,
            com.lerdorf.kimetsunoyaibamultiplayer.config.SwordRackConfig.class),
        entry("biomes", "Biomes", ModConfig.Type.COMMON,
            com.lerdorf.kimetsunoyaibamultiplayer.config.BiomeConfig.SPEC,
            com.lerdorf.kimetsunoyaibamultiplayer.config.BiomeConfig.class),
        entry("spawn_rates", "Spawn Rates", ModConfig.Type.COMMON,
            com.lerdorf.kimetsunoyaibamultiplayer.config.SpawnRateConfig.SPEC,
            com.lerdorf.kimetsunoyaibamultiplayer.config.SpawnRateConfig.class),
        entry("enhanced_spawning", "Enhanced Spawning", ModConfig.Type.COMMON,
            com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedSpawnConfig.SPEC,
            com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedSpawnConfig.class),
        entry("raids", "Raids", ModConfig.Type.COMMON,
            com.lerdorf.kimetsunoyaibamultiplayer.config.RaidConfig.SPEC,
            com.lerdorf.kimetsunoyaibamultiplayer.config.RaidConfig.class),
        entry("survival_raids", "Survival Raids", ModConfig.Type.COMMON,
            com.lerdorf.kimetsunoyaibamultiplayer.config.SurvivalRaidConfig.SPEC,
            com.lerdorf.kimetsunoyaibamultiplayer.config.SurvivalRaidConfig.class),
        entry("final_selection_raids", "Final Selection", ModConfig.Type.COMMON,
            com.lerdorf.kimetsunoyaibamultiplayer.config.FinalSelectionRaidConfig.SPEC,
            com.lerdorf.kimetsunoyaibamultiplayer.config.FinalSelectionRaidConfig.class),
        entry("enhanced_breathing", "Enhanced Breathing", ModConfig.Type.COMMON,
            com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedBreathingConfig.SPEC,
            com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedBreathingConfig.class),
        entry("customnpcs", "Custom NPCs", ModConfig.Type.COMMON,
            com.lerdorf.kimetsunoyaibamultiplayer.config.CustomNPCConfig.SPEC,
            com.lerdorf.kimetsunoyaibamultiplayer.config.CustomNPCConfig.class),
        entry("variations", "Variations", ModConfig.Type.COMMON,
            com.lerdorf.kimetsunoyaibamultiplayer.config.VariationConfig.SPEC,
            com.lerdorf.kimetsunoyaibamultiplayer.config.VariationConfig.class),
        entry("demon_slayer", "Demon Slayer", ModConfig.Type.COMMON,
            com.lerdorf.kimetsunoyaibamultiplayer.config.DemonSlayerConfig.SPEC,
            com.lerdorf.kimetsunoyaibamultiplayer.config.DemonSlayerConfig.class),
        entry("enhanced_blocks", "Enhanced Blocks", ModConfig.Type.COMMON,
            com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedBlocksConfig.SPEC,
            com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedBlocksConfig.class),
        entry("swordsmith_village", "Swordsmith Village", ModConfig.Type.COMMON,
            com.lerdorf.kimetsunoyaibamultiplayer.config.SwordsmithVillageConfig.SPEC,
            com.lerdorf.kimetsunoyaibamultiplayer.config.SwordsmithVillageConfig.class),
        entry("demon_ranking", "Demon Ranking", ModConfig.Type.COMMON,
            com.lerdorf.kimetsunoyaibamultiplayer.config.DemonRankingConfig.SPEC,
            com.lerdorf.kimetsunoyaibamultiplayer.config.DemonRankingConfig.class),
        entry("gravity", "Gravity", ModConfig.Type.COMMON,
            com.lerdorf.kimetsunoyaibamultiplayer.config.GravityConfig.SPEC,
            com.lerdorf.kimetsunoyaibamultiplayer.config.GravityConfig.class),
        entry("client_particles", "Client Particles", ModConfig.Type.CLIENT,
            com.lerdorf.kimetsunoyaibamultiplayer.config.ClientParticleConfig.SPEC,
            com.lerdorf.kimetsunoyaibamultiplayer.config.ClientParticleConfig.class),
        entry("entity_skin_layers", "Entity Skin Layers", ModConfig.Type.CLIENT,
            com.lerdorf.kimetsunoyaibamultiplayer.config.EntitySkinLayersConfig.SPEC,
            com.lerdorf.kimetsunoyaibamultiplayer.config.EntitySkinLayersConfig.class),
        entry("enhanced_mount_biomes", "Enhanced Mount Biomes", ModConfig.Type.SERVER,
            com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedMountBiomeConfig.SPEC,
            com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedMountBiomeConfig.class),
        entry("custom_progression", "Custom Progression", ModConfig.Type.SERVER,
            com.lerdorf.kimetsunoyaibamultiplayer.config.CustomProgressionConfig.SPEC,
            com.lerdorf.kimetsunoyaibamultiplayer.config.CustomProgressionConfig.class),
        entry("futon", "Futon", ModConfig.Type.SERVER,
            com.lerdorf.kimetsunoyaibamultiplayer.config.FutonConfig.SPEC,
            com.lerdorf.kimetsunoyaibamultiplayer.config.FutonConfig.class)
    );

    private static final List<SpecEntry> SPECS = buildSpecs();

    private static final Map<String, SpecEntry> SPECS_BY_ID = SPECS.stream()
        .collect(java.util.stream.Collectors.toUnmodifiableMap(SpecEntry::id, value -> value));

    private ConfigEditorRegistry() {
    }

    private static SpecEntry entry(String id, String label, ModConfig.Type type, ForgeConfigSpec spec,
                                   Class<?>... owners) {
        return new SpecEntry(KimetsunoyaibaMultiplayer.MODID, id, label, type, spec, List.of(owners));
    }

    public static List<SpecEntry> specs(EditorTab tab) {
        return SPECS.stream()
            .filter(entry -> switch (tab) {
                case SERVER -> KimetsunoyaibaMultiplayer.MODID.equals(entry.modId())
                    && entry.type() == ModConfig.Type.SERVER;
                case KNY_WORLDS -> "kny_worlds".equals(entry.modId());
                case CLIENT -> KimetsunoyaibaMultiplayer.MODID.equals(entry.modId())
                    && entry.type() != ModConfig.Type.SERVER;
            })
            .toList();
    }

    private static List<SpecEntry> buildSpecs() {
        List<SpecEntry> specs = new ArrayList<>(OWN_SPECS);
        addExternalSpec(specs, "kny_worlds_common", "KnY Worlds", ModConfig.Type.COMMON,
            "com.lerdorf.knyworlds.mugentrain.MugenTrainConfig", "COMMON_SPEC");
        addExternalSpec(specs, "kny_worlds_client", "KnY Worlds Client", ModConfig.Type.CLIENT,
            "com.lerdorf.knyworlds.mugentrain.MugenTrainConfig", "CLIENT_SPEC");
        return List.copyOf(specs);
    }

    private static void addExternalSpec(List<SpecEntry> specs, String id, String label, ModConfig.Type type,
                                        String ownerName, String specFieldName) {
        try {
            Class<?> owner = Class.forName(ownerName);
            Field specField = owner.getDeclaredField(specFieldName);
            specField.setAccessible(true);
            Object value = specField.get(null);
            if (value instanceof ForgeConfigSpec spec) {
                specs.add(new SpecEntry("kny_worlds", id, label, type, spec, List.of(owner)));
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            System.err.println("[Kimetsunoyaiba Tweaks] Could not load " + ownerName + "." + specFieldName
                + ": " + exception.getMessage());
        }
    }

    public static List<ValueEntry> values(SpecEntry specEntry) {
        Map<String, ValueEntry> values = new LinkedHashMap<>();
        Set<Object> visited = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        for (Class<?> owner : specEntry.owners()) {
            scan(owner, null, specEntry, values, visited);
        }
        return values.values().stream()
            .sorted(Comparator.comparing(ValueEntry::path))
            .toList();
    }

    private static void scan(Class<?> type, Object instance, SpecEntry specEntry,
                             Map<String, ValueEntry> values, Set<Object> visited) {
        if (instance != null && !visited.add(instance)) {
            return;
        }
        for (Field field : type.getDeclaredFields()) {
            try {
                if (!Modifier.isStatic(field.getModifiers()) && instance == null) {
                    continue;
                }
                field.setAccessible(true);
                Object value = field.get(Modifier.isStatic(field.getModifiers()) ? null : instance);
                if (value instanceof ForgeConfigSpec.ConfigValue<?> configValue) {
                    String path = String.join(".", configValue.getPath());
                    Object rawSpec = specEntry.spec().getSpec().get(configValue.getPath());
                    if (rawSpec instanceof ForgeConfigSpec.ValueSpec valueSpec) {
                        values.putIfAbsent(path, new ValueEntry(specEntry, configValue, valueSpec));
                    }
                } else if (value != null && isConfigObject(value.getClass())) {
                    scanValue(value, specEntry, values, visited);
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // A malformed optional config entry should not prevent the editor from opening.
            }
        }
    }

    private static void scanValue(Object value, SpecEntry specEntry,
                                  Map<String, ValueEntry> values, Set<Object> visited) {
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                Object item = Array.get(value, i);
                if (item != null) {
                    scan(item.getClass(), item, specEntry, values, visited);
                }
            }
        } else {
            scan(value.getClass(), value, specEntry, values, visited);
        }
    }

    private static boolean isConfigObject(Class<?> type) {
        return type.getName().startsWith("com.lerdorf.kimetsunoyaibamultiplayer.config.")
            || type.getName().startsWith("com.lerdorf.knyworlds.");
    }

    public static ValueEntry findValue(String specId, String path) {
        SpecEntry spec = SPECS_BY_ID.get(specId);
        if (spec == null) {
            return null;
        }
        return values(spec).stream().filter(value -> value.path().equals(path)).findFirst().orElse(null);
    }

    public static Object parse(ValueEntry entry, String raw) {
        Object defaultValue = entry.value().getDefault();
        Class<?> valueClass = defaultValue == null ? entry.valueSpec().getClazz() : defaultValue.getClass();
        String trimmed = raw.trim();
        if (valueClass == Boolean.class || valueClass == boolean.class) {
            return Boolean.parseBoolean(trimmed);
        }
        if (valueClass == Integer.class || valueClass == int.class) {
            return Integer.valueOf(trimmed);
        }
        if (valueClass == Long.class || valueClass == long.class) {
            return Long.valueOf(trimmed);
        }
        if (valueClass == Double.class || valueClass == double.class) {
            return Double.valueOf(trimmed);
        }
        if (valueClass == Float.class || valueClass == float.class) {
            return Float.valueOf(trimmed);
        }
        if (valueClass.isEnum()) {
            @SuppressWarnings({"rawtypes", "unchecked"})
            Object result = Enum.valueOf((Class<? extends Enum>) valueClass, trimmed.toUpperCase(Locale.ROOT));
            return result;
        }
        if (defaultValue instanceof List<?> defaults) {
            List<Object> result = new ArrayList<>();
            if (!trimmed.isEmpty()) {
                Class<?> itemClass = defaults.isEmpty() || defaults.get(0) == null
                    ? String.class : defaults.get(0).getClass();
                for (String part : trimmed.split(",")) {
                    result.add(parseScalar(part.trim(), itemClass));
                }
            }
            return result;
        }
        return raw;
    }

    private static Object parseScalar(String raw, Class<?> type) {
        if (type == Integer.class) return Integer.valueOf(raw);
        if (type == Long.class) return Long.valueOf(raw);
        if (type == Double.class) return Double.valueOf(raw);
        if (type == Float.class) return Float.valueOf(raw);
        if (type == Boolean.class) return Boolean.valueOf(raw);
        return raw;
    }

    public static String format(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(", "));
        }
        return String.valueOf(value);
    }

    public static boolean apply(ValueEntry entry, Object value) {
        try {
            if (!entry.valueSpec().test(value)) {
                return false;
            }
            @SuppressWarnings("rawtypes")
            ForgeConfigSpec.ConfigValue rawValue = entry.value();
            rawValue.set(value);
            rawValue.save();
            entry.specEntry().spec().save();
            refresh(entry.specEntry());
            return true;
        } catch (RuntimeException exception) {
            System.err.println("[Kimetsunoyaiba Tweaks] Could not apply config value " + entry.path() + ": "
                + exception.getMessage());
            return false;
        }
    }

    public static boolean applySerialized(String specId, String path, String raw) {
        ValueEntry entry = findValue(specId, path);
        if (entry == null) {
            return false;
        }
        try {
            return apply(entry, parse(entry, raw));
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static void refresh(SpecEntry specEntry) {
        try {
            for (Set<ModConfig> configs : ConfigTracker.INSTANCE.configSets().values()) {
                for (ModConfig config : configs) {
                    if (!specEntry.modId().equals(config.getModId()) || config.getSpec() != specEntry.spec()) {
                        continue;
                    }
                    if (KimetsunoyaibaMultiplayer.MODID.equals(specEntry.modId())) {
                        FMLJavaModLoadingContext.get().getModEventBus()
                            .post(new ModConfigEvent.Reloading(config));
                    } else {
                        for (Class<?> owner : specEntry.owners()) {
                            try {
                                Method apply = owner.getDeclaredMethod("apply", ModConfig.class);
                                apply.setAccessible(true);
                                apply.invoke(null, config);
                            } catch (ReflectiveOperationException exception) {
                                System.err.println("[Kimetsunoyaiba Tweaks] Could not refresh external config: "
                                    + exception.getMessage());
                            }
                            break;
                        }
                    }
                    return;
                }
            }
        } catch (RuntimeException exception) {
            System.err.println("[Kimetsunoyaiba Tweaks] Config applied, but live refresh failed: "
                + exception.getMessage());
        }
    }

    private static String humanize(String value) {
        String result = value.replace('-', ' ').replace('_', ' ');
        return result.isEmpty() ? "Value" : Character.toUpperCase(result.charAt(0)) + result.substring(1);
    }
}
