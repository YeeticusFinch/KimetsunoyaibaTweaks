package com.lerdorf.kimetsunoyaibamultiplayer.biome;

import com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedMountBiomeConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Persists the discovered center of each generated Natagumo ring region. */
@Mod.EventBusSubscriber(modid = com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer.MODID)
public final class NatagumoPeakSavedData extends SavedData {
    private static final String DATA_NAME = "kimetsunoyaibamultiplayer_natagumo_peaks";
    private static final String PEAKS_TAG = "peaks";
    private static final int DISPLAY_PEAK_Y = 280;
    private static final Map<Long, Map<Integer, Peak>> PENDING = new ConcurrentHashMap<>();

    private final Map<Integer, Peak> peaks = new ConcurrentHashMap<>();

    public static NatagumoPeakSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                NatagumoPeakSavedData::load,
                NatagumoPeakSavedData::new,
                DATA_NAME);
    }

    /** Queues a discovered peak without touching SavedData from a worldgen worker thread. */
    public static void queue(long seed, EnhancedMountBiomeSource.NatagumoRegion region) {
        if (region == null || region.strength() < EnhancedMountBiomeConfig.natagumoBiomeThreshold) {
            return;
        }
        PENDING.computeIfAbsent(seed, ignored -> new ConcurrentHashMap<>())
                .putIfAbsent(region.ring(), new Peak(
                        region.ring(),
                        region.centerX(),
                        region.centerZ(),
                        DISPLAY_PEAK_Y));
    }

    public static void flush(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        if (overworld == null) {
            return;
        }
        Map<Integer, Peak> pending = PENDING.remove(overworld.getSeed());
        if (pending == null || pending.isEmpty()) {
            return;
        }

        NatagumoPeakSavedData data = get(overworld);
        boolean changed = false;
        for (Peak peak : pending.values()) {
            if (data.peaks.putIfAbsent(peak.ring(), peak) == null) {
                changed = true;
            }
        }
        if (changed) {
            data.setDirty();
        }
    }

    public java.util.List<Peak> peaks() {
        return Collections.unmodifiableList(new ArrayList<>(peaks.values()));
    }

    private static NatagumoPeakSavedData load(CompoundTag tag) {
        NatagumoPeakSavedData data = new NatagumoPeakSavedData();
        ListTag peakTags = tag.getList(PEAKS_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < peakTags.size(); i++) {
            CompoundTag peakTag = peakTags.getCompound(i);
            data.peaks.put(peakTag.getInt("ring"), new Peak(
                    peakTag.getInt("ring"),
                    peakTag.getInt("x"),
                    peakTag.getInt("z"),
                    peakTag.getInt("y")));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag peakTags = new ListTag();
        for (Peak peak : peaks.values()) {
            CompoundTag peakTag = new CompoundTag();
            peakTag.putInt("ring", peak.ring());
            peakTag.putInt("x", peak.x());
            peakTag.putInt("z", peak.z());
            peakTag.putInt("y", peak.y());
            peakTags.add(peakTag);
        }
        tag.put(PEAKS_TAG, peakTags);
        return tag;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        try {
            flush(event.getServer());
        } catch (Exception exception) {
            System.err.println("Failed to persist Natagumo peak locations: " + exception.getMessage());
        }
    }

    public record Peak(int ring, int x, int z, int y) {
    }
}
