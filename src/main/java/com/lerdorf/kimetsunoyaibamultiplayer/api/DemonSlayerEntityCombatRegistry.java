package com.lerdorf.kimetsunoyaibamultiplayer.api;

import com.lerdorf.kimetsunoyaibamultiplayer.raids.EntityCategorization;
import com.lerdorf.kimetsunoyaibamultiplayer.raids.EntityPowerScale;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Metadata registry for addon entities that should participate in KnY demon slayer combat systems.
 *
 * This does not replace Forge entity registration. Addons still register their own EntityType.
 * The profile here lets generated code declare the entity's default breathing style, sword, and
 * demonized state in one place, then apply those defaults when the entity is spawned.
 */
public final class DemonSlayerEntityCombatRegistry {
    private static final Map<ResourceLocation, CombatProfile> PROFILES = new ConcurrentHashMap<>();

    private DemonSlayerEntityCombatRegistry() {
    }

    public static CombatProfile register(
            ResourceLocation entityId,
            EntityPowerScale powerScale,
            String breathingStyleId,
            String defaultSwordId,
            boolean canUseBreathingForms,
            boolean demonized) {
        CombatProfile profile = new CombatProfile(
            entityId,
            powerScale,
            emptyToNull(breathingStyleId),
            emptyToNull(defaultSwordId),
            canUseBreathingForms,
            demonized
        );

        PROFILES.put(entityId, profile);
        EntityCategorization.registerCustomEntity(entityId, powerScale);
        return profile;
    }

    public static CombatProfile get(ResourceLocation entityId) {
        return PROFILES.get(entityId);
    }

    public static boolean isRegistered(ResourceLocation entityId) {
        return PROFILES.containsKey(entityId);
    }

    public static Collection<CombatProfile> getAll() {
        return Collections.unmodifiableCollection(PROFILES.values());
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public static final class CombatProfile {
        private final ResourceLocation entityId;
        private final EntityPowerScale powerScale;
        private final String breathingStyleId;
        private final String defaultSwordId;
        private final boolean canUseBreathingForms;
        private final boolean demonized;

        private CombatProfile(
                ResourceLocation entityId,
                EntityPowerScale powerScale,
                String breathingStyleId,
                String defaultSwordId,
                boolean canUseBreathingForms,
                boolean demonized) {
            this.entityId = entityId;
            this.powerScale = powerScale;
            this.breathingStyleId = breathingStyleId;
            this.defaultSwordId = defaultSwordId;
            this.canUseBreathingForms = canUseBreathingForms;
            this.demonized = demonized;
        }

        public ResourceLocation getEntityId() {
            return entityId;
        }

        public EntityPowerScale getPowerScale() {
            return powerScale;
        }

        public String getBreathingStyleId() {
            return breathingStyleId;
        }

        public String getDefaultSwordId() {
            return defaultSwordId;
        }

        public boolean canUseBreathingForms() {
            return canUseBreathingForms;
        }

        public boolean isDemonized() {
            return demonized;
        }
    }
}
