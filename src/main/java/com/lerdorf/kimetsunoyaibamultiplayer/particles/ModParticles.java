package com.lerdorf.kimetsunoyaibamultiplayer.particles;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
        DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, KimetsunoyaibaMultiplayer.MODID);

    public static final RegistryObject<SimpleParticleType> MIST_PARTICLE = PARTICLE_TYPES.register("mist",
        () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> SMALL_MIST_PARTICLE = PARTICLE_TYPES.register("mistsmall",
        () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> LOVE_IMPACT = PARTICLE_TYPES.register("love_impact",
        () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> LOVE_SLASH = PARTICLE_TYPES.register("love_slash",
        () -> new SimpleParticleType(false));

    @SuppressWarnings("deprecation")
    public static final RegistryObject<ParticleType<EnergyParticleOptions>> ENERGY = PARTICLE_TYPES.register("energy",
        () -> new ParticleType<EnergyParticleOptions>(false, EnergyParticleOptions.DESERIALIZER) {
            @Override
            public Codec<EnergyParticleOptions> codec() {
                return EnergyParticleOptions.CODEC;
            }
        });

    public static void register(IEventBus eventBus) {
        PARTICLE_TYPES.register(eventBus);
    }
}