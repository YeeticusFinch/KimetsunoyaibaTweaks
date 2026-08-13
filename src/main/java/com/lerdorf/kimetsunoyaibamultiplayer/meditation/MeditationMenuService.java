package com.lerdorf.kimetsunoyaibamultiplayer.meditation;

import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.BlueSpiderLilyTeaHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.config.CustomProgressionConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.config.DemonRankingConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.demonranking.DemonRank;
import com.lerdorf.kimetsunoyaibamultiplayer.demonranking.DemonRankingSavedData;
import com.lerdorf.kimetsunoyaibamultiplayer.events.DemonTransformationHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.OpenMeditationMenuPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.quest.PlayerRole;
import com.lerdorf.kimetsunoyaibamultiplayer.quest.QuestProgressionManager;
import com.lerdorf.kimetsunoyaibamultiplayer.util.DemonEyesHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.util.TrainingSwordHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

public final class MeditationMenuService {
    public static final String SELECTED_TYPE_QUEST = "quest";
    public static final String SELECTED_TYPE_LOCATION = "location";

    private static final ResourceLocation COMPLETED_FINAL_SELECTION =
        ResourceLocation.parse("kimetsunoyaibamultiplayer:completed_final_selectioni");
    private static final ResourceLocation KAKUSHI =
        ResourceLocation.parse("kimetsunoyaibamultiplayer:kakushi");
    private static final ResourceLocation DEMON_SLAYER_CORPS =
        ResourceLocation.parse("kimetsunoyaibamultiplayer:demon_slayer_corps");
    private static final List<MeditationMenuData.LocationEntry> CORPS_STRUCTURE_LOCATIONS = List.of(
        new MeditationMenuData.LocationEntry("swordsmith_village", "Swordsmith Village", "Find a Kakushi who can escort you to the Swordsmith Village.", false),
        new MeditationMenuData.LocationEntry("house_kocho", "Kocho House", "Waypoint for kimetsunoyaiba:house_kocho.", false),
        new MeditationMenuData.LocationEntry("graveyard", "Graveyard", "Waypoint for kimetsunoyaiba:graveyard.", false),
        new MeditationMenuData.LocationEntry("house_rengoku", "Rengoku House", "Waypoint for kimetsunoyaiba:house_rengoku.", false),
        new MeditationMenuData.LocationEntry("house_tamayo", "Tamayo House", "Waypoint for kimetsunoyaiba:house_tamayo.", false),
        new MeditationMenuData.LocationEntry("house_tanjiro", "Tanjiro House", "Waypoint for kimetsunoyaiba:house_tanjiro.", false),
        new MeditationMenuData.LocationEntry("house_ubuyashiki", "Ubuyashiki House", "Waypoint for kimetsunoyaiba:house_ubuyashiki.", false),
        new MeditationMenuData.LocationEntry("house_urokodaki", "Urokodaki House", "Waypoint for kimetsunoyaiba:house_urokodaki.", false)
    );
    private static final List<ResourceLocation> RANK_ADVANCEMENTS = List.of(
        ResourceLocation.parse("kimetsunoyaiba:hashira"),
        ResourceLocation.parse("kimetsunoyaiba:kinoe"),
        ResourceLocation.parse("kimetsunoyaiba:kinoto"),
        ResourceLocation.parse("kimetsunoyaiba:hinoe"),
        ResourceLocation.parse("kimetsunoyaiba:hinoto"),
        ResourceLocation.parse("kimetsunoyaiba:tsuchinoe"),
        ResourceLocation.parse("kimetsunoyaiba:tsuchinoto"),
        ResourceLocation.parse("kimetsunoyaiba:kanoe"),
        ResourceLocation.parse("kimetsunoyaiba:kanoto"),
        ResourceLocation.parse("kimetsunoyaiba:mizunoe"),
        ResourceLocation.parse("kimetsunoyaiba:mizunoto")
    );

    private MeditationMenuService() {
    }

    public static void openFor(ServerPlayer player) {
        if (!CustomProgressionConfig.isCustomProgressionEnabled()) {
            return;
        }
        ModNetworking.sendToPlayer(new OpenMeditationMenuPacket(buildFor(player)), player);
    }

    public static MeditationMenuData buildFor(ServerPlayer player) {
        if (!CustomProgressionConfig.isCustomProgressionEnabled()) {
            return new MeditationMenuData(
                PlayerRole.CIVILIAN.getDisplayName(),
                "None",
                "0",
                "0",
                "None",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                0,
                "",
                "",
                false,
                DemonEyesHelper.DEFAULT_DEMON_EYES_INDEX
            );
        }

        PlayerRole role = resolveRoleForProgression(player);
        String rank = resolveRank(player, role);
        String muzanBlood = resolveMuzanBlood(player);
        String humansConsumed = resolveHumansConsumed(player);
        String kizukiRank = resolveKizukiRank(player);
        boolean demonPlayer = role == PlayerRole.DEMON;
        int demonEyesIndex = DemonEyesHelper.getOrCreateIndex(player);

        List<MeditationMenuData.InfoSection> infoSections = buildInfoSections(player);
        List<MeditationMenuData.QuestEntry> quests = QuestProgressionManager.buildQuestEntries(player, role);
        if (role == PlayerRole.DEMON && DemonRankingConfig.isEnabled()) {
            List<MeditationMenuData.QuestEntry> withBloodyBattle = new ArrayList<>();
            withBloodyBattle.add(buildBloodyBattleEntry(player));
            withBloodyBattle.addAll(quests);
            quests = withBloodyBattle;
        }
        List<MeditationMenuData.LocationEntry> locations = buildLocations(role);
        List<MeditationMenuData.PassiveSkillEntry> passiveSkills = PassiveSkillManager.buildEntries(player, role);
        int passiveSkillPoints = PassiveSkillManager.getAvailableSkillPoints(player, role);

        String selectedType = player.getPersistentData().getString("MeditationSelectedType");
        String selectedId = player.getPersistentData().getString("MeditationSelectedId");
        if (!selectionExists(selectedType, selectedId, quests, locations)) {
            if (!quests.isEmpty()) {
                selectedType = SELECTED_TYPE_QUEST;
                selectedId = quests.get(0).id();
            } else if (!locations.isEmpty()) {
                selectedType = SELECTED_TYPE_LOCATION;
                selectedId = locations.get(0).id();
            } else {
                selectedType = "";
                selectedId = "";
            }
            saveSelection(player, selectedType, selectedId);
        }

        quests = applyQuestSelection(quests, selectedType, selectedId);
        locations = applyLocationSelection(locations, selectedType, selectedId);

        return new MeditationMenuData(role.getDisplayName(), rank, muzanBlood, humansConsumed, kizukiRank,
            infoSections, quests, locations, passiveSkills, passiveSkillPoints, selectedType, selectedId, demonPlayer, demonEyesIndex);
    }

    public static void saveSelection(ServerPlayer player, String type, String id) {
        if (!CustomProgressionConfig.isCustomProgressionEnabled()) {
            return;
        }
        player.getPersistentData().putString("MeditationSelectedType", type);
        player.getPersistentData().putString("MeditationSelectedId", id);
    }

    private static List<MeditationMenuData.InfoSection> buildInfoSections(ServerPlayer player) {
        int demonKills = getInt(player, MeditationStatsTracker.DEMON_KILLS_TOTAL);
        int kizukiKills = getInt(player, MeditationStatsTracker.KIZUKI_KILLS_TOTAL);
        int nonKizukiKills = Math.max(0, demonKills - kizukiKills);
        int humanKills = getInt(player, MeditationStatsTracker.HUMAN_KILLS_TOTAL);

        MeditationMenuData.InfoSection demonsKilled = new MeditationMenuData.InfoSection(
            "demons_killed",
            "Demons Killed",
            demonKills,
            List.of(
                new MeditationMenuData.InfoSection(
                    "kizuki_demons_killed",
                    "Kizuki Demons Killed",
                    kizukiKills,
                    buildEntityBreakdown(player, MeditationStatsTracker.KIZUKI_KILLS_BY_ENTITY)
                ),
                new MeditationMenuData.InfoSection(
                    "non_kizuki_demons_killed",
                    "Non Kizuki Demons Killed",
                    nonKizukiKills,
                    buildEntityBreakdown(player, MeditationStatsTracker.NON_KIZUKI_KILLS_BY_ENTITY)
                )
            )
        );

        MeditationMenuData.InfoSection humansKilled = new MeditationMenuData.InfoSection(
            "humans_killed",
            "Humans Killed",
            humanKills,
            List.of(
                new MeditationMenuData.InfoSection(
                    "demon_slayers_killed",
                    "Demon Slayers Killed",
                    getInt(player, MeditationStatsTracker.HUMAN_KILLS_DEMON_SLAYER),
                    buildEntityBreakdown(player, MeditationStatsTracker.HUMAN_KILLS_DEMON_SLAYER_BY_ENTITY)
                ),
                new MeditationMenuData.InfoSection(
                    "swordsmiths_killed",
                    "Swordsmiths Killed",
                    getInt(player, MeditationStatsTracker.HUMAN_KILLS_SWORDSMITH),
                    buildEntityBreakdown(player, MeditationStatsTracker.HUMAN_KILLS_SWORDSMITH_BY_ENTITY)
                ),
                new MeditationMenuData.InfoSection(
                    "kakushi_killed",
                    "Kakushi Killed",
                    getInt(player, MeditationStatsTracker.HUMAN_KILLS_KAKUSHI),
                    buildEntityBreakdown(player, MeditationStatsTracker.HUMAN_KILLS_KAKUSHI_BY_ENTITY)
                ),
                new MeditationMenuData.InfoSection(
                    "civilians_killed",
                    "Civilians Killed",
                    getInt(player, MeditationStatsTracker.HUMAN_KILLS_CIVILIAN),
                    buildEntityBreakdown(player, MeditationStatsTracker.HUMAN_KILLS_CIVILIAN_BY_ENTITY)
                )
            )
        );

        MeditationMenuData.InfoSection blueSpiderLilyTea = new MeditationMenuData.InfoSection(
            "blue_spider_lily_tea_days",
            "Blue Spider Lily Tea Days",
            BlueSpiderLilyTeaHandler.getTeaDays(player),
            List.of()
        );

        return List.of(demonsKilled, humansKilled, blueSpiderLilyTea);
    }

    private static List<MeditationMenuData.LocationEntry> buildLocations(PlayerRole role) {
        List<MeditationMenuData.LocationEntry> locations = new ArrayList<>();
        switch (role) {
            case DEMON_SLAYER, KAKUSHI, SWORDSMITH:
                locations.addAll(CORPS_STRUCTURE_LOCATIONS);
                break;
            case DEMON:
                locations.add(new MeditationMenuData.LocationEntry("mt_yoko", "Mt. Yoko",
                        "A sacred mountain touched by the sun.", false));
                locations.add(new MeditationMenuData.LocationEntry("mt_natagumo", "Mt. Natagumo",
                        "Rui's mountain.", false));
                break;
            case CIVILIAN:
                locations.add(new MeditationMenuData.LocationEntry("local_village", "Nearby Village",
                        "A placeholder civilian navigation target.", false));
                break;
            case DEMON_SLAYER_IN_TRAINING, DEMON_TRANSFORMATION:
                break;
        }
        return locations;
    }

    private static List<MeditationMenuData.QuestEntry> applyQuestSelection(List<MeditationMenuData.QuestEntry> quests,
                                                                            String selectedType, String selectedId) {
        return quests.stream()
            .map(quest -> new MeditationMenuData.QuestEntry(
                quest.id(),
                quest.name(),
                quest.categoryLabel(),
                quest.categoryColor(),
                quest.description(),
                quest.rewards(),
                quest.progressText(),
                quest.completed(),
                SELECTED_TYPE_QUEST.equals(selectedType) && quest.id().equals(selectedId)
            ))
            .toList();
    }

    private static List<MeditationMenuData.LocationEntry> applyLocationSelection(List<MeditationMenuData.LocationEntry> locations,
                                                                                  String selectedType, String selectedId) {
        return locations.stream()
            .map(location -> new MeditationMenuData.LocationEntry(
                location.id(),
                location.name(),
                location.description(),
                SELECTED_TYPE_LOCATION.equals(selectedType) && location.id().equals(selectedId)
            ))
            .toList();
    }

    private static boolean selectionExists(String type, String id, List<MeditationMenuData.QuestEntry> quests,
                                           List<MeditationMenuData.LocationEntry> locations) {
        if (SELECTED_TYPE_QUEST.equals(type)) {
            return quests.stream().anyMatch(quest -> quest.id().equals(id));
        }
        if (SELECTED_TYPE_LOCATION.equals(type)) {
            return locations.stream().anyMatch(location -> location.id().equals(id));
        }
        return false;
    }

    public static PlayerRole resolveRoleForProgression(ServerPlayer player) {
        enforceTransformationRolePrecedence(player);
        enforceDemonRolePrecedence(player);
        if (DemonTransformationHandler.isTransforming(player)) {
            return PlayerRole.DEMON_TRANSFORMATION;
        }
        if (player.getPersistentData().getBoolean("oni")) {
            return PlayerRole.DEMON;
        }
        if (player.getPersistentData().getBoolean("swordsmith")) {
            return PlayerRole.SWORDSMITH;
        }
        if (hasAdvancement(player, KAKUSHI) || player.getPersistentData().getBoolean("kakushi")) {
            return PlayerRole.KAKUSHI;
        }
        if (hasAdvancement(player, COMPLETED_FINAL_SELECTION)) {
            return PlayerRole.DEMON_SLAYER;
        }
        if (hasNichirinOrTrainingSword(player)) {
            return PlayerRole.DEMON_SLAYER_IN_TRAINING;
        }
        return PlayerRole.CIVILIAN;
    }

    public static void enforceTransformationRolePrecedence(ServerPlayer player) {
        if (player == null || !DemonTransformationHandler.isTransforming(player)) {
            return;
        }

        player.getPersistentData().remove("kakushi");
        player.getPersistentData().putBoolean("swordsmith", false);

        revokeAdvancementIfPresent(player, KAKUSHI);
        revokeAdvancementIfPresent(player, COMPLETED_FINAL_SELECTION);
        revokeAdvancementIfPresent(player, DEMON_SLAYER_CORPS);
    }

    public static void enforceDemonRolePrecedence(ServerPlayer player) {
        if (player == null || !player.getPersistentData().getBoolean("oni")) {
            return;
        }

        player.getPersistentData().remove("kakushi");
        player.getPersistentData().putBoolean("swordsmith", false);

        revokeAdvancementIfPresent(player, KAKUSHI);
        revokeAdvancementIfPresent(player, COMPLETED_FINAL_SELECTION);
        revokeAdvancementIfPresent(player, DEMON_SLAYER_CORPS);
    }

    private static String resolveRank(ServerPlayer player, PlayerRole role) {
        if (role == PlayerRole.DEMON_TRANSFORMATION) {
            return "Transforming";
        }
        if (role == PlayerRole.DEMON) {
            return "Unranked Demon";
        }
        for (ResourceLocation id : RANK_ADVANCEMENTS) {
            if (hasAdvancement(player, id)) {
                return prettify(id.getPath());
            }
        }
        return role == PlayerRole.CIVILIAN ? "None" : "Unranked";
    }

    private static String resolveMuzanBlood(ServerPlayer player) {
        if (!player.getPersistentData().getBoolean("oni")) {
            return "";
        }
        return Integer.toString(DemonTransformationHandler.getTrackedMuzanBlood(player));
    }

    private static String resolveHumansConsumed(ServerPlayer player) {
        if (!player.getPersistentData().getBoolean("oni")) {
            return "";
        }
        return Integer.toString(DemonTransformationHandler.getTrackedHumansConsumed(player));
    }

    private static String resolveKizukiRank(ServerPlayer player) {
        DemonRank rank = DemonRankingSavedData.get(player.serverLevel()).getRankOf(player.getUUID());
        return rank == null ? "None" : rank.displayName();
    }

    private static MeditationMenuData.QuestEntry buildBloodyBattleEntry(ServerPlayer player) {
        DemonRankingSavedData data = DemonRankingSavedData.get(player.serverLevel());
        DemonRank myRank = data.getRankOf(player.getUUID());

        List<String> description = new ArrayList<>();
        description.add("Your rank: " + (myRank == null ? "Unranked" : myRank.displayName()));

        DemonRank targetRank = myRank == null ? DemonRank.LOWER_6 : myRank.above();
        String progressText;
        if (targetRank == null) {
            description.add("Next target: none — you hold the highest rank.");
            progressText = "Highest rank";
        } else {
            String targetName = describeHolder(player, data, targetRank);
            description.add("Next target: " + targetRank.displayName() + " — " + targetName);
            progressText = "Next: " + targetName;
        }

        description.add("");
        description.add("Roster:");
        for (DemonRank rank : DemonRank.values()) {
            description.add(rank.displayName() + ": " + describeHolder(player, data, rank));
        }

        return new MeditationMenuData.QuestEntry(
            "bloody_battle", "Bloody Battle", "Kizuki Rank", 0xAA0000,
            description, List.of(), progressText, false, false
        );
    }

    private static String describeHolder(ServerPlayer viewer, DemonRankingSavedData data, DemonRank rank) {
        UUID holderId = data.getHolder(rank);
        if (holderId != null) {
            ServerPlayer holder = viewer.server.getPlayerList().getPlayer(holderId);
            return holder != null ? holder.getName().getString() : "Offline player";
        }
        return entityDisplayName(rank) + " (entity)";
    }

    private static String entityDisplayName(DemonRank rank) {
        for (RegistryObject<? extends net.minecraft.world.entity.EntityType<?>> entity : rank.fallbackEntities()) {
            if (entity.isPresent()) {
                ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.get());
                if (id != null) {
                    return prettify(id.getPath());
                }
            }
        }
        return "Unclaimed";
    }

    private static boolean hasNichirinOrTrainingSword(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            if (isProgressionSword(stack)) {
                return true;
            }
        }
        return isProgressionSword(player.getMainHandItem()) ||
            isProgressionSword(player.getOffhandItem());
    }

    private static boolean isProgressionSword(ItemStack stack) {
        return TrainingSwordHelper.isTrainingSword(stack) || TrainingSwordHelper.isNichirinOrBreathingSword(stack);
    }

    private static boolean hasAdvancement(ServerPlayer player, ResourceLocation id) {
        Advancement advancement = player.server.getAdvancements().getAdvancement(id);
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    private static void revokeAdvancementIfPresent(ServerPlayer player, ResourceLocation id) {
        Advancement advancement = player.server.getAdvancements().getAdvancement(id);
        if (advancement == null) {
            return;
        }

        List<String> completedCriteria = new ArrayList<>();
        for (String criterion : player.getAdvancements().getOrStartProgress(advancement).getCompletedCriteria()) {
            completedCriteria.add(criterion);
        }
        for (String criterion : completedCriteria) {
            player.getAdvancements().revoke(advancement, criterion);
        }
    }

    private static String prettify(String raw) {
        String[] parts = raw.replace('_', ' ').split(" ");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            String lower = part.toLowerCase(Locale.ROOT);
            builder.append(Character.toUpperCase(lower.charAt(0)));
            if (lower.length() > 1) {
                builder.append(lower.substring(1));
            }
        }
        return builder.toString();
    }

    private static int getInt(ServerPlayer player, String key) {
        return player.getPersistentData().getInt(key);
    }

    private static List<MeditationMenuData.InfoSection> buildEntityBreakdown(ServerPlayer player, String key) {
        CompoundTag compound = player.getPersistentData().getCompound(key);
        Map<String, Integer> counts = new HashMap<>();
        for (String entityId : compound.getAllKeys()) {
            counts.put(entityId, compound.getInt(entityId));
        }

        return counts.entrySet().stream()
            .filter(entry -> entry.getValue() > 0)
            .sorted(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed()
                .thenComparing(Map.Entry::getKey))
            .map(entry -> new MeditationMenuData.InfoSection(
                key + ":" + entry.getKey(),
                prettifyEntityId(entry.getKey()),
                entry.getValue(),
                List.of()
            ))
            .toList();
    }

    private static String prettifyEntityId(String entityId) {
        ResourceLocation id = ResourceLocation.tryParse(entityId);
        if (id == null) {
            return entityId;
        }
        return prettify(id.getPath());
    }

}
