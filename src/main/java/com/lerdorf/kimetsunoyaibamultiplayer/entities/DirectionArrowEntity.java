package com.lerdorf.kimetsunoyaibamultiplayer.entities;

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
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** A temporary, client-visible arrow with a configurable direction and tint. */
public class DirectionArrowEntity extends Entity implements GeoEntity {
    private static final EntityDataAccessor<Float> DATA_DIRECTION_X =
        SynchedEntityData.defineId(DirectionArrowEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DIRECTION_Y =
        SynchedEntityData.defineId(DirectionArrowEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DIRECTION_Z =
        SynchedEntityData.defineId(DirectionArrowEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_COLOR =
        SynchedEntityData.defineId(DirectionArrowEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LIFETIME =
        SynchedEntityData.defineId(DirectionArrowEntity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public DirectionArrowEntity(EntityType<? extends DirectionArrowEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
        this.setInvulnerable(true);
        this.setSilent(true);
        this.setNoGravity(true);
    }

    public static DirectionArrowEntity create(Level level, Vec3 position, Vec3 direction,
                                              int color, int lifetimeTicks) {
        DirectionArrowEntity arrow = new DirectionArrowEntity(ModEntities.DIRECTION_ARROW.get(), level);
        arrow.setPos(position.x, position.y, position.z);
        arrow.setDirection(direction);
        arrow.setColor(color);
        arrow.setLifetime(lifetimeTicks);
        return arrow;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_DIRECTION_X, 0.0F);
        this.entityData.define(DATA_DIRECTION_Y, -1.0F);
        this.entityData.define(DATA_DIRECTION_Z, 0.0F);
        this.entityData.define(DATA_COLOR, 0xFFFFFF);
        this.entityData.define(DATA_LIFETIME, 1);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount >= getLifetime()) {
            this.discard();
        }
    }

    @Override
    public void registerControllers(software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "arrow", 0,
            state -> state.setAndContinue(RawAnimation.begin().thenLoop("arrow"))));
    }

    public void setDirection(Vec3 direction) {
        if (direction.lengthSqr() < 1.0E-6D) {
            direction = new Vec3(0.0D, -1.0D, 0.0D);
        } else {
            direction = direction.normalize();
        }
        this.entityData.set(DATA_DIRECTION_X, (float) direction.x);
        this.entityData.set(DATA_DIRECTION_Y, (float) direction.y);
        this.entityData.set(DATA_DIRECTION_Z, (float) direction.z);
    }

    public Vec3 getArrowDirection() {
        return new Vec3(
            this.entityData.get(DATA_DIRECTION_X),
            this.entityData.get(DATA_DIRECTION_Y),
            this.entityData.get(DATA_DIRECTION_Z));
    }

    public void setColor(int color) {
        this.entityData.set(DATA_COLOR, color & 0xFFFFFF);
    }

    public int getColor() {
        return this.entityData.get(DATA_COLOR);
    }

    public void setLifetime(int lifetimeTicks) {
        this.entityData.set(DATA_LIFETIME, Math.max(1, lifetimeTicks));
    }

    public int getLifetime() {
        return this.entityData.get(DATA_LIFETIME);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        Vec3 direction = getArrowDirection();
        tag.putDouble("DirectionX", direction.x);
        tag.putDouble("DirectionY", direction.y);
        tag.putDouble("DirectionZ", direction.z);
        tag.putInt("Color", getColor());
        tag.putInt("Lifetime", getLifetime());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setDirection(new Vec3(tag.getDouble("DirectionX"), tag.getDouble("DirectionY"),
            tag.getDouble("DirectionZ")));
        if (tag.contains("Color")) {
            setColor(tag.getInt("Color"));
        }
        if (tag.contains("Lifetime")) {
            setLifetime(tag.getInt("Lifetime"));
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

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
