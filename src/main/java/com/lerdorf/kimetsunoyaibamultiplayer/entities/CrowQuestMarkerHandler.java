package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.config.EntityConfig;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles the shared state for quest markers supplied by kasugai crows.
 *
 * NOTE: This is the SHARED (server-safe) part. Client-only methods are in CrowQuestMarkerHandlerClient.
 */
public class CrowQuestMarkerHandler {
    // Store quest markers for each player
    private static final Map<UUID, QuestMarker> activeQuests = new HashMap<>();

    // Pattern to match crow quest messages like "Mt Sagiri is at 12 64 57" or "Mt Sagiri is at 12 ~ 57"
    // The tilde means the height is unknown and should be resolved on the client.
    private static final Pattern QUEST_PATTERN = Pattern.compile(
        "(.+?)\\s+is at\\s+(-?\\d+)\\s+(?:(-?\\d+)\\s+(-?\\d+)|~\\s+(-?\\d+))(?!\\s*~)",
        Pattern.CASE_INSENSITIVE
    );

    public record ParsedCrowQuestMessage(Vec3 location, boolean explicitHeight) {
    }

    public static class QuestMarker {
        public final UUID playerId;
        public final Vec3 targetLocation;
        public final long expirationTime; // Game time when this quest marker expires
        public int tickCounter;
        public boolean completed;

        public QuestMarker(UUID playerId, Vec3 targetLocation, long currentTime, long durationTicks) {
            this.playerId = playerId;
            this.targetLocation = targetLocation;
            this.expirationTime = currentTime + durationTicks;
            this.tickCounter = 0;
            this.completed = false;
        }

        public boolean isExpired(long currentTime) {
            return currentTime >= expirationTime;
        }

        public boolean isNearTarget(Vec3 playerPos) {
            // Check X/Z distance only, ignore Y
            double dx = playerPos.x - targetLocation.x;
            double dz = playerPos.z - targetLocation.z;
            double distanceXZ = Math.sqrt(dx * dx + dz * dz);
            return distanceXZ <= EntityConfig.crowWaypointCompleteDistance;
        }
    }

    /**
     * Package-private: Add a quest marker (used by client wrapper)
     */
    public static void addQuestMarker(UUID playerId, QuestMarker marker) {
        activeQuests.put(playerId, marker);
    }

    /**
     * Removes a quest marker for a player
     */
    public static void clearQuestMarker(UUID playerId) {
        activeQuests.remove(playerId);
    }

    /**
     * Package-private: Client-side tick to update quest markers
     * Called from CrowQuestMarkerHandlerClient
     */
    static void clientTick() {
        // This will be called from the client wrapper with Minecraft instance
        // Actual implementation moved to CrowQuestMarkerHandlerClient
    }

    /**
     * Called when player reaches a waypoint
     */
    public static void onWaypointReached(Player player, Vec3 location, Level level) {
        // Play success sound
        level.playSound(player, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.0f);

        if (Config.logDebug) {
            Log.info("Player reached waypoint at {}", location);
        }
    }

    /**
     * Attempts to parse a crow quest message from chat
     * Returns the target location if successful, null otherwise
     */
    public static ParsedCrowQuestMessage parseCrowQuestMessage(String message) {
        Matcher matcher = QUEST_PATTERN.matcher(message);
        if (matcher.find()) {
            try {
                String locationName = matcher.group(1).trim();
                int x = Integer.parseInt(matcher.group(2));
                String yGroup = matcher.group(3);
                boolean explicitHeight = yGroup != null;
                int y = explicitHeight ? Integer.parseInt(yGroup) : 64;
                int z = Integer.parseInt(explicitHeight ? matcher.group(4) : matcher.group(5));

                if (Config.logDebug) {
                    if (explicitHeight) {
                        Log.info("Parsed crow quest: '{}' at ({}, {}, {})", locationName, x, y, z);
                    } else {
                        Log.info("Parsed crow quest: '{}' at ({}, ~, {})", locationName, x, z);
                    }
                }
                return new ParsedCrowQuestMessage(new Vec3(x, y, z), explicitHeight);
            } catch (NumberFormatException e) {
                if (Config.logDebug)
                Log.warn("Failed to parse coordinates from message: {}", message);
            }
        }
        return null;
    }

    /**
     * Package-private: Called when a chat message is received (client-side)
     * Called from CrowQuestMarkerHandlerClient
     */
    static void onChatMessage(String message) {
        // Implementation moved to client wrapper to avoid Minecraft.getInstance()
    }

    /**
     * Play waypoint sound (called from client wrapper)
     */
    static void playWaypointSound(Level level, Vec3 target) {
        // Sound playback moved to client wrapper to avoid Minecraft.getInstance()
    }

    /**
     * Clears all quest markers (called on world unload)
     */
    public static void clearAllMarkers() {
        activeQuests.clear();
        //if (Config.logDebug)
        //    Log.info("Cleared all quest markers");
    }

    /**
     * Gets the current quest marker for a player, if any
     */
    public static QuestMarker getQuestMarker(UUID playerId) {
        return activeQuests.get(playerId);
    }

    /**
     * Checks if a player has an active quest marker
     */
    static boolean hasQuestMarker(UUID playerId) {
        return activeQuests.containsKey(playerId);
    }
}
