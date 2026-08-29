package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.client.PuppetLineRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class PuppetLineSyncPacket {
    private static final int MAX_ENDPOINTS = 16;

    private final UUID puppetUuid;
    private final int entityId;
    private final boolean active;
    private final Vec3[] endpoints;

    public PuppetLineSyncPacket(UUID puppetUuid, int entityId, boolean active, Vec3[] endpoints) {
        this.puppetUuid = puppetUuid;
        this.entityId = entityId;
        this.active = active;
        this.endpoints = endpoints == null ? new Vec3[0] : endpoints;
    }

    public PuppetLineSyncPacket(FriendlyByteBuf buf) {
        this.puppetUuid = buf.readUUID();
        this.entityId = buf.readVarInt();
        this.active = buf.readBoolean();
        int encodedCount = buf.readVarInt();
        int count = Math.min(encodedCount, MAX_ENDPOINTS);
        this.endpoints = new Vec3[count];
        for (int i = 0; i < encodedCount; i++) {
            Vec3 endpoint = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
            if (i < count) {
                this.endpoints[i] = endpoint;
            }
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(puppetUuid);
        buf.writeVarInt(entityId);
        buf.writeBoolean(active);
        int count = Math.min(endpoints.length, MAX_ENDPOINTS);
        buf.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            Vec3 endpoint = endpoints[i];
            buf.writeDouble(endpoint.x);
            buf.writeDouble(endpoint.y);
            buf.writeDouble(endpoint.z);
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (active) {
                PuppetLineRenderer.setSyncedEndpoints(puppetUuid, entityId, endpoints);
            } else {
                PuppetLineRenderer.clearSyncedEndpoints(puppetUuid, entityId);
            }
        }));
        context.setPacketHandled(true);
        return true;
    }
}
