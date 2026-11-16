package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.GravityDirectionHelper;
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

/** Server-authoritative animated tornado used by Merciless Punishment. */
public final class BloodTornadoEntity extends Mob implements GeoEntity {
    private static final double SPEED = 1.0D;
    private static final double GRAVITY_ACCELERATION = 0.08D;
    private static final float DAMAGE = 8.0F;
    private static final double HORIZONTAL_KNOCKBACK = 0.45D;
    private static final double UPWARD_KNOCKBACK = 0.65D;

    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
        SynchedEntityData.defineId(BloodTornadoEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Float> DATA_DIRECTION_X =
        SynchedEntityData.defineId(BloodTornadoEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DIRECTION_Y =
        SynchedEntityData.defineId(BloodTornadoEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DIRECTION_Z =
        SynchedEntityData.defineId(BloodTornadoEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_UP_X =
        SynchedEntityData.defineId(BloodTornadoEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_UP_Y =
        SynchedEntityData.defineId(BloodTornadoEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_UP_Z =
        SynchedEntityData.defineId(BloodTornadoEntity.class, EntityDataSerializers.FLOAT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private Vec3 direction = Vec3.ZERO;
    private Vec3 gravityUp = new Vec3(0.0D, 1.0D, 0.0D);
    private int ticksAlive;
    private int lifetime = 21;

    public BloodTornadoEntity(EntityType<? extends BloodTornadoEntity> type, Level level) {
        super(type, level);
        this.noPhysics = false;
        this.setInvulnerable(true);
        this.setNoAi(true);
        this.setNoGravity(false);
        this.setSilent(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 1.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    public void initialize(Vec3 position, Vec3 direction, LivingEntity owner, int lifetime) {
        setPos(position.x, position.y, position.z);
        setGravityUp(owner == null ? new Vec3(0.0D, 1.0D, 0.0D)
            : GravityDirectionHelper.localToWorld(owner, new Vec3(0.0D, 1.0D, 0.0D)).normalize());
        setDirection(direction);
        setOwner(owner == null ? null : owner.getUUID());
        this.lifetime = Math.max(1, lifetime);
        setOldPosAndRot();
    }

    private void setOwner(UUID ownerId) {
        entityData.set(DATA_OWNER, Optional.ofNullable(ownerId));
    }

    private void setDirection(Vec3 direction) {
        Vec3 tangent = direction.subtract(gravityUp.scale(direction.dot(gravityUp)));
        this.direction = tangent.lengthSqr() < 1.0E-8D ? Vec3.ZERO : tangent.normalize();
        entityData.set(DATA_DIRECTION_X, (float) this.direction.x);
        entityData.set(DATA_DIRECTION_Y, (float) this.direction.y);
        entityData.set(DATA_DIRECTION_Z, (float) this.direction.z);
    }

    private void setGravityUp(Vec3 up) {
        gravityUp = up.lengthSqr() < 1.0E-8D ? new Vec3(0.0D, 1.0D, 0.0D) : up.normalize();
        entityData.set(DATA_UP_X, (float) gravityUp.x);
        entityData.set(DATA_UP_Y, (float) gravityUp.y);
        entityData.set(DATA_UP_Z, (float) gravityUp.z);
    }

    private Vec3 getTravelDirection() {
        if (!level().isClientSide()) {
            return direction;
        }
        return new Vec3(entityData.get(DATA_DIRECTION_X), entityData.get(DATA_DIRECTION_Y),
            entityData.get(DATA_DIRECTION_Z));
    }

    private Vec3 getGravityUp() {
        if (!level().isClientSide()) {
            return gravityUp;
        }
        return new Vec3(entityData.get(DATA_UP_X), entityData.get(DATA_UP_Y),
            entityData.get(DATA_UP_Z)).normalize();
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
        entityData.define(DATA_DIRECTION_Y, 0.0F);
        entityData.define(DATA_DIRECTION_Z, 0.0F);
        entityData.define(DATA_UP_X, 0.0F);
        entityData.define(DATA_UP_Y, 1.0F);
        entityData.define(DATA_UP_Z, 0.0F);
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
        setNoGravity(false);

        int age = ticksAlive++;
        if (age >= lifetime) {
            discard();
            return;
        }

        Vec3 up = getGravityUp();
        Vec3 currentVelocity = getDeltaMovement();
        double normalVelocity = currentVelocity.dot(up);
        if (isSupported(up) && normalVelocity <= 0.0D) {
            normalVelocity = 0.0D;
        } else {
            normalVelocity -= GRAVITY_ACCELERATION;
        }
        Vec3 movement = getTravelDirection().scale(SPEED).add(up.scale(normalVelocity));
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

        AABB damageBox = getBoundingBox().inflate(0.5D);
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
        Vec3 horizontal = away.subtract(gravityUp.scale(away.dot(gravityUp)));
        if (horizontal.lengthSqr() < 1.0E-8D) {
            horizontal = direction.scale(-1.0D);
        } else {
            horizontal = horizontal.normalize();
        }
        Vec3 current = target.getDeltaMovement();
        Vec3 retained = current.scale(0.35D);
        Vec3 horizontalVelocity = horizontal.scale(HORIZONTAL_KNOCKBACK);
        Vec3 upward = gravityUp.scale(UPWARD_KNOCKBACK);
        target.setDeltaMovement(
            retained.x + horizontalVelocity.x + upward.x,
            retained.y + horizontalVelocity.y + upward.y,
            retained.z + horizontalVelocity.z + upward.z);
        target.hasImpulse = true;
    }

    private boolean isSupported(Vec3 up) {
        Vec3 start = position().add(up.scale(0.25D));
        Vec3 end = position().subtract(up.scale(0.5D));
        net.minecraft.world.level.ClipContext context = new net.minecraft.world.level.ClipContext(
            start, end, net.minecraft.world.level.ClipContext.Block.COLLIDER,
            net.minecraft.world.level.ClipContext.Fluid.NONE, this);
        return level().clip(context).getType() == net.minecraft.world.phys.HitResult.Type.BLOCK;
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
        setGravityUp(new Vec3(tag.getDouble("UpX"), tag.getDouble("UpY"), tag.getDouble("UpZ")));
        setDirection(new Vec3(tag.getDouble("DirectionX"), tag.getDouble("DirectionY"),
            tag.getDouble("DirectionZ")));
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
        tag.putDouble("DirectionY", direction.y);
        tag.putDouble("DirectionZ", direction.z);
        tag.putDouble("UpX", gravityUp.x);
        tag.putDouble("UpY", gravityUp.y);
        tag.putDouble("UpZ", gravityUp.z);
    }
}
