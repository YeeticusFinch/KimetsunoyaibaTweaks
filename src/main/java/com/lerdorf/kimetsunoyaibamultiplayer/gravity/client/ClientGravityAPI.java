package com.lerdorf.kimetsunoyaibamultiplayer.gravity.client;

import com.lerdorf.kimetsunoyaibamultiplayer.gravity.engine.GravityCapabilityImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

public final class ClientGravityAPI {
    private ClientGravityAPI() {
    }

    public static void sync(UUID entityId, boolean noAnimation, Direction baseGravityDirection, Direction currentGravityDirection,
                            double baseGravityStrength, double currentGravityStrength) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        Entity entity = null;
        for (Entity candidate : Minecraft.getInstance().level.entitiesForRendering()) {
            if (candidate.getUUID().equals(entityId)) {
                entity = candidate;
                break;
            }
        }
        if (entity == null && Minecraft.getInstance().player != null && Minecraft.getInstance().player.getUUID().equals(entityId)) {
            entity = Minecraft.getInstance().player;
        }
        if (entity == null) {
            return;
        }
        entity.getCapability(GravityCapabilityImpl.GRAVITY).ifPresent(cap -> cap.sync(
            noAnimation,
            baseGravityDirection,
            currentGravityDirection,
            baseGravityStrength,
            currentGravityStrength
        ));
    }
}
