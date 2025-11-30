package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedSpawnConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.raids.RaidRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.util.EntityTagHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Spawns demon slayers to protect civilians when attacked or targeted by demons.
 * Controlled by EnhancedSpawnConfig (enable_protective_spawning + spawn chances).
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CivilianProtectionHandler {

    private static final RandomSource RNG = RandomSource.create();

    @SubscribeEvent
    public static void onCivilianAttacked(LivingAttackEvent event) {
        if (!EnhancedSpawnConfig.enhancedSpawningRules || !EnhancedSpawnConfig.enableProtectiveSpawning) return;

        LivingEntity target = event.getEntity();
        if (!EntityTagHelper.isCivilian(target)) return;

        LivingEntity attacker = null;
        if (event.getSource().getEntity() instanceof LivingEntity le) attacker = le;
        if (attacker == null) return;

        handleProtection(target, attacker);
    }

    @SubscribeEvent
    public static void onCivilianTargeted(LivingChangeTargetEvent event) {
        if (!EnhancedSpawnConfig.enhancedSpawningRules || !EnhancedSpawnConfig.enableProtectiveSpawning) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;

        LivingEntity attacker = (LivingEntity) event.getEntity();
        LivingEntity target = event.getNewTarget();
        if (target == null) return;

        if (EntityTagHelper.isCivilian(target)) {
            handleProtection(target, attacker);
        }
    }

    private static void handleProtection(LivingEntity civilian, LivingEntity threat) {
        Level lvl = civilian.level();
        if (!(lvl instanceof ServerLevel level)) return;

        BlockPos near = civilian.blockPosition();

        // Check for raids within 200 blocks - raids take precedence over civilian protection
        if (RaidRegistry.hasRaidNearby(level, near, 200)) {
            return; // Don't spawn protectors during raids
        }

        // Count nearby demon slayers within 50 blocks
        int nearbySlayerCount = countNearbyDemonSlayers(level, near, 50);

        // If 10+ demon slayers nearby, don't spawn any more
        if (nearbySlayerCount >= 10) {
            return;
        }

        // Calculate spawn chance multiplier based on nearby slayers
        double spawnChanceMultiplier = 1.0;
        if (nearbySlayerCount >= 5) {
            // 5-9 demon slayers: significantly reduce spawn chance (20% of normal)
            spawnChanceMultiplier = 0.2;
        }

        boolean isDemon = EntityTagHelper.isDemon(threat);
        boolean isKizuki = EntityTagHelper.isTwelveKizuki(threat);

        if (isKizuki) {
            // Chance to spawn a hashira and/or a kamaboko (with multiplier)
            if (RNG.nextDouble() < EnhancedSpawnConfig.hashiraSpawnChance * spawnChanceMultiplier) {
                spawnById(level, near, ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", pickRandomHashira()));
            }
            if (RNG.nextDouble() < EnhancedSpawnConfig.kamabokoSpawnChance * spawnChanceMultiplier) {
                spawnById(level, near, ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", pickRandomKamaboko()));
            }
        } else if (isDemon) {
            // Spawn a small group of generic demon slayers (with multiplier)
            if (RNG.nextDouble() < EnhancedSpawnConfig.demonSlayerSpawnChance * spawnChanceMultiplier) {
                int count = clamp(EnhancedSpawnConfig.demonSlayerGroupSizeMin, EnhancedSpawnConfig.demonSlayerGroupSizeMax, 2);
                for (int i = 0; i < count; i++) {
                    spawnById(level, near.offset(RNG.nextInt(7) - 3, 0, RNG.nextInt(7) - 3),
                        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "demon_slayer"));
                }
            }
        }
    }

    /**
     * Count demon slayers (including hashira and kamaboko) within a radius.
     */
    private static int countNearbyDemonSlayers(ServerLevel level, BlockPos center, int radius) {
        List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(
            LivingEntity.class,
            new net.minecraft.world.phys.AABB(center).inflate(radius),
            entity -> entity != null && (
                EntityTagHelper.isDemonSlayer(entity) ||
                EntityTagHelper.isHashira(entity) ||
                EntityTagHelper.isKamaboko(entity)
            )
        );
        return nearbyEntities.size();
    }

    private static void spawnById(ServerLevel level, BlockPos around, ResourceLocation id) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
        if (type == null) return;
        Mob mob = (Mob) type.create(level);
        if (mob == null) return;

        BlockPos p = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, around);
        mob.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, level.random.nextFloat() * 360F, 0);
        level.addFreshEntity(mob);
    }

    private static int clamp(int min, int max, int fallback) {
        if (min > max) return fallback;
        int range = max - min + 1;
        return min + (range <= 0 ? 0 : RNG.nextInt(range));
    }

    private static String pickRandomHashira() {
        String[] opts = {"kocho", "kanroji", "kanae", "shinazugawa", "rengoku", "iguro", "uzui", "tomioka", "muichirou", "himejima"};
        return opts[RNG.nextInt(opts.length)];
    }

    private static String pickRandomKamaboko() {
        String[] opts = {"genya", "inosuke", "tanjiro", "zennitsu", "kanawo"};
        return opts[RNG.nextInt(opts.length)];
    }
}

