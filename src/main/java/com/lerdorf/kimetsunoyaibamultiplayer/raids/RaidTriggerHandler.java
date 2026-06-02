package com.lerdorf.kimetsunoyaibamultiplayer.raids;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.ModGameRules;
import com.lerdorf.kimetsunoyaibamultiplayer.config.RaidConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.DemonSlayerEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.effects.ModEffects;
import com.lerdorf.kimetsunoyaibamultiplayer.quest.QuestProgressionManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

/**
 * Triggers raids when players with omen effects enter civilian structures.
 * Also handles raid entity death tracking and prevents omen effects during raids.
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RaidTriggerHandler {

    private static final int RAID_PROXIMITY_CHECK_RADIUS = 200;
    private static final int KILLING_INTENT_MIN_SECONDS = 20;
    private static final int KILLING_INTENT_MAX_SECONDS = 40;
    private static final int KILLING_INTENT_MAX_LEVEL = 200;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Log.startupProbeOnce("RaidTriggerHandler.onPlayerTick");
        Player player = event.player;
        if (!(player.level() instanceof ServerLevel level)) return;

        // Master switches
        if (!RaidConfig.enableRaids.get() || !ModGameRules.areRaidsEnabled(level.getGameRules())) return;

        // Check only every 20 ticks (~1s)
        if (player.tickCount % 20 != 0) return;

        // Check omen effects
        MobEffectInstance muzanEffect = player.getEffect(ModEffects.OMEN_OF_MUZAN.get());
        MobEffectInstance ubuyashikiEffect = player.getEffect(ModEffects.OMEN_OF_UBUYASHIKI.get());

        boolean hasMuzan = muzanEffect != null;
        boolean hasUbuyashiki = ubuyashikiEffect != null;

        if (!hasMuzan && !hasUbuyashiki) return;

        Log.debugVisibleEvery(
            "raid-trigger-check:" + player.getUUID(),
            5000L,
            "Raid trigger check for {} at {} in {} (muzan={}, ubuyashiki={})",
            player.getName().getString(),
            player.blockPosition(),
            level.dimension().location(),
            hasMuzan,
            hasUbuyashiki
        );

        // Check if player is already near an active raid - don't trigger new raids
        if (RaidRegistry.hasRaidNearby(level, player.blockPosition(), RAID_PROXIMITY_CHECK_RADIUS)
            || SurvivalRaidRegistry.hasRaidNearby(level, player.blockPosition(), RAID_PROXIMITY_CHECK_RADIUS)) {
            Log.debug("Player is near active raid, not triggering new raid");
            return;
        }

        // Check structure at player location
        long structureLookupStart = System.currentTimeMillis();
        Optional<StructureLocationCache.CachedStructure> cached = StructureLocationCache.getStructureAt(level, player.blockPosition());
        long structureLookupElapsed = System.currentTimeMillis() - structureLookupStart;
        if (structureLookupElapsed >= 50L) {
            Log.debugVisibleEvery(
                "raid-structure-lookup:" + player.getUUID(),
                5000L,
                "Raid structure lookup took {} ms for {} at {}",
                structureLookupElapsed,
                player.getName().getString(),
                player.blockPosition()
            );
        }
        if (cached.isEmpty()) return;

        StructureLocationCache.CachedStructure s = cached.get();

        // Only trigger at civilian structures
        if (!CivilianStructureRegistry.isCivilianStructure(s.structureId)) return;

        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
            && QuestProgressionManager.isRaidSuppressedForPlayer(serverPlayer, s.structureId)) {
            return;
        }

        // Already an active raid at this specific structure?
        if (RaidRegistry.hasRaid(level, s.structureId, s.center)) return;

        // Also check for any raids within 200 blocks of this structure
        if (RaidRegistry.hasRaidNearby(level, s.center, RAID_PROXIMITY_CHECK_RADIUS)) {
            Log.debug("Active raid too close to structure " + s.structureId + ", not triggering");
            return;
        }

        // Determine raid type
        KnYRaid.RaidType type;
        int difficultyLevel = 1;

        if (hasMuzan) {
            type = KnYRaid.RaidType.DEMON;
            difficultyLevel = muzanEffect.getAmplifier() + 1;

            // Demon raids can only start at night (13000-23000 is night time)
            long timeOfDay = level.getDayTime() % 24000;
            if (timeOfDay < 13000 || timeOfDay > 23000) {
                // It's daytime - don't start demon raid
                return;
            }
        } else {
            type = KnYRaid.RaidType.SLAYER;
            difficultyLevel = ubuyashikiEffect.getAmplifier() + 1;
            // Demon slayer raids can start at any time
        }

        // Clamp to valid range
        difficultyLevel = Math.max(1, Math.min(5, difficultyLevel));

        // Start raid
        RaidRegistry.getOrCreate(level, s.structureId, s.center, type, difficultyLevel);

        // Remove omen effect from triggering player
        player.removeEffect(ModEffects.OMEN_OF_MUZAN.get());
        player.removeEffect(ModEffects.OMEN_OF_UBUYASHIKI.get());

        Log.debug("Started " + type + " raid level " + difficultyLevel + " at " + s.center);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.getServer() == null) return;

        // Tick all raids in all dimensions
        for (ServerLevel level : event.getServer().getAllLevels()) {
            RaidRegistry.tickAll(level);
            SurvivalRaidRegistry.tickAll(level);
        }
    }

    /**
     * Track entity deaths to update raid progress.
     */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level)) return;

        // Notify raid registry of entity death
        RaidRegistry.get(level).onEntityKilled(entity.getUUID());
        SurvivalRaidRegistry.onEntityKilled(level, entity.getUUID());
        FinalSelectionProcedure.onEntityKilledStatic(entity.getUUID());

        if (!(event.getSource().getEntity() instanceof LivingEntity killer)) return;
        tryApplyKillingIntentFromRaidKill(killer, entity);
    }

    /**
     * Track entity damage to update raid boss bar progress.
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level)) return;

        // Notify raid registry of damage
        RaidRegistry.get(level).onEntityDamaged(entity.getUUID(), event.getAmount());
    }

    /**
     * Check if a player is near an active raid.
     * Used to prevent giving omen effects during raids.
     */
    public static boolean isPlayerNearRaid(Player player) {
        if (!(player.level() instanceof ServerLevel level)) return false;
        return RaidRegistry.hasRaidNearby(level, player.blockPosition(), RAID_PROXIMITY_CHECK_RADIUS)
            || SurvivalRaidRegistry.hasRaidNearby(level, player.blockPosition(), RAID_PROXIMITY_CHECK_RADIUS);
    }

    /**
     * Check if an entity is part of any active raid.
     * Used to prevent giving omen effects when killing/damaging raid entities.
     */
    public static boolean isRaidEntity(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return false;
        return RaidRegistry.get(level).isRaidEntity(entity.getUUID())
            || SurvivalRaidRegistry.isRaidEntity(level, entity.getUUID())
            || FinalSelectionProcedure.isRaidEntityStatic(entity.getUUID());
    }

    private static void tryApplyKillingIntentFromRaidKill(LivingEntity killer, LivingEntity target) {
        if (!(killer instanceof Player) && !(killer instanceof DemonSlayerEntity)) {
            return;
        }

        boolean targetIsDemon = Damager.isDemon(target);
        boolean targetIsDemonSlayer = Damager.isDemonSlayer(target);

        boolean inFinalSelectionRaid = target.getPersistentData().hasUUID("FinalSelectionRaidId");
        String raidType = target.getPersistentData().getString("RaidType");
        boolean inDemonRaid = "DEMON".equals(raidType);
        boolean inSlayerRaid = "SLAYER".equals(raidType);

        boolean qualifies =
            (inFinalSelectionRaid && targetIsDemon)
                || (inDemonRaid && targetIsDemon)
                || (inSlayerRaid && targetIsDemonSlayer && killer instanceof Player);

        if (!qualifies) {
            return;
        }

        MobEffectInstance current = killer.getEffect(ModEffects.KILLING_INTENT.get());
        int nextAmplifier = current == null ? 0 : Math.min(KILLING_INTENT_MAX_LEVEL - 1, current.getAmplifier() + 1);

        int bonusSeconds = KILLING_INTENT_MIN_SECONDS + killer.level().random.nextInt(KILLING_INTENT_MAX_SECONDS - KILLING_INTENT_MIN_SECONDS + 1);
        int totalDuration = bonusSeconds * 20;
        if (current != null) {
            totalDuration += current.getDuration();
        }

        // No bubbles/particles. Keep icon visible for the custom bone icon.
        killer.addEffect(new MobEffectInstance(
            ModEffects.KILLING_INTENT.get(),
            totalDuration,
            nextAmplifier,
            false,
            false,
            true
        ));
    }
}
