package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.api.DemonRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedSpawnConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.config.SpawnRateConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.util.EntityTagHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Set;

/** Replaces selected base demon natural spawns with demons from this mod. */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DemonSpawnReplacementHandler {
    private static final ResourceLocation BASE_DEMON = id("kimetsunoyaiba", "demon");
    private static final ResourceLocation BASE_DEMON_2 = id("kimetsunoyaiba", "demon_2");
    private static final ResourceLocation BASE_DEMON_3 = id("kimetsunoyaiba", "demon_3");
    private static final ResourceLocation BASE_DEMON_5 = id("kimetsunoyaiba", "demon_5");
    private static final ResourceLocation BASE_DEMON_8 = id("kimetsunoyaiba", "demon_8");
    private static final ResourceLocation BASE_DEMON_9 = id("kimetsunoyaiba", "demon_9");
    private static final ResourceLocation BASE_DEMON_10 = id("kimetsunoyaiba", "demon_10");

    private static final Set<ResourceLocation> EASY_DEMONS = Set.of(BASE_DEMON, BASE_DEMON_2, BASE_DEMON_3);
    private static final Set<ResourceLocation> SWAMP_DEMONS = Set.of(BASE_DEMON_5, BASE_DEMON_9);
    private static final Set<ResourceLocation> HARD_DEMONS = Set.of(BASE_DEMON_8, BASE_DEMON_10);

    private static final String TWEAKS_NAMESPACE = KimetsunoyaibaMultiplayer.MODID;
    private static final ThreadLocal<Boolean> REPLACING = ThreadLocal.withInitial(() -> false);

    private DemonSpawnReplacementHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (REPLACING.get() || event.getSpawnType() != MobSpawnType.NATURAL
                || !(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        Mob entity = event.getEntity();
        if (entity == null) {
            return;
        }

        if (EnhancedSpawnConfig.disableTweaksDemons && isTweaksDemonType(entity.getType())) {
            cancelSpawn(event);
            return;
        }

        // Do not replace a spawn another handler has already rejected.
        if (event.isCanceled()) {
            return;
        }

        ResourceLocation baseId = EntityType.getKey(entity.getType());
        EntityType<?> replacementType = chooseReplacement(baseId, serverLevel);
        if (replacementType == null) {
            return;
        }

        try {
            EntityType<?> selectedType = replacementType;
            Mob replacement = (Mob) selectedType.create(serverLevel);
            if (replacement == null) {
                return;
            }

            replacement.moveTo(event.getX(), event.getY(), event.getZ(), entity.getYRot(), entity.getXRot());
            replacement.setDeltaMovement(entity.getDeltaMovement());

            REPLACING.set(true);
            try {
                replacement.finalizeSpawn(
                    serverLevel,
                    serverLevel.getCurrentDifficultyAt(replacement.blockPosition()),
                    MobSpawnType.NATURAL,
                    null,
                    null
                );
            } finally {
                REPLACING.set(false);
            }

            if (serverLevel.addFreshEntity(replacement)) {
                cancelSpawn(event);
            } else {
                replacement.discard();
            }
        } catch (Exception exception) {
            System.err.println("[Demon Spawn Replacement] Error replacing " + baseId + ": " + exception.getMessage());
            exception.printStackTrace();
        }
    }

    /** Used by the Infinity Castle timed spawner when the disable option is enabled. */
    public static boolean isTweaksDemonType(EntityType<?> entityType) {
        if (entityType == null) {
            return false;
        }

        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        return entityId != null
                && TWEAKS_NAMESPACE.equals(entityId.getNamespace())
                && (entityType.is(EntityTagHelper.DEMON) || DemonRegistry.isRegistered(entityId));
    }

    private static EntityType<?> chooseReplacement(ResourceLocation baseId, ServerLevel level) {
        List<EntityType<?>> candidates;
        if (EASY_DEMONS.contains(baseId)) {
            candidates = List.of(
                ModEntities.DEMON_EFE.get(),
                ModEntities.DEMON_ARI.get(),
                ModEntities.DEMON_KAI.get(),
                ModEntities.DEMON_MAKENA.get(),
                ModEntities.DEMON_NOOR.get(),
                ModEntities.DEMON_SUNNY.get(),
                ModEntities.DEMON_ZURI.get()
            );
        } else if (SWAMP_DEMONS.contains(baseId)) {
            candidates = List.of(ModEntities.SWAMP_DEMON.get());
        } else if (HARD_DEMONS.contains(baseId)) {
            candidates = List.of(ModEntities.SIX_EYE_DEMON.get());
        } else {
            return null;
        }

        List<EntityType<?>> enabledCandidates = candidates.stream()
            .filter(candidate -> replacementPriority(candidate) > 0.0D)
            .toList();
        if (enabledCandidates.isEmpty()) {
            return null;
        }

        EntityType<?> selected = enabledCandidates.get(level.random.nextInt(enabledCandidates.size()));
        ResourceLocation selectedId = BuiltInRegistries.ENTITY_TYPE.getKey(selected);
        if (selectedId == null) {
            return null;
        }

        double priority = replacementPriority(selected);
        return level.random.nextDouble() < priority / 100.0D ? selected : null;
    }

    private static double replacementPriority(EntityType<?> entityType) {
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        if (entityId == null) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(100.0D, SpawnRateConfig.getSpawnRate(entityId.toString())));
    }

    private static ResourceLocation id(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    private static void cancelSpawn(MobSpawnEvent.FinalizeSpawn event) {
        event.setSpawnCancelled(true);
        event.setCanceled(true);
    }
}
