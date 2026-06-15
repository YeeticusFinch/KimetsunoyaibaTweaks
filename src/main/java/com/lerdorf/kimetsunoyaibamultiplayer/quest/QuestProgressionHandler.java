package com.lerdorf.kimetsunoyaibamultiplayer.quest;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.config.CustomProgressionConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.GeckolibCrowEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.meditation.MeditationMenuService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
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
        QuestScenarioActions.tickTamayoHouseTest(player);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer deadPlayer && !deadPlayer.level().isClientSide()) {
            PlayerRole role = MeditationMenuService.resolveRoleForProgression(deadPlayer);
            QuestProgressionManager.handlePlayerDeath(deadPlayer, role);
        }

        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }

        PlayerRole role = MeditationMenuService.resolveRoleForProgression(player);
        QuestProgressionManager.handleKill(player, event.getEntity(), role);

        // Check if the killed entity is the quest-targeted swamp demon
        String targetKey = event.getEntity().getPersistentData().getString(
            com.lerdorf.kimetsunoyaibamultiplayer.quest.QuestScenarioActions.QUEST_TARGET_ID_TAG);
        if ("swamp_demon_kidnappers_bog_satoko".equals(targetKey)) {
            player.getPersistentData().putBoolean("KnYSwampDemonKilled", true);
        }
    }

    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }

        PlayerRole role = MeditationMenuService.resolveRoleForProgression(player);
        QuestProgressionManager.handleHumanFleshConsumed(player, event.getItem(), role);
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

        // Check if clicking on Kazumi during walking phase
        String questNpcId = target.getPersistentData().getString(QuestScenarioActions.QUEST_NPC_ID_TAG);
        if ("kazumi".equals(questNpcId)) {
            ItemStack heldStack = player.getItemInHand(event.getHand());
            if (QuestScenarioActions.isSatokosBow(heldStack)) {
                boolean swampDemonAliveNearby = QuestScenarioActions.isSwampDemonAliveNear(player, target, 100.0D);
                QuestProgressionManager.handleKazumiBowTurnIn(player, target, role, swampDemonAliveNearby);
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                return;
            }

            String kazumiCoords = QuestScenarioActions.getKazumiTargetCoordinates(player);
            if (kazumiCoords != null && QuestScenarioActions.isKazumiWalking(player)) {
                player.sendSystemMessage(Component.literal("§6[Kazumi] §fSatoko disappeared at " + kazumiCoords));
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                return;
            }
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
        if (!(target instanceof TamableAnimal tamableAnimal) || !tamableAnimal.isTame()) {
            return false;
        }
        if (!player.getUUID().equals(tamableAnimal.getOwnerUUID())) {
            return false;
        }
        String typeKey = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(target.getType()).toString();
        return typeKey.contains("kasugai_crow") || typeKey.contains("princess");
    }
}
