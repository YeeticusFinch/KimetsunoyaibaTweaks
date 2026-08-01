package com.lerdorf.kimetsunoyaibamultiplayer.gravity.engine;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class RotationAnimation {
    private boolean inAnimation;
    private Quaternionf startGravityRotation = new Quaternionf();
    private Quaternionf endGravityRotation = new Quaternionf();
    private Vec3 relativeRotationCenter = Vec3.ZERO;
    private long startTimeMs;
    private long endTimeMs;

    public void startRotationAnimation(Direction newGravity, Direction prevGravity, long durationTimeMs,
                                       Entity entity, long timeMs, boolean rotateView, Vec3 relativeRotationCenter) {
        if (durationTimeMs == 0) {
            inAnimation = false;
            return;
        }

        Vec3 newLookingDirection = getNewLookingDirection(newGravity, prevGravity, entity, rotateView);
        Quaternionf oldViewRotation = QuaternionUtil.getViewRotation(entity.getXRot(), entity.getYRot());
        update(timeMs);
        Quaternionf currentAnimatedGravityRotation = getCurrentGravityRotation(prevGravity, timeMs);
        Quaternionf currentAnimatedCameraRotation = new Quaternionf().set(oldViewRotation).mul(currentAnimatedGravityRotation);

        Vec2 newYawAndPitch = RotationUtil.vecToRot(RotationUtil.vecWorldToPlayer(newLookingDirection, newGravity).x,
            RotationUtil.vecWorldToPlayer(newLookingDirection, newGravity).y,
            RotationUtil.vecWorldToPlayer(newLookingDirection, newGravity).z);
        float deltaYaw = newYawAndPitch.x - entity.getYRot();
        float deltaPitch = newYawAndPitch.y - entity.getXRot();
        entity.setYRot(entity.getYRot() + deltaYaw);
        entity.setXRot(entity.getXRot() + deltaPitch);
        entity.yRotO += deltaYaw;
        entity.xRotO += deltaPitch;
        if (entity instanceof LivingEntity living) {
            living.yBodyRot += deltaYaw;
            living.yBodyRotO += deltaYaw;
            living.yHeadRot += deltaYaw;
            living.yHeadRotO += deltaYaw;
        }

        Quaternionf newViewRotation = QuaternionUtil.getViewRotation(entity.getXRot(), entity.getYRot());
        this.relativeRotationCenter = relativeRotationCenter;
        this.inAnimation = true;
        this.startGravityRotation = new Quaternionf().set(newViewRotation).conjugate().mul(currentAnimatedCameraRotation);
        this.endGravityRotation = RotationUtil.getWorldRotationQuaternion(newGravity);
        this.startTimeMs = timeMs;
        this.endTimeMs = timeMs + durationTimeMs;
    }

    private Vec3 getNewLookingDirection(Direction newGravity, Direction prevGravity, Entity entity, boolean rotateView) {
        Vec3 oldLookingDirection = RotationUtil.vecPlayerToWorld(RotationUtil.rotToVec(entity.getYRot(), entity.getXRot()), prevGravity);
        if (!rotateView) {
            return oldLookingDirection;
        }
        if (newGravity == prevGravity.getOpposite()) {
            return oldLookingDirection.scale(-1);
        }
        Vector3f lookingDirection = oldLookingDirection.toVector3f();
        lookingDirection.rotate(QuaternionUtil.getRotationBetween(Vec3.atLowerCornerOf(prevGravity.getNormal()),
            Vec3.atLowerCornerOf(newGravity.getNormal())));
        return new Vec3(lookingDirection);
    }

    public Quaternionf getCurrentGravityRotation(Direction currentGravity, long timeMs) {
        update(timeMs);
        if (!inAnimation) {
            return RotationUtil.getWorldRotationQuaternion(currentGravity);
        }
        double delta = (double) (timeMs - startTimeMs) / (endTimeMs - startTimeMs);
        return RotationUtil.interpolate(startGravityRotation, endGravityRotation, Mth.clamp((float) (delta * delta * (3 - 2 * delta)), 0, 1));
    }

    public void update(long timeMs) {
        if (timeMs > endTimeMs) {
            inAnimation = false;
        }
    }

    public Vec3 getEyeOffset(Quaternionf gravityRot, Vec3 localEyeOffset, Direction newGravity) {
        Quaternionf gravityRotForEntity = new Quaternionf(gravityRot).conjugate();
        if (!inAnimation || relativeRotationCenter.equals(Vec3.ZERO)) {
            return QuaternionUtil.rotate(localEyeOffset, gravityRotForEntity);
        }
        Vec3 rotationCenterOffset = RotationUtil.vecPlayerToWorld(relativeRotationCenter, newGravity);
        Vec3 eyeOffsetFromRotationCenter = localEyeOffset.subtract(relativeRotationCenter);
        return rotationCenterOffset.add(QuaternionUtil.rotate(eyeOffsetFromRotationCenter, gravityRotForEntity));
    }

    public boolean isInAnimation() {
        return inAnimation;
    }
}
