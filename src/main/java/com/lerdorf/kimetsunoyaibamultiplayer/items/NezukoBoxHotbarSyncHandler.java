package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.NezukoBoxSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
public final class NezukoBoxHotbarSyncHandler {
    private static final String LAST_HAS_BOX_TAG = "KimetsuMpNezukoBoxHotbar";
    private static final String LAST_OPEN_TAG = "KimetsuMpNezukoBoxOpen";
    private static final String TICK_COUNTER_TAG = "KimetsuMpNezukoBoxSyncTicks";

    private NezukoBoxHotbarSyncHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        if (!(event.player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        var data = serverPlayer.getPersistentData();
        int ticks = data.getInt(TICK_COUNTER_TAG) + 1;
        data.putInt(TICK_COUNTER_TAG, ticks);
        if (ticks % 10 != 0) {
            return;
        }

        syncIfChanged(serverPlayer);
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer trackingPlayer) ||
            !(event.getTarget() instanceof ServerPlayer trackedPlayer)) {
            return;
        }

        BoxState state = findBoxState(trackedPlayer);
        ModNetworking.sendToPlayer(
            new NezukoBoxSyncPacket(trackedPlayer.getUUID(), state.hasBox(), state.open()),
            trackingPlayer
        );
    }

    public static void syncNow(ServerPlayer player) {
        BoxState state = findBoxState(player);
        remember(player, state);
        ModNetworking.sendToTrackingAndSelf(
            new NezukoBoxSyncPacket(player.getUUID(), state.hasBox(), state.open()),
            player
        );
    }

    private static void syncIfChanged(ServerPlayer player) {
        BoxState state = findBoxState(player);
        var data = player.getPersistentData();
        boolean lastHasBox = data.getBoolean(LAST_HAS_BOX_TAG);
        boolean lastOpen = data.getBoolean(LAST_OPEN_TAG);
        if (state.hasBox() == lastHasBox && state.open() == lastOpen) {
            return;
        }

        remember(player, state);
        ModNetworking.sendToTrackingAndSelf(
            new NezukoBoxSyncPacket(player.getUUID(), state.hasBox(), state.open()),
            player
        );
    }

    private static void remember(ServerPlayer player, BoxState state) {
        var data = player.getPersistentData();
        data.putBoolean(LAST_HAS_BOX_TAG, state.hasBox());
        data.putBoolean(LAST_OPEN_TAG, state.open());
    }

    private static BoxState findBoxState(Player player) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(ModItems.NEZUKO_BOX.get())) {
                return new BoxState(true, NezukoBoxItem.isOpen(stack));
            }
        }
        return new BoxState(false, false);
    }

    private record BoxState(boolean hasBox, boolean open) {
    }
}
