package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.entities.LoveSwordSlashesEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UpdateLoveSwordSlashesPacket {
    private final int entityId;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;

    public UpdateLoveSwordSlashesPacket(int entityId, double x, double y, double z, float yaw, float pitch) {
        this.entityId = entityId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public UpdateLoveSwordSlashesPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.yaw = buf.readFloat();
        this.pitch = buf.readFloat();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeFloat(yaw);
        buf.writeFloat(pitch);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            if (ctx.getDirection().getReceptionSide().isClient()) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClientSide(entityId, x, y, z, yaw, pitch));
            }
        });
        ctx.setPacketHandled(true);
        return true;
    }

    private static void handleClientSide(int entityId, double x, double y, double z, float yaw, float pitch) {
        Level level = com.lerdorf.kimetsunoyaibamultiplayer.client.ClientPacketHandler.getClientLevel();
        if (level == null) {
            return;
        }

        Entity entity = level.getEntity(entityId);
        if (!(entity instanceof LoveSwordSlashesEntity slash)) {
            return;
        }

        slash.absMoveTo(x, y, z, yaw, pitch);
        slash.setPos(x, y, z);
        slash.yRotO = yaw;
        slash.xRotO = pitch;
        slash.setDeltaMovement(0.0D, 0.0D, 0.0D);
    }
}
