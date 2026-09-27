package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.TrainWheelBlockEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
public final class TrainWheelDebugStickHandler {
    private TrainWheelDebugStickHandler() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !event.getItemStack().is(Items.DEBUG_STICK)) {
            return;
        }

        BlockEntity blockEntity = event.getLevel().getBlockEntity(event.getPos());
        if (!(blockEntity instanceof TrainWheelBlockEntity wheel)) {
            return;
        }

        if (!event.getLevel().isClientSide) {
            wheel.toggleSpinDirection();
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
