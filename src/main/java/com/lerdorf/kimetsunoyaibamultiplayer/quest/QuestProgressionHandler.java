package com.lerdorf.kimetsunoyaibamultiplayer.quest;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.config.CustomProgressionConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.EyeFamiliarEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.GeckolibCrowEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.OrochiEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.meditation.MeditationMenuService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
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
        QuestScenarioActions.tickSatokosBowSwampDemonResistanceRemoval(player);
        QuestScenarioActions.tickTamayoHouseTest(player);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer deadPlayer && !deadPlayer.level().isClientSide()) {
            PlayerRole role = MeditationMenuService.resolveRoleForProgression(deadPlayer);
            QuestProgressionManager.handlePlayerDeath(deadPlayer, role);
        }

        ServerPlayer player = event.getSource().getEntity() instanceof ServerPlayer attacker ? attacker : null;
        if (player != null && !player.level().isClientSide()) {
            PlayerRole role = MeditationMenuService.resolveRoleForProgression(player);
            QuestProgressionManager.handleKill(player, event.getEntity(), role);
        }

        // Check if the killed entity is the quest-targeted swamp demon
        String targetKey = event.getEntity().getPersistentData().getString(
            com.lerdorf.kimetsunoyaibamultiplayer.quest.QuestScenarioActions.QUEST_TARGET_ID_TAG);
        if ("swamp_demon_kidnappers_bog_satoko".equals(targetKey)) {
            if (event.getEntity().level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                for (ServerPlayer nearbyPlayer : serverLevel.players()) {
                    if (nearbyPlayer == player || nearbyPlayer.distanceToSqr(event.getEntity()) > 128.0D * 128.0D) {
                        continue;
                    }
                    PlayerRole nearbyRole = MeditationMenuService.resolveRoleForProgression(nearbyPlayer);
                    QuestProgressionManager.handleKidnappersBogSatokoDemonKilled(nearbyPlayer, event.getEntity(), nearbyRole);
                }
            }
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
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Mob mob)
            || !(event.getNewTarget() instanceof ServerPlayer player)
            || player.level().isClientSide()) {
            return;
        }
        if (!QuestProgressionManager.isCruelAsakusaActiveForPlayer(player)
            || !QuestScenarioActions.isAsakusaCompanionNpc(mob)) {
            return;
        }
        QuestScenarioActions.clearMobTargeting(mob, player);
        event.setCanceled(true);
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
        handleEntityInteract(event, player, target);
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getLevel().isClientSide() || !CustomProgressionConfig.isCustomProgressionEnabled()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Entity target = unwrapCrowMirror(event.getTarget());
        handleEntityInteract(event, player, target);
    }

    private static void handleEntityInteract(PlayerInteractEvent event, ServerPlayer player, Entity target) {
        PlayerRole role = MeditationMenuService.resolveRoleForProgression(player);

        if (isOwnedQuestEntity(player, target)) {
            if (target instanceof EyeFamiliarEntity eyeFamiliar && player.isShiftKeyDown()) {
                if (eyeFamiliar.openMugenDoor(player)) {
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.SUCCESS);
                }
                return;
            }

            if (target instanceof OrochiEntity orochi && player.isShiftKeyDown()) {
                if (orochi.mountOwner(player)) {
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.SUCCESS);
                }
                return;
            }

            String speaker = target instanceof OrochiEntity ? "Orochi"
                : target instanceof EyeFamiliarEntity ? "Eye Familiar"
                : "Crow";
            if (QuestProgressionManager.handleQuestEntityInteract(player, role, speaker)) {
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

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer player) || !player.isShiftKeyDown()) {
            return;
        }

        if (Config.logDebug) {
            Log.debug("[Orochi] Shift-right-click block dismount attempt at {} by {} (passengers={})",
                event.getPos(), player.getName().getString(), player.getPassengers().size());
        }

        if (tryDismountOwnedOrochi(player, event)) {
            return;
        }
    }

    @SubscribeEvent
    public static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        if (event.getEntity().level().isClientSide() || !(event.getEntity() instanceof ServerPlayer player) || !player.isShiftKeyDown()) {
            return;
        }

        if (Config.logDebug) {
            Log.debug("[Orochi] Shift-right-click empty dismount attempt by {} at {} (passengers={})",
                player.getName().getString(), player.blockPosition(), player.getPassengers().size());
        }

        tryDismountOwnedOrochi(player, event);
    }

    private static boolean tryDismountOwnedOrochi(ServerPlayer player, PlayerInteractEvent event) {
        for (Entity passenger : player.getPassengers()) {
            if (passenger instanceof OrochiEntity orochi && player.getUUID().equals(orochi.getOwnerUUID())) {
                if (Config.logDebug) {
                    Log.debug("[Orochi] Found owned passenger {} for {} (cooldownRemaining={} ticks, vehicle={})",
                        orochi.getUUID(), player.getName().getString(), orochi.getMountToggleCooldownRemainingTicks(),
                        orochi.getVehicle() == null ? "none" : orochi.getVehicle().getName().getString());
                }
                if (orochi.dismountToSafeLocation()) {
                    if (Config.logDebug) {
                        Log.debug("[Orochi] Dismounted {} from {} to safe location", orochi.getUUID(), player.getName().getString());
                    }
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    return true;
                }
                if (Config.logDebug) {
                    Log.debug("[Orochi] Dismount denied for {} (cooldownRemaining={} ticks)",
                        player.getName().getString(), orochi.getMountToggleCooldownRemainingTicks());
                }
                return false;
            }
        }
        if (Config.logDebug) {
            Log.debug("[Orochi] Shift-right-click dismount attempt found no owned Orochi passenger for {}", player.getName().getString());
        }
        return false;
    }

    private static boolean isOwnedQuestEntity(ServerPlayer player, Entity target) {
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
        return typeKey.contains("kasugai_crow")
            || typeKey.contains("princess")
            || typeKey.contains("orochi")
            || typeKey.contains("eye_familiar");
    }
}
