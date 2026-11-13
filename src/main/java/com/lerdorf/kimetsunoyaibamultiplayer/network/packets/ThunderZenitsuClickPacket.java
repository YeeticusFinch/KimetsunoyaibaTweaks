package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.ThunderBreathingForms;
import com.lerdorf.kimetsunoyaibamultiplayer.items.NichirinSwordZenitsu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Relays an empty-space/block M1 click to an active Zenitsu chain. */
public class ThunderZenitsuClickPacket {
    public ThunderZenitsuClickPacket() {
    }

    public ThunderZenitsuClickPacket(FriendlyByteBuf ignored) {
    }

    public void toBytes(FriendlyByteBuf ignored) {
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer player = context.getSender();
        context.enqueueWork(() -> {
            if (player == null) {
                return;
            }
            ItemStack heldItem = player.getMainHandItem();
            if (heldItem.getItem() instanceof NichirinSwordZenitsu) {
                ThunderBreathingForms.handleZenitsuClick(player);
            }
        });
        context.setPacketHandled(true);
        return true;
    }
}
