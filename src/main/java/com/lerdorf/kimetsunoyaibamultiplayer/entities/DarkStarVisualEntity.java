package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

public class DarkStarVisualEntity extends Entity {
    private static final EntityDataAccessor<Integer> DATA_TINT_COLOR =
        SynchedEntityData.defineId(DarkStarVisualEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_RENDER_SCALE =
        SynchedEntityData.defineId(DarkStarVisualEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_LIFETIME_TICKS =
        SynchedEntityData.defineId(DarkStarVisualEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER_UUID =
        SynchedEntityData.defineId(DarkStarVisualEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    public DarkStarVisualEntity(EntityType<? extends DarkStarVisualEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
        this.setInvisible(true);
        this.setInvulnerable(true);
        this.setSilent(true);
        this.setNoGravity(true);
    }

    public static DarkStarVisualEntity create(Level level, Vec3 position, UUID ownerUuid, int tintColor, float renderScale, int lifetimeTicks) {
        DarkStarVisualEntity entity = new DarkStarVisualEntity(ModEntities.DARK_STAR_VISUAL.get(), level);
        entity.setPos(position.x, position.y, position.z);
        entity.setOwnerUuid(ownerUuid);
        entity.setTintColor(tintColor);
        entity.setRenderScale(renderScale);
        entity.setLifetimeTicks(lifetimeTicks);
        return entity;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_TINT_COLOR, 0xFFFFFF);
        this.entityData.define(DATA_RENDER_SCALE, 10.0F);
        this.entityData.define(DATA_LIFETIME_TICKS, 80);
        this.entityData.define(DATA_OWNER_UUID, Optional.empty());
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.tickCount >= getLifetimeTicks()) {
            this.discard();
        }
    }

    public void setTintColor(int tintColor) {
        this.entityData.set(DATA_TINT_COLOR, tintColor & 0xFFFFFF);
    }

    public int getTintColor() {
        return this.entityData.get(DATA_TINT_COLOR);
    }

    public void setRenderScale(float renderScale) {
        this.entityData.set(DATA_RENDER_SCALE, Math.max(0.0F, renderScale));
    }

    public float getRenderScale() {
        return this.entityData.get(DATA_RENDER_SCALE);
    }

    public void setLifetimeTicks(int lifetimeTicks) {
        this.entityData.set(DATA_LIFETIME_TICKS, Math.max(1, lifetimeTicks));
    }

    public int getLifetimeTicks() {
        return this.entityData.get(DATA_LIFETIME_TICKS);
    }

    public void setOwnerUuid(UUID ownerUuid) {
        this.entityData.set(DATA_OWNER_UUID, Optional.ofNullable(ownerUuid));
    }

    public UUID getOwnerUuid() {
        return this.entityData.get(DATA_OWNER_UUID).orElse(null);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("TintColor", getTintColor());
        tag.putFloat("RenderScale", getRenderScale());
        tag.putInt("LifetimeTicks", getLifetimeTicks());
        if (getOwnerUuid() != null) {
            tag.putUUID("Owner", getOwnerUuid());
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("TintColor")) {
            setTintColor(tag.getInt("TintColor"));
        }
        if (tag.contains("RenderScale")) {
            setRenderScale(tag.getFloat("RenderScale"));
        }
        if (tag.contains("LifetimeTicks")) {
            setLifetimeTicks(tag.getInt("LifetimeTicks"));
        }
        if (tag.hasUUID("Owner")) {
            setOwnerUuid(tag.getUUID("Owner"));
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
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
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 16384.0D;
    }
}
