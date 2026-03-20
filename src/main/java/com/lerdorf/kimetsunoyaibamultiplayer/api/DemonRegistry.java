package com.lerdorf.kimetsunoyaibamultiplayer.api;

import com.lerdorf.kimetsunoyaibamultiplayer.raids.EntityCategorization;
import com.lerdorf.kimetsunoyaibamultiplayer.raids.EntityPowerScale;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for demon entities added by this mod or addons.
 */
public final class DemonRegistry {
    private static final Map<ResourceLocation, RegisteredDemon> DEMONS = new ConcurrentHashMap<>();

    private DemonRegistry() {
    }

    public static RegisteredDemon register(ResourceLocation entityId, EntityPowerScale powerScale) {
        return register(entityId, powerScale, false, null);
    }

    public static RegisteredDemon register(ResourceLocation entityId, EntityPowerScale powerScale, boolean sunlightImmune) {
        return register(entityId, powerScale, sunlightImmune, null);
    }

    public static RegisteredDemon register(
            ResourceLocation entityId,
            EntityPowerScale powerScale,
            boolean sunlightImmune,
            String bloodDemonArtId) {
        RegisteredDemon demon = new RegisteredDemon(entityId, powerScale, sunlightImmune, bloodDemonArtId);
        DEMONS.put(entityId, demon);
        EntityCategorization.registerCustomEntity(entityId, powerScale);
        return demon;
    }

    public static RegisteredDemon get(ResourceLocation entityId) {
        return DEMONS.get(entityId);
    }

    public static boolean isRegistered(ResourceLocation entityId) {
        return DEMONS.containsKey(entityId);
    }

    public static boolean isSunlightImmune(ResourceLocation entityId) {
        RegisteredDemon demon = DEMONS.get(entityId);
        return demon != null && demon.isSunlightImmune();
    }

    public static Collection<RegisteredDemon> getAll() {
        return Collections.unmodifiableCollection(DEMONS.values());
    }

    public static class RegisteredDemon {
        private final ResourceLocation entityId;
        private final EntityPowerScale powerScale;
        private final boolean sunlightImmune;
        private final String bloodDemonArtId;

        private RegisteredDemon(ResourceLocation entityId, EntityPowerScale powerScale, boolean sunlightImmune, String bloodDemonArtId) {
            this.entityId = entityId;
            this.powerScale = powerScale;
            this.sunlightImmune = sunlightImmune;
            this.bloodDemonArtId = bloodDemonArtId;
        }

        public ResourceLocation getEntityId() {
            return entityId;
        }

        public EntityPowerScale getPowerScale() {
            return powerScale;
        }

        public boolean isSunlightImmune() {
            return sunlightImmune;
        }

        public String getBloodDemonArtId() {
            return bloodDemonArtId;
        }
    }
}
