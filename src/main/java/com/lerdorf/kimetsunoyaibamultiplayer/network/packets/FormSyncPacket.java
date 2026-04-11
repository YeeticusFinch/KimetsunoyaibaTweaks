package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData;
import com.lerdorf.kimetsunoyaibamultiplayer.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Packet sent from server to clients to synchronize breathing form selection.
 * This ensures all clients know which form a player has selected.
 */
public class FormSyncPacket {
    private final UUID playerId;
    private final String styleKey;
    private final int formIndex;

    public FormSyncPacket(UUID playerId, String styleKey, int formIndex) {
        this.playerId = playerId;
        this.styleKey = styleKey;
        this.formIndex = formIndex;
    }

    public FormSyncPacket(FriendlyByteBuf buf) {
        this.playerId = buf.readUUID();
        this.styleKey = buf.readUtf();
        this.formIndex = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(playerId);
        buf.writeUtf(styleKey);
        buf.writeInt(formIndex);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // This packet is received on the client side
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                var level = ClientPacketHandler.getClientLevel();
                if (level != null) {
                    Player player = level.getPlayerByUUID(playerId);
                    if (player != null) {
                        // Update the player's form index in client-side data
                        PlayerBreathingData.PlayerData data = PlayerBreathingData.getOrCreate(playerId);
                        data.setCurrentFormIndex(styleKey, formIndex);
                    }
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
