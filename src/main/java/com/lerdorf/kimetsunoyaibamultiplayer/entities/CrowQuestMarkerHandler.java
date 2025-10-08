package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.config.EntityConfig;
import net.minecraft.core.particles.ParticleTypes;
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
 * Handles quest markers for kasugai crows - draws particle arrows pointing to quest locations
 * and spawns waypoint markers at the target locations.
 *
 * NOTE: This is the SHARED (server-safe) part. Client-only methods are in CrowQuestMarkerHandlerClient.
 */
public class CrowQuestMarkerHandler {
    // Store quest markers for each player
    private static final Map<UUID, QuestMarker> activeQuests = new HashMap<>();

    // Pattern to match crow quest messages like "Mt Sagiri  is at 12 ~ 57" or "Mt Sagiri is at 12 ~ 57"
    // Also matches variations like "Location is at X ~ Z" or "Place  is at X ~ Z"
    private static final Pattern QUEST_PATTERN = Pattern.compile("(.+?)\\s+is at\\s+(-?\\d+)\\s*~\\s*(-?\\d+)", Pattern.CASE_INSENSITIVE);

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
    static void addQuestMarker(UUID playerId, QuestMarker marker) {
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
    static void onWaypointReached(Player player, Vec3 location, Level level) {
        // Play success sound
        level.playSound(player, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.0f);

        // Spawn celebration particles
        for (int i = 0; i < 50; i++) {
            double angle = Math.random() * Math.PI * 2;
            double radius = Math.random() * 2;
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            double offsetY = Math.random() * 2;

            level.addParticle(ParticleTypes.HAPPY_VILLAGER,
                    location.x + offsetX,
                    location.y + offsetY,
                    location.z + offsetZ,
                    0, 0.1, 0);

            level.addParticle(ParticleTypes.END_ROD,
                    location.x + offsetX,
                    location.y + offsetY,
                    location.z + offsetZ,
                    0, 0.05, 0);
        }

        if (Config.logDebug) {
            Log.info("Player reached waypoint at {}", location);
        }
    }

    /**
     * Attempts to parse a crow quest message from chat
     * Returns the target location if successful, null otherwise
     */
    public static Vec3 parseCrowQuestMessage(String message) {
        Matcher matcher = QUEST_PATTERN.matcher(message);
        if (matcher.find()) {
            try {
                String locationName = matcher.group(1).trim();
                int x = Integer.parseInt(matcher.group(2));
                int z = Integer.parseInt(matcher.group(3));

                // Use a reasonable Y value (surface level)
                int y = 64;
                if (Config.logDebug)
                    Log.info("Parsed crow quest: '{}' at ({}, {}, {})", locationName, x, y, z);
                return new Vec3(x, y, z);
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
     * Draws a particle arrow from the player pointing towards the quest location
     */
    static void drawQuestArrow(Player player, Vec3 target, Level level) {
        Vec3 playerPos = player.position().add(0, player.getEyeHeight(), 0);
        Vec3 direction = target.subtract(playerPos).normalize();

        // Draw arrow particles in front of the player
        double arrowLength = EntityConfig.crowArrowLength;
        int particleCount = (int) (arrowLength * 4); // 4 particles per block

        for (int i = 0; i < particleCount; i++) {
            double progress = (double) i / particleCount;
            Vec3 particlePos = playerPos.add(direction.scale(0.5 + progress * arrowLength));

            // Main arrow shaft
            level.addParticle(ParticleTypes.END_ROD,
                    particlePos.x, particlePos.y, particlePos.z,
                    0, 0, 0);

            // Arrow head at the end
            if (progress > 0.7 && progress < 0.9) {
                // Create arrow head effect with angled particles
                Vec3 perpendicular1 = new Vec3(-direction.z, 0, direction.x).normalize().scale(0.2);
                Vec3 perpendicular2 = new Vec3(0, 1, 0).cross(direction).normalize().scale(0.2);

                level.addParticle(ParticleTypes.END_ROD,
                        particlePos.x + perpendicular1.x, particlePos.y + perpendicular1.y, particlePos.z + perpendicular1.z,
                        0, 0, 0);
                level.addParticle(ParticleTypes.END_ROD,
                        particlePos.x - perpendicular1.x, particlePos.y - perpendicular1.y, particlePos.z - perpendicular1.z,
                        0, 0, 0);
                level.addParticle(ParticleTypes.END_ROD,
                        particlePos.x + perpendicular2.x, particlePos.y + perpendicular2.y, particlePos.z + perpendicular2.z,
                        0, 0, 0);
                level.addParticle(ParticleTypes.END_ROD,
                        particlePos.x - perpendicular2.x, particlePos.y - perpendicular2.y, particlePos.z - perpendicular2.z,
                        0, 0, 0);
            }
        }
    }

    /**
     * Draws a waypoint marker at the target location
     */
    static void drawWaypoint(Vec3 target, Level level) {
        // Draw a vertical beam of particles
        for (int i = 0; i < 20; i++) {
            double y = target.y + i * 0.5;
            level.addParticle(ParticleTypes.FLAME,
                    target.x, y, target.z,
                    0, 0.05, 0);

            // Add some circling particles around the beam
            double angle = (level.getGameTime() + i * 18) * 0.1;
            double radius = 0.5;
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;

            level.addParticle(ParticleTypes.END_ROD,
                    target.x + offsetX, y, target.z + offsetZ,
                    0, 0, 0);
        }

        // Add a glowing sphere at the base
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4;
            double radius = 0.8;
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;

            level.addParticle(ParticleTypes.GLOW,
                    target.x + offsetX, target.y + 0.5, target.z + offsetZ,
                    0, 0, 0);
        }
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
    static void clearAllMarkers() {
        activeQuests.clear();
        if (Config.logDebug)
            Log.info("Cleared all quest markers");
    }

    /**
     * Gets the current quest marker for a player, if any
     */
    static QuestMarker getQuestMarker(UUID playerId) {
        return activeQuests.get(playerId);
    }

    /**
     * Checks if a player has an active quest marker
     */
    static boolean hasQuestMarker(UUID playerId) {
        return activeQuests.containsKey(playerId);
    }
}
