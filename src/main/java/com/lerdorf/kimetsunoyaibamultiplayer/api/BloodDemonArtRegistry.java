package com.lerdorf.kimetsunoyaibamultiplayer.api;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for blood demon arts.
 */
public final class BloodDemonArtRegistry {
    private static final Map<String, RegisteredBloodDemonArt> BLOOD_DEMON_ARTS = new ConcurrentHashMap<>();

    private BloodDemonArtRegistry() {
    }

    public static RegisteredBloodDemonArt register(
            String artId,
            String artName,
            BloodDemonArtTechnique technique) {
        if (BLOOD_DEMON_ARTS.containsKey(artId)) {
            throw new IllegalArgumentException("Blood demon art already registered: " + artId);
        }

        RegisteredBloodDemonArt art = new RegisteredBloodDemonArt(artId, artName, technique);
        BLOOD_DEMON_ARTS.put(artId, art);
        return art;
    }

    public static RegisteredBloodDemonArt getArt(String artId) {
        return BLOOD_DEMON_ARTS.get(artId);
    }

    public static boolean isRegistered(String artId) {
        return BLOOD_DEMON_ARTS.containsKey(artId);
    }

    public static Collection<RegisteredBloodDemonArt> getAllArts() {
        return Collections.unmodifiableCollection(BLOOD_DEMON_ARTS.values());
    }

    public static void clear() {
        BLOOD_DEMON_ARTS.clear();
    }

    public static class RegisteredBloodDemonArt {
        private final String artId;
        private final String artName;
        private final BloodDemonArtTechnique technique;

        private RegisteredBloodDemonArt(String artId, String artName, BloodDemonArtTechnique technique) {
            this.artId = artId;
            this.artName = artName;
            this.technique = technique;
        }

        public String getArtId() {
            return artId;
        }

        public String getArtName() {
            return artName;
        }

        public BloodDemonArtTechnique getTechnique() {
            return technique;
        }
    }
}
