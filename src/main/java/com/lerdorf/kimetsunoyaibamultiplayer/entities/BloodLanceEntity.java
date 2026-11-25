package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Server-authoritative blood lance projectile used by Muzan's Blood Demon Arts. */
public final class BloodLanceEntity extends Projectile {
    private static final float DEFAULT_DAMAGE = 4.0F;
    private static final int MAX_LIFETIME_TICKS = 60;
    private static final double SPEED_MULTIPLIER = 1.5D;
    private static final DustParticleOptions TRAIL_DUST =
        new DustParticleOptions(new Vector3f(0.8F, 0.02F, 0.02F), 0.45F);
    private static final EntityDataAccessor<Boolean> DATA_ORBITING =
        SynchedEntityData.defineId(BloodLanceEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_ORBIT_CENTER_X =
        SynchedEntityData.defineId(BloodLanceEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_ORBIT_CENTER_Y =
        SynchedEntityData.defineId(BloodLanceEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_ORBIT_CENTER_Z =
        SynchedEntityData.defineId(BloodLanceEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_ORBIT_AXIS_X =
        SynchedEntityData.defineId(BloodLanceEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_ORBIT_AXIS_Y =
        SynchedEntityData.defineId(BloodLanceEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_ORBIT_AXIS_Z =
        SynchedEntityData.defineId(BloodLanceEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_ORBIT_BASIS_X =
        SynchedEntityData.defineId(BloodLanceEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_ORBIT_BASIS_Y =
        SynchedEntityData.defineId(BloodLanceEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_ORBIT_BASIS_Z =
        SynchedEntityData.defineId(BloodLanceEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_ORBIT_RADIUS =
        SynchedEntityData.defineId(BloodLanceEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_ORBIT_SPEED =
        SynchedEntityData.defineId(BloodLanceEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_ORBIT_ANGLE =
        SynchedEntityData.defineId(BloodLanceEntity.class, EntityDataSerializers.FLOAT);

    private float damage = DEFAULT_DAMAGE;

    public BloodLanceEntity(EntityType<? extends BloodLanceEntity> type, Level level) {
        super(type, level);
        this.noPhysics = false;
        this.setNoGravity(true);
    }

    public static BloodLanceEntity create(Level level, LivingEntity owner, Vec3 position,
                                          Vec3 velocity, float damage) {
        BloodLanceEntity lance = new BloodLanceEntity(ModEntities.BLOOD_LANCE.get(), level);
        lance.setOwner(owner);
        lance.setPos(position.x, position.y, position.z);
        lance.setDamage(damage);
        lance.setVelocity(velocity);
        return lance;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_ORBITING, false);
        entityData.define(DATA_ORBIT_CENTER_X, 0.0F);
        entityData.define(DATA_ORBIT_CENTER_Y, 0.0F);
        entityData.define(DATA_ORBIT_CENTER_Z, 0.0F);
        entityData.define(DATA_ORBIT_AXIS_X, 0.0F);
        entityData.define(DATA_ORBIT_AXIS_Y, 1.0F);
        entityData.define(DATA_ORBIT_AXIS_Z, 0.0F);
        entityData.define(DATA_ORBIT_BASIS_X, 1.0F);
        entityData.define(DATA_ORBIT_BASIS_Y, 0.0F);
        entityData.define(DATA_ORBIT_BASIS_Z, 0.0F);
        entityData.define(DATA_ORBIT_RADIUS, 1.0F);
        entityData.define(DATA_ORBIT_SPEED, 0.0F);
        entityData.define(DATA_ORBIT_ANGLE, 0.0F);
    }

    public void initializeOrbit(Vec3 center, Vec3 axis, double radius, double angularSpeed) {
        Vec3 normalizedAxis = axis.lengthSqr() < 1.0E-8D
            ? new Vec3(0.0D, 1.0D, 0.0D) : axis.normalize();
        Vec3 radial = position().subtract(center);
        radial = radial.subtract(normalizedAxis.scale(radial.dot(normalizedAxis)));
        if (radial.lengthSqr() < 1.0E-8D) {
            radial = normalizedAxis.cross(new Vec3(1.0D, 0.0D, 0.0D));
            if (radial.lengthSqr() < 1.0E-8D) {
                radial = normalizedAxis.cross(new Vec3(0.0D, 0.0D, 1.0D));
            }
        }
        radial = radial.normalize();
        entityData.set(DATA_ORBIT_CENTER_X, (float) center.x);
        entityData.set(DATA_ORBIT_CENTER_Y, (float) center.y);
        entityData.set(DATA_ORBIT_CENTER_Z, (float) center.z);
        entityData.set(DATA_ORBIT_AXIS_X, (float) normalizedAxis.x);
        entityData.set(DATA_ORBIT_AXIS_Y, (float) normalizedAxis.y);
        entityData.set(DATA_ORBIT_AXIS_Z, (float) normalizedAxis.z);
        entityData.set(DATA_ORBIT_BASIS_X, (float) radial.x);
        entityData.set(DATA_ORBIT_BASIS_Y, (float) radial.y);
        entityData.set(DATA_ORBIT_BASIS_Z, (float) radial.z);
        entityData.set(DATA_ORBIT_RADIUS, (float) Math.max(0.1D, radius));
        entityData.set(DATA_ORBIT_SPEED, (float) angularSpeed);
        entityData.set(DATA_ORBIT_ANGLE, 0.0F);
        entityData.set(DATA_ORBITING, true);
        setDeltaMovement(normalizedAxis.cross(radial).scale(radius * angularSpeed));
    }

    public void setVelocity(Vec3 velocity) {
        Vec3 fasterVelocity = velocity.scale(SPEED_MULTIPLIER);
        setDeltaMovement(fasterVelocity);
        alignToVelocity(fasterVelocity);
    }

    public void setDamage(float damage) {
        this.damage = Math.max(0.0F, damage);
    }

    public float getDamage() {
        return damage;
    }

    @Override
    public void tick() {
        super.tick();

        if (tickCount >= MAX_LIFETIME_TICKS) {
            discard();
            return;
        }

        Vec3 velocity;
        if (entityData.get(DATA_ORBITING)) {
            velocity = advanceOrbit();
        } else {
            velocity = getDeltaMovement();
        }
        if (velocity.lengthSqr() > 1.0E-8D) {
            alignToVelocity(velocity);
        }

        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS && !ForgeEventFactory.onProjectileImpact(this, hit)) {
            onHit(hit);
        }

        if (isRemoved()) {
            return;
        }

        move(MoverType.SELF, velocity);
        checkInsideBlocks();

        if (!isRemoved() && level() instanceof ServerLevel serverLevel
            && velocity.lengthSqr() > 1.0E-8D) {
            Vec3 trailPosition = position().subtract(velocity.normalize().scale(0.45D));
            serverLevel.sendParticles(TRAIL_DUST, trailPosition.x, trailPosition.y, trailPosition.z,
                2, 0.08D, 0.08D, 0.08D, 0.01D);
        }
    }

    private Vec3 advanceOrbit() {
        Vec3 center = new Vec3(entityData.get(DATA_ORBIT_CENTER_X),
            entityData.get(DATA_ORBIT_CENTER_Y), entityData.get(DATA_ORBIT_CENTER_Z));
        Vec3 axis = new Vec3(entityData.get(DATA_ORBIT_AXIS_X), entityData.get(DATA_ORBIT_AXIS_Y),
            entityData.get(DATA_ORBIT_AXIS_Z)).normalize();
        Vec3 basis = new Vec3(entityData.get(DATA_ORBIT_BASIS_X), entityData.get(DATA_ORBIT_BASIS_Y),
            entityData.get(DATA_ORBIT_BASIS_Z)).normalize();
        double angle = entityData.get(DATA_ORBIT_ANGLE) + entityData.get(DATA_ORBIT_SPEED);
        entityData.set(DATA_ORBIT_ANGLE, (float) angle);
        Vec3 perpendicular = axis.cross(basis).normalize();
        Vec3 radial = basis.scale(Math.cos(angle)).add(perpendicular.scale(Math.sin(angle)));
        Vec3 next = center.add(radial.scale(entityData.get(DATA_ORBIT_RADIUS)));
        Vec3 velocity = next.subtract(position());
        setDeltaMovement(velocity);
        return velocity;
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        Entity owner = getOwner();
        if (owner != null && (target == owner
            || owner.isPassengerOfSameVehicle(target)
            || target.isPassengerOfSameVehicle(owner))) {
            return false;
        }
        return super.canHitEntity(target);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level().isClientSide) {
            return;
        }

        Entity target = result.getEntity();
        Entity owner = getOwner();
        if (target == owner) {
            return;
        }

        if (target instanceof LivingEntity livingTarget && owner instanceof ServerPlayer serverPlayer) {
            Damager.hurt(serverPlayer, livingTarget, damage, true);
        } else if (target instanceof LivingEntity livingTarget && owner instanceof LivingEntity livingOwner) {
            livingTarget.hurt(livingTarget.damageSources().mobProjectile(this, livingOwner), damage);
        } else {
            target.hurt(target.damageSources().generic(), damage);
        }

        discard();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            discard();
        }
    }

    private void alignToVelocity(Vec3 velocity) {
        double horizontal = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        if (horizontal < 1.0E-6D && Math.abs(velocity.y) < 1.0E-6D) {
            return;
        }

        float yaw = (float) (Mth.atan2(velocity.x, velocity.z) * (180.0D / Math.PI));
        float pitch = (float) (Mth.atan2(velocity.y, horizontal) * (180.0D / Math.PI));
        setYRot(yaw);
        setXRot(pitch);
        yRotO = yaw;
        xRotO = pitch;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Damage")) {
            damage = Math.max(0.0F, tag.getFloat("Damage"));
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
