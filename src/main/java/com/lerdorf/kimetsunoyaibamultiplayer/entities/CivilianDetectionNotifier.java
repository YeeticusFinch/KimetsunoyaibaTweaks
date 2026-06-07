package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CivilianDetectionNotifier {

    private static final Map<UUID, Long> LAST_SPOTTED_MESSAGE_MS = new ConcurrentHashMap<>();
    private static final long SPOTTED_MESSAGE_COOLDOWN_MS = 10_000L;

    private CivilianDetectionNotifier() {
    }

    public static void notifyDemonSpotted(LivingEntity demon) {
        if (!(demon instanceof ServerPlayer player)) {
            return;
        }

        long now = System.currentTimeMillis();
        UUID uuid = player.getUUID();
        long lastSent = LAST_SPOTTED_MESSAGE_MS.getOrDefault(uuid, 0L);
        if (now - lastSent < SPOTTED_MESSAGE_COOLDOWN_MS) {
            return;
        }

        LAST_SPOTTED_MESSAGE_MS.put(uuid, now);
        player.sendSystemMessage(Component.literal("You've been spotted!"));
    }
}
