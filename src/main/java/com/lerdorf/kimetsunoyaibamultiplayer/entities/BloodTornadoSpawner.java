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
        return spawn(level, position, direction, owner, lifetime, false);
    }

    public static BloodTornadoEntity spawnNoGravity(ServerLevel level, Vec3 position, Vec3 direction,
                                                    LivingEntity owner, int lifetime) {
        return spawn(level, position, direction, owner, lifetime, true);
    }

    private static BloodTornadoEntity spawn(ServerLevel level, Vec3 position, Vec3 direction,
                                            LivingEntity owner, int lifetime, boolean noGravity) {
        BloodTornadoEntity tornado = ModEntities.BLOOD_TORNADO.get().create(level);
        if (tornado == null) {
            return null;
        }
        com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.inheritVisual(tornado);
        if (noGravity) {
            tornado.initializeNoGravity(position, direction, owner, lifetime);
        } else {
            tornado.initialize(position, direction, owner, lifetime);
        }
        level.addFreshEntity(tornado);
        return tornado;
    }
}
