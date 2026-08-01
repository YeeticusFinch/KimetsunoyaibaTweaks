package com.lerdorf.kimetsunoyaibamultiplayer.gravity.engine;

import com.lerdorf.kimetsunoyaibamultiplayer.config.GravityConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.mixin.gravity.EntityAccessor;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.UpdateGravityCapabilityPacket;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class GravityCapabilityImpl implements IGravityCapability {
    public static final Capability<IGravityCapability> GRAVITY = CapabilityManager.get(new CapabilityToken<>() {});

    private final Entity entity;
    private final LazyOptional<IGravityCapability> self = LazyOptional.of(() -> this);

    private Direction prevGravityDirection = Direction.DOWN;
    private double prevGravityStrength = 1.0D;
    private Direction baseGravityDirection = Direction.DOWN;
    private Direction currentGravityDirection = Direction.DOWN;
    private double baseGravityStrength = 1.0D;
    private double currentGravityStrength = 1.0D;
    private double currentEffectPriority = Double.NEGATIVE_INFINITY;
    private boolean firingUpdateEvent;
    private boolean needsSync;
    private boolean noAnimation;
    private boolean noPositionAdjust;
    private @Nullable GravityDirEffect delayedDirEffect;
    private double delayedStrengthEffect = 1.0D;
    private @Nullable RotationParameters currentRotationParameters = RotationParameters.getDefault();
    private @Nullable RotationAnimation animation;

    public GravityCapabilityImpl(Entity entity) {
        this.entity = entity;
        if (entity.level().isClientSide) {
            this.animation = new RotationAnimation();
        }
    }

    @Override
    public void tick() {
        if (!GravityConfig.enableGravityChanging) {
            if (currentGravityDirection != Direction.DOWN || currentGravityStrength != 1.0D) {
                currentGravityDirection = Direction.DOWN;
                currentGravityStrength = 1.0D;
                prevGravityDirection = Direction.DOWN;
                needsSync = true;
            }
        } else {
            updateGravityStatus(true);
        }

        applyGravityChange();
        if (!entity.level().isClientSide && needsSync) {
            syncToTracking();
        }
    }

    public void updateGravityStatus() {
        updateGravityStatus(true);
    }

    public void updateGravityStatus(boolean sendPacketIfNecessary) {
        if (shouldAcceptServerSync()) {
            return;
        }

        Direction oldDirection = currentGravityDirection;
        double oldStrength = currentGravityStrength;

        Entity vehicle = entity.getVehicle();
        if (vehicle != null) {
            currentGravityDirection = GravityAPI.getGravityDirection(vehicle);
            currentGravityStrength = GravityAPI.getGravityStrength(vehicle);
        } else {
            currentGravityDirection = baseGravityDirection;
            currentGravityStrength = baseGravityStrength * GravityConfig.gravityStrengthMultiplier;
            currentEffectPriority = Double.NEGATIVE_INFINITY;
            firingUpdateEvent = true;
            try {
                MinecraftForge.EVENT_BUS.post(new GravityEffectEvent(entity));
                if (delayedDirEffect != null) {
                    applyGravityDirectionEffect(delayedDirEffect.direction(), delayedDirEffect.rotationParameters(), delayedDirEffect.priority());
                    delayedDirEffect = null;
                }
                currentGravityStrength *= delayedStrengthEffect;
                delayedStrengthEffect = 1.0D;
            } finally {
                firingUpdateEvent = false;
            }
            if (currentEffectPriority == Double.NEGATIVE_INFINITY) {
                currentRotationParameters = RotationParameters.getDefault();
            }
        }

        if (sendPacketIfNecessary && (oldDirection != currentGravityDirection || Math.abs(oldStrength - currentGravityStrength) > 0.0001D)) {
            needsSync = true;
        }
    }

    private boolean shouldAcceptServerSync() {
        return entity.level().isClientSide && !entity.isControlledByLocalInstance();
    }

    public void applyGravityDirectionEffect(@NotNull Direction direction, @Nullable RotationParameters rotationParameters, double priority) {
        if (!GravityConfig.enableGravityChanging) {
            return;
        }
        if (firingUpdateEvent) {
            if (priority > currentEffectPriority) {
                currentEffectPriority = priority;
                currentGravityDirection = direction;
                if (rotationParameters != null) {
                    currentRotationParameters = rotationParameters;
                }
            }
            return;
        }
        if (delayedDirEffect == null || priority > delayedDirEffect.priority()) {
            delayedDirEffect = new GravityDirEffect(direction, rotationParameters, priority);
        }
    }

    public void applyGravityStrengthEffect(double strengthMultiplier) {
        if (!GravityConfig.enableGravityChanging) {
            return;
        }
        if (firingUpdateEvent) {
            currentGravityStrength *= strengthMultiplier;
        } else {
            delayedStrengthEffect *= strengthMultiplier;
        }
    }

    public void applyGravityChange() {
        if (currentRotationParameters == null) {
            currentRotationParameters = RotationParameters.getDefault();
        }
        if (prevGravityDirection != currentGravityDirection) {
            applyGravityDirectionChange(prevGravityDirection, currentGravityDirection, currentRotationParameters, false);
            prevGravityDirection = currentGravityDirection;
        }
        if (Math.abs(currentGravityStrength - prevGravityStrength) > 0.0001D) {
            prevGravityStrength = currentGravityStrength;
        }
    }

    public void applyGravityDirectionChange(Direction oldGravity, Direction newGravity, RotationParameters rotationParameters, boolean isInitialization) {
        entity.setBoundingBox(((EntityAccessor) entity).gc_makeBoundingBox());
        if (isInitialization) {
            return;
        }

        entity.fallDistance = 0.0F;

        long timeMs = entity.level().getGameTime() * 50;
        Vec3 relativeRotationCenter = getLocalRotationCenter(entity, oldGravity, newGravity);
        Vec3 oldPos = entity.position();
        Vec3 oldLastTickPos = new Vec3(entity.xOld, entity.yOld, entity.zOld);
        Vec3 rotationCenter = oldPos.add(RotationUtil.vecPlayerToWorld(relativeRotationCenter, oldGravity));
        Vec3 newPos = rotationCenter.subtract(RotationUtil.vecPlayerToWorld(relativeRotationCenter, newGravity));
        Vec3 posTranslation = newPos.subtract(oldPos);
        Vec3 newLastTickPos = oldLastTickPos.add(posTranslation);

        if (!noPositionAdjust) {
            entity.setPos(newPos);
            entity.xo = newLastTickPos.x;
            entity.yo = newLastTickPos.y;
            entity.zo = newLastTickPos.z;
            entity.xOld = newLastTickPos.x;
            entity.yOld = newLastTickPos.y;
            entity.zOld = newLastTickPos.z;
            adjustEntityPosition(oldGravity, entity.getBoundingBox());
        }

        if (entity.level().isClientSide && animation != null && !noAnimation) {
            animation.startRotationAnimation(newGravity, oldGravity, rotationParameters.rotationTimeMS(), entity, timeMs,
                rotationParameters.rotateView(), relativeRotationCenter);
        }

        Vec3 realWorldVelocity = getRealWorldVelocity(entity, oldGravity);
        if (rotationParameters.rotateVelocity()) {
            Vector3f worldSpaceVelocity = realWorldVelocity.toVector3f();
            worldSpaceVelocity.rotate(RotationUtil.getRotationBetween(oldGravity, newGravity));
            entity.setDeltaMovement(RotationUtil.vecWorldToPlayer(new Vec3(worldSpaceVelocity), newGravity));
        } else {
            entity.setDeltaMovement(RotationUtil.vecWorldToPlayer(realWorldVelocity, newGravity));
        }
        noAnimation = false;
        noPositionAdjust = false;
    }

    private static Vec3 getRealWorldVelocity(Entity entity, Direction prevGravityDirection) {
        if (entity.isControlledByLocalInstance()) {
            return new Vec3(entity.getX() - entity.xo, entity.getY() - entity.yo, entity.getZ() - entity.zo);
        }
        return RotationUtil.vecPlayerToWorld(entity.getDeltaMovement(), prevGravityDirection);
    }

    private static Vec3 getLocalRotationCenter(Entity entity, Direction oldGravity, Direction newGravity) {
        if (entity instanceof EndCrystal) {
            return new Vec3(0.0D, -0.5D, 0.0D);
        }
        EntityDimensions dimensions = entity.getDimensions(entity.getPose());
        if (newGravity.getOpposite() == oldGravity) {
            return new Vec3(0.0D, dimensions.height / 2.0D, 0.0D);
        }
        return Vec3.ZERO;
    }

    private void adjustEntityPosition(Direction oldGravity, AABB entityBoundingBox) {
        if (!GravityConfig.adjustPositionAfterChangingGravity) {
            return;
        }
        if (entity instanceof AreaEffectCloud || entity instanceof AbstractArrow || entity instanceof EndCrystal) {
            return;
        }

        Direction movingDirection = oldGravity.getOpposite();
        Iterable<VoxelShape> collisions = entity.level().getCollisions(entity, entityBoundingBox.inflate(-0.01D));
        AABB totalCollisionBox = null;
        for (VoxelShape collision : collisions) {
            if (!collision.isEmpty()) {
                AABB box = collision.bounds();
                totalCollisionBox = totalCollisionBox == null ? box : totalCollisionBox.minmax(box);
            }
        }
        if (totalCollisionBox != null) {
            Vec3 positionAdjustmentOffset = getPositionAdjustmentOffset(entityBoundingBox, totalCollisionBox, movingDirection);
            entity.setPos(entity.position().add(positionAdjustmentOffset));
        }
    }

    private static Vec3 getPositionAdjustmentOffset(AABB entityBoundingBox, AABB nearbyCollisionUnion, Direction movingDirection) {
        Direction.Axis axis = movingDirection.getAxis();
        double offset = 0.0D;
        if (movingDirection.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            double pushing = nearbyCollisionUnion.max(axis);
            double pushed = entityBoundingBox.min(axis);
            if (pushing > pushed) {
                offset = pushing - pushed;
            }
        } else {
            double pushing = nearbyCollisionUnion.min(axis);
            double pushed = entityBoundingBox.max(axis);
            if (pushing < pushed) {
                offset = pushed - pushing;
            }
        }
        return new Vec3(movingDirection.step()).scale(offset);
    }

    private void syncToTracking() {
        ModNetworking.sendToTrackingAndSelf(new UpdateGravityCapabilityPacket(
            noAnimation,
            entity.getUUID(),
            baseGravityDirection,
            currentGravityDirection,
            baseGravityStrength,
            currentGravityStrength
        ), entity);
        needsSync = false;
        noAnimation = false;
    }

    @Override
    public void sync(boolean noAnimation, Direction baseGravityDirection, Direction currentGravityDirection,
                     double baseGravityStrength, double currentGravityStrength) {
        this.noAnimation = noAnimation;
        this.baseGravityDirection = baseGravityDirection;
        this.currentGravityDirection = currentGravityDirection;
        this.baseGravityStrength = baseGravityStrength;
        this.currentGravityStrength = currentGravityStrength;
        this.needsSync = false;
        if (noAnimation) {
            this.prevGravityDirection = currentGravityDirection;
            this.prevGravityStrength = currentGravityStrength;
            entity.setBoundingBox(((EntityAccessor) entity).gc_makeBoundingBox());
        }
    }

    public Direction getCurrentGravityDirection() {
        return GravityConfig.enableGravityChanging ? currentGravityDirection : Direction.DOWN;
    }

    public Direction getCurrGravityDirection() {
        return getCurrentGravityDirection();
    }

    public Direction getBaseGravityDirection() {
        return GravityConfig.enableGravityChanging ? baseGravityDirection : Direction.DOWN;
    }

    public double getCurrentGravityStrength() {
        return GravityConfig.enableGravityChanging ? currentGravityStrength : 1.0D;
    }

    public double getCurrGravityStrength() {
        return getCurrentGravityStrength();
    }

    public Direction getPrevGravityDirection() {
        return GravityConfig.enableGravityChanging ? prevGravityDirection : Direction.DOWN;
    }

    public double getBaseGravityStrength() {
        return GravityConfig.enableGravityChanging ? baseGravityStrength : 1.0D;
    }

    public void setBaseGravityDirection(Direction direction) {
        if (!GravityConfig.enableGravityChanging || direction == null) {
            return;
        }
        if (baseGravityDirection != direction) {
            baseGravityDirection = direction;
            updateGravityStatus(false);
            needsSync = true;
        }
    }

    public void setBaseGravityStrength(double strength) {
        if (!GravityConfig.enableGravityChanging) {
            return;
        }
        baseGravityStrength = Math.max(0.0D, strength);
        updateGravityStatus(false);
        needsSync = true;
    }

    public void reset() {
        baseGravityDirection = Direction.DOWN;
        currentGravityDirection = Direction.DOWN;
        baseGravityStrength = 1.0D;
        currentGravityStrength = 1.0D;
        prevGravityDirection = Direction.DOWN;
        prevGravityStrength = 1.0D;
        delayedDirEffect = null;
        delayedStrengthEffect = 1.0D;
        needsSync = true;
        noAnimation = true;
    }

    public @Nullable RotationAnimation getRotationAnimation() {
        return animation;
    }

    public void forceApplyGravityChange() {
        prevGravityDirection = currentGravityDirection;
        prevGravityStrength = currentGravityStrength;
        entity.setBoundingBox(((EntityAccessor) entity).gc_makeBoundingBox());
    }

    public @Nullable RotationParameters getCurrentRotationParameters() {
        return currentRotationParameters;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("baseGravityDirection", baseGravityDirection.getName());
        tag.putString("currentGravityDirection", currentGravityDirection.getName());
        tag.putDouble("baseGravityStrength", baseGravityStrength);
        tag.putDouble("currentGravityStrength", currentGravityStrength);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        baseGravityDirection = readDirection(tag, "baseGravityDirection", Direction.DOWN);
        currentGravityDirection = readDirection(tag, "currentGravityDirection", baseGravityDirection);
        baseGravityStrength = tag.contains("baseGravityStrength") ? tag.getDouble("baseGravityStrength") : 1.0D;
        currentGravityStrength = tag.contains("currentGravityStrength") ? tag.getDouble("currentGravityStrength") : baseGravityStrength;
        prevGravityDirection = currentGravityDirection;
        needsSync = true;
        noAnimation = true;
    }

    private static Direction readDirection(CompoundTag tag, String key, Direction fallback) {
        if (!tag.contains(key)) {
            return fallback;
        }
        Direction direction = Direction.byName(tag.getString(key));
        return direction == null ? fallback : direction;
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return GRAVITY.orEmpty(cap, self);
    }

    public void invalidate() {
        self.invalidate();
    }

    private record GravityDirEffect(@NotNull Direction direction, @Nullable RotationParameters rotationParameters, double priority) {
    }
}
