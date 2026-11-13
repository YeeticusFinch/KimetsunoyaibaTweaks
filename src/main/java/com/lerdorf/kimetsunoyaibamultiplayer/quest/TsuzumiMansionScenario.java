package com.lerdorf.kimetsunoyaibamultiplayer.quest;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.StructureMarkerEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.meditation.MeditationMenuService;
import com.lerdorf.kimetsunoyaibamultiplayer.raids.StructureLocationCache;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Runtime behavior for the Cruel quest line's Tsuzumi Mansion stage. */
public final class TsuzumiMansionScenario {
    public static final ResourceLocation STRUCTURE_ID = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID, "tsuzumi_mansion");

    private static final String PREFIX = "KnYTsuzumi";
    private static final String CENTER_X = PREFIX + "CenterX";
    private static final String CENTER_Y = PREFIX + "CenterY";
    private static final String CENTER_Z = PREFIX + "CenterZ";
    private static final String ROOMS_INITIALIZED = PREFIX + "RoomsInitialized";
    private static final String NPC_PREFIX = PREFIX + "Npc_";
    private static final String ROOM_PREFIX = PREFIX + "Room_";
    private static final int ROOM_COUNT = 13;
    private static final double MARKER_RADIUS = 512.0D;

    private static final ResourceLocation DEMON = id("kimetsunoyaiba", "demon");
    private static final ResourceLocation DEMON_TONGUE = id("kimetsunoyaiba", "demon_2");
    private static final ResourceLocation DEMON_HORNED = id("kimetsunoyaiba", "demon_3");
    private static final ResourceLocation KYOGAI = id("kimetsunoyaiba", "kyogai");

    private TsuzumiMansionScenario() {
    }

    private static ResourceLocation id(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    public static void startStage(ServerPlayer player, QuestRuntimeContext context) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        BlockPos center = getOrFindCenter(player, level);
        if (center == null) {
            return;
        }
        spawnSupportNpcs(level, center);
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
            "§6Mission No.3 - Tsuzumi Mansion: §fFind the mansion and rescue the children inside."));
    }

    public static void spawnRoomDemons(ServerPlayer player, QuestRuntimeContext context) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        BlockPos center = getOrFindCenter(player, level);
        if (center == null) {
            return;
        }

        int foundMarkers = 0;
        for (int room = 0; room < ROOM_COUNT; room++) {
            StructureMarkerEntity marker = findMarker(level, center, "room_" + room);
            if (marker == null) {
                continue;
            }
            foundMarkers++;
            String roomTag = ROOM_PREFIX + room;
            if (findTaggedEntity(level, center, roomTag) != null || hasNearbyMob(level, marker, 5.0D)) {
                continue;
            }

            ResourceLocation entityId = switch (room % 3) {
                case 1 -> DEMON_TONGUE;
                case 2 -> DEMON_HORNED;
                default -> DEMON;
            };
            spawnMob(level, entityId, marker.blockPosition(), roomTag, player);
        }
        if (foundMarkers == ROOM_COUNT) {
            player.getPersistentData().putBoolean(ROOMS_INITIALIZED, true);
        }
    }

    public static boolean areRoomDemonsDefeated(ServerPlayer player, QuestRuntimeContext context) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        BlockPos center = getStoredCenter(player);
        if (center == null || !player.getPersistentData().getBoolean(ROOMS_INITIALIZED)) {
            return false;
        }
        for (int room = 0; room < ROOM_COUNT; room++) {
            if (findTaggedEntity(level, center, ROOM_PREFIX + room) != null) {
                return false;
            }
        }
        return true;
    }

    public static void spawnKyogai(ServerPlayer player, QuestRuntimeContext context) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        BlockPos center = getOrFindCenter(player, level);
        if (center == null || findTaggedEntity(level, center, NPC_PREFIX + "kyogai") != null) {
            return;
        }
        StructureMarkerEntity marker = findMarker(level, center, "kyogai");
        if (marker == null) {
            marker = findMarker(level, center, "room_12");
        }
        if (marker == null) {
            marker = findMarker(level, center, "outside");
        }
        if (marker != null) {
            spawnMob(level, KYOGAI, marker.blockPosition(), NPC_PREFIX + "kyogai", player);
        }
    }

    public static boolean isKyogaiDefeated(ServerPlayer player, QuestRuntimeContext context) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        BlockPos center = getStoredCenter(player);
        return center != null && findTaggedEntity(level, center, NPC_PREFIX + "kyogai") == null;
    }

    public static BlockPos resolveOutsideMarker(ServerPlayer player, QuestRuntimeContext context) {
        return resolveMarker(player, "outside");
    }

    public static BlockPos resolveRoomMarker(ServerPlayer player, QuestRuntimeContext context) {
        if (!(player.level() instanceof ServerLevel level)) {
            return null;
        }
        BlockPos center = getStoredCenter(player);
        if (center == null) {
            return null;
        }
        for (int room = 0; room < ROOM_COUNT; room++) {
            Entity livingDemon = findTaggedEntity(level, center, ROOM_PREFIX + room);
            if (livingDemon != null) {
                return livingDemon.blockPosition();
            }
        }
        for (int room = 0; room < ROOM_COUNT; room++) {
            StructureMarkerEntity marker = findMarker(level, center, "room_" + room);
            if (marker != null) {
                return marker.blockPosition();
            }
        }
        return center;
    }

    public static BlockPos resolveKyogaiMarker(ServerPlayer player, QuestRuntimeContext context) {
        Entity kyogai = findTaggedEntity(player.level() instanceof ServerLevel level ? level : null,
            getStoredCenter(player), NPC_PREFIX + "kyogai");
        return kyogai == null ? resolveOutsideMarker(player, context) : kyogai.blockPosition();
    }

    /** Called before a stage restart so only named Tsuzumi NPCs are removed. */
    public static void cleanupForRestart(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        BlockPos center = getStoredCenter(player);
        if (center == null) {
            center = QuestScenarioActions.findNearestStructure(level, player.blockPosition(), STRUCTURE_ID);
        }
        if (center == null) {
            return;
        }
        AABB area = new AABB(center).inflate(MARKER_RADIUS);
        for (Entity entity : level.getEntities((Entity) null, area,
                candidate -> isTsuzumiNpc(candidate))) {
            entity.discard();
        }
    }

    /** Fails and restarts the shared stage for every player at the same mansion. */
    public static boolean failForPlayer(ServerPlayer deadPlayer, String reason) {
        if (!isActive(deadPlayer)) {
            return false;
        }
        List<ServerPlayer> involved = involvedPlayers(deadPlayer);
        teleportPlayersOutside(involved);
        QuestProgressionManager.restartCurrentQuestStage(
            deadPlayer, MeditationMenuService.resolveRoleForProgression(deadPlayer));
        for (ServerPlayer player : involved) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§cQuest Failed: §f" + (reason == null ? "A player died." : reason)
                    + " Restarting Tsuzumi Mansion."));
        }
        return true;
    }

    public static boolean isActive(ServerPlayer player) {
        return player != null
            && "cruel".equals(player.getPersistentData().getString("KnYActiveQuestGroup"))
            && player.getPersistentData().getInt("KnYQuestActiveStageIndex") == 2;
    }

    private static void spawnSupportNpcs(ServerLevel level, BlockPos center) {
        StructureMarkerEntity outside = findMarker(level, center, "outside");
        if (outside == null) {
            return;
        }
        ResourceLocation[] ids = {
            id("kimetsunoyaiba", "zennitsu"),
            id("kimetsunoyaiba", "inosuke"),
            id(KimetsunoyaibaMultiplayer.MODID, "shoichi"),
            id(KimetsunoyaibaMultiplayer.MODID, "teruko"),
            id(KimetsunoyaibaMultiplayer.MODID, "kiyoshi")
        };
        for (int i = 0; i < ids.length; i++) {
            String tag = NPC_PREFIX + npcName(ids[i]);
            if (findTaggedEntity(level, center, tag) == null) {
                BlockPos pos = outside.blockPosition().offset((i % 3) - 1, 0, i / 3 + 1);
                spawnMob(level, ids[i], pos, tag, null);
            }
        }
    }

    private static String npcName(ResourceLocation id) {
        return id.getPath();
    }

    private static LivingEntity spawnMob(ServerLevel level, ResourceLocation entityId, BlockPos pos,
                                         String questTag, ServerPlayer target) {
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(entityId);
        if (type == null) {
            System.err.println("[Tsuzumi] Missing entity type " + entityId);
            return null;
        }
        Entity entity = type.create(level);
        if (!(entity instanceof LivingEntity living)) {
            return null;
        }
        living.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
            level.random.nextFloat() * 360.0F, 0.0F);
        if (living instanceof Mob mob) {
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.MOB_SUMMONED, null, null);
            mob.setPersistenceRequired();
            if (target == null) {
                mob.setNoAi(true);
            } else {
                mob.setTarget(target);
            }
        }
        living.getPersistentData().putString(QuestScenarioActions.QUEST_NPC_ID_TAG, questTag);
        living.getPersistentData().putString(QuestScenarioActions.QUEST_TARGET_ID_TAG, questTag);
        level.addFreshEntity(living);
        return living;
    }

    private static boolean hasNearbyMob(ServerLevel level, Entity marker, double radius) {
        return !level.getEntitiesOfClass(Mob.class, marker.getBoundingBox().inflate(radius),
            mob -> mob.isAlive() && !isTsuzumiNpc(mob) && !isRoomDemon(mob)).isEmpty();
    }

    private static Entity findTaggedEntity(ServerLevel level, BlockPos center, String tag) {
        if (level == null || center == null) {
            return null;
        }
        return level.getEntities((Entity) null, new AABB(center).inflate(MARKER_RADIUS),
                entity -> entity.isAlive() && tag.equals(entity.getPersistentData()
                    .getString(QuestScenarioActions.QUEST_NPC_ID_TAG)))
            .stream().min(Comparator.comparingDouble(entity -> entity.distanceToSqr(
                center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D))).orElse(null);
    }

    private static boolean isTsuzumiNpc(Entity entity) {
        String tag = entity.getPersistentData().getString(QuestScenarioActions.QUEST_NPC_ID_TAG);
        return tag.startsWith(NPC_PREFIX);
    }

    private static boolean isRoomDemon(Entity entity) {
        String tag = entity.getPersistentData().getString(QuestScenarioActions.QUEST_NPC_ID_TAG);
        return tag.startsWith(ROOM_PREFIX);
    }

    private static StructureMarkerEntity findMarker(ServerLevel level, BlockPos center, String markerId) {
        if (level == null || center == null) {
            return null;
        }
        return level.getEntitiesOfClass(StructureMarkerEntity.class,
                new AABB(center).inflate(MARKER_RADIUS, 256.0D, MARKER_RADIUS),
                marker -> markerId.equals(marker.getMarkerId()))
            .stream().min(Comparator.comparingDouble(entity -> entity.distanceToSqr(
                center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D))).orElse(null);
    }

    private static BlockPos resolveMarker(ServerPlayer player, String markerId) {
        if (!(player.level() instanceof ServerLevel level)) {
            return null;
        }
        BlockPos center = getStoredCenter(player);
        StructureMarkerEntity marker = findMarker(level, center, markerId);
        return marker == null ? center : marker.blockPosition();
    }

    private static BlockPos getOrFindCenter(ServerPlayer player, ServerLevel level) {
        BlockPos stored = getStoredCenter(player);
        if (stored != null) {
            return stored;
        }
        BlockPos found = QuestScenarioActions.findNearestStructure(level, player.blockPosition(), STRUCTURE_ID);
        if (found != null) {
            player.getPersistentData().putInt(CENTER_X, found.getX());
            player.getPersistentData().putInt(CENTER_Y, found.getY());
            player.getPersistentData().putInt(CENTER_Z, found.getZ());
        }
        return found;
    }

    private static BlockPos getStoredCenter(ServerPlayer player) {
        if (player == null || !player.getPersistentData().contains(CENTER_X)) {
            return null;
        }
        return new BlockPos(player.getPersistentData().getInt(CENTER_X),
            player.getPersistentData().getInt(CENTER_Y), player.getPersistentData().getInt(CENTER_Z));
    }

    public static List<ServerPlayer> involvedPlayers(ServerPlayer source) {
        List<ServerPlayer> players = new ArrayList<>();
        if (source == null || source.server == null || !(source.level() instanceof ServerLevel level)) {
            return players;
        }
        BlockPos center = getStoredCenter(source);
        for (ServerPlayer candidate : level.players()) {
            if (!isActive(candidate)) {
                continue;
            }
            BlockPos candidateCenter = getStoredCenter(candidate);
            if (candidate == source || (center != null && candidateCenter != null
                && center.distSqr(candidateCenter) <= 128.0D * 128.0D)) {
                players.add(candidate);
            }
        }
        if (!players.contains(source)) {
            players.add(source);
        }
        return players;
    }

    public static void teleportPlayersOutside(List<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            teleportOutsideIfInside(player);
        }
    }

    private static void teleportOutsideIfInside(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level) || !isInside(player)) {
            return;
        }
        BlockPos center = getStoredCenter(player);
        BlockPos outside = findMarker(level, center, "outside") == null
            ? center : findMarker(level, center, "outside").blockPosition();
        if (outside != null) {
            player.teleportTo(outside.getX() + 0.5D, outside.getY(), outside.getZ() + 0.5D);
        }
    }

    private static boolean isInside(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        return StructureLocationCache.getStructureAt(level, player.blockPosition())
            .map(cached -> STRUCTURE_ID.equals(cached.structureId)).orElse(false);
    }
}
