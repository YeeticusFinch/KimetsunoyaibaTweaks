package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.network.NetworkHooks;

public class SpineEntity extends Projectile {
    private static final EntityDataAccessor<Integer> DATA_COLOR =
        SynchedEntityData.defineId(SpineEntity.class, EntityDataSerializers.INT);

    private static final int STUCK_TICKS_MAX = 100;

    private float damage = 4.0F;
    private boolean stuckInBlock = false;
    private int stuckTicks = 0;

    public SpineEntity(EntityType<? extends SpineEntity> type, Level level) {
        super(type, level);
        this.noPhysics = false;
        this.setNoGravity(true);
    }

    public static SpineEntity create(Level level, LivingEntity owner, Vec3 spawnPos, Vec3 velocity, float damage, int color) {
        SpineEntity spine = new SpineEntity(ModEntities.SPINE.get(), level);
        spine.setOwner(owner);
        spine.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        spine.setDeltaMovement(velocity);
        spine.setDamage(damage);
        spine.setTintColor(color);
        spine.alignToVelocity(velocity);
        return spine;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_COLOR, 0xFFFFFF);
    }

    public void setTintColor(int color) {
        this.entityData.set(DATA_COLOR, color & 0xFFFFFF);
    }

    public int getTintColor() {
        return this.entityData.get(DATA_COLOR);
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

        if (stuckInBlock) {
            this.setDeltaMovement(Vec3.ZERO);
            if (!this.level().isClientSide && ++stuckTicks >= STUCK_TICKS_MAX) {
                this.discard();
            }
            return;
        }

        Vec3 velocity = this.getDeltaMovement();
        if (velocity.lengthSqr() > 1.0E-8D) {
            alignToVelocity(velocity);
        }

        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS && !ForgeEventFactory.onProjectileImpact(this, hit)) {
            this.onHit(hit);
        }

        if (this.isRemoved()) {
            return;
        }

        this.move(MoverType.SELF, this.getDeltaMovement());
        this.checkInsideBlocks();
    }

    private void alignToVelocity(Vec3 velocity) {
        double horizontal = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        if (horizontal < 1.0E-6D && Math.abs(velocity.y) < 1.0E-6D) {
            return;
        }

        float yaw = (float) (Mth.atan2(velocity.x, velocity.z) * (180.0D / Math.PI));
        float pitch = (float) (Mth.atan2(velocity.y, horizontal) * (180.0D / Math.PI));

        this.setYRot(yaw);
        this.setXRot(pitch);
        this.yRotO = yaw;
        this.xRotO = pitch;
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        Entity owner = this.getOwner();
        if (owner != null && (target == owner || owner.isPassengerOfSameVehicle(target) || target.isPassengerOfSameVehicle(owner))) {
            return false;
        }
        return super.canHitEntity(target);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level().isClientSide) {
            return;
        }

        Entity owner = this.getOwner();
        Entity hitEntity = result.getEntity();

        if (hitEntity instanceof LivingEntity target && owner instanceof ServerPlayer serverPlayer) {
            Damager.hurt(serverPlayer, target, this.damage, true);
        } else if (hitEntity instanceof LivingEntity target && owner instanceof LivingEntity livingOwner) {
            target.hurt(target.damageSources().mobProjectile(this, livingOwner), this.damage);
        } else if (hitEntity instanceof LivingEntity target) {
            target.hurt(target.damageSources().generic(), this.damage);
        }

        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (this.level().isClientSide) {
            return;
        }

        this.stuckInBlock = true;
        this.stuckTicks = 0;
        this.setDeltaMovement(Vec3.ZERO);
        Vec3 pos = result.getLocation();
        this.setPos(pos.x, pos.y, pos.z);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("TintColor", getTintColor());
        tag.putFloat("Damage", this.damage);
        tag.putBoolean("StuckInBlock", this.stuckInBlock);
        tag.putInt("StuckTicks", this.stuckTicks);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("TintColor")) {
            setTintColor(tag.getInt("TintColor"));
        }
        if (tag.contains("Damage")) {
            this.damage = tag.getFloat("Damage");
        }
        this.stuckInBlock = tag.getBoolean("StuckInBlock");
        this.stuckTicks = tag.getInt("StuckTicks");
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
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 16384.0D;
    }
}
