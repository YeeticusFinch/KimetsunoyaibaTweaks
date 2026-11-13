package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -> client packet to show a temporary directional arrow toward the active boss.
 */
public class BossArrowPacket {
    private final double bossX;
    private final double bossY;
    private final double bossZ;
    private final int remainingTicks;

    public BossArrowPacket(double bossX, double bossY, double bossZ, int remainingTicks) {
        this.bossX = bossX;
        this.bossY = bossY;
        this.bossZ = bossZ;
        this.remainingTicks = remainingTicks;
    }

    public BossArrowPacket(FriendlyByteBuf buf) {
        this.bossX = buf.readDouble();
        this.bossY = buf.readDouble();
        this.bossZ = buf.readDouble();
        this.remainingTicks = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeDouble(bossX);
        buf.writeDouble(bossY);
        buf.writeDouble(bossZ);
        buf.writeInt(remainingTicks);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (remainingTicks <= 0) return;
            try {
                Class<?> renderer = Class.forName(
                    "com.lerdorf.kimetsunoyaibamultiplayer.client.CrowQuestMarkerRenderer");
                renderer.getMethod("setBossArrow", Vec3.class, long.class)
                    .invoke(null, new Vec3(bossX, bossY, bossZ), (long) remainingTicks);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Unable to show boss direction arrow", exception);
            }
        }));
        ctx.get().setPacketHandled(true);
    }
}
