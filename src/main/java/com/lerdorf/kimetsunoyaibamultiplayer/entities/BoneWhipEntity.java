package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Flesh Vine variant used by Biokineses with a narrower bone-whip body. */
public final class BoneWhipEntity extends FleshVineEntity {
    public static final double DEFAULT_DIAMETER = FleshVineEntity.DEFAULT_DIAMETER * 0.5D;

    public BoneWhipEntity(EntityType<? extends BoneWhipEntity> type, Level level) {
        super(type, level);
    }
}
