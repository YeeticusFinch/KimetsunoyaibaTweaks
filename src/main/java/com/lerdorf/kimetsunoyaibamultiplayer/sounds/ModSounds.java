package com.lerdorf.kimetsunoyaibamultiplayer.sounds;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, KimetsunoyaibaMultiplayer.MODID);

    // Crow sounds
    public static final RegistryObject<SoundEvent> CROW1 = registerSoundEvent("crow1");
    public static final RegistryObject<SoundEvent> CROW2 = registerSoundEvent("crow2");
    public static final RegistryObject<SoundEvent> CROW3 = registerSoundEvent("crow3");

    private static RegistryObject<SoundEvent> registerSoundEvent(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, name);
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
