package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server-to-client trigger for Kyogai's local gravity direction indicator. */
public class KyogaiGravityArrowPacket {
    private final Direction gravity;
    private final int durationTicks;
    private final int color;

    public KyogaiGravityArrowPacket(Direction gravity, int durationTicks, int color) {
        this.gravity = gravity;
        this.durationTicks = durationTicks;
        this.color = color;
    }

    public KyogaiGravityArrowPacket(FriendlyByteBuf buffer) {
        this.gravity = buffer.readEnum(Direction.class);
        this.durationTicks = buffer.readVarInt();
        this.color = buffer.readInt();
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeEnum(gravity);
        buffer.writeVarInt(durationTicks);
        buffer.writeInt(color);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            try {
                Class<?> renderer = Class.forName(
                    "com.lerdorf.kimetsunoyaibamultiplayer.client.KyogaiGravityArrowRenderer");
                renderer.getMethod("show", Direction.class, int.class, int.class)
                    .invoke(null, gravity, durationTicks, color);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Unable to show Kyogai gravity arrow", exception);
            }
        }));
        context.setPacketHandled(true);
    }
}
