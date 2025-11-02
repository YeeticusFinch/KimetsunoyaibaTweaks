package com.lerdorf.kimetsunoyaibamultiplayer.biome;

import com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedSpawnConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.util.EntityTagHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles wisteria forest protection mechanics.
 *
 * Wisteria forests are safe havens from demons:
 * - No demons can spawn in wisteria forests
 * - Demons cannot pathfind into wisteria forests
 * - Twelve kizuki demons immediately despawn if they enter wisteria forests
 *
 * Based on the lore that demons are repelled by wisteria flowers.
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WisteriaForestProtectionHandler {

    // All 3 wisteria forest biome types provide protection
    private static final ResourceLocation WISTERIA_FOREST_CYAN =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "wisteria_forest_cyan");
    private static final ResourceLocation WISTERIA_FOREST_CREAM =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "wisteria_forest_cream");
    private static final ResourceLocation WISTERIA_FOREST =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "wisteria_forest");  // Default lavender+pink

    /**
     * Check if entity is in any wisteria forest biome
     */
    private static boolean isInWisteriaForest(net.minecraft.world.entity.Entity entity) {
        return EntityTagHelper.isInBiome(entity, WISTERIA_FOREST_CYAN) ||
               EntityTagHelper.isInBiome(entity, WISTERIA_FOREST_CREAM) ||
               EntityTagHelper.isInBiome(entity, WISTERIA_FOREST);
    }

    /**
     * Prevent demons from spawning in wisteria forests
     */
    @SubscribeEvent
    public static void onPositionCheck(MobSpawnEvent.PositionCheck event) {
        // Check if enhanced spawning rules are enabled
        if (!EnhancedSpawnConfig.enhancedSpawningRules) {
            return;
        }

        LevelAccessor levelAccessor = event.getLevel();
        if (levelAccessor == null) {
            return;
        }

        // Check if this is a demon
        Mob mob = event.getEntity();
        if (mob == null) {
            return;
        }

        if (!EntityTagHelper.isDemon(mob)) {
            return;
        }

        // Check if spawn location is in any wisteria forest
        if (isInWisteriaForest(mob)) {
            // Deny spawn
            event.setResult(Event.Result.DENY);

            if (com.lerdorf.kimetsunoyaibamultiplayer.Config.logDebug) {
                System.out.println("[Wisteria Forest] Prevented demon spawn in wisteria forest: " +
                    EntityTagHelper.getEntityTypeId(mob));
            }
        }
    }

    /**
     * Prevent demons from targeting entities in wisteria forests
     * and prevent them from pathfinding into wisteria forests
     */
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        // Check if enhanced spawning rules are enabled
        if (!EnhancedSpawnConfig.enhancedSpawningRules) {
            return;
        }

        LivingEntity entity = event.getEntity();
        LivingEntity newTarget = event.getNewTarget();

        if (entity == null || newTarget == null) {
            return;
        }

        // Check if the attacker is a demon
        if (!EntityTagHelper.isDemon(entity)) {
            return;
        }

        // Check if the target is in any wisteria forest
        if (isInWisteriaForest(newTarget)) {
            // Cancel targeting - demons can't pursue targets into wisteria forests
            event.setCanceled(true);

            if (com.lerdorf.kimetsunoyaibamultiplayer.Config.logDebug) {
                System.out.println("[Wisteria Forest] Prevented demon from targeting entity in wisteria forest");
            }
        }
    }

    /**
     * Despawn twelve kizuki demons that enter wisteria forests
     * Regular demons are just prevented from targeting, but twelve kizuki are immediately removed
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        // Check if enhanced spawning rules are enabled
        if (!EnhancedSpawnConfig.enhancedSpawningRules) {
            return;
        }

        // Only run on server side
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }

        // Check if this is a twelve kizuki demon
        if (!EntityTagHelper.isTwelveKizuki(mob)) {
            return;
        }

        // Check if in any wisteria forest
        if (isInWisteriaForest(mob)) {
            // Despawn immediately
            mob.discard();

            if (com.lerdorf.kimetsunoyaibamultiplayer.Config.logDebug) {
                System.out.println("[Wisteria Forest] Despawned twelve kizuki in wisteria forest: " +
                    EntityTagHelper.getEntityTypeId(mob));
            }
        }
    }

    /**
     * Periodically check if twelve kizuki have wandered into wisteria forests
     * This is called via the entity tick event
     */
    @SubscribeEvent
    public static void onLivingTick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
        // Check if enhanced spawning rules are enabled
        if (!EnhancedSpawnConfig.enhancedSpawningRules) {
            return;
        }

        // Only check every 20 ticks (once per second)
        if (event.getEntity().tickCount % 20 != 0) {
            return;
        }

        // Only run on server side
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }

        // Check if this is a twelve kizuki demon
        if (!EntityTagHelper.isTwelveKizuki(mob)) {
            // For regular demons, just check if they're in any wisteria forest and clear their target
            if (EntityTagHelper.isDemon(mob) && isInWisteriaForest(mob)) {
                // Clear target and make them flee
                if (mob.getTarget() != null) {
                    mob.setTarget(null);
                }
            }
            return;
        }

        // Check if in any wisteria forest
        if (isInWisteriaForest(mob)) {
            // Despawn immediately
            mob.discard();

            if (com.lerdorf.kimetsunoyaibamultiplayer.Config.logDebug) {
                System.out.println("[Wisteria Forest] Despawned twelve kizuki that wandered into wisteria forest: " +
                    EntityTagHelper.getEntityTypeId(mob));
            }
        }
    }
}
