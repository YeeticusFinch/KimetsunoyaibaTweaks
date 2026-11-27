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

    /** Fields exposed by the dedicated in-game tentacle editor. Values are expressed in config units. */
    public enum EditorField {
        FLESH_WIDTH("Flesh width", 0.001D, 2.0D),
        BONE_WIDTH("Bone width", 0.001D, 2.0D),
        BONE_LENGTH("Bone length", 0.01D, 8.0D),
        SPIKE_LENGTH("Spike length", 0.01D, 4.0D),
        SPIKE_WIDTH("Spike width", 0.001D, 2.0D),
        SPIKE_DEPTH("Spike depth", 0.001D, 2.0D),
        ANCHOR_STRAY_RADIUS("Anchor stray radius", 0.0D, 32.0D),
        FACE_INWARD_WEIGHT("Blockface inward weight", -4.0D, 4.0D),
        FACE_TOWARD_USER_WEIGHT("Blockface toward-user weight", -4.0D, 4.0D),
        START_LEFT("Start left", -64.0D, 64.0D),
        START_UP("Start up", -64.0D, 64.0D),
        START_FORWARD("Start forward", -64.0D, 64.0D),
        START_PITCH("Start pitch", -180.0D, 180.0D),
        START_YAW("Start yaw", -180.0D, 180.0D),
        START_ROLL("Start roll", -180.0D, 180.0D),
        TARGET_LEFT("Default left", -64.0D, 64.0D),
        TARGET_UP("Default up", -64.0D, 64.0D),
        TARGET_FORWARD("Default forward", -64.0D, 64.0D),
        TARGET_PITCH("Default pitch", -180.0D, 180.0D),
        TARGET_YAW("Default yaw", -180.0D, 180.0D),
        TARGET_ROLL("Default roll", -180.0D, 180.0D),
        ANCHOR_LEFT("Anchor left", -64.0D, 64.0D),
        ANCHOR_UP("Anchor up", -64.0D, 64.0D),
        ANCHOR_FORWARD("Anchor forward", -64.0D, 64.0D),
        ANCHOR_RADIUS("Anchor radius", 0.0D, 32.0D),
        ARC_LEFT("Arc left", -64.0D, 64.0D),
        ARC_UP("Arc up", -64.0D, 64.0D),
        ARC_FORWARD("Arc forward", -64.0D, 64.0D);

        private final String label;
        private final double minimum;
        private final double maximum;

        EditorField(String label, double minimum, double maximum) {
            this.label = label;
            this.minimum = minimum;
            this.maximum = maximum;
        }

        public String label() {
            return label;
        }

        public double minimum() {
            return minimum;
        }

        public double maximum() {
            return maximum;
        }

        public boolean isShared() {
            return ordinal() <= FACE_TOWARD_USER_WEIGHT.ordinal();
        }

        public boolean isAngle() {
            return name().endsWith("PITCH") || name().endsWith("YAW") || name().endsWith("ROLL");
        }
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

    public static double editorValue(EditorField field, int arm) {
        return editorSpec(field, arm).get();
    }

    public static double editorDefault(EditorField field, int arm) {
        return editorSpec(field, arm).getDefault();
    }

    public static void setEditorValue(EditorField field, int arm, double value) {
        if (!Double.isFinite(value) || value < field.minimum() || value > field.maximum()) {
            throw new IllegalArgumentException("Value outside range for " + field.label());
        }
        editorSpec(field, arm).set(value);
        Config.SPEC.save();
        load();
    }

    public static void resetEditorValues() {
        for (EditorField field : EditorField.values()) {
            if (field.isShared()) {
                editorSpec(field, -1).set(editorSpec(field, -1).getDefault());
            } else {
                for (int arm = 0; arm < ARM_COUNT; arm++) {
                    editorSpec(field, arm).set(editorSpec(field, arm).getDefault());
                }
            }
        }
        Config.SPEC.save();
        load();
    }

    private static ForgeConfigSpec.DoubleValue editorSpec(EditorField field, int arm) {
        if (field.isShared()) {
            if (arm != -1) {
                throw new IllegalArgumentException("Shared fields do not accept an arm index");
            }
            return switch (field) {
                case FLESH_WIDTH -> fleshWidthSpec;
                case BONE_WIDTH -> boneWidthSpec;
                case BONE_LENGTH -> boneLengthSpec;
                case SPIKE_LENGTH -> spikeLengthSpec;
                case SPIKE_WIDTH -> spikeWidthSpec;
                case SPIKE_DEPTH -> spikeDepthSpec;
                case ANCHOR_STRAY_RADIUS -> anchorStrayRadiusSpec;
                case FACE_INWARD_WEIGHT -> faceInwardWeightSpec;
                case FACE_TOWARD_USER_WEIGHT -> faceTowardUserWeightSpec;
                default -> throw new IllegalArgumentException("Not a shared field: " + field);
            };
        }
        if (arm < 0 || arm >= ARM_COUNT) {
            throw new IllegalArgumentException("Invalid arm index: " + arm);
        }
        return switch (field) {
            case START_LEFT -> startLeft[arm];
            case START_UP -> startUp[arm];
            case START_FORWARD -> startForward[arm];
            case START_PITCH -> startPitch[arm];
            case START_YAW -> startYaw[arm];
            case START_ROLL -> startRoll[arm];
            case TARGET_LEFT -> targetLeft[arm];
            case TARGET_UP -> targetUp[arm];
            case TARGET_FORWARD -> targetForward[arm];
            case TARGET_PITCH -> targetPitch[arm];
            case TARGET_YAW -> targetYaw[arm];
            case TARGET_ROLL -> targetRoll[arm];
            case ANCHOR_LEFT -> anchorLeft[arm];
            case ANCHOR_UP -> anchorUp[arm];
            case ANCHOR_FORWARD -> anchorForward[arm];
            case ANCHOR_RADIUS -> anchorRadius[arm];
            case ARC_LEFT -> arcLeft[arm];
            case ARC_UP -> arcUp[arm];
            case ARC_FORWARD -> arcForward[arm];
            default -> throw new IllegalArgumentException("Not an arm field: " + field);
        };
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
