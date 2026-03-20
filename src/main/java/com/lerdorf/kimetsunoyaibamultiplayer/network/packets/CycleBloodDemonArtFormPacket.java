package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.items.BloodDemonArtAxeItem;
import com.lerdorf.kimetsunoyaibamultiplayer.items.BloodDemonArtItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CycleBloodDemonArtFormPacket {
    private final int direction;

    public CycleBloodDemonArtFormPacket(int direction) {
        this.direction = direction;
    }

    public CycleBloodDemonArtFormPacket(FriendlyByteBuf buf) {
        this.direction = buf.readInt();
    }

    public static void encode(CycleBloodDemonArtFormPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.direction);
    }

    public static void handle(CycleBloodDemonArtFormPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() instanceof BloodDemonArtItem artItem) {
                artItem.cycleForm(player, stack, packet.direction);
                player.getInventory().setChanged();
            } else if (stack.getItem() instanceof BloodDemonArtAxeItem artItem) {
                artItem.cycleForm(player, stack, packet.direction);
                player.getInventory().setChanged();
            }
        });
        context.setPacketHandled(true);
    }
}
