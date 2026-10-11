package com.lerdorf.kimetsunoyaibamultiplayer.progression;

import com.lerdorf.kimetsunoyaibamultiplayer.meditation.PassiveSkillManager;
import com.lerdorf.kimetsunoyaibamultiplayer.quest.QuestProgressionManager;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class DemonSlayerRankManager {
    private static final String RANK_TAG = "KnYCustomDemonSlayerRank";
    private static final String SLAYER_EXPERIENCE_TAG = "KnYSlayerExperience";
    private static final String RANK_BUFFS_ACTIVE_TAG = "KnYRankBuffsActive";
    private static final ResourceLocation CUSTOM_CORPS = ResourceLocation.fromNamespaceAndPath(
        "kimetsunoyaibamultiplayer", "demon_slayer_corps");
    private static final ResourceLocation COMPLETED_FINAL_SELECTION = ResourceLocation.fromNamespaceAndPath(
        "kimetsunoyaibamultiplayer", "completed_final_selectioni");
    private static final int BUFF_DURATION_TICKS = MobEffectInstance.INFINITE_DURATION;
    private static final int BUFF_REFRESH_INTERVAL_TICKS = 20 * 20;
    private static final ThreadLocal<ServerPlayer> BREATHING_DAMAGE_PLAYER = new ThreadLocal<>();

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
            assignRank(player, current, true);
            return;
        }

        grantProgressionAdvancements(player, current);
        applyRankBuffs(player, current);
    }

    public static void assignRank(ServerPlayer player, DemonSlayerRank rank) {
        assignRank(player, rank, false);
    }

    public static void assignRank(ServerPlayer player, DemonSlayerRank rank, boolean announce) {
        if (player == null || rank == null) {
            return;
        }

        DemonSlayerRank previous = getRank(player);
        player.getPersistentData().putInt(RANK_TAG, rank.level());
        grantProgressionAdvancements(player, rank);
        PassiveSkillManager.setDemonSlayerSkillPoints(player, rank.level());
        if (previous != null && rank.level() < previous.level()) {
            clearRankBuffs(player);
        }
        applyRankBuffs(player, rank);
        if (announce) {
            announceRankUp(player, rank);
        }
    }

    public static DemonSlayerRank getRank(ServerPlayer player) {
        if (player == null) {
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

    public static int getSpeedAmplifier(ServerPlayer player) {
        DemonSlayerRank rank = getRank(player);
        return getSpeedAmplifier(rank);
    }

    public static int getSpeedAmplifier(DemonSlayerRank rank) {
        return rank == null ? -1 : rank.level() <= 5 ? 0 : 1;
    }

    public static int getStrengthAmplifier(DemonSlayerRank rank) {
        if (rank == null) {
            return -1;
        }
        int amplifier = rank.level() == 1 ? 0 : rank.level() - 2;
        if (rank.level() >= 11) {
            amplifier += 2;
        }
        return Math.min(11, amplifier);
    }

    public static int getResistanceAmplifier(DemonSlayerRank rank) {
        if (rank == null) {
            return -1;
        }
        return rank.level() <= 5 ? 0 : rank.level() <= 10 ? 1 : 2;
    }

    public static int getHealthBoostAmplifier(DemonSlayerRank rank) {
        if (rank == null) {
            return -1;
        }
        return switch (rank.level()) {
            case 2, 3 -> 0;
            case 4 -> 1;
            case 5 -> 2;
            case 6 -> 3;
            case 7 -> 4;
            case 8 -> 5;
            case 9 -> 6;
            case 10 -> 7;
            case 11 -> 8;
            case 12 -> 9;
            default -> -1;
        };
    }

    public static void refreshSpeedEffect(ServerPlayer player) {
        int speedAmplifier = getSpeedAmplifier(player);
        if (speedAmplifier < 0) {
            return;
        }
        refreshRankEffect(player, MobEffects.MOVEMENT_SPEED, speedAmplifier);
    }

    public static void syncPlayer(ServerPlayer player) {
        if (player == null) {
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
        } else if (player.getPersistentData().getBoolean(RANK_BUFFS_ACTIVE_TAG)) {
            clearRankBuffs(player);
        }
    }

    public static int getSlayerExperience(ServerPlayer player) {
        return player == null ? 0 : Math.max(0, player.getPersistentData().getInt(SLAYER_EXPERIENCE_TAG));
    }

    public static void addSlayerExperience(ServerPlayer player, int amount) {
        if (!canGainSlayerExperience(player) || amount <= 0) {
            return;
        }
        player.getPersistentData().putInt(SLAYER_EXPERIENCE_TAG, getSlayerExperience(player) + amount);
        updateRankFromExperience(player);
    }

    public static void addBreathingDamageExperience(ServerPlayer player, float damage) {
        addSlayerExperience(player, Math.max(0, (int) Math.floor(damage)));
    }

    public static void beginBreathingDamage(ServerPlayer player) {
        BREATHING_DAMAGE_PLAYER.set(Objects.requireNonNull(player));
    }

    public static void endBreathingDamage() {
        BREATHING_DAMAGE_PLAYER.remove();
    }

    public static boolean isBreathingDamageInProgress(ServerPlayer player) {
        return BREATHING_DAMAGE_PLAYER.get() == player;
    }

    public static void addNichirinAttackExperience(ServerPlayer player, float damage) {
        if (!canGainSlayerExperience(player) || damage <= 0.0F) {
            return;
        }
        int points = Math.max(0, (int) Math.floor(damage));
        int awarded = 0;
        for (int i = 0; i < points; i++) {
            if (player.getRandom().nextBoolean()) {
                awarded++;
            }
        }
        addSlayerExperience(player, awarded);
    }

    public static int getRequiredExperience(DemonSlayerRank rank) {
        if (rank == null || rank.level() <= 1) {
            return 0;
        }
        int total = 0;
        for (int level = 2; level <= rank.level(); level++) {
            total += level * 50;
        }
        return total;
    }

    private static boolean canGainSlayerExperience(ServerPlayer player) {
        return player != null && !isDemon(player) && getRank(player) != null;
    }

    private static void updateRankFromExperience(ServerPlayer player) {
        DemonSlayerRank current = getRank(player);
        DemonSlayerRank maximum = QuestProgressionManager.getDemonSlayerRankCap(player);
        if (current == null || maximum == null) {
            return;
        }

        DemonSlayerRank target = current;
        int experience = getSlayerExperience(player);
        for (DemonSlayerRank candidate : DemonSlayerRank.values()) {
            if (candidate.level() > current.level()
                && candidate.level() <= maximum.level()
                && experience >= getRequiredExperience(candidate)) {
                target = candidate;
            }
        }
        if (target != current) {
            assignRank(player, target, true);
        }
    }

    public static void tick(ServerPlayer player) {
        if (player == null || player.level().getGameTime() % BUFF_REFRESH_INTERVAL_TICKS != 0L) {
            return;
        }
        syncPlayer(player);
        updateRankFromExperience(player);
    }

    public static void copyOnClone(Player original, Player clone) {
        if (original == null || clone == null) {
            return;
        }

        CompoundTag originalData = original.getPersistentData();
        if (originalData.contains(RANK_TAG)) {
            clone.getPersistentData().putInt(RANK_TAG, originalData.getInt(RANK_TAG));
        }
        if (originalData.contains(SLAYER_EXPERIENCE_TAG)) {
            clone.getPersistentData().putInt(SLAYER_EXPERIENCE_TAG, originalData.getInt(SLAYER_EXPERIENCE_TAG));
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
        int speedAmplifier = getSpeedAmplifier(rank);
        int strengthAmplifier = getStrengthAmplifier(rank);

        refreshRankEffect(player, MobEffects.MOVEMENT_SPEED, speedAmplifier);
        refreshRankEffect(player, MobEffects.DAMAGE_BOOST, strengthAmplifier);

        int resistanceAmplifier = getResistanceAmplifier(rank);
        refreshRankEffect(player, MobEffects.DAMAGE_RESISTANCE, resistanceAmplifier);

        int healthBoostAmplifier = getHealthBoostAmplifier(rank);
        if (healthBoostAmplifier >= 0) {
            refreshRankEffect(player, MobEffects.HEALTH_BOOST, healthBoostAmplifier);
        } else {
            player.removeEffect(MobEffects.HEALTH_BOOST);
        }
        player.getPersistentData().putBoolean(RANK_BUFFS_ACTIVE_TAG, true);
    }

    private static void refreshRankEffect(ServerPlayer player, MobEffect effect, int amplifier) {
        MobEffectInstance current = player.getEffect(effect);
        MobEffectInstance refreshed = new MobEffectInstance(
            effect, BUFF_DURATION_TICKS, amplifier, true, false, false);
        if (current != null && current.getAmplifier() == amplifier) {
            // Update the existing instance so HEALTH_BOOST does not remove and
            // reapply its max-health modifier on every rank refresh.
            current.update(refreshed);
            return;
        }
        player.addEffect(refreshed);
    }

    private static void clearRankBuffs(ServerPlayer player) {
        player.removeEffect(MobEffects.MOVEMENT_SPEED);
        player.removeEffect(MobEffects.DAMAGE_BOOST);
        player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        player.removeEffect(MobEffects.HEALTH_BOOST);
        player.getPersistentData().putBoolean(RANK_BUFFS_ACTIVE_TAG, false);
    }

    private static void announceRankUp(ServerPlayer player, DemonSlayerRank rank) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 10));
        player.connection.send(new ClientboundSetSubtitleTextPacket(
            Component.literal(rank.displayName()).withStyle(ChatFormatting.RED)));
        player.connection.send(new ClientboundSetTitleTextPacket(
            Component.literal(rank.kanji()).withStyle(ChatFormatting.WHITE)));
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
