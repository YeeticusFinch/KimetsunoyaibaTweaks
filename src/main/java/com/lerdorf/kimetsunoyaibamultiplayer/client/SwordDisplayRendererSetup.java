package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.client.renderer.DemonEyesPlayerLayer;
import com.lerdorf.kimetsunoyaibamultiplayer.client.renderer.SwordDisplayRenderer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.client.GeoSwordDisplayLayer;
import net.minecraft.client.Minecraft;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/**
 * Sets up the sword display renderer layer on player models and GeckoLib entities
 */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SwordDisplayRendererSetup {

    private static final String BASE_MODID = "kimetsunoyaiba";

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        Log.info("Adding sword display renderer layers to player models");

        // Add to all player model types (default, slim)
        addLayerToPlayerSkin(event, "default");
        addLayerToPlayerSkin(event, "slim");

        addLayerToBaseModEntities(event);

        Log.info("Sword display renderer layers added successfully");
    }


    private static void addLayerToBaseModEntities(EntityRenderersEvent.AddLayers event) {
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (id == null || !BASE_MODID.equals(id.getNamespace())) {
                continue;
            }

            Class<?> baseClass = type.getBaseClass();
            if (!LivingEntity.class.isAssignableFrom(baseClass) ||
                !GeoAnimatable.class.isAssignableFrom(baseClass)) {
                continue;
            }

            @SuppressWarnings("unchecked")
            EntityType<? extends LivingEntity> livingType = (EntityType<? extends LivingEntity>) type;
            EntityRenderer<?> renderer = event.getRenderer(livingType);
            if (renderer instanceof GeoEntityRenderer<?> geoRenderer) {
                addGeoSwordLayer(geoRenderer, id);
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addGeoSwordLayer(GeoEntityRenderer geoRenderer, ResourceLocation id) {
        geoRenderer.addRenderLayer(new GeoSwordDisplayLayer(geoRenderer));
        Log.debug("Added sword display layer to base mod entity renderer {}", id);
    }

    private static void addLayerToPlayerSkin(EntityRenderersEvent.AddLayers event, String skinName) {
        EntityRenderer<? extends net.minecraft.world.entity.player.Player> renderer = event.getSkin(skinName);

        if (renderer instanceof PlayerRenderer playerRenderer) {
            playerRenderer.addLayer(new DemonEyesPlayerLayer(playerRenderer));
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
