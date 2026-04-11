package com.lerdorf.kimetsunoyaibamultiplayer.meditation;

import com.lerdorf.kimetsunoyaibamultiplayer.config.CustomProgressionConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.OpenMeditationMenuPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.quest.PlayerRole;
import com.lerdorf.kimetsunoyaibamultiplayer.quest.QuestProgressionManager;
import com.lerdorf.kimetsunoyaibamultiplayer.util.TrainingSwordHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

public final class MeditationMenuService {
    public static final String SELECTED_TYPE_QUEST = "quest";
    public static final String SELECTED_TYPE_LOCATION = "location";

    private static final ResourceLocation COMPLETED_FINAL_SELECTION =
        ResourceLocation.parse("kimetsunoyaibamultiplayer:completed_final_selectioni");
    private static final ResourceLocation KAKUSHI =
        ResourceLocation.parse("kimetsunoyaibamultiplayer:kakushi");
    private static final List<MeditationMenuData.LocationEntry> CORPS_STRUCTURE_LOCATIONS = List.of(
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
                "None",
                List.of(),
                List.of(),
                List.of(),
                "",
                ""
            );
        }

        PlayerRole role = resolveRoleForProgression(player);
        String rank = resolveRank(player, role);
        String muzanBlood = resolveMuzanBlood(player);
        String kizukiRank = resolveKizukiRank(player);

        List<MeditationMenuData.InfoSection> infoSections = buildInfoSections(player);
        List<MeditationMenuData.QuestEntry> quests = QuestProgressionManager.buildQuestEntries(player, role);
        List<MeditationMenuData.LocationEntry> locations = buildLocations(role);

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

        return new MeditationMenuData(role.getDisplayName(), rank, muzanBlood, kizukiRank,
            infoSections, quests, locations, selectedType, selectedId);
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

        return List.of(demonsKilled, humansKilled);
    }

    private static List<MeditationMenuData.LocationEntry> buildLocations(PlayerRole role) {
        List<MeditationMenuData.LocationEntry> locations = new ArrayList<>();
        switch (role) {
            case DEMON_SLAYER, KAKUSHI, SWORDSMITH -> locations.addAll(CORPS_STRUCTURE_LOCATIONS);
            case DEMON -> locations.add(new MeditationMenuData.LocationEntry("night_hunt", "Night Hunting Grounds", "A placeholder demon navigation target.", false));
            case CIVILIAN -> locations.add(new MeditationMenuData.LocationEntry("local_village", "Nearby Village", "A placeholder civilian navigation target.", false));
            case DEMON_SLAYER_IN_TRAINING -> {
            }
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

    private static String resolveRank(ServerPlayer player, PlayerRole role) {
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
        String[] keys = {"muzan_blood", "blood_of_muzan", "bloodmuzan"};
        for (String key : keys) {
            int value = player.getPersistentData().getInt(key);
            if (value > 0) {
                return Integer.toString(value);
            }
        }
        return "0";
    }

    private static String resolveKizukiRank(ServerPlayer player) {
        String[] stringKeys = {"kizuki_rank", "kizukiRank", "moon_rank", "moonRank"};
        for (String key : stringKeys) {
            String value = player.getPersistentData().getString(key);
            if (!value.isBlank()) {
                return value;
            }
        }
        if (player.getPersistentData().getBoolean("upper_moon")) {
            return "Upper Moon";
        }
        if (player.getPersistentData().getBoolean("lower_moon")) {
            return "Lower Moon";
        }
        return "None";
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
