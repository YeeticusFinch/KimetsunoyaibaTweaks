package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.config.SpawnRateConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

/**
 * Handles spawn priority for kimetsunoyaiba mobs.
 *
 * Priority values (0-100+):
 * - 0 = Never spawns (always cancelled)
 * - 50 = 50% chance to spawn
 * - 100 = Always spawns (default)
 * - >100 = Treated as 100 (always spawns)
 *
 * Only affects NATURAL spawns. Structure, command, and other special spawns are unaffected.
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SpawnRateHandler {

    private static final Random RANDOM = new Random();

    // Default priority when spawning should always succeed
    private static final double DEFAULT_PRIORITY = 100.0;

    // Only affect natural spawns from this namespace
    private static final String KIMETSUNOYAIBA_NAMESPACE = "kimetsunoyaiba";

    /**
     * Intercepts mob spawn checks BEFORE entity creation.
     * This is more efficient and reliable than FinalizeSpawn.
     */
    @SubscribeEvent
    public static void onSpawnCheck(MobSpawnEvent.SpawnPlacementCheck event) {
        EntityType<?> entityType = event.getEntityType();

        if (entityType == null) {
            return;
        }

        // Get the entity's resource location (e.g., "kimetsunoyaiba:tanjiro")
        ResourceLocation entityName = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES
                .getKey(entityType);

        if (entityName == null || !KIMETSUNOYAIBA_NAMESPACE.equals(entityName.getNamespace())) {
            // Not a kimetsunoyaiba mob, ignore
            return;
        }

        // Only affect natural spawns - preserve structure/biome/command spawns
        MobSpawnType spawnType = event.getSpawnType();
        if (spawnType != MobSpawnType.NATURAL) {
            // Don't affect non-natural spawns (structures, commands, spawn eggs, etc.)
            return;
        }

        String entityString = entityName.toString();

        // Get the configured spawn priority for this entity (0-100+)
        double spawnPriority = SpawnRateConfig.getSpawnRate(entityString);

        // If priority is default (100), use default behavior (no modification)
        if (spawnPriority == DEFAULT_PRIORITY) {
            return;
        }

        // Priority 0 = never spawn
        if (spawnPriority <= 0.0) {
            event.setResult(Event.Result.DENY);
            return;
        }

        // Priority > 100 is treated as 100 (always spawn)
        if (spawnPriority >= DEFAULT_PRIORITY) {
            event.setResult(Event.Result.ALLOW);
            return;
        }

        // Priority 1-99: Use as percentage chance
        // e.g., priority 75 = 75% chance to allow spawn
        double spawnChance = spawnPriority / DEFAULT_PRIORITY;

        if (RANDOM.nextDouble() < spawnChance) {
            // Spawn allowed
            event.setResult(Event.Result.ALLOW);
        } else {
            // Spawn denied
            event.setResult(Event.Result.DENY);
        }
    }
}