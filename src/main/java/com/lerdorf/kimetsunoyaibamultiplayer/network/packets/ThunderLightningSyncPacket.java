package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.client.ThunderLightningRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** Syncs the server-authoritative path and colors of an enhanced Thunder bolt. */
public class ThunderLightningSyncPacket {
    private static final int MAX_POSITIONS = 64;

    private final UUID boltId;
    private final boolean active;
    private final boolean spawn;
    private final Vec3[] positions;
    private final Vec3 primaryColor;
    private final Vec3 secondaryColor;
    private final boolean whiteMiddle;
    private final int lifetime;

    public ThunderLightningSyncPacket(UUID boltId, boolean active, boolean spawn, Vec3[] positions,
                                      Vec3 primaryColor, Vec3 secondaryColor, boolean whiteMiddle,
                                      int lifetime) {
        this.boltId = boltId;
        this.active = active;
        this.spawn = spawn;
        this.positions = positions == null ? new Vec3[0] : positions;
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.whiteMiddle = whiteMiddle;
        this.lifetime = lifetime;
    }

    public ThunderLightningSyncPacket(FriendlyByteBuf buf) {
        this.boltId = buf.readUUID();
        this.active = buf.readBoolean();
        this.spawn = buf.readBoolean();
        int encodedCount = Math.max(0, buf.readVarInt());
        int count = Math.min(encodedCount, MAX_POSITIONS);
        this.positions = new Vec3[count];
        for (int i = 0; i < encodedCount; i++) {
            Vec3 position = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
            if (i < count) {
                this.positions[i] = position;
            }
        }
        this.primaryColor = new Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat());
        this.secondaryColor = new Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat());
        this.whiteMiddle = buf.readBoolean();
        this.lifetime = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(boltId);
        buf.writeBoolean(active);
        buf.writeBoolean(spawn);
        int count = Math.min(positions.length, MAX_POSITIONS);
        buf.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            Vec3 position = positions[i];
            buf.writeDouble(position.x);
            buf.writeDouble(position.y);
            buf.writeDouble(position.z);
        }
        buf.writeFloat((float) primaryColor.x);
        buf.writeFloat((float) primaryColor.y);
        buf.writeFloat((float) primaryColor.z);
        buf.writeFloat((float) secondaryColor.x);
        buf.writeFloat((float) secondaryColor.y);
        buf.writeFloat((float) secondaryColor.z);
        buf.writeBoolean(whiteMiddle);
        buf.writeVarInt(lifetime);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (active) {
                ThunderLightningRenderer.updateBolt(boltId, positions, primaryColor, secondaryColor,
                    whiteMiddle, lifetime, spawn);
            } else {
                ThunderLightningRenderer.clearBolt(boltId);
            }
        }));
        context.setPacketHandled(true);
        return true;
    }
}
