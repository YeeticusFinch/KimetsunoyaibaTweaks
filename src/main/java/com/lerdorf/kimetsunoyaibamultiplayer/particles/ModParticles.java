package com.lerdorf.kimetsunoyaibamultiplayer.particles;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
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

    public static void register(IEventBus eventBus) {
        PARTICLE_TYPES.register(eventBus);
    }
}