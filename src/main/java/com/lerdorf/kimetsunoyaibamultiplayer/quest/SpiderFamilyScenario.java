package com.lerdorf.kimetsunoyaibamultiplayer.quest;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.effects.ModEffects;
import com.lerdorf.kimetsunoyaibamultiplayer.effects.PuppetryHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.DaughterEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.DemonSlayerEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Scenario logic for the Permanence questline's Spider Family arc:
 * - Stage No.4 - The Spider Family
 * - Stage No.5 - Demonweb Prince (Daughter path + Rui path)
 *
 * All state lives in the player's persistent NBT so it survives relogs.
 * Named entities (Rui, Mother, Father, Brother, Sister, Zenitsu, Tanjiro,
 * Giyu/Tomioka, Shinobu/Kocho, Inosuke, Kamanue-style spawns) come from the
 * base mod; Daughter/Ryoko is our own {@link DaughterEntity}.
 */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
public final class SpiderFamilyScenario {

    // ==================== Entity IDs ====================
    private static final ResourceLocation RUI_ID =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "rui");
    private static final ResourceLocation MOTHER_ID =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "mother");
    private static final ResourceLocation FATHER_ID =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "rui_father");
    private static final ResourceLocation BROTHER_ID =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "rui_brother");
    private static final ResourceLocation SISTER_ID =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "rui_sister");
    private static final ResourceLocation SPIDER_DEMON_ID =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "spider_demon");
    private static final ResourceLocation ZENITSU_ID =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "zennitsu");
    private static final ResourceLocation TANJIRO_ID =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "tanjiro");
    private static final ResourceLocation GIYU_ID =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "tomioka");
    private static final ResourceLocation SHINOBU_ID =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "kocho");
    private static final ResourceLocation INOSUKE_ID =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "inosuke");
    public static final ResourceLocation MT_NATAGUMO_BIOME =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "mt_natagumo");

    // ==================== Player NBT keys ====================
    private static final String PREFIX = "KnYSpiderFamily";
    /** Public for marker resolvers in QuestGroupRegistry. */
    public static final String MOTHER_UUID_KEY = PREFIX + "MotherUuid";
    private static final String KEY_RYOKO_UUID = PREFIX + "RyokoUuid";
    private static final String KEY_FAMILY_SPAWNED = PREFIX + "FamilySpawned";
    private static final String KEY_MOTHER_UUID = MOTHER_UUID_KEY;
    private static final String KEY_RUI_UUID = PREFIX + "RuiUuid";
    private static final String KEY_FATHER_UUID = PREFIX + "FatherUuid";
    private static final String KEY_DAUGHTER_PATH = PREFIX + "DaughterPath";
    private static final String KEY_PATH_CHOSEN = PREFIX + "PathChosen";
    private static final String KEY_DIALOGUE_START_TICK = PREFIX + "DialogueStartTick";
    private static final String KEY_DIALOGUE_STARTED = PREFIX + "DialogueStarted";
    private static final String KEY_PUPPET_COUNT = PREFIX + "PuppetCount";
    private static final String KEY_NATAGUMO_X = PREFIX + "NatagumoX";
    private static final String KEY_NATAGUMO_Y = PREFIX + "NatagumoY";
    private static final String KEY_NATAGUMO_Z = PREFIX + "NatagumoZ";
    private static final String KEY_MANIFESTATIONS_TAG = PREFIX + "Manifestation";
    private static final String KEY_QUEST_SLAYER_TAG = PREFIX + "QuestSlayer";

    // ==================== Tuning ====================
    private static final int DIALOGUE_TICK_INTERVAL = 45;
    private static final double TALK_RANGE_SQR = 6.0D * 6.0D;
    private static final int PUPPETS_REQUIRED = 15;
    private static final int PUPPETRY_DURATION_TICKS = 20 * 60 * 20;   // 20 minutes
    private static final int CIRCUS_PUPPETRY_DURATION_TICKS = 15 * 60 * 20; // 15 minutes
    private static final int ARMY_SLAYERS_TARGET = 24;

    private SpiderFamilyScenario() {
    }

    // =====================================================================
    // Shared helpers
    // =====================================================================

    public static boolean isInMtNatagumo(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        var biome = serverLevel.getBiome(player.blockPosition());
        return biome.is(ResourceKey(MT_NATAGUMO_BIOME));
    }

    private static net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome> ResourceKey(
            ResourceLocation id) {
        return net.minecraft.resources.ResourceKey.create(
            net.minecraft.core.registries.Registries.BIOME, id);
    }

    /** Stores (once) and returns the player's Mt Natagumo anchor position. */
    public static BlockPos getOrStoreNatagumoCenter(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (data.contains(KEY_NATAGUMO_X)) {
            return new BlockPos(data.getInt(KEY_NATAGUMO_X), data.getInt(KEY_NATAGUMO_Y),
                data.getInt(KEY_NATAGUMO_Z));
        }
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        BlockPos pos = QuestScenarioActions.findNearestBiome(serverLevel, player.blockPosition(),
            MT_NATAGUMO_BIOME);
        if (pos == null) {
            return null;
        }
        data.putInt(KEY_NATAGUMO_X, pos.getX());
        data.putInt(KEY_NATAGUMO_Y, pos.getY());
        data.putInt(KEY_NATAGUMO_Z, pos.getZ());
        return pos;
    }

    public static BlockPos resolveNatagumoMarker(ServerPlayer player, QuestRuntimeContext context) {
        return getOrStoreNatagumoCenter(player);
    }

    private static Entity findByUuid(ServerPlayer player, String key) {
        CompoundTag data = player.getPersistentData();
        if (!data.hasUUID(key) || !(player.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntity(data.getUUID(key));
        return entity != null && entity.isAlive() ? entity : null;
    }

    /** Public accessor used by quest step marker resolvers. */
    public static Entity findQuestEntityByUuid(ServerPlayer player, String key) {
        return findByUuid(player, key);
    }

    // ==================== Path guards & completion wrappers ====================

    /** True when the Rui path was NOT chosen (i.e. Daughter path active or undecided). */
    private static boolean onDaughterPath(ServerPlayer player, QuestRuntimeContext context) {
        CompoundTag data = player.getPersistentData();
        return !data.getBoolean(KEY_PATH_CHOSEN) || data.getBoolean(KEY_DAUGHTER_PATH);
    }

    private static boolean onRuiPath(ServerPlayer player, QuestRuntimeContext context) {
        CompoundTag data = player.getPersistentData();
        return data.getBoolean(KEY_PATH_CHOSEN) && !data.getBoolean(KEY_DAUGHTER_PATH);
    }

    public static boolean isTerrorizeVillageComplete(ServerPlayer player, QuestRuntimeContext context) {
        return onDaughterPath(player, context)
            && player.getPersistentData().getBoolean(PREFIX + "terror_complete");
    }

    public static boolean hasOverheardConversation(ServerPlayer player, QuestRuntimeContext context) {
        return onDaughterPath(player, context)
            && player.getPersistentData().getBoolean(PREFIX + "overheard")
            && player.level().getGameTime() >= player.getPersistentData()
                .getLong(KEY_DIALOGUE_START_TICK) + 250;
    }

    public static boolean hasSurvivedWrathReachedMother(ServerPlayer player, QuestRuntimeContext context) {
        return onDaughterPath(player, context)
            && player.getPersistentData().getBoolean(PREFIX + "reached_mother_wrath");
    }

    public static boolean hasAlliesDialogueDone(ServerPlayer player, QuestRuntimeContext context) {
        return onDaughterPath(player, context)
            && player.getPersistentData().getBoolean(PREFIX + "allies_dialogue_done");
    }

    public static boolean isCircusComplete(ServerPlayer player, QuestRuntimeContext context) {
        return onDaughterPath(player, context)
            && player.getPersistentData().getBoolean(PREFIX + "tanjiro_kills_mother")
            && player.level().getGameTime() >= player.getPersistentData()
                .getLong(KEY_DIALOGUE_START_TICK) + 150;
    }

    public static boolean hasFledMountNatagumo(ServerPlayer player, QuestRuntimeContext context) {
        return player.getPersistentData().getBoolean(PREFIX + "fled_natagumo");
    }

    public static boolean hasFatherBeenDefeated(ServerPlayer player, QuestRuntimeContext context) {
        return onRuiPath(player, context) && isFatherDead(player, context);
    }

    public static boolean hasVillageScenePlayed(ServerPlayer player, QuestRuntimeContext context) {
        return onRuiPath(player, context)
            && player.getPersistentData().getBoolean(PREFIX + "village_scene")
            && player.level().getGameTime() >= player.getPersistentData()
                .getLong(KEY_DIALOGUE_START_TICK) + 250;
    }

    public static boolean areVillageSlayersDefeated(ServerPlayer player, QuestRuntimeContext context) {
        return onRuiPath(player, context)
            && player.getPersistentData().getBoolean(PREFIX + "village_scene")
            && tickDefeatVillageSlayers(player, context);
    }

    public static boolean hasWarnedFamily(ServerPlayer player, QuestRuntimeContext context) {
        return onRuiPath(player, context)
            && player.getPersistentData().getBoolean(PREFIX + "warned_family");
    }

    public static boolean isArmyComplete(ServerPlayer player, QuestRuntimeContext context) {
        return onRuiPath(player, context)
            && player.getPersistentData().getInt(KEY_PUPPET_COUNT) >= PUPPETS_REQUIRED;
    }

    public static boolean hasReportedBackToRui(ServerPlayer player, QuestRuntimeContext context) {
        return onRuiPath(player, context)
            && player.getPersistentData().getBoolean(PREFIX + "rui_death_scene")
            && player.level().getGameTime() >= player.getPersistentData()
                .getLong(KEY_DIALOGUE_START_TICK) + 120;
    }

    private static <T extends Entity> T findNearbyQuestEntity(ServerLevel level, BlockPos center,
                                                              double radius, String tagValue,
                                                              Class<T> clazz) {
        return level.getEntitiesOfClass(clazz, new AABB(center).inflate(radius),
                e -> e.isAlive() && tagValue.equals(e.getPersistentData().getString(
                    QuestScenarioActions.QUEST_NPC_ID_TAG)))
            .stream()
            .min(Comparator.comparingDouble(e -> e.distanceToSqr(center.getX(), center.getY(), center.getZ())))
            .orElse(null);
    }

    /** Spawns a base-mod entity, marks it as a quest NPC, optionally invulnerable + frozen. */
    private static LivingEntity spawnBaseModEntity(ServerLevel level, ResourceLocation entityId,
                                                   BlockPos pos, String questTag, boolean invulnerable,
                                                   boolean freeze) {
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(entityId);
        if (type == null) {
            com.lerdorf.kimetsunoyaibamultiplayer.Log.debug("[SpiderFamily] missing entity type " + entityId);
            return null;
        }
        Entity entity = type.create(level);
        if (!(entity instanceof LivingEntity living)) {
            return null;
        }
        living.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
        if (living instanceof Mob mobToSpawn) {
            mobToSpawn.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.MOB_SUMMONED, null, null);
        }
        living.getPersistentData().putString(QuestScenarioActions.QUEST_NPC_ID_TAG, questTag);
        if (living instanceof Mob mob) {
            mob.setPersistenceRequired();
        }
        living.setCustomName(null);
        if (invulnerable) {
            living.setInvulnerable(true);
        }
        if (living instanceof Mob mob && freeze) {
            mob.setNoAi(true);
        }
        level.addFreshEntity(living);
        return living;
    }

    private static DemonSlayerEntity spawnQuestSlayer(ServerLevel level, BlockPos center, int radius,
                                                      boolean hostileToPlayer, ServerPlayer target) {
        DemonSlayerEntity slayer = (level.random.nextBoolean()
            ? ModEntities.DEMON_SLAYER : ModEntities.DEMON_SLAYER_FEMALE).get().create(level);
        if (slayer == null) {
            return null;
        }
        BlockPos pos = randomSurfacePos(level, center, radius);
        slayer.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
            level.random.nextFloat() * 360.0F, 0.0F);
        slayer.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.MOB_SUMMONED, null, null);
        slayer.getPersistentData().putString(QuestScenarioActions.QUEST_NPC_ID_TAG, KEY_QUEST_SLAYER_TAG);
        slayer.setPersistenceRequired();
        if (hostileToPlayer && target != null) {
            slayer.setTarget(target);
        }
        level.addFreshEntity(slayer);
        return slayer;
    }

    private static BlockPos randomSurfacePos(ServerLevel level, BlockPos center, int radius) {
        for (int attempt = 0; attempt < 32; attempt++) {
            int dx = level.random.nextInt(radius * 2 + 1) - radius;
            int dz = level.random.nextInt(radius * 2 + 1) - radius;
            int x = center.getX() + dx;
            int z = center.getZ() + dz;
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                return pos;
            }
        }
        return center;
    }

    private static void sendDialogue(ServerPlayer player, List<Component> lines) {
        QuestScenarioActions.sendDelayedMessages(player, lines, DIALOGUE_TICK_INTERVAL);
        QuestScenarioActions.processDelayedMessages(player);
    }

    private static void clearDialogueState(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        data.remove(KEY_DIALOGUE_STARTED);
        data.remove(KEY_DIALOGUE_START_TICK);
    }

    /**
     * Proximity talk helper: returns true once the player comes within talk range
     * of the entity; plays the given dialogue exactly once per call-site flag.
     */
    private static boolean talkOnce(ServerPlayer player, Entity npc, List<Component> lines,
                                    String dialogueFlagKey) {
        if (npc == null || player.distanceToSqr(npc) > TALK_RANGE_SQR) {
            return false;
        }
        CompoundTag data = player.getPersistentData();
        if (!data.getBoolean(dialogueFlagKey)) {
            data.putBoolean(dialogueFlagKey, true);
            sendDialogue(player, lines);
        }
        return true;
    }

    private static void despawnQuestEntities(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        AABB around = new AABB(player.blockPosition()).inflate(256.0D);
        List<Mob> questEntities = serverLevel.getEntitiesOfClass(Mob.class, around,
            e -> e.getPersistentData().getString(QuestScenarioActions.QUEST_NPC_ID_TAG)
                .startsWith(PREFIX)
                || KEY_QUEST_SLAYER_TAG.equals(e.getPersistentData().getString(
                    QuestScenarioActions.QUEST_NPC_ID_TAG)));
        for (Mob mob : questEntities) {
            mob.discard();
        }
        CompoundTag data = player.getPersistentData();
        for (String key : List.of(KEY_RYOKO_UUID, KEY_MOTHER_UUID, KEY_RUI_UUID, KEY_FATHER_UUID)) {
            data.remove(key);
        }
        data.putBoolean(KEY_FAMILY_SPAWNED, false);
    }

    // =====================================================================
    // Damage interception: invulnerability + Daughter-to-2HP + path choice
    // =====================================================================

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        String questTag = target.getPersistentData().getString(
            QuestScenarioActions.QUEST_NPC_ID_TAG);

        // Quest-invulnerable Spider Family NPCs never take damage from anyone
        // except scripted Rui-on-Daughter punishment hits (handled below).
        if ((questTag.startsWith(PREFIX) && !"punishable".equals(
                target.getPersistentData().getString(PREFIX + "VulnMode")))) {
            event.setCanceled(true);
            return;
        }

        // Path choice: whichever traitor the player strikes first picks the branch.
        if ("path_choice".equals(target.getPersistentData().getString(PREFIX + "VulnMode"))
            && event.getSource().getEntity() instanceof ServerPlayer chooser) {
            chooser.getPersistentData().putBoolean(KEY_PATH_CHOSEN, true);
            if (target instanceof DaughterEntity) {
                chooser.getPersistentData().putBoolean(KEY_DAUGHTER_PATH, false); // Rui path
                chooser.sendSystemMessage(Component.literal(
                    "\u00A7c[Rui] \u00A7fGood. Kill her, and prove your loyalty."));
            } else {
                chooser.getPersistentData().putBoolean(KEY_DAUGHTER_PATH, true);
                chooser.sendSystemMessage(Component.literal(
                    "\u00A7b[Daughter] \u00A7fSo you stand with me. Then Rui must fall."));
            }
        }
    }

    // =====================================================================
    // Stage No.4 - The Spider Family
    // =====================================================================

    // ---- Step 1: Find the demon child ----
    public static void startFindDemonChild(ServerPlayer player, QuestRuntimeContext context) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockPos natagumo = getOrStoreNatagumoCenter(player);
        BlockPos center = natagumo != null ? natagumo : player.blockPosition();

        // Don't duplicate Ryoko if she already exists nearby
        DaughterEntity ryoko = findNearbyQuestEntity(serverLevel, center, 128.0D,
            PREFIX + "ryoko", DaughterEntity.class);
        if (ryoko == null) {
            BlockPos ryokoPos = randomSurfacePos(serverLevel, center, 48);
            ryoko = ModEntities.DAUGHTER.get().create(serverLevel);
            if (ryoko == null) {
                return;
            }
            ryoko.moveTo(ryokoPos.getX() + 0.5D, ryokoPos.getY(), ryokoPos.getZ() + 0.5D, 0.0F, 0.0F);
            ryoko.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(ryokoPos),
                MobSpawnType.MOB_SUMMONED, null, null);
            ryoko.enterHumanDisguise();
            ryoko.setPersistenceRequired();
            ryoko.getPersistentData().putString(QuestScenarioActions.QUEST_NPC_ID_TAG, PREFIX + "ryoko");
            serverLevel.addFreshEntity(ryoko);
        }
        player.getPersistentData().putUUID(KEY_RYOKO_UUID, ryoko.getUUID());

        // Surround her with demon slayers that aren't currently attacking
        for (int i = 0; i < 4; i++) {
            DemonSlayerEntity slayer = spawnQuestSlayer(serverLevel, ryoko.blockPosition(), 8, false, null);
            if (slayer != null) {
                slayer.getPersistentData().putString(QuestScenarioActions.QUEST_NPC_ID_TAG,
                    PREFIX + "ryoko_captor");
            }
        }
        player.sendSystemMessage(Component.literal(
            "\u00A76Stage No.4 - The Spider Family: \u00A7fA spider familiar whispers of a demon child in peril. Find her."));
    }

    public static BlockPos resolveRyokoMarker(ServerPlayer player, QuestRuntimeContext context) {
        Entity ryoko = findByUuid(player, KEY_RYOKO_UUID);
        return ryoko == null ? null : ryoko.blockPosition();
    }

    // ---- Step 2: Protect Ryoko ----
    public static void tickProtectRyoko(ServerPlayer player, QuestRuntimeContext context) {
        Entity ryoko = findByUuid(player, KEY_RYOKO_UUID);
        if (!(player.level() instanceof ServerLevel serverLevel) || !(ryoko instanceof DaughterEntity daughter)) {
            return;
        }
        // Captors turn hostile the moment protection starts
        for (DemonSlayerEntity slayer : serverLevel.getEntitiesOfClass(DemonSlayerEntity.class,
                new AABB(daughter.blockPosition()).inflate(48.0D),
                s -> (PREFIX + "ryoko_captor").equals(s.getPersistentData().getString(
                    QuestScenarioActions.QUEST_NPC_ID_TAG)))) {
            if (slayer.getTarget() == null) {
                slayer.setTarget(nearestValidTarget(slayer, player));
            }
        }
    }

    private static LivingEntity nearestValidTarget(DemonSlayerEntity slayer, ServerPlayer player) {
        if (player != null && player.isAlive() && !player.isCreative() && !player.isSpectator()
            && player.distanceToSqr(slayer) < 32.0D * 32.0D) {
            return player;
        }
        return null;
    }

    public static boolean areAllCaptorsDead(ServerPlayer player, QuestRuntimeContext context) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        boolean anyAlive = !serverLevel.getEntitiesOfClass(DemonSlayerEntity.class,
            new AABB(player.blockPosition()).inflate(96.0D),
            s -> (PREFIX + "ryoko_captor").equals(s.getPersistentData().getString(
                QuestScenarioActions.QUEST_NPC_ID_TAG))).isEmpty();
        return !anyAlive;
    }

    public static boolean hasRyokoDied(ServerPlayer player, QuestRuntimeContext context) {
        return findByUuid(player, KEY_RYOKO_UUID) == null;
    }

    public static void failProtectRyoko(ServerPlayer player, QuestRuntimeContext context) {
        player.sendSystemMessage(Component.literal(
            "\u00A7cThe demon child has died. The quest fails and must be restarted."));
        despawnQuestEntities(player);
    }

    // ---- Step 3: Talk to the demon child ----
    public static boolean talkToRyoko(ServerPlayer player, QuestRuntimeContext context) {
        Entity ryoko = findByUuid(player, KEY_RYOKO_UUID);
        String name = player.getName().getString();
        return talkOnce(player, ryoko, List.of(
            Component.literal("\u00A7b[Ryoko] \u00A7fThank you for saving me!"),
            Component.literal("\u00A7b[Ryoko] \u00A7fMy name is Ryoko... may I travel with you?")
        ), PREFIX + "talked_ryoko");
    }

    public static boolean hasTalkedToRyoko(ServerPlayer player, QuestRuntimeContext context) {
        return player.getPersistentData().getBoolean(PREFIX + "talked_ryoko");
    }

    // ---- Step 4: Take Ryoko to Mount Natagumo ----
    public static void tickEscortRyoko(ServerPlayer player, QuestRuntimeContext context) {
        Entity ryoko = findByUuid(player, KEY_RYOKO_UUID);
        if (ryoko instanceof Mob mob) {
            // She follows the player
            if (mob.distanceToSqr(player) > 100.0D) {
                mob.getNavigation().moveTo(player, 1.05D);
            }
        }
    }

    public static boolean hasEscortedRyokoToNatagumo(ServerPlayer player, QuestRuntimeContext context) {
        return isInMtNatagumo(player) && hasRyokoNear(player);
    }

    private static boolean hasRyokoNear(ServerPlayer player) {
        Entity ryoko = findByUuid(player, KEY_RYOKO_UUID);
        return ryoko != null && player.distanceToSqr(ryoko) <= 32.0D * 32.0D;
    }

    // ---- Step 5: Talk to Ryoko's mother ----
    public static void startTalkToMother(ServerPlayer player, QuestRuntimeContext context) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        LivingEntity mother = findByUuid(player, KEY_MOTHER_UUID) instanceof LivingEntity living
            ? living : null;
        if (mother == null) {
            BlockPos pos = randomSurfacePos(serverLevel, player.blockPosition(), 24);
            mother = spawnBaseModEntity(serverLevel, MOTHER_ID, pos, PREFIX + "mother", true, true);
            if (mother != null) {
                player.getPersistentData().putUUID(KEY_MOTHER_UUID, mother.getUUID());
            }
        }
    }

    public static boolean tickMotherReunion(ServerPlayer player, QuestRuntimeContext context) {
        Entity mother = findByUuid(player, KEY_MOTHER_UUID);
        if (!talkOnce(player, mother, List.of(
            Component.literal("\u00A7b[Ryoko] \u00A7fNo! Please don't bring me back here!"),
            Component.literal("\u00A7c[Mother] \u00A7fMy daughter... you brought her home. Thank you."),
            Component.literal("\u00A7c[Mother] \u00A7fBut running away? You know He doesn't like it when we don't keep up appearances."),
            Component.literal("\u00A77*Spider webs grab Ryoko and drag her back. She transforms into her demon form...*")
        ), PREFIX + "mother_reunion_done")) {
            return false;
        }
        // Transform Ryoko back into demon form
        Entity ryoko = findByUuid(player, KEY_RYOKO_UUID);
        if (ryoko instanceof DaughterEntity daughter) {
            daughter.revealDemonForm();
            daughter.getPersistentData().putString(PREFIX + "VulnMode", "invulnerable");
            daughter.setInvulnerable(true);
        }
        if (mother != null) {
            mother.getPersistentData().putString(PREFIX + "VulnMode", "invulnerable");
        }
        return true;
    }

    public static boolean hasMotherReunionFinished(ServerPlayer player, QuestRuntimeContext context) {
        return player.getPersistentData().getBoolean(PREFIX + "mother_reunion_done");
    }

    // ---- Step 6: Join the Spider Family ----
    public static void ensureFamilyGathered(ServerPlayer player, QuestRuntimeContext context) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        CompoundTag data = player.getPersistentData();
        if (data.getBoolean(KEY_FAMILY_SPAWNED)) {
            return;
        }
        BlockPos house = getOrStoreNatagumoCenter(player);
        if (house == null) {
            house = player.blockPosition();
        }
        LivingEntity mother = findByUuid(player, KEY_MOTHER_UUID) instanceof LivingEntity m ? m : null;
        if (mother == null) {
            mother = spawnBaseModEntity(serverLevel, MOTHER_ID, randomSurfacePos(serverLevel, house, 16),
                PREFIX + "mother", true, true);
            if (mother != null) {
                data.putUUID(KEY_MOTHER_UUID, mother.getUUID());
            }
        }
        LivingEntity rui = spawnBaseModEntity(serverLevel, RUI_ID, randomSurfacePos(serverLevel, house, 10),
            PREFIX + "rui", true, true);
        spawnBaseModEntity(serverLevel, FATHER_ID, randomSurfacePos(serverLevel, house, 14),
            PREFIX + "father", true, true);
        spawnBaseModEntity(serverLevel, BROTHER_ID, randomSurfacePos(serverLevel, house, 18),
            PREFIX + "brother", true, true);
        spawnBaseModEntity(serverLevel, SISTER_ID, randomSurfacePos(serverLevel, house, 18),
            PREFIX + "sister", true, true);
        if (rui != null) {
            data.putUUID(KEY_RUI_UUID, rui.getUUID());
        }
        data.putBoolean(KEY_FAMILY_SPAWNED, true);

        // Reunited Daughter joins the courtyard too
        Entity ryoko = findByUuid(player, KEY_RYOKO_UUID);
        if (ryoko instanceof Mob mob) {
            mob.teleportTo(house.getX() + 0.5, house.getY(), house.getZ() + 0.5);
        }
        player.sendSystemMessage(Component.literal(
            "\u00A7fThe Spider Family awaits at their home. Speak with Rui."));
    }

    public static boolean tickJoinFamily(ServerPlayer player, QuestRuntimeContext context) {
        Entity rui = findByUuid(player, KEY_RUI_UUID);
        if (!talkOnce(player, rui, List.of(
            Component.literal("\u00A7c[Rui] \u00A7fYou saved my sister. Welcome to the family, sibling."),
            Component.literal("\u00A7c[Rui] \u00A7fTake this Spider Demon Blood. Drink, and become one of us.")
        ), PREFIX + "rui_welcome_done")) {
            return false;
        }
        // Reward handled by the stage's QuestRewardDefinition; here just mark done.
        return true;
    }

    public static boolean hasJoinedFamily(ServerPlayer player, QuestRuntimeContext context) {
        return player.getPersistentData().getBoolean(PREFIX + "rui_welcome_done");
    }

    public static void cleanupStageFour(ServerPlayer player, QuestRuntimeContext context) {
        clearDialogueState(player);
        // Keep family spawned for Stage No.5.
    }

    public static BlockPos resolveFamilyMarker(ServerPlayer player, QuestRuntimeContext context) {
        Entity rui = findByUuid(player, KEY_RUI_UUID);
        if (rui != null) {
            return rui.blockPosition();
        }
        Entity mother = findByUuid(player, KEY_MOTHER_UUID);
        return mother == null ? null : mother.blockPosition();
    }

    // =====================================================================
    // Stage No.5 - Demonweb Prince
    // =====================================================================

    // ---- Step 1: Witness Daughter's punishment ----
    public static void startPunishment(ServerPlayer player, QuestRuntimeContext context) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        ensureFamilyGathered(player, context);
        BlockPos courtyard = getOrStoreNatagumoCenter(player);
        if (courtyard == null) {
            courtyard = player.blockPosition();
        }
        // Gather everyone at the courtyard
        List<String> uuidKeys = List.of(KEY_MOTHER_UUID, KEY_FATHER_UUID, KEY_RUI_UUID);
        for (String key : uuidKeys) {
            Entity member = findByUuid(player, key);
            if (member != null) {
                member.teleportTo(courtyard.getX() + 0.5, courtyard.getY(), courtyard.getZ() + 0.5);
            }
        }
        DaughterEntity daughter = findNearbyQuestEntity(serverLevel, courtyard, 64.0D,
            PREFIX + "ryoko", DaughterEntity.class);
        if (daughter == null) {
            daughter = ModEntities.DAUGHTER.get().create(serverLevel);
            if (daughter != null) {
                daughter.moveTo(courtyard.getX() + 2.5, courtyard.getY(), courtyard.getZ() + 0.5, 0.0F, 0.0F);
                daughter.getPersistentData().putString(QuestScenarioActions.QUEST_NPC_ID_TAG, PREFIX + "ryoko");
                daughter.setPersistenceRequired();
                daughter.setInvulnerable(true);
                daughter.getPersistentData().putString(PREFIX + "VulnMode", "punishment");
                serverLevel.addFreshEntity(daughter);
            }
        } else {
            daughter.teleportTo(courtyard.getX() + 2.5, courtyard.getY(), courtyard.getZ() + 0.5);
            daughter.setInvulnerable(true);
            daughter.getPersistentData().putString(PREFIX + "VulnMode", "punishment");
        }
        player.sendSystemMessage(Component.literal(
            "\u00A76Demonweb Prince: \u00A7fWitness Daughter's punishment in the courtyard."));
        player.getPersistentData().remove(KEY_PATH_CHOSEN);
        player.getPersistentData().remove(KEY_DAUGHTER_PATH);
    }

    public static boolean tickPunishment(ServerPlayer player, QuestRuntimeContext context) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        DaughterEntity daughter = findNearbyQuestEntity(serverLevel, player.blockPosition(), 64.0D,
            PREFIX + "ryoko", DaughterEntity.class);
        if (daughter == null) {
            return false;
        }
        Entity rui = findByUuid(player, KEY_RUI_UUID);
        if (rui == null || player.distanceToSqr(rui) > TALK_RANGE_SQR) {
            return false;
        }
        CompoundTag data = player.getPersistentData();
        if (!data.getBoolean(PREFIX + "punishment_scene")) {
            data.putBoolean(PREFIX + "punishment_scene", true);
            sendDialogue(player, List.of(
                Component.literal("\u00A7c[Rui] \u00A7fSister... you risked the lives of this family by running away."),
                Component.literal("\u00A77*Rui's sharp webs slice Daughter apart, leaving a bloody mess on the ground.*")
            ));
            // Scripted punishment: Daughter reduced to 2 HP and heals herself
            daughter.setHealth(Math.min(2.0F, daughter.getHealth()));
            daughter.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.REGENERATION, 20 * 30, 1, false, false, false));
            daughter.getPersistentData().putString(PREFIX + "VulnMode", "invulnerable");
            return true;
        }
        return data.getLong(KEY_DIALOGUE_START_TICK) > 0
            && player.level().getGameTime() >= data.getLong(KEY_DIALOGUE_START_TICK) + 200;
    }

    public static boolean hasWitnessedPunishment(ServerPlayer player, QuestRuntimeContext context) {
        return player.getPersistentData().getBoolean(PREFIX + "punishment_scene")
            && player.level().getGameTime() >= player.getPersistentData()
                .getLong(KEY_DIALOGUE_START_TICK) + 200;
    }

    // ---- Step 2: Talk to Daughter ----
    public static boolean tickTalkToDaughter(ServerPlayer player, QuestRuntimeContext context) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        DaughterEntity daughter = findNearbyQuestEntity(serverLevel, player.blockPosition(), 64.0D,
            PREFIX + "ryoko", DaughterEntity.class);
        if (!talkOnce(player, daughter, List.of(
            Component.literal("\u00A7b[Daughter] \u00A7fThis is a violent and abusive family. Rui must be destroyed."),
            Component.literal("\u00A7b[Daughter] \u00A7fI'm too weak to run again. No demon can defeat Rui... find the demon slayers.")
        ), PREFIX + "daughter_plea_done")) {
            return false;
        }
        return true;
    }

    public static boolean hasTalkedToDaughterAfterPunishment(ServerPlayer player, QuestRuntimeContext context) {
        return player.getPersistentData().getBoolean(PREFIX + "daughter_plea_done");
    }

    // ---- Step 3: Talk to Rui + path choice ----
    public static boolean tickTalkToRuiChoice(ServerPlayer player, QuestRuntimeContext context) {
        Entity rui = findByUuid(player, KEY_RUI_UUID);
        if (!talkOnce(player, rui, List.of(
            Component.literal("\u00A7c[Rui] \u00A7fThis family is built on trust. We protect each other from this cruel world."),
            Component.literal("\u00A7c[Rui] \u00A7fI am Muzan's favorite. And my sister has broken our trust."),
            Component.literal("\u00A7c[Rui] \u00A7fShe trusts you. So YOU will kill her to prove your loyalty..."),
            Component.literal("\u00A7c[Rui] \u00A7f...or I will kill you both."),
            Component.literal("\u00A7eChoose now: strike Rui to side with Daughter, or strike Daughter to stay loyal to Rui.")
        ), PREFIX + "rui_choice_talk")) {
            return false;
        }
        // Enable the choice window
        CompoundTag data = player.getPersistentData();
        if (findByUuid(player, KEY_RYOKO_UUID) instanceof LivingEntity daughter) {
            daughter.setInvulnerable(false);
            daughter.getPersistentData().putString(PREFIX + "VulnMode", "path_choice");
        }
        if (rui instanceof LivingEntity ruiLiving) {
            ruiLiving.setInvulnerable(false);
            ruiLiving.getPersistentData().putString(PREFIX + "VulnMode", "path_choice");
        }
        return false; // completes when a path is chosen
    }

    public static boolean isPathChosen(ServerPlayer player, QuestRuntimeContext context) {
        return player.getPersistentData().getBoolean(KEY_PATH_CHOSEN);
    }

    public static boolean isDaughterPath(ServerPlayer player) {
        return player.getPersistentData().getBoolean(KEY_DAUGHTER_PATH);
    }

    public static void lockInPath(ServerPlayer player, QuestRuntimeContext context) {
        CompoundTag data = player.getPersistentData();
        // Restore invulnerability pending later steps
        Entity rui = findByUuid(player, KEY_RUI_UUID);
        if (rui instanceof LivingEntity ruiLiving && isDaughterPath(player)) {
            ruiLiving.setInvulnerable(true);
            ruiLiving.getPersistentData().putString(PREFIX + "VulnMode", "invulnerable");
        }
        Entity daughterEnt = findByUuid(player, KEY_RYOKO_UUID);
        if (daughterEnt instanceof LivingEntity daughter && !isDaughterPath(player)) {
            daughter.setInvulnerable(true);
            daughter.getPersistentData().putString(PREFIX + "VulnMode", "invulnerable");
        }
    }

    // ==================== Daughter Path ====================

    public static void startTerrorizeVillage(ServerPlayer player, QuestRuntimeContext context) {
        player.getPersistentData().remove(PREFIX + "terror_slayers_spawned");
        player.sendSystemMessage(Component.literal(
            "\u00A7fWear your spider demon transformation and terrorize a nearby village until demon slayers arrive."));
    }

    public static void tickTerrorizeVillage(ServerPlayer player, QuestRuntimeContext context) {
        CompoundTag data = player.getPersistentData();
        if (!(player.level() instanceof ServerLevel serverLevel) || !onDaughterPath(player, context)) {
            return;
        }
        BlockPos village = QuestScenarioActions.findNearestVanillaVillage(serverLevel, player.blockPosition());
        if (village == null || player.blockPosition().distSqr(village) > 64.0D * 64.0D) {
            return;
        }
        if (!data.getBoolean(PREFIX + "terror_slayers_spawned")) {
            data.putBoolean(PREFIX + "terror_slayers_spawned", true);
            for (int i = 0; i < 4; i++) {
                spawnQuestSlayer(serverLevel, village, 12, true, player);
            }
            player.sendSystemMessage(Component.literal("\u00A77Demon slayers have arrived..."));
        }
        boolean anyAlive = !serverLevel.getEntitiesOfClass(DemonSlayerEntity.class,
            new AABB(village).inflate(96.0D),
            s -> KEY_QUEST_SLAYER_TAG.equals(s.getPersistentData().getString(
                QuestScenarioActions.QUEST_NPC_ID_TAG))).isEmpty();
        if (!anyAlive && !data.getBoolean(PREFIX + "terror_complete")) {
            data.putBoolean(PREFIX + "terror_complete", true);
            player.sendSystemMessage(Component.literal(
                "\u00A7aThe demon slayers are defeated. \u00A7fReturn to Mount Natagumo."));
        }
    }

    public static boolean tickReturnOverhear(ServerPlayer player, QuestRuntimeContext context) {
        if (!isInMtNatagumo(player)) {
            return false;
        }
        CompoundTag data = player.getPersistentData();
        if (!data.getBoolean(PREFIX + "overheard")) {
            data.putBoolean(PREFIX + "overheard", true);
            String name = player.getName().getString();
            sendDialogue(player, List.of(
                Component.literal("\u00A7c[Father] \u00A7fRui, you owe Daughter an apology. Daughter is not the traitor."),
                Component.literal("\u00A7b[Daughter] \u00A7fYes, the traitor is " + name + "."),
                Component.literal("\u00A7c[Father] \u00A7fDaughter never ran away, " + name + " kidnapped her."),
                Component.literal("\u00A7b[Daughter] \u00A7fAnd now " + name + " is bringing the demon slayers to kill us all!"),
                Component.literal("\u00A7c[Rui] \u00A7fThey can't hurt us anymore, I'll make sure of it. And " + name + " must be destroyed.")
            ));
            // First wave of demon slayers arrives targeting Rui and Father
            if (player.level() instanceof ServerLevel serverLevel) {
                BlockPos center = player.blockPosition();
                for (int i = 0; i < 5; i++) {
                    DemonSlayerEntity slayer = spawnQuestSlayer(serverLevel, center, 24, false, null);
                    if (slayer != null) {
                        LivingEntity target = findByUuid(player, KEY_RUI_UUID) instanceof LivingEntity l ? l : null;
                        if (target == null) {
                            target = findByUuid(player, KEY_FATHER_UUID) instanceof LivingEntity f ? f : null;
                        }
                        if (target != null) {
                            slayer.setTarget(target);
                        }
                    }
                }
            }
        }
        return true;
    }

    public static boolean tickSurviveWrath(ServerPlayer player, QuestRuntimeContext context) {
        if (!onDaughterPath(player, context)) {
            return false;
        }
        // Survive: reach Mother while any pursuers exist
        Entity motherRaw = findByUuid(player, KEY_MOTHER_UUID);
        if (!(motherRaw instanceof LivingEntity mother)) {
            return false;
        }
        // Any demon slayer coming near Mother instantly becomes her puppet, pulled up into the trees
        if (player.level() instanceof ServerLevel serverLevel) {
            for (DemonSlayerEntity slayer : serverLevel.getEntitiesOfClass(DemonSlayerEntity.class,
                    new AABB(mother.blockPosition()).inflate(8.0D),
                    s -> !s.hasEffect(ModEffects.PUPPETRY.get()))) {
                PuppetryHandler.applyPuppetry(slayer, mother, CIRCUS_PUPPETRY_DURATION_TICKS);
                slayer.teleportRelative(0, 8 + slayer.getRandom().nextInt(4), 0);
            }
        }
        boolean reached = player.distanceToSqr(mother) <= TALK_RANGE_SQR;
        if (reached && !player.getPersistentData().getBoolean(PREFIX + "reached_mother_wrath")) {
            player.getPersistentData().putBoolean(PREFIX + "reached_mother_wrath", true);
        }
        return reached;
    }

    public static boolean tickMotherAlliesDialogue(ServerPlayer player, QuestRuntimeContext context) {
        Entity mother = findByUuid(player, KEY_MOTHER_UUID);
        if (!talkOnce(player, mother, List.of(
            Component.literal("\u00A76[" + player.getName().getString() + "] \u00A7fHelp! Daughter has betrayed me, and now Rui is going to kill me!"),
            Component.literal("\u00A7c[Mother] \u00A7fYou expect me to be grateful you rescued Daughter? I resent you for it!"),
            Component.literal("\u00A7c[Mother] \u00A7fIf you want to survive as a demon you must eliminate all traces of humanity within you."),
            Component.literal("\u00A76[" + player.getName().getString() + "] \u00A7fThe demon slayers are here, Rui's time will soon come to an end."),
            Component.literal("\u00A7c[Mother] \u00A7fRui has killed hundreds of demon slayers, and if I let you live then he will kill me too.")
        ), PREFIX + "allies_dialogue_done")) {
            return false;
        }
        return true;
    }

    public static void startCircusOfHorrors(ServerPlayer player, QuestRuntimeContext context) {
        if (!(player.level() instanceof ServerLevel serverLevel) || !onDaughterPath(player, context)) {
            return;
        }
        LivingEntity mother = findByUuid(player, KEY_MOTHER_UUID) instanceof LivingEntity m ? m : null;
        // Mother gets pulled up into the trees by her own webs
        if (mother != null) {
            mother.teleportRelative(0, 12, 0);
            if (mother instanceof Mob mob) {
                mob.setNoAi(true);
            }
        }
        // 10 puppets descend onto the player and attack
        for (int i = 0; i < 10; i++) {
            DemonSlayerEntity slayer = spawnQuestSlayer(serverLevel, player.blockPosition(), 16, true, player);
            if (slayer != null) {
                slayer.teleportRelative(0, 6 + i, 0);
                if (mother != null) {
                    PuppetryHandler.applyPuppetry(slayer, mother, CIRCUS_PUPPETRY_DURATION_TICKS);
                }
                slayer.setTarget(player);
                // Descending puppets drop their anchor where they land
                slayer.getPersistentData().putString(PREFIX + "VulnMode", "puppet");
            }
        }
        player.sendSystemMessage(Component.literal(
            "\u00A7cCircus of Horrors: \u00A7fDestroy all 10 demon slayer puppets!"));
    }

    public static boolean tickCircusOfHorrors(ServerPlayer player, QuestRuntimeContext context) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        long puppetsAlive = serverLevel.getEntitiesOfClass(DemonSlayerEntity.class,
            new AABB(player.blockPosition()).inflate(96.0D),
            s -> s.hasEffect(ModEffects.PUPPETRY.get())).size();
        if (puppetsAlive > 0) {
            return false;
        }
        // All puppets dead -> Tanjiro spawns at Mother's location and kills her
        LivingEntity mother = findByUuid(player, KEY_MOTHER_UUID) instanceof LivingEntity m ? m : null;
        if (mother != null && !player.getPersistentData().getBoolean(PREFIX + "tanjiro_kills_mother")) {
            player.getPersistentData().putBoolean(PREFIX + "tanjiro_kills_mother", true);
            BlockPos motherPos = mother.blockPosition();
            LivingEntity tanjiro = spawnBaseModEntity(serverLevel, TANJIRO_ID,
                motherPos.above(), PREFIX + "tanjiro", false, false);
            if (tanjiro instanceof Mob mob) {
                mob.setTarget(mother);
            }
            Damager.hurt(tanjiro != null ? tanjiro : player, mother, Float.MAX_VALUE, true);
            player.sendSystemMessage(Component.literal("\u00A77Tanjiro descends... and Mother is slain."));
        }
        return player.getPersistentData().getBoolean(PREFIX + "tanjiro_kills_mother");
    }

    public static void startConfrontDaughterDPath(ServerPlayer player, QuestRuntimeContext context) {
        player.sendSystemMessage(Component.literal(
            "\u00A7b[Daughter] \u00A7fNow's our chance to run away! Mother is dead, Rui is distracted..."));
        player.sendSystemMessage(Component.literal(
            "\u00A76[" + player.getName().getString() + "] \u00A7fYou betrayed me, you will die with the rest of your wretched family."));
        if (player.level() instanceof ServerLevel serverLevel) {
            LivingEntity shinobu = spawnBaseModEntity(serverLevel, SHINOBU_ID,
                randomSurfacePos(serverLevel, player.blockPosition(), 16), PREFIX + "shinobu", false, false);
            if (shinobu != null) {
                player.sendSystemMessage(Component.literal("\u00A75[Shinobu] \u00A7fYou will all die."));
                Entity daughter = findByUuid(player, KEY_RYOKO_UUID);
                if (shinobu instanceof Mob mob && daughter != null) {
                    mob.setTarget((LivingEntity) daughter);
                }
            }
            LivingEntity giyu = spawnBaseModEntity(serverLevel, GIYU_ID,
                randomSurfacePos(serverLevel, player.blockPosition(), 20), PREFIX + "giyu", true, false);
            if (giyu instanceof Mob mob) {
                mob.setTarget(player); // invulnerable for quest purposes
            }
        }
    }

    public static boolean tickFinishWhatYouStartedDPath(ServerPlayer player, QuestRuntimeContext context) {
        // Giyu hunts Rui once found; Rui becomes vulnerable
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        Entity rui = findByUuid(player, KEY_RUI_UUID);
        if (rui == null) {
            return true; // already dead
        }
        rui.setInvulnerable(false);
        rui.getPersistentData().putString(PREFIX + "VulnMode", "killable");
        LivingEntity giyuEnt = serverLevel.getEntitiesOfClass(LivingEntity.class,
                new AABB(player.blockPosition()).inflate(128.0D),
                e -> (PREFIX + "giyu").equals(e.getPersistentData().getString(
                    QuestScenarioActions.QUEST_NPC_ID_TAG)))
            .stream().findFirst().orElse(null);
        if (giyuEnt instanceof Mob mob) {
            if (player.distanceToSqr(rui) <= 48.0D * 48.0D) {
                mob.setTarget((LivingEntity) rui);
            } else {
                mob.setTarget(player);
            }
        }
        return false; // completes via Rui death check
    }

    public static boolean isRuiDead(ServerPlayer player, QuestRuntimeContext context) {
        return findByUuid(player, KEY_RUI_UUID) == null;
    }

    public static boolean tickFleeMountNatagumo(ServerPlayer player, QuestRuntimeContext context) {
        if (isInMtNatagumo(player)) {
            return false;
        }
        despawnQuestEntities(player);
        player.sendSystemMessage(Component.literal("\u00A7aYou escaped Mount Natagumo."));
        player.getPersistentData().putBoolean(PREFIX + "fled_natagumo", true);
        cleanupStageFive(player, context);
        return true;
    }

    // ==================== Rui Path ====================

    public static void startConfrontDaughterRPath(ServerPlayer player, QuestRuntimeContext context) {
        CompoundTag data = player.getPersistentData();
        data.remove(PREFIX + "daughter_fled");
        player.sendSystemMessage(Component.literal(
            "\u00A7fFind Daughter... she has been fully healed."));
        if (player.level() instanceof ServerLevel serverLevel) {
            DaughterEntity daughter = findNearbyQuestEntity(serverLevel, player.blockPosition(), 96.0D,
                PREFIX + "ryoko", DaughterEntity.class);
            if (daughter == null) {
                BlockPos pos = randomSurfacePos(serverLevel, player.blockPosition(), 40);
                daughter = ModEntities.DAUGHTER.get().create(serverLevel);
                if (daughter != null) {
                    daughter.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
                    daughter.getPersistentData().putString(QuestScenarioActions.QUEST_NPC_ID_TAG, PREFIX + "ryoko");
                    daughter.setPersistenceRequired();
                    serverLevel.addFreshEntity(daughter);
                }
            }
            if (daughter != null) {
                daughter.setHumanForm(true); // healed back to human-looking form
                data.putUUID(KEY_RYOKO_UUID, daughter.getUUID());
            }
        }
    }

    public static boolean tickConfrontDaughterRPath(ServerPlayer player, QuestRuntimeContext context) {
        Entity daughter = findByUuid(player, KEY_RYOKO_UUID);
        if (!talkOnce(player, daughter, List.of(
            Component.literal("\u00A7b[Daughter] \u00A7fYou aim to betray me? Fine, I do not need your help!"),
            Component.literal("\u00A7b[Daughter] \u00A7fI will get the demon slayers myself and burn this whole family to the ground!")
        ), PREFIX + "confront_daughter_done")) {
            return false;
        }
        if (!player.getPersistentData().getBoolean(PREFIX + "father_spawned")) {
            player.getPersistentData().putBoolean(PREFIX + "father_spawned", true);
            player.sendSystemMessage(Component.literal(
                "\u00A7c[Father] \u00A7f" + player.getName().getString() + ", you will be punished for your insolence!"));
            if (player.level() instanceof ServerLevel serverLevel) {
                LivingEntity father = spawnBaseModEntity(serverLevel, FATHER_ID,
                    randomSurfacePos(serverLevel, player.blockPosition(), 16),
                    PREFIX + "father_hunt", false, false);
                if (father != null) {
                    player.getPersistentData().putUUID(KEY_FATHER_UUID, father.getUUID());
                    if (father instanceof Mob mob) {
                        mob.setTarget(player);
                    }
                }
            }
        }
        return false; // completes when Father dies
    }

    public static boolean isFatherDead(ServerPlayer player, QuestRuntimeContext context) {
        if (findByUuid(player, KEY_FATHER_UUID) != null) {
            return false;
        }
        if (!player.getPersistentData().getBoolean(PREFIX + "rui_go_kill_message")
            && player.getPersistentData().getBoolean(PREFIX + "father_spawned")) {
            player.getPersistentData().putBoolean(PREFIX + "rui_go_kill_message", true);
            player.sendSystemMessage(Component.literal(
                "\u00A7c[Rui] \u00A7fStop wasting your time and go kill Daughter!"));
        }
        return player.getPersistentData().getBoolean(PREFIX + "father_spawned");
    }

    public static void startFindDaughterAtVillage(ServerPlayer player, QuestRuntimeContext context) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockPos village = QuestScenarioActions.findNearestVanillaVillage(serverLevel, player.blockPosition());
        BlockPos center = village != null ? village : player.blockPosition();

        // Daughter waits in human (Ryoko) form in the middle of the village
        DaughterEntity ryoko = ModEntities.DAUGHTER.get().create(serverLevel);
        if (ryoko != null) {
            ryoko.moveTo(center.getX() + 0.5, center.getY(), center.getZ() + 0.5, 0.0F, 0.0F);
            ryoko.enterHumanDisguise();
            ryoko.getPersistentData().putString(QuestScenarioActions.QUEST_NPC_ID_TAG, PREFIX + "village_ryoko");
            ryoko.setPersistenceRequired();
            ryoko.setInvulnerable(true); // Zenitsu saves her
            serverLevel.addFreshEntity(ryoko);
            player.getPersistentData().putUUID(KEY_RYOKO_UUID, ryoko.getUUID());
        }
        player.sendSystemMessage(Component.literal(
            "\u00A7fThe villagers hang in cocoons. Ryoko stands in the center of the village..."));
    }

    public static boolean tickVillageScene(ServerPlayer player, QuestRuntimeContext context) {
        Entity ryoko = findByUuid(player, KEY_RYOKO_UUID);
        CompoundTag data = player.getPersistentData();
        if (!data.getBoolean(PREFIX + "village_scene")) {
            if (!talkOnce(player, ryoko, List.of(
                Component.literal("\u00A7b[Ryoko] \u00A7fHelp! The demon is here!")
            ), PREFIX + "village_help")) {
                return false;
            }
            data.putBoolean(PREFIX + "village_scene", true);
            if (player.level() instanceof ServerLevel serverLevel) {
                LivingEntity zenitsu = spawnBaseModEntity(serverLevel, ZENITSU_ID,
                    randomSurfacePos(serverLevel, player.blockPosition(), 12),
                    PREFIX + "zenitsu", false, true);
                if (zenitsu != null) {
                    data.putUUID(PREFIX + "ZenitsuUuid", zenitsu.getUUID());
                    sendDialogue(player, List.of(
                        Component.literal("\u00A7e[Zenitsu] \u00A7fI'll save you Ryoko-chan!"),
                        Component.literal("\u00A77*Zenitsu grabs Ryoko and vanishes in a flash of thunder...*"),
                        Component.literal("\u00A7cDemon slayers ambush you!")
                    ));
                    if (ryoko != null) {
                        ryoko.discard(); // taken away
                        data.remove(KEY_RYOKO_UUID);
                    }
                    for (int i = 0; i < 5; i++) {
                        spawnQuestSlayer(serverLevel, player.blockPosition(), 12, true, player);
                    }
                }
            }
        }
        return data.getBoolean(PREFIX + "village_scene");
    }

    public static boolean tickDefeatVillageSlayers(ServerPlayer player, QuestRuntimeContext context) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        return serverLevel.getEntitiesOfClass(DemonSlayerEntity.class,
            new AABB(player.blockPosition()).inflate(96.0D),
            s -> KEY_QUEST_SLAYER_TAG.equals(s.getPersistentData().getString(
                QuestScenarioActions.QUEST_NPC_ID_TAG))).isEmpty();
    }

    public static boolean tickWarnFamily(ServerPlayer player, QuestRuntimeContext context) {
        Entity mother = findByUuid(player, KEY_MOTHER_UUID);
        if (!isInMtNatagumo(player)) {
            return false;
        }
        if (mother == null && player.level() instanceof ServerLevel serverLevel) {
            BlockPos pos = randomSurfacePos(serverLevel, player.blockPosition(), 24);
            mother = spawnBaseModEntity(serverLevel, MOTHER_ID, pos, PREFIX + "mother", true, true);
            if (mother != null) {
                player.getPersistentData().putUUID(KEY_MOTHER_UUID, mother.getUUID());
            }
        }
        if (!talkOnce(player, mother, List.of(
            Component.literal("\u00A76[" + player.getName().getString() + "] \u00A7fDaughter betrayed us, the Demon Slayers are coming to exterminate our family!"),
            Component.literal("\u00A7c[Mother] \u00A7fLure the demon slayers into my traps, we will build an army of puppets to defend our family!")
        ), PREFIX + "warned_family")) {
            return false;
        }
        return true;
    }

    // ---- Step 8: Build the army of puppets ----
    public static void startArmyOfPuppets(ServerPlayer player, QuestRuntimeContext context) {
        if (!(player.level() instanceof ServerLevel serverLevel) || !onRuiPath(player, context)) {
            return;
        }
        CompoundTag data = player.getPersistentData();
        data.putInt(KEY_PUPPET_COUNT, 0);
        data.remove(PREFIX + "army_started");
        BlockPos forest = getOrStoreNatagumoCenter(player);
        if (forest == null) {
            forest = player.blockPosition();
        }
        // Hidden Spider Manifestations throughout the forest
        for (int i = 0; i < 8; i++) {
            BlockPos pos = randomSurfacePos(serverLevel, forest, 64);
            LivingEntity manifestation = spawnBaseModEntity(serverLevel, SPIDER_DEMON_ID, pos,
                PREFIX + "manifestation", true, true);
            if (manifestation != null) {
                manifestation.setInvisible(true);
                manifestation.getPersistentData().putBoolean(KEY_MANIFESTATIONS_TAG, true);
            }
        }
        player.sendSystemMessage(Component.literal(
            "\u00A7fLure demon slayers within 5 blocks of the hidden Spider Manifestations. Puppets created: 0/" + PUPPETS_REQUIRED));
    }

    public static boolean tickArmyOfPuppets(ServerPlayer player, QuestRuntimeContext context) {
        if (!(player.level() instanceof ServerLevel serverLevel) || !onRuiPath(player, context)) {
            return false;
        }
        CompoundTag data = player.getPersistentData();
        data.putBoolean(PREFIX + "army_started", true);
        List<LivingEntity> manifestations = serverLevel.getEntitiesOfClass(LivingEntity.class,
            new AABB(player.blockPosition()).inflate(128.0D),
            s -> (PREFIX + "manifestation").equals(s.getPersistentData().getString(
                QuestScenarioActions.QUEST_NPC_ID_TAG)));

        Entity mother = findByUuid(player, KEY_MOTHER_UUID);
        if (mother == null) {
            mother = player; // fallback owner if Mother is unloaded
        }

        // Convert slayers within 5 blocks of any manifestation into puppets owned by Mother
        int converted = 0;
        for (DemonSlayerEntity slayer : serverLevel.getEntitiesOfClass(DemonSlayerEntity.class,
                new AABB(player.blockPosition()).inflate(128.0D),
                s -> !s.hasEffect(ModEffects.PUPPETRY.get())
                    && !(PREFIX + "manifestation").equals(s.getPersistentData().getString(
                        QuestScenarioActions.QUEST_NPC_ID_TAG)))) {
            for (LivingEntity manifestation : manifestations) {
                if (slayer.distanceToSqr(manifestation) <= 5.0D * 5.0D
                    && !slayer.hasEffect(ModEffects.PUPPETRY.get())) {
                    PuppetryHandler.applyPuppetry(slayer, (LivingEntity) mother, PUPPETRY_DURATION_TICKS);
                    slayer.getPersistentData().putString(PREFIX + "VulnMode", "puppet");
                    converted++;
                    break;
                }
            }
        }
        if (converted > 0) {
            int count = data.getInt(KEY_PUPPET_COUNT) + converted;
            data.putInt(KEY_PUPPET_COUNT, count);
            player.sendSystemMessage(Component.literal(
                "\u00A7fPuppets created: " + Math.min(count, PUPPETS_REQUIRED) + "/" + PUPPETS_REQUIRED));
        }

        // Keep spawning wild demon slayers throughout the forest to lure
        long wildAlive = serverLevel.getEntitiesOfClass(DemonSlayerEntity.class,
            new AABB(player.blockPosition()).inflate(128.0D),
            s -> !s.hasEffect(ModEffects.PUPPETRY.get())).size();
        if (wildAlive < 6 && data.getLong(PREFIX + "last_slayer_spawn") + 100L < player.level().getGameTime()) {
            data.putLong(PREFIX + "last_slayer_spawn", player.level().getGameTime());
            spawnQuestSlayer(serverLevel, player.blockPosition(), 48, false, null);
        }

        return data.getInt(KEY_PUPPET_COUNT) >= PUPPETS_REQUIRED;
    }

    public static void completeArmyOfPuppets(ServerPlayer player, QuestRuntimeContext context) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        // Rui arrives: teleport over, or spawn if none within 500 blocks
        LivingEntity rui = findByUuid(player, KEY_RUI_UUID) instanceof LivingEntity l ? l : null;
        if (rui == null) {
            rui = spawnBaseModEntity(serverLevel, RUI_ID,
                randomSurfacePos(serverLevel, player.blockPosition(), 8),
                PREFIX + "rui", true, true);
            if (rui != null) {
                player.getPersistentData().putUUID(KEY_RUI_UUID, rui.getUUID());
            }
        } else {
            rui.teleportTo(player.getX() + 2, player.getY(), player.getZ());
        }
        sendDialogue(player, List.of(
            Component.literal("\u00A7c[Rui] \u00A7f" + player.getName().getString()
                + ", you are proving to be a valuable member of the family. Now find Daughter and kill her!")
        ));
        player.getPersistentData().putBoolean(PREFIX + "rui_arrived_army", true);
    }

    public static boolean hasArmyRuiArrived(ServerPlayer player, QuestRuntimeContext context) {
        return player.getPersistentData().getBoolean(PREFIX + "rui_arrived_army")
            && player.level().getGameTime() >= player.getPersistentData()
                .getLong(KEY_DIALOGUE_START_TICK) + 250;
    }

    // ---- Step 9 (Rui path): Finish what you started (boss battle) ----
    public static void startBossDaughter(ServerPlayer player, QuestRuntimeContext context) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        CompoundTag data = player.getPersistentData();
        // Daughter in helpless Ryoko form, guarded by Zenitsu and demon slayers
        DaughterEntity ryoko = ModEntities.DAUGHTER.get().create(serverLevel);
        if (ryoko != null) {
            BlockPos pos = randomSurfacePos(serverLevel, player.blockPosition(), 64);
            ryoko.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
            ryoko.enterHumanDisguise();
            ryoko.setPersistenceRequired();
            ryoko.setInvulnerable(false);
            ryoko.getAttribute(Attributes.MAX_HEALTH).setBaseValue(150.0D);
            ryoko.setHealth(150.0F);
            ryoko.getPersistentData().putString(QuestScenarioActions.QUEST_NPC_ID_TAG, PREFIX + "boss_ryoko");
            serverLevel.addFreshEntity(ryoko);
            data.putUUID(KEY_RYOKO_UUID, ryoko.getUUID());
        }
        LivingEntity zenitsu = spawnBaseModEntity(serverLevel, ZENITSU_ID,
            randomSurfacePos(serverLevel, player.blockPosition(), 12), PREFIX + "zenitsu_boss", false, false);
        if (zenitsu != null) {
            data.putUUID(PREFIX + "ZenitsuUuid", zenitsu.getUUID());
            if (zenitsu instanceof Mob mob) {
                mob.setTarget(player);
            }
        }
        for (int i = 0; i < 4; i++) {
            DemonSlayerEntity slayer = spawnQuestSlayer(serverLevel, player.blockPosition(), 16, true, player);
            if (slayer != null && ryoko != null) {
                slayer.setTarget(player);
            }
        }
        sendDialogue(player, List.of(
            Component.literal("\u00A7e[Zenitsu] \u00A7fYou monster! Leave this girl alone!"),
            Component.literal("\u00A76[" + player.getName().getString() + "] \u00A7fShe's a demon, just like me."),
            Component.literal("\u00A7e[Zenitsu] \u00A7fRyoko-chan is not a monster like you, I won't let you murder her!")
        ));
        player.sendSystemMessage(Component.literal(
            "\u00A7cBoss Battle: \u00A7fKill Daughter!"));
    }

    public static boolean isBossDaughterDead(ServerPlayer player, QuestRuntimeContext context) {
        return findByUuid(player, KEY_RYOKO_UUID) == null;
    }

    // ---- Step 10 (Rui path): Report back to Rui ----
    public static boolean tickReportBackToRui(ServerPlayer player, QuestRuntimeContext context) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        LivingEntity rui = findByUuid(player, KEY_RUI_UUID) instanceof LivingEntity l ? l : null;
        if (rui == null) {
            rui = spawnBaseModEntity(serverLevel, RUI_ID,
                randomSurfacePos(serverLevel, player.blockPosition(), 32),
                PREFIX + "rui", true, true);
            if (rui != null) {
                player.getPersistentData().putUUID(KEY_RUI_UUID, rui.getUUID());
            }
            return false;
        }
        if (player.distanceToSqr(rui) > TALK_RANGE_SQR) {
            return false;
        }
        CompoundTag data = player.getPersistentData();
        if (!data.getBoolean(PREFIX + "rui_death_scene")) {
            data.putBoolean(PREFIX + "rui_death_scene", true);
            // Giyu instantly kills Rui; more slayers + full Kamaboko squad spawn
            LivingEntity giyu = spawnBaseModEntity(serverLevel, GIYU_ID,
                rui.blockPosition().east(2), PREFIX + "giyu_final", false, false);
            Damager.hurt(giyu != null ? giyu : player, rui, Float.MAX_VALUE, true);
            sendDialogue(player, List.of(
                Component.literal("\u00A77*Before you can speak, a flash of water cuts through the air-*"),
                Component.literal("\u00A7c[Rui] \u00A7fImposs...ible...")
            ));
            spawnBaseModEntity(serverLevel, TANJIRO_ID,
                randomSurfacePos(serverLevel, player.blockPosition(), 20), PREFIX + "tanjiro_final", false, false);
            spawnBaseModEntity(serverLevel, INOSUKE_ID,
                randomSurfacePos(serverLevel, player.blockPosition(), 20), PREFIX + "inosuke_final", false, false);
            spawnBaseModEntity(serverLevel, SHINOBU_ID,
                randomSurfacePos(serverLevel, player.blockPosition(), 20), PREFIX + "shinobu_final", false, false);
            for (int i = 0; i < 6; i++) {
                DemonSlayerEntity slayer = spawnQuestSlayer(serverLevel, player.blockPosition(), 32, true, player);
                if (slayer != null) {
                    slayer.setTarget(player);
                }
            }
        }
        return data.getBoolean(PREFIX + "rui_death_scene")
            && player.level().getGameTime() >= data.getLong(KEY_DIALOGUE_START_TICK) + 120;
    }

    public static void cleanupStageFive(ServerPlayer player, QuestRuntimeContext context) {
        clearDialogueState(player);
        CompoundTag data = player.getPersistentData();
        for (String key : new String[]{
            KEY_RYOKO_UUID, KEY_MOTHER_UUID, KEY_RUI_UUID, KEY_FATHER_UUID,
            PREFIX + "ZenitsuUuid"}) {
            data.remove(key);
        }
    }
}
