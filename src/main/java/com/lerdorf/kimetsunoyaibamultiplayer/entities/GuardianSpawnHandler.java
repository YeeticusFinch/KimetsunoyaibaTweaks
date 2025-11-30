package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedSpawnConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.util.EntityTagHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

/**
 * Handles protective spawning mechanics (Iron Golem style).
 *
 * When civilians are threatened by demons, demon slayers may spawn to protect them:
 * - Hashira spawn when civilians see/are attacked by twelve kizuki (rare)
 * - Kamaboko members spawn when civilians see/are attacked by twelve kizuki (rare)
 * - Regular demon slayers spawn in groups when civilians see/are attacked by demons (more common)
 *
 * This creates dynamic protection mechanics similar to how iron golems protect villagers.
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GuardianSpawnHandler {

    private static final Random RANDOM = new Random();

    // Track recent spawn attempts to prevent spam
    private static final Map<UUID, Long> RECENT_SPAWN_ATTEMPTS = new HashMap<>();
    private static final long SPAWN_COOLDOWN_MS = 30000; // 30 seconds

    // Lists of entities that can spawn as guardians
    private static final List<ResourceLocation> HASHIRA_ENTITIES = Arrays.asList(
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "tomioka"),
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "rengoku"),
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "muichirou"),
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "shinazugawa"),
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "himejima"),
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "kocho"),
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "kanroji"),
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "iguro"),
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "uzui"),
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "dice_steak_senior_super")
    );

    private static final List<ResourceLocation> KAMABOKO_ENTITIES = Arrays.asList(
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "tanjiro"),
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "zennitsu"),
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "inosuke"),
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "genya"),
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "kanawo")
    );

    private static final List<ResourceLocation> DEMON_SLAYER_ENTITIES = Arrays.asList(
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "dice_steak_senior"),
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "murata"),
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "sabito"),
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "makomo")
    );

    /**
     * Handle civilian being attacked by demons
     */
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        // Check if protective spawning is enabled
        if (!EnhancedSpawnConfig.enableProtectiveSpawning) {
            return;
        }

        // Check if enhanced spawning rules are enabled
        if (!EnhancedSpawnConfig.enhancedSpawningRules) {
            return;
        }

        LivingEntity victim = event.getEntity();
        Entity sourceEntity = event.getSource().getEntity();

        // Check if victim is a civilian
        if (!EntityTagHelper.isCivilian(victim)) {
            return;
        }

        // Check if attacker is a demon or twelve kizuki
        if (!(sourceEntity instanceof LivingEntity attacker)) {
            return;
        }

        boolean isTwelveKizuki = EntityTagHelper.isTwelveKizuki(attacker);
        boolean isDemon = EntityTagHelper.isDemon(attacker);

        if (!isDemon && !isTwelveKizuki) {
            return;
        }

        // Only run on server side
        if (victim.level().isClientSide()) {
            return;
        }

        // Check cooldown
        if (!checkCooldown(victim)) {
            return;
        }

        ServerLevel level = (ServerLevel) victim.level();
        BlockPos spawnPos = victim.blockPosition();

        try {
            // Twelve kizuki attacked - rare chance for hashira or kamaboko
            if (isTwelveKizuki) {
                // Chance to spawn hashira
                if (RANDOM.nextDouble() < EnhancedSpawnConfig.hashiraSpawnChance) {
                    spawnRandomHashira(level, spawnPos);
                }
                // Chance to spawn kamaboko
                else if (RANDOM.nextDouble() < EnhancedSpawnConfig.kamabokoSpawnChance) {
                    spawnRandomKamaboko(level, spawnPos);
                }
            }
            // Regular demon attacked - chance to spawn demon slayers
            else if (isDemon) {
                if (RANDOM.nextDouble() < EnhancedSpawnConfig.demonSlayerSpawnChance) {
                    spawnDemonSlayerGroup(level, spawnPos);
                }
            }

        } catch (Exception e) {
            System.err.println("[Guardian Spawn Handler] Error spawning guardian: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Check spawn cooldown for an entity
     */
    private static boolean checkCooldown(LivingEntity entity) {
        UUID uuid = entity.getUUID();
        long currentTime = System.currentTimeMillis();

        // Check if entity has recent spawn attempt
        if (RECENT_SPAWN_ATTEMPTS.containsKey(uuid)) {
            long lastAttempt = RECENT_SPAWN_ATTEMPTS.get(uuid);
            if (currentTime - lastAttempt < SPAWN_COOLDOWN_MS) {
                return false; // Still on cooldown
            }
        }

        // Update cooldown
        RECENT_SPAWN_ATTEMPTS.put(uuid, currentTime);

        // Clean up old entries
        cleanupCooldowns(currentTime);

        return true;
    }

    /**
     * Clean up old cooldown entries
     */
    private static void cleanupCooldowns(long currentTime) {
        RECENT_SPAWN_ATTEMPTS.entrySet().removeIf(entry ->
            currentTime - entry.getValue() > SPAWN_COOLDOWN_MS * 2
        );
    }

    /**
     * Spawn a random hashira near the position
     */
    private static void spawnRandomHashira(ServerLevel level, BlockPos pos) {
        ResourceLocation hashiraId = HASHIRA_ENTITIES.get(RANDOM.nextInt(HASHIRA_ENTITIES.size()));
        EntityType<?> hashiraType = BuiltInRegistries.ENTITY_TYPE.get(hashiraId);

        if (hashiraType == null) {
            System.err.println("[Guardian Spawn Handler] Could not find hashira type: " + hashiraId);
            return;
        }

        Entity hashira = hashiraType.create(level);
        if (hashira == null) {
            return;
        }

        // Find a valid spawn position
        BlockPos spawnPos = findValidSpawnPosition(level, pos, 5);
        if (spawnPos == null) {
            return;
        }

        hashira.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0, 0);
        level.addFreshEntity(hashira);

        if (com.lerdorf.kimetsunoyaibamultiplayer.Config.logInfo) {
            Log.debug("[Guardian Spawn Handler] Spawned hashira to protect civilian: " + hashiraId);
        }
    }

    /**
     * Spawn a random kamaboko member near the position
     */
    private static void spawnRandomKamaboko(ServerLevel level, BlockPos pos) {
        ResourceLocation kamabokoId = KAMABOKO_ENTITIES.get(RANDOM.nextInt(KAMABOKO_ENTITIES.size()));
        EntityType<?> kamabokoType = BuiltInRegistries.ENTITY_TYPE.get(kamabokoId);

        if (kamabokoType == null) {
            System.err.println("[Guardian Spawn Handler] Could not find kamaboko type: " + kamabokoId);
            return;
        }

        Entity kamaboko = kamabokoType.create(level);
        if (kamaboko == null) {
            return;
        }

        // Find a valid spawn position
        BlockPos spawnPos = findValidSpawnPosition(level, pos, 5);
        if (spawnPos == null) {
            return;
        }

        kamaboko.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0, 0);
        level.addFreshEntity(kamaboko);

        if (com.lerdorf.kimetsunoyaibamultiplayer.Config.logInfo) {
            Log.debug("[Guardian Spawn Handler] Spawned kamaboko to protect civilian: " + kamabokoId);
        }
    }

    /**
     * Spawn a group of demon slayers near the position
     */
    private static void spawnDemonSlayerGroup(ServerLevel level, BlockPos pos) {
        int groupSize = EnhancedSpawnConfig.demonSlayerGroupSizeMin +
            RANDOM.nextInt(EnhancedSpawnConfig.demonSlayerGroupSizeMax - EnhancedSpawnConfig.demonSlayerGroupSizeMin + 1);

        int spawned = 0;
        for (int i = 0; i < groupSize; i++) {
            ResourceLocation slayerId = DEMON_SLAYER_ENTITIES.get(RANDOM.nextInt(DEMON_SLAYER_ENTITIES.size()));
            EntityType<?> slayerType = BuiltInRegistries.ENTITY_TYPE.get(slayerId);

            if (slayerType == null) {
                continue;
            }

            Entity slayer = slayerType.create(level);
            if (slayer == null) {
                continue;
            }

            // Find a valid spawn position
            BlockPos spawnPos = findValidSpawnPosition(level, pos, 8);
            if (spawnPos == null) {
                continue;
            }

            slayer.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0, 0);
            level.addFreshEntity(slayer);
            spawned++;
        }

        if (com.lerdorf.kimetsunoyaibamultiplayer.Config.logInfo && spawned > 0) {
            Log.debug("[Guardian Spawn Handler] Spawned " + spawned + " demon slayers to protect civilian");
        }
    }

    /**
     * Find a valid spawn position near the target position
     */
    private static BlockPos findValidSpawnPosition(ServerLevel level, BlockPos center, int radius) {
        // Try several random positions
        for (int attempt = 0; attempt < 10; attempt++) {
            int xOffset = RANDOM.nextInt(radius * 2 + 1) - radius;
            int zOffset = RANDOM.nextInt(radius * 2 + 1) - radius;
            BlockPos testPos = center.offset(xOffset, 0, zOffset);

            // Find ground level
            for (int y = -5; y <= 5; y++) {
                BlockPos checkPos = testPos.offset(0, y, 0);
                if (level.getBlockState(checkPos.below()).isSolid() &&
                    level.getBlockState(checkPos).isAir() &&
                    level.getBlockState(checkPos.above()).isAir()) {
                    return checkPos;
                }
            }
        }

        // Fallback to original position
        return center;
    }
}
