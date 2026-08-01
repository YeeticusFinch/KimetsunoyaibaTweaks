package com.lerdorf.kimetsunoyaibamultiplayer.gravity.engine;

import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.EntityEvent;

public class GravityEffectEvent extends EntityEvent {
    public GravityEffectEvent(Entity entity) {
        super(entity);
    }
}
