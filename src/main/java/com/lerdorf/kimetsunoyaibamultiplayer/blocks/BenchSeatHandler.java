package com.lerdorf.kimetsunoyaibamultiplayer.blocks;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.BenchSeatEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Sitting logic for benches. The block's own use() handles most right-clicks;
 * this event handler is a fallback that also catches hand-specific edge cases
 * and keeps behaviour consistent with CushionSeatHandler.
 */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
public final class BenchSeatHandler {

    private BenchSeatHandler() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (!(state.getBlock() instanceof BenchBlock)) {
            return;
        }

        // BenchBlock.use() handles the actual seating; cancel the default
        // interaction so blocks behind the bench don't also activate.
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);

        if (event.getLevel().isClientSide()) {
            return;
        }

        Player player = event.getEntity();
        if (!player.isAlive() || player.isShiftKeyDown() || player.isPassenger()) {
            return;
        }

        seatEntity(event.getLevel(), event.getPos(), player);
    }

    /**
     * Seat the given entity on the bench at pos, reusing an existing seat
     * entity if one is already present.
     */
    public static void seatEntity(Level level, BlockPos pos, Entity entity) {
        if (entity.isPassenger()) {
            return;
        }

        AABB searchBox = new AABB(pos).inflate(0.4D, 0.5D, 0.4D);
        for (BenchSeatEntity seat : level.getEntitiesOfClass(BenchSeatEntity.class, searchBox)) {
            if (!seat.matchesSeat(pos)) {
                continue;
            }
            if (seat.getPassengers().isEmpty()) {
                entity.startRiding(seat, false);
            }
            return;
        }

        BenchSeatEntity seat = BenchSeatEntity.create(level, pos);
        level.addFreshEntity(seat);
        entity.startRiding(seat, false);
    }
}
