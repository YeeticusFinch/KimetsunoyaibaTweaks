package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.BenchBlock;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.BenchSeatHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.BenchSeatEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.util.EntityTagHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Allows peaceful (non-combat, non-aggro) KnY mobs - demon slayers, civilians,
 * and demons - to wander over to nearby benches and sit down. Any damage taken
 * or new combat target cancels the sitting and forces them off the bench.
 */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
public final class BenchMobSittingHandler {
    /** How often (in ticks) mobs are scanned for benches to sit on. */
    private static final int SCAN_INTERVAL = 40;
    /** Radius (blocks) a mob looks for a bench from its position. */
    private static final double BENCH_SEARCH_RADIUS = 2.0D;
    /** Chance per scan that an eligible mob decides to sit (keeps it casual). */
    private static final double SIT_CHANCE = 0.3D;

    private static int tickCounter = 0;

    private BenchMobSittingHandler() {
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide()) {
            return;
        }

        if (++tickCounter < SCAN_INTERVAL) {
            return;
        }
        tickCounter = 0;

        Level level = event.level;

        // Mobs already sitting: verify their state, dismount on combat
        for (var player : level.players()) {
            if (player.isSpectator()) {
                continue;
            }

            List<BenchSeatEntity> seats = level.getEntitiesOfClass(BenchSeatEntity.class,
                player.getBoundingBox().inflate(48.0D));
            for (BenchSeatEntity seat : seats) {
                for (Entity passenger : seat.getPassengers()) {
                    if (passenger instanceof Mob mob && shouldDismount(mob)) {
                        mob.stopRiding();
                    }
                }
            }

            // Idle mobs near benches: sit down occasionally
            List<Mob> mobs = level.getEntitiesOfClass(Mob.class,
                player.getBoundingBox().inflate(48.0D));
            for (Mob mob : mobs) {
                trySitOnBench(level, mob);
            }
        }
    }

    private static void trySitOnBench(Level level, Mob mob) {
        if (mob.isPassenger() || !mob.isAlive() || mob.isNoAi()) {
            return;
        }

        if (!isEligibleSitter(mob)) {
            return;
        }

        if (shouldDismount(mob)) {
            return;
        }

        if (mob.getRandom().nextDouble() > SIT_CHANCE) {
            return;
        }

        BlockPos mobPos = mob.blockPosition();
        BlockPos found = null;

        for (BlockPos pos : BlockPos.betweenClosed(
                mobPos.offset(-2, -1, -2), mobPos.offset(2, 1, 2))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof BenchBlock && isBenchFree(level, pos)) {
                found = pos.immutable();
                break;
            }
        }

        if (found == null) {
            return;
        }

        BenchSeatHandler.seatEntity(level, found, mob);
    }

    private static boolean isBenchFree(Level level, BlockPos pos) {
        return level.getEntitiesOfClass(BenchSeatEntity.class, new AABB(pos).inflate(0.4D, 0.5D, 0.4D))
            .stream()
            .filter(s -> s.matchesSeat(pos))
            .allMatch(s -> s.getPassengers().isEmpty());
    }

    /**
     * Demon slayers, civilians, and demons (from either mod) may sit.
     */
    private static boolean isEligibleSitter(Mob mob) {
        return EntityTagHelper.isDemonSlayer(mob)
            || EntityTagHelper.isCivilian(mob)
            || EntityTagHelper.isDemon(mob);
    }

    /**
     * A mob should get off the bench as soon as it is attacked, aggros onto
     * another entity, or otherwise has a combat target.
     */
    private static boolean shouldDismount(Mob mob) {
        return mob.getLastHurtByMob() != null || mob.getTarget() != null;
    }

    /**
     * Any damage to a seated mob forces it off the bench immediately.
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Mob mob && mob.isPassenger()
                && mob.getVehicle() instanceof BenchSeatEntity) {
            mob.stopRiding();
        }
    }
}
