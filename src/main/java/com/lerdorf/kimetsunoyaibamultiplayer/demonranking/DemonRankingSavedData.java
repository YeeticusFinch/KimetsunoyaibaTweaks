package com.lerdorf.kimetsunoyaibamultiplayer.demonranking;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * World-level roster of who currently holds each Demon Rank.
 * Absent/null entry for a rank means that slot is currently entity-fallback (unclaimed).
 */
public class DemonRankingSavedData extends SavedData {
    private static final String DATA_NAME = "kny_demon_ranking";

    private final Map<DemonRank, UUID> holders = new EnumMap<>(DemonRank.class);
    private final Map<UUID, Long> offlineSince = new HashMap<>();
    private final Set<DemonRank> forcedFree = EnumSet.noneOf(DemonRank.class);

    public static DemonRankingSavedData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return new DemonRankingSavedData();
        }
        return overworld.getDataStorage().computeIfAbsent(
            DemonRankingSavedData::load,
            DemonRankingSavedData::new,
            DATA_NAME
        );
    }

    private static DemonRankingSavedData load(CompoundTag tag) {
        DemonRankingSavedData data = new DemonRankingSavedData();

        if (tag.contains("holders", Tag.TAG_LIST)) {
            ListTag holdersTag = tag.getList("holders", Tag.TAG_COMPOUND);
            for (int i = 0; i < holdersTag.size(); i++) {
                CompoundTag entry = holdersTag.getCompound(i);
                DemonRank rank = rankByName(entry.getString("rank"));
                if (rank != null && entry.hasUUID("player")) {
                    data.holders.put(rank, entry.getUUID("player"));
                }
            }
        }

        if (tag.contains("offlineSince", Tag.TAG_LIST)) {
            ListTag offlineTag = tag.getList("offlineSince", Tag.TAG_COMPOUND);
            for (int i = 0; i < offlineTag.size(); i++) {
                CompoundTag entry = offlineTag.getCompound(i);
                if (entry.hasUUID("player")) {
                    data.offlineSince.put(entry.getUUID("player"), entry.getLong("since"));
                }
            }
        }

        if (tag.contains("forcedFree", Tag.TAG_LIST)) {
            ListTag forcedTag = tag.getList("forcedFree", Tag.TAG_STRING);
            for (int i = 0; i < forcedTag.size(); i++) {
                DemonRank rank = rankByName(forcedTag.getString(i));
                if (rank != null) {
                    data.forcedFree.add(rank);
                }
            }
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag holdersTag = new ListTag();
        for (Map.Entry<DemonRank, UUID> entry : holders.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString("rank", entry.getKey().name());
            entryTag.putUUID("player", entry.getValue());
            holdersTag.add(entryTag);
        }
        tag.put("holders", holdersTag);

        ListTag offlineTag = new ListTag();
        for (Map.Entry<UUID, Long> entry : offlineSince.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putUUID("player", entry.getKey());
            entryTag.putLong("since", entry.getValue());
            offlineTag.add(entryTag);
        }
        tag.put("offlineSince", offlineTag);

        ListTag forcedTag = new ListTag();
        for (DemonRank rank : forcedFree) {
            forcedTag.add(net.minecraft.nbt.StringTag.valueOf(rank.name()));
        }
        tag.put("forcedFree", forcedTag);

        return tag;
    }

    private static DemonRank rankByName(String name) {
        try {
            return DemonRank.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public UUID getHolder(DemonRank rank) {
        return holders.get(rank);
    }

    public DemonRank getRankOf(UUID player) {
        if (player == null) {
            return null;
        }
        for (Map.Entry<DemonRank, UUID> entry : holders.entrySet()) {
            if (player.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    /** Assigns a rank to a player, clearing any other rank they previously held. */
    public void assign(DemonRank rank, UUID player) {
        holders.values().removeIf(player::equals);
        holders.put(rank, player);
        forcedFree.remove(rank);
        offlineSince.remove(player);
        setDirty();
    }

    /** Frees a rank slot entirely (falls back to its entity). */
    public void clear(DemonRank rank) {
        UUID previous = holders.remove(rank);
        if (previous != null) {
            offlineSince.remove(previous);
        }
        forcedFree.remove(rank);
        setDirty();
    }

    /** Removes whatever rank the given player holds, if any. */
    public void clearPlayer(UUID player) {
        DemonRank rank = getRankOf(player);
        if (rank != null) {
            clear(rank);
        }
    }

    public void markOffline(UUID player, long timestampMillis) {
        if (getRankOf(player) != null) {
            offlineSince.put(player, timestampMillis);
            setDirty();
        }
    }

    public void markOnline(UUID player) {
        if (offlineSince.remove(player) != null) {
            setDirty();
        }
    }

    public boolean isOfflineTakeable(DemonRank rank, long nowMillis, long offlineThresholdMillis) {
        if (forcedFree.contains(rank)) {
            return true;
        }
        UUID holder = holders.get(rank);
        if (holder == null) {
            return false;
        }
        Long since = offlineSince.get(holder);
        if (since == null) {
            return false;
        }
        return (nowMillis - since) >= offlineThresholdMillis;
    }

    public void forceFree(DemonRank rank) {
        forcedFree.add(rank);
        setDirty();
    }

    public void unforceFree(DemonRank rank) {
        if (forcedFree.remove(rank)) {
            setDirty();
        }
    }
}
