package com.lerdorf.kimetsunoyaibamultiplayer;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue LOG_DEBUG = BUILDER
            .comment("Enable debug logging to console for animation sync debugging. Shows detailed information about animation detection, packet sending/receiving, and sync status.")
            .define("log-debug", false);

    private static final ForgeConfigSpec.BooleanValue ON_SCREEN_DEBUG = BUILDER
            .comment("Enable on-screen debug messages for animation sync. Shows colored chat messages when animations are detected, sent, or received from other players.")
            .define("on-screen-debug", false);

    private static final ForgeConfigSpec.BooleanValue SWORD_PARTICLES_ENABLED = BUILDER
            .comment("Enable particle effects for Kimetsunoyaiba sword swings. Particles will appear at the sword tip during animations.")
            .define("sword-particles-enabled", true);

    private static final ForgeConfigSpec.BooleanValue SWORD_PARTICLES_FOR_OTHER_ENTITIES = BUILDER
            .comment("Enable sword particle effects for other biped-based entities that perform animations (only if they are close enough to the player for particles to render).")
            .define("sword-particles-for-other-entities", true);

    private static final ForgeConfigSpec.EnumValue<ParticleTriggerMode> PARTICLE_TRIGGER_MODE = BUILDER
            .comment("When to spawn sword particles: ATTACK_ONLY = only during normal attacks (left click), ALL_ANIMATIONS = during any kimetsunoyaiba animation including special moves")
            .defineEnum("particle-trigger-mode", ParticleTriggerMode.ALL_ANIMATIONS);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean logDebug;
    public static boolean onScreenDebug;
    public static boolean swordParticlesEnabled;
    public static boolean swordParticlesForOtherEntities;
    public static ParticleTriggerMode particleTriggerMode;

    public enum ParticleTriggerMode {
        ATTACK_ONLY,
        ALL_ANIMATIONS
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        logDebug = LOG_DEBUG.get();
        onScreenDebug = ON_SCREEN_DEBUG.get();
        swordParticlesEnabled = SWORD_PARTICLES_ENABLED.get();
        swordParticlesForOtherEntities = SWORD_PARTICLES_FOR_OTHER_ENTITIES.get();
        particleTriggerMode = PARTICLE_TRIGGER_MODE.get();
    }
}
