package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.client.renderer.SwordDisplayRenderer;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
/**
 * Sets up the sword display renderer layer on player models
 */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SwordDisplayRendererSetup {

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        Log.info("Adding sword display renderer layers to player models");

        // Add to all player model types (default, slim)
        addLayerToPlayerSkin(event, "default");
        addLayerToPlayerSkin(event, "slim");

        Log.info("Sword display renderer layers added successfully");
    }

    private static void addLayerToPlayerSkin(EntityRenderersEvent.AddLayers event, String skinName) {
        EntityRenderer<? extends net.minecraft.world.entity.player.Player> renderer = event.getSkin(skinName);

        if (renderer instanceof PlayerRenderer playerRenderer) {
            playerRenderer.addLayer(new SwordDisplayRenderer(
                playerRenderer,
                Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer()
            ));
            Log.debug("Added sword display layer to {} player model", skinName);
        } else {
            Log.warn("Could not add sword display layer to {} player model (renderer type mismatch)", skinName);
        }
    }
}
