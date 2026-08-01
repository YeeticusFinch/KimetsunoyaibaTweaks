package com.lerdorf.kimetsunoyaibamultiplayer.gravity;

import com.lerdorf.kimetsunoyaibamultiplayer.config.GravityConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.gravity.engine.GravityAPI;
import com.lerdorf.kimetsunoyaibamultiplayer.gravity.engine.GravityCapabilityImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class GravityEvents {
    private GravityEvents() {
    }

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        GravityCapabilityImpl backend = new GravityCapabilityImpl(event.getObject());
        event.addCapability(com.lerdorf.kimetsunoyaibamultiplayer.gravity.engine.IGravityCapability.ID, backend);
        event.addListener(backend::invalidate);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        Player clone = event.getEntity();
        original.reviveCaps();
        GravityAPI.getGravityCapability(original).ifPresent(oldCap ->
            GravityAPI.getGravityCapability(clone).ifPresent(newCap -> {
                newCap.deserializeNBT(oldCap.serializeNBT());
                if (event.isWasDeath() && GravityConfig.resetGravityOnRespawn) {
                    newCap.reset();
                }
            }));
        original.invalidateCaps();
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        GravityAPI.getGravityCapability(event.getEntity()).ifPresent(GravityCapabilityImpl::tick);
    }
}
