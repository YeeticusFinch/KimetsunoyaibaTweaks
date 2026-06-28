package com.lerdorf.kimetsunoyaibamultiplayer.util;

import com.lerdorf.kimetsunoyaibamultiplayer.config.EntityConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;

import java.util.function.Function;

/**
 * Shared helpers for quest familiars and other owned quest companions.
 */
public final class FamiliarEntityHelper {
    private static final String KASUGAI_CROW_PATH = "kasugai_crow";
    private static final String PRINCESS_PATH = "princess";
    private static final String OROCHI_PATH = "orochi";
    private static final String EYE_FAMILIAR_PATH = "eye_familiar";

    private static final int SAFE_SEARCH_RADIUS = 8;
    private static final int[] VERTICAL_OFFSETS = {0, 1, -1, 2, -2, 3, -3, 4, -4};

    private FamiliarEntityHelper() {
    }

    public static boolean isTrackedQuestFamiliar(Entity entity) {
        return isKasugaiCrow(entity)
            || hasEntityPath(entity, PRINCESS_PATH)
            || hasEntityPath(entity, OROCHI_PATH)
            || hasEntityPath(entity, EYE_FAMILIAR_PATH);
    }

    public static boolean isProtectedQuestFamiliar(Entity entity) {
        return isKasugaiCrow(entity)
            || hasEntityPath(entity, OROCHI_PATH)
            || hasEntityPath(entity, EYE_FAMILIAR_PATH);
    }

    public static boolean isKasugaiCrow(Entity entity) {
        return hasEntityPath(entity, KASUGAI_CROW_PATH);
    }

    public static boolean isOwnedQuestFamiliar(Entity entity) {
        if (!(entity instanceof TamableAnimal tamable) || !tamable.isTame() || tamable.getOwnerUUID() == null) {
            return false;
        }
        return isTrackedQuestFamiliar(entity);
    }

    public static boolean isDamageImmuneQuestFamiliar(Entity entity) {
        if (entity == null) {
            return false;
        }

        if (hasEntityPath(entity, KASUGAI_CROW_PATH)) {
            return EntityConfig.crowImmuneToDamage;
        }
        if (hasEntityPath(entity, OROCHI_PATH)) {
            return EntityConfig.orochiImmuneToDamage;
        }
        if (hasEntityPath(entity, EYE_FAMILIAR_PATH)) {
            return EntityConfig.eyeFamiliarImmuneToDamage;
        }
        return false;
    }

    public static BlockPos findNearestSafeTeleportPosition(Level level, BlockPos origin) {
        if (level == null || origin == null) {
            return null;
        }

        for (int radius = 0; radius <= SAFE_SEARCH_RADIUS; radius++) {
            for (BlockPos.MutableBlockPos mutable : BlockPos.spiralAround(origin, radius, Direction.EAST, Direction.SOUTH)) {
                BlockPos base = mutable.immutable();
                for (int verticalOffset : VERTICAL_OFFSETS) {
                    BlockPos candidate = base.offset(0, verticalOffset, 0);
                    if (!level.isInWorldBounds(candidate) || !level.isInWorldBounds(candidate.above())) {
                        continue;
                    }
                    if (isSafeGroundTeleportPos(level, candidate) || isSafeWaterTeleportPos(level, candidate)) {
                        return candidate;
                    }
                }
            }
        }

        return null;
    }

    public static Entity teleportOwnedQuestFamiliarToOwner(Entity familiar, ServerPlayer owner) {
        if (familiar == null || owner == null || !familiar.isAlive() || !isOwnedQuestFamiliar(familiar)) {
            return familiar;
        }

        ServerLevel targetLevel = owner.serverLevel();
        BlockPos targetPos = findNearestSafeTeleportPosition(targetLevel, owner.blockPosition());
        if (targetPos == null) {
            targetPos = owner.blockPosition();
        }

        double x = targetPos.getX() + 0.5D;
        double y = targetPos.getY();
        double z = targetPos.getZ() + 0.5D;

        Entity movedFamiliar = familiar;
        if (familiar.level() != targetLevel) {
            float yaw = familiar.getYRot();
            float pitch = familiar.getXRot();
            movedFamiliar = familiar.changeDimension(targetLevel, new ITeleporter() {
                @Override
                public Entity placeEntity(Entity entity, ServerLevel currentWorld, ServerLevel destWorld, float portalYaw, Function<Boolean, Entity> repositionEntity) {
                    Entity teleported = repositionEntity.apply(false);
                    teleported.moveTo(x, y, z, yaw, pitch);
                    teleported.setDeltaMovement(Vec3.ZERO);
                    teleported.fallDistance = 0.0F;
                    return teleported;
                }
            });
        } else {
            familiar.moveTo(x, y, z, familiar.getYRot(), familiar.getXRot());
            familiar.setDeltaMovement(Vec3.ZERO);
            familiar.fallDistance = 0.0F;
        }

        if (movedFamiliar == null) {
            return null;
        }

        if (movedFamiliar instanceof Mob mob) {
            mob.getNavigation().stop();
            mob.setTarget(null);
            mob.setNoGravity(false);
        }
        movedFamiliar.fallDistance = 0.0F;
        return movedFamiliar;
    }

    private static boolean hasEntityPath(Entity entity, String path) {
        if (entity == null) {
            return false;
        }
        ResourceLocation key = EntityType.getKey(entity.getType());
        return key != null && path.equals(key.getPath());
    }

    private static boolean isSafeGroundTeleportPos(Level level, BlockPos pos) {
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        BlockState floor = level.getBlockState(pos.below());

        return feet.getCollisionShape(level, pos).isEmpty()
            && head.getCollisionShape(level, pos.above()).isEmpty()
            && !floor.getCollisionShape(level, pos.below()).isEmpty()
            && !level.getFluidState(pos).is(FluidTags.WATER);
    }

    private static boolean isSafeWaterTeleportPos(Level level, BlockPos pos) {
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());

        return level.getFluidState(pos).is(FluidTags.WATER)
            && feet.getCollisionShape(level, pos).isEmpty()
            && head.getCollisionShape(level, pos.above()).isEmpty();
    }
}
