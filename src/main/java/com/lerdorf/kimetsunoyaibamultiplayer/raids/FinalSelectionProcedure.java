package com.lerdorf.kimetsunoyaibamultiplayer.raids;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.TorilGateTeleportHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.config.DemonSlayerConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.config.FinalSelectionRaidConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.DemonSlayerEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import com.lerdorf.kimetsunoyaibamultiplayer.items.NichirinOreItem;
import com.lerdorf.kimetsunoyaibamultiplayer.items.NichirinSwordBlack;
import com.lerdorf.kimetsunoyaibamultiplayer.util.PlayerColorChangeStyleHelper;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

/**
 * Manages the Final Selection ceremony in the Mt Fujikasane dimension.
 *
 * When a player enters via the toril gate, this procedure:
 * 1. Sets time to sunset and pauses daylight cycle
 * 2. Spawns Kanata and Kiriya NPCs
 * 3. Plays dialogue sequence with timed pauses
 * 4. After dialogue, starts the Final Selection raid (to be implemented later)
 *
 * Only one procedure runs per dimension at a time.
 */
public class FinalSelectionProcedure {
    private static final String DATA_NAME = "final_selection_procedure";
    private static final String OWNED_ENTITY_TAG = "KnYMPFinalSelectionProcedureOwned";
    private static final String OWNED_ENTITY_ROLE_TAG = "KnYMPFinalSelectionProcedureRole";
    private static final String ROLE_KANATA = "kanata";
    private static final String ROLE_KIRIYA = "kiriya";
    private static final String ROLE_TRAINEE = "trainee";

    public static final ResourceKey<Level> MT_FUJIKASANE_KEY = ResourceKey.create(
        Registries.DIMENSION,
        new ResourceLocation("kimetsunoyaibamultiplayer", "mt_fujikasane")
    );

    public enum State {
        INITIAL_PAUSE,      // 10-second pause after spawning NPCs
        DIALOGUE_1,         // Kiriya speaks first line
        PAUSE_1,            // 5-second pause
        DIALOGUE_2,         // Kanata speaks second line
        PAUSE_2,            // 5-second pause
        DIALOGUE_3,         // Kiriya speaks third line
        PAUSE_3,            // 5-second pause
        DIALOGUE_4,         // Kanata speaks "Final selection begins now!"
        RAID_START,         // Start the final selection raid
        ACTIVE,             // Raid is running (placeholder for future implementation)
        RAID_END,           // Raid has finished running, all DemonSlayerEntity mobs should return to 308 80 736
        FINISHED            // Procedure complete
    }

    private State state = State.INITIAL_PAUSE;
    private final ServerLevel level;
    private long nextActionTime;
    private UUID kanataEntityId;
    private UUID kiriyaEntityId;
    private FinalSelectionRaid finalSelectionRaid;
    private final Set<UUID> traineeDemonSlayerIds = new HashSet<>();
    private final Map<UUID, BlockPos> traineeDestinations = new HashMap<>();
    private final Map<UUID, Long> traineeNextRepathTick = new HashMap<>();
    private final Set<UUID> raidEndCompletedPlayers = new HashSet<>();
    private final Map<UUID, Long> playerExitPromptCooldown = new HashMap<>();
    private final Map<UUID, Long> playerExitPendingUntilTick = new HashMap<>();
    private final Set<UUID> kakushiAcceptedPlayers = new HashSet<>();
    private final Map<UUID, Long> kakushiPromptCooldown = new HashMap<>();
    private final Map<UUID, Long> kakushiPendingUntilTick = new HashMap<>();
    private boolean raidSuccessWaypointIssued = false;
    private TraineeMode traineeMode = TraineeMode.IDLE;
    private boolean raidSucceeded = false;
    private long raidEndStartTime = 0L;

    // NPC spawn positions
    private static final BlockPos KANATA_POS = new BlockPos(307, 80, 713);
    private static final BlockPos KIRIYA_POS = new BlockPos(309, 80, 713);
    private static final BlockPos TRAINEE_STAGING_CENTER = new BlockPos(308, 80, 736);
    private static final BlockPos FINAL_SELECTION_RAID_CENTER = new BlockPos(141, 323, 86);
    private static final int FINAL_SELECTION_RAID_RADIUS = 300;
    private static final float NPC_YAW = 0.0f;
    private static final float NPC_PITCH = 0.0f;
    private static final int PLAYER_START_TRIGGER_DISTANCE = 15;
    private static final int TRAINEE_SPAWN_MIN = 20;
    private static final int TRAINEE_SPAWN_MAX = 30;
    private static final int TRAINEE_STAGING_RADIUS = 30;
    private static final int TRAINEE_EXISTING_SCAN_RADIUS = 96;
    private static final int TRAINEE_RAID_WANDER_RADIUS = 100;
    private static final double TRAINEE_MOVE_SPEED = 1.0D;
    private static final double TRAINEE_MOUNTAIN_PATH_SPEED_MIN_MULTIPLIER = 1.8D;
    private static final double TRAINEE_MOUNTAIN_PATH_SPEED_MAX_MULTIPLIER = 2.9D;
    private static final long TRAINEE_REPATH_INTERVAL = 40L;

    private enum TraineeMode {
        IDLE,
        MOVE_TO_RAID_AREA,
        RETURN_TO_STAGING
    }

    private record UniformSet(Item chest, Item leggings, Item boots) {}

    // Dialogue lines
    private static final String KIRIYA_LINE_1 = "Final Selection is a selective process for individuals who seek to become a recognized Demon Slayer of the Demon Slayer Corps";
    private static final String KANATA_LINE_2 = "The exam entails surviving seven consecutive nights on the mountaintop, relying solely on their own resources without external assistance.";
    private static final String KIRIYA_LINE_3 = "Candidates who manage to survive all seven nights successfully are granted the opportunity to join the esteemed ranks of the Demon Slayer Corps as official Demon Slayers.";
    private static final String KANATA_LINE_4 = "Final selection begins now!";
    private static final BlockPos FINAL_SELECTION_EXIT_POS = new BlockPos(307, 79, 775);
    private static final double EXIT_PROMPT_DISTANCE = 5.0D;
    private static final long EXIT_PROMPT_COOLDOWN_TICKS = 100L;
    private static final long EXIT_CONFIRM_TIMEOUT_TICKS = 600L;
    private static final long KANATA_WAYPOINT_REMINDER_TICKS = 200L;
    private static final long KAKUSHI_PROMPT_COOLDOWN_TICKS = 100L;
    private static final long KAKUSHI_CONFIRM_TIMEOUT_TICKS = 600L;
    private static final double NPC_EXISTING_MATCH_DISTANCE_SQR = 9.0D;
    private static final ResourceLocation DEMON_SLAYER_CORPS_ADVANCEMENT =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "demon_slayer_corps");
    private static final ResourceLocation COMPLETED_FINAL_SELECTION_ADVANCEMENT =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "completed_final_selectioni");
    private static final ResourceLocation KAKUSHI_ADVANCEMENT =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "kakushi");
    private static final ResourceLocation MIZUNOTO_ADVANCEMENT =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "mizunoto");
    private static final String FINAL_SELECTION_ORE_SELECTED_TAG = "KnYMPFinalSelectionOreSelected";
    private static final String FINAL_SELECTION_ORE_STYLE_TAG = "KnYMPFinalSelectionOreStyleId";

    // Singleton registry: one procedure per dimension (only mt_fujikasane)
    private static FinalSelectionProcedure activeProcedure = null;
    private boolean restoringFromSave = false;

    public FinalSelectionProcedure(ServerLevel level) {
        this.level = level;
        this.nextActionTime = level.getGameTime() + 200; // 10 seconds

        // Set time to sunset (12786 ticks = sunset start) and pause daylight cycle
        level.setDayTime(12786);

        // Store and pause the daylight cycle for this dimension only
        // We use the level's game rules which are per-server, so we track it ourselves
        MtFujikasaneDaylightController.pauseDaylightCycle(level);

        // Spawn NPCs
        spawnNPCs();

        Log.debug("[FinalSelection] Procedure started, NPCs spawned, time set to sunset");
    }

    private FinalSelectionProcedure(ServerLevel level, boolean restoringFromSave) {
        this.level = level;
        this.restoringFromSave = restoringFromSave;
    }

    private void spawnNPCs() {
        try {
            spawnOrReuseCeremonyNpc(ROLE_KANATA, KANATA_POS);
            spawnOrReuseCeremonyNpc(ROLE_KIRIYA, KIRIYA_POS);
            spawnTraineeDemonSlayers();
        } catch (Exception e) {
            Log.debug("[FinalSelection] Error spawning NPCs: " + e.getMessage());
        }
    }

    private void spawnOrReuseCeremonyNpc(String role, BlockPos spawnPos) {
        Entity existing = findOwnedCeremonyNpc(role, spawnPos);
        if (existing != null) {
            if (ROLE_KANATA.equals(role)) {
                kanataEntityId = existing.getUUID();
            } else if (ROLE_KIRIYA.equals(role)) {
                kiriyaEntityId = existing.getUUID();
            }
            markOwnedEntity(existing, role);
            Log.debug("[FinalSelection] Reused {} at {}", role, spawnPos);
            return;
        }

        var npc = ROLE_KANATA.equals(role)
            ? ModEntities.KANATA.get().create(level)
            : ModEntities.KIRIYA.get().create(level);
        if (npc == null) {
            return;
        }

        npc.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, NPC_YAW, NPC_PITCH);
        npc.setPersistenceRequired();
        npc.setGuardHomePosition(spawnPos);
        markOwnedEntity(npc, role);
        level.addFreshEntity(npc);

        if (ROLE_KANATA.equals(role)) {
            kanataEntityId = npc.getUUID();
        } else if (ROLE_KIRIYA.equals(role)) {
            kiriyaEntityId = npc.getUUID();
        }

        Log.debug("[FinalSelection] Spawned {} at {}", role, spawnPos);
    }

    private void spawnTraineeDemonSlayers() {
        if (collectExistingTrainees() > 0) {
            Log.debug("[FinalSelection] Reused {} existing trainee DemonSlayerEntity NPCs", traineeDemonSlayerIds.size());
            return;
        }

        int spawnCount = TRAINEE_SPAWN_MIN + level.random.nextInt(TRAINEE_SPAWN_MAX - TRAINEE_SPAWN_MIN + 1);
        double femaleChance = DemonSlayerConfig.getFemaleSpawnChance();

        for (int i = 0; i < spawnCount; i++) {
            try {
                boolean female = level.random.nextDouble() < femaleChance;
                EntityType<?> type = female
                    ? ModEntities.DEMON_SLAYER_FEMALE.get()
                    : ModEntities.DEMON_SLAYER.get();
                Entity created = type.create(level);
                if (!(created instanceof DemonSlayerEntity slayer)) continue;

                BlockPos spawnPos = findRandomSurfacePosition(TRAINEE_STAGING_CENTER, TRAINEE_STAGING_RADIUS);
                slayer.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, level.random.nextFloat() * 360.0f, 0.0f);
                slayer.configurePowerLevelLoadout(0);
                slayer.setPersistenceRequired();
                markOwnedEntity(slayer, ROLE_TRAINEE);

                if (level.addFreshEntity(slayer)) {
                    traineeDemonSlayerIds.add(slayer.getUUID());
                }
            } catch (Exception e) {
                Log.debug("[FinalSelection] Failed to spawn trainee DemonSlayerEntity: {}", e.getMessage());
            }
        }

        Log.debug("[FinalSelection] Spawned {} trainee DemonSlayerEntity NPCs", traineeDemonSlayerIds.size());
    }

    private int collectExistingTrainees() {
        traineeDemonSlayerIds.clear();

        AABB searchBounds = new AABB(TRAINEE_STAGING_CENTER).inflate(TRAINEE_EXISTING_SCAN_RADIUS);
        for (DemonSlayerEntity slayer : level.getEntitiesOfClass(DemonSlayerEntity.class, searchBounds, EntitySelector.ENTITY_STILL_ALIVE)) {
            if (!isProcedureTraineeCandidate(slayer)) {
                continue;
            }
            markOwnedEntity(slayer, ROLE_TRAINEE);
            traineeDemonSlayerIds.add(slayer.getUUID());
        }

        return traineeDemonSlayerIds.size();
    }

    private boolean isProcedureTraineeCandidate(DemonSlayerEntity slayer) {
        if (hasOwnedRole(slayer, ROLE_TRAINEE)) {
            return true;
        }
        if (slayer.getPowerLevel() > 0) {
            return false;
        }
        return slayer.blockPosition().distSqr(TRAINEE_STAGING_CENTER) <= (TRAINEE_EXISTING_SCAN_RADIUS * TRAINEE_EXISTING_SCAN_RADIUS);
    }

    private Entity findOwnedCeremonyNpc(String role, BlockPos expectedPos) {
        AABB searchBounds = new AABB(expectedPos).inflate(8.0D);
        for (Entity entity : level.getEntities((Entity) null, searchBounds, candidate -> candidate != null && candidate.isAlive())) {
            if (ROLE_KANATA.equals(role) && entity.getType() != ModEntities.KANATA.get()) {
                continue;
            }
            if (ROLE_KIRIYA.equals(role) && entity.getType() != ModEntities.KIRIYA.get()) {
                continue;
            }
            if (hasOwnedRole(entity, role) || entity.blockPosition().distSqr(expectedPos) <= NPC_EXISTING_MATCH_DISTANCE_SQR) {
                return entity;
            }
        }
        return null;
    }

    private static void markOwnedEntity(Entity entity, String role) {
        if (entity == null) {
            return;
        }
        entity.getPersistentData().putBoolean(OWNED_ENTITY_TAG, true);
        entity.getPersistentData().putString(OWNED_ENTITY_ROLE_TAG, role);
    }

    private static boolean hasOwnedRole(Entity entity, String role) {
        if (entity == null) {
            return false;
        }
        CompoundTag data = entity.getPersistentData();
        return data.getBoolean(OWNED_ENTITY_TAG) && role.equals(data.getString(OWNED_ENTITY_ROLE_TAG));
    }

    /**
     * Make NPCs face the nearest player every tick.
     */
    private void updateNPCFacing() {
        ServerPlayer nearestPlayer = findNearestPlayer();
        if (nearestPlayer == null) return;

        if (kanataEntityId != null) {
            var entity = level.getEntity(kanataEntityId);
            if (entity instanceof Mob mob) {
                mob.getLookControl().setLookAt(nearestPlayer, 30.0f, 30.0f);
            }
        }

        if (kiriyaEntityId != null) {
            var entity = level.getEntity(kiriyaEntityId);
            if (entity instanceof Mob mob) {
                mob.getLookControl().setLookAt(nearestPlayer, 30.0f, 30.0f);
            }
        }
    }

    private ServerPlayer findNearestPlayer() {
        ServerPlayer nearest = null;
        double nearestDistSq = Double.MAX_VALUE;
        BlockPos center = new BlockPos(308, 80, 713);

        for (ServerPlayer player : level.players()) {
            double distSq = player.blockPosition().distSqr(center);
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearest = player;
            }
        }
        return nearest;
    }

    public void tick() {
        long gameTime = level.getGameTime();

        // Keep NPCs facing players
        updateNPCFacing();
        updateTraineePathing(gameTime);
        processActiveKakushiOffers(gameTime);

        // During dialogue/pre-raid states, keep Mt Fujikasane at sunset.
        if (isPreRaidState(state)) {
            MtFujikasaneDaylightController.setPausedTime(level, 12786L);
        }

        if (isTimedDialogueState(state) && gameTime < nextActionTime) return;

        switch (state) {
            case INITIAL_PAUSE -> {
                if (!isAnyPlayerNearCeremonyNpc(PLAYER_START_TRIGGER_DISTANCE)) {
                    return;
                }

                // Kiriya speaks first line
                broadcastDialogue("\u00a7e[Kiriya]\u00a7f " + KIRIYA_LINE_1);
                state = State.DIALOGUE_1;
                nextActionTime = gameTime + 100; // 5 seconds
            }
            case DIALOGUE_1 -> {
                // Kanata speaks second line
                broadcastDialogue("\u00a7d[Kanata]\u00a7f " + KANATA_LINE_2);
                state = State.DIALOGUE_2;
                nextActionTime = gameTime + 100; // 5 seconds
            }
            case DIALOGUE_2 -> {
                // Kiriya speaks third line
                broadcastDialogue("\u00a7e[Kiriya]\u00a7f " + KIRIYA_LINE_3);
                state = State.DIALOGUE_3;
                nextActionTime = gameTime + 100; // 5 seconds
            }
            case DIALOGUE_3 -> {
                // Kanata speaks final line
                broadcastDialogue("\u00a7d[Kanata]\u00a76\u00a7l " + KANATA_LINE_4);
                state = State.DIALOGUE_4;
                nextActionTime = gameTime + 40; // 2-second pause before raid starts
            }
            case DIALOGUE_4 -> {
                state = State.RAID_START;
                nextActionTime = gameTime + 1;
            }
            case RAID_START -> {
                if (!FinalSelectionRaidConfig.enableFinalSelectionRaid.get()) {
                    broadcastDialogue("\u00a7cFinal Selection raid is disabled by config.");
                    state = State.FINISHED;
                } else {
                    finalSelectionRaid = new FinalSelectionRaid(level, FINAL_SELECTION_RAID_CENTER, FINAL_SELECTION_RAID_RADIUS);
                    traineeMode = TraineeMode.MOVE_TO_RAID_AREA;
                    traineeDestinations.clear();
                    traineeNextRepathTick.clear();
                    Log.debug("[FinalSelection] Dialogue complete. FinalSelectionRaid started.");
                    state = State.ACTIVE;
                }
            }
            case ACTIVE -> {
                if (!hasAnyAliveNonDemonPlayerInDimension()) {
                    if (finalSelectionRaid != null && !finalSelectionRaid.isFinished()) {
                        finalSelectionRaid.stop("All candidates left Final Selection");
                    }
                    state = State.FINISHED;
                    break;
                }

                if (finalSelectionRaid != null) {
                    finalSelectionRaid.tick();
                    if (finalSelectionRaid.isFinished()) {
                        raidSucceeded = finalSelectionRaid.wasSuccessful();
                        traineeMode = TraineeMode.RETURN_TO_STAGING;
                        registerAllAliveDemonSlayersForReturn();
                        traineeDestinations.clear();
                        traineeNextRepathTick.clear();
                        raidEndStartTime = gameTime;

                        if (raidSucceeded) {
                            broadcastDialogue("\u00a7aReturn to Kanata or Kiriya to complete Final Selection.");
                            sendKanataWaypointToAllPlayers(gameTime);
                        }

                        state = State.RAID_END;
                    }
                } else {
                    state = State.FINISHED;
                }
            }
            case RAID_END -> {
                if (raidSucceeded) {
                    registerAllAliveDemonSlayersForReturn();
                    sendKanataWaypointReminders(gameTime);
                    processRaidEndPlayerCompletions();
                    processExitPrompts(gameTime);
                    if (!hasAnyAliveNonDemonPlayerInDimension()) {
                        state = State.FINISHED;
                    }
                } else {
                    // Failed runs don't grant rewards; end once candidates have left or after a short grace period.
                    if (!hasAnyAliveNonDemonPlayerInDimension() || gameTime >= raidEndStartTime + 600L) {
                        state = State.FINISHED;
                    }
                }
            }
            default -> {}
        }
    }

    private boolean isPreRaidState(State state) {
        return state == State.INITIAL_PAUSE
            || state == State.DIALOGUE_1
            || state == State.DIALOGUE_2
            || state == State.DIALOGUE_3
            || state == State.DIALOGUE_4
            || state == State.RAID_START;
    }

    private boolean isTimedDialogueState(State state) {
        return state == State.INITIAL_PAUSE
            || state == State.DIALOGUE_1
            || state == State.DIALOGUE_2
            || state == State.DIALOGUE_3
            || state == State.DIALOGUE_4;
    }

    private boolean hasAnyAliveNonDemonPlayerInDimension() {
        for (ServerPlayer player : level.players()) {
            if (!isEligibleCandidate(player)) continue;
            return true;
        }
        return false;
    }

    private boolean isEligibleCandidate(ServerPlayer player) {
        return player != null
            && player.isAlive()
            && !player.getPersistentData().getBoolean("oni")
            && !kakushiAcceptedPlayers.contains(player.getUUID());
    }

    private boolean isAnyPlayerNearCeremonyNpc(double maxDistance) {
        double maxDistSq = maxDistance * maxDistance;
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive()) continue;
            if (isPlayerNearCeremonyNpc(player, maxDistSq)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPlayerNearCeremonyNpc(ServerPlayer player, double maxDistSq) {
        Entity kanata = kanataEntityId == null ? null : level.getEntity(kanataEntityId);
        Entity kiriya = kiriyaEntityId == null ? null : level.getEntity(kiriyaEntityId);

        if (kanata != null && kanata.isAlive() && player.distanceToSqr(kanata) <= maxDistSq) {
            return true;
        }
        if (kiriya != null && kiriya.isAlive() && player.distanceToSqr(kiriya) <= maxDistSq) {
            return true;
        }

        // Fallback to static positions if NPC entity lookup fails
        return player.blockPosition().distSqr(KANATA_POS) <= maxDistSq
            || player.blockPosition().distSqr(KIRIYA_POS) <= maxDistSq;
    }

    private void updateTraineePathing(long gameTime) {
        Iterator<UUID> it = traineeDemonSlayerIds.iterator();
        while (it.hasNext()) {
            UUID slayerId = it.next();
            Entity entity = level.getEntity(slayerId);
            if (!(entity instanceof DemonSlayerEntity slayer) || !slayer.isAlive()) {
                it.remove();
                traineeDestinations.remove(slayerId);
                traineeNextRepathTick.remove(slayerId);
                continue;
            }

            if (traineeMode == TraineeMode.IDLE) {
                slayer.setFinalSelectionPathingActive(false);
                slayer.setFinalSelectionPathSpeed(0.0D);
                continue;
            }

            LivingEntity currentTarget = slayer.getTarget();
            if (currentTarget != null && currentTarget.isAlive()) {
                if (traineeMode == TraineeMode.MOVE_TO_RAID_AREA) {
                    // During mountain ascent, pathing takes priority over combat.
                    slayer.setTarget(null);
                    slayer.setLastHurtByMob(null);
                } else {
                    slayer.setFinalSelectionPathingActive(false);
                    slayer.setFinalSelectionPathSpeed(0.0D);
                    continue; // Let combat AI take over while aggroed
                }
            }

            BlockPos destination = traineeDestinations.get(slayerId);
            boolean reached = destination != null && slayer.blockPosition().distSqr(destination) <= 9.0D;
            boolean needsDestination = destination == null || reached;
            long nextRepath = traineeNextRepathTick.getOrDefault(slayerId, 0L);

            if (!needsDestination && gameTime < nextRepath) {
                continue;
            }

            if (needsDestination) {
                destination = switch (traineeMode) {
                    case MOVE_TO_RAID_AREA -> findRandomSurfacePosition(FINAL_SELECTION_RAID_CENTER, TRAINEE_RAID_WANDER_RADIUS);
                    case RETURN_TO_STAGING -> findRandomSurfacePosition(TRAINEE_STAGING_CENTER, TRAINEE_STAGING_RADIUS);
                    default -> null;
                };
                if (destination == null) {
                    continue;
                }
                traineeDestinations.put(slayerId, destination);
            }

            double moveSpeed = getTraineeMoveSpeedForMode();
            slayer.setFinalSelectionPathingActive(true);
            slayer.setFinalSelectionPathSpeed(moveSpeed);
            slayer.getNavigation().moveTo(destination.getX() + 0.5, destination.getY(), destination.getZ() + 0.5, moveSpeed);
            traineeNextRepathTick.put(slayerId, gameTime + TRAINEE_REPATH_INTERVAL);
        }
    }

    private double getTraineeMoveSpeedForMode() {
        if (traineeMode == TraineeMode.MOVE_TO_RAID_AREA) {
            // Add a small speed variance so trainees don't move in perfectly uniform packs.
            // Guard against invalid tuning (negative/zero or swapped min/max multipliers).
            double minMultiplier = Math.max(0.05D, Math.min(TRAINEE_MOUNTAIN_PATH_SPEED_MIN_MULTIPLIER, TRAINEE_MOUNTAIN_PATH_SPEED_MAX_MULTIPLIER));
            double maxMultiplier = Math.max(minMultiplier, Math.max(TRAINEE_MOUNTAIN_PATH_SPEED_MIN_MULTIPLIER, TRAINEE_MOUNTAIN_PATH_SPEED_MAX_MULTIPLIER));
            double multiplierRange = maxMultiplier - minMultiplier;
            double randomMultiplier = minMultiplier + (level.random.nextDouble() * multiplierRange);
            return TRAINEE_MOVE_SPEED * randomMultiplier;
        }
        return TRAINEE_MOVE_SPEED;
    }

    private BlockPos findRandomSurfacePosition(BlockPos anchor, int radius) {
        for (int attempt = 0; attempt < 20; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0;
            int dist = level.random.nextInt(Math.max(1, radius + 1));
            int x = (int) Math.round(anchor.getX() + Math.cos(angle) * dist);
            int z = (int) Math.round(anchor.getZ() + Math.sin(angle) * dist);

            BlockPos test = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, anchor.getY(), z));
            if (isValidSurfacePosition(test)) {
                return test;
            }
        }

        BlockPos fallback = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, anchor);
        return isValidSurfacePosition(fallback) ? fallback : fallback.above();
    }

    private boolean isValidSurfacePosition(BlockPos pos) {
        BlockPos below = pos.below();
        if (!level.getBlockState(below).isSolidRender(level, below)) return false;
        if (!level.getBlockState(pos).isAir()) return false;
        if (!level.getBlockState(pos.above()).isAir()) return false;
        if (level.getBlockState(pos).getFluidState().isSource()) return false;
        return true;
    }

    private void processRaidEndPlayerCompletions() {
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive()) continue;
            if (player.getPersistentData().getBoolean("oni")) continue;
            if (raidEndCompletedPlayers.contains(player.getUUID())) continue;
            if (!isPlayerNearCeremonyNpc(player, PLAYER_START_TRIGGER_DISTANCE * PLAYER_START_TRIGGER_DISTANCE)) continue;

            completeRaidEndForPlayer(player);
        }
    }

    private void completeRaidEndForPlayer(ServerPlayer player) {
        raidEndCompletedPlayers.add(player.getUUID());

        String npcName = level.random.nextBoolean() ? "Kanata" : "Kiriya";
        player.sendSystemMessage(Component.literal("[" + npcName + "] Congratulations " + player.getName().getString()
            + ", you have survived final selection, you have been issued a Demon Slayer Uniform and a Kasugai crow. Please select an ore for your new nichirin blade."));

        spawnAndTameKasugaiCrow(player);
        grantRandomUniformSet(player);
        awardAdvancement(player, COMPLETED_FINAL_SELECTION_ADVANCEMENT);
        awardAdvancement(player, MIZUNOTO_ADVANCEMENT);
        runOreSelectionProcedurePlaceholder(player);
    }

    private void registerAllAliveDemonSlayersForReturn() {
        AABB worldBounds = new AABB(
            -30_000_000.0D, level.getMinBuildHeight(), -30_000_000.0D,
            30_000_000.0D, level.getMaxBuildHeight(), 30_000_000.0D
        );

        for (DemonSlayerEntity slayer : level.getEntitiesOfClass(DemonSlayerEntity.class, worldBounds, EntitySelector.ENTITY_STILL_ALIVE)) {
            traineeDemonSlayerIds.add(slayer.getUUID());
        }
    }

    private void sendKanataWaypointToAllPlayers(long gameTime) {
        if (raidSuccessWaypointIssued) {
            return;
        }
        raidSuccessWaypointIssued = true;

        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(Component.literal("\u00a7bKanata is at 307 ~ 713"));
            playerExitPromptCooldown.put(player.getUUID(), gameTime);
        }
    }

    private void sendKanataWaypointReminders(long gameTime) {
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.getPersistentData().getBoolean("oni")) {
                continue;
            }
            if (raidEndCompletedPlayers.contains(player.getUUID())) {
                continue;
            }

            long lastSent = playerExitPromptCooldown.getOrDefault(player.getUUID(), Long.MIN_VALUE);
            if (gameTime - lastSent < KANATA_WAYPOINT_REMINDER_TICKS) {
                continue;
            }
            player.sendSystemMessage(Component.literal("\u00a7bKanata is at 307 ~ 713"));
            playerExitPromptCooldown.put(player.getUUID(), gameTime);
        }
    }

    private static void sendExitWaypoint(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("\u00a7bFinal Selection Exit is at 307 ~ 775"));
    }

    private void processExitPrompts(long gameTime) {
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.getPersistentData().getBoolean("oni")) {
                continue;
            }
            if (!raidEndCompletedPlayers.contains(player.getUUID())) {
                continue;
            }

            if (!hasCompletedOreSelection(player)) {
                if (isPlayerNearExit(player)) {
                    long lastSent = playerExitPromptCooldown.getOrDefault(player.getUUID(), Long.MIN_VALUE);
                    if (gameTime - lastSent >= KANATA_WAYPOINT_REMINDER_TICKS) {
                        player.sendSystemMessage(Component.literal("\u00a76[Final Selection] Choose your Scarlet ore before leaving."));
                        playerExitPromptCooldown.put(player.getUUID(), gameTime);
                    }
                }
                continue;
            }

            long pendingUntil = playerExitPendingUntilTick.getOrDefault(player.getUUID(), 0L);
            if (pendingUntil > 0L && gameTime > pendingUntil) {
                playerExitPendingUntilTick.remove(player.getUUID());
            }

            if (!isPlayerNearExit(player)) {
                continue;
            }

            if (pendingUntil > 0L && gameTime <= pendingUntil) {
                continue;
            }

            long cooldownUntil = playerExitPromptCooldown.getOrDefault(player.getUUID(), 0L);
            if (gameTime < cooldownUntil) {
                continue;
            }

            promptExitConfirmation(player, gameTime);
        }
    }

    private void processActiveKakushiOffers(long gameTime) {
        if (state != State.ACTIVE || finalSelectionRaid == null || finalSelectionRaid.isFinished()) {
            return;
        }

        for (ServerPlayer player : level.players()) {
            if (!isEligibleCandidate(player)) {
                continue;
            }
            if (!isPlayerNearCeremonyNpc(player, PLAYER_START_TRIGGER_DISTANCE * PLAYER_START_TRIGGER_DISTANCE)) {
                continue;
            }
            if (finalSelectionRaid.isWithinRaidArea(player.getX(), player.getZ())) {
                continue;
            }

            long pendingUntil = kakushiPendingUntilTick.getOrDefault(player.getUUID(), 0L);
            if (pendingUntil > 0L && gameTime > pendingUntil) {
                kakushiPendingUntilTick.remove(player.getUUID());
            }
            if (pendingUntil > 0L && gameTime <= pendingUntil) {
                continue;
            }

            long cooldownUntil = kakushiPromptCooldown.getOrDefault(player.getUUID(), 0L);
            if (gameTime < cooldownUntil) {
                continue;
            }

            promptKakushiOffer(player, gameTime);
        }
    }

    private void promptKakushiOffer(ServerPlayer player, long gameTime) {
        kakushiPromptCooldown.put(player.getUUID(), gameTime + KAKUSHI_PROMPT_COOLDOWN_TICKS);
        kakushiPendingUntilTick.put(player.getUUID(), gameTime + KAKUSHI_CONFIRM_TIMEOUT_TICKS);

        String npcName = level.random.nextBoolean() ? "Kanata" : "Kiriya";
        player.sendSystemMessage(Component.literal("[" + npcName + "] You have failed Final Selection, but you may still serve the Demon Slayer Corps as a Kakushi. Will you accept?"));

        Component yes = Component.literal("\u00a7a\u00a7l[Yes]")
            .withStyle(style -> style.withClickEvent(new net.minecraft.network.chat.ClickEvent(
                net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/finalselection kakushi accept")));
        Component no = Component.literal("\u00a7c\u00a7l[No]")
            .withStyle(style -> style.withClickEvent(new net.minecraft.network.chat.ClickEvent(
                net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/finalselection kakushi decline")));
        player.sendSystemMessage(Component.literal("\u00a76\u00a7l[Final Selection] ").append(yes).append(Component.literal(" ")).append(no));
    }

    private boolean isPlayerNearExit(ServerPlayer player) {
        double dx = player.getX() - (FINAL_SELECTION_EXIT_POS.getX() + 0.5D);
        double dz = player.getZ() - (FINAL_SELECTION_EXIT_POS.getZ() + 0.5D);
        return (dx * dx + dz * dz) <= (EXIT_PROMPT_DISTANCE * EXIT_PROMPT_DISTANCE);
    }

    private void promptExitConfirmation(ServerPlayer player, long gameTime) {
        playerExitPromptCooldown.put(player.getUUID(), gameTime + EXIT_PROMPT_COOLDOWN_TICKS);
        playerExitPendingUntilTick.put(player.getUUID(), gameTime + EXIT_CONFIRM_TIMEOUT_TICKS);

        Component prefix = Component.literal("\u00a76\u00a7l[Final Selection] \u00a7eLeave Mt. Fujikasane?");
        Component yes = Component.literal("\u00a7a\u00a7l[Yes]")
            .withStyle(style -> style.withClickEvent(new net.minecraft.network.chat.ClickEvent(
                net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/finalselection leave confirm")));
        Component no = Component.literal("\u00a7c\u00a7l[No]")
            .withStyle(style -> style.withClickEvent(new net.minecraft.network.chat.ClickEvent(
                net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/finalselection leave cancel")));
        player.sendSystemMessage(prefix.copy().append(Component.literal(" ")).append(yes).append(Component.literal(" ")).append(no));
    }

    private void awardAdvancement(ServerPlayer player, ResourceLocation advancementId) {
        if (player.getServer() == null) {
            return;
        }

        Advancement advancement = player.server.getAdvancements().getAdvancement(advancementId);
        if (advancement == null) {
            Log.debug("[FinalSelection] Missing advancement definition: {}", advancementId);
            return;
        }

        if (player.getAdvancements().getOrStartProgress(advancement).isDone()) {
            return;
        }

        List<String> remainingCriteria = new ArrayList<>();
        for (String criterion : player.getAdvancements().getOrStartProgress(advancement).getRemainingCriteria()) {
            remainingCriteria.add(criterion);
        }
        for (String criterion : remainingCriteria) {
            player.getAdvancements().award(advancement, criterion);
        }
    }

    private void spawnAndTameKasugaiCrow(ServerPlayer player) {
        try {
            EntityType<?> crowType = ForgeRegistries.ENTITY_TYPES.getValue(
                ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "kasugai_crow")
            );
            if (crowType == null) {
                Log.debug("[FinalSelection] Could not find kasugai crow entity type");
                return;
            }

            Entity crow = crowType.create(level);
            if (crow == null) {
                return;
            }

            BlockPos spawnPos = findRandomSurfacePosition(player.blockPosition(), 8);
            if (crow instanceof Mob mob) {
                mob.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, level.random.nextFloat() * 360.0f, 0.0f);
                mob.setPersistenceRequired();
            } else {
                crow.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
            }

            if (crow instanceof TamableAnimal tamable) {
                tamable.tame(player);
                tamable.setOwnerUUID(player.getUUID());
                tamable.setPersistenceRequired();
            }

            level.addFreshEntity(crow);
        } catch (Exception e) {
            Log.debug("[FinalSelection] Failed to spawn/tame kasugai crow: {}", e.getMessage());
        }
    }

    private void grantRandomUniformSet(ServerPlayer player) {
        List<UniformSet> options = new ArrayList<>();

        Item baseChest = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "uniform_chestplate"));
        Item baseLeggings = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "uniform_leggings"));
        Item baseBoots = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "uniform_boots"));
        if (baseChest != null && baseLeggings != null && baseBoots != null) {
            options.add(new UniformSet(baseChest, baseLeggings, baseBoots));
        }

        options.add(new UniformSet(
            ModItems.SLAYER_UNIFORM_2_CHESTPLATE.get(),
            ModItems.SLAYER_UNIFORM_2_LEGGINGS.get(),
            ModItems.SLAYER_UNIFORM_2_BOOTS.get()
        ));

        options.add(new UniformSet(
            ModItems.SLAYER_UNIFORM_2_CHESTPLATE_PURPLE.get(),
            ModItems.SLAYER_UNIFORM_2_LEGGINGS_PURPLE.get(),
            ModItems.SLAYER_UNIFORM_2_BOOTS_PURPLE.get()
        ));

        options.add(new UniformSet(
            ModItems.PURPLE_DEMON_SLAYER_UNIFORM_CHESTPLATE.get(),
            ModItems.PURPLE_DEMON_SLAYER_UNIFORM_LEGGINGS.get(),
            ModItems.PURPLE_DEMON_SLAYER_UNIFORM_BOOTS.get()
        ));

        if (options.isEmpty()) return;

        UniformSet chosen = options.get(level.random.nextInt(options.size()));
        givePlayerItemOrDrop(player, new ItemStack(chosen.chest()));
        givePlayerItemOrDrop(player, new ItemStack(chosen.leggings()));
        givePlayerItemOrDrop(player, new ItemStack(chosen.boots()));
    }

    private void givePlayerItemOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private void runOreSelectionProcedurePlaceholder(ServerPlayer player) {
        openOreSelectionMenu(player, selectedStack -> completeOreSelection(player, selectedStack));
    }

    public static void openStandaloneOreSelection(ServerPlayer player) {
        openOreSelectionMenu(player, selectedStack -> finishOreSelection(player, selectedStack));
    }

    private static void openOreSelectionMenu(ServerPlayer player, java.util.function.Consumer<ItemStack> onSelection) {
        player.getPersistentData().remove(FINAL_SELECTION_ORE_SELECTED_TAG);
        player.getPersistentData().remove(FINAL_SELECTION_ORE_STYLE_TAG);

        var assignedStyle = PlayerColorChangeStyleHelper.resolveOrAssignColorChangeStyle(player);
        if (assignedStyle == null) {
            Log.debug("[FinalSelection] No color-change style could be resolved for {}", player.getGameProfile().getName());
            player.getPersistentData().putBoolean(FINAL_SELECTION_ORE_SELECTED_TAG, true);
            sendExitWaypoint(player);
            return;
        }

        String assignedStyleId = assignedStyle.getStyleId();
        boolean blackSwordFamily = "black".equals(assignedStyleId);
        if (blackSwordFamily) {
            assignedStyleId = NichirinSwordBlack.resolveOrAssignPlayerStyle(player, player.getRandom());
            if (assignedStyleId == null || assignedStyleId.isEmpty()) {
                Log.debug("[FinalSelection] No remembered black sword style could be resolved for {}", player.getGameProfile().getName());
                player.getPersistentData().putBoolean(FINAL_SELECTION_ORE_SELECTED_TAG, true);
                sendExitWaypoint(player);
                return;
            }
        }

        List<com.lerdorf.kimetsunoyaibamultiplayer.api.StyleMetadataRegistry.StyleMetadata> oreChoices =
            PlayerColorChangeStyleHelper.getFamilyOreSelectionStyles(assignedStyleId);

        if (oreChoices.isEmpty()) {
            Log.debug("[FinalSelection] No ore-selection styles found for {}", assignedStyleId);
            player.getPersistentData().putBoolean(FINAL_SELECTION_ORE_SELECTED_TAG, true);
            player.getPersistentData().putString(FINAL_SELECTION_ORE_STYLE_TAG, assignedStyleId);
            sendExitWaypoint(player);
            return;
        }

        OreSelectionContainer container = new OreSelectionContainer(player, onSelection);
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 22, 4};
        for (int i = 0; i < oreChoices.size() && i < slots.length; i++) {
            String styleId = oreChoices.get(i).getStyleId();
            ItemStack oreStack = blackSwordFamily && styleId.equals(assignedStyleId)
                ? NichirinOreItem.createBlackForStyle(ModItems.NICHIRIN_ORE.get(), styleId)
                : NichirinOreItem.createForStyle(ModItems.NICHIRIN_ORE.get(), styleId);
            container.setItem(slots[i], oreStack);
        }

        player.openMenu(new SimpleMenuProvider(
            (containerId, inventory, menuPlayer) -> ChestMenu.threeRows(containerId, inventory, container),
            Component.literal("Select Scarlet Ore")
        ));
    }

    private void completeOreSelection(ServerPlayer player, ItemStack selectedStack) {
        finishOreSelection(player, selectedStack);
    }

    private static void finishOreSelection(ServerPlayer player, ItemStack selectedStack) {
        if (hasCompletedOreSelection(player)) {
            return;
        }

        String styleId = NichirinOreItem.getStyleId(selectedStack);
        if (styleId == null || styleId.isEmpty()) {
            styleId = PlayerColorChangeStyleHelper.getAssignedColorChangeStyleId(player);
        }

        player.getPersistentData().putBoolean(FINAL_SELECTION_ORE_SELECTED_TAG, true);
        if (styleId != null && !styleId.isEmpty()) {
            player.getPersistentData().putString(FINAL_SELECTION_ORE_STYLE_TAG, styleId);
        }

        player.sendSystemMessage(Component.literal("\u00a7aYou selected " + selectedStack.getHoverName().getString() + "."));
        sendExitWaypoint(player);
    }

    private static boolean hasCompletedOreSelection(ServerPlayer player) {
        return player.getPersistentData().getBoolean(FINAL_SELECTION_ORE_SELECTED_TAG);
    }

    /**
     * Send a chat message to all players in the Mt Fujikasane dimension.
     */
    private void broadcastDialogue(String message) {
        Component component = Component.literal(message);
        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(component);
        }
    }

    public boolean isFinished() {
        return state == State.FINISHED;
    }

    public State getState() {
        return state;
    }

    /**
     * Clean up NPCs when procedure ends or is cancelled.
     */
    public void cleanup() {
        if (finalSelectionRaid != null) {
            finalSelectionRaid.cleanup();
            finalSelectionRaid = null;
        }

        for (UUID slayerId : new HashSet<>(traineeDemonSlayerIds)) {
            Entity entity = level.getEntity(slayerId);
            if (entity != null) {
                if (entity instanceof DemonSlayerEntity slayer) {
                    slayer.setFinalSelectionPathingActive(false);
                    slayer.setFinalSelectionPathSpeed(0.0D);
                }
                entity.discard();
            }
        }
        traineeDemonSlayerIds.clear();
        traineeDestinations.clear();
        traineeNextRepathTick.clear();
        raidEndCompletedPlayers.clear();
        playerExitPromptCooldown.clear();
        playerExitPendingUntilTick.clear();
        kakushiAcceptedPlayers.clear();
        kakushiPromptCooldown.clear();
        kakushiPendingUntilTick.clear();
        traineeMode = TraineeMode.IDLE;
        raidSuccessWaypointIssued = false;

        cleanupOwnedProcedureEntities();
        kanataEntityId = null;
        kiriyaEntityId = null;

        // Resume daylight cycle if still paused
        MtFujikasaneDaylightController.resumeDaylightCycle(level);

        Log.debug("[FinalSelection] Procedure cleaned up");
    }

    public void onEntityKilled(UUID entityId) {
        if (finalSelectionRaid != null) {
            finalSelectionRaid.onEntityKilled(entityId);
        }
    }

    public boolean isRaidEntity(UUID entityId) {
        return finalSelectionRaid != null && finalSelectionRaid.isRaidEntity(entityId);
    }

    private CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("State", state.name());
        tag.putLong("NextActionTime", nextActionTime);
        if (kanataEntityId != null) tag.putUUID("KanataEntityId", kanataEntityId);
        if (kiriyaEntityId != null) tag.putUUID("KiriyaEntityId", kiriyaEntityId);
        tag.putBoolean("RaidSucceeded", raidSucceeded);
        tag.putBoolean("RaidSuccessWaypointIssued", raidSuccessWaypointIssued);
        tag.putLong("RaidEndStartTime", raidEndStartTime);
        tag.putString("TraineeMode", traineeMode.name());
        tag.putBoolean("RestoringFromSave", restoringFromSave);

        ListTag traineeIds = new ListTag();
        for (UUID id : traineeDemonSlayerIds) traineeIds.add(net.minecraft.nbt.StringTag.valueOf(id.toString()));
        tag.put("TraineeDemonSlayerIds", traineeIds);

        ListTag completedPlayers = new ListTag();
        for (UUID id : raidEndCompletedPlayers) completedPlayers.add(net.minecraft.nbt.StringTag.valueOf(id.toString()));
        tag.put("RaidEndCompletedPlayers", completedPlayers);

        ListTag kakushiAccepted = new ListTag();
        for (UUID id : kakushiAcceptedPlayers) kakushiAccepted.add(net.minecraft.nbt.StringTag.valueOf(id.toString()));
        tag.put("KakushiAcceptedPlayers", kakushiAccepted);

        ListTag destinations = new ListTag();
        for (Map.Entry<UUID, BlockPos> entry : traineeDestinations.entrySet()) {
            CompoundTag d = new CompoundTag();
            d.putString("EntityId", entry.getKey().toString());
            d.putInt("X", entry.getValue().getX());
            d.putInt("Y", entry.getValue().getY());
            d.putInt("Z", entry.getValue().getZ());
            destinations.add(d);
        }
        tag.put("TraineeDestinations", destinations);

        ListTag repath = new ListTag();
        for (Map.Entry<UUID, Long> entry : traineeNextRepathTick.entrySet()) {
            CompoundTag r = new CompoundTag();
            r.putString("EntityId", entry.getKey().toString());
            r.putLong("Tick", entry.getValue());
            repath.add(r);
        }
        tag.put("TraineeNextRepathTick", repath);

        if (finalSelectionRaid != null) {
            tag.put("FinalSelectionRaid", finalSelectionRaid.toTag());
        }

        return tag;
    }

    private static FinalSelectionProcedure fromTag(ServerLevel level, CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return null;
        }

        FinalSelectionProcedure procedure = new FinalSelectionProcedure(level, true);
        procedure.state = parseEnum(tag.getString("State"), State.FINISHED, State.class);
        procedure.nextActionTime = tag.getLong("NextActionTime");
        if (tag.hasUUID("KanataEntityId")) procedure.kanataEntityId = tag.getUUID("KanataEntityId");
        if (tag.hasUUID("KiriyaEntityId")) procedure.kiriyaEntityId = tag.getUUID("KiriyaEntityId");
        procedure.raidSucceeded = tag.getBoolean("RaidSucceeded");
        procedure.raidSuccessWaypointIssued = tag.getBoolean("RaidSuccessWaypointIssued");
        procedure.raidEndStartTime = tag.getLong("RaidEndStartTime");
        procedure.traineeMode = parseEnum(tag.getString("TraineeMode"), TraineeMode.IDLE, TraineeMode.class);
        procedure.restoringFromSave = tag.getBoolean("RestoringFromSave");

        ListTag traineeIds = tag.getList("TraineeDemonSlayerIds", Tag.TAG_STRING);
        for (int i = 0; i < traineeIds.size(); i++) {
            try {
                procedure.traineeDemonSlayerIds.add(UUID.fromString(traineeIds.getString(i)));
            } catch (Exception ignored) {}
        }

        ListTag completedPlayers = tag.getList("RaidEndCompletedPlayers", Tag.TAG_STRING);
        for (int i = 0; i < completedPlayers.size(); i++) {
            try {
                procedure.raidEndCompletedPlayers.add(UUID.fromString(completedPlayers.getString(i)));
            } catch (Exception ignored) {}
        }

        ListTag kakushiAccepted = tag.getList("KakushiAcceptedPlayers", Tag.TAG_STRING);
        for (int i = 0; i < kakushiAccepted.size(); i++) {
            try {
                procedure.kakushiAcceptedPlayers.add(UUID.fromString(kakushiAccepted.getString(i)));
            } catch (Exception ignored) {}
        }

        ListTag destinations = tag.getList("TraineeDestinations", Tag.TAG_COMPOUND);
        for (int i = 0; i < destinations.size(); i++) {
            CompoundTag d = destinations.getCompound(i);
            try {
                UUID id = UUID.fromString(d.getString("EntityId"));
                procedure.traineeDestinations.put(id, new BlockPos(d.getInt("X"), d.getInt("Y"), d.getInt("Z")));
            } catch (Exception ignored) {}
        }

        ListTag repath = tag.getList("TraineeNextRepathTick", Tag.TAG_COMPOUND);
        for (int i = 0; i < repath.size(); i++) {
            CompoundTag r = repath.getCompound(i);
            try {
                UUID id = UUID.fromString(r.getString("EntityId"));
                procedure.traineeNextRepathTick.put(id, r.getLong("Tick"));
            } catch (Exception ignored) {}
        }

        if (tag.contains("FinalSelectionRaid", Tag.TAG_COMPOUND)) {
            procedure.finalSelectionRaid = FinalSelectionRaid.fromTag(level, tag.getCompound("FinalSelectionRaid"));
        }

        return procedure;
    }

    // ===== Static registry methods =====

    private static FinalSelectionSavedState getSavedState(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            FinalSelectionSavedState::load,
            FinalSelectionSavedState::new,
            DATA_NAME
        );
    }

    private void persistState() {
        FinalSelectionSavedState saved = getSavedState(level);
        saved.setProcedureTag(toTag());
    }

    private static void clearPersistedState(ServerLevel level) {
        FinalSelectionSavedState saved = getSavedState(level);
        saved.clear();
    }

    /**
     * Integrated singleplayer can stop/start servers in the same JVM.
     * If that happens, static references can point at a dead ServerLevel instance.
     */
    private static void clearStaleActiveProcedure(ServerLevel level) {
        if (activeProcedure == null) {
            return;
        }
        if (activeProcedure.level == level) {
            return;
        }

        Log.debug("[FinalSelection] Cleared stale active procedure reference for dimension {}", level.dimension().location());
        activeProcedure = null;
    }

    private static void tryRestore(ServerLevel level) {
        if (activeProcedure != null && !activeProcedure.isFinished()) {
            return;
        }
        FinalSelectionSavedState saved = getSavedState(level);
        if (!saved.hasProcedureTag()) {
            return;
        }
        FinalSelectionProcedure restored = fromTag(level, saved.getProcedureTag());
        if (restored == null || restored.isFinished()) {
            clearPersistedState(level);
            return;
        }
        activeProcedure = restored;
        Log.debug("[FinalSelection] Restored active procedure from saved data");
    }

    private static <E extends Enum<E>> E parseEnum(String value, E fallback, Class<E> enumClass) {
        try {
            return Enum.valueOf(enumClass, value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static class FinalSelectionSavedState extends SavedData {
        private CompoundTag procedureTag;

        private FinalSelectionSavedState() {
            this.procedureTag = null;
        }

        private static FinalSelectionSavedState load(CompoundTag tag) {
            FinalSelectionSavedState state = new FinalSelectionSavedState();
            if (tag.contains("Procedure", Tag.TAG_COMPOUND)) {
                state.procedureTag = tag.getCompound("Procedure");
            }
            return state;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            if (procedureTag != null && !procedureTag.isEmpty()) {
                tag.put("Procedure", procedureTag);
            }
            return tag;
        }

        private void setProcedureTag(CompoundTag tag) {
            this.procedureTag = tag == null ? null : tag.copy();
            setDirty();
        }

        private boolean hasProcedureTag() {
            return procedureTag != null && !procedureTag.isEmpty();
        }

        private CompoundTag getProcedureTag() {
            return procedureTag == null ? new CompoundTag() : procedureTag.copy();
        }

        private void clear() {
            this.procedureTag = null;
            setDirty();
        }
    }

    /**
     * Check if a final selection procedure is currently running.
     */
    public static boolean isRunning() {
        return activeProcedure != null && !activeProcedure.isFinished();
    }

    /**
     * Get the active procedure, or null if none.
     */
    public static FinalSelectionProcedure getActive() {
        return activeProcedure;
    }

    /**
     * Start a new final selection procedure if none is running.
     * @return the new procedure, or the existing one if already running
     */
    public static FinalSelectionProcedure startOrGet(ServerLevel level) {
        if (!level.dimension().equals(MT_FUJIKASANE_KEY)) {
            Log.debug("[FinalSelection] Ignored start request outside Mt Fujikasane: " + level.dimension().location());
            return null;
        }

        clearStaleActiveProcedure(level);

        if (activeProcedure == null) {
            tryRestore(level);
        }

        if (activeProcedure != null && !activeProcedure.isFinished()) {
            return activeProcedure;
        }

        activeProcedure = new FinalSelectionProcedure(level);
        activeProcedure.persistState();
        return activeProcedure;
    }

    /**
     * Tick the active procedure. Called from server tick handler.
     */
    public static void tickActive(ServerLevel level) {
        if (level == null || !level.dimension().equals(MT_FUJIKASANE_KEY)) return;

        clearStaleActiveProcedure(level);

        if (activeProcedure == null) {
            tryRestore(level);
        }
        if (activeProcedure == null) return;

        activeProcedure.tick();
        activeProcedure.persistState();

        if (activeProcedure.isFinished()) {
            activeProcedure.cleanup();
            clearPersistedState(level);
            activeProcedure = null;
        }
    }

    /**
     * Force-stop the active procedure.
     */
    public static void stop() {
        if (activeProcedure != null) {
            ServerLevel level = activeProcedure.level;
            activeProcedure.cleanup();
            activeProcedure = null;
            if (level != null && level.dimension().equals(MT_FUJIKASANE_KEY)) {
                clearPersistedState(level);
            }
        }
    }

    public static void resetRuntimeState() {
        activeProcedure = null;
    }

    public static void onEntityKilledStatic(UUID entityId) {
        if (activeProcedure != null) {
            activeProcedure.onEntityKilled(entityId);
        }
    }

    public static boolean isRaidEntityStatic(UUID entityId) {
        return activeProcedure != null && activeProcedure.isRaidEntity(entityId);
    }

    public static boolean isInsideActiveRaidArea(ServerLevel level, double x, double z) {
        if (level == null) return false;
        if (!level.dimension().equals(MT_FUJIKASANE_KEY)) return false;
        clearStaleActiveProcedure(level);
        if (activeProcedure == null || activeProcedure.isFinished()) return false;
        if (activeProcedure.level != level) return false;
        if (activeProcedure.finalSelectionRaid == null) return false;
        return activeProcedure.finalSelectionRaid.isWithinRaidArea(x, z);
    }

    public static boolean isRaidOngoing(ServerLevel level) {
        if (level == null) return false;
        if (!level.dimension().equals(MT_FUJIKASANE_KEY)) return false;
        clearStaleActiveProcedure(level);
        if (activeProcedure == null || activeProcedure.isFinished()) return false;
        if (activeProcedure.level != level) return false;
        return activeProcedure.finalSelectionRaid != null && !activeProcedure.finalSelectionRaid.isFinished();
    }

    public static boolean canSpawnAdditionalNonBossDemon(ServerLevel level) {
        if (!isRaidOngoing(level)) {
            return false;
        }

        int playersInDimension = level.players().size();
        if (playersInDimension <= 0) {
            return false;
        }

        int maxPerPlayer = Math.max(0, FinalSelectionRaidConfig.maxDemonsPerPlayer.get());
        int maxAllowedDemons = maxPerPlayer * playersInDimension;
        if (maxAllowedDemons <= 0) {
            return false;
        }

        int currentDemonCount = 0;
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof Mob mob) || !mob.isAlive()) {
                continue;
            }

            ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
            if (entityId == null || !EntityCategorization.isDemon(entityId)) {
                continue;
            }

            currentDemonCount++;
            if (currentDemonCount >= maxAllowedDemons) {
                return false;
            }
        }

        return true;
    }

    public static int getActiveRaidNight(ServerLevel level) {
        if (!isRaidOngoing(level)) {
            return 0;
        }
        return activeProcedure.finalSelectionRaid.getCurrentNight();
    }

    public static boolean shouldRelocateDemonSlayersOnChunkUnload(ServerLevel level) {
        if (level == null || !level.dimension().equals(MT_FUJIKASANE_KEY)) {
            return false;
        }
        clearStaleActiveProcedure(level);
        if (activeProcedure == null || activeProcedure.isFinished() || activeProcedure.level != level) {
            return false;
        }

        if (activeProcedure.finalSelectionRaid != null && !activeProcedure.finalSelectionRaid.isFinished()) {
            return true;
        }

        return activeProcedure.state == State.RAID_END &&
            activeProcedure.raidSucceeded &&
            activeProcedure.traineeMode == TraineeMode.RETURN_TO_STAGING;
    }

    private void cleanupOwnedProcedureEntities() {
        AABB worldBounds = new AABB(
            -30_000_000.0D, level.getMinBuildHeight(), -30_000_000.0D,
            30_000_000.0D, level.getMaxBuildHeight(), 30_000_000.0D
        );

        for (Entity entity : level.getEntities((Entity) null, worldBounds, candidate -> candidate != null && candidate.isAlive())) {
            if (candidateIsOwnedProcedureEntity(entity) || candidateIsLegacyProcedureEntity(entity)) {
                entity.discard();
            }
        }
    }

    private boolean candidateIsOwnedProcedureEntity(Entity entity) {
        CompoundTag data = entity.getPersistentData();
        return data.getBoolean(OWNED_ENTITY_TAG);
    }

    private boolean candidateIsLegacyProcedureEntity(Entity entity) {
        if (entity.getType() == ModEntities.KANATA.get() && entity.blockPosition().distSqr(KANATA_POS) <= 16.0D) {
            return true;
        }
        if (entity.getType() == ModEntities.KIRIYA.get() && entity.blockPosition().distSqr(KIRIYA_POS) <= 16.0D) {
            return true;
        }
        if (entity instanceof DemonSlayerEntity slayer && slayer.getPowerLevel() <= 0) {
            return slayer.blockPosition().distSqr(TRAINEE_STAGING_CENTER) <= (TRAINEE_EXISTING_SCAN_RADIUS * TRAINEE_EXISTING_SCAN_RADIUS);
        }
        return false;
    }

    public static boolean confirmExit(ServerPlayer player) {
        if (activeProcedure == null || player == null || !player.level().dimension().equals(MT_FUJIKASANE_KEY)) {
            return false;
        }
        if (!activeProcedure.raidEndCompletedPlayers.contains(player.getUUID())) {
            player.sendSystemMessage(Component.literal("\u00a7cComplete Final Selection rewards first."));
            return false;
        }

        Long pendingUntil = activeProcedure.playerExitPendingUntilTick.get(player.getUUID());
        long nowTick = player.serverLevel().getGameTime();
        if (pendingUntil == null || nowTick > pendingUntil) {
            activeProcedure.playerExitPendingUntilTick.remove(player.getUUID());
            player.sendSystemMessage(Component.literal("\u00a7cNo pending exit confirmation."));
            return false;
        }

        activeProcedure.playerExitPendingUntilTick.remove(player.getUUID());
        if (TorilGateTeleportHandler.returnToPreviousPosition(player)) {
            return true;
        }

        ServerLevel overworld = player.server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            player.sendSystemMessage(Component.literal("\u00a7cCould not find overworld to return you."));
            return false;
        }

        BlockPos spawn = overworld.getSharedSpawnPos();
        player.teleportTo(overworld, spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D, player.getYRot(), player.getXRot());
        player.sendSystemMessage(Component.literal("\u00a7aReturned to overworld spawn."));
        return true;
    }

    public static void cancelExit(ServerPlayer player) {
        if (activeProcedure == null || player == null) {
            if (player != null) {
                player.sendSystemMessage(Component.literal("\u00a7cNo pending exit confirmation."));
            }
            return;
        }

        if (activeProcedure.playerExitPendingUntilTick.remove(player.getUUID()) != null) {
            player.sendSystemMessage(Component.literal("\u00a7eExit cancelled."));
        } else {
            player.sendSystemMessage(Component.literal("\u00a7cNo pending exit confirmation."));
        }
    }

    public static boolean reopenOreSelection(ServerPlayer player) {
        if (activeProcedure == null || player == null || !player.level().dimension().equals(MT_FUJIKASANE_KEY)) {
            return false;
        }

        activeProcedure.runOreSelectionProcedurePlaceholder(player);
        return true;
    }

    public static boolean acceptKakushiOffer(ServerPlayer player) {
        if (activeProcedure == null || player == null || !player.level().dimension().equals(MT_FUJIKASANE_KEY)) {
            return false;
        }

        Long pendingUntil = activeProcedure.kakushiPendingUntilTick.get(player.getUUID());
        long nowTick = player.serverLevel().getGameTime();
        if (pendingUntil == null || nowTick > pendingUntil) {
            activeProcedure.kakushiPendingUntilTick.remove(player.getUUID());
            player.sendSystemMessage(Component.literal("\u00a7cNo Kakushi offer is pending."));
            return false;
        }

        activeProcedure.kakushiPendingUntilTick.remove(player.getUUID());
        activeProcedure.kakushiAcceptedPlayers.add(player.getUUID());
        activeProcedure.awardAdvancement(player, DEMON_SLAYER_CORPS_ADVANCEMENT);
        activeProcedure.awardAdvancement(player, KAKUSHI_ADVANCEMENT);
        activeProcedure.givePlayerItemOrDrop(player, new ItemStack(ModItems.KAKUSHI_UNIFORM_HELMET.get()));
        activeProcedure.givePlayerItemOrDrop(player, new ItemStack(ModItems.KAKUSHI_UNIFORM_CHESTPLATE.get()));
        activeProcedure.givePlayerItemOrDrop(player, new ItemStack(ModItems.KAKUSHI_UNIFORM_LEGGINGS.get()));
        activeProcedure.givePlayerItemOrDrop(player, new ItemStack(ModItems.KAKUSHI_UNIFORM_BOOTS.get()));
        player.sendSystemMessage(Component.literal("\u00a7aYou have joined the Kakushi and received your uniform."));

        if (TorilGateTeleportHandler.returnToPreviousPosition(player)) {
            return true;
        }

        ServerLevel overworld = player.server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            player.sendSystemMessage(Component.literal("\u00a7cCould not find overworld to return you."));
            return false;
        }

        BlockPos spawn = overworld.getSharedSpawnPos();
        player.teleportTo(overworld, spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D, player.getYRot(), player.getXRot());
        player.sendSystemMessage(Component.literal("\u00a7aReturned to overworld spawn."));
        return true;
    }

    public static void declineKakushiOffer(ServerPlayer player) {
        if (activeProcedure == null || player == null) {
            if (player != null) {
                player.sendSystemMessage(Component.literal("\u00a7cNo Kakushi offer is pending."));
            }
            return;
        }

        if (activeProcedure.kakushiPendingUntilTick.remove(player.getUUID()) != null) {
            player.sendSystemMessage(Component.literal("\u00a7eKakushi offer declined."));
        } else {
            player.sendSystemMessage(Component.literal("\u00a7cNo Kakushi offer is pending."));
        }
    }

    public static boolean isPlayerDisqualified(ServerPlayer player) {
        return activeProcedure != null && player != null && activeProcedure.kakushiAcceptedPlayers.contains(player.getUUID());
    }
}
