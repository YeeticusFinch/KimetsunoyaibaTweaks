package com.lerdorf.kimetsunoyaibamultiplayer.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;

public class ParticleConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    static {
        BUILDER.comment("Sword Particle Configuration")
                .push("particles");
    }

    // Particle enabling/disabling
    private static final ForgeConfigSpec.BooleanValue SWORD_PARTICLES_ENABLED = BUILDER
            .comment("Enable sword particle effects during kimetsunoyaiba animations")
            .define("sword-particles-enabled", true);

    private static final ForgeConfigSpec.BooleanValue SWORD_PARTICLES_FOR_OTHER_ENTITIES = BUILDER
            .comment("Enable sword particle effects for other biped-based entities that perform animations (only if they are close enough to the player for particles to render).")
            .define("sword-particles-for-other-entities", true);

    private static final ForgeConfigSpec.EnumValue<ParticleTriggerMode> PARTICLE_TRIGGER_MODE = BUILDER
            .comment("When to spawn sword particles: ATTACK_ONLY = only during normal attacks (left click), ALL_ANIMATIONS = during any kimetsunoyaiba animation including special moves")
            .defineEnum("particle-trigger-mode", ParticleTriggerMode.ALL_ANIMATIONS);

    // Animation speed and timing
    static {
        BUILDER.comment("Animation Timing Configuration")
                .push("animation");
    }

    private static final ForgeConfigSpec.DoubleValue PARTICLE_ANGLE_INCREMENT = BUILDER
            .comment("Angle increment in degrees per step during particle arc animation")
            .defineInRange("angle-increment", 4.0, 1.0, 15.0);

    private static final ForgeConfigSpec.IntValue PARTICLE_STEPS_PER_TICK = BUILDER
            .comment("Number of angle increments to process per tick (higher = faster animation)")
            .defineInRange("steps-per-tick", 6, 1, 12);

    private static final ForgeConfigSpec.DoubleValue PARTICLE_ARC_DEGREES = BUILDER
            .comment("Total arc length in degrees for sword swing animations")
            .defineInRange("arc-degrees", 120.0, 60.0, 180.0);

    static {
        BUILDER.pop(); // animation
    }

    // Visual appearance
    static {
        BUILDER.comment("Visual Appearance Configuration")
                .push("appearance");
    }

    private static final ForgeConfigSpec.IntValue RADIAL_LAYERS = BUILDER
            .comment("Number of radial layers in the particle ribbon (more = thicker)")
            .defineInRange("radial-layers", 5, 3, 10);

    private static final ForgeConfigSpec.DoubleValue BASE_RADIUS = BUILDER
            .comment("Base radius distance from player center")
            .defineInRange("base-radius", 1.8, 1.0, 3.0);

    private static final ForgeConfigSpec.DoubleValue RADIUS_INCREMENT = BUILDER
            .comment("Distance between each radial layer")
            .defineInRange("radius-increment", 0.15, 0.05, 0.5);

    private static final ForgeConfigSpec.IntValue PARTICLES_PER_POSITION = BUILDER
            .comment("Number of particles to spawn at each calculated position")
            .defineInRange("particles-per-position", 2, 1, 5);

    private static final ForgeConfigSpec.IntValue MAX_PARTICLES_PER_TICK = BUILDER
            .comment("Maximum total particles to spawn per tick (0 = unlimited)")
            .defineInRange("max-particles-per-tick", 0, 0, 1000);

    static {
        BUILDER.pop(); // appearance
        BUILDER.pop(); // particles
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    // Cached values
    public static boolean swordParticlesEnabled;
    public static boolean swordParticlesForOtherEntities;
    public static ParticleTriggerMode particleTriggerMode;

    public static double particleAngleIncrement;
    public static int particleStepsPerTick;
    public static double particleArcDegrees;

    public static int radialLayers;
    public static double baseRadius;
    public static double radiusIncrement;
    public static int particlesPerPosition;
    public static int maxParticlesPerTick;

    public enum ParticleTriggerMode {
        ATTACK_ONLY,
        ALL_ANIMATIONS
    }

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event) {
        System.out.println("PARTICLE CONFIG LOADING...");
        swordParticlesEnabled = SWORD_PARTICLES_ENABLED.get();
        swordParticlesForOtherEntities = SWORD_PARTICLES_FOR_OTHER_ENTITIES.get();
        particleTriggerMode = PARTICLE_TRIGGER_MODE.get();

        particleAngleIncrement = PARTICLE_ANGLE_INCREMENT.get();
        particleStepsPerTick = PARTICLE_STEPS_PER_TICK.get();
        particleArcDegrees = PARTICLE_ARC_DEGREES.get();

        radialLayers = RADIAL_LAYERS.get();
        baseRadius = BASE_RADIUS.get();
        radiusIncrement = RADIUS_INCREMENT.get();
        particlesPerPosition = PARTICLES_PER_POSITION.get();
        maxParticlesPerTick = MAX_PARTICLES_PER_TICK.get();

        System.out.println("ParticleConfig loaded: particles=" + swordParticlesEnabled +
                         ", layers=" + radialLayers + ", stepsPerTick=" + particleStepsPerTick +
                         ", maxPerTick=" + maxParticlesPerTick);
    }
}