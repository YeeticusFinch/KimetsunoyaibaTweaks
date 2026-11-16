package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.client.BrambleRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** Syncs one server-authored Biokineses bramble. */
public class BrambleSyncPacket {
    private final UUID brambleId;
    private final boolean active;
    private final boolean spawn;
    private final Vec3 start;
    private final Vec3 end;
    private final Vec3 velocity;
    private final Vec3 arcing;
    private final Vec3 redColor;
    private final Vec3 blackColor;
    private final float width;
    private final int lifetime;

    public BrambleSyncPacket(UUID brambleId, boolean active, boolean spawn, Vec3 start, Vec3 end,
                             Vec3 velocity, Vec3 arcing, Vec3 redColor, Vec3 blackColor,
                             float width, int lifetime) {
        this.brambleId = brambleId;
        this.active = active;
        this.spawn = spawn;
        this.start = start;
        this.end = end;
        this.velocity = velocity;
        this.arcing = arcing;
        this.redColor = redColor;
        this.blackColor = blackColor;
        this.width = width;
        this.lifetime = lifetime;
    }

    public BrambleSyncPacket(FriendlyByteBuf buf) {
        this.brambleId = buf.readUUID();
        this.active = buf.readBoolean();
        this.spawn = buf.readBoolean();
        this.start = readVec3(buf);
        this.end = readVec3(buf);
        this.velocity = readVec3(buf);
        this.arcing = readVec3(buf);
        this.redColor = readVec3(buf);
        this.blackColor = readVec3(buf);
        this.width = buf.readFloat();
        this.lifetime = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(brambleId);
        buf.writeBoolean(active);
        buf.writeBoolean(spawn);
        writeVec3(buf, start);
        writeVec3(buf, end);
        writeVec3(buf, velocity);
        writeVec3(buf, arcing);
        writeVec3(buf, redColor);
        writeVec3(buf, blackColor);
        buf.writeFloat(width);
        buf.writeVarInt(lifetime);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (active) {
                BrambleRenderer.update(brambleId, start, end, velocity, arcing,
                    redColor, blackColor, width, lifetime, spawn);
            } else {
                BrambleRenderer.clear(brambleId);
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
