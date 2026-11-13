package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.client.ThunderLightningRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** Syncs the server-authored endpoints and motion of a Thunder Lightning Spark. */
public class ThunderLightningSparkSyncPacket {
    private final UUID sparkId;
    private final boolean active;
    private final boolean spawn;
    private final Vec3 start;
    private final Vec3 end;
    private final Vec3 velocity;
    private final Vec3 arcing;
    private final Vec3 outerColor;
    private final Vec3 innerColor;
    private final float width;
    private final int lifetime;
    private final boolean flickering;

    public ThunderLightningSparkSyncPacket(UUID sparkId, boolean active, boolean spawn, Vec3 start, Vec3 end,
                                           Vec3 velocity, Vec3 arcing, Vec3 outerColor, Vec3 innerColor,
                                           float width, int lifetime, boolean flickering) {
        this.sparkId = sparkId;
        this.active = active;
        this.spawn = spawn;
        this.start = start;
        this.end = end;
        this.velocity = velocity;
        this.arcing = arcing;
        this.outerColor = outerColor;
        this.innerColor = innerColor;
        this.width = width;
        this.lifetime = lifetime;
        this.flickering = flickering;
    }

    public ThunderLightningSparkSyncPacket(FriendlyByteBuf buf) {
        this.sparkId = buf.readUUID();
        this.active = buf.readBoolean();
        this.spawn = buf.readBoolean();
        this.start = readVec3(buf);
        this.end = readVec3(buf);
        this.velocity = readVec3(buf);
        this.arcing = readVec3(buf);
        this.outerColor = readVec3(buf);
        this.innerColor = readVec3(buf);
        this.width = buf.readFloat();
        this.lifetime = buf.readVarInt();
        this.flickering = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(sparkId);
        buf.writeBoolean(active);
        buf.writeBoolean(spawn);
        writeVec3(buf, start);
        writeVec3(buf, end);
        writeVec3(buf, velocity);
        writeVec3(buf, arcing);
        writeVec3(buf, outerColor);
        writeVec3(buf, innerColor);
        buf.writeFloat(width);
        buf.writeVarInt(lifetime);
        buf.writeBoolean(flickering);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (active) {
                ThunderLightningRenderer.updateSpark(sparkId, start, end, velocity, arcing,
                    outerColor, innerColor, width, lifetime, flickering, spawn);
            } else {
                ThunderLightningRenderer.clearSpark(sparkId);
            }
        }));
        context.setPacketHandled(true);
        return true;
    }

    private static Vec3 readVec3(FriendlyByteBuf buf) {
        return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    private static void writeVec3(FriendlyByteBuf buf, Vec3 value) {
        buf.writeDouble(value.x);
        buf.writeDouble(value.y);
        buf.writeDouble(value.z);
    }
}
