package com.enxv.aerouniversaljoint;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config, written to {@code config/aeronautics_utility_objects-common.toml}.
 * Edit the file directly; changes are picked up when the file is saved, no restart needed.
 */
public final class AeroUniversalJointConfig {
    public static final double DEFAULT_JOINT_STRESS_IMPACT = 2.0D;

    public static final double DEFAULT_ANDESITE_JOINT_LINK_RANGE = 15.0D;
    public static final double DEFAULT_ANDESITE_JOINT_SOFT_RANGE = 1.0D;
    public static final double DEFAULT_ANDESITE_JOINT_DISCONNECT_RANGE = 2.5D;
    public static final double DEFAULT_ANDESITE_JOINT_PULL_STIFFNESS = 1320.0D;
    public static final double DEFAULT_ANDESITE_JOINT_PULL_DAMPING = 18.0D;

    public static final double DEFAULT_BRASS_JOINT_LINK_RANGE = 5.0D;
    public static final double DEFAULT_BRASS_JOINT_SOFT_RANGE = 4.0D;
    public static final double DEFAULT_BRASS_JOINT_DISCONNECT_RANGE = 5.5D;
    public static final double DEFAULT_BRASS_JOINT_PULL_STIFFNESS = 12.0D;
    public static final double DEFAULT_BRASS_JOINT_PULL_CURVE = 4.0D;
    public static final double DEFAULT_BRASS_JOINT_PULL_DAMPING = 7.0D;
    public static final double DEFAULT_BRASS_JOINT_PULL_DAMPING_GAIN = 3.0D;
    public static final double DEFAULT_BRASS_JOINT_ENDPOINT_PULL_MULTIPLIER = 4.0D;

    private static final double MIN_JOINT_RANGE_GAP = 0.01D;

    public static final Common COMMON;
    public static final ModConfigSpec COMMON_SPEC;

    static {
        var pair = new ModConfigSpec.Builder().configure(Common::new);
        COMMON = pair.getLeft();
        COMMON_SPEC = pair.getRight();
    }

    private AeroUniversalJointConfig() {
    }

    public static double jointStressImpact() {
        return getDouble(COMMON.jointStressImpact, DEFAULT_JOINT_STRESS_IMPACT);
    }

    public static double andesiteJointLinkRange() {
        return getDouble(COMMON.andesiteJointLinkRange, DEFAULT_ANDESITE_JOINT_LINK_RANGE);
    }

    public static double andesiteJointSoftRange() {
        return getDouble(COMMON.andesiteJointSoftRange, DEFAULT_ANDESITE_JOINT_SOFT_RANGE);
    }

    public static double andesiteJointDisconnectRange() {
        return Math.max(andesiteJointSoftRange() + MIN_JOINT_RANGE_GAP,
                getDouble(COMMON.andesiteJointDisconnectRange, DEFAULT_ANDESITE_JOINT_DISCONNECT_RANGE));
    }

    public static double andesiteJointPullStiffness() {
        return getDouble(COMMON.andesiteJointPullStiffness, DEFAULT_ANDESITE_JOINT_PULL_STIFFNESS);
    }

    public static double andesiteJointPullDamping() {
        return getDouble(COMMON.andesiteJointPullDamping, DEFAULT_ANDESITE_JOINT_PULL_DAMPING);
    }

    public static double brassJointLinkRange() {
        return getDouble(COMMON.brassJointLinkRange, DEFAULT_BRASS_JOINT_LINK_RANGE);
    }

    public static double brassJointSoftRange() {
        return getDouble(COMMON.brassJointSoftRange, DEFAULT_BRASS_JOINT_SOFT_RANGE);
    }

    public static double brassJointDisconnectRange() {
        return Math.max(brassJointSoftRange() + MIN_JOINT_RANGE_GAP,
                getDouble(COMMON.brassJointDisconnectRange, DEFAULT_BRASS_JOINT_DISCONNECT_RANGE));
    }

    public static double brassJointPullStiffness() {
        return getDouble(COMMON.brassJointPullStiffness, DEFAULT_BRASS_JOINT_PULL_STIFFNESS);
    }

    public static double brassJointPullCurve() {
        return getDouble(COMMON.brassJointPullCurve, DEFAULT_BRASS_JOINT_PULL_CURVE);
    }

    public static double brassJointPullDamping() {
        return getDouble(COMMON.brassJointPullDamping, DEFAULT_BRASS_JOINT_PULL_DAMPING);
    }

    public static double brassJointPullDampingGain() {
        return getDouble(COMMON.brassJointPullDampingGain, DEFAULT_BRASS_JOINT_PULL_DAMPING_GAIN);
    }

    public static double brassJointEndpointPullMultiplier() {
        return getDouble(COMMON.brassJointEndpointPullMultiplier, DEFAULT_BRASS_JOINT_ENDPOINT_PULL_MULTIPLIER);
    }

    private static double getDouble(ModConfigSpec.DoubleValue value, double fallback) {
        try {
            double result = value.get();
            return Double.isFinite(result) ? result : fallback;
        } catch (IllegalStateException | NullPointerException ignored) {
            return fallback;
        }
    }

    public static final class Common {
        final ModConfigSpec.DoubleValue jointStressImpact;

        final ModConfigSpec.DoubleValue andesiteJointLinkRange;
        final ModConfigSpec.DoubleValue andesiteJointSoftRange;
        final ModConfigSpec.DoubleValue andesiteJointDisconnectRange;
        final ModConfigSpec.DoubleValue andesiteJointPullStiffness;
        final ModConfigSpec.DoubleValue andesiteJointPullDamping;

        final ModConfigSpec.DoubleValue brassJointLinkRange;
        final ModConfigSpec.DoubleValue brassJointSoftRange;
        final ModConfigSpec.DoubleValue brassJointDisconnectRange;
        final ModConfigSpec.DoubleValue brassJointPullStiffness;
        final ModConfigSpec.DoubleValue brassJointPullCurve;
        final ModConfigSpec.DoubleValue brassJointPullDamping;
        final ModConfigSpec.DoubleValue brassJointPullDampingGain;
        final ModConfigSpec.DoubleValue brassJointEndpointPullMultiplier;

        Common(ModConfigSpec.Builder builder) {
            builder.push("universal_joint");

            this.jointStressImpact = builder
                    .comment("Stress cost of each universal joint head, in stress units per RPM (like Create's own",
                            "stress_impact values). A linked pair counts both heads. Set to 0 for no stress cost.")
                    .defineInRange("stress_impact", DEFAULT_JOINT_STRESS_IMPACT, 0.0D, 1024.0D);

            builder.push("andesite");
            this.andesiteJointLinkRange = builder
                    .comment("Maximum distance for creating an andesite universal joint link.")
                    .defineInRange("link_range", DEFAULT_ANDESITE_JOINT_LINK_RANGE, 0.5D, 128.0D);
            this.andesiteJointSoftRange = builder
                    .comment("Allowed stretch or compression from rest length before andesite elastic pull starts.")
                    .defineInRange("soft_range", DEFAULT_ANDESITE_JOINT_SOFT_RANGE, 0.0D, 128.0D);
            this.andesiteJointDisconnectRange = builder
                    .comment("Stretch or compression from rest length that breaks an andesite joint link.")
                    .defineInRange("disconnect_range", DEFAULT_ANDESITE_JOINT_DISCONNECT_RANGE, 0.01D, 128.0D);
            this.andesiteJointPullStiffness = builder
                    .comment("Elastic pull stiffness for andesite joint links beyond the soft range.")
                    .defineInRange("pull_stiffness", DEFAULT_ANDESITE_JOINT_PULL_STIFFNESS, 0.0D, 100000.0D);
            this.andesiteJointPullDamping = builder
                    .comment("Elastic damping for andesite joint links beyond the soft range.")
                    .defineInRange("pull_damping", DEFAULT_ANDESITE_JOINT_PULL_DAMPING, 0.0D, 10000.0D);
            builder.pop();

            builder.push("brass");
            this.brassJointLinkRange = builder
                    .comment("Maximum distance for creating a brass universal joint link.")
                    .defineInRange("link_range", DEFAULT_BRASS_JOINT_LINK_RANGE, 0.5D, 128.0D);
            this.brassJointSoftRange = builder
                    .comment("Distance before brass joint elastic pull starts.")
                    .defineInRange("soft_range", DEFAULT_BRASS_JOINT_SOFT_RANGE, 0.0D, 128.0D);
            this.brassJointDisconnectRange = builder
                    .comment("Distance that breaks a brass joint link.")
                    .defineInRange("disconnect_range", DEFAULT_BRASS_JOINT_DISCONNECT_RANGE, 0.01D, 128.0D);
            this.brassJointPullStiffness = builder
                    .comment("Elastic pull stiffness for brass joint links beyond the soft range.")
                    .defineInRange("pull_stiffness", DEFAULT_BRASS_JOINT_PULL_STIFFNESS, 0.0D, 100000.0D);
            this.brassJointPullCurve = builder
                    .comment("Exponential pull curve for brass joint links beyond the soft range.")
                    .defineInRange("pull_curve", DEFAULT_BRASS_JOINT_PULL_CURVE, 0.0D, 32.0D);
            this.brassJointPullDamping = builder
                    .comment("Base damping for brass joint links beyond the soft range.")
                    .defineInRange("pull_damping", DEFAULT_BRASS_JOINT_PULL_DAMPING, 0.0D, 10000.0D);
            this.brassJointPullDampingGain = builder
                    .comment("Additional damping per block of brass joint overshoot.")
                    .defineInRange("pull_damping_gain", DEFAULT_BRASS_JOINT_PULL_DAMPING_GAIN, 0.0D, 10000.0D);
            this.brassJointEndpointPullMultiplier = builder
                    .comment("Extra pull multiplier as brass links approach disconnect range.")
                    .defineInRange("endpoint_pull_multiplier", DEFAULT_BRASS_JOINT_ENDPOINT_PULL_MULTIPLIER, 1.0D, 128.0D);
            builder.pop();

            builder.pop();
        }
    }
}
