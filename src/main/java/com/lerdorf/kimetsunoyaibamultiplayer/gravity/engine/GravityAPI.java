package com.lerdorf.kimetsunoyaibamultiplayer.gravity.engine;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public final class GravityAPI {
    private GravityAPI() {
    }

    public static Direction getGravityDirection(Entity entity) {
        return getGravityCapability(entity).map(GravityCapabilityImpl::getCurrentGravityDirection).orElse(Direction.DOWN);
    }

    public static Direction getBaseGravityDirection(Entity entity) {
        return getGravityCapability(entity).map(GravityCapabilityImpl::getBaseGravityDirection).orElse(Direction.DOWN);
    }

    public static double getGravityStrength(Entity entity) {
        return getGravityCapability(entity).map(GravityCapabilityImpl::getCurrentGravityStrength).orElse(1.0D);
    }

    public static double getBaseGravityStrength(Entity entity) {
        return getGravityCapability(entity).map(GravityCapabilityImpl::getBaseGravityStrength).orElse(1.0D);
    }

    public static void setBaseGravityStrength(Entity entity, double strength) {
        getGravityCapability(entity).ifPresent(cap -> cap.setBaseGravityStrength(strength));
    }

    public static void setBaseGravityDirection(Entity entity, Direction direction) {
        getGravityCapability(entity).ifPresent(cap -> cap.setBaseGravityDirection(direction));
    }

    public static void resetGravity(Entity entity) {
        getGravityCapability(entity).ifPresent(GravityCapabilityImpl::reset);
    }

    public static Optional<GravityCapabilityImpl> getGravityCapability(Entity entity) {
        return entity.getCapability(GravityCapabilityImpl.GRAVITY).resolve()
            .filter(GravityCapabilityImpl.class::isInstance)
            .map(GravityCapabilityImpl.class::cast);
    }

    public static Entity getEntityByUUID(ServerLevel level, UUID uuid) {
        return level.getEntity(uuid);
    }

    public static Entity getEntityByUUID(Level level, UUID uuid) {
        return level instanceof ServerLevel serverLevel ? serverLevel.getEntity(uuid) : null;
    }

    public static @Nullable RotationAnimation getRotationAnimation(Entity entity) {
        return getGravityCapability(entity).map(GravityCapabilityImpl::getRotationAnimation).orElse(null);
    }

    public static void instantlySetClientBaseGravityDirection(Entity entity, Direction direction) {
        getGravityCapability(entity).ifPresent(cap -> {
            cap.setBaseGravityDirection(direction);
            cap.updateGravityStatus(false);
            cap.forceApplyGravityChange();
        });
    }

    public static Vec3 getWorldVelocity(Entity entity) {
        return RotationUtil.vecPlayerToWorld(entity.getDeltaMovement(), getGravityDirection(entity));
    }

    public static void setWorldVelocity(Entity entity, Vec3 worldVelocity) {
        entity.setDeltaMovement(RotationUtil.vecWorldToPlayer(worldVelocity, getGravityDirection(entity)));
    }

    public static Vec3 getEyeOffset(Entity entity) {
        return RotationUtil.vecPlayerToWorld(0.0D, entity.getEyeHeight(), 0.0D, getGravityDirection(entity));
    }

    public static double eyeX(Entity entity) {
        return getGravityDirection(entity) == Direction.DOWN ? entity.getX() : entity.getEyePosition().x;
    }

    public static double eyeY(Entity entity) {
        return getGravityDirection(entity) == Direction.DOWN ? entity.getY() : entity.getEyePosition().y;
    }

    public static double eyeZ(Entity entity) {
        return getGravityDirection(entity) == Direction.DOWN ? entity.getZ() : entity.getEyePosition().z;
    }

    public static Vec3 deltaMovement(LivingEntity target) {
        Direction gravityDirection = getGravityDirection(target);
        return gravityDirection == Direction.DOWN
            ? target.getDeltaMovement()
            : RotationUtil.vecPlayerToWorld(target.getDeltaMovement(), gravityDirection);
    }

    private static Vec3 projectileSpawnVec(LivingEntity shooter) {
        Direction gravityDirection = getGravityDirection(shooter);
        if (gravityDirection == Direction.DOWN) {
            return new Vec3(shooter.getX(), shooter.getEyeY() - 0.1D, shooter.getZ());
        }
        return shooter.getEyePosition().subtract(RotationUtil.vecPlayerToWorld(0.0D, 0.1D, 0.0D, gravityDirection));
    }

    public static double projectileSpawnX(LivingEntity shooter) {
        return projectileSpawnVec(shooter).x;
    }

    public static double projectileSpawnY(LivingEntity shooter) {
        return projectileSpawnVec(shooter).y;
    }

    public static double projectileSpawnZ(LivingEntity shooter) {
        return projectileSpawnVec(shooter).z;
    }

    public static double rangedBodyTargetX(LivingEntity target) {
        Direction gravityDirection = getGravityDirection(target);
        if (gravityDirection == Direction.DOWN) {
            return target.getX();
        }
        return target.position().add(RotationUtil.vecPlayerToWorld(0.0D, target.getBbHeight() / 3.0D, 0.0D, gravityDirection)).x;
    }

    public static double rangedBodyTargetY(LivingEntity target, double heightScale) {
        Direction gravityDirection = getGravityDirection(target);
        if (gravityDirection == Direction.DOWN) {
            return target.getY(heightScale);
        }
        return target.position().add(RotationUtil.vecPlayerToWorld(0.0D, target.getBbHeight() / 3.0D, 0.0D, gravityDirection)).y;
    }

    public static double rangedBodyTargetZ(LivingEntity target) {
        Direction gravityDirection = getGravityDirection(target);
        if (gravityDirection == Direction.DOWN) {
            return target.getZ();
        }
        return target.position().add(RotationUtil.vecPlayerToWorld(0.0D, target.getBbHeight() / 3.0D, 0.0D, gravityDirection)).z;
    }

    private static final double RANGED_WITCH_EYE_OFFSET = 1.100000023841858D;

    public static double rangedEyeTargetX(LivingEntity target) {
        Direction gravityDirection = getGravityDirection(target);
        if (gravityDirection == Direction.DOWN) {
            return target.getX();
        }
        return target.position().add(RotationUtil.vecPlayerToWorld(0.0D, target.getEyeHeight() - RANGED_WITCH_EYE_OFFSET, 0.0D, gravityDirection)).x;
    }

    public static double rangedEyeTargetY(LivingEntity target) {
        Direction gravityDirection = getGravityDirection(target);
        if (gravityDirection == Direction.DOWN) {
            return target.getEyeY();
        }
        return target.position().add(RotationUtil.vecPlayerToWorld(0.0D, target.getEyeHeight() - RANGED_WITCH_EYE_OFFSET, 0.0D, gravityDirection)).y
            + RANGED_WITCH_EYE_OFFSET;
    }

    public static double rangedEyeTargetZ(LivingEntity target) {
        Direction gravityDirection = getGravityDirection(target);
        if (gravityDirection == Direction.DOWN) {
            return target.getZ();
        }
        return target.position().add(RotationUtil.vecPlayerToWorld(0.0D, target.getEyeHeight() - RANGED_WITCH_EYE_OFFSET, 0.0D, gravityDirection)).z;
    }

    public static double rangedSqrt(double value, LivingEntity target) {
        return getGravityDirection(target) == Direction.DOWN ? Math.sqrt(value) : Math.sqrt(Math.sqrt(value));
    }

    public static Vec3 addWithGravity(Vec3 vec, double x, double y, double z, Entity entity) {
        return vec.add(x, y * getGravityStrength(entity), z);
    }

    public static double scale(double constant, Entity entity) {
        return constant * getGravityStrength(entity);
    }

    public static float scaleF(float value, Entity entity) {
        return value * (float) getGravityStrength(entity);
    }
}
