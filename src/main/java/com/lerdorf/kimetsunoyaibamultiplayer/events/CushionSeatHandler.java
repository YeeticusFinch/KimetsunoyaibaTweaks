package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.CushionSeatEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;

@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
public final class CushionSeatHandler {
    private static final Set<ResourceLocation> CUSHION_BLOCK_IDS = Set.of(
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "cushion_green"),
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "cushion_red"),
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "cushion_purple"),
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "cushion_blue")
    );

    private CushionSeatHandler() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (!isCushionBlock(state)) {
            return;
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);

        if (event.getLevel().isClientSide()) {
            return;
        }

        Player player = event.getEntity();
        if (!player.isAlive() || player.isShiftKeyDown() || player.isPassenger()) {
            return;
        }

        seatPlayer(event.getLevel(), event.getPos(), player);
    }

    private static void seatPlayer(Level level, BlockPos pos, Player player) {
        AABB searchBox = new AABB(pos).inflate(0.4D, 0.5D, 0.4D);
        for (CushionSeatEntity seat : level.getEntitiesOfClass(CushionSeatEntity.class, searchBox)) {
            if (!seat.matchesSeat(pos)) {
                continue;
            }
            if (seat.getPassengers().isEmpty()) {
                player.startRiding(seat, false);
            }
            return;
        }

        CushionSeatEntity seat = CushionSeatEntity.create(level, pos);
        level.addFreshEntity(seat);
        player.startRiding(seat, false);
    }

    public static boolean isCushionBlock(BlockState state) {
        Block block = state.getBlock();
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block);
        return blockId != null && CUSHION_BLOCK_IDS.contains(blockId);
    }
}
