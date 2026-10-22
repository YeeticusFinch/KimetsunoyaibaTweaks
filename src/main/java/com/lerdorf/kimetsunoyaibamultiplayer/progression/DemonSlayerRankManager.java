package com.lerdorf.kimetsunoyaibamultiplayer.progression;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.config.CustomProgressionConfig;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class DemonSlayerRankManager {
    private static final String RANK_TAG = "KnYCustomDemonSlayerRank";
    private static final String SLAYER_EXPERIENCE_TAG = "KnYSlayerExperience";
    private static final String DEMON_DAMAGE_CONTRIBUTORS_TAG = "KnYDemonDamageContributors";
    private static final String CONTRIBUTOR_UUID_TAG = "Player";
    private static final String RANK_BUFFS_ACTIVE_TAG = "KnYRankBuffsActive";
    private static final ResourceLocation CUSTOM_CORPS = ResourceLocation.fromNamespaceAndPath(
        "kimetsunoyaibamultiplayer", "demon_slayer_corps");
    private static final ResourceLocation COMPLETED_FINAL_SELECTION = ResourceLocation.fromNamespaceAndPath(
        "kimetsunoyaibamultiplayer", "completed_final_selectioni");
    private static final int BUFF_DURATION_TICKS = MobEffectInstance.INFINITE_DURATION;
    private static final int BUFF_REFRESH_INTERVAL_TICKS = 20 * 20;
    private static final int DIRECT_DEMON_KILL_EXPERIENCE = 10;
    private static final int ASSISTED_DEMON_KILL_EXPERIENCE = 5;

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
        return rank == null ? -1 : rank.level() - 1;
    }

    public static int getResistanceAmplifier(DemonSlayerRank rank) {
        if (rank == null) {
            return -1;
        }
        return switch (rank.level()) {
            case 1, 2 -> 0; // Resistance I
            case 3, 4, 5, 6 -> 1; // Resistance II
            case 7, 8, 9, 10 -> 2; // Resistance III
            case 11, 12 -> 3; // Resistance IV
            default -> -1;
        };
    }

    public static int getHealthBoostAmplifier(DemonSlayerRank rank) {
        if (rank == null) {
            return -1;
        }
        return switch (rank.level()) {
            case 1, 2 -> 0; // Health Boost I
            case 3, 4, 5 -> 1; // Health Boost II
            case 6, 7, 8 -> 2; // Health Boost III
            case 9, 10 -> 3; // Health Boost IV
            case 11, 12 -> 4; // Health Boost V
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

    public static void recordDemonDamage(LivingEntity target, DamageSource source) {
        if (target == null || source == null || target.level().isClientSide() || !Damager.isDemon(target)) {
            return;
        }
        ServerPlayer player = getPlayerFromDamageSource(source);
        if (!canGainSlayerExperience(player)) {
            return;
        }

        ListTag contributors = target.getPersistentData().getList(
            DEMON_DAMAGE_CONTRIBUTORS_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < contributors.size(); i++) {
            if (contributors.getCompound(i).hasUUID(CONTRIBUTOR_UUID_TAG)
                && contributors.getCompound(i).getUUID(CONTRIBUTOR_UUID_TAG).equals(player.getUUID())) {
                return;
            }
        }

        CompoundTag contributor = new CompoundTag();
        contributor.putUUID(CONTRIBUTOR_UUID_TAG, player.getUUID());
        contributors.add(contributor);
        target.getPersistentData().put(DEMON_DAMAGE_CONTRIBUTORS_TAG, contributors);
    }

    public static void awardDemonKillExperience(LivingEntity target, DamageSource source) {
        if (target == null || target.level().isClientSide() || !Damager.isDemon(target)) {
            return;
        }

        ListTag contributors = target.getPersistentData().getList(
            DEMON_DAMAGE_CONTRIBUTORS_TAG, Tag.TAG_COMPOUND);
        target.getPersistentData().remove(DEMON_DAMAGE_CONTRIBUTORS_TAG);

        ServerPlayer killer = source == null ? null : getPlayerFromDamageSource(source);
        if (canGainSlayerExperience(killer)) {
            addSlayerExperience(killer, DIRECT_DEMON_KILL_EXPERIENCE);
        }

        for (int i = 0; i < contributors.size(); i++) {
            CompoundTag contributor = contributors.getCompound(i);
            if (!contributor.hasUUID(CONTRIBUTOR_UUID_TAG)) {
                continue;
            }
            UUID contributorId = contributor.getUUID(CONTRIBUTOR_UUID_TAG);
            if (killer != null && contributorId.equals(killer.getUUID())) {
                continue;
            }
            if (target.level().getServer() == null) {
                continue;
            }
            ServerPlayer player = target.level().getServer().getPlayerList().getPlayer(contributorId);
            if (canGainSlayerExperience(player)) {
                addSlayerExperience(player, ASSISTED_DEMON_KILL_EXPERIENCE);
            }
        }
    }

    public static int getRequiredExperience(DemonSlayerRank rank) {
        if (rank == null || rank.level() <= 1) {
            return 0;
        }
        long total = 0L;
        for (int level = 2; level <= rank.level(); level++) {
            total += CustomProgressionConfig.getSlayerExperienceForRank(DemonSlayerRank.fromLevel(level));
        }
        return total >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
    }

    private static ServerPlayer getPlayerFromDamageSource(DamageSource source) {
        ServerPlayer player = getPlayerFromEntity(source.getEntity());
        if (player != null) {
            return player;
        }
        return getPlayerFromEntity(source.getDirectEntity());
    }

    private static ServerPlayer getPlayerFromEntity(Entity entity) {
        if (entity instanceof ServerPlayer player) {
            return player;
        }
        if (entity instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer player) {
            return player;
        }
        return null;
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
