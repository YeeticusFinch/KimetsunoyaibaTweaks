package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.ModParticles;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Optional;
import java.util.UUID;

/** Server-authoritative animated tornado used by Ultimate Authority. */
public final class BloodTornadoEntity extends Mob implements GeoEntity {
    private static final double SPEED = 2.0D;
    private static final float DAMAGE = 8.0F;
    private static final double HORIZONTAL_KNOCKBACK = 0.45D;
    private static final double UPWARD_KNOCKBACK = 0.65D;

    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
        SynchedEntityData.defineId(BloodTornadoEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Float> DATA_DIRECTION_X =
        SynchedEntityData.defineId(BloodTornadoEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DIRECTION_Z =
        SynchedEntityData.defineId(BloodTornadoEntity.class, EntityDataSerializers.FLOAT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private Vec3 direction = Vec3.ZERO;
    private int ticksAlive;
    private int lifetime = 21;

    public BloodTornadoEntity(EntityType<? extends BloodTornadoEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setInvulnerable(true);
        this.setNoAi(true);
        this.setNoGravity(true);
        this.setSilent(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 1.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    public void initialize(Vec3 position, Vec3 direction, LivingEntity owner, int lifetime) {
        setPos(position.x, position.y, position.z);
        Vec3 groundDirection = new Vec3(direction.x, 0.0D, direction.z);
        setDirection(groundDirection.lengthSqr() < 1.0E-8D
            ? Vec3.ZERO
            : groundDirection.normalize());
        setOwner(owner == null ? null : owner.getUUID());
        this.lifetime = Math.max(1, lifetime);
        setOldPosAndRot();
    }

    private void setOwner(UUID ownerId) {
        entityData.set(DATA_OWNER, Optional.ofNullable(ownerId));
    }

    private void setDirection(Vec3 direction) {
        this.direction = direction;
        entityData.set(DATA_DIRECTION_X, (float) direction.x);
        entityData.set(DATA_DIRECTION_Z, (float) direction.z);
    }

    private Vec3 getTravelDirection() {
        if (!level().isClientSide()) {
            return direction;
        }
        return new Vec3(entityData.get(DATA_DIRECTION_X), 0.0D, entityData.get(DATA_DIRECTION_Z));
    }

    private LivingEntity getOwner() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        UUID ownerId = entityData.get(DATA_OWNER).orElse(null);
        return ownerId != null && serverLevel.getEntity(ownerId) instanceof LivingEntity owner
            ? owner
            : null;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_OWNER, Optional.empty());
        entityData.define(DATA_DIRECTION_X, 0.0F);
        entityData.define(DATA_DIRECTION_Z, 0.0F);
    }

    @Override
    public void registerControllers(software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "spin", 0,
            state -> state.setAndContinue(RawAnimation.begin().thenLoop("spin"))));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void tick() {
        baseTick();
        setNoGravity(true);

        int age = ticksAlive++;
        if (age >= lifetime) {
            discard();
            return;
        }

        Vec3 movement = getTravelDirection().scale(SPEED);
        setDeltaMovement(movement);
        move(MoverType.SELF, movement);
        hasImpulse = true;

        if (level().isClientSide()) {
            return;
        }

        ServerLevel serverLevel = (ServerLevel) level();
        spawnImpact(serverLevel, position());
        if (age % 2 == 0) {
            damageNearby(serverLevel);
        }
        if (age % 8 == 0) {
            serverLevel.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE,
                SoundSource.HOSTILE, 1.0F, 0.75F);
        }

    }

    private void damageNearby(ServerLevel serverLevel) {
        LivingEntity owner = getOwner();
        if (owner == null) {
            return;
        }

        AABB damageBox = new AABB(
            getX() - 1.5D, getY(), getZ() - 1.5D,
            getX() + 1.5D, getY() + 4.0D, getZ() + 1.5D);
        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, damageBox,
            candidate -> candidate.isAlive() && !candidate.isSpectator()
                && !candidate.getUUID().equals(owner.getUUID()))) {
            if (Damager.hurt(owner, target, DAMAGE, true)) {
                spawnImpact(serverLevel, target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D));
                launchTarget(target);
            }
        }
    }

    private void launchTarget(LivingEntity target) {
        Vec3 away = target.position().subtract(position());
        Vec3 horizontal = new Vec3(away.x, 0.0D, away.z);
        if (horizontal.lengthSqr() < 1.0E-8D) {
            horizontal = direction.scale(-1.0D);
        } else {
            horizontal = horizontal.normalize();
        }
        Vec3 current = target.getDeltaMovement();
        target.setDeltaMovement(
            current.x * 0.35D + horizontal.x * HORIZONTAL_KNOCKBACK,
            Math.max(current.y, UPWARD_KNOCKBACK),
            current.z * 0.35D + horizontal.z * HORIZONTAL_KNOCKBACK);
        target.hasImpulse = true;
    }

    private static void spawnImpact(ServerLevel level, Vec3 position) {
        level.sendParticles(ModParticles.BLACKRED_IMPACT.get(),
            position.x, position.y, position.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ticksAlive = tag.getInt("TicksAlive");
        lifetime = Math.max(1, tag.getInt("Lifetime"));
        if (tag.hasUUID("Owner")) {
            setOwner(tag.getUUID("Owner"));
        }
        setDirection(new Vec3(tag.getDouble("DirectionX"), 0.0D, tag.getDouble("DirectionZ")));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("TicksAlive", ticksAlive);
        tag.putInt("Lifetime", lifetime);
        UUID ownerId = entityData.get(DATA_OWNER).orElse(null);
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        tag.putDouble("DirectionX", direction.x);
        tag.putDouble("DirectionZ", direction.z);
    }
}
