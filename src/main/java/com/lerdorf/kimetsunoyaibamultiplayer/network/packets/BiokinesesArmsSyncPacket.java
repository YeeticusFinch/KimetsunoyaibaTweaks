package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.client.BiokinesesArmsClientHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.capability.IBiokinesesArmData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Synchronizes six-arm state from the server for client rendering. */
public class BiokinesesArmsSyncPacket {
    private final int entityId;
    private final boolean active;
    private final boolean climbing;
    private final boolean retracting;
    private final long expiresAt;
    private final int allocationMode;
    private final ArmState[] arms;

    public BiokinesesArmsSyncPacket(int entityId, IBiokinesesArmData data) {
        this.entityId = entityId;
        this.active = data != null && data.isActive();
        this.climbing = data != null && data.isClimbing();
        this.retracting = data != null && data.isRetracting();
        this.expiresAt = data == null ? 0L : data.getExpiresAt();
        this.allocationMode = data == null ? IBiokinesesArmData.ALLOCATION_NORMAL : data.getAllocationMode();
        this.arms = new ArmState[IBiokinesesArmData.ARM_COUNT];
        for (int i = 0; i < arms.length; i++) {
            arms[i] = data == null ? new ArmState(Vec3.ZERO, 0.0F, 0, -1, 0L, 0L, -1, 0L, -1, false)
                : new ArmState(data.getTarget(i), data.getProgress(i), data.getMode(i),
                    data.getHeldEntityId(i), data.getAttackUntil(i), data.getAttackCooldownUntil(i),
                    data.getTargetEntityId(i), data.getAnchorBlockPos(i), data.getAnchorFace(i), data.hasAnchor(i));
        }
    }

    public BiokinesesArmsSyncPacket(FriendlyByteBuf buffer) {
        entityId = buffer.readVarInt();
        active = buffer.readBoolean();
        climbing = buffer.readBoolean();
        retracting = buffer.readBoolean();
        expiresAt = buffer.readLong();
        allocationMode = buffer.readVarInt();
        arms = new ArmState[IBiokinesesArmData.ARM_COUNT];
        for (int i = 0; i < arms.length; i++) {
            arms[i] = new ArmState(
                new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                buffer.readFloat(), buffer.readVarInt(), buffer.readVarInt(), buffer.readLong(),
                buffer.readLong(), buffer.readVarInt(), buffer.readLong(), buffer.readVarInt(),
                buffer.readBoolean());
        }
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId);
        buffer.writeBoolean(active);
        buffer.writeBoolean(climbing);
        buffer.writeBoolean(retracting);
        buffer.writeLong(expiresAt);
        buffer.writeVarInt(allocationMode);
        for (ArmState arm : arms) {
            buffer.writeDouble(arm.target.x);
            buffer.writeDouble(arm.target.y);
            buffer.writeDouble(arm.target.z);
            buffer.writeFloat(arm.progress);
            buffer.writeVarInt(arm.mode);
            buffer.writeVarInt(arm.heldEntityId);
            buffer.writeLong(arm.attackUntil);
            buffer.writeLong(arm.attackCooldownUntil);
            buffer.writeVarInt(arm.targetEntityId);
            buffer.writeLong(arm.anchorBlockPos);
            buffer.writeVarInt(arm.anchorFace);
            buffer.writeBoolean(arm.hasAnchor);
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> BiokinesesArmsClientHandler.apply(this)));
        context.setPacketHandled(true);
        return true;
    }

    public int getEntityId() {
        return entityId;
    }

    public void applyTo(IBiokinesesArmData data) {
        if (data == null) {
            return;
        }
        data.setActive(active);
        data.setClimbing(climbing);
        data.setRetracting(retracting);
        data.setExpiresAt(expiresAt);
        data.setAllocationMode(allocationMode);
            for (int i = 0; i < arms.length; i++) {
                ArmState arm = arms[i];
                data.setTarget(i, arm.target);
                data.setProgress(i, arm.progress);
                data.setMode(i, arm.mode);
                data.setHeldEntityId(i, arm.heldEntityId);
                data.setAttackUntil(i, arm.attackUntil);
                data.setAttackCooldownUntil(i, arm.attackCooldownUntil);
                data.setTargetEntityId(i, arm.targetEntityId);
                if (arm.hasAnchor) {
                    data.setAnchor(i, arm.anchorBlockPos, arm.anchorFace);
                } else {
                    data.clearAnchor(i);
                }
            }
    }

    private record ArmState(Vec3 target, float progress, int mode, int heldEntityId, long attackUntil,
                            long attackCooldownUntil, int targetEntityId, long anchorBlockPos,
                            int anchorFace, boolean hasAnchor) {
    }
}
