package com.lerdorf.kimetsunoyaibamultiplayer.raids;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.fml.ModList;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for spawning kimetsu mod entities.
 *
 * Some kimetsu entities require spawn eggs to spawn correctly due to complex
 * initialization logic. This class handles both direct spawning and spawn egg-based
 * spawning with automatic fallback.
 */
public class KimetsuEntitySpawner {

    private static Boolean kimetsuModLoaded = null;

    // Map of entity IDs to their spawn egg IDs
    private static final Map<String, String> ENTITY_TO_SPAWN_EGG = new HashMap<>();

    static {
        // Demons
        ENTITY_TO_SPAWN_EGG.put("nakime", "nakime_spawn_egg");
        ENTITY_TO_SPAWN_EGG.put("daki", "daki_spawn_egg");
        ENTITY_TO_SPAWN_EGG.put("gyutaro", "gyutaro_spawn_egg");
        ENTITY_TO_SPAWN_EGG.put("kaigaku", "kaigaku_spawn_egg");
        ENTITY_TO_SPAWN_EGG.put("gyokko", "gyokokko_spawn_egg");  // Note: typo in original name
        ENTITY_TO_SPAWN_EGG.put("zohakuten", "zohakuten_spawn_egg");
        ENTITY_TO_SPAWN_EGG.put("nakime_real", "nakime_real_spawn_egg");
        ENTITY_TO_SPAWN_EGG.put("akaza", "akaza_spawn_egg");
        ENTITY_TO_SPAWN_EGG.put("doma", "doma_spawn_egg");
        ENTITY_TO_SPAWN_EGG.put("kokushibo", "kokushibo_spawn_egg");
        ENTITY_TO_SPAWN_EGG.put("yoriichi", "demon_yoriichi_spawn_egg");  // Demon version
        ENTITY_TO_SPAWN_EGG.put("kocho", "demon_kocho_spawn_egg");  // Demon version
        ENTITY_TO_SPAWN_EGG.put("tomioka", "demon_tomioka_spawn_egg");  // Demon version
        ENTITY_TO_SPAWN_EGG.put("demon_nakime", "nakime_spawn_egg");  // Alias

        // Demon Slayers
        ENTITY_TO_SPAWN_EGG.put("himejima", "himejima_spawn_egg");
        ENTITY_TO_SPAWN_EGG.put("slayer_kocho", "slayer_kocho_spawn_egg");
        ENTITY_TO_SPAWN_EGG.put("slayer_kocho_kakusei", "slayer_kocho_kakusei_spawn_egg");
    }

    /**
     * Check if kimetsu mod is loaded.
     */
    public static boolean isKimetsuModLoaded() {
        if (kimetsuModLoaded == null) {
            kimetsuModLoaded = ModList.get().isLoaded("kimetsu");
        }
        return kimetsuModLoaded;
    }

    /**
     * Spawn a kimetsu entity at the given position.
     *
     * Kimetsu entities are ONLY spawned using spawn eggs, as this is the only
     * reliable method that works consistently. Direct spawning is not attempted.
     *
     * @param level Server level
     * @param pos Position to spawn at
     * @param entityId Entity resource location (e.g., "kimetsu:akaza")
     * @return The spawned mob, or null if spawning failed
     */
    @Nullable
    public static Mob spawnKimetsuEntity(ServerLevel level, BlockPos pos, ResourceLocation entityId) {
        if (!isKimetsuModLoaded()) {
            Log.debug("[Kimetsu Spawner] Kimetsu mod not loaded, cannot spawn: " + entityId);
            return null;
        }

        if (!entityId.getNamespace().equals("kimetsu")) {
            Log.debug("[Kimetsu Spawner] Not a kimetsu entity: " + entityId);
            return null;
        }

        // ONLY use spawn egg method for kimetsu entities (direct spawn is unreliable)
        return spawnFromSpawnEgg(level, pos, entityId);
    }

    /**
     * Spawn entity using spawn egg (the ONLY reliable method for kimetsu entities).
     */
    @Nullable
    private static Mob spawnFromSpawnEgg(ServerLevel level, BlockPos pos, ResourceLocation entityId) {
        String entityPath = entityId.getPath();
        String spawnEggPath = ENTITY_TO_SPAWN_EGG.get(entityPath);

        if (spawnEggPath == null) {
            Log.debug("[Kimetsu Spawner] No spawn egg mapping for: " + entityPath);
            return null;
        }

        ResourceLocation spawnEggId = ResourceLocation.fromNamespaceAndPath("kimetsu", spawnEggPath);

        try {
            Item spawnEggItem = BuiltInRegistries.ITEM.get(spawnEggId);
            if (spawnEggItem == null) {
                Log.debug("[Kimetsu Spawner] Spawn egg not found: " + spawnEggId);
                return null;
            }

            if (!(spawnEggItem instanceof ForgeSpawnEggItem spawnEgg)) {
                Log.debug("[Kimetsu Spawner] Item is not a spawn egg: " + spawnEggId);
                return null;
            }

            // Get the entity type from the spawn egg
            EntityType<?> entityType = spawnEgg.getType(new ItemStack(spawnEggItem).getTag());
            if (entityType == null) {
                Log.debug("[Kimetsu Spawner] Spawn egg has no entity type: " + spawnEggId);
                return null;
            }

            // Create entity using spawn egg's entity type
            Entity entity = entityType.create(level);
            if (!(entity instanceof Mob mob)) {
                return null;
            }

            // Position and configure the mob
            mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                       level.random.nextFloat() * 360F, 0);
            mob.setPersistenceRequired();

            // Call finalizeSpawn to run initialization logic
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos),
                            MobSpawnType.MOB_SUMMONED, null, null);

            if (level.addFreshEntity(mob)) {
                Log.debug("[Kimetsu Spawner] Successfully spawned " + entityId + " from spawn egg");
                return mob;
            }

            Log.debug("[Kimetsu Spawner] Failed to add entity to level: " + entityId);
            return null;
        } catch (Exception e) {
            System.err.println("[Kimetsu Spawner] Error spawning " + entityId + " from spawn egg: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Check if an entity has a spawn egg mapping.
     */
    public static boolean hasSpawnEggMapping(String entityPath) {
        return ENTITY_TO_SPAWN_EGG.containsKey(entityPath);
    }

    /**
     * Get spawn egg ID for an entity path.
     */
    @Nullable
    public static ResourceLocation getSpawnEggId(String entityPath) {
        String spawnEggPath = ENTITY_TO_SPAWN_EGG.get(entityPath);
        if (spawnEggPath == null) {
            return null;
        }
        return ResourceLocation.fromNamespaceAndPath("kimetsu", spawnEggPath);
    }
}
