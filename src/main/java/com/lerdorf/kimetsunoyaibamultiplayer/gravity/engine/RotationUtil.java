package com.lerdorf.kimetsunoyaibamultiplayer.gravity.engine;

import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class RotationUtil {
    private static final Direction[][] DIR_WORLD_TO_PLAYER = new Direction[6][];
    private static final Direction[][] DIR_PLAYER_TO_WORLD = new Direction[6][];
    private static final Quaternionf[] ENTITY_ROTATION_QUATERNIONS = new Quaternionf[6];
    private static final Quaternionf[] WORLD_ROTATION_QUATERNIONS = new Quaternionf[6];

    static {
        WORLD_ROTATION_QUATERNIONS[0] = new Quaternionf();
        WORLD_ROTATION_QUATERNIONS[1] = Axis.ZP.rotationDegrees(-180);
        WORLD_ROTATION_QUATERNIONS[2] = Axis.XP.rotationDegrees(-90);
        WORLD_ROTATION_QUATERNIONS[3] = Axis.XP.rotationDegrees(-90).mul(Axis.YP.rotationDegrees(-180));
        WORLD_ROTATION_QUATERNIONS[4] = Axis.XP.rotationDegrees(-90).mul(Axis.YP.rotationDegrees(-90));
        WORLD_ROTATION_QUATERNIONS[5] = Axis.XP.rotationDegrees(-90).mul(Axis.YP.rotationDegrees(-270));

        for (Direction gravityDirection : Direction.values()) {
            DIR_WORLD_TO_PLAYER[gravityDirection.get3DDataValue()] = new Direction[6];
            DIR_PLAYER_TO_WORLD[gravityDirection.get3DDataValue()] = new Direction[6];
            for (Direction direction : Direction.values()) {
                Vec3 directionVector = Vec3.atLowerCornerOf(direction.getNormal());
                Vec3 playerDirection = vecWorldToPlayer(directionVector, gravityDirection);
                Vec3 worldDirection = vecPlayerToWorld(directionVector, gravityDirection);
                DIR_WORLD_TO_PLAYER[gravityDirection.get3DDataValue()][direction.get3DDataValue()] =
                    Direction.getNearest(playerDirection.x, playerDirection.y, playerDirection.z);
                DIR_PLAYER_TO_WORLD[gravityDirection.get3DDataValue()][direction.get3DDataValue()] =
                    Direction.getNearest(worldDirection.x, worldDirection.y, worldDirection.z);
            }
        }

        for (int i = 0; i < 6; i++) {
            ENTITY_ROTATION_QUATERNIONS[i] = new Quaternionf().set(WORLD_ROTATION_QUATERNIONS[i]).conjugate();
        }
    }

    private RotationUtil() {
    }

    public static Direction dirWorldToPlayer(Direction direction, Direction gravityDirection) {
        return DIR_WORLD_TO_PLAYER[gravityDirection.get3DDataValue()][direction.get3DDataValue()];
    }

    public static Direction dirPlayerToWorld(Direction direction, Direction gravityDirection) {
        return DIR_PLAYER_TO_WORLD[gravityDirection.get3DDataValue()][direction.get3DDataValue()];
    }

    public static Vec3 vecWorldToPlayer(double x, double y, double z, Direction gravityDirection) {
        return switch (gravityDirection) {
            case DOWN -> new Vec3(x, y, z);
            case UP -> new Vec3(-x, -y, z);
            case NORTH -> new Vec3(x, z, -y);
            case SOUTH -> new Vec3(-x, -z, -y);
            case WEST -> new Vec3(-z, x, -y);
            case EAST -> new Vec3(z, -x, -y);
        };
    }

    public static Vec3 vecWorldToPlayer(Vec3 vec, Direction gravityDirection) {
        return vecWorldToPlayer(vec.x, vec.y, vec.z, gravityDirection);
    }

    public static Vector3f vecWorldToPlayer(float x, float y, float z, Direction gravityDirection) {
        return switch (gravityDirection) {
            case DOWN -> new Vector3f(x, y, z);
            case UP -> new Vector3f(-x, -y, z);
            case NORTH -> new Vector3f(x, z, -y);
            case SOUTH -> new Vector3f(-x, -z, -y);
            case WEST -> new Vector3f(-z, x, -y);
            case EAST -> new Vector3f(z, -x, -y);
        };
    }

    public static Vector3f vecWorldToPlayer(Vector3f vec, Direction gravityDirection) {
        return vecWorldToPlayer(vec.x(), vec.y(), vec.z(), gravityDirection);
    }

    public static Vec3 vecEntityToWorld(double x, double y, double z, Direction gravityDirection) {
        return switch (gravityDirection) {
            case DOWN -> new Vec3(x, y, z);
            case UP -> new Vec3(x, -y, z);
            case NORTH -> new Vec3(x, -z, y);
            case SOUTH -> new Vec3(-x, -z, -y);
            case WEST -> new Vec3(y, -z, -x);
            case EAST -> new Vec3(-y, -z, x);
        };
    }

    public static Vec3 vecEntityToWorld(Vec3 vec, Direction gravityDirection) {
        return vecEntityToWorld(vec.x, vec.y, vec.z, gravityDirection);
    }

    public static Vec3 vecPlayerToWorld(double x, double y, double z, Direction gravityDirection) {
        return switch (gravityDirection) {
            case DOWN -> new Vec3(x, y, z);
            case UP -> new Vec3(-x, -y, z);
            case NORTH -> new Vec3(x, -z, y);
            case SOUTH -> new Vec3(-x, -z, -y);
            case WEST -> new Vec3(y, -z, -x);
            case EAST -> new Vec3(-y, -z, x);
        };
    }

    public static Vec3 vecPlayerToWorld(Vec3 vec, Direction gravityDirection) {
        return vecPlayerToWorld(vec.x, vec.y, vec.z, gravityDirection);
    }

    public static Vector3f vecPlayerToWorld(float x, float y, float z, Direction gravityDirection) {
        return switch (gravityDirection) {
            case DOWN -> new Vector3f(x, y, z);
            case UP -> new Vector3f(-x, -y, z);
            case NORTH -> new Vector3f(x, -z, y);
            case SOUTH -> new Vector3f(-x, -z, -y);
            case WEST -> new Vector3f(y, -z, -x);
            case EAST -> new Vector3f(-y, -z, x);
        };
    }

    public static Vector3f vecPlayerToWorld(Vector3f vec, Direction gravityDirection) {
        return vecPlayerToWorld(vec.x(), vec.y(), vec.z(), gravityDirection);
    }

    public static Vec3 maskWorldToPlayer(Vec3 vec, Direction gravityDirection) {
        return switch (gravityDirection) {
            case DOWN, UP -> vec;
            case NORTH, SOUTH -> new Vec3(vec.x, vec.z, vec.y);
            case WEST, EAST -> new Vec3(vec.z, vec.x, vec.y);
        };
    }

    public static Vec3 maskWorldToPlayer(double x, double y, double z, Direction gravityDirection) {
        return maskWorldToPlayer(new Vec3(x, y, z), gravityDirection);
    }

    public static Vec3 maskPlayerToWorld(Vec3 vec, Direction gravityDirection) {
        return switch (gravityDirection) {
            case DOWN, UP -> vec;
            case NORTH, SOUTH -> new Vec3(vec.x, vec.z, vec.y);
            case WEST, EAST -> new Vec3(vec.y, vec.z, vec.x);
        };
    }

    public static Vec3 maskPlayerToWorld(double x, double y, double z, Direction gravityDirection) {
        return maskPlayerToWorld(new Vec3(x, y, z), gravityDirection);
    }

    public static AABB boxWorldToPlayer(AABB box, Direction gravityDirection) {
        return new AABB(
            vecWorldToPlayer(box.minX, box.minY, box.minZ, gravityDirection),
            vecWorldToPlayer(box.maxX, box.maxY, box.maxZ, gravityDirection)
        );
    }

    public static AABB boxPlayerToWorld(AABB box, Direction gravityDirection) {
        return new AABB(
            vecPlayerToWorld(box.minX, box.minY, box.minZ, gravityDirection),
            vecPlayerToWorld(box.maxX, box.maxY, box.maxZ, gravityDirection)
        );
    }

    public static Vec2 rotWorldToPlayer(float yaw, float pitch, Direction gravityDirection) {
        Vec3 vec = vecWorldToPlayer(rotToVec(yaw, pitch), gravityDirection);
        return vecToRot(vec.x, vec.y, vec.z);
    }

    public static Vec2 rotWorldToPlayer(Vec2 rotation, Direction gravityDirection) {
        return rotWorldToPlayer(rotation.x, rotation.y, gravityDirection);
    }

    public static Vec2 rotPlayerToWorld(float yaw, float pitch, Direction gravityDirection) {
        Vec3 vec = vecPlayerToWorld(rotToVec(yaw, pitch), gravityDirection);
        return vecToRot(vec.x, vec.y, vec.z);
    }

    public static Vec2 rotPlayerToWorld(Vec2 rotation, Direction gravityDirection) {
        return rotPlayerToWorld(rotation.x, rotation.y, gravityDirection);
    }

    public static Vec3 rotToVec(float yaw, float pitch) {
        double radPitch = pitch * 0.017453292D;
        double radNegYaw = -yaw * 0.017453292D;
        double cosNegYaw = Math.cos(radNegYaw);
        double sinNegYaw = Math.sin(radNegYaw);
        double cosPitch = Math.cos(radPitch);
        double sinPitch = Math.sin(radPitch);
        return new Vec3(sinNegYaw * cosPitch, -sinPitch, cosNegYaw * cosPitch);
    }

    public static Vec2 vecToRot(double x, double y, double z) {
        double sinPitch = -y;
        double radPitch = Math.asin(Mth.clamp(sinPitch, -1.0D, 1.0D));
        double cosPitch = Math.cos(radPitch);
        if (Math.abs(cosPitch) < 1.0E-6D) {
            return new Vec2(0.0F, (float) (radPitch / 0.017453292D));
        }
        double sinNegYaw = x / cosPitch;
        double cosNegYaw = Mth.clamp(z / cosPitch, -1, 1);
        double radNegYaw = Math.acos(cosNegYaw);
        if (sinNegYaw < 0) {
            radNegYaw = Math.PI * 2 - radNegYaw;
        }
        return new Vec2(Mth.wrapDegrees((float) (-radNegYaw / 0.017453292D)), (float) (radPitch / 0.017453292D));
    }

    public static Vec2 vecToRot(Vec3 vec) {
        return vecToRot(vec.x, vec.y, vec.z);
    }

    public static Quaternionf getWorldRotationQuaternion(Direction gravityDirection) {
        return WORLD_ROTATION_QUATERNIONS[gravityDirection.get3DDataValue()];
    }

    public static Quaternionf getCameraRotationQuaternion(Direction gravityDirection) {
        return ENTITY_ROTATION_QUATERNIONS[gravityDirection.get3DDataValue()];
    }

    public static Quaternionf getRotationBetween(Direction from, Direction to) {
        if (from.getOpposite() == to) {
            return new Quaternionf().fromAxisAngleDeg(new Vector3f(0, 0, -1), 180.0F);
        }
        return QuaternionUtil.getRotationBetween(new Vec3(from.step()), new Vec3(to.step()));
    }

    public static Quaternionf interpolate(Quaternionf start, Quaternionf end, float progress) {
        return new Quaternionf().set(start).slerp(end, progress);
    }

    public static AABB makeBoxFromDimensions(EntityDimensions dimensions, Direction gravityDir, Vec3 pos) {
        AABB rawBox = dimensions.makeBoundingBox(0, 0, 0);
        return boxPlayerToWorld(rawBox, gravityDir).move(pos);
    }
}
