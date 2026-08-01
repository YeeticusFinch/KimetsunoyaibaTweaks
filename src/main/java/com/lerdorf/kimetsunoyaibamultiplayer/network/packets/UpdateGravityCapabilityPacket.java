package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class UpdateGravityCapabilityPacket {
    private final boolean noAnimation;
    private final UUID entityUUID;
    private final Direction baseGravityDirection;
    private final Direction currentGravityDirection;
    private final double baseGravityStrength;
    private final double currentGravityStrength;

    public UpdateGravityCapabilityPacket(boolean noAnimation, UUID entityUUID, Direction baseGravityDirection,
                                         Direction currentGravityDirection, double baseGravityStrength,
                                         double currentGravityStrength) {
        this.noAnimation = noAnimation;
        this.entityUUID = entityUUID;
        this.baseGravityDirection = baseGravityDirection;
        this.currentGravityDirection = currentGravityDirection;
        this.baseGravityStrength = baseGravityStrength;
        this.currentGravityStrength = currentGravityStrength;
    }

    public UpdateGravityCapabilityPacket(FriendlyByteBuf buf) {
        this(buf.readBoolean(), buf.readUUID(), buf.readEnum(Direction.class), buf.readEnum(Direction.class),
            buf.readDouble(), buf.readDouble());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(noAnimation);
        buf.writeUUID(entityUUID);
        buf.writeEnum(baseGravityDirection);
        buf.writeEnum(currentGravityDirection);
        buf.writeDouble(baseGravityStrength);
        buf.writeDouble(currentGravityStrength);
    }

    public static boolean handle(UpdateGravityCapabilityPacket message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> syncClient(message));
            }
        });
        ctx.get().setPacketHandled(true);
        return true;
    }

    private static void syncClient(UpdateGravityCapabilityPacket message) {
        try {
            Class<?> clientApi = Class.forName("com.lerdorf.kimetsunoyaibamultiplayer.gravity.client.ClientGravityAPI");
            clientApi.getMethod("sync", UUID.class, boolean.class, Direction.class, Direction.class, double.class, double.class)
                .invoke(null, message.entityUUID, message.noAnimation, message.baseGravityDirection,
                    message.currentGravityDirection, message.baseGravityStrength, message.currentGravityStrength);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to apply client gravity sync", exception);
        }
    }
}
