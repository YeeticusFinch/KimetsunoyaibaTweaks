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

/** A temporary, synchronized Bezier curve used by Beast Breathing's tenth form. */
public class BeastBezierCurveEntity extends Entity {
    private static final EntityDataAccessor<Float> AXIS_X =
        SynchedEntityData.defineId(BeastBezierCurveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> AXIS_Y =
        SynchedEntityData.defineId(BeastBezierCurveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> AXIS_Z =
        SynchedEntityData.defineId(BeastBezierCurveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> RADIUS =
        SynchedEntityData.defineId(BeastBezierCurveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> PHASE =
        SynchedEntityData.defineId(BeastBezierCurveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> WHITE_COLOR =
        SynchedEntityData.defineId(BeastBezierCurveEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> LIFETIME =
        SynchedEntityData.defineId(BeastBezierCurveEntity.class, EntityDataSerializers.INT);

    public BeastBezierCurveEntity(EntityType<? extends BeastBezierCurveEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.setSilent(true);
    }

    public static BeastBezierCurveEntity create(Level level, Vec3 center, Vec3 axis,
                                                 double radius, double phase, int lifetime) {
        BeastBezierCurveEntity curve = new BeastBezierCurveEntity(ModEntities.BEAST_BEZIER_CURVE.get(), level);
        curve.setCurve(center, axis, radius, phase);
        curve.setLifetime(lifetime);
        return curve;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(AXIS_X, 0.0F);
        this.entityData.define(AXIS_Y, 0.0F);
        this.entityData.define(AXIS_Z, 1.0F);
        this.entityData.define(RADIUS, 0.5F);
        this.entityData.define(PHASE, 0.0F);
        this.entityData.define(WHITE_COLOR, false);
        this.entityData.define(LIFETIME, 1);
    }

    public void setCurve(Vec3 center, Vec3 axis, double radius, double phase) {
        Vec3 normalizedAxis = axis.lengthSqr() < 1.0E-6D
            ? new Vec3(0, 0, 1) : axis.normalize();
        this.setPos(center.x, center.y, center.z);
        this.entityData.set(AXIS_X, (float) normalizedAxis.x);
        this.entityData.set(AXIS_Y, (float) normalizedAxis.y);
        this.entityData.set(AXIS_Z, (float) normalizedAxis.z);
        this.entityData.set(RADIUS, (float) Math.max(0.05D, radius));
        this.entityData.set(PHASE, (float) phase);
    }

    public Vec3 getAxis() {
        return new Vec3(this.entityData.get(AXIS_X), this.entityData.get(AXIS_Y), this.entityData.get(AXIS_Z));
    }

    public double getRadius() {
        return this.entityData.get(RADIUS);
    }

    public double getPhase() {
        return this.entityData.get(PHASE);
    }

    public void setWhiteColor(boolean white) {
        this.entityData.set(WHITE_COLOR, white);
    }

    public boolean usesWhiteColor() {
        return this.entityData.get(WHITE_COLOR);
    }

    public void setLifetime(int lifetime) {
        this.entityData.set(LIFETIME, Math.max(1, lifetime));
    }

    public int getLifetime() {
        return this.entityData.get(LIFETIME);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.tickCount >= this.getLifetime()) {
            this.discard();
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        Vec3 axis = this.getAxis();
        tag.putDouble("AxisX", axis.x);
        tag.putDouble("AxisY", axis.y);
        tag.putDouble("AxisZ", axis.z);
        tag.putDouble("Radius", this.getRadius());
        tag.putDouble("Phase", this.getPhase());
        tag.putBoolean("WhiteColor", this.usesWhiteColor());
        tag.putInt("Lifetime", this.getLifetime());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.setCurve(position(), new Vec3(tag.getDouble("AxisX"), tag.getDouble("AxisY"), tag.getDouble("AxisZ")),
            tag.getDouble("Radius"), tag.getDouble("Phase"));
        if (tag.contains("WhiteColor")) {
            this.setWhiteColor(tag.getBoolean("WhiteColor"));
        }
        if (tag.contains("Lifetime")) {
            this.setLifetime(tag.getInt("Lifetime"));
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
