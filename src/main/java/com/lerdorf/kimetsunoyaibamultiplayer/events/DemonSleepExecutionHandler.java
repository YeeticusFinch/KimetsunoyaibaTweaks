package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.DamageCalculator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DemonSleepExecutionHandler {

    private static final float SLEEP_EXECUTION_DAMAGE = 40.0F;

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

    private static void executeSleepAttack(LivingEntity attacker, LivingEntity target) {
        float damage = DamageCalculator.calculateScaledDamage(attacker, SLEEP_EXECUTION_DAMAGE);
        Damager.hurt(attacker, target, damage);
    }

    private static boolean isSleepingInBed(LivingEntity entity) {
        return entity.isSleeping() && entity.getSleepingPos().isPresent();
    }

    private static boolean isDemonPlayer(LivingEntity entity) {
        return entity instanceof Player player && player.getPersistentData().getBoolean("oni");
    }
}
