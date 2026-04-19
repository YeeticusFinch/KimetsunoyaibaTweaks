package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Handles teleporting familiars (tamed kasugai crows and princess dogs)
 * to their owner when the owner changes dimensions.
 */
@Mod.EventBusSubscriber
public class FamiliarDimensionHandler {

    private static final int TELEPORT_SCAN_RADIUS = 64;
    private static final int TELEPORT_FALLBACK_Y_OFFSET = 2;

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ServerLevel targetLevel = player.serverLevel();
        if (targetLevel == null) {
            return;
        }

        // Find all tamed familiars in the player's current level (the level they're leaving)
        // Actually, at this point the player has already changed dimensions, so we need to
        // search in ALL levels for familiars owned by this player.
        teleportFamiliarsToPlayer(player);
    }

    private static void teleportFamiliarsToPlayer(ServerPlayer player) {
        ServerLevel targetLevel = player.serverLevel();
        BlockPos playerPos = player.blockPosition();

        // Search in the target level first (most likely the familiar is already there or needs to come here)
        // Also search in all levels to catch stragglers
        for (ServerLevel level : player.server.getAllLevels()) {
            List<Entity> familiars = findFamiliarsInLevel(level, player);
            for (Entity familiar : familiars) {
                teleportFamiliarToPlayer(familiar, player, targetLevel, playerPos);
            }
        }
    }

    private static List<Entity> findFamiliarsInLevel(ServerLevel level, ServerPlayer player) {
        // Search for kasugai crows owned by the player
        var crowBounds = new net.minecraft.world.phys.AABB(
            -30_000_000, level.getMinBuildHeight(), -30_000_000,
            30_000_000, level.getMaxBuildHeight(), 30_000_000
        );

        return level.getEntities((Entity) null, crowBounds, entity -> {
            if (!(entity instanceof TamableAnimal tamable) || !tamable.isTame()) {
                return false;
            }
            if (!player.getUUID().equals(tamable.getOwnerUUID())) {
                return false;
            }
            String typeKey = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
            return typeKey.contains("kasugai_crow") || entity.getType() == ModEntities.PRINCESS.get();
        });
    }

    private static void teleportFamiliarToPlayer(Entity familiar, ServerPlayer player, ServerLevel targetLevel, BlockPos playerPos) {
        if (familiar == null || !familiar.isAlive()) {
            return;
        }

        // If familiar is already in the same level and close enough, skip
        if (familiar.level() == targetLevel && familiar.distanceToSqr(player) < 16.0D) {
            return;
        }

        BlockPos targetPos = findSafeTeleportPosition(targetLevel, playerPos);
        if (targetPos == null) {
            targetPos = playerPos.above(TELEPORT_FALLBACK_Y_OFFSET);
        }

        Log.debug("[FamiliarDimension] Teleporting familiar {} to player {} in dimension {}",
            familiar.getType().toString(), player.getGameProfile().getName(), targetLevel.dimension().location());

        if (familiar instanceof Mob mob) {
            // Stop any active AI/goals
            mob.getNavigation().stop();
            mob.setTarget(null);
        }

        // Teleport the familiar
        familiar.teleportToWithTicket(targetPos.getX() + 0.5D, targetPos.getY(), targetPos.getZ() + 0.5D);

        // If the familiar is in a different dimension, we need to change its dimension
        if (familiar.level() != targetLevel) {
            familiar.changeDimension(targetLevel);
        }

        // Set familiar's position after dimension change
        familiar.moveTo(targetPos.getX() + 0.5D, targetPos.getY(), targetPos.getZ() + 0.5D,
            familiar.getYRot(), familiar.getXRot());

        // If it's a crow, make it fly near the player
        if (familiar instanceof Mob mob) {
            mob.setNoGravity(false);
        }
    }

    private static BlockPos findSafeTeleportPosition(ServerLevel level, BlockPos nearPos) {
        for (int dx = -TELEPORT_SCAN_RADIUS; dx <= TELEPORT_SCAN_RADIUS; dx += 4) {
            for (int dz = -TELEPORT_SCAN_RADIUS; dz <= TELEPORT_SCAN_RADIUS; dz += 4) {
                int x = nearPos.getX() + dx;
                int z = nearPos.getZ() + dz;
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockPos pos = new BlockPos(x, y, z);

                if (level.getBlockState(pos.below()).isSolid() && level.getBlockState(pos).isAir()) {
                    return pos;
                }
            }
        }
        return null;
    }
}
