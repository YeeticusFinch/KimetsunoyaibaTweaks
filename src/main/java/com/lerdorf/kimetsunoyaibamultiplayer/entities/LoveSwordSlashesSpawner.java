package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SpawnLoveSwordSlashesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Utility class for spawning Love Sword Slashes entities in multiplayer.
 *
 * This helper ensures that the visual effects are properly synchronized across all clients
 * by spawning the entity on the server and sending packets to all nearby clients.
 */
public class LoveSwordSlashesSpawner {
    private static final String NEZUKO_TORNADO_ANIMATION = "nezuko_tornado";
    private static final int DEFAULT_NEZUKO_TORNADO_LIFETIME_TICKS = 40;

    /**
     * Spawn a Love Sword Slashes entity at the specified location with custom orientation and animation.
     *
     * This method handles both server and client-side spawning:
     * - On server: Creates entity in world and sends packet to all clients
     * - On client: Does nothing (packet will be received from server)
     *
     * @param level The world
     * @param position The spawn position (Vec3)
     * @param yaw Horizontal rotation in degrees (0 = north, 90 = east, 180 = south, 270 = west)
     * @param pitch Vertical rotation in degrees (-90 = straight up, 0 = horizontal, 90 = straight down)
     * @param animationName The animation to play from love_sword_slashes.animation.json
     * @param lifetimeTicks How long the entity should exist (in ticks, 20 ticks = 1 second)
     */
    public static LoveSwordSlashesEntity spawnLoveSwordSlashes(Level level, Vec3 position, float yaw, float pitch,
                                             String animationName, int lifetimeTicks) {
        return spawnLoveSwordSlashes(level, position, yaw, pitch, animationName, lifetimeTicks, true);
    }

    private static LoveSwordSlashesEntity spawnLoveSwordSlashes(Level level, Vec3 position, float yaw, float pitch,
                                             String animationName, int lifetimeTicks, boolean sendCustomSpawnPacket) {
        // Only spawn on server side - clients will receive packet
        if (level.isClientSide) {
            return null;
        }

        // Create the entity on the server
        LoveSwordSlashesEntity entity = LoveSwordSlashesEntity.create(
            level, position, yaw, pitch, animationName, lifetimeTicks
        );

        // Add to world
        level.addFreshEntity(entity);

        // Send packet to all nearby clients so they can see the effect
        if (sendCustomSpawnPacket && level instanceof ServerLevel serverLevel) {
            SpawnLoveSwordSlashesPacket packet = new SpawnLoveSwordSlashesPacket(
                position, yaw, pitch, animationName, lifetimeTicks
            );

            // Send to all clients within 64 blocks
            ModNetworking.sendToNearby(packet, serverLevel,
                position.x, position.y, position.z, 64.0);
        }

        if (com.lerdorf.kimetsunoyaibamultiplayer.Config.logDebug) {
            com.lerdorf.kimetsunoyaibamultiplayer.Log.debug(
                "[LoveSwordSlashesSpawner] Spawned love sword slashes at " +
                String.format("(%.2f, %.2f, %.2f)", position.x, position.y, position.z) +
                " with animation: " + animationName +
                ", lifetime: " + lifetimeTicks + " ticks" +
                ", yaw: " + yaw + ", pitch: " + pitch
            );
        }
        
        return entity;
    }

    /**
     * Convenience method - spawn with default lifetime (40 ticks = 2 seconds).
     */
    public static void spawnLoveSwordSlashes(Level level, Vec3 position, float yaw, float pitch,
                                             String animationName) {
        spawnLoveSwordSlashes(level, position, yaw, pitch, animationName, 40);
    }

    /**
     * Convenience method - spawn facing horizontally (pitch = 0) with default lifetime.
     */
    public static void spawnLoveSwordSlashes(Level level, Vec3 position, float yaw, String animationName) {
        spawnLoveSwordSlashes(level, position, yaw, 0.0f, animationName, 40);
    }

    /**
     * Spawn a Nezuko tornado slash at a position facing the supplied direction.
     */
    public static LoveSwordSlashesEntity spawnNezukoTornado(Level level, Vec3 position, Vec3 direction) {
        return spawnNezukoTornado(level, position, direction, DEFAULT_NEZUKO_TORNADO_LIFETIME_TICKS);
    }

    /**
     * Spawn a Nezuko tornado slash at a position facing the supplied direction.
     */
    public static LoveSwordSlashesEntity spawnNezukoTornado(Level level, Vec3 position, Vec3 direction, int lifetimeTicks) {
        Vec3 normalizedDirection = direction;
        if (normalizedDirection == null || normalizedDirection.lengthSqr() < 1.0E-4D) {
            normalizedDirection = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            normalizedDirection = normalizedDirection.normalize();
        }

        float yaw = (float) Math.toDegrees(Math.atan2(-normalizedDirection.x, normalizedDirection.z));
        yaw += 180.0F;
        float pitch = (float) Math.toDegrees(-Math.asin(normalizedDirection.y));
        return spawnLoveSwordSlashes(level, position, yaw, pitch, NEZUKO_TORNADO_ANIMATION, lifetimeTicks, false);
    }
}
