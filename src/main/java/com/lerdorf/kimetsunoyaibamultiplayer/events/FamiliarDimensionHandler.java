package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.CrowMirrorHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Handles teleporting familiars (tamed kasugai crows and princess dogs)
 * to their owner when the owner changes dimensions.
 */
@Mod.EventBusSubscriber
public class FamiliarDimensionHandler {

    private static final int TELEPORT_SCAN_RADIUS = 64;
    private static final int TELEPORT_FALLBACK_Y_OFFSET = 2;
    private static final double PLAYER_LONG_TELEPORT_DISTANCE_SQR = 50.0D * 50.0D;
    private static final Map<UUID, PlayerPositionSnapshot> LAST_PLAYER_POSITIONS = new HashMap<>();

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
        rememberPlayerPosition(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        PlayerPositionSnapshot previous = LAST_PLAYER_POSITIONS.get(player.getUUID());
        ResourceKey<Level> currentDimension = player.level().dimension();
        Vec3 currentPosition = player.position();
        if (previous != null) {
            boolean changedDimension = !previous.dimension.equals(currentDimension);
            boolean movedLongDistance = previous.position.distanceToSqr(currentPosition) > PLAYER_LONG_TELEPORT_DISTANCE_SQR;
            if (changedDimension || movedLongDistance) {
                teleportFamiliarsToPlayer(player);
            }
        }

        LAST_PLAYER_POSITIONS.put(player.getUUID(), new PlayerPositionSnapshot(currentDimension, currentPosition));
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_PLAYER_POSITIONS.remove(event.getEntity().getUUID());
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
            mob.getNavigation().stop();
            mob.setTarget(null);
        }

        double x = targetPos.getX() + 0.5D;
        double y = targetPos.getY();
        double z = targetPos.getZ() + 0.5D;
        boolean isCrow = isKasugaiCrow(familiar);
        Entity movedFamiliar = familiar;
        if (familiar.level() != targetLevel) {
            float yaw = familiar.getYRot();
            float pitch = familiar.getXRot();
            movedFamiliar = familiar.changeDimension(targetLevel, new ITeleporter() {
                @Override
                public Entity placeEntity(Entity entity, ServerLevel currentWorld, ServerLevel destWorld, float portalYaw, Function<Boolean, Entity> repositionEntity) {
                    Entity teleported = repositionEntity.apply(false);
                    teleported.moveTo(x, y, z, yaw, pitch);
                    return teleported;
                }
            });
        } else {
            familiar.teleportToWithTicket(x, y, z);
            familiar.moveTo(x, y, z, familiar.getYRot(), familiar.getXRot());
        }

        if (movedFamiliar == null) {
            return;
        }

        if (movedFamiliar instanceof Mob mob) {
            mob.setNoGravity(false);
            mob.getNavigation().stop();
        }
        movedFamiliar.fallDistance = 0.0F;

        if (isCrow) {
            CrowMirrorHandler.syncMirrorAfterCrowTeleport(movedFamiliar, targetLevel);
        }
    }

    private static boolean isKasugaiCrow(Entity entity) {
        String typeKey = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
        return typeKey.contains("kasugai_crow");
    }

    private static void rememberPlayerPosition(ServerPlayer player) {
        LAST_PLAYER_POSITIONS.put(player.getUUID(), new PlayerPositionSnapshot(player.level().dimension(), player.position()));
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

    private record PlayerPositionSnapshot(ResourceKey<Level> dimension, Vec3 position) {}
}
