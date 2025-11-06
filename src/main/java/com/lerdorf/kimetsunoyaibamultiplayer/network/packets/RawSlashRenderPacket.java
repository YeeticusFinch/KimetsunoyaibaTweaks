package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.client.particles.BonePositionTracker;

import dev.kosmx.playerAnim.core.util.Vector3;
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

import org.joml.Vector3f;

/**
 * Server-to-Client packet for rendering raw slash models with custom angles.
 * Allows server-side code (like breathing forms) to trigger client-side slash rendering.
 */
public class RawSlashRenderPacket {
    private final String modelKey;
    private final float angle;
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
    public RawSlashRenderPacket(String modelKey, float angle, float arcRange, int duration,
                                float yawOffset, float pitchOffset, float rollOffset,
                                float radiusScaler, float sizeScaler, float angleOffset, boolean reverse,
                                UUID entityId, String animationName, Vec3 posOffset) {
        this.modelKey = modelKey;
        this.angle = angle;
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
     * Simplified constructor with default arc range and duration
     */
    public RawSlashRenderPacket(String modelKey, float angle, UUID entityId, String animationName, Vec3 posOffset) {
        this(modelKey, angle, 180f, 150, 0f, 0f, 0f, 1.0f, 1.0f, 0f, false, entityId, animationName, posOffset);
    }

    /**
     * Decode from network buffer
     */
    public RawSlashRenderPacket(FriendlyByteBuf buf) {
        this.modelKey = buf.readUtf();
        this.angle = buf.readFloat();
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
        Vector3f vec = buf.readVector3f();
        this.posOffset = new Vec3(vec.x(), vec.y(), vec.z());
    }

    /**
     * Encode to network buffer
     */
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(modelKey);
        buf.writeFloat(angle);
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
        buf.writeVector3f(new Vector3f((float)posOffset.x, (float)posOffset.y, (float)posOffset.z));
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

                // Render the raw slash model on client
                BonePositionTracker.renderRawSlash(
                    modelKey,
                    angle,
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
