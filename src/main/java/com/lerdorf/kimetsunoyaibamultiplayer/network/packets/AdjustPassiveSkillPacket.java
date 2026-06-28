package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.meditation.MeditationMenuService;
import com.lerdorf.kimetsunoyaibamultiplayer.meditation.PassiveSkillManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AdjustPassiveSkillPacket {
    private final String skillId;
    private final int delta;

    public AdjustPassiveSkillPacket(String skillId, int delta) {
        this.skillId = skillId == null ? "" : skillId;
        this.delta = delta < 0 ? -1 : delta > 0 ? 1 : 0;
    }

    public AdjustPassiveSkillPacket(FriendlyByteBuf buf) {
        this.skillId = buf.readUtf(128);
        int rawDelta = buf.readVarInt();
        this.delta = rawDelta < 0 ? -1 : rawDelta > 0 ? 1 : 0;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(skillId, 128);
        buf.writeVarInt(delta);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && PassiveSkillManager.adjustSkillLevel(player, skillId, delta)) {
                MeditationMenuService.openFor(player);
            }
        });
        context.setPacketHandled(true);
        return true;
    }
}
