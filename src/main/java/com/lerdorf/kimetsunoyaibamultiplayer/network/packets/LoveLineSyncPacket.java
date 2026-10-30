package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.client.LoveLineRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** Synchronizes one animated Love Breathing curve with tracking clients. */
public class LoveLineSyncPacket {
    private static final int MAX_POINTS = 256;

    private final UUID entityUuid;
    private final int entityId;
    private final boolean active;
    private final Vec3[] points;

    public LoveLineSyncPacket(UUID entityUuid, int entityId, boolean active, Vec3[] points) {
        this.entityUuid = entityUuid;
        this.entityId = entityId;
        this.active = active;
        this.points = points == null ? new Vec3[0] : points;
    }

    public LoveLineSyncPacket(FriendlyByteBuf buf) {
        this.entityUuid = buf.readUUID();
        this.entityId = buf.readVarInt();
        this.active = buf.readBoolean();
        int encodedCount = Math.max(0, buf.readVarInt());
        int count = Math.min(encodedCount, MAX_POINTS);
        this.points = new Vec3[count];
        for (int i = 0; i < encodedCount; i++) {
            Vec3 point = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
            if (i < count) {
                this.points[i] = point;
            }
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(entityUuid);
        buf.writeVarInt(entityId);
        buf.writeBoolean(active);
        int count = Math.min(points.length, MAX_POINTS);
        buf.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            Vec3 point = points[i];
            buf.writeDouble(point.x);
            buf.writeDouble(point.y);
            buf.writeDouble(point.z);
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (active && points.length >= 2) {
                LoveLineRenderer.setSyncedPoints(entityUuid, entityId, points);
            } else {
                LoveLineRenderer.clearSyncedPoints(entityUuid, entityId);
            }
        }));
        context.setPacketHandled(true);
        return true;
    }
}
