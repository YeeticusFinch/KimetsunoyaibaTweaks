package com.lerdorf.kimetsunoyaibamultiplayer.gravity.api;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class GravityAwareTargeting {
    private GravityAwareTargeting() {
    }

    public static Vec3 getEyePosition(Entity entity) {
        Direction gravity = KNYGravity.getGravityDirection(entity);
        if (gravity == Direction.DOWN) {
            return entity.getEyePosition();
        }
        return entity.position().add(GravityDirectionHelper.localToWorld(entity, new Vec3(0.0D, entity.getEyeHeight(), 0.0D)));
    }

    public static Vec3 getBodyTarget(Entity entity) {
        Direction gravity = KNYGravity.getGravityDirection(entity);
        if (gravity == Direction.DOWN) {
            return entity instanceof LivingEntity living ? living.getEyePosition().add(0.0D, -living.getBbHeight() * 0.35D, 0.0D) : entity.position();
        }
        return entity.position().add(GravityDirectionHelper.localToWorld(entity, new Vec3(0.0D, entity.getBbHeight() * 0.5D, 0.0D)));
    }

    public static Vec3 projectToLocalHorizontal(Entity entity, Vec3 vec) {
        Vec3 local = GravityDirectionHelper.worldToLocal(entity, vec);
        return GravityDirectionHelper.localToWorld(entity, new Vec3(local.x, 0.0D, local.z));
    }
}
