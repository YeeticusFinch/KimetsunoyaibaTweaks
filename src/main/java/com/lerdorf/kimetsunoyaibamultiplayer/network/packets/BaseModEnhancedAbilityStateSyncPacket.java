package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.client.ClientPacketHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.util.BaseModEnhancedAbilityState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** Syncs the enhanced base-mod ability lock to the local client. */
public class BaseModEnhancedAbilityStateSyncPacket {
    private final UUID playerUUID;
    private final boolean active;
    private final int formId;

    public BaseModEnhancedAbilityStateSyncPacket(UUID playerUUID, boolean active, int formId) {
        this.playerUUID = playerUUID;
        this.active = active;
        this.formId = formId;
    }

    public BaseModEnhancedAbilityStateSyncPacket(FriendlyByteBuf buf) {
        this.playerUUID = buf.readUUID();
        this.active = buf.readBoolean();
        this.formId = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(playerUUID);
        buf.writeBoolean(active);
        buf.writeVarInt(formId);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient()));
        context.setPacketHandled(true);
    }

    private void handleClient() {
        Player player = ClientPacketHandler.getClientPlayer();
        if (player == null || !player.getUUID().equals(playerUUID)) {
            return;
        }

        if (active) {
            BaseModEnhancedAbilityState.start(player, formId, player.getMainHandItem());
        } else {
            BaseModEnhancedAbilityState.clear(player);
        }
    }
}
