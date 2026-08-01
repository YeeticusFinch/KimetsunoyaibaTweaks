package com.lerdorf.kimetsunoyaibamultiplayer.gravity.field;

import com.lerdorf.kimetsunoyaibamultiplayer.gravity.engine.RotationParameters;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record GravityField(
    UUID id,
    ResourceKey<Level> dimension,
    AABB box,
    Direction projectorFacing,
    Direction gravityDirection,
    int range,
    int width,
    int height,
    boolean enabled,
    double priority,
    BlockPos sourcePos
) implements GravityEffectSource {
    @Override
    public boolean affects(Entity entity) {
        return enabled && entity.level().dimension().equals(dimension) && box.intersects(entity.getBoundingBox());
    }

    @Override
    public Direction getDirection(Entity entity) {
        return gravityDirection;
    }

    @Override
    public double getPriority(Entity entity) {
        return priority;
    }

    @Override
    public @Nullable RotationParameters getRotationParameters(Entity entity) {
        return RotationParameters.getDefault();
    }
}
