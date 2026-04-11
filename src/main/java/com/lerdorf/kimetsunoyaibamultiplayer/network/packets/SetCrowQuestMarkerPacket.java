package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.client.CrowQuestMarkerHandlerClient;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SetCrowQuestMarkerPacket {
    private final double x;
    private final double y;
    private final double z;
    private final int durationTicks;

    public SetCrowQuestMarkerPacket(Vec3 target, int durationTicks) {
        this(target.x, target.y, target.z, durationTicks);
    }

    public SetCrowQuestMarkerPacket(double x, double y, double z, int durationTicks) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.durationTicks = durationTicks;
    }

    public SetCrowQuestMarkerPacket(FriendlyByteBuf buf) {
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.durationTicks = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeVarInt(durationTicks);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null && minecraft.level != null) {
                CrowQuestMarkerHandlerClient.setQuestMarker(
                    minecraft.player.getUUID(),
                    new Vec3(x, y, z),
                    minecraft.level.getGameTime(),
                    durationTicks
                );
            }
        }));
        context.setPacketHandled(true);
        return true;
    }
}
