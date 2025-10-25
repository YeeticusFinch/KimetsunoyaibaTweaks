package com.lerdorf.kimetsunoyaibamultiplayer.structures;

import com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedSpawnConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.MaxEntityTracker;
import com.lerdorf.kimetsunoyaibamultiplayer.util.EntityTagHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

/**
 * Handles structure-specific spawning rules.
 *
 * Applies spawning restrictions and requirements based on the structure
 * the entity is spawning in. This includes:
 * - Maximum entity limits per structure
 * - Structure-specific entity types
 * - Preventing certain entity types in certain structures
 *
 * Structure detection is performed at spawn time using Minecraft's structure system.
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class StructureSpawnHandler {

    /**
     * Check and apply structure-specific spawning rules
     */
    @SubscribeEvent
    public static void onMobSpawn(MobSpawnEvent.FinalizeSpawn event) {
        // Check if enhanced spawning rules are enabled
        if (!EnhancedSpawnConfig.enhancedSpawningRules) {
            return;
        }

        // Only affect natural spawns
        if (event.getSpawnType() != MobSpawnType.NATURAL &&
            event.getSpawnType() != MobSpawnType.STRUCTURE) {
            return;
        }

        Mob entity = event.getEntity();
        LevelAccessor levelAccessor = event.getLevel();

        if (!(levelAccessor instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos pos = BlockPos.containing(event.getX(), event.getY(), event.getZ());

        try {
            // Get the structure at this location
            StructureManager structureManager = serverLevel.structureManager();

            // Check all structures at this position
            for (Holder<Structure> structureHolder : serverLevel.registryAccess().registryOrThrow(Registries.STRUCTURE).holders().toList()) {
                Structure structure = structureHolder.value();
                StructureStart structureStart = structureManager.getStructureAt(pos, structure);

                if (structureStart == null || !structureStart.isValid()) {
                    continue;
                }

                // Get structure ID
                ResourceLocation structureId = structureHolder.unwrapKey().map(key -> key.location()).orElse(null);
                if (structureId == null) {
                    continue;
                }

                // Apply structure-specific rules
                if (shouldDenySpawnInStructure(entity, serverLevel, pos, structureId)) {
                    event.setSpawnCancelled(true);
                    event.setCanceled(true);
                    return;
                }
            }

        } catch (Exception e) {
            System.err.println("[Structure Spawn Handler] Error processing structure spawn: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Check if entity should be denied spawning in a specific structure
     */
    private static boolean shouldDenySpawnInStructure(Mob entity, Level level, BlockPos pos, ResourceLocation structureId) {
        String structurePath = structureId.getPath();

        // Graveyard: Max 1 ubuyashiki, max 1 himejima
        if (structurePath.equals("graveyard")) {
            return applyGraveyardRules(entity, level, pos);
        }

        // House Ubuyashiki: Prevent demons from spawning
        if (structurePath.equals("house_ubuyashiki")) {
            return applyHouseUbuyashikiRules(entity);
        }

        // Mugen Train: Complex spawn rules with enmu, rengoku, akaza
        if (structurePath.equals("mugen_train")) {
            return applyMugenTrainRules(entity, level, pos);
        }

        // Temple Doma: Max 1 doma
        if (structurePath.equals("temple_doma")) {
            return applyTempleDomaRules(entity, level, pos);
        }

        // Village Yukak (Entertainment District): Max 1 daki, no daytime spawning for daki
        if (structurePath.equals("village_yukak")) {
            return applyVillageYukakRules(entity, level, pos);
        }

        // House Rengoku: Max 1 rengoku
        if (structurePath.equals("house_rengoku")) {
            return applyHouseRengokuRules(entity, level, pos);
        }

        return false;
    }

    /**
     * Apply graveyard structure rules
     */
    private static boolean applyGraveyardRules(Mob entity, Level level, BlockPos pos) {
        ResourceLocation entityId = EntityTagHelper.getEntityTypeId(entity);
        if (entityId == null) {
            return false;
        }

        // Max 1 ubuyashiki
        if (entityId.getPath().equals("ubuyashiki")) {
            if (MaxEntityTracker.isMaxReached(level, pos, entity.getType(), 100, 1, true)) {
                return true; // Deny - already have one
            }
        }

        // Max 1 himejima
        if (entityId.getPath().equals("himejima")) {
            if (MaxEntityTracker.isMaxReached(level, pos, entity.getType(), 100, 1, true)) {
                return true; // Deny - already have one
            }
        }

        return false;
    }

    /**
     * Apply house ubuyashiki rules - prevent demons from spawning
     */
    private static boolean applyHouseUbuyashikiRules(Mob entity) {
        // Prevent all demons from spawning
        if (EntityTagHelper.isDemon(entity)) {
            return true; // Deny demons
        }

        return false;
    }

    /**
     * Apply mugen train rules
     * - Max 1 enmu
     * - Max 1 rengoku
     * - Max 1 akaza (only if no enmu present)
     */
    private static boolean applyMugenTrainRules(Mob entity, Level level, BlockPos pos) {
        ResourceLocation entityId = EntityTagHelper.getEntityTypeId(entity);
        if (entityId == null) {
            return false;
        }

        // Max 1 enmu
        if (entityId.getPath().equals("enmu")) {
            if (MaxEntityTracker.isMaxReached(level, pos, entity.getType(), 200, 1, true)) {
                return true; // Deny - already have one
            }
        }

        // Max 1 rengoku
        if (entityId.getPath().equals("rengoku")) {
            if (MaxEntityTracker.isMaxReached(level, pos, entity.getType(), 200, 1, true)) {
                return true; // Deny - already have one
            }
        }

        // Akaza only if no enmu
        if (entityId.getPath().equals("akaza")) {
            // Check if enmu exists
            EntityType<?> enmuType = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                .get(ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "enmu"));

            if (enmuType != null && MaxEntityTracker.entityTypeExists(level, pos, enmuType, 200)) {
                return true; // Deny - enmu is present
            }

            // Max 1 akaza
            if (MaxEntityTracker.isMaxReached(level, pos, entity.getType(), 200, 1, true)) {
                return true; // Deny - already have one
            }
        }

        return false;
    }

    /**
     * Apply temple doma rules - max 1 doma
     */
    private static boolean applyTempleDomaRules(Mob entity, Level level, BlockPos pos) {
        ResourceLocation entityId = EntityTagHelper.getEntityTypeId(entity);
        if (entityId == null) {
            return false;
        }

        // Max 1 doma
        if (entityId.getPath().equals("doma")) {
            if (MaxEntityTracker.isMaxReached(level, pos, entity.getType(), 150, 1, true)) {
                return true; // Deny - already have one
            }
        }

        return false;
    }

    /**
     * Apply village yukak (entertainment district) rules
     * - Max 1 daki
     * - No daki spawning during daytime
     * - No daki if gyutaro is present
     */
    private static boolean applyVillageYukakRules(Mob entity, Level level, BlockPos pos) {
        ResourceLocation entityId = EntityTagHelper.getEntityTypeId(entity);
        if (entityId == null) {
            return false;
        }

        // Rules for daki
        if (entityId.getPath().equals("daki")) {
            // No daytime spawning
            if (level.isDay()) {
                return true; // Deny daytime spawn
            }

            // Check if gyutaro is present
            EntityType<?> gyutaroType = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                .get(ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "gyutaro"));

            if (gyutaroType != null && MaxEntityTracker.entityTypeExists(level, pos, gyutaroType, 150)) {
                return true; // Deny - gyutaro is present
            }

            // Check kimetsu mod gyutaro too
            EntityType<?> kimetsuGyutaroType = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                .get(ResourceLocation.fromNamespaceAndPath("kimetsu", "gyutaro"));

            if (kimetsuGyutaroType != null && MaxEntityTracker.entityTypeExists(level, pos, kimetsuGyutaroType, 150)) {
                return true; // Deny - kimetsu gyutaro is present
            }

            // Max 1 daki
            if (MaxEntityTracker.isMaxReached(level, pos, entity.getType(), 150, 1, true)) {
                return true; // Deny - already have one
            }
        }

        return false;
    }

    /**
     * Apply house rengoku rules - max 1 rengoku
     */
    private static boolean applyHouseRengokuRules(Mob entity, Level level, BlockPos pos) {
        ResourceLocation entityId = EntityTagHelper.getEntityTypeId(entity);
        if (entityId == null) {
            return false;
        }

        // Max 1 rengoku
        if (entityId.getPath().equals("rengoku")) {
            if (MaxEntityTracker.isMaxReached(level, pos, entity.getType(), 100, 1, true)) {
                return true; // Deny - already have one
            }
        }

        return false;
    }
}
