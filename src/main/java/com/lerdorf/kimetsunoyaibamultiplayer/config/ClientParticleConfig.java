package com.lerdorf.kimetsunoyaibamultiplayer.config;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

public final class ClientParticleConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    static {
        BUILDER.comment("Client-side particle rendering settings")
            .push("particles");
    }

    private static final ForgeConfigSpec.IntValue ABILITY_PARTICLE_RENDER_PERCENT = BUILDER
        .comment(
            "Percentage of KimetsunoYaiba and KimetsunoYaiba Multiplayer ability particles to render on this client.",
            "100 renders all eligible particles, 50 renders about every other particle, and 0 suppresses them all."
        )
        .defineInRange("ability-particle-render-percent", 100, 0, 100);

    static {
        BUILDER.pop();
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static int abilityParticleRenderPercent = 100;

    private ClientParticleConfig() {
    }

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event) {
        ModConfig config = event.getConfig();
        if (config == null || config.getSpec() != SPEC) {
            return;
        }

        abilityParticleRenderPercent = ABILITY_PARTICLE_RENDER_PERCENT.get();
        Log.debug("Client particle config loaded: abilityParticleRenderPercent={}", abilityParticleRenderPercent);
    }
}
