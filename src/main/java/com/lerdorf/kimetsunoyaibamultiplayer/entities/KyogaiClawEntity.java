package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** A short-lived, gravity-aware claw projectile created by Kyogai's navel drum. */
public class KyogaiClawEntity extends Mob implements GeoEntity {
    private static final double SURFACE_SEARCH_DISTANCE = 30.0D;
    private static final double SURFACE_OFFSET = 0.15D;
    private static final double CLIMB_SPEED = 1.725D;
    private static final EntityDataAccessor<Float> DATA_DIRECTION_X =
        SynchedEntityData.defineId(KyogaiClawEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DIRECTION_Y =
        SynchedEntityData.defineId(KyogaiClawEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DIRECTION_Z =
        SynchedEntityData.defineId(KyogaiClawEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
        SynchedEntityData.defineId(KyogaiClawEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> DATA_GRAVITY_DIRECTION =
        SynchedEntityData.defineId(KyogaiClawEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SURFACE_DIRECTION =
        SynchedEntityData.defineId(KyogaiClawEntity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Set<UUID> damagedEntities = new HashSet<>();
    private Vec3 serverTravelDirection = Vec3.ZERO;
    private int lifetime;
    private BlockPos lastDustBlock;
    private boolean climbing;

    public KyogaiClawEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoAi(true);
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.setSilent(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 1.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    public static KyogaiClawEntity create(Level level, Vec3 position, Vec3 direction, LivingEntity owner,
                                          Direction gravityDirection) {
        KyogaiClawEntity claw = new KyogaiClawEntity(ModEntities.KYOGAI_CLAW.get(), level);
        claw.setPos(position.x, position.y, position.z);
        claw.setOwner(owner.getUUID());
        claw.setGravityDirection(gravityDirection);
        claw.setTravelDirection(direction);
        claw.setOldPosAndRot();
        return claw;
    }

    private void setTravelDirection(Vec3 direction) {
        if (direction.lengthSqr() < 1.0E-6D) {
            direction = new Vec3(0.0D, 0.0D, 1.0D);
        }
        direction = direction.normalize();
        this.serverTravelDirection = direction;
        this.entityData.set(DATA_DIRECTION_X, (float) direction.x);
        this.entityData.set(DATA_DIRECTION_Y, (float) direction.y);
        this.entityData.set(DATA_DIRECTION_Z, (float) direction.z);
        this.setDeltaMovement(Vec3.ZERO);
    }

    public Vec3 getTravelDirection() {
        if (!level().isClientSide && serverTravelDirection.lengthSqr() > 1.0E-6D) {
            return serverTravelDirection;
        }
        return new Vec3(
            this.entityData.get(DATA_DIRECTION_X),
            this.entityData.get(DATA_DIRECTION_Y),
            this.entityData.get(DATA_DIRECTION_Z));
    }

    private void setGravityDirection(Direction direction) {
        this.entityData.set(DATA_GRAVITY_DIRECTION, direction.get3DDataValue());
    }

    public Direction getGravityDirection() {
        return Direction.from3DDataValue(this.entityData.get(DATA_GRAVITY_DIRECTION));
    }

    private Vec3 gravityDown() {
        return Vec3.atLowerCornerOf(getGravityDirection().getNormal());
    }

    private Vec3 gravityUp() {
        return gravityDown().scale(-1.0D);
    }

    private void setOwner(UUID owner) {
        this.entityData.set(DATA_OWNER, Optional.ofNullable(owner));
    }

    private UUID getOwnerUuid() {
        return this.entityData.get(DATA_OWNER).orElse(null);
    }

    private LivingEntity getOwner() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        UUID ownerUuid = getOwnerUuid();
        return ownerUuid != null && serverLevel.getEntity(ownerUuid) instanceof LivingEntity owner ? owner : null;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_DIRECTION_X, 0.0F);
        this.entityData.define(DATA_DIRECTION_Y, 0.0F);
        this.entityData.define(DATA_DIRECTION_Z, 1.0F);
        this.entityData.define(DATA_OWNER, Optional.empty());
        this.entityData.define(DATA_GRAVITY_DIRECTION, Direction.DOWN.get3DDataValue());
        this.entityData.define(DATA_SURFACE_DIRECTION, Direction.UP.get3DDataValue());
    }

    @Override
    public void registerControllers(software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "claw_spin", 0,
            state -> state.setAndContinue(RawAnimation.begin().thenLoop("claw_spin"))));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void tick() {
        this.baseTick();
        this.setNoGravity(true);

        Vec3 current = position();
        Vec3 next = advanceOneStep();
        Vec3 movement = next.subtract(current);
        if (movement.lengthSqr() > 1.0E-8D) {
            // This entity is deliberately no-physics, so update its position
            // directly instead of relying on Mob collision movement.
            setPos(next.x, next.y, next.z);
            setDeltaMovement(movement);
            hasImpulse = true;
        }
        if (level().isClientSide) {
            return;
        }
        damageGroundedEntities();
        if (++lifetime >= 35) {
            discard();
        }
    }

    private Vec3 advanceOneStep() {
        Vec3 down = gravityDown();
        Vec3 up = down.scale(-1.0D);
        Vec3 current = position();

        if (climbing) {
            return climbOneStep(current, down, up);
        }

        Vec3 travel = getTravelDirection();
        if (travel.lengthSqr() < 1.0E-6D) {
            travel = new Vec3(0.0D, 0.0D, 1.0D);
        }
        Vec3 forward = travel.normalize().scale(CLIMB_SPEED);
        Vec3 candidate = current.add(forward);
        BlockHitResult obstacle = traceMovement(current, candidate);
        if (obstacle != null) {
            SurfaceSample wall = surfaceFromObstacle(obstacle);
            climbing = true;
            setSurfaceDirection(wall.faceDirection);
            emitSurfaceDust(wall);
            return climbOneStep(wall.position, down, up);
        }

        SurfaceSample nextSupport = findSurface(candidate, down);
        if (nextSupport != null) {
            setSurfaceDirection(nextSupport.faceDirection);
            emitSurfaceDust(nextSupport);
            return nextSupport.position;
        }

        // Keep the projectile moving even when the next support ray misses.
        // The next tick will reacquire the face or continue falling.
        return candidate.add(down.scale(CLIMB_SPEED * 0.35D));
    }

    private Vec3 climbOneStep(Vec3 current, Vec3 down, Vec3 up) {
        Vec3 candidate = current.add(up.scale(CLIMB_SPEED));
        Vec3 forward = getTravelDirection().scale(CLIMB_SPEED);
        BlockHitResult obstacle = traceMovement(candidate, candidate.add(forward));
        if (obstacle != null) {
            return candidate;
        }

        SurfaceSample top = findSurface(candidate.add(forward), down);
        if (top != null) {
            climbing = false;
            setSurfaceDirection(top.faceDirection);
            emitSurfaceDust(top);
            return top.position;
        }

        // Once the obstacle is no longer ahead, stop climbing. The normal
        // support check on the next tick handles any remaining drop.
        climbing = false;
        return candidate;
    }

    private SurfaceSample findSurface(Vec3 position, Vec3 down) {
        HitResult hit = level().clip(new ClipContext(position, position.add(down.scale(SURFACE_SEARCH_DISTANCE)),
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        BlockHitResult blockHit = (BlockHitResult) hit;
        Direction faceDirection = downDirectionToFace(down);
        return new SurfaceSample(
            blockHit.getLocation().subtract(down.scale(SURFACE_OFFSET)),
            blockHit.getBlockPos(),
            level().getBlockState(blockHit.getBlockPos()),
            faceDirection);
    }

    private BlockHitResult traceMovement(Vec3 start, Vec3 end) {
        HitResult hit = level().clip(new ClipContext(start, end,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return hit.getType() == HitResult.Type.BLOCK ? (BlockHitResult) hit : null;
    }

    private SurfaceSample surfaceFromObstacle(BlockHitResult hit) {
        Vec3 faceNormal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
        return new SurfaceSample(
            hit.getLocation().add(faceNormal.scale(SURFACE_OFFSET)),
            hit.getBlockPos(),
            level().getBlockState(hit.getBlockPos()),
            hit.getDirection());
    }

    private Direction downDirectionToFace(Vec3 down) {
        return Direction.getNearest(-down.x, -down.y, -down.z);
    }

    private void setSurfaceDirection(Direction direction) {
        this.entityData.set(DATA_SURFACE_DIRECTION, direction.get3DDataValue());
    }

    public Direction getSurfaceDirection() {
        return Direction.from3DDataValue(this.entityData.get(DATA_SURFACE_DIRECTION));
    }

    private void emitSurfaceDust(SurfaceSample surface) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        boolean enteredNewBlock = !surface.blockPos.equals(lastDustBlock);
        lastDustBlock = surface.blockPos;

        Vec3 faceNormal = Vec3.atLowerCornerOf(surface.faceDirection.getNormal());
        Vec3 facePosition = surface.position.subtract(faceNormal.scale(SURFACE_OFFSET - 0.01D));
        serverLevel.sendParticles(
            new BlockParticleOption(ParticleTypes.BLOCK, surface.blockState),
            facePosition.x, facePosition.y, facePosition.z, 4,
            faceNormal.x * 0.03D, faceNormal.y * 0.03D, faceNormal.z * 0.03D, 0.015D);
        if (enteredNewBlock) {
            var soundType = surface.blockState.getSoundType();
            serverLevel.playSound(null, facePosition.x, facePosition.y, facePosition.z,
                soundType.getBreakSound(), SoundSource.BLOCKS, soundType.getVolume(), soundType.getPitch());
        }
    }

    private void damageGroundedEntities() {
        LivingEntity owner = getOwner();
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
            getBoundingBox().inflate(0.65D), entity -> entity.isAlive() && entity != owner
                && !damagedEntities.contains(entity.getUUID()))) {
            if (Damager.hurt(owner == null ? this : owner, target, 6.0F)) {
                damagedEntities.add(target.getUUID());
            }
        }
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
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        if (getOwnerUuid() != null) {
            tag.putUUID("Owner", getOwnerUuid());
        }
        tag.putInt("GravityDirection", getGravityDirection().get3DDataValue());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            setOwner(tag.getUUID("Owner"));
        }
        if (tag.contains("GravityDirection")) {
            setGravityDirection(Direction.from3DDataValue(tag.getInt("GravityDirection")));
        }
    }

    private record SurfaceSample(Vec3 position, BlockPos blockPos, BlockState blockState,
                                 Direction faceDirection) {
    }
}
