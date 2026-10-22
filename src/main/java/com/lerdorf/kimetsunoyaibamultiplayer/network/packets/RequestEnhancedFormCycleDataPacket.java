package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.customdemonart.CustomBloodDemonArtSavedData;
import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.items.CustomDemonArtItem;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class RequestEnhancedFormCycleDataPacket {
    public RequestEnhancedFormCycleDataPacket() {
    }

    public RequestEnhancedFormCycleDataPacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (!Config.enhancedFormCycle || player == null || !(player.level() instanceof ServerLevel serverLevel)
                || !(player.getMainHandItem().getItem() instanceof CustomDemonArtItem)) {
                return;
            }

            ItemStack stack = player.getMainHandItem();
            CustomBloodDemonArtSavedData savedData = CustomBloodDemonArtSavedData.get(serverLevel);
            CustomBloodDemonArtSavedData.PlayerArtData data = savedData.getOrCreate(player);
            int unlockedSlots = savedData.getUnlockedSlots(player);
            List<Integer> indices = new ArrayList<>();
            List<String> names = new ArrayList<>();
            for (int i = 0; i < Math.min(unlockedSlots, data.slots().size()); i++) {
                CustomBloodDemonArtSavedData.CustomFormSlot slot = data.slots().get(i);
                if (slot.filled()) {
                    indices.add(i);
                    names.add(slot.name() == null || slot.name().isBlank() ? "Form " + (i + 1) : slot.name());
                }
            }

            if (!indices.isEmpty()) {
                int selected = CustomDemonArtItem.getSelectedSlot(stack, data.selectedSlot());
                if (!indices.contains(selected)) {
                    selected = indices.get(0);
                }
                ModNetworking.sendToPlayer(
                    new EnhancedFormCycleDataPacket(indices, names, selected, CustomDemonArtItem.getDisplayColor(stack)),
                    player
                );
            }
        });
        context.setPacketHandled(true);
    }
}
