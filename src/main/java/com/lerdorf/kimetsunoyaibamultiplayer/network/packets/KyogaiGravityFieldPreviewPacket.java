package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Targeted server-to-client state for the Kyogai gravity field preview. */
public class KyogaiGravityFieldPreviewPacket {
    private final AABB box;
    private final BlockPos sourcePos;
    private final Direction gravityDirection;
    private final int durationTicks;

    public KyogaiGravityFieldPreviewPacket(AABB box, BlockPos sourcePos,
                                           Direction gravityDirection, int durationTicks) {
        this.box = box;
        this.sourcePos = sourcePos;
        this.gravityDirection = gravityDirection;
        this.durationTicks = durationTicks;
    }

    public KyogaiGravityFieldPreviewPacket(FriendlyByteBuf buffer) {
        this.box = new AABB(
            buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
            buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        this.sourcePos = buffer.readBlockPos();
        this.gravityDirection = buffer.readEnum(Direction.class);
        this.durationTicks = buffer.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeDouble(box.minX);
        buffer.writeDouble(box.minY);
        buffer.writeDouble(box.minZ);
        buffer.writeDouble(box.maxX);
        buffer.writeDouble(box.maxY);
        buffer.writeDouble(box.maxZ);
        buffer.writeBlockPos(sourcePos);
        buffer.writeEnum(gravityDirection);
        buffer.writeVarInt(durationTicks);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            try {
                Class<?> renderer = Class.forName(
                    "com.lerdorf.kimetsunoyaibamultiplayer.client.KyogaiGravityFieldRenderer");
                renderer.getMethod("setPreview", AABB.class, BlockPos.class, Direction.class, int.class)
                    .invoke(null, box, sourcePos, gravityDirection, durationTicks);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Unable to show Kyogai gravity field", exception);
            }
        }));
        context.setPacketHandled(true);
    }
}
