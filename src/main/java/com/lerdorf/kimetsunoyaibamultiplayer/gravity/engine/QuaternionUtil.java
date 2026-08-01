package com.lerdorf.kimetsunoyaibamultiplayer.gravity.engine;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class QuaternionUtil {
    private QuaternionUtil() {
    }

    public static Quaternionf getViewRotation(float pitch, float yaw) {
        Quaternionf xRotation = new Quaternionf().fromAxisAngleDeg(new Vector3f(1, 0, 0), pitch);
        Quaternionf yRotation = new Quaternionf().fromAxisAngleDeg(new Vector3f(0, 1, 0), yaw + 180);
        return xRotation.mul(yRotation);
    }

    public static Quaternionf getRotationBetween(Vec3 from, Vec3 to) {
        Vec3 normalizedFrom = from.normalize();
        Vec3 normalizedTo = to.normalize();
        Vec3 axis = normalizedFrom.cross(normalizedTo).normalize();
        double cos = normalizedFrom.dot(normalizedTo);
        double angle = Math.acos(cos);
        return new Quaternionf().fromAxisAngleRad(new Vector3f((float) axis.x, (float) axis.y, (float) axis.z), (float) angle);
    }

    public static Vec3 rotate(Vec3 vec, Quaternionf quaternion) {
        Vector3f vector = vec.toVector3f();
        vector.rotate(quaternion);
        return new Vec3(vector);
    }
}
