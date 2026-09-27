package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.client.SheathCosmeticsClientState;
import com.lerdorf.kimetsunoyaibamultiplayer.config.SwordDisplayConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class SheathCosmeticsSyncPacket {
    private final UUID playerId;
    private final SwordDisplayConfig.SwordDisplayPosition position;
    private final int textureIndex;
    private final double translateX;
    private final double translateY;
    private final double translateZ;
    private final double rotateX;
    private final double rotateY;
    private final double rotateZ;

    public SheathCosmeticsSyncPacket(UUID playerId, SwordDisplayConfig.SwordDisplayPosition position, int textureIndex,
                                     double translateX, double translateY, double translateZ,
                                     double rotateX, double rotateY, double rotateZ) {
        this.playerId = playerId;
        this.position = position;
        this.textureIndex = Math.max(0, textureIndex);
        this.translateX = translateX;
        this.translateY = translateY;
        this.translateZ = translateZ;
        this.rotateX = rotateX;
        this.rotateY = rotateY;
        this.rotateZ = rotateZ;
    }

    public SheathCosmeticsSyncPacket(FriendlyByteBuf buf) {
        playerId = buf.readUUID();
        position = buf.readBoolean() ? SwordDisplayConfig.SwordDisplayPosition.BACK : SwordDisplayConfig.SwordDisplayPosition.HIP;
        textureIndex = Math.max(0, buf.readVarInt());
        translateX = buf.readDouble();
        translateY = buf.readDouble();
        translateZ = buf.readDouble();
        rotateX = buf.readDouble();
        rotateY = buf.readDouble();
        rotateZ = buf.readDouble();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(playerId);
        buf.writeBoolean(position == SwordDisplayConfig.SwordDisplayPosition.BACK);
        buf.writeVarInt(textureIndex);
        buf.writeDouble(translateX);
        buf.writeDouble(translateY);
        buf.writeDouble(translateZ);
        buf.writeDouble(rotateX);
        buf.writeDouble(rotateY);
        buf.writeDouble(rotateZ);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
            SheathCosmeticsClientState.set(playerId, position, textureIndex, translateX, translateY, translateZ,
                rotateX, rotateY, rotateZ)));
        context.setPacketHandled(true);
        return true;
    }
}
