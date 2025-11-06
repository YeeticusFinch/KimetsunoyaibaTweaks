package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.client.particles.BonePositionTracker;
import net.minecraft.client.Minecraft;
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

/**
 * Server-to-Client packet for rendering raw vertical slash models.
 * Allows server-side code (like breathing forms) to trigger client-side vertical slash rendering.
 */
public class RawVerticalSlashRenderPacket {
    private final String modelKey;
    private final float vert;
    private final float arcRange;
    private final int duration;
    private final float yawOffset;
    private final float pitchOffset;
    private final float rollOffset;
    private final float radiusScaler;
    private final float sizeScaler;
    private final float angleOffset;
    private final boolean reverse;
    private final UUID entityId;
    private final String animationName;
    private final Vec3 posOffset;

    /**
     * Full constructor with all parameters
     */
    public RawVerticalSlashRenderPacket(String modelKey, float vert, float arcRange, int duration,
                                        float yawOffset, float pitchOffset, float rollOffset,
                                        float radiusScaler, float sizeScaler, float angleOffset, boolean reverse,
                                        UUID entityId, String animationName, Vec3 posOffset) {
        this.modelKey = modelKey;
        this.vert = vert;
        this.arcRange = arcRange;
        this.duration = duration;
        this.yawOffset = yawOffset;
        this.pitchOffset = pitchOffset;
        this.rollOffset = rollOffset;
        this.radiusScaler = radiusScaler;
        this.sizeScaler = sizeScaler;
        this.angleOffset = angleOffset;
        this.reverse = reverse;
        this.entityId = entityId;
        this.animationName = animationName;
        this.posOffset = posOffset;
    }

    /**
     * Decode from network buffer
     */
    public RawVerticalSlashRenderPacket(FriendlyByteBuf buf) {
        this.modelKey = buf.readUtf();
        this.vert = buf.readFloat();
        this.arcRange = buf.readFloat();
        this.duration = buf.readInt();
        this.yawOffset = buf.readFloat();
        this.pitchOffset = buf.readFloat();
        this.rollOffset = buf.readFloat();
        this.radiusScaler = buf.readFloat();
        this.sizeScaler = buf.readFloat();
        this.angleOffset = buf.readFloat();
        this.reverse = buf.readBoolean();
        this.entityId = buf.readUUID();
        this.animationName = buf.readUtf();
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        this.posOffset = new Vec3(x, y, z);
    }

    /**
     * Encode to network buffer
     */
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(modelKey);
        buf.writeFloat(vert);
        buf.writeFloat(arcRange);
        buf.writeInt(duration);
        buf.writeFloat(yawOffset);
        buf.writeFloat(pitchOffset);
        buf.writeFloat(rollOffset);
        buf.writeFloat(radiusScaler);
        buf.writeFloat(sizeScaler);
        buf.writeFloat(angleOffset);
        buf.writeBoolean(reverse);
        buf.writeUUID(entityId);
        buf.writeUtf(animationName);
        buf.writeDouble(posOffset.x);
        buf.writeDouble(posOffset.y);
        buf.writeDouble(posOffset.z);
    }

    /**
     * Handle packet on client side
     */
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            // Run on client thread only
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                Minecraft mc = Minecraft.getInstance();
                ClientLevel level = mc.level;
                if (level == null) return;

                // Find the entity by UUID
                Entity entity = level.getEntity(entityId.hashCode());
                LivingEntity livingEntity = null;

                if (entity instanceof LivingEntity) {
                    livingEntity = (LivingEntity) entity;
                } else {
                    // Try alternate lookup
                    for (Entity e : level.entitiesForRendering()) {
                        if (e.getUUID().equals(entityId) && e instanceof LivingEntity) {
                            livingEntity = (LivingEntity) e;
                            break;
                        }
                    }
                }

                if (livingEntity == null) return;

                // Render the raw vertical slash model on client
                BonePositionTracker.renderRawVerticalSlash(
                    modelKey,
                    vert,
                    arcRange,
                    duration,
                    yawOffset,
                    pitchOffset,
                    rollOffset,
                    radiusScaler,
                    sizeScaler,
                    angleOffset,
                    reverse,
                    entityId,
                    animationName,
                    livingEntity,
                    posOffset
                );
            });
        });
        ctx.setPacketHandled(true);
        return true;
    }
}
