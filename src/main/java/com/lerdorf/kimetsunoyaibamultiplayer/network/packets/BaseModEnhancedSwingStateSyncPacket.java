package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.client.ClientPacketHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.util.BaseModEnhancedSwingState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** Syncs ownership of base-mod sword left-click handling to the local client. */
public class BaseModEnhancedSwingStateSyncPacket {
    private final UUID playerUUID;
    private final boolean active;
    private final boolean resetTransientState;

    public BaseModEnhancedSwingStateSyncPacket(UUID playerUUID, boolean active) {
        this(playerUUID, active, false);
    }

    public BaseModEnhancedSwingStateSyncPacket(UUID playerUUID, boolean active, boolean resetTransientState) {
        this.playerUUID = playerUUID;
        this.active = active;
        this.resetTransientState = resetTransientState;
    }

    public BaseModEnhancedSwingStateSyncPacket(FriendlyByteBuf buf) {
        this.playerUUID = buf.readUUID();
        this.active = buf.readBoolean();
        this.resetTransientState = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(playerUUID);
        buf.writeBoolean(active);
        buf.writeBoolean(resetTransientState);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient()));
        context.setPacketHandled(true);
    }

    private void handleClient() {
        Player player = ClientPacketHandler.getClientPlayer();
        if (player != null && player.getUUID().equals(playerUUID)) {
            if (resetTransientState) {
                BaseModEnhancedSwingState.resetTransientState(player);
            } else {
                BaseModEnhancedSwingState.setActive(player, active);
            }
        }
    }
}
