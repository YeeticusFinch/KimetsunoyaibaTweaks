package com.lerdorf.kimetsunoyaibamultiplayer.raids;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.*;

/**
 * Tracks entities spawned in a raid wave.
 *
 * Each wave spawns a specific set of entities, and this class tracks:
 * - Which entities should spawn
 * - Which entities have already spawned
 * - Which spawned entities are still alive
 * - Wave completion status
 */
public class WaveData {

    // Wave number (1-indexed)
    private final int waveNumber;

    // Entities to spawn in this wave (entity ID -> count)
    private final Map<ResourceLocation, Integer> entitiesToSpawn;

    // Total entities that should spawn
    private final int totalEntitiesToSpawn;

    // UUIDs of spawned entities that are still being tracked
    private final Set<UUID> spawnedEntityUUIDs;

    // Number of entities killed so far
    private int entitiesKilled;

    // Wave state
    private WaveState state;

    /**
     * Wave states.
     */
    public enum WaveState {
        /** Wave is waiting to start (preparation phase) */
        PREPARING,

        /** Wave is currently spawning entities */
        SPAWNING,

        /** Wave has finished spawning, waiting for entities to be killed */
        IN_PROGRESS,

        /** Wave is complete (all entities killed) */
        COMPLETED,

        /** Wave failed (timeout or other failure) */
        FAILED
    }

    /**
     * Create a new wave data.
     *
     * @param waveNumber       The wave number (1-indexed)
     * @param entitiesToSpawn  Map of entity IDs to spawn counts
     */
    public WaveData(int waveNumber, Map<ResourceLocation, Integer> entitiesToSpawn) {
        this.waveNumber = waveNumber;
        this.entitiesToSpawn = new HashMap<>(entitiesToSpawn);
        this.spawnedEntityUUIDs = new HashSet<>();
        this.entitiesKilled = 0;
        this.state = WaveState.PREPARING;

        // Calculate total
        this.totalEntitiesToSpawn = entitiesToSpawn.values().stream()
            .mapToInt(Integer::intValue)
            .sum();
    }

    /**
     * Get the wave number.
     *
     * @return Wave number (1-indexed)
     */
    public int getWaveNumber() {
        return waveNumber;
    }

    /**
     * Get the entities to spawn in this wave.
     *
     * @return Unmodifiable map of entity IDs to counts
     */
    public Map<ResourceLocation, Integer> getEntitiesToSpawn() {
        return Collections.unmodifiableMap(entitiesToSpawn);
    }

    /**
     * Get the total number of entities to spawn.
     *
     * @return Total entity count
     */
    public int getTotalEntitiesToSpawn() {
        return totalEntitiesToSpawn;
    }

    /**
     * Get the number of entities currently alive.
     *
     * @return Alive entity count
     */
    public int getEntitiesAlive() {
        return spawnedEntityUUIDs.size();
    }

    /**
     * Get the number of entities killed.
     *
     * @return Killed entity count
     */
    public int getEntitiesKilled() {
        return entitiesKilled;
    }

    /**
     * Get the wave state.
     *
     * @return Current wave state
     */
    public WaveState getState() {
        return state;
    }

    /**
     * Set the wave state.
     *
     * @param state New wave state
     */
    public void setState(WaveState state) {
        this.state = state;
    }

    /**
     * Register a spawned entity.
     *
     * @param entity The spawned entity
     */
    public void registerSpawnedEntity(Entity entity) {
        spawnedEntityUUIDs.add(entity.getUUID());
    }

    /**
     * Mark an entity as killed.
     *
     * @param entityUUID The entity UUID
     * @return true if this was the last entity in the wave
     */
    public boolean markEntityKilled(UUID entityUUID) {
        if (spawnedEntityUUIDs.remove(entityUUID)) {
            entitiesKilled++;

            // Check if wave is complete
            if (spawnedEntityUUIDs.isEmpty() && state == WaveState.IN_PROGRESS) {
                state = WaveState.COMPLETED;
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the wave is complete.
     *
     * @return true if wave is completed or failed
     */
    public boolean isComplete() {
        return state == WaveState.COMPLETED || state == WaveState.FAILED;
    }

    /**
     * Check if the wave is active (spawning or in progress).
     *
     * @return true if wave is active
     */
    public boolean isActive() {
        return state == WaveState.SPAWNING || state == WaveState.IN_PROGRESS;
    }

    /**
     * Get wave completion percentage (0.0 to 1.0).
     *
     * @return Completion percentage
     */
    public float getCompletionPercentage() {
        if (totalEntitiesToSpawn == 0) {
            return 1.0f;
        }
        return (float) entitiesKilled / (float) totalEntitiesToSpawn;
    }

    /**
     * Get wave progress percentage (entities killed / total, 0.0 to 1.0).
     * Used for boss bar display.
     *
     * @return Progress percentage (1.0 = all entities alive, 0.0 = all dead)
     */
    public float getProgressPercentage() {
        if (totalEntitiesToSpawn == 0) {
            return 0.0f;
        }
        return (float) spawnedEntityUUIDs.size() / (float) totalEntitiesToSpawn;
    }

    /**
     * Serialize to NBT.
     *
     * @return NBT compound tag
     */
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();

        tag.putInt("waveNumber", waveNumber);
        tag.putInt("totalEntitiesToSpawn", totalEntitiesToSpawn);
        tag.putInt("entitiesKilled", entitiesKilled);
        tag.putString("state", state.name());

        // Serialize entities to spawn
        CompoundTag entitiesToSpawnTag = new CompoundTag();
        for (Map.Entry<ResourceLocation, Integer> entry : entitiesToSpawn.entrySet()) {
            entitiesToSpawnTag.putInt(entry.getKey().toString(), entry.getValue());
        }
        tag.put("entitiesToSpawn", entitiesToSpawnTag);

        // Serialize spawned entity UUIDs
        ListTag uuidList = new ListTag();
        for (UUID uuid : spawnedEntityUUIDs) {
            CompoundTag uuidTag = new CompoundTag();
            uuidTag.putUUID("uuid", uuid);
            uuidList.add(uuidTag);
        }
        tag.put("spawnedEntityUUIDs", uuidList);

        return tag;
    }

    /**
     * Deserialize from NBT.
     *
     * @param tag NBT compound tag
     * @return WaveData instance
     */
    public static WaveData fromNBT(CompoundTag tag) {
        int waveNumber = tag.getInt("waveNumber");

        // Deserialize entities to spawn
        Map<ResourceLocation, Integer> entitiesToSpawn = new HashMap<>();
        CompoundTag entitiesToSpawnTag = tag.getCompound("entitiesToSpawn");
        for (String key : entitiesToSpawnTag.getAllKeys()) {
            ResourceLocation entityId = ResourceLocation.parse(key);
            int count = entitiesToSpawnTag.getInt(key);
            entitiesToSpawn.put(entityId, count);
        }

        WaveData waveData = new WaveData(waveNumber, entitiesToSpawn);

        // Restore state
        waveData.entitiesKilled = tag.getInt("entitiesKilled");
        if (tag.contains("state")) {
            waveData.state = WaveState.valueOf(tag.getString("state"));
        }

        // Restore spawned entity UUIDs
        ListTag uuidList = tag.getList("spawnedEntityUUIDs", Tag.TAG_COMPOUND);
        for (int i = 0; i < uuidList.size(); i++) {
            CompoundTag uuidTag = uuidList.getCompound(i);
            UUID uuid = uuidTag.getUUID("uuid");
            waveData.spawnedEntityUUIDs.add(uuid);
        }

        return waveData;
    }

    @Override
    public String toString() {
        return String.format("Wave %d: %d/%d entities killed (%s)",
            waveNumber, entitiesKilled, totalEntitiesToSpawn, state);
    }
}
