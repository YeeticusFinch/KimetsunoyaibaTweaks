package com.lerdorf.kimetsunoyaibamultiplayer;

import net.minecraftforge.common.ForgeConfigSpec;

/** Configurable geometry and movement preferences for the six Biokineses tentacles. */
public final class BiokinesesTentacleConfig {
    public static final int ARM_COUNT = 6;
    public static final String[] ARM_NAMES = {
        "left_upper", "left_lower", "left_combat", "right_upper", "right_lower", "right_combat"
    };

    private static final double[] DEFAULT_START_LEFT = {0.30D, 0.30D, 0.40D, -0.30D, -0.30D, -0.40D};
    private static final double[] DEFAULT_START_UP = {0.34D, -0.18D, 0.18D, 0.34D, -0.18D, 0.18D};
    private static final double[] DEFAULT_START_FORWARD = {0.08D, 0.08D, 0.18D, 0.08D, 0.08D, 0.18D};
    private static final double[] DEFAULT_TARGET_LEFT = {1.35D, 1.35D, 1.15D, -1.35D, -1.35D, -1.15D};
    private static final double[] DEFAULT_TARGET_UP = {1.75D, 0.95D, 1.35D, 1.75D, 0.95D, 1.35D};
    private static final double[] DEFAULT_TARGET_FORWARD = {0.10D, 0.10D, 0.45D, 0.10D, 0.10D, 0.45D};
    private static final double[] DEFAULT_ANCHOR_LEFT = {3.0D, 3.0D, 2.5D, -3.0D, -3.0D, -2.5D};
    private static final double[] DEFAULT_ANCHOR_UP = {3.0D, -2.5D, 0.0D, 3.0D, -2.5D, 0.0D};
    private static final double[] DEFAULT_ANCHOR_FORWARD = {3.0D, 0.0D, 1.5D, 3.0D, 0.0D, 1.5D};

    private static ForgeConfigSpec.DoubleValue fleshWidthSpec;
    private static ForgeConfigSpec.DoubleValue boneWidthSpec;
    private static ForgeConfigSpec.DoubleValue boneLengthSpec;
    private static ForgeConfigSpec.DoubleValue spikeLengthSpec;
    private static ForgeConfigSpec.DoubleValue spikeWidthSpec;
    private static ForgeConfigSpec.DoubleValue spikeDepthSpec;
    private static ForgeConfigSpec.DoubleValue anchorStrayRadiusSpec;
    private static ForgeConfigSpec.DoubleValue faceInwardWeightSpec;
    private static ForgeConfigSpec.DoubleValue faceTowardUserWeightSpec;

    private static final ForgeConfigSpec.DoubleValue[] startLeft = new ForgeConfigSpec.DoubleValue[ARM_COUNT];
    private static final ForgeConfigSpec.DoubleValue[] startUp = new ForgeConfigSpec.DoubleValue[ARM_COUNT];
    private static final ForgeConfigSpec.DoubleValue[] startForward = new ForgeConfigSpec.DoubleValue[ARM_COUNT];
    private static final ForgeConfigSpec.DoubleValue[] startPitch = new ForgeConfigSpec.DoubleValue[ARM_COUNT];
    private static final ForgeConfigSpec.DoubleValue[] startYaw = new ForgeConfigSpec.DoubleValue[ARM_COUNT];
    private static final ForgeConfigSpec.DoubleValue[] startRoll = new ForgeConfigSpec.DoubleValue[ARM_COUNT];
    private static final ForgeConfigSpec.DoubleValue[] targetLeft = new ForgeConfigSpec.DoubleValue[ARM_COUNT];
    private static final ForgeConfigSpec.DoubleValue[] targetUp = new ForgeConfigSpec.DoubleValue[ARM_COUNT];
    private static final ForgeConfigSpec.DoubleValue[] targetForward = new ForgeConfigSpec.DoubleValue[ARM_COUNT];
    private static final ForgeConfigSpec.DoubleValue[] targetPitch = new ForgeConfigSpec.DoubleValue[ARM_COUNT];
    private static final ForgeConfigSpec.DoubleValue[] targetYaw = new ForgeConfigSpec.DoubleValue[ARM_COUNT];
    private static final ForgeConfigSpec.DoubleValue[] targetRoll = new ForgeConfigSpec.DoubleValue[ARM_COUNT];
    private static final ForgeConfigSpec.DoubleValue[] anchorLeft = new ForgeConfigSpec.DoubleValue[ARM_COUNT];
    private static final ForgeConfigSpec.DoubleValue[] anchorUp = new ForgeConfigSpec.DoubleValue[ARM_COUNT];
    private static final ForgeConfigSpec.DoubleValue[] anchorForward = new ForgeConfigSpec.DoubleValue[ARM_COUNT];
    private static final ForgeConfigSpec.DoubleValue[] arcLeft = new ForgeConfigSpec.DoubleValue[ARM_COUNT];
    private static final ForgeConfigSpec.DoubleValue[] arcUp = new ForgeConfigSpec.DoubleValue[ARM_COUNT];
    private static final ForgeConfigSpec.DoubleValue[] arcForward = new ForgeConfigSpec.DoubleValue[ARM_COUNT];
    private static ForgeConfigSpec.DoubleValue[] anchorRadius = new ForgeConfigSpec.DoubleValue[ARM_COUNT];

    public static double fleshWidth;
    public static double boneWidth;
    public static double boneLength;
    public static double spikeLength;
    public static double spikeWidth;
    public static double spikeDepth;
    public static double anchorStrayRadius;
    public static double faceInwardWeight;
    public static double faceTowardUserWeight;
    private static final double[] startLeftValue = new double[ARM_COUNT];
    private static final double[] startUpValue = new double[ARM_COUNT];
    private static final double[] startForwardValue = new double[ARM_COUNT];
    private static final double[] startPitchValue = new double[ARM_COUNT];
    private static final double[] startYawValue = new double[ARM_COUNT];
    private static final double[] startRollValue = new double[ARM_COUNT];
    private static final double[] targetLeftValue = new double[ARM_COUNT];
    private static final double[] targetUpValue = new double[ARM_COUNT];
    private static final double[] targetForwardValue = new double[ARM_COUNT];
    private static final double[] targetPitchValue = new double[ARM_COUNT];
    private static final double[] targetYawValue = new double[ARM_COUNT];
    private static final double[] targetRollValue = new double[ARM_COUNT];
    private static final double[] anchorLeftValue = new double[ARM_COUNT];
    private static final double[] anchorUpValue = new double[ARM_COUNT];
    private static final double[] anchorForwardValue = new double[ARM_COUNT];
    private static final double[] anchorRadiusValue = new double[ARM_COUNT];
    private static final double[] arcLeftValue = new double[ARM_COUNT];
    private static final double[] arcUpValue = new double[ARM_COUNT];
    private static final double[] arcForwardValue = new double[ARM_COUNT];

    private BiokinesesTentacleConfig() {
    }

    public static void init(ForgeConfigSpec.Builder builder) {
        builder.push("biokineses-tentacles");
        fleshWidthSpec = builder.comment("Full width of the thin red flesh section in blocks")
            .defineInRange("flesh-width", 0.08D, 0.001D, 2.0D);
        boneWidthSpec = builder.comment("Full width of the bone section in blocks")
            .defineInRange("bone-width", 0.105D, 0.001D, 2.0D);
        boneLengthSpec = builder.comment("Length of the bone section in blocks")
            .defineInRange("bone-length", 0.70D, 0.01D, 8.0D);
        spikeLengthSpec = builder.comment("Length of each spike in blocks")
            .defineInRange("spike-length", 0.48D, 0.01D, 4.0D);
        spikeWidthSpec = builder.comment("Full width of each spike in blocks")
            .defineInRange("spike-width", 0.14D, 0.001D, 2.0D);
        spikeDepthSpec = builder.comment("Full depth of each spike in blocks")
            .defineInRange("spike-depth", 0.08D, 0.001D, 2.0D);
        anchorStrayRadiusSpec = builder.comment("Fallback radius for climbing anchor preference regions")
            .defineInRange("anchor-stray-radius", 3.0D, 0.0D, 32.0D);
        faceInwardWeightSpec = builder.comment("Weight of the direction into the contacted block face")
            .defineInRange("blockface-inward-weight", 1.0D, -4.0D, 4.0D);
        faceTowardUserWeightSpec = builder.comment("Weight of the direction from the contacted face toward the user")
            .defineInRange("blockface-toward-user-weight", 0.36D, -4.0D, 4.0D);

        for (int arm = 0; arm < ARM_COUNT; arm++) {
            builder.push(ARM_NAMES[arm]);
            startLeft[arm] = offset(builder, "start-left", DEFAULT_START_LEFT[arm],
                "Torso-relative start offset toward the user's left");
            startUp[arm] = offset(builder, "start-up", DEFAULT_START_UP[arm],
                "Torso-relative start offset upward");
            startForward[arm] = offset(builder, "start-forward", DEFAULT_START_FORWARD[arm],
                "Torso-relative start offset forward");
            startPitch[arm] = angle(builder, "start-pitch", 0.0D, "Start rotation around the torso left axis");
            startYaw[arm] = angle(builder, "start-yaw", 0.0D, "Start rotation around the torso up axis");
            startRoll[arm] = angle(builder, "start-roll", 0.0D, "Start rotation around the torso forward axis");

            targetLeft[arm] = offset(builder, "default-left", DEFAULT_TARGET_LEFT[arm],
                "Default endpoint offset toward the user's left");
            targetUp[arm] = offset(builder, "default-up", DEFAULT_TARGET_UP[arm],
                "Default endpoint offset upward");
            targetForward[arm] = offset(builder, "default-forward", DEFAULT_TARGET_FORWARD[arm],
                "Default endpoint offset forward");
            targetPitch[arm] = angle(builder, "default-pitch", 0.0D, "Default endpoint rotation around the torso left axis");
            targetYaw[arm] = angle(builder, "default-yaw", 0.0D, "Default endpoint rotation around the torso up axis");
            targetRoll[arm] = angle(builder, "default-roll", 0.0D, "Default endpoint rotation around the torso forward axis");

            anchorLeft[arm] = offset(builder, "anchor-left", DEFAULT_ANCHOR_LEFT[arm],
                "Preferred climbing anchor offset toward the user's left");
            anchorUp[arm] = offset(builder, "anchor-up", DEFAULT_ANCHOR_UP[arm],
                "Preferred climbing anchor offset upward");
            anchorForward[arm] = offset(builder, "anchor-forward", DEFAULT_ANCHOR_FORWARD[arm],
                "Preferred climbing anchor offset forward");
            anchorRadius[arm] = builder.comment("Maximum preferred-region stray radius in blocks")
                .defineInRange("anchor-radius", 3.0D, 0.0D, 32.0D);

            arcLeft[arm] = offset(builder, "arc-left", 0.0D, "Visual arc vector toward the user's left");
            arcUp[arm] = offset(builder, "arc-up", 0.0D, "Visual arc vector upward");
            arcForward[arm] = offset(builder, "arc-forward", 0.0D, "Visual arc vector forward");
            builder.pop();
        }
        builder.pop();
    }

    private static ForgeConfigSpec.DoubleValue offset(ForgeConfigSpec.Builder builder, String name,
                                                        double defaultValue, String comment) {
        return builder.comment(comment).defineInRange(name, defaultValue, -64.0D, 64.0D);
    }

    private static ForgeConfigSpec.DoubleValue angle(ForgeConfigSpec.Builder builder, String name,
                                                       double defaultValue, String comment) {
        return builder.comment(comment).defineInRange(name, defaultValue, -180.0D, 180.0D);
    }

    public static void load() {
        fleshWidth = fleshWidthSpec.get();
        boneWidth = boneWidthSpec.get();
        boneLength = boneLengthSpec.get();
        spikeLength = spikeLengthSpec.get();
        spikeWidth = spikeWidthSpec.get();
        spikeDepth = spikeDepthSpec.get();
        anchorStrayRadius = anchorStrayRadiusSpec.get();
        faceInwardWeight = faceInwardWeightSpec.get();
        faceTowardUserWeight = faceTowardUserWeightSpec.get();
        for (int arm = 0; arm < ARM_COUNT; arm++) {
            startLeftValue[arm] = startLeft[arm].get();
            startUpValue[arm] = startUp[arm].get();
            startForwardValue[arm] = startForward[arm].get();
            startPitchValue[arm] = Math.toRadians(startPitch[arm].get());
            startYawValue[arm] = Math.toRadians(startYaw[arm].get());
            startRollValue[arm] = Math.toRadians(startRoll[arm].get());
            targetLeftValue[arm] = targetLeft[arm].get();
            targetUpValue[arm] = targetUp[arm].get();
            targetForwardValue[arm] = targetForward[arm].get();
            targetPitchValue[arm] = Math.toRadians(targetPitch[arm].get());
            targetYawValue[arm] = Math.toRadians(targetYaw[arm].get());
            targetRollValue[arm] = Math.toRadians(targetRoll[arm].get());
            anchorLeftValue[arm] = anchorLeft[arm].get();
            anchorUpValue[arm] = anchorUp[arm].get();
            anchorForwardValue[arm] = anchorForward[arm].get();
            anchorRadiusValue[arm] = anchorRadius[arm].get();
            arcLeftValue[arm] = arcLeft[arm].get();
            arcUpValue[arm] = arcUp[arm].get();
            arcForwardValue[arm] = arcForward[arm].get();
        }
    }

    public static double startLeft(int arm) { return startLeftValue[arm]; }
    public static double startUp(int arm) { return startUpValue[arm]; }
    public static double startForward(int arm) { return startForwardValue[arm]; }
    public static double startPitch(int arm) { return startPitchValue[arm]; }
    public static double startYaw(int arm) { return startYawValue[arm]; }
    public static double startRoll(int arm) { return startRollValue[arm]; }
    public static double targetLeft(int arm) { return targetLeftValue[arm]; }
    public static double targetUp(int arm) { return targetUpValue[arm]; }
    public static double targetForward(int arm) { return targetForwardValue[arm]; }
    public static double targetPitch(int arm) { return targetPitchValue[arm]; }
    public static double targetYaw(int arm) { return targetYawValue[arm]; }
    public static double targetRoll(int arm) { return targetRollValue[arm]; }
    public static double anchorLeft(int arm) { return anchorLeftValue[arm]; }
    public static double anchorUp(int arm) { return anchorUpValue[arm]; }
    public static double anchorForward(int arm) { return anchorForwardValue[arm]; }
    public static double anchorRadius(int arm) { return anchorRadiusValue[arm]; }
    public static double arcLeft(int arm) { return arcLeftValue[arm]; }
    public static double arcUp(int arm) { return arcUpValue[arm]; }
    public static double arcForward(int arm) { return arcForwardValue[arm]; }
}
