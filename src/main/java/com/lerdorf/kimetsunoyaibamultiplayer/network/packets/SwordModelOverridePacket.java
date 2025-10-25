package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Packet for syncing sword model override from server to client
 * Used for effects like Golden Senses (7th Form Frost Breathing)
 */
public class SwordModelOverridePacket {
    private final UUID playerUUID;
    private final String modelLocation; // null means clear override

    public SwordModelOverridePacket(UUID playerUUID, ResourceLocation model) {
        this.playerUUID = playerUUID;
        this.modelLocation = model != null ? model.toString() : null;
    }

    public SwordModelOverridePacket(FriendlyByteBuf buf) {
        this.playerUUID = buf.readUUID();
        this.modelLocation = buf.readBoolean() ? buf.readUtf() : null;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(playerUUID);
        buf.writeBoolean(modelLocation != null);
        if (modelLocation != null) {
            buf.writeUtf(modelLocation);
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            try {
                if (ctx.getDirection().getReceptionSide().isClient()) {
                    // Use DistExecutor to safely run client code
                    net.minecraftforge.api.distmarker.Dist clientDist =
                        net.minecraftforge.api.distmarker.Dist.CLIENT;
                    net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(clientDist, () -> () -> {
                        net.minecraft.client.Minecraft mc =
                            net.minecraft.client.Minecraft.getInstance();
                        if (mc.level != null) {
                            Player player = mc.level.getPlayerByUUID(playerUUID);
                            if (player != null) {
                                ResourceLocation model = modelLocation != null ?
                                    ResourceLocation.parse(modelLocation) : null;

                                player.getCapability(KimetsunoyaibaMultiplayer.SWORD_WIELDER_DATA)
                                    .ifPresent(data -> {
                                        data.setSwordModelOverride(model);
                                        if (Config.logDebug) {
                                            Log.debug("Set sword model override for player {} to: {}",
                                                player.getName().getString(),
                                                model != null ? model : "null (cleared)");
                                        }
                                    });
                            }
                        }
                    });
                }
            } catch (Exception e) {
                if (Config.logDebug) {
                    System.err.println("Error in SwordModelOverridePacket: " + e.getMessage());
                }
            }
        });
        ctx.setPacketHandled(true);
        return true;
    }
}
