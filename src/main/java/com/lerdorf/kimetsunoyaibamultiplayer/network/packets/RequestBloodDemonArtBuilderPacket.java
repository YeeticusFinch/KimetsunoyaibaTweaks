package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.customdemonart.BloodDemonArtBuilderData;
import com.lerdorf.kimetsunoyaibamultiplayer.customdemonart.CustomBloodDemonArtSavedData;
import com.lerdorf.kimetsunoyaibamultiplayer.events.DemonTransformationHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RequestBloodDemonArtBuilderPacket {
    public RequestBloodDemonArtBuilderPacket() {
    }

    public RequestBloodDemonArtBuilderPacket(FriendlyByteBuf buf) {
    }

    public static void encode(RequestBloodDemonArtBuilderPacket packet, FriendlyByteBuf buf) {
    }

    public static void handle(RequestBloodDemonArtBuilderPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !(player.level() instanceof ServerLevel serverLevel) || !player.getPersistentData().getBoolean("oni")) {
                return;
            }

            CustomBloodDemonArtSavedData savedData = CustomBloodDemonArtSavedData.get(serverLevel);
            BloodDemonArtBuilderData data = BloodDemonArtBuilderData.fromPlayerData(
                player.experienceLevel,
                DemonTransformationHandler.getTrackedMuzanBlood(player),
                savedData.getUnlockedSlots(player),
                savedData.hasCustomItem(player, new net.minecraft.world.item.ItemStack(ModItems.CUSTOM_DEMON_ART.get())),
                savedData.getOrCreate(player)
            );
            ModNetworking.sendToPlayer(new OpenBloodDemonArtBuilderPacket(data), player);
        });
        context.setPacketHandled(true);
    }
}
