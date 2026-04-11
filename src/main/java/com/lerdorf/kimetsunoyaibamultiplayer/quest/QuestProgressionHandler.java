package com.lerdorf.kimetsunoyaibamultiplayer.quest;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.config.CustomProgressionConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.GeckolibCrowEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.meditation.MeditationMenuService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
public final class QuestProgressionHandler {
    private QuestProgressionHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        PlayerRole role = MeditationMenuService.resolveRoleForProgression(player);
        QuestProgressionManager.tick(player, role);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }

        PlayerRole role = MeditationMenuService.resolveRoleForProgression(player);
        QuestProgressionManager.handleKill(player, event.getEntity(), role);
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() || !CustomProgressionConfig.isCustomProgressionEnabled()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Entity target = unwrapCrowMirror(event.getTarget());
        PlayerRole role = MeditationMenuService.resolveRoleForProgression(player);

        if (isOwnedKasugaiCrow(player, target)) {
            if (QuestProgressionManager.handleCrowInteract(player, role)) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
            return;
        }

        if (QuestProgressionManager.handleTalkToEntity(player, target, role)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    private static Entity unwrapCrowMirror(Entity target) {
        if (target instanceof GeckolibCrowEntity geckolibCrowEntity) {
            Entity original = geckolibCrowEntity.getOriginalCrow();
            if (original != null) {
                return original;
            }
        }
        return target;
    }

    private static boolean isOwnedKasugaiCrow(ServerPlayer player, Entity target) {
        if (target == null) {
            return false;
        }
        String entityKey = target.getType().toString();
        if (!entityKey.contains("kasugai_crow")) {
            return false;
        }
        if (!(target instanceof TamableAnimal tamableAnimal) || !tamableAnimal.isTame()) {
            return false;
        }
        return player.getUUID().equals(tamableAnimal.getOwnerUUID());
    }
}
