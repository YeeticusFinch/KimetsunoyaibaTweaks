package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.DamageCalculator;
import com.lerdorf.kimetsunoyaibamultiplayer.quest.QuestProgressionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.SleepingLocationCheckEvent;
import net.minecraftforge.event.entity.player.SleepingTimeCheckEvent;
import net.minecraftforge.event.level.SleepFinishedTimeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DemonSleepExecutionHandler {

    private static final float SLEEP_EXECUTION_DAMAGE = 40.0F;
    private static final String DAYTIME_DEMON_SLEEP_DENIED_MESSAGE = "You can only sleep during the day";
    private static final String DAYTIME_DEMON_SLEEP_TAG = "KnYDaytimeDemonSleep";
    private static final String DAYTIME_DEMON_SLEEP_DENIED_MESSAGE_PENDING_TICK_TAG = "KnYDaytimeDemonSleepDeniedMessagePendingTick";

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        Player attacker = event.getEntity();
        if (!isDemonPlayer(attacker)) {
            return;
        }
        if (!(event.getTarget() instanceof LivingEntity target)) {
            return;
        }
        if (!isSleepingInBed(target) || isDemonPlayer(target)) {
            return;
        }

        executeSleepAttack(attacker, target);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        Player attacker = event.getEntity();
        if (!isDemonPlayer(attacker)) {
            return;
        }

        BlockPos clickedPos = event.getPos();
        BlockState clickedState = event.getLevel().getBlockState(clickedPos);
        if (!(clickedState.getBlock() instanceof BedBlock)) {
            return;
        }

        LivingEntity sleeper = findSleeperInBed((ServerLevel) event.getLevel(), clickedPos, clickedState);
        if (sleeper == null || isDemonPlayer(sleeper)) {
            return;
        }

        executeSleepAttack(attacker, sleeper);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player) || !isDemonPlayer(player)) {
            return;
        }

        BlockState clickedState = event.getLevel().getBlockState(event.getPos());
        if (!(clickedState.getBlock() instanceof BedBlock)) {
            return;
        }

        // Let vanilla bed interaction run so respawn-point validation and
        // missing/obstructed-bed handling stay unchanged.
        if (player.isSleeping() || player.isPassenger()) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.PASS);
        }
    }


    @SubscribeEvent
    public static void onSleepingTimeCheck(SleepingTimeCheckEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        Player player = event.getEntity();
        if (!isDemonPlayer(player)) {
            return;
        }

        long gameTime = player.level().getGameTime();
        if (player.level().isDay()) {
            event.setResult(net.minecraftforge.eventbus.api.Event.Result.ALLOW);
            player.getPersistentData().putBoolean(DAYTIME_DEMON_SLEEP_TAG, true);
            player.getPersistentData().remove(DAYTIME_DEMON_SLEEP_DENIED_MESSAGE_PENDING_TICK_TAG);
        } else {
            event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
            clearDaytimeDemonSleep(player);
            player.getPersistentData().putLong(DAYTIME_DEMON_SLEEP_DENIED_MESSAGE_PENDING_TICK_TAG, gameTime + 1L);
        }
    }

    @SubscribeEvent
    public static void onSleepingLocationCheck(SleepingLocationCheckEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        if (event.getEntity() instanceof Player player && isDemonPlayer(player) && player.isSleeping()) {
            event.setResult(net.minecraftforge.eventbus.api.Event.Result.ALLOW);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer() == null) {
            return;
        }

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            long currentGameTime = player.level().getGameTime();
            if (player.isSleeping() && isDemonPlayer(player) && isDaytimeDemonSleep(player)) {
                continue;
            }

            if (!player.isSleeping() && isDaytimeDemonSleep(player)) {
                clearDaytimeDemonSleep(player);
            }

            long pendingMessageTick = player.getPersistentData().getLong(DAYTIME_DEMON_SLEEP_DENIED_MESSAGE_PENDING_TICK_TAG);
            if (pendingMessageTick > 0L && currentGameTime >= pendingMessageTick) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(DAYTIME_DEMON_SLEEP_DENIED_MESSAGE), true);
                player.getPersistentData().remove(DAYTIME_DEMON_SLEEP_DENIED_MESSAGE_PENDING_TICK_TAG);
            }
        }
    }

    @SubscribeEvent
    public static void onSleepFinishedTime(SleepFinishedTimeEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        boolean hasDaytimeDemonSleeper = false;
        for (ServerPlayer player : serverLevel.players()) {
            if (player.isSleeping() && isDemonPlayer(player) && isDaytimeDemonSleep(player)) {
                hasDaytimeDemonSleeper = true;
                break;
            }
        }

        if (!hasDaytimeDemonSleeper) {
            return;
        }

        long dayTime = serverLevel.getDayTime();
        long cycleTime = dayTime % 24000L;
        long nextNightTime = cycleTime < 13000L
            ? dayTime - cycleTime + 13000L
            : dayTime - cycleTime + 24000L + 13000L;
        event.setTimeAddition(nextNightTime);
    }

    private static LivingEntity findSleeperInBed(ServerLevel level, BlockPos clickedPos, BlockState clickedState) {
        BlockPos otherPartPos = getOtherBedPartPos(clickedPos, clickedState);
        AABB searchBox = new AABB(clickedPos).inflate(1.5D);
        if (otherPartPos != null) {
            searchBox = searchBox.minmax(new AABB(otherPartPos).inflate(1.5D));
        }

        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, searchBox, LivingEntity::isSleeping)) {
            Optional<BlockPos> sleepingPos = candidate.getSleepingPos();
            if (sleepingPos.isEmpty()) {
                continue;
            }
            BlockPos pos = sleepingPos.get();
            if (pos.equals(clickedPos) || (otherPartPos != null && pos.equals(otherPartPos))) {
                return candidate;
            }
        }
        return null;
    }

    private static BlockPos getOtherBedPartPos(BlockPos bedPos, BlockState state) {
        if (!(state.getBlock() instanceof BedBlock) || !state.hasProperty(BlockStateProperties.BED_PART)) {
            return null;
        }
        BedPart part = state.getValue(BlockStateProperties.BED_PART);
        if (!state.hasProperty(BedBlock.FACING)) {
            return null;
        }
        return part == BedPart.HEAD ? bedPos.relative(state.getValue(BedBlock.FACING).getOpposite())
            : bedPos.relative(state.getValue(BedBlock.FACING));
    }

    public static void executeSleepAttack(LivingEntity attacker, LivingEntity target) {
        boolean wasAlive = target.isAlive();
        float damage = (SLEEP_EXECUTION_DAMAGE);
        Damager.hurt(attacker, target, damage);
        if (wasAlive && !target.isAlive() && attacker instanceof ServerPlayer player) {
            QuestProgressionManager.handleSleepingHumanKilled(player, target);
        }
    }

    public static boolean isSleepingInBed(LivingEntity entity) {
        return entity.isSleeping() && entity.getSleepingPos().isPresent();
    }

    private static boolean isDaytimeDemonSleep(Player player) {
        return player.getPersistentData().getBoolean(DAYTIME_DEMON_SLEEP_TAG);
    }

    private static void clearDaytimeDemonSleep(Player player) {
        player.getPersistentData().remove(DAYTIME_DEMON_SLEEP_TAG);
        player.getPersistentData().remove(DAYTIME_DEMON_SLEEP_DENIED_MESSAGE_PENDING_TICK_TAG);
    }

    private static boolean isDemonPlayer(LivingEntity entity) {
        return entity instanceof Player player && player.getPersistentData().getBoolean("oni");
    }
}
