package com.lerdorf.kimetsunoyaibamultiplayer.gravity.api;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class KNYGravity {
    /** Persistent data tag marking entities that KNY gravity fields should affect. */
    public static final String GRAVITY_AFFECTED_TAG = "knymp_gravity_affected";

    private KNYGravity() {
    }

    public static boolean isEnabled() {
        return com.lerdorf.kimetsunoyaibamultiplayer.compat.GravityApiCompat.isAvailable();
    }

    public static Direction getGravityDirection(Entity entity) {
        return isEnabled() ? com.lerdorf.kimetsunoyaibamultiplayer.compat.GravityApiCompat.getGravityDirection(entity) : Direction.DOWN;
    }

    public static Direction getBaseGravityDirection(Entity entity) {
        return isEnabled() ? com.lerdorf.kimetsunoyaibamultiplayer.compat.GravityApiCompat.getBaseGravityDirection(entity) : Direction.DOWN;
    }

    public static double getGravityStrength(Entity entity) {
        return isEnabled() ? com.lerdorf.kimetsunoyaibamultiplayer.compat.GravityApiCompat.getGravityStrength(entity) : 1.0D;
    }

    public static double getBaseGravityStrength(Entity entity) {
        return isEnabled() ? com.lerdorf.kimetsunoyaibamultiplayer.compat.GravityApiCompat.getBaseGravityStrength(entity) : 1.0D;
    }

    public static void setBaseGravityDirection(Entity entity, Direction direction) {
        if (isEnabled()) {
            com.lerdorf.kimetsunoyaibamultiplayer.compat.GravityApiCompat.setBaseGravityDirection(entity, direction);
        }
    }

    public static void setBaseGravityStrength(Entity entity, double strength) {
        if (isEnabled()) {
            com.lerdorf.kimetsunoyaibamultiplayer.compat.GravityApiCompat.setBaseGravityStrength(entity, strength);
        }
    }

    public static void resetGravity(Entity entity) {
        if (isEnabled()) {
            com.lerdorf.kimetsunoyaibamultiplayer.compat.GravityApiCompat.resetGravity(entity);
        }
    }

    public static Vec3 getWorldVelocity(Entity entity) {
        return isEnabled() ? com.lerdorf.kimetsunoyaibamultiplayer.compat.GravityApiCompat.getWorldVelocity(entity) : entity.getDeltaMovement();
    }

    public static void setWorldVelocity(Entity entity, Vec3 velocity) {
        if (isEnabled()) {
            com.lerdorf.kimetsunoyaibamultiplayer.compat.GravityApiCompat.setWorldVelocity(entity, velocity);
        } else {
            entity.setDeltaMovement(velocity);
        }
    }

    public static Vec3 getEyeOffset(Entity entity) {
        return isEnabled()
            ? com.lerdorf.kimetsunoyaibamultiplayer.compat.GravityApiCompat.getEyeOffset(entity)
            : new Vec3(0.0D, entity.getEyeHeight(), 0.0D);
    }

    public static boolean canChangeGravity(Entity entity) {
        return isEnabled() && com.lerdorf.kimetsunoyaibamultiplayer.compat.GravityApiCompat.canChangeGravity(entity);
    }

    /**
     * True when the entity may participate in KNY gravity fields: either the provider
     * allows it to change gravity, or it was explicitly marked (e.g. ability-spawned
     * projectiles and summons).
     */
    public static boolean isGravityAffected(Entity entity) {
        return canChangeGravity(entity)
            || (isEnabled() && entity != null && entity.getPersistentData().getBoolean(GRAVITY_AFFECTED_TAG));
    }

    /**
     * The provider's smooth client-side rotation animation for this entity, or null.
     * Used by client renderers to align visuals with the rotated entity model.
     */
    public static Object getRotationAnimation(Entity entity) {
        return isEnabled() ? com.lerdorf.kimetsunoyaibamultiplayer.compat.GravityApiCompat.getRotationAnimation(entity) : null;
    }
}
