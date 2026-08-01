package com.lerdorf.kimetsunoyaibamultiplayer.gravity.api;

import com.lerdorf.kimetsunoyaibamultiplayer.gravity.engine.RotationUtil;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class GravityDirectionHelper {
    private GravityDirectionHelper() {
    }

    public static Vec3 getLookDirection(Entity entity) {
        Direction gravity = KNYGravity.getGravityDirection(entity);
        if (gravity == Direction.DOWN) {
            return entity.getLookAngle();
        }
        return RotationUtil.vecPlayerToWorld(RotationUtil.rotToVec(entity.getYRot(), entity.getXRot()), gravity).normalize();
    }

    public static Vec3 getLocalUp(Entity entity) {
        return Vec3.atLowerCornerOf(KNYGravity.getGravityDirection(entity).getOpposite().getNormal());
    }

    public static Vec3 getLocalDown(Entity entity) {
        return Vec3.atLowerCornerOf(KNYGravity.getGravityDirection(entity).getNormal());
    }

    public static Vec3 localToWorld(Entity entity, Vec3 localVector) {
        Direction gravity = KNYGravity.getGravityDirection(entity);
        return gravity == Direction.DOWN ? localVector : RotationUtil.vecPlayerToWorld(localVector, gravity);
    }

    public static Vec3 worldToLocal(Entity entity, Vec3 worldVector) {
        Direction gravity = KNYGravity.getGravityDirection(entity);
        return gravity == Direction.DOWN ? worldVector : RotationUtil.vecWorldToPlayer(worldVector, gravity);
    }
}
