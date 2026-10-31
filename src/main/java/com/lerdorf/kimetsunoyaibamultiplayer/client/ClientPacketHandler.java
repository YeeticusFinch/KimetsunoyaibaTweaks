package com.lerdorf.kimetsunoyaibamultiplayer.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.UUID;

/**
 * Client-only helper for packet handlers to safely access client-side objects
 * without importing Minecraft in the packet classes (which would crash dedicated servers).
 */
@OnlyIn(Dist.CLIENT)
public class ClientPacketHandler {

    /**
     * Gets the client level safely.
     * This method should only be called from client-side packet handlers via DistExecutor.
     */
    public static Level getClientLevel() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level;
    }

    /**
     * Gets the client player safely.
     * This method should only be called from client-side packet handlers via DistExecutor.
     */
    public static Player getClientPlayer() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player;
    }

    /** Resolves the exact tracked entity; UUIDs are not network entity IDs. */
    public static LivingEntity findLivingEntity(ClientLevel level, UUID entityUUID) {
        for (Entity entity : level.entitiesForRendering()) {
            if (entity.getUUID().equals(entityUUID) && entity instanceof LivingEntity livingEntity) {
                return livingEntity;
            }
        }
        return null;
    }
}
