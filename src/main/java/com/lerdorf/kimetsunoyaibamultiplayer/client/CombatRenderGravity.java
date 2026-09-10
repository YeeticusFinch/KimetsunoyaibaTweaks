package com.lerdorf.kimetsunoyaibamultiplayer.client;

import java.lang.reflect.Method;

import com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame;
import com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.KNYGravity;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Client-side gravity alignment for combat visuals (port of WibsDSCore's
 * CastleCombatRender). Shares the provider's animated local-to-world rotation
 * with slash offsets and particles so they follow the rotated entity model.
 * All methods are vanilla passthroughs without a gravity provider.
 */
@OnlyIn(Dist.CLIENT)
public final class CombatRenderGravity {
    private static final Quaternionf[] ROTATIONS = new Quaternionf[Direction.values().length];
    private static Method getCurrentGravityRotationMethod;
    private static boolean rotationMethodResolved;
    static {
        for (Direction direction : Direction.values()) {
            Vec3 x = CombatGravityFrame.toWorld(new Vec3(1, 0, 0), direction);
            Vec3 y = CombatGravityFrame.toWorld(new Vec3(0, 1, 0), direction);
            Vec3 z = CombatGravityFrame.toWorld(new Vec3(0, 0, 1), direction);
            ROTATIONS[direction.ordinal()] = new Quaternionf().setFromNormalized(new Matrix3f(
                (float) x.x, (float) x.y, (float) x.z,
                (float) y.x, (float) y.y, (float) y.z,
                (float) z.x, (float) z.y, (float) z.z));
        }
    }

    private CombatRenderGravity() {
    }

    /** Static gravity-basis rotation for a direction, or null for DOWN. */
    public static Quaternionf rotation(Direction direction) {
        return direction == null || direction == Direction.DOWN ? null : new Quaternionf(ROTATIONS[direction.ordinal()]);
    }

    /** The entity's current smooth gravity rotation, or null when vanilla. */
    public static Quaternionf rotation(Entity entity, float partialTick) {
        if (entity == null || !KNYGravity.isEnabled()) {
            return null;
        }
        Object animation = KNYGravity.getRotationAnimation(entity);
        if (animation == null) {
            return null;
        }
        try {
            Method method = currentGravityRotationMethod(animation);
            if (method == null) {
                return null;
            }
            long time = entity.level().getGameTime() * 50L + (long) (partialTick * 50.0F);
            Direction direction = KNYGravity.getGravityDirection(entity);
            Quaternionf result = new Quaternionf((Quaternionf) method.invoke(animation, direction, time)).conjugate();
            return result.x == 0 && result.y == 0 && result.z == 0 ? null : result;
        } catch (Exception exception) {
            return null;
        }
    }

    private static Method currentGravityRotationMethod(Object animation) {
        if (!rotationMethodResolved) {
            rotationMethodResolved = true;
            try {
                getCurrentGravityRotationMethod = animation.getClass()
                    .getMethod("getCurrentGravityRotation", Direction.class, long.class);
            } catch (NoSuchMethodException exception) {
                getCurrentGravityRotationMethod = null;
            }
        }
        return getCurrentGravityRotationMethod;
    }

    /** Transform an authored local position (relative to the entity) into world space. */
    public static Vec3 position(Entity entity, Vec3 authoredPosition, Quaternionf rotation) {
        if (rotation == null || entity == null) {
            return authoredPosition;
        }
        Vec3 origin = entity.position();
        Vec3 offset = authoredPosition.subtract(origin);
        Vector3f rotated = rotation.transform(new Vector3f((float) offset.x, (float) offset.y, (float) offset.z));
        return origin.add(rotated.x, rotated.y, rotated.z);
    }

    /** Spawn a particle whose authored local coordinates/velocity follow the entity's gravity. */
    public static void particle(Entity owner, ClientLevel level, ParticleOptions options,
            double x, double y, double z, double vx, double vy, double vz) {
        Quaternionf rotation = rotation(owner, 1.0F);
        if (rotation == null) {
            level.addParticle(options, x, y, z, vx, vy, vz);
            return;
        }
        Vec3 point = position(owner, new Vec3(x, y, z), rotation);
        Vector3f velocity = rotation.transform(new Vector3f((float) vx, (float) vy, (float) vz));
        level.addParticle(options, point.x, point.y, point.z, velocity.x, velocity.y, velocity.z);
    }
}
