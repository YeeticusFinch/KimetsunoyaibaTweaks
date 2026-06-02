package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SwampDomainSpawnHandler {
    private static final ResourceLocation SWAMP_DOMAIN_DIM_ID =
        ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "swamp_domain");

    private static boolean isNaturalLikeSpawn(MobSpawnType spawnType) {
        return spawnType == MobSpawnType.NATURAL
            || spawnType == MobSpawnType.CHUNK_GENERATION
            || spawnType == MobSpawnType.STRUCTURE
            || spawnType == MobSpawnType.SPAWNER
            || spawnType == MobSpawnType.REINFORCEMENT
            || spawnType == MobSpawnType.PATROL;
    }

    @SubscribeEvent
    public static void onSpawnPlacementCheck(MobSpawnEvent.SpawnPlacementCheck event) {
        ServerLevel serverLevel = event.getLevel().getLevel();
        if (serverLevel == null) {
            return;
        }
        if (!serverLevel.dimension().location().equals(SWAMP_DOMAIN_DIM_ID)) {
            return;
        }
        if (!isNaturalLikeSpawn(event.getSpawnType())) {
            return;
        }

        event.setResult(Event.Result.DENY);
    }

    @SubscribeEvent
    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!serverLevel.dimension().location().equals(SWAMP_DOMAIN_DIM_ID)) {
            return;
        }
        if (!isNaturalLikeSpawn(event.getSpawnType())) {
            return;
        }

        event.setSpawnCancelled(true);
    }
}
