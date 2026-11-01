package com.enxv.aerouniversaljoint.content;

import net.minecraft.world.phys.Vec3;

/** World-space rod geometry shared by client selection and server interaction validation. */
public record HydraulicRodGeometry(Vec3 start, Vec3 end, Vec3 rollReference, double radius, boolean giant) {
    private static final double PIXEL = 1.0D / 16.0D;

    public double intersect(Vec3 origin, Vec3 direction, double maxDistance) {
        double distance = giant ? giantHitDistance(origin, direction, this)
                : prismHitDistance(origin, direction, this, 0.0D, end.subtract(start).length(),
                        radius, 4, Math.PI / 4.0D);
        return distance < maxDistance ? distance : Double.POSITIVE_INFINITY;
    }

    private static double giantHitDistance(Vec3 origin, Vec3 direction, HydraulicRodGeometry candidate) {
        Vec3 delta = candidate.end().subtract(candidate.start());
        double distance = delta.length();
        if (distance < 1.0E-6D) return Double.POSITIVE_INFINITY;
        Vec3 axis = delta.scale(1.0D / distance);
        Vec3 basisX = candidate.rollReference().subtract(axis.scale(candidate.rollReference().dot(axis)));
        if (basisX.lengthSqr() < 1.0E-6D) {
            basisX = Math.abs(axis.y) < 0.9D ? new Vec3(0.0D, 1.0D, 0.0D) : new Vec3(1.0D, 0.0D, 0.0D);
            basisX = basisX.subtract(axis.scale(basisX.dot(axis)));
        }
        basisX = basisX.normalize();
        Vec3 basisZ = axis.cross(basisX).normalize();
        double best = Double.POSITIVE_INFINITY;
        double barrelRadius = Math.hypot(6.5D, 2.69239D) * PIXEL;
        double barrelPhase = Math.atan2(2.69239D, 6.5D);
        best = Math.min(best, prismHitDistance(origin, direction, candidate, 0.9D * PIXEL,
                Math.min(distance, 19.9D * PIXEL), barrelRadius, 8, barrelPhase, axis, basisX, basisZ));
        double collarRadius = Math.hypot(7.0D, 2.89949D) * PIXEL;
        double collarPhase = Math.atan2(2.89949D, 7.0D);
        best = Math.min(best, prismHitDistance(origin, direction, candidate, 19.9D * PIXEL,
                Math.min(distance, 23.1D * PIXEL), collarRadius, 8, collarPhase, axis, basisX, basisZ));
        best = Math.min(best, prismHitDistance(origin, direction, candidate, 23.1D * PIXEL,
                Math.min(distance, 24.9D * PIXEL), barrelRadius, 8, barrelPhase, axis, basisX, basisZ));
        best = Math.min(best, prismHitDistance(origin, direction, candidate, 24.9D * PIXEL,
                Math.min(distance, 28.1D * PIXEL), collarRadius, 8, collarPhase, axis, basisX, basisZ));
        best = Math.min(best, prismHitDistance(origin, direction, candidate, 28.1D * PIXEL,
                Math.min(distance, 34.1D * PIXEL), barrelRadius, 8, barrelPhase, axis, basisX, basisZ));
        double noseRadius = Math.hypot(5.0D, 2.07107D) * PIXEL;
        double nosePhase = Math.atan2(2.07107D, 5.0D);
        best = Math.min(best, prismHitDistance(origin, direction, candidate, 34.1D * PIXEL,
                Math.min(distance, 37.1D * PIXEL), noseRadius, 8, nosePhase, axis, basisX, basisZ));
        best = Math.min(best, prismHitDistance(origin, direction, candidate, 37.1D * PIXEL,
                Math.min(distance, 37.9D * PIXEL), Math.hypot(4.0D, 1.65685D) * PIXEL,
                8, Math.atan2(1.65685D, 4.0D), axis, basisX, basisZ));
        best = Math.min(best, prismHitDistance(origin, direction, candidate, 37.9D * PIXEL,
                Math.min(distance, 39.1D * PIXEL), noseRadius,
                8, nosePhase, axis, basisX, basisZ));
        best = Math.min(best, prismHitDistance(origin, direction, candidate, 39.1D * PIXEL,
                Math.min(distance, 40.0D * PIXEL), 3.0D * Math.sqrt(2.0D) * PIXEL,
                4, Math.PI / 4.0D, axis, basisX, basisZ));
        best = Math.min(best, prismHitDistance(origin, direction, candidate, 0.0D,
                Math.min(distance, 0.9D * PIXEL), Math.hypot(5.5D, 2.27817D) * PIXEL,
                8, Math.atan2(2.27817D, 5.5D), axis, basisX, basisZ));
        GiantHydraulicRodVisualState visual = GiantHydraulicRodVisualState.fromDistance(distance);
        double movingLength = GiantHydraulicRodVisualState.NOMINAL_ROD_LENGTH * visual.continuousScale();
        best = Math.min(best, prismHitDistance(origin, direction, candidate,
                visual.thickOffset(), Math.min(distance, visual.thickOffset() + movingLength),
                2.5D * Math.sqrt(2.0D) / 16.0D, 4, Math.PI / 4.0D, axis, basisX, basisZ));
        best = Math.min(best, prismHitDistance(origin, direction, candidate,
                visual.mediumOffset(), Math.min(distance, visual.mediumOffset() + movingLength),
                2.0D * Math.sqrt(2.0D) / 16.0D, 4, Math.PI / 4.0D, axis, basisX, basisZ));
        best = Math.min(best, prismHitDistance(origin, direction, candidate,
                visual.thinOffset(), Math.min(distance, visual.thinOffset() + movingLength),
                1.5D * Math.sqrt(2.0D) / 16.0D, 4, Math.PI / 4.0D, axis, basisX, basisZ));
        return best;
    }

    private static double prismHitDistance(Vec3 origin, Vec3 direction, HydraulicRodGeometry candidate,
                                           double startY, double endY, double radius, int sides, double phase) {
        Vec3 delta = candidate.end().subtract(candidate.start());
        double distance = delta.length();
        if (distance < 1.0E-6D) return Double.POSITIVE_INFINITY;
        Vec3 axis = delta.scale(1.0D / distance);
        Vec3 basisX = candidate.rollReference().subtract(axis.scale(candidate.rollReference().dot(axis)));
        if (basisX.lengthSqr() < 1.0E-6D) {
            basisX = Math.abs(axis.y) < 0.9D ? new Vec3(0.0D, 1.0D, 0.0D) : new Vec3(1.0D, 0.0D, 0.0D);
            basisX = basisX.subtract(axis.scale(basisX.dot(axis)));
        }
        basisX = basisX.normalize();
        return prismHitDistance(origin, direction, candidate, startY, endY, radius, sides, phase,
                axis, basisX, axis.cross(basisX).normalize());
    }

    private static double prismHitDistance(Vec3 origin, Vec3 direction, HydraulicRodGeometry candidate,
                                           double startY, double endY, double radius, int sides, double phase,
                                           Vec3 axis, Vec3 basisX, Vec3 basisZ) {
        if (endY <= startY + 1.0E-6D) return Double.POSITIVE_INFINITY;
        Vec3 relative = origin.subtract(candidate.start());
        double ox = relative.dot(basisX), oy = relative.dot(axis), oz = relative.dot(basisZ);
        double dx = direction.dot(basisX), dy = direction.dot(axis), dz = direction.dot(basisZ);
        double tMin = 0.0D, tMax = Double.POSITIVE_INFINITY;
        if (Math.abs(dy) < 1.0E-9D) {
            if (oy < startY - 1.0E-7D || oy > endY + 1.0E-7D) return Double.POSITIVE_INFINITY;
        } else {
            double a = (startY - oy) / dy, b = (endY - oy) / dy;
            if (a > b) { double tmp = a; a = b; b = tmp; }
            tMin = Math.max(tMin, a); tMax = Math.min(tMax, b);
            if (tMin > tMax) return Double.POSITIVE_INFINITY;
        }
        for (int i = 0; i < sides; i++) {
            double a1 = phase + Math.PI * 2.0D * i / sides;
            double a2 = phase + Math.PI * 2.0D * ((i + 1) % sides) / sides;
            double x1 = Math.cos(a1) * radius, z1 = Math.sin(a1) * radius;
            double ex = Math.cos(a2) * radius - x1, ez = Math.sin(a2) * radius - z1;
            double centerValue = ex * (0.0D - z1) - ez * (0.0D - x1);
            double sign = centerValue >= 0.0D ? 1.0D : -1.0D;
            double value = sign * (ex * (oz - z1) - ez * (ox - x1));
            double slope = sign * (ex * dz - ez * dx);
            if (Math.abs(slope) < 1.0E-9D) {
                if (value < -1.0E-7D) return Double.POSITIVE_INFINITY;
            } else if (slope > 0.0D) {
                tMin = Math.max(tMin, -value / slope);
            } else {
                tMax = Math.min(tMax, -value / slope);
            }
            if (tMin > tMax) return Double.POSITIVE_INFINITY;
        }
        return tMin <= tMax ? tMin : Double.POSITIVE_INFINITY;
    }

}
