package com.lerdorf.kimetsunoyaibamultiplayer.progression;

import com.lerdorf.kimetsunoyaibamultiplayer.meditation.PassiveSkillManager;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.List;

public final class DemonSlayerRankManager {
    private static final String RANK_TAG = "KnYCustomDemonSlayerRank";
    private static final ResourceLocation CUSTOM_CORPS = ResourceLocation.fromNamespaceAndPath(
        "kimetsunoyaibamultiplayer", "demon_slayer_corps");
    private static final ResourceLocation COMPLETED_FINAL_SELECTION = ResourceLocation.fromNamespaceAndPath(
        "kimetsunoyaibamultiplayer", "completed_final_selectioni");
    private static final int BUFF_DURATION_TICKS = 60;

    private DemonSlayerRankManager() {
    }

    public static void ensureMizunoto(ServerPlayer player) {
        if (player == null || isDemon(player)) {
            return;
        }

        DemonSlayerRank current = getRank(player);
        if (current == null || !player.getPersistentData().contains(RANK_TAG)) {
            if (current == null || current.level() < DemonSlayerRank.MIZUNOTO.level()) {
                current = DemonSlayerRank.MIZUNOTO;
            }
            assignRank(player, current);
            return;
        }

        grantProgressionAdvancements(player, current);
        applyRankBuffs(player, current);
    }

    public static void assignRank(ServerPlayer player, DemonSlayerRank rank) {
        if (player == null || rank == null || isDemon(player)) {
            return;
        }

        player.getPersistentData().putInt(RANK_TAG, rank.level());
        grantProgressionAdvancements(player, rank);
        PassiveSkillManager.setDemonSlayerSkillPoints(player, rank.level());
        clearRankBuffs(player);
        applyRankBuffs(player, rank);
    }

    public static DemonSlayerRank getRank(ServerPlayer player) {
        if (player == null || isDemon(player)) {
            return null;
        }

        CompoundTag data = player.getPersistentData();
        if (data.contains(RANK_TAG)) {
            return DemonSlayerRank.fromLevel(data.getInt(RANK_TAG));
        }

        for (DemonSlayerRank rank : reverseRanks()) {
            if (hasAdvancement(player, rank.advancementId())) {
                return rank;
            }
        }

        // Final selection itself is the minimum proof of rank eligibility. This
        // repairs worlds where the rank advancement was awarded but rank setup
        // was interrupted before the player state was initialized.
        if (hasAdvancement(player, COMPLETED_FINAL_SELECTION)) {
            return DemonSlayerRank.MIZUNOTO;
        }
        return null;
    }

    public static String getDisplayName(ServerPlayer player) {
        DemonSlayerRank rank = getRank(player);
        return rank == null ? "Unranked" : rank.displayName();
    }

    public static void syncPlayer(ServerPlayer player) {
        if (player == null || isDemon(player)) {
            return;
        }

        if (!player.getPersistentData().contains(RANK_TAG)) {
            DemonSlayerRank inferred = getRank(player);
            if (inferred != null) {
                assignRank(player, inferred);
                return;
            }
        }

        DemonSlayerRank rank = getRank(player);
        if (rank != null) {
            applyRankBuffs(player, rank);
        }
    }

    public static void tick(ServerPlayer player) {
        if (player == null || player.level().getGameTime() % 20L != 0L) {
            return;
        }
        syncPlayer(player);
    }

    public static void copyOnClone(Player original, Player clone) {
        if (original == null || clone == null) {
            return;
        }

        CompoundTag originalData = original.getPersistentData();
        if (originalData.contains(RANK_TAG)) {
            clone.getPersistentData().putInt(RANK_TAG, originalData.getInt(RANK_TAG));
        }
    }

    private static void grantProgressionAdvancements(ServerPlayer player, DemonSlayerRank rank) {
        awardAdvancement(player, CUSTOM_CORPS);
        awardAdvancement(player, COMPLETED_FINAL_SELECTION);

        for (DemonSlayerRank candidate : DemonSlayerRank.values()) {
            if (candidate.level() <= rank.level()) {
                awardAdvancement(player, candidate.advancementId());
            } else {
                revokeAdvancement(player, candidate.advancementId());
            }
        }
    }

    private static boolean hasAdvancement(ServerPlayer player, ResourceLocation id) {
        Advancement advancement = player.server.getAdvancements().getAdvancement(id);
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    private static void awardAdvancement(ServerPlayer player, ResourceLocation id) {
        Advancement advancement = player.server.getAdvancements().getAdvancement(id);
        if (advancement == null) {
            return;
        }

        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
        List<String> criteria = new ArrayList<>();
        for (String criterion : progress.getRemainingCriteria()) {
            criteria.add(criterion);
        }
        for (String criterion : criteria) {
            player.getAdvancements().award(advancement, criterion);
        }
    }

    private static void revokeAdvancement(ServerPlayer player, ResourceLocation id) {
        Advancement advancement = player.server.getAdvancements().getAdvancement(id);
        if (advancement == null) {
            return;
        }

        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
        List<String> criteria = new ArrayList<>();
        for (String criterion : progress.getCompletedCriteria()) {
            criteria.add(criterion);
        }
        for (String criterion : criteria) {
            player.getAdvancements().revoke(advancement, criterion);
        }
    }

    private static void applyRankBuffs(ServerPlayer player, DemonSlayerRank rank) {
        int level = rank.level();
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            double health = switch (level) {
                case 1 -> 40.0D;
                case 2 -> 60.0D;
                case 3 -> 70.0D;
                case 4 -> 80.0D;
                case 5 -> 82.0D;
                case 6 -> 84.0D;
                case 7 -> 86.0D;
                case 8 -> 88.0D;
                case 9 -> 90.0D;
                case 10 -> 92.0D;
                case 11 -> 94.0D;
                case 12 -> 95.0D;
                default -> 20.0D;
            };
            maxHealth.setBaseValue(health);
            if (player.getHealth() > health) {
                player.setHealth((float) health);
            }
        }

        int speedAmplifier = switch (level) {
            case 1 -> 0;
            case 2 -> 1;
            case 3 -> 3;
            case 4, 5 -> 1;
            default -> 0;
        };
        int strengthAmplifier = level == 1 ? 0 : level - 2;
        if (level >= 11) {
            strengthAmplifier += 2;
        }
        strengthAmplifier = Math.min(11, strengthAmplifier);

        player.addEffect(new MobEffectInstance(
            MobEffects.MOVEMENT_SPEED, BUFF_DURATION_TICKS, speedAmplifier, true, false, true));
        player.addEffect(new MobEffectInstance(
            MobEffects.DAMAGE_BOOST, BUFF_DURATION_TICKS, strengthAmplifier, true, false, true));

        if (level >= 2) {
            int resistanceAmplifier = Math.min(2, level - 2);
            player.addEffect(new MobEffectInstance(
                MobEffects.DAMAGE_RESISTANCE, BUFF_DURATION_TICKS, resistanceAmplifier, true, false, true));
        }
    }

    private static void clearRankBuffs(ServerPlayer player) {
        player.removeEffect(MobEffects.MOVEMENT_SPEED);
        player.removeEffect(MobEffects.DAMAGE_BOOST);
        player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
    }

    private static boolean isDemon(ServerPlayer player) {
        return player.getPersistentData().getBoolean("oni");
    }

    private static List<DemonSlayerRank> reverseRanks() {
        List<DemonSlayerRank> ranks = new ArrayList<>(List.of(DemonSlayerRank.values()));
        java.util.Collections.reverse(ranks);
        return ranks;
    }
}
