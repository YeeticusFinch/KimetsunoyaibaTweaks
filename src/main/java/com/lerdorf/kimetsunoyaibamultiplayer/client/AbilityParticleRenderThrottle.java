package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.config.ClientParticleConfig;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

public final class AbilityParticleRenderThrottle {
    private static final AtomicInteger PARTICLE_SEQUENCE = new AtomicInteger();

    private AbilityParticleRenderThrottle() {
    }

    public static boolean shouldRender(ParticleOptions particle) {
        if (!isKimetsunoyaibaParticle(particle)) {
            return true;
        }

        int percent = ClientParticleConfig.abilityParticleRenderPercent;
        if (percent >= 100) {
            return true;
        }
        if (percent <= 0) {
            return false;
        }

        int slot = Math.floorMod(PARTICLE_SEQUENCE.getAndIncrement(), 100);
        return slot < percent;
    }

    private static boolean isKimetsunoyaibaParticle(ParticleOptions particle) {
        ParticleType<?> type = particle.getType();
        ResourceLocation id = ForgeRegistries.PARTICLE_TYPES.getKey(type);
        if (id == null) {
            return false;
        }

        String namespace = id.getNamespace();
        return "kimetsunoyaiba".equals(namespace) || "kimetsunoyaibamultiplayer".equals(namespace);
    }
}
