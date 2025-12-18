package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.client.ClientPacketHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.LoveTornadoEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet to spawn Love Tornado entity on all clients.
 * Sent from server to clients to ensure synchronized visual effects.
 *
 * Parameters:
 * - position (Vec3): Where to spawn the entity
 * - yaw (float): Horizontal rotation in degrees
 * - pitch (float): Vertical rotation in degrees
 * - animationName (String): Animation to play from love_tornado.animation.json
 * - lifetimeTicks (int): How long the entity should exist
 */
public class SpawnLoveTornadoPacket {

    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;
    private final String animationName;
    private final int lifetimeTicks;

    public SpawnLoveTornadoPacket(Vec3 position, float yaw, float pitch, String animationName, int lifetimeTicks) {
        this.x = position.x;
        this.y = position.y;
        this.z = position.z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.animationName = animationName;
        this.lifetimeTicks = lifetimeTicks;
    }

    public SpawnLoveTornadoPacket(FriendlyByteBuf buf) {
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.yaw = buf.readFloat();
        this.pitch = buf.readFloat();
        this.animationName = buf.readUtf(256);
        this.lifetimeTicks = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeFloat(yaw);
        buf.writeFloat(pitch);
        buf.writeUtf(animationName, 256);
        buf.writeVarInt(lifetimeTicks);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            // Only process on client side
            if (ctx.getDirection().getReceptionSide().isClient()) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    handleClientSide(x, y, z, yaw, pitch, animationName, lifetimeTicks);
                });
            }
        });
        ctx.setPacketHandled(true);
        return true;
    }

    private static void handleClientSide(double x, double y, double z, float yaw, float pitch,
                                          String animationName, int lifetimeTicks) {
        try {
            Level level = ClientPacketHandler.getClientLevel();
            if (level == null) {
                return;
            }

            // Create and spawn the entity on the client
            Vec3 position = new Vec3(x, y, z);
            LoveTornadoEntity entity = LoveTornadoEntity.create(
                level, position, yaw, pitch, animationName, lifetimeTicks
            );

            // Add the entity to the world
            level.addFreshEntity(entity);

            if (com.lerdorf.kimetsunoyaibamultiplayer.Config.logDebug) {
                com.lerdorf.kimetsunoyaibamultiplayer.Log.debug(
                    "[SpawnLoveTornadoPacket] Client spawned love tornado at " +
                    String.format("(%.2f, %.2f, %.2f)", x, y, z) +
                    " with animation: " + animationName +
                    ", lifetime: " + lifetimeTicks + " ticks"
                );
            }
        } catch (Exception e) {
            System.err.println("[SpawnLoveTornadoPacket] Failed to spawn love tornado: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
