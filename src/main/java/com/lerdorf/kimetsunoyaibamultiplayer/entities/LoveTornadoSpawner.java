package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SpawnLoveSwordSlashesPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SpawnLoveTornadoPacket;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Utility class for spawning Love Sword Slashes entities in multiplayer.
 *
 * This helper ensures that the visual effects are properly synchronized across all clients
 * by spawning the entity on the server and sending packets to all nearby clients.
 */
public class LoveTornadoSpawner {

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
    public static LoveTornadoEntity spawnLoveTornado(Level level, Vec3 position, float yaw, float pitch,
                                             String animationName, int lifetimeTicks) {
        // Only spawn on server side - clients will receive packet
        if (level.isClientSide) {
            return null;
        }

        // Create the entity on the server
        LoveTornadoEntity entity = LoveTornadoEntity.create(
            level, position, yaw, pitch, animationName, lifetimeTicks
        );

        // Add to world
        level.addFreshEntity(entity);

        // Send packet to all nearby clients so they can see the effect
        if (level instanceof ServerLevel serverLevel) {
            SpawnLoveTornadoPacket packet = new SpawnLoveTornadoPacket(
                position, yaw, pitch, animationName, lifetimeTicks
            );

            // Send to all clients within 64 blocks
            ModNetworking.sendToNearby(packet, serverLevel,
                position.x, position.y, position.z, 64.0);
        }

        if (com.lerdorf.kimetsunoyaibamultiplayer.Config.logDebug) {
            com.lerdorf.kimetsunoyaibamultiplayer.Log.debug(
                "[LoveTornadoSpawner] Spawned love tornado at " +
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
    public static void spawnLoveTornado(Level level, Vec3 position, float yaw, float pitch,
                                             String animationName) {
        spawnLoveTornado(level, position, yaw, pitch, animationName, 40);
    }

    /**
     * Convenience method - spawn facing horizontally (pitch = 0) with default lifetime.
     */
    public static void spawnLoveTornado(Level level, Vec3 position, float yaw, String animationName) {
    	spawnLoveTornado(level, position, yaw, 0.0f, animationName, 40);
    }
}
