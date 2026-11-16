package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Spawns the server-side Blood Tornado entity. */
public final class BloodTornadoSpawner {
    private BloodTornadoSpawner() {
    }

    public static BloodTornadoEntity spawn(ServerLevel level, Vec3 position, Vec3 direction,
                                           LivingEntity owner, int lifetime) {
        BloodTornadoEntity tornado = ModEntities.BLOOD_TORNADO.get().create(level);
        if (tornado == null) {
            return null;
        }
        tornado.initialize(position, direction, owner, lifetime);
        level.addFreshEntity(tornado);
        return tornado;
    }
}
