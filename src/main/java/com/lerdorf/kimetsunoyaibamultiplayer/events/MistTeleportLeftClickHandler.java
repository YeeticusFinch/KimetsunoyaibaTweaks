package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.GhostlyCloneEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Additional handler for left-click teleports during 7th form.
 * Handles left-clicks on entities, blocks, and air (via swing detection) - even during cooldown.
 * Works on both LAN worlds and dedicated servers.
 */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
public class MistTeleportLeftClickHandler {

    // Track last swing time to prevent multiple teleports per swing
    private static final Map<UUID, Integer> lastSwingTick = new HashMap<>();
    private static final int SWING_COOLDOWN = 5; // Ticks between swing detections

    /**
     * Handle left-click on entities (attacks) - triggers even during cooldown
     */
    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer && player.getPersistentData().getBoolean("mist_7th_teleport_active")) {
            Log.debug("[TELEPORT DEBUG] Attack entity triggered for " + player.getName().getString());
            triggerCloneSwing(player);
            MistTeleportHandler.performSafeTeleport(player);
        }
    }

    /**
     * Handle left-click on blocks - triggers even during cooldown
     */
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer && player.getPersistentData().getBoolean("mist_7th_teleport_active")) {
            Log.debug("[TELEPORT DEBUG] Left click block triggered for " + player.getName().getString());
            triggerCloneSwing(player);
            MistTeleportHandler.performSafeTeleport(player);
        }
    }

    /**
     * Trigger sword swing animation on all nearby ghostly clones
     */
    private static void triggerCloneSwing(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        // Find all ghostly clones within a 30 block radius (same as mist range)
        AABB searchArea = new AABB(player.position(), player.position()).inflate(30.0);
        List<GhostlyCloneEntity> clones = serverLevel.getEntitiesOfClass(
            GhostlyCloneEntity.class, searchArea,
            clone -> clone.isPlayerClone() && clone.getPlayerName().equals(player.getName().getString())
        );

        // Trigger swing animation on all player's clones
        for (GhostlyCloneEntity clone : clones) {
            clone.setSwinging(true);
        }
    }

    /**
     * Detect swing animation to catch left-clicks in the air.
     * This works server-side by detecting when swing time changes.
     * Works on both LAN worlds and dedicated servers.
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Log.startupProbeOnce("MistTeleportLeftClickHandler.onPlayerTick");

        Player player = event.player;

        // Only run on server side (works for both LAN and dedicated servers)
        if (player.level().isClientSide) return;
        if (!player.getPersistentData().getBoolean("mist_7th_teleport_active")) return;

        // Check if player is swinging
        if (player.swinging && player.swingTime == 0) {
            // Just started swinging - check cooldown
            UUID playerId = player.getUUID();
            int currentTick = player.tickCount;

            Integer lastSwing = lastSwingTick.get(playerId);
            if (lastSwing == null || (currentTick - lastSwing) >= SWING_COOLDOWN) {
                Log.debug("[TELEPORT DEBUG] Swing detected for " + player.getName().getString());
                triggerCloneSwing(player);
                MistTeleportHandler.performSafeTeleport(player);
                lastSwingTick.put(playerId, currentTick);
            }
        }
    }
}
