package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.client.BleedingOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class BleedingFlashPacket {
    private final int bleedingLevel;

    public BleedingFlashPacket(int bleedingLevel) {
        this.bleedingLevel = bleedingLevel;
    }

    public BleedingFlashPacket(FriendlyByteBuf buf) {
        this.bleedingLevel = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(bleedingLevel);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> BleedingOverlay.triggerFlash(bleedingLevel)));
        ctx.get().setPacketHandled(true);
    }
}
