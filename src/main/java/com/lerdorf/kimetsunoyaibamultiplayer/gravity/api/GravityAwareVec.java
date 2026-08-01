package com.lerdorf.kimetsunoyaibamultiplayer.gravity.api;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class GravityAwareVec {
    private GravityAwareVec() {
    }

    public static Vec3 local(Entity entity, double x, double y, double z) {
        return GravityDirectionHelper.localToWorld(entity, new Vec3(x, y, z));
    }

    public static Vec3 localHorizontal(Entity entity, double x, double z) {
        return local(entity, x, 0.0D, z);
    }

    public static Vec3 localUp(Entity entity, double amount) {
        return GravityDirectionHelper.getLocalUp(entity).scale(amount);
    }

    public static Vec3 localDown(Entity entity, double amount) {
        return GravityDirectionHelper.getLocalDown(entity).scale(amount);
    }
}
