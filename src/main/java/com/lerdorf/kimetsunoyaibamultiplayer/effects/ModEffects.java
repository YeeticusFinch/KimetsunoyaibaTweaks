package com.lerdorf.kimetsunoyaibamultiplayer.effects;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, KimetsunoyaibaMultiplayer.MODID);

    // Omen of Ubuyashiki effect - for demons who kill demon slayers/hashira
    public static final RegistryObject<MobEffect> OMEN_OF_UBUYASHIKI = MOB_EFFECTS.register("omen_of_ubuyashiki",
            OmenOfUbuyashikiEffect::new);

    // Omen of Muzan effect - for humans who kill demons
    public static final RegistryObject<MobEffect> OMEN_OF_MUZAN = MOB_EFFECTS.register("omen_of_muzan",
            OmenOfMuzanEffect::new);

    // Favor of Ubuyashiki effect - positive effect for demons
    public static final RegistryObject<MobEffect> FAVOR_OF_UBUYASHIKI = MOB_EFFECTS.register("favor_of_ubuyashiki",
            FavorOfUbuyashikiEffect::new);

    // Favor of Muzan effect - positive effect for demons
    public static final RegistryObject<MobEffect> FAVOR_OF_MUZAN = MOB_EFFECTS.register("favor_of_muzan",
            FavorOfMuzanEffect::new);

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}