package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.client.DemonPropositionClientController;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SetDemonPropositionStatePacket {
    private final boolean active;
    private final int attackerEntityId;

    public SetDemonPropositionStatePacket(boolean active, int attackerEntityId) {
        this.active = active;
        this.attackerEntityId = attackerEntityId;
    }

    public SetDemonPropositionStatePacket(FriendlyByteBuf buf) {
        this.active = buf.readBoolean();
        this.attackerEntityId = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(active);
        buf.writeInt(attackerEntityId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                if (active && attackerEntityId >= 0) {
                    DemonPropositionClientController.activate(attackerEntityId);
                } else {
                    DemonPropositionClientController.deactivate();
                }
            })
        );
        context.setPacketHandled(true);
        return true;
    }
}
