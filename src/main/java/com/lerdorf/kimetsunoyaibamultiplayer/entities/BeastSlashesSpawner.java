package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class BeastSlashesSpawner {
    public static BeastSlashesEntity spawnBeastSlashes(Level level, Vec3 position, float yaw, float pitch, String animationName, int lifetimeTicks) {
        if (level.isClientSide) {
            return null;
        }
        BeastSlashesEntity entity = BeastSlashesEntity.create(level, position, yaw, pitch, animationName, lifetimeTicks);
        level.addFreshEntity(entity);
        return entity;
    }
}
