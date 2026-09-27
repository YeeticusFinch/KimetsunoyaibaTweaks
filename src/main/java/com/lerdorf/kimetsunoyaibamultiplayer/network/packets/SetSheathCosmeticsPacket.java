package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.events.SheathCosmeticsSyncHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.config.SwordDisplayConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.util.SheathCosmeticsHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SetSheathCosmeticsPacket {
    private final SwordDisplayConfig.SwordDisplayPosition position;
    private final int textureIndex;
    private final double translateX;
    private final double translateY;
    private final double translateZ;
    private final double rotateX;
    private final double rotateY;
    private final double rotateZ;

    public SetSheathCosmeticsPacket(SwordDisplayConfig.SwordDisplayPosition position, int textureIndex,
                                    double translateX, double translateY, double translateZ,
                                    double rotateX, double rotateY, double rotateZ) {
        this.position = position;
        this.textureIndex = textureIndex;
        this.translateX = translateX;
        this.translateY = translateY;
        this.translateZ = translateZ;
        this.rotateX = rotateX;
        this.rotateY = rotateY;
        this.rotateZ = rotateZ;
    }

    public SetSheathCosmeticsPacket(FriendlyByteBuf buf) {
        position = buf.readBoolean() ? SwordDisplayConfig.SwordDisplayPosition.BACK : SwordDisplayConfig.SwordDisplayPosition.HIP;
        textureIndex = buf.readVarInt();
        translateX = buf.readDouble();
        translateY = buf.readDouble();
        translateZ = buf.readDouble();
        rotateX = buf.readDouble();
        rotateY = buf.readDouble();
        rotateZ = buf.readDouble();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(position == SwordDisplayConfig.SwordDisplayPosition.BACK);
        buf.writeVarInt(Math.max(0, textureIndex));
        buf.writeDouble(translateX);
        buf.writeDouble(translateY);
        buf.writeDouble(translateZ);
        buf.writeDouble(rotateX);
        buf.writeDouble(rotateY);
        buf.writeDouble(rotateZ);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            SheathCosmeticsHelper.setAll(player, position, textureIndex, translateX, translateY, translateZ,
                rotateX, rotateY, rotateZ);
            SheathCosmeticsSyncHandler.broadcastState(player);
        });
        context.setPacketHandled(true);
        return true;
    }
}
