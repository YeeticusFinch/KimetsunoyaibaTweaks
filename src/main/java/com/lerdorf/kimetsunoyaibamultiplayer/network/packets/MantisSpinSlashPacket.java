package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.client.ClientPacketHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.client.particles.BonePositionTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** Server-to-client rendering instruction for the Mantis Demon spin attack. */
public final class MantisSpinSlashPacket {
    private static final int DURATION_MILLIS = 750;

    private final UUID entityId;

    public MantisSpinSlashPacket(UUID entityId) {
        this.entityId = entityId;
    }

    public MantisSpinSlashPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readUUID();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.entityId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> handleClient(this.entityId)));
        context.setPacketHandled(true);
        return true;
    }

    private static void handleClient(UUID entityId) {
        var level = ClientPacketHandler.getClientLevel();
        if (!(level instanceof ClientLevel clientLevel)) {
            return;
        }

        Entity found = clientLevel.getEntity(entityId.hashCode());
        LivingEntity living = found instanceof LivingEntity ? (LivingEntity) found : null;
        if (living == null) {
            for (Entity entity : clientLevel.entitiesForRendering()) {
                if (entity.getUUID().equals(entityId) && entity instanceof LivingEntity candidate) {
                    living = candidate;
                    break;
                }
            }
        }
        if (living == null) {
            return;
        }

        BonePositionTracker.renderRawSlash(
            "claw", 0.0F, 360.0F, DURATION_MILLIS,
            0.0F, 0.0F, 0.0F, 1.0F, 2.0F, 0.0F, false,
            entityId, "mantis_spin", living, Vec3.ZERO
        );
    }
}
