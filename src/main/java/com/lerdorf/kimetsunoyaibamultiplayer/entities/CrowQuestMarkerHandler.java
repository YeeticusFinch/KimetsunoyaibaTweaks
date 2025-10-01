package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.config.EntityConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.sounds.ModSounds;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles quest markers for kasugai crows - draws particle arrows pointing to quest locations
 * and spawns waypoint markers at the target locations.
 */
public class CrowQuestMarkerHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

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
     * Adds or updates a quest marker for a player
     * @param playerId The player who received the quest
     * @param targetLocation The location the crow is directing them to
     * @param durationTicks How long the marker should last (in ticks, 20 = 1 second)
     */
    public static void setQuestMarker(UUID playerId, Vec3 targetLocation, long currentTime, long durationTicks) {
        if (!EntityConfig.crowEnhancementsEnabled) {
            return;
        }

        QuestMarker marker = new QuestMarker(playerId, targetLocation, currentTime, durationTicks);
        activeQuests.put(playerId, marker);

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
            LOGGER.info("Set quest marker for player {} to location {}", playerId, targetLocation);
        }
    }

    /**
     * Removes a quest marker for a player
     */
    public static void clearQuestMarker(UUID playerId) {
        activeQuests.remove(playerId);
    }

    /**
     * Client-side tick to update quest markers
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
        QuestMarker marker = activeQuests.get(player.getUUID());

        if (marker == null) {
            return;
        }

        // Check if expired
        if (marker.isExpired(mc.level.getGameTime())) {
            if (Config.logDebug) {
                LOGGER.info("Quest marker expired");
            }
            clearQuestMarker(player.getUUID());
            return;
        }

        // Check if player reached waypoint
        if (!marker.completed && marker.isNearTarget(player.position())) {
            marker.completed = true;
            onWaypointReached(player, marker.targetLocation, mc.level);
            clearQuestMarker(player.getUUID());
            return;
        }

        marker.tickCounter++;

        // Draw quest arrow pointing to target
        if (EntityConfig.crowQuestArrowEnabled && marker.tickCounter % EntityConfig.crowArrowUpdateInterval == 0) {
            drawQuestArrow(player, marker.targetLocation, mc.level);
        }

        // Draw waypoint at target location
        if (EntityConfig.crowWaypointEnabled) {
            drawWaypoint(marker.targetLocation, mc.level);
        }
    }

    /**
     * Called when player reaches a waypoint
     */
    private static void onWaypointReached(Player player, Vec3 location, Level level) {
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
            LOGGER.info("Player reached waypoint at {}", location);
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

                LOGGER.info("Parsed crow quest: '{}' at ({}, {}, {})", locationName, x, y, z);
                return new Vec3(x, y, z);
            } catch (NumberFormatException e) {
                LOGGER.warn("Failed to parse coordinates from message: {}", message);
            }
        }
        return null;
    }

    /**
     * Called when a chat message is received (client-side)
     */
    public static void onChatMessage(String message) {
        if (!EntityConfig.crowEnhancementsEnabled || !EntityConfig.crowAutoDetectQuests) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        Vec3 questLocation = parseCrowQuestMessage(message);
        if (questLocation != null) {
            long currentTime = mc.level.getGameTime();
            setQuestMarker(mc.player.getUUID(), questLocation, currentTime, EntityConfig.crowWaypointDuration);

            LOGGER.info("Auto-detected crow quest, set waypoint at {}", questLocation);
        }
    }

    /**
     * Draws a particle arrow from the player pointing towards the quest location
     */
    private static void drawQuestArrow(Player player, Vec3 target, Level level) {
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
    private static void drawWaypoint(Vec3 target, Level level) {
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

        // Play subtle ambient sound at waypoint (every 3 seconds)
        if (level.getGameTime() % 60 == 0) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                level.playSound(mc.player,
                    (int) target.x, (int) target.y, (int) target.z,
                    SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.3f, 1.2f);
            }
        }
    }

    /**
     * Clears all quest markers (called on world unload)
     */
    public static void clearAllMarkers() {
        activeQuests.clear();
        LOGGER.info("Cleared all quest markers");
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
    public static boolean hasQuestMarker(UUID playerId) {
        return activeQuests.containsKey(playerId);
    }
}