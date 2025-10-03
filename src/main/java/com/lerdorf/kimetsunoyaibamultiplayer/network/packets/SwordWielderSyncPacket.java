package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Synchronizes SWORD_WIELDER_DATA capability from server to client
 * Sends when cancelAttackSwing state changes
 */
public class SwordWielderSyncPacket {
    private final UUID playerUUID;
    private final boolean cancelAttackSwing;

    public SwordWielderSyncPacket(UUID playerUUID, boolean cancelAttackSwing) {
        this.playerUUID = playerUUID;
        this.cancelAttackSwing = cancelAttackSwing;
    }

    public SwordWielderSyncPacket(FriendlyByteBuf buf) {
        this.playerUUID = buf.readUUID();
        this.cancelAttackSwing = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(playerUUID);
        buf.writeBoolean(cancelAttackSwing);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            // This packet only goes from server -> client
            if (ctx.getDirection().getReceptionSide().isClient()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.level != null) {
                    Player player = mc.level.getPlayerByUUID(playerUUID);
                    if (player != null) {
                        player.getCapability(KimetsunoyaibaMultiplayer.SWORD_WIELDER_DATA).ifPresent(data -> {
                            data.setCancelAttackSwing(cancelAttackSwing);

                            if (Config.logDebug) {
                                Log.debug("Client received SWORD_WIELDER_DATA sync for player {}: cancelAttackSwing={}",
                                    player.getName().getString(), cancelAttackSwing);
                            }
                        });
                    }
                }
            }
        });
        ctx.setPacketHandled(true);
        return true;
    }
}
