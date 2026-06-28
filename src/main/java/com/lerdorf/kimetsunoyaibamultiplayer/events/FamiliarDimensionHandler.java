package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.entities.CrowMirrorHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.util.FamiliarEntityHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles teleporting familiars (tamed kasugai crows and quest companions)
 * to their owner when the owner changes dimensions.
 */
@Mod.EventBusSubscriber
public class FamiliarDimensionHandler {

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

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        var chunkPos = event.getChunk().getPos();
        BlockPos min = new BlockPos(chunkPos.getMinBlockX(), level.getMinBuildHeight(), chunkPos.getMinBlockZ());
        BlockPos max = new BlockPos(chunkPos.getMaxBlockX() + 1, level.getMaxBuildHeight() + 1, chunkPos.getMaxBlockZ() + 1);
        var searchBox = new net.minecraft.world.phys.AABB(min, max);

        List<Entity> familiars = new ArrayList<>(level.getEntities((Entity) null, searchBox, FamiliarEntityHelper::isOwnedQuestFamiliar));
        for (Entity familiar : familiars) {
            if (!(familiar instanceof TamableAnimal tamable) || tamable.getOwnerUUID() == null) {
                continue;
            }

            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(tamable.getOwnerUUID());
            if (owner == null) {
                continue;
            }

            Entity movedFamiliar = FamiliarEntityHelper.teleportOwnedQuestFamiliarToOwner(familiar, owner);
            if (movedFamiliar != null && FamiliarEntityHelper.isKasugaiCrow(movedFamiliar)) {
                CrowMirrorHandler.syncMirrorAfterCrowTeleport(movedFamiliar, owner.serverLevel());
            }
        }
    }

    private static void teleportFamiliarsToPlayer(ServerPlayer player) {
        // Search in the target level first (most likely the familiar is already there or needs to come here)
        // Also search in all levels to catch stragglers
        for (ServerLevel level : player.server.getAllLevels()) {
            List<Entity> familiars = findFamiliarsInLevel(level, player);
            for (Entity familiar : familiars) {
                Entity movedFamiliar = FamiliarEntityHelper.teleportOwnedQuestFamiliarToOwner(familiar, player);
                if (movedFamiliar != null && FamiliarEntityHelper.isKasugaiCrow(movedFamiliar)) {
                    CrowMirrorHandler.syncMirrorAfterCrowTeleport(movedFamiliar, player.serverLevel());
                }
            }
        }
    }

    private static List<Entity> findFamiliarsInLevel(ServerLevel level, ServerPlayer player) {
        var crowBounds = new net.minecraft.world.phys.AABB(
            -30_000_000, level.getMinBuildHeight(), -30_000_000,
            30_000_000, level.getMaxBuildHeight(), 30_000_000
        );

        return level.getEntities((Entity) null, crowBounds, entity -> {
            if (!FamiliarEntityHelper.isOwnedQuestFamiliar(entity)) {
                return false;
            }
            return player.getUUID().equals(((TamableAnimal) entity).getOwnerUUID()) && !entity.isPassenger();
        });
    }

    private static void rememberPlayerPosition(ServerPlayer player) {
        LAST_PLAYER_POSITIONS.put(player.getUUID(), new PlayerPositionSnapshot(player.level().dimension(), player.position()));
    }

    private record PlayerPositionSnapshot(ResourceKey<Level> dimension, Vec3 position) {}
}
