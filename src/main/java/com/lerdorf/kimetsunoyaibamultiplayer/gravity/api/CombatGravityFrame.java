package com.lerdorf.kimetsunoyaibamultiplayer.gravity.api;

import com.lerdorf.kimetsunoyaibamultiplayer.gravity.engine.RotationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/**
 * Coordinate boundary for combat math (port of WibsDSCore's CastleCombatFrame).
 *
 * When a gravity provider (gravityapi or gravitychanger) is available and an entity
 * using an ability/attack is inside a frame, all combat coordinates authored by ability
 * code are expressed relative to the entity's own local axes: +Y is "up" away from the
 * surface the entity stands on, X/Z span the surface. Authored local vectors are
 * converted to world coordinates only at API boundaries (velocity writes, particles,
 * block lookups).
 *
 * Without a provider the frame never activates and every method here is an exact
 * vanilla passthrough, so ability code needs no gravity awareness itself.
 */
public final class CombatGravityFrame {
    private static final ThreadLocal<CombatGravityFrame> ACTIVE = new ThreadLocal<>();
    private static final CombatGravityFrame[] FRAMES = new CombatGravityFrame[Direction.values().length];
    static {
        for (Direction direction : Direction.values()) {
            FRAMES[direction.ordinal()] = new CombatGravityFrame(direction);
        }
    }

    private final Direction direction;

    private CombatGravityFrame(Direction direction) {
        this.direction = direction;
    }

    /**
     * Enter the entity's gravity frame. Returns the previous frame state for restore().
     * If no gravity provider is available the frame stays inactive (vanilla passthrough).
     */
    public static CombatGravityFrame enter(Entity entity) {
        CombatGravityFrame previous = ACTIVE.get();
        if (entity == null || !KNYGravity.isEnabled()) {
            ACTIVE.remove();
        } else {
            ACTIVE.set(FRAMES[KNYGravity.getGravityDirection(entity).ordinal()]);
        }
        return previous;
    }

    public static void restore(CombatGravityFrame previous) {
        if (previous == null) {
            ACTIVE.remove();
        } else {
            ACTIVE.set(previous);
        }
    }

    /**
     * Run an action inside the entity's gravity frame, restoring state afterwards.
     */
    public static void run(Entity entity, Runnable action) {
        CombatGravityFrame previous = enter(entity);
        try {
            action.run();
        } finally {
            restore(previous);
        }
    }

    /**
     * Capture the currently active frame into a runnable. Used by the ability scheduler
     * so delayed/repeating ability callbacks keep executing in the frame they were
     * authored in. Returns the action unchanged when no frame is active.
     */
    public static Runnable bind(Entity owner, Runnable action) {
        CombatGravityFrame captured = ACTIVE.get();
        if (captured == null) {
            return action;
        }
        return () -> {
            if (owner == null || owner.isRemoved() || !KNYGravity.isEnabled()) {
                return;
            }
            CombatGravityFrame previous = ACTIVE.get();
            ACTIVE.set(FRAMES[KNYGravity.getGravityDirection(owner).ordinal()]);
            try {
                action.run();
            } finally {
                restore(previous);
            }
        };
    }

    /**
     * Called for spawned visual/projectile entities during an active frame: applies
     * the frame's gravity direction to the entity (so providers rotate and simulate
     * it correctly) and marks it as KNY-gravity-affected for field arbitration.
     */
    public static void inheritVisual(Entity entity) {
        if (active() && entity != null && KNYGravity.isEnabled()) {
            entity.getPersistentData().putBoolean(KNYGravity.GRAVITY_AFFECTED_TAG, true);
            KNYGravity.setBaseGravityDirection(entity, currentDirection());
        }
    }

    public static boolean active() {
        return ACTIVE.get() != null;
    }

    public static Direction currentDirection() {
        CombatGravityFrame frame = ACTIVE.get();
        return frame == null ? Direction.DOWN : frame.direction;
    }

    /** The entity's actual gravity direction (vanilla DOWN without a provider). */
    public static Direction physicalDirection(Entity entity) {
        return KNYGravity.getGravityDirection(entity);
    }

    /**
     * Local (gravity-relative) to world coordinates.
     * Matches RotationUtil.vecPlayerToWorld so wrappers stay consistent.
     */
    public static Vec3 toWorld(Vec3 local, Direction gravity) {
        return RotationUtil.vecPlayerToWorld(local, gravity);
    }

    /** World to local (gravity-relative) coordinates. */
    public static Vec3 toLocal(Vec3 world, Direction gravity) {
        return RotationUtil.vecWorldToPlayer(world, gravity);
    }

    /** Convert an authored local vector to world coordinates using the active frame. */
    public static Vec3 world(Vec3 value) {
        return toWorld(value, currentDirection());
    }

    /** Convert a world vector to authored local coordinates using the active frame. */
    public static Vec3 local(Vec3 value) {
        return toLocal(value, currentDirection());
    }

    public static BlockPos world(BlockPos value) {
        return active() ? BlockPos.containing(world(Vec3.atCenterOf(value))) : value;
    }

    public static BlockPos local(BlockPos value) {
        return active() ? BlockPos.containing(local(Vec3.atCenterOf(value))) : value;
    }

    public static AABB world(AABB box) {
        if (!active()) {
            return box;
        }
        return transformedBounds(box, true);
    }

    public static AABB local(AABB box) {
        if (!active()) {
            return box;
        }
        return transformedBounds(box, false);
    }

    private static AABB transformedBounds(AABB box, boolean toWorld) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                for (int z = 0; z < 2; z++) {
                    Vec3 corner = new Vec3(
                        x == 0 ? box.minX : box.maxX,
                        y == 0 ? box.minY : box.maxY,
                        z == 0 ? box.minZ : box.maxZ);
                    Vec3 transformed = toWorld ? world(corner) : local(corner);
                    minX = Math.min(minX, transformed.x);
                    minY = Math.min(minY, transformed.y);
                    minZ = Math.min(minZ, transformed.z);
                    maxX = Math.max(maxX, transformed.x);
                    maxY = Math.max(maxY, transformed.y);
                    maxZ = Math.max(maxZ, transformed.z);
                }
            }
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static Direction world(Direction value) {
        if (!active()) {
            return value;
        }
        Vec3 normal = world(Vec3.atLowerCornerOf(value.getNormal()));
        return Direction.getNearest(normal.x, normal.y, normal.z);
    }

    public static Direction local(Direction value) {
        if (!active()) {
            return value;
        }
        Vec3 normal = local(Vec3.atLowerCornerOf(value.getNormal()));
        return Direction.getNearest(normal.x, normal.y, normal.z);
    }

    // ------------------------------------------------------------------
    // Position reads (always in authored local coordinates)
    // ------------------------------------------------------------------

    public static Vec3 position(Entity entity) {
        return active() ? local(entity.position()) : entity.position();
    }

    public static Vec3 eye(Entity entity) {
        return active() ? local(entity.getEyePosition()) : entity.getEyePosition();
    }

    public static Vec3 eye(Entity entity, float partialTick) {
        return active() ? local(entity.getEyePosition(partialTick)) : entity.getEyePosition(partialTick);
    }

    /** Point above the entity's feet along its local up axis. */
    public static Vec3 bodyPoint(Entity entity, double height) {
        if (!active()) {
            return entity.position().add(0.0D, height, 0.0D);
        }
        return entity.position().add(toWorld(new Vec3(0.0D, height, 0.0D), currentDirection()));
    }

    public static double x(Entity entity) {
        return active() ? local(entity.position()).x : entity.getX();
    }

    public static double y(Entity entity) {
        return active() ? local(entity.position()).y : entity.getY();
    }

    public static double z(Entity entity) {
        return active() ? local(entity.position()).z : entity.getZ();
    }

    // ------------------------------------------------------------------
    // Look / rotation reads (authored local basis)
    // ------------------------------------------------------------------

    public static Vec3 look(Entity entity) {
        if (!active()) {
            return entity.getLookAngle();
        }
        Direction actual = KNYGravity.getGravityDirection(entity);
        return local(RotationUtil.vecPlayerToWorld(entity.getLookAngle(), actual)).normalize();
    }

    public static Vec3 view(Entity entity, float partialTick) {
        if (!active()) {
            return entity.getViewVector(partialTick);
        }
        Direction actual = KNYGravity.getGravityDirection(entity);
        return local(RotationUtil.vecPlayerToWorld(entity.getViewVector(partialTick), actual));
    }

    public static float yaw(Entity entity) {
        if (!active() || KNYGravity.getGravityDirection(entity) == currentDirection()) {
            return entity.getYRot();
        }
        return rotation(entity).y;
    }

    public static float pitch(Entity entity) {
        if (!active() || KNYGravity.getGravityDirection(entity) == currentDirection()) {
            return entity.getXRot();
        }
        return rotation(entity).x;
    }

    /**
     * Convert authored local yaw/pitch to the entity-native rotation values the
     * entity's storage (and the provider's renderer) expects. Vanilla passthrough
     * when no frame is active.
     */
    public static Vec2 nativeRotation(Entity entity, float yaw, float pitch) {
        Direction actual = KNYGravity.getGravityDirection(entity);
        if (!active() || actual == currentDirection()) {
            return new Vec2(pitch, yaw);
        }
        Vec3 worldVec = toWorld(Vec3.directionFromRotation(pitch, yaw), currentDirection());
        Vec3 entityLocal = toLocal(worldVec, actual);
        return angles(entityLocal);
    }

    /** Convert an entity-native rotation to authored local yaw/pitch. */
    public static Vec2 rotation(Entity entity) {
        Direction actual = KNYGravity.getGravityDirection(entity);
        if (!active() || actual == currentDirection()) {
            return entity.getRotationVector();
        }
        Vec3 localLook = toLocal(RotationUtil.vecPlayerToWorld(entity.getLookAngle(), actual), currentDirection());
        return angles(localLook);
    }

    private static Vec2 angles(Vec3 vector) {
        double horizontal = Math.sqrt(vector.x * vector.x + vector.z * vector.z);
        return new Vec2(
            (float) Math.toDegrees(Math.atan2(-vector.y, horizontal)),
            (float) Math.toDegrees(Math.atan2(-vector.x, vector.z))
        );
    }

    // ------------------------------------------------------------------
    // Velocity (providers store delta movement in entity-local basis)
    // ------------------------------------------------------------------

    /** Read velocity as authored local coordinates. */
    public static Vec3 velocity(Entity entity) {
        if (!active()) {
            return entity.getDeltaMovement();
        }
        Direction actual = KNYGravity.getGravityDirection(entity);
        if (actual == currentDirection()) {
            return entity.getDeltaMovement();
        }
        return local(toWorld(entity.getDeltaMovement(), actual));
    }

    /** Write velocity from authored local coordinates. */
    public static void velocity(Entity entity, Vec3 value) {
        if (!active()) {
            entity.setDeltaMovement(value);
            return;
        }
        Direction actual = KNYGravity.getGravityDirection(entity);
        entity.setDeltaMovement(actual == currentDirection() ? value : toLocal(world(value), actual));
    }

    public static void velocity(Entity entity, double x, double y, double z) {
        if (!active()) {
            entity.setDeltaMovement(x, y, z);
        } else {
            velocity(entity, new Vec3(x, y, z));
        }
    }

    // ------------------------------------------------------------------
    // Misc combat helpers
    // ------------------------------------------------------------------

    public static double distanceToSqr(Entity entity, double x, double y, double z) {
        if (!active()) {
            return entity.distanceToSqr(x, y, z);
        }
        Vec3 point = world(new Vec3(x, y, z));
        return entity.distanceToSqr(point.x, point.y, point.z);
    }

    /**
     * sendParticles boundary: authored local position/spread converted to world
     * coordinates. Exact vanilla passthrough when no frame is active.
     */
    public static <T extends ParticleOptions> int sendParticles(ServerLevel level, T particle,
            double x, double y, double z, int count, double dx, double dy, double dz, double speed) {
        if (!active()) {
            return level.sendParticles(particle, x, y, z, count, dx, dy, dz, speed);
        }
        Vec3 point = world(new Vec3(x, y, z));
        Vec3 spread = world(new Vec3(dx, dy, dz));
        return level.sendParticles(particle, point.x, point.y, point.z, count,
            count == 0 ? spread.x : Math.abs(spread.x),
            count == 0 ? spread.y : Math.abs(spread.y),
            count == 0 ? spread.z : Math.abs(spread.z),
            speed);
    }

    /** Send particles whose position and spread are already in world coordinates. */
    public static <T extends ParticleOptions> int sendWorldParticles(ServerLevel level, T particle,
            double x, double y, double z, int count, double dx, double dy, double dz, double speed) {
        if (!active()) {
            return level.sendParticles(particle, x, y, z, count, dx, dy, dz, speed);
        }
        Vec3 point = local(new Vec3(x, y, z));
        Vec3 spread = local(new Vec3(dx, dy, dz));
        return sendParticles(level, particle, point.x, point.y, point.z, count,
            spread.x, spread.y, spread.z, speed);
    }
}
