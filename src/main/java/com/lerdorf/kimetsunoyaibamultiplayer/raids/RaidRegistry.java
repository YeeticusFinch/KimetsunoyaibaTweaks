package com.lerdorf.kimetsunoyaibamultiplayer.raids;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

/**
 * WorldSavedData registry for active raids, with persistence and spatial queries.
 */
public class RaidRegistry extends SavedData {
    private static final String DATA_NAME = "kny_raids";

    private final Map<UUID, KnYRaid> activeRaids = new HashMap<>();
    private final ServerLevel level;

    public RaidRegistry(ServerLevel level) {
        this.level = level;
    }

    public static RaidRegistry get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            tag -> load(level, tag),
            () -> new RaidRegistry(level),
            DATA_NAME
        );
    }

    private static RaidRegistry load(ServerLevel level, CompoundTag tag) {
        RaidRegistry registry = new RaidRegistry(level);

        if (tag.contains("raids", Tag.TAG_LIST)) {
            ListTag raids = tag.getList("raids", Tag.TAG_COMPOUND);
            for (int i = 0; i < raids.size(); i++) {
                CompoundTag raidTag = raids.getCompound(i);
                KnYRaid raid = KnYRaid.fromNBT(level, raidTag);
                if (raid != null) {
                    registry.activeRaids.put(raid.getRaidId(), raid);
                }
            }
        }

        Log.debug("Loaded " + registry.activeRaids.size() + " active raids from save data");
        return registry;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag raids = new ListTag();
        for (KnYRaid raid : activeRaids.values()) {
            raids.add(raid.toNBT());
        }
        tag.put("raids", raids);
        return tag;
    }

    /**
     * Check if there's an active raid at a specific structure.
     */
    public boolean hasRaidAt(ResourceLocation structureId, BlockPos center) {
        for (KnYRaid raid : activeRaids.values()) {
            if (raid.getStructureId().equals(structureId) && raid.getCenter().equals(center)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if there's an active raid within a radius of a position.
     */
    public boolean hasRaidNear(BlockPos pos, int radius) {
        int radiusSq = radius * radius;
        for (KnYRaid raid : activeRaids.values()) {
            if (raid.getCenter().distSqr(pos) <= radiusSq) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get a raid near a position if one exists.
     */
    public KnYRaid getRaidNear(BlockPos pos, int radius) {
        int radiusSq = radius * radius;
        for (KnYRaid raid : activeRaids.values()) {
            if (raid.getCenter().distSqr(pos) <= radiusSq) {
                return raid;
            }
        }
        return null;
    }

    /**
     * Get all active raids.
     */
    public Collection<KnYRaid> getAllRaids() {
        return Collections.unmodifiableCollection(activeRaids.values());
    }

    /**
     * Create a new raid.
     */
    public KnYRaid createRaid(ResourceLocation structureId, BlockPos center, KnYRaid.RaidType type, int difficultyLevel) {
        KnYRaid raid = new KnYRaid(level, structureId, center, type, difficultyLevel);
        activeRaids.put(raid.getRaidId(), raid);
        setDirty();
        Log.debug("Created " + type + " raid level " + difficultyLevel + " at " + center);
        return raid;
    }

    /**
     * Remove a raid.
     */
    public void removeRaid(KnYRaid raid) {
        if (activeRaids.remove(raid.getRaidId()) != null) {
            setDirty();
            Log.debug("Removed raid " + raid.getRaidId() + " at " + raid.getCenter());
        }
    }

    /**
     * Tick all active raids.
     */
    public void tick() {
        // Copy to avoid concurrent modification
        List<KnYRaid> toRemove = new ArrayList<>();

        for (KnYRaid raid : activeRaids.values()) {
            raid.tick();
            if (raid.isFinished()) {
                toRemove.add(raid);
            }
        }

        for (KnYRaid raid : toRemove) {
            removeRaid(raid);
        }

        if (!toRemove.isEmpty()) {
            setDirty();
        }
    }

    /**
     * Check if an entity UUID is part of any active raid.
     */
    public boolean isRaidEntity(UUID entityId) {
        for (KnYRaid raid : activeRaids.values()) {
            if (raid.isRaidEntity(entityId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Notify when an entity is killed.
     */
    public void onEntityKilled(UUID entityId) {
        for (KnYRaid raid : activeRaids.values()) {
            if (raid.onEntityKilled(entityId)) {
                setDirty();
                break;
            }
        }
    }

    /**
     * Notify when an entity is damaged.
     */
    public void onEntityDamaged(UUID entityId, float damage) {
        for (KnYRaid raid : activeRaids.values()) {
            raid.onEntityDamaged(entityId, damage);
        }
    }

    // Static methods for backward compatibility
    private static final Map<ServerLevel, RaidRegistry> CACHE = new WeakHashMap<>();

    public static boolean hasRaid(ServerLevel level, ResourceLocation structureId, BlockPos center) {
        return get(level).hasRaidAt(structureId, center);
    }

    public static boolean hasRaidNearby(ServerLevel level, BlockPos pos, int radius) {
        return get(level).hasRaidNear(pos, radius);
    }

    public static KnYRaid getOrCreate(ServerLevel level, ResourceLocation structureId, BlockPos center, KnYRaid.RaidType type, int difficultyLevel) {
        RaidRegistry registry = get(level);
        // Check if already exists
        for (KnYRaid raid : registry.activeRaids.values()) {
            if (raid.getStructureId().equals(structureId) && raid.getCenter().equals(center)) {
                return raid;
            }
        }
        return registry.createRaid(structureId, center, type, difficultyLevel);
    }

    public static void tickAll(ServerLevel level) {
        get(level).tick();
    }
}
