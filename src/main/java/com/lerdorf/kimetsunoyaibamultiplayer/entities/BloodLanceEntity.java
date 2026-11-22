package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
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

/** Server-authoritative blood lance projectile used by Muzan's Blood Demon Arts. */
public final class BloodLanceEntity extends Projectile {
    private static final float DEFAULT_DAMAGE = 4.0F;

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
    }

    public void setVelocity(Vec3 velocity) {
        setDeltaMovement(velocity);
        alignToVelocity(velocity);
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

        Vec3 velocity = getDeltaMovement();
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
