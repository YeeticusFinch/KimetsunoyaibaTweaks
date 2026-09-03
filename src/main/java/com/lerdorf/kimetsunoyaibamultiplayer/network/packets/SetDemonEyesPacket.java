package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.events.DemonEyesSyncHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.config.CustomProgressionConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.util.DemonEyesHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SetDemonEyesPacket {
    private final int eyesIndex;
    private final int hue;
    private final float offsetX;
    private final float offsetY;

    public SetDemonEyesPacket(int eyesIndex) {
        this(eyesIndex, DemonEyesHelper.DEFAULT_DEMON_EYES_HUE);
    }

    public SetDemonEyesPacket(int eyesIndex, int hue) {
        this(eyesIndex, hue, DemonEyesHelper.DEFAULT_DEMON_EYES_OFFSET, DemonEyesHelper.DEFAULT_DEMON_EYES_OFFSET);
    }

    public SetDemonEyesPacket(int eyesIndex, int hue, float offsetX, float offsetY) {
        this.eyesIndex = Math.max(0, eyesIndex);
        this.hue = DemonEyesHelper.normalizeHue(hue);
        this.offsetX = DemonEyesHelper.normalizeOffset(offsetX);
        this.offsetY = DemonEyesHelper.normalizeOffset(offsetY);
    }

    public SetDemonEyesPacket(FriendlyByteBuf buf) {
        this.eyesIndex = Math.max(0, buf.readVarInt());
        this.hue = DemonEyesHelper.normalizeHue(buf.readVarInt());
        this.offsetX = DemonEyesHelper.normalizeOffset(buf.readFloat());
        this.offsetY = DemonEyesHelper.normalizeOffset(buf.readFloat());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(eyesIndex);
        buf.writeVarInt(hue);
        buf.writeFloat(offsetX);
        buf.writeFloat(offsetY);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !Damager.isDemon(player)
                || (eyesIndex == DemonEyesHelper.EMPTY_DEMON_EYES_INDEX
                    && !CustomProgressionConfig.isEmptyDemonEyesAllowed())) {
                return;
            }
            DemonEyesHelper.setStyle(player, eyesIndex, hue, offsetX, offsetY);
            DemonEyesSyncHandler.broadcastState(player);
        });
        context.setPacketHandled(true);
        return true;
    }
}
