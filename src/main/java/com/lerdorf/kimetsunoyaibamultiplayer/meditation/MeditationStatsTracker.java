package com.lerdorf.kimetsunoyaibamultiplayer.meditation;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.util.EntityTagHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
public final class MeditationStatsTracker {
    public static final String DEMON_KILLS_TOTAL = "MeditationDemonKills";
    public static final String KIZUKI_KILLS_TOTAL = "MeditationKizukiKills";
    public static final String HUMAN_KILLS_TOTAL = "MeditationHumanKills";
    public static final String DEMON_KILLS_BY_ENTITY = "MeditationDemonKillsByEntity";
    public static final String KIZUKI_KILLS_BY_ENTITY = "MeditationKizukiKillsByEntity";
    public static final String NON_KIZUKI_KILLS_BY_ENTITY = "MeditationNonKizukiKillsByEntity";
    public static final String HUMAN_KILLS_DEMON_SLAYER = "MeditationHumanKillsDemonSlayer";
    public static final String HUMAN_KILLS_DEMON_SLAYER_BY_ENTITY = "MeditationHumanKillsDemonSlayerByEntity";
    public static final String HUMAN_KILLS_SWORDSMITH = "MeditationHumanKillsSwordsmith";
    public static final String HUMAN_KILLS_SWORDSMITH_BY_ENTITY = "MeditationHumanKillsSwordsmithByEntity";
    public static final String HUMAN_KILLS_KAKUSHI = "MeditationHumanKillsKakushi";
    public static final String HUMAN_KILLS_KAKUSHI_BY_ENTITY = "MeditationHumanKillsKakushiByEntity";
    public static final String HUMAN_KILLS_CIVILIAN = "MeditationHumanKillsCivilian";
    public static final String HUMAN_KILLS_CIVILIAN_BY_ENTITY = "MeditationHumanKillsCivilianByEntity";
    private static final TagKey<EntityType<?>> WOMAN =
        TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("forge", "woman"));
    private static final ResourceLocation KAKUSHI_ID =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "kakushi");

    private MeditationStatsTracker() {
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        Entity source = event.getSource().getEntity();
        if (!(source instanceof Player player) || player.level().isClientSide()) {
            return;
        }

        LivingEntity victim = event.getEntity();
        ResourceLocation victimId = EntityType.getKey(victim.getType());
        if (victimId == null) {
            return;
        }

        if (EntityTagHelper.isDemon(victim)) {
            increment(player, DEMON_KILLS_TOTAL);
            incrementEntityMap(player, DEMON_KILLS_BY_ENTITY, victimId);
            if (EntityTagHelper.isTwelveKizuki(victim)) {
                increment(player, KIZUKI_KILLS_TOTAL);
                incrementEntityMap(player, KIZUKI_KILLS_BY_ENTITY, victimId);
            } else {
                incrementEntityMap(player, NON_KIZUKI_KILLS_BY_ENTITY, victimId);
            }
            return;
        }

        if (isTrackedHuman(victim, victimId)) {
            increment(player, HUMAN_KILLS_TOTAL);
            if (isKakushi(victimId)) {
                increment(player, HUMAN_KILLS_KAKUSHI);
                incrementEntityMap(player, HUMAN_KILLS_KAKUSHI_BY_ENTITY, victimId);
            } else if (EntityTagHelper.isSwordSmith(victim)) {
                increment(player, HUMAN_KILLS_SWORDSMITH);
                incrementEntityMap(player, HUMAN_KILLS_SWORDSMITH_BY_ENTITY, victimId);
            } else if (EntityTagHelper.isDemonSlayer(victim) || victim instanceof Player) {
                increment(player, HUMAN_KILLS_DEMON_SLAYER);
                incrementEntityMap(player, HUMAN_KILLS_DEMON_SLAYER_BY_ENTITY, victimId);
            } else {
                increment(player, HUMAN_KILLS_CIVILIAN);
                incrementEntityMap(player, HUMAN_KILLS_CIVILIAN_BY_ENTITY, victimId);
            }
        }
    }

    private static void increment(Player player, String key) {
        player.getPersistentData().putInt(key, player.getPersistentData().getInt(key) + 1);
    }

    private static void incrementEntityMap(Player player, String key, ResourceLocation entityId) {
        CompoundTag root = player.getPersistentData();
        CompoundTag counts = root.getCompound(key);
        String id = entityId.toString();
        counts.putInt(id, counts.getInt(id) + 1);
        root.put(key, counts);
    }

    private static boolean isTrackedHuman(LivingEntity victim, ResourceLocation victimId) {
        return victim instanceof Player ||
            isKakushi(victimId) ||
            EntityTagHelper.isSwordSmith(victim) ||
            EntityTagHelper.isDemonSlayer(victim) ||
            EntityTagHelper.isCivilian(victim) ||
            victim.getType().is(WOMAN);
    }

    private static boolean isKakushi(ResourceLocation victimId) {
        return KAKUSHI_ID.equals(victimId);
    }
}
