package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.config.FutonConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
public final class FutonInteractionHandler {
    private static final ResourceLocation FUTON_2_ID =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "futon_2");

    private FutonInteractionHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()
            || !FutonConfig.isTimeSkipDisabled()
            || !event.getEntity().isShiftKeyDown()) {
            return;
        }

        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(
            event.getLevel().getBlockState(event.getPos()).getBlock());
        if (!FUTON_2_ID.equals(blockId)) {
            return;
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
