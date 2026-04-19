package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.blooddemonarts.SwampDemonArt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;

public class SwampPuddleEntity extends Entity {
    private static final double AVATAR_HITBOX_SIZE = 0.01D;
    private static final EntityDataAccessor<Boolean> PORTAL_MODE =
        SynchedEntityData.defineId(SwampPuddleEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> RENDER_SCALE =
        SynchedEntityData.defineId(SwampPuddleEntity.class, EntityDataSerializers.FLOAT);

    private UUID ownerUuid;
    private ResourceKey<Level> targetDimension;
    private Vec3 targetPos = Vec3.ZERO;
    private int remainingTicks = 0;

    public SwampPuddleEntity(EntityType<? extends SwampPuddleEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(PORTAL_MODE, false);
        this.entityData.define(RENDER_SCALE, 1.0F);
    }

    public void bindAvatar(LivingEntity owner) {
        this.ownerUuid = owner.getUUID();
        this.remainingTicks = 40;
        this.entityData.set(PORTAL_MODE, false);
        this.entityData.set(RENDER_SCALE, Math.max(0.9F, owner.getBbWidth() * 1.2F));
        updateAvatarBoundingBox(owner.position());
    }

    public void makePortal(ResourceKey<Level> targetDimension, Vec3 targetPos, int durationTicks, float renderScale) {
        this.targetDimension = targetDimension;
        this.targetPos = targetPos;
        this.remainingTicks = durationTicks;
        this.entityData.set(PORTAL_MODE, true);
        this.entityData.set(RENDER_SCALE, renderScale);
        updateAvatarBoundingBox(this.position());
    }

    public boolean isPortalMode() {
        return this.entityData.get(PORTAL_MODE);
    }

    public float getRenderScale() {
        return this.entityData.get(RENDER_SCALE);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            return;
        }

        if (remainingTicks-- <= 0) {
            this.discard();
            return;
        }

        if (!isPortalMode()) {
            tickAvatar();
            return;
        }

        tickPortal();
    }

    private void tickAvatar() {
        if (!(this.level() instanceof ServerLevel serverLevel) || ownerUuid == null) {
            this.discard();
            return;
        }

        Entity owner = serverLevel.getEntity(ownerUuid);
        if (!(owner instanceof LivingEntity living) || !living.isAlive() || !SwampDemonArt.isPuddled(living)) {
            this.discard();
            return;
        }

        float lead = 0.2f;

        this.moveTo(living.getX() + living.getDeltaMovement().x * lead, living.getY() + 0.01D, living.getZ() + living.getDeltaMovement().z * lead, living.getYRot(), 0.0F);
        this.setDeltaMovement(Vec3.ZERO);
        updateAvatarBoundingBox(this.position());
        this.remainingTicks = 40;
    }

    private void tickPortal() {
        if (!(this.level() instanceof ServerLevel serverLevel) || targetDimension == null) {
            return;
        }

        AABB area = new AABB(
            this.getX() - 0.2D,
            this.getY() - 0.4D,
            this.getZ() - 0.2D,
            this.getX() + 0.2D,
            this.getY() + 0.4D,
            this.getZ() + 0.2D
        );
        for (LivingEntity living : serverLevel.getEntitiesOfClass(LivingEntity.class, area,
            target -> target.isAlive() && !target.isSpectator()
                && !(target instanceof SwampDemonEntity)
                && !SwampDemonArt.isPortalTeleportBlocked(target, targetDimension))) {
            if (SwampDemonArt.teleportThroughPortal(living, targetDimension, targetPos)) {
                return;
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.remainingTicks = tag.getInt("RemainingTicks");
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        if (tag.contains("TargetDim")) {
            this.targetDimension = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                net.minecraft.resources.ResourceLocation.parse(tag.getString("TargetDim")));
        }
        if (tag.contains("TargetX")) {
            this.targetPos = new Vec3(tag.getDouble("TargetX"), tag.getDouble("TargetY"), tag.getDouble("TargetZ"));
        }
        this.entityData.set(PORTAL_MODE, tag.getBoolean("PortalMode"));
        this.entityData.set(RENDER_SCALE, tag.getFloat("RenderScale"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("RemainingTicks", remainingTicks);
        if (ownerUuid != null) {
            tag.putUUID("Owner", ownerUuid);
        }
        if (targetDimension != null) {
            tag.putString("TargetDim", targetDimension.location().toString());
            tag.putDouble("TargetX", targetPos.x);
            tag.putDouble("TargetY", targetPos.y);
            tag.putDouble("TargetZ", targetPos.z);
        }
        tag.putBoolean("PortalMode", isPortalMode());
        tag.putFloat("RenderScale", getRenderScale());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void updateAvatarBoundingBox(Vec3 pos) {
        double halfSize = AVATAR_HITBOX_SIZE * 0.5D;
        this.setBoundingBox(new AABB(
            pos.x - halfSize,
            pos.y,
            pos.z - halfSize,
            pos.x + halfSize,
            pos.y + AVATAR_HITBOX_SIZE,
            pos.z + halfSize
        ));
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return false;
    }
}
