package com.lerdorf.kimetsunoyaibamultiplayer.config;

import java.util.List;

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
            .defineEnum("particle-trigger-mode", ParticleTriggerMode.ATTACK_ONLY);

    // Animation speed and timing
    static {
        BUILDER.comment("Animation Timing Configuration")
                .push("animation");
    }

    private static final ForgeConfigSpec.DoubleValue PARTICLE_ANGLE_INCREMENT = BUILDER
            .comment("Angle increment in degrees per step during particle arc animation")
            .defineInRange("angle-increment", 10.0, 1.0, 20.0);

    private static final ForgeConfigSpec.IntValue PARTICLE_STEPS_PER_TICK = BUILDER
            .comment("Number of angle increments to process per tick (higher = faster animation)")
            .defineInRange("steps-per-tick", 6, 1, 20);

    private static final ForgeConfigSpec.DoubleValue PARTICLE_ARC_DEGREES = BUILDER
            .comment("Total arc length in degrees for sword swing animations")
            .defineInRange("arc-degrees", 160.0, 60.0, 240.0);

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
            .defineInRange("base-radius", 2.8, 1.0, 6.0);

    private static final ForgeConfigSpec.DoubleValue RADIUS_INCREMENT = BUILDER
            .comment("Distance between each radial layer")
            .defineInRange("radius-increment", 0.2, 0.05, 0.6);

    private static final ForgeConfigSpec.IntValue PARTICLES_PER_POSITION = BUILDER
            .comment("Number of particles to spawn at each calculated position")
            .defineInRange("particles-per-position", 1, 1, 5);

    private static final ForgeConfigSpec.IntValue MAX_PARTICLES_PER_TICK = BUILDER
            .comment("Maximum total particles to spawn per tick (0 = unlimited)")
            .defineInRange("max-particles-per-tick", 0, 0, 1000);

    static {
        BUILDER.pop(); // appearance
    }

    // Particle Mappings
    static {
        BUILDER.comment("Particle Mappings Configuration")
                .push("mappings");
    }

    // Default particle mappings - these can be overridden in config
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> PARTICLE_MAPPINGS = BUILDER
            .comment("Particle mappings in format 'item_id:particle_type[:size:red:green:blue]'",
                    "For dust particles, add size (0.1-2.0) and RGB values (0.0-1.0)",
                    "Examples:",
                    "  'kimetsunoyaiba:nichirinsword_thunder:minecraft:dust:1.2:1.0:1.0:0.2'",
                    "  'kimetsunoyaiba:nichirinsword_water:kimetsunoyaiba:particle_blue_smoke'",
                    "  'minecraft:diamond_sword:minecraft:enchanted_hit'")
            .defineListAllowEmpty("particle-mappings", () -> {
                return java.util.Arrays.asList(
                    "kimetsunoyaiba:nichirinsword_thunder:minecraft:dust:0.8:1.0:1.0:0.2",
                    "kimetsunoyaiba:nichirinsword_water:kimetsunoyaiba:particle_blue_smoke",
                    "kimetsunoyaiba:nichirinsword_flame:minecraft:dust:0.8:1.0:0.8:0.1",
                    "kimetsunoyaiba:nichirinsword_stone:minecraft:dust:0.8:0.6:0.6:0.6",
                    "kimetsunoyaiba:nichirinsword_wind:minecraft:dust:0.8:0.1:1.0:0.1",
                    "kimetsunoyaiba:nichirinsword_sun:minecraft:flame",
                    "kimetsunoyaiba:nichirinswordmoon:minecraft:end_rod", // moon is missing an underscore, not a typo!!!
                    "kimetsunoyaiba:nichirinsword_flower:kimetsunoyaiba:particle_flower",
                    "kimetsunoyaiba:nichirinsword_insect:minecraft:dust:0.6:0.8:1.0:0.9",
                    "kimetsunoyaiba:nichirinsword_sound:kimetsunoyaiba:particle_sound",
                    "kimetsunoyaiba:nichirinsword_love:kimetsunoyaiba:particle_love",
                    "kimetsunoyaiba:nichirinsword_mist:minecraft:dust:1.0:1.0:1.0:1.0",
                    "kimetsunoyaiba:nichirinsword_serpent:minecraft:dust:1.0:1.0:1.0:1.0",
                    "kimetsunoyaiba:nichirinsword_beast:kimetsunoyaiba:particle_beast",
                    "kimetsunoyaiba:nichirinsword_black:minecraft:dust:0.8:1.0:1.0:1.0",
                    "kimetsunoyaiba:nichirinsword_inosuke:minecraft:crit",
                    "kimetsunoyaiba:nichirinsword_basic:minecraft:crit",
                    "kimetsunoyaiba:nichirinsword_generic:minecraft:enchanted_hit",
                    "kimetsunoyaiba:nichirinsword_tanjiro:kimetsunoyaiba:particle_blue_smoke",
                    "kimetsunoyaiba:nichirinsword_tanjiro_2:minecraft:flame",
                    "kimetsunoyaiba:nichirinsword_zenitsu:minecraft:dust:0.8:1.0:1.0:1.0",
                    "kimetsunoyaiba:nichirinsword_kanawo:kimetsunoyaiba:particle_flower",
                    "kimetsunoyaiba:nichirinsword_tomioka:kimetsunoyaiba:particle_blue_smoke",
                    "kimetsunoyaiba:nichirinsword_kocho:minecraft:dust:0.8:1.0:0.9:0.9",
                    "kimetsunoyaiba:nichirinsword_rengoku:minecraft:dust:0.9:1.0:0.8:0.1",
                    "kimetsunoyaiba:nichirinsword_uzui:minecraft:firework",
                    "kimetsunoyaiba:nichirinsword_tokito:minecraft:dust:1.0:1.0:1.0:1.0",
                    "kimetsunoyaiba:nichirinsword_kanroji:minecraft:dust:0.9:1.0:0.9:0.9",
                    "kimetsunoyaiba:nichirinsword_iguro:minecraft:dust:1.0:1.0:1.0:1.0",
                    "kimetsunoyaiba:nichirinsword_shinazugawa:minecraft:dust:0.8:0.1:1.0:0.1",
                    "kimetsunoyaiba:nichirinsword_kanae:minecraft:dust:0.7:1.0:0.9:0.9",
                    "kimetsunoyaiba:nichirinsword_yoriichi:minecraft:flame"
                    
                );
            }, obj -> obj instanceof String);

    static {
        BUILDER.pop(); // mappings
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

    public static java.util.Map<String, ParticleMapping> particleMappings;

    public enum ParticleTriggerMode {
        ATTACK_ONLY,
        ALL_ANIMATIONS
    }

    public static class ParticleMapping {
        public final String itemId;
        public final String particleType;
        public final float size;
        public final float red;
        public final float green;
        public final float blue;
        public final boolean isDust;

        public ParticleMapping(String itemId, String particleType, float size, float red, float green, float blue) {
            this.itemId = itemId;
            this.particleType = particleType;
            this.size = size;
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.isDust = particleType.equals("minecraft:dust");
        }

        public ParticleMapping(String itemId, String particleType) {
            this(itemId, particleType, 1.0f, 1.0f, 1.0f, 1.0f);
        }
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

        // Parse particle mappings
        particleMappings = new java.util.HashMap<>();
        List<? extends String> mappingStrings = PARTICLE_MAPPINGS.get();
        for (String mapping : mappingStrings) {
            try {
                System.out.println("Parsing mapping: " + mapping);
                ParticleMapping parsed = parseParticleMapping(mapping);
                if (parsed != null) {
                    particleMappings.put(parsed.itemId, parsed);
                    System.out.println("Successfully added mapping: " + parsed.itemId + " -> " + parsed.particleType);
                } else {
                    System.err.println("Failed to parse mapping (returned null): " + mapping);
                }
            } catch (Exception e) {
                System.err.println("Failed to parse particle mapping: " + mapping + " - " + e.getMessage());
            }
        }

        System.out.println("ParticleConfig loaded: particles=" + swordParticlesEnabled +
                         ", layers=" + radialLayers + ", stepsPerTick=" + particleStepsPerTick +
                         ", maxPerTick=" + maxParticlesPerTick + ", mappings=" + particleMappings.size());
    }

    private static ParticleMapping parseParticleMapping(String mapping) {
        String[] parts = mapping.split(":");
        if (parts.length < 4) {
            System.err.println("Invalid particle mapping format (need at least 4 parts): " + mapping);
            return null;
        }

        String itemId = parts[0] + ":" + parts[1];
        String particleType = parts[2] + ":" + parts[3];

        if (parts.length >= 8 && particleType.equals("minecraft:dust")) {
            // Dust particle with size and color
            try {
                float size = Float.parseFloat(parts[4]);
                float red = Float.parseFloat(parts[5]);
                float green = Float.parseFloat(parts[6]);
                float blue = Float.parseFloat(parts[7]);
                return new ParticleMapping(itemId, particleType, size, red, green, blue);
            } catch (NumberFormatException e) {
                System.err.println("Invalid dust particle parameters in mapping: " + mapping);
                return new ParticleMapping(itemId, particleType);
            }
        } else {
            // Regular particle
            return new ParticleMapping(itemId, particleType);
        }
    }
}