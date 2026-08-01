package com.lerdorf.kimetsunoyaibamultiplayer.gravity.field;

import com.lerdorf.kimetsunoyaibamultiplayer.gravity.engine.RotationParameters;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public interface GravityEffectSource {
    boolean affects(Entity entity);

    Direction getDirection(Entity entity);

    double getPriority(Entity entity);

    @Nullable
    RotationParameters getRotationParameters(Entity entity);
}
