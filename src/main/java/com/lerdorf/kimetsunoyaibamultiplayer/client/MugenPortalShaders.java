package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import java.io.IOException;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;

@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MugenPortalShaders {
    private static ShaderInstance portalSkyShader;

    private MugenPortalShaders() {
    }

    public static ShaderInstance getPortalSkyShader() {
        return portalSkyShader;
    }

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(
                new ShaderInstance(
                    event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "mugen_portal_sky"),
                    DefaultVertexFormat.POSITION
                ),
                shader -> portalSkyShader = shader
            );
        } catch (IOException e) {
            Log.warn("Mugen portal shader registration failed: {}", e.getMessage());
        }
    }
}
