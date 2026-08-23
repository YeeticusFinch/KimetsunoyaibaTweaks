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

    public static final RegistryObject<MobEffect> DEMON_TRANSFORMATION = MOB_EFFECTS.register("demon_transformation",
            DemonTransformationEffect::new);

    // Vermilion Eye effect - enhanced perception with red tint, entity visibility through walls, and faster cooldowns
    public static final RegistryObject<MobEffect> VERMILION_EYE = MOB_EFFECTS.register("vermilion_eye",
            VermilionEyeEffect::new);

    // Spatial Awareness effect - detached free camera, movement lock, kneel pose, and monochrome vision
    public static final RegistryObject<MobEffect> SPATIAL_AWARENESS = MOB_EFFECTS.register("spatial_awareness",
            SpatialAwarenessEffect::new);

    // Killing Intent effect - stacking raid kill buff (+2% attack damage per level)
    public static final RegistryObject<MobEffect> KILLING_INTENT = MOB_EFFECTS.register("killing_intent",
            KillingIntentEffect::new);

    // Bleeding effect - causes extra attack damage and movement-triggered blood loss
    public static final RegistryObject<MobEffect> BLEEDING = MOB_EFFECTS.register("bleeding",
            BleedingEffect::new);

    // Fear effect - intermittent paralysis and client fear visuals
    public static final RegistryObject<MobEffect> FEAR = MOB_EFFECTS.register("fear",
            FearEffect::new);

    // Fear cooldown effect - prevents repeated low-level fear applications
    public static final RegistryObject<MobEffect> FEAR_COOLDOWN = MOB_EFFECTS.register("fear_cooldown",
            FearCooldownEffect::new);

    public static final RegistryObject<MobEffect> COURAGE = MOB_EFFECTS.register("courage",
            CourageEffect::new);

    public static final RegistryObject<MobEffect> DEMONIC_SATURATION = MOB_EFFECTS.register("demonic_saturation",
            DemonicSaturationEffect::new);

    public static final RegistryObject<MobEffect> DEMON_RESTORATION = MOB_EFFECTS.register("demon_restoration",
            DemonRestorationEffect::new);

    public static final RegistryObject<MobEffect> SUNLIGHT_RESISTANCE = MOB_EFFECTS.register("sunlight_resistance",
            SunlightResistanceEffect::new);

    public static final RegistryObject<MobEffect> WISTERIA_RESISTANCE = MOB_EFFECTS.register("wisteria_resistance",
            WisteriaResistanceEffect::new);

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}
