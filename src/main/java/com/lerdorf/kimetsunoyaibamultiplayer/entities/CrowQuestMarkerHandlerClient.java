package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.config.EntityConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.sounds.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.UUID;

/**
 * Client-side quest marker handling - separated to avoid loading client classes on server
 */
@OnlyIn(Dist.CLIENT)
public class CrowQuestMarkerHandlerClient {

    /**
     * Client-only: Set quest marker and play sound
     */
    public static void setQuestMarker(UUID playerId, Vec3 targetLocation, long currentTime, long durationTicks) {
        if (!EntityConfig.crowEnhancementsEnabled) {
            return;
        }

        CrowQuestMarkerHandler.QuestMarker marker = new CrowQuestMarkerHandler.QuestMarker(
            playerId, targetLocation, currentTime, durationTicks);
        CrowQuestMarkerHandler.addQuestMarker(playerId, marker);

        // Play random crow sound to indicate waypoint was set
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getUUID().equals(playerId)) {
            // Pick a random crow sound
            SoundEvent[] crowSounds = {
                ModSounds.CROW1.get(),
                ModSounds.CROW2.get(),
                ModSounds.CROW3.get()
            };
            SoundEvent randomCrowSound = crowSounds[mc.player.getRandom().nextInt(crowSounds.length)];

            mc.level.playSound(mc.player, mc.player.blockPosition(),
                randomCrowSound, SoundSource.NEUTRAL, 1.0f, 1.0f);
        }

        if (Config.logDebug) {
            Log.info("Set quest marker for player {} to location {}", playerId, targetLocation);
        }
    }

    /**
     * Client-only: Process chat messages for quest markers
     */
    public static void onChatMessage(String message) {
        if (!EntityConfig.crowEnhancementsEnabled || !EntityConfig.crowAutoDetectQuests) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        Vec3 questLocation = CrowQuestMarkerHandler.parseCrowQuestMessage(message);
        if (questLocation != null) {
            long currentTime = mc.level.getGameTime();
            setQuestMarker(mc.player.getUUID(), questLocation, currentTime, EntityConfig.crowWaypointDuration);
            if (Config.logDebug)
                Log.info("Auto-detected crow quest, set waypoint at {}", questLocation);
        }
    }

    /**
     * Client-only: Tick quest markers (spawn particles, etc.)
     */
    public static void clientTick() {
        if (!EntityConfig.crowEnhancementsEnabled) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        Player player = mc.player;
        CrowQuestMarkerHandler.QuestMarker marker = CrowQuestMarkerHandler.getQuestMarker(player.getUUID());

        if (marker == null) {
            return;
        }

        // Check if expired
        if (marker.isExpired(mc.level.getGameTime())) {
            if (Config.logDebug) {
                Log.info("Quest marker expired");
            }
            clearQuestMarker(player.getUUID());
            return;
        }

        // Check if player reached waypoint
        if (!marker.completed && marker.isNearTarget(player.position())) {
            marker.completed = true;
            CrowQuestMarkerHandler.onWaypointReached(player, marker.targetLocation, mc.level);
            clearQuestMarker(player.getUUID());
            return;
        }

        marker.tickCounter++;

        // Draw quest arrow pointing to target
        if (EntityConfig.crowQuestArrowEnabled && marker.tickCounter % EntityConfig.crowArrowUpdateInterval == 0) {
            CrowQuestMarkerHandler.drawQuestArrow(player, marker.targetLocation, mc.level);
        }

        // Draw waypoint at target location
        if (EntityConfig.crowWaypointEnabled) {
            CrowQuestMarkerHandler.drawWaypoint(marker.targetLocation, mc.level);

            // Play subtle ambient sound at waypoint (every 3 seconds)
            if (mc.level.getGameTime() % 60 == 0) {
                mc.level.playSound(mc.player,
                    (int) marker.targetLocation.x, (int) marker.targetLocation.y, (int) marker.targetLocation.z,
                    SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.3f, 1.2f);
            }
        }
    }

    /**
     * Client-only: Clear all markers
     */
    public static void clearAllMarkers() {
        CrowQuestMarkerHandler.clearAllMarkers();
    }

    /**
     * Client-only: Clear marker for specific player
     */
    public static void clearQuestMarker(UUID playerId) {
        CrowQuestMarkerHandler.clearQuestMarker(playerId);
    }
}
