package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedSpawnConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.util.EntityTagHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraft.world.level.LightLayer;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

/**
 * Core handler for enhanced spawning rules.
 *
 * Implements comprehensive spawning restrictions including:
 * - Generic spawn rate reductions for demons and demon slayers
 * - Prevention of twelve_kizuki, hashira, and kamaboko natural spawning
 * - Biome-specific spawning rules
 * - Dimension-specific spawning rules (Mt Fujikasane)
 * - Maximum entity limits per area
 * - Day/night restrictions
 *
 * Only active when enhanced_spawning_rules config is enabled.
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EnhancedSpawnRulesHandler {

    private static final Random RANDOM = new Random();

    /**
     * Main spawn event handler - applies all enhanced spawning rules
     */
    @SubscribeEvent
    public static void onMobSpawn(MobSpawnEvent.FinalizeSpawn event) {
        // Check if enhanced spawning rules are enabled
        if (!EnhancedSpawnConfig.enhancedSpawningRules) {
            return;
        }

        // Only affect natural AND structure spawns
        // Structure spawns need to be processed for max entity limits
        if (event.getSpawnType() != MobSpawnType.NATURAL &&
            event.getSpawnType() != MobSpawnType.STRUCTURE &&
            event.getSpawnType() != MobSpawnType.SPAWNER) {
            return;
        }

        Mob entity = event.getEntity();
        LevelAccessor levelAccessor = event.getLevel();
        BlockPos pos = BlockPos.containing(event.getX(), event.getY(), event.getZ());

        if (!(levelAccessor instanceof Level level)) {
            return;
        }

        // Get entity type info
        EntityType<?> entityType = entity.getType();
        ResourceLocation entityId = EntityType.getKey(entityType);

        // Only affect kimetsunoyaiba mod entities (and kimetsu mod if loaded)
        if (entityId == null ||
            (!entityId.getNamespace().equals("kimetsunoyaiba") && !entityId.getNamespace().equals("kimetsu"))) {
            return;
        }

        try {
            // Apply rules in order of priority

            // 1. Apply dimension-specific rules (these allow some spawns that would otherwise be denied)
            if (shouldDenyByDimension(entity, level, pos)) {
                event.setSpawnCancelled(true);
                event.setCanceled(true);
                return;
            }

            // 2. Apply biome-specific rules (these allow some spawns that would otherwise be denied)
            if (shouldDenyByBiome(entity, level, pos)) {
                event.setSpawnCancelled(true);
                event.setCanceled(true);
                return;
            }

            // 3. Check generic restrictions ONLY for natural spawns (not structure spawns)
            // Structures/biomes/dimensions can override these restrictions
            if (event.getSpawnType() == MobSpawnType.NATURAL) {
                if (shouldDenyGenericSpawn(entity, level, pos)) {
                    event.setSpawnCancelled(true);
                    event.setCanceled(true);
                    return;
                }
            }

            // 4. Apply maximum entity limits (applies to all spawn types)
            if (shouldDenyByMaxLimit(entity, level, pos)) {
                event.setSpawnCancelled(true);
                event.setCanceled(true);
                return;
            }

            // 5. Apply day/night restrictions
            if (shouldDenyByDayNight(entity, level, pos)) {
                event.setSpawnCancelled(true);
                event.setCanceled(true);
                return;
            }

            // 6. Apply generic spawn rate reductions (only for natural spawns)
            if (event.getSpawnType() == MobSpawnType.NATURAL) {
                if (shouldReduceGenericSpawn(entity)) {
                    event.setSpawnCancelled(true);
                    event.setCanceled(true);
                    return;
                }
            }

        } catch (Exception e) {
            System.err.println("[Enhanced Spawn Rules] Error processing spawn: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Check if entity should be denied due to generic restrictions
     * (twelve_kizuki, hashira, kamaboko should never spawn naturally outside designated areas)
     *
     * NOTE: This check should ONLY be applied to NATURAL spawns, NOT structure/spawner spawns.
     * Biome/dimension checks run BEFORE this, so designated areas can allow these entities.
     */
    private static boolean shouldDenyGenericSpawn(Mob entity, Level level, BlockPos pos) {
        ResourceLocation biomeId = EntityTagHelper.getCurrentBiome(entity);

        // Check if in a biome that allows special spawns
        boolean inMugenBiome = biomeId != null && biomeId.equals(ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "mugen_biome"));

        // Twelve kizuki should never spawn naturally EXCEPT in mugen_biome
        if (EntityTagHelper.isTwelveKizuki(entity)) {
            if (inMugenBiome) {
                return false; // Allow in mugen biome
            }
            return true; // Deny elsewhere
        }

        // Hashira should never spawn naturally (no exceptions for natural spawns)
        if (EntityTagHelper.isHashira(entity)) {
            return true;
        }

        // Kamaboko should never spawn naturally (no exceptions for natural spawns)
        if (EntityTagHelper.isKamaboko(entity)) {
            return true;
        }

        // Named slayers and retired hashira should only appear through their base mod
        // biome/structure rules, not through generic natural spawning.
        if ((EntityTagHelper.isNamedDemonSlayer(entity) || EntityTagHelper.isFormerHashira(entity)) &&
            !isAllowedNamedSlayerBiomeSpawn(entity, biomeId)) {
            return true;
        }

        return false;
    }

    private static boolean isAllowedNamedSlayerBiomeSpawn(Mob entity, ResourceLocation biomeId) {
        if (biomeId == null) {
            return false;
        }

        if (!biomeId.equals(ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "mt_sagiri"))) {
            return false;
        }

        ResourceLocation entityId = EntityTagHelper.getEntityTypeId(entity);
        if (entityId == null) {
            return false;
        }

        String path = entityId.getPath();
        return path.equals("sabito") || path.equals("makomo") || path.equals("urokodaki");
    }

    /**
     * Check if entity should be denied based on dimension rules
     */
    private static boolean shouldDenyByDimension(Mob entity, Level level, BlockPos pos) {
        ResourceLocation dimensionId = level.dimension().location();

        // Mt Fujikasane dimension rules
        if (dimensionId.equals(ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "mt_fujikasane"))) {
            return applyMtFujikasaneRules(entity, level, pos);
        }

        return false;
    }

    /**
     * Apply Mt Fujikasane dimension spawning rules
     */
    private static boolean applyMtFujikasaneRules(Mob entity, Level level, BlockPos pos) {
        ResourceLocation entityId = EntityType.getKey(entity.getType());
        if (entityId != null
            && entityId.getNamespace().equals("kimetsunoyaiba")
            && entityId.getPath().equals("swamp_demon")) {
            return true; // Swamp demons are never allowed in Mt Fujikasane.
        }

        boolean isDemon = EntityTagHelper.isDemon(entity);
        boolean isTwelveKizuki = EntityTagHelper.isTwelveKizuki(entity);
        boolean isKamaboko = EntityTagHelper.isKamaboko(entity);

        // Demons spawn only at night near the mountain center
        if (isDemon) {
            // No twelve_kizuki demons in Mt Fujikasane
            if (isTwelveKizuki) {
                return true; // Deny spawn
            }

            // Demons only spawn at night
            if (level.isDay()) {
                return true; // Deny spawn
            }

            // Check if within demon spawn radius
            BlockPos demonCenter = new BlockPos(
                EnhancedSpawnConfig.mtFujikasaneDemonCenterX,
                EnhancedSpawnConfig.mtFujikasaneDemonCenterY,
                EnhancedSpawnConfig.mtFujikasaneDemonCenterZ
            );

            double distance = Math.sqrt(pos.distSqr(demonCenter));
            if (distance > EnhancedSpawnConfig.mtFujikasaneDemonRadius) {
                return true; // Deny spawn - too far from demon center
            }

            // Allow spawn
            return false;
        }

        // Kamaboko spawn near specific location
        if (isKamaboko) {
            BlockPos kamabokoCenter = new BlockPos(
                EnhancedSpawnConfig.mtFujikasaneKamabokoX,
                EnhancedSpawnConfig.mtFujikasaneKamabokoY,
                EnhancedSpawnConfig.mtFujikasaneKamabokoZ
            );

            double distance = Math.sqrt(pos.distSqr(kamabokoCenter));
            if (distance > EnhancedSpawnConfig.mtFujikasaneKamabokoRadius) {
                return true; // Deny spawn - too far from kamaboko spawn area
            }

            // Check max 1 of each kamaboko type in the entire dimension
            if (MaxEntityTracker.isMaxReached(level, pos, entity.getType(), 500, 1, true)) {
                return true; // Deny spawn - already have one
            }

            // Allow spawn
            return false;
        }

        return false;
    }

    /**
     * Check if entity should be denied based on biome rules
     */
    private static boolean shouldDenyByBiome(Mob entity, Level level, BlockPos pos) {
        ResourceLocation biomeId = EntityTagHelper.getCurrentBiome(entity);
        if (biomeId == null) {
            return false;
        }

        // Mt Natagumo biome rules
        if (biomeId.equals(ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "mt_natagumo"))) {
            return applyMtNatagumoRules(entity, level, pos, biomeId);
        }

        // Mt Sagiri biome rules
        if (biomeId.equals(ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "mt_sagiri"))) {
            return applyMtSagiriRules(entity, level, pos, biomeId);
        }

        // Mt Yoko biome rules
        if (biomeId.equals(ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "mt_yoko"))) {
            return applyMtYokoRules(entity, level, pos);
        }

        // Mugen biome rules
        if (biomeId.equals(ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "mugen_biome"))) {
            return applyMugenBiomeRules(entity, level, pos);
        }

        return false;
    }

    /**
     * Apply Mt Natagumo biome rules (spider demon mountain)
     */
    private static boolean applyMtNatagumoRules(Mob entity, Level level, BlockPos pos, ResourceLocation biomeId) {
        ResourceLocation entityId = EntityTagHelper.getEntityTypeId(entity);
        if (entityId == null) {
            return false;
        }

        // Only allow specific demons
        boolean isSpiderDemon = entityId.getPath().equals("demon_3") ||
                               entityId.getPath().equals("spider_demon") ||
                               entityId.getPath().equals("dice_steak_senior_demon");

        // Rui family members (max 1 of each per 500 blocks)
        boolean isRuiFamily = entityId.getPath().equals("rui") ||
                             entityId.getPath().equals("rui_father") ||
                             entityId.getPath().equals("rui_mother") ||
                             entityId.getPath().equals("rui_brother") ||
                             entityId.getPath().equals("rui_sister");

        // If not an allowed demon type, deny
        if (!isSpiderDemon && !isRuiFamily && EntityTagHelper.isDemon(entity)) {
            return true; // Deny non-spider demons
        }

        // Check maximum for Rui family
        if (isRuiFamily) {
            if (MaxEntityTracker.isMaxReachedInBiome(level, pos, entity.getType(), biomeId, 1, true)) {
                return true; // Deny - already have one in this biome
            }
        }

        // Deny demon spawning during the day
        if (EntityTagHelper.isDemon(entity) && level.isDay()) {
            return true;
        }

        return false;
    }

    /**
     * Apply Mt Sagiri biome rules
     */
    private static boolean applyMtSagiriRules(Mob entity, Level level, BlockPos pos, ResourceLocation biomeId) {
        ResourceLocation entityId = EntityTagHelper.getEntityTypeId(entity);
        if (entityId == null) {
            return false;
        }

        // Only allow specific demon slayers
        boolean isSabito = entityId.getPath().equals("sabito");
        boolean isMakomo = entityId.getPath().equals("makomo");
        boolean isUrokodaki = entityId.getPath().equals("urokodaki");

        if (!isSabito && !isMakomo && !isUrokodaki) {
            return false; // Not a Mt Sagiri-specific entity
        }

        // Max 1 of each per 500 blocks
        if (MaxEntityTracker.isMaxReachedInBiome(level, pos, entity.getType(), biomeId, 1, true)) {
            return true; // Deny - already have one
        }

        return false;
    }

    /**
     * Apply Mt Yoko biome rules (regular demons only)
     */
    private static boolean applyMtYokoRules(Mob entity, Level level, BlockPos pos) {
        // Allow regular demons, deny twelve_kizuki
        if (EntityTagHelper.isTwelveKizuki(entity)) {
            return true; // Deny twelve_kizuki
        }

        return false;
    }

    /**
     * Apply Mugen biome rules (twelve kizuki allowed, max 1 per type per 500 blocks)
     */
    private static boolean applyMugenBiomeRules(Mob entity, Level level, BlockPos pos) {
        // Allow twelve kizuki, but max 1 of each type per 500 blocks
        if (EntityTagHelper.isTwelveKizuki(entity)) {
            if (MaxEntityTracker.isMaxReached(level, pos, entity.getType(), 500, 1, true)) {
                return true; // Deny - already have one of this type nearby
            }
        }

        return false;
    }

    /**
     * Check if entity should be denied due to maximum entity limits
     */
    private static boolean shouldDenyByMaxLimit(Mob entity, Level level, BlockPos pos) {
        // This is handled by biome-specific rules for now
        return false;
    }

    /**
     * Check if entity should be denied due to day/night restrictions
     */
    private static boolean shouldDenyByDayNight(Mob entity, Level level, BlockPos pos) {
        // Generic demons shouldn't spawn in direct sunlight during the day
        if (EntityTagHelper.isDemon(entity) && level.isDay()) {
            // Mugen biome allows demons regardless of day/night
            ResourceLocation biomeId = EntityTagHelper.getCurrentBiome(entity);
            if (biomeId != null && biomeId.equals(ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "mugen_biome"))) {
                return false;
            }

            // Allow in shade: under cover or low sky light
            if (!level.canSeeSky(pos)) {
                return false;
            }
            int sky = level.getBrightness(LightLayer.SKY, pos);
            if (sky < 12) {
                return false;
            }

            // Otherwise, deny daytime demon spawns
            return true;
        }

        return false;
    }

    /**
     * Check if entity should be randomly denied due to generic spawn rate reduction
     */
    private static boolean shouldReduceGenericSpawn(Mob entity) {
        // Reduce generic demon spawning
        if (EntityTagHelper.isDemon(entity) && !EntityTagHelper.isTwelveKizuki(entity)) {
            double spawnChance = EnhancedSpawnConfig.genericDemonSpawnRate;
            if (RANDOM.nextDouble() >= spawnChance) {
                return true; // Deny spawn
            }
        }

        // Reduce demon slayer spawning (not hashira or kamaboko)
        if (EntityTagHelper.isDemonSlayer(entity) &&
            !EntityTagHelper.isHashira(entity) &&
            !EntityTagHelper.isKamaboko(entity) &&
            !EntityTagHelper.isNamedDemonSlayer(entity) &&
            !EntityTagHelper.isFormerHashira(entity)) {
            double spawnChance = EnhancedSpawnConfig.genericDemonSlayerSpawnRate;
            if (RANDOM.nextDouble() >= spawnChance) {
                return true; // Deny spawn
            }
        }

        return false;
    }
}
