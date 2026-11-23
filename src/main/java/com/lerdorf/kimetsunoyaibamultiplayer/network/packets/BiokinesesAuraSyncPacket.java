package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.blooddemonarts.BiokinesesAuraTexture;
import com.lerdorf.kimetsunoyaibamultiplayer.client.BiokinesesAuraRenderer;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** Synchronizes a server-authored terrain-projected aura. */
public class BiokinesesAuraSyncPacket {
    private final UUID auraId;
    private final boolean active;
    private final boolean resetLifetime;
    private final BiokinesesAuraTexture texture;
    private final Vec3 origin;
    private final int entityId;
    private final Direction gravityDirection;
    private final float opacity;
    private final float radius;
    private final double maxDepth;
    private final double upOffset;
    private final float rotation;
    private final float rotationSpeed;
    private final int lifetime;

    public BiokinesesAuraSyncPacket(UUID auraId, boolean active, boolean resetLifetime,
                                    BiokinesesAuraTexture texture, Vec3 origin, int entityId,
                                    Direction gravityDirection, float opacity, float radius,
                                    double maxDepth, double upOffset, float rotation,
                                    float rotationSpeed, int lifetime) {
        this.auraId = auraId;
        this.active = active;
        this.resetLifetime = resetLifetime;
        this.texture = texture;
        this.origin = origin;
        this.entityId = entityId;
        this.gravityDirection = gravityDirection;
        this.opacity = opacity;
        this.radius = radius;
        this.maxDepth = maxDepth;
        this.upOffset = upOffset;
        this.rotation = rotation;
        this.rotationSpeed = rotationSpeed;
        this.lifetime = lifetime;
    }

    public BiokinesesAuraSyncPacket(FriendlyByteBuf buffer) {
        this.auraId = buffer.readUUID();
        this.active = buffer.readBoolean();
        this.resetLifetime = buffer.readBoolean();
        this.texture = buffer.readEnum(BiokinesesAuraTexture.class);
        this.origin = readVec3(buffer);
        this.entityId = buffer.readVarInt();
        this.gravityDirection = buffer.readEnum(Direction.class);
        this.opacity = buffer.readFloat();
        this.radius = buffer.readFloat();
        this.maxDepth = buffer.readDouble();
        this.upOffset = buffer.readDouble();
        this.rotation = buffer.readFloat();
        this.rotationSpeed = buffer.readFloat();
        this.lifetime = buffer.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeUUID(auraId);
        buffer.writeBoolean(active);
        buffer.writeBoolean(resetLifetime);
        buffer.writeEnum(texture);
        writeVec3(buffer, origin);
        buffer.writeVarInt(entityId);
        buffer.writeEnum(gravityDirection);
        buffer.writeFloat(opacity);
        buffer.writeFloat(radius);
        buffer.writeDouble(maxDepth);
        buffer.writeDouble(upOffset);
        buffer.writeFloat(rotation);
        buffer.writeFloat(rotationSpeed);
        buffer.writeVarInt(lifetime);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (active) {
                BiokinesesAuraRenderer.update(auraId, texture, origin, entityId, gravityDirection,
                    opacity, radius, maxDepth, upOffset, rotation, rotationSpeed, lifetime,
                    resetLifetime);
            } else {
                BiokinesesAuraRenderer.clear(auraId);
            }
        }));
        context.setPacketHandled(true);
    }

    private static Vec3 readVec3(FriendlyByteBuf buffer) {
        return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    private static void writeVec3(FriendlyByteBuf buffer, Vec3 value) {
        buffer.writeDouble(value.x);
        buffer.writeDouble(value.y);
        buffer.writeDouble(value.z);
    }
}
