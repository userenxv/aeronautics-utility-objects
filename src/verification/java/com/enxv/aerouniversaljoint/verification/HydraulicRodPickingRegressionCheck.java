package com.enxv.aerouniversaljoint.verification;

import com.enxv.aerouniversaljoint.content.HydraulicRodGeometry;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import java.util.Random;

public final class HydraulicRodPickingRegressionCheck {
    public static void main(String[] args) {
        HydraulicRodGeometry rod = rod(Vec3.ZERO, new Vec3(0, 8, 0), false);
        Vec3 eye = new Vec3(0, 1, -2);
        Vec3 forward = new Vec3(0, 0, 1);
        double distance = rod.intersect(eye, forward, 5);
        close(distance, 2 - 0.28 / Math.sqrt(2), "ordinary rod surface");
        miss(rod.intersect(eye, forward, 1), "front obstruction must win");
        miss(rod.intersect(eye, forward, distance), "block surface wins a tie");
        close(rod.intersect(eye, forward, 3), distance, "obstruction behind rod");
        miss(rod.intersect(eye, forward.scale(-1), 5), "rod behind eye");
        miss(rod.intersect(new Vec3(1, 1, -2), forward, 5), "ray beside rod");
        miss(rod(Vec3.ZERO, Vec3.ZERO, false).intersect(eye, forward, 5), "zero length link");
        close(rod.intersect(new Vec3(0, 1, -80), forward, 100),
                80 - 0.28 / Math.sqrt(2), "extended interaction range");

        HydraulicRodGeometry giant = rod(Vec3.ZERO, new Vec3(0, 8, 0), true);
        close(giant.intersect(eye, forward, 5), 2 - 6.5 / 16, "giant barrel surface");
        miss(giant.intersect(eye, forward, 1), "giant barrel behind obstruction");
        if (!Double.isFinite(giant.intersect(new Vec3(0, 5, -2), forward, 5))) {
            throw new AssertionError("extended giant moving stage must be selectable");
        }

        // Moving/rotated structures must not change the ordering of a rod and a world-space blocker.
        Random random = new Random(0x524f44);
        for (int i = 0; i < 2000; i++) {
            Quaterniond rotation = new Quaterniond().rotationXYZ(random.nextDouble() * 6,
                    random.nextDouble() * 6, random.nextDouble() * 6);
            Vec3 offset = new Vec3(30000000 + random.nextDouble(), 100, -30000000);
            Vec3 from = new Vec3(random.nextDouble() - 0.5, random.nextDouble() * 8, -2);
            HydraulicRodGeometry original = i % 2 == 0 ? rod : giant;
            HydraulicRodGeometry transformed = new HydraulicRodGeometry(
                    rotate(original.start(), rotation).add(offset), rotate(original.end(), rotation).add(offset),
                    rotate(original.rollReference(), rotation), original.radius(), original.giant());
            double expected = original.intersect(from, forward, 5);
            double actual = transformed.intersect(rotate(from, rotation).add(offset), rotate(forward, rotation), 5);
            if (Double.isFinite(expected)) close(actual, expected, "rotated/translated link");
            else miss(actual, "rotated miss");
            miss(transformed.intersect(rotate(from, rotation).add(offset), rotate(forward, rotation), 1),
                    "world-space obstruction before transformed rod");
        }
        System.out.println("PASS: ordinary/giant rods, obstruction ordering, reach, and 2000 transformed rays");
    }

    private static HydraulicRodGeometry rod(Vec3 start, Vec3 end, boolean giant) {
        return new HydraulicRodGeometry(start, end, new Vec3(1, 0, 0), giant ? 0.72 : 0.28, giant);
    }

    private static Vec3 rotate(Vec3 point, Quaterniond rotation) {
        Vector3d result = rotation.transform(new Vector3d(point.x, point.y, point.z));
        return new Vec3(result.x, result.y, result.z);
    }

    private static void miss(double value, String description) {
        if (Double.isFinite(value)) throw new AssertionError(description + ": unexpected hit " + value);
    }

    private static void close(double actual, double expected, String description) {
        if (!Double.isFinite(actual) || Math.abs(actual - expected) > 1.0E-6) {
            throw new AssertionError(description + ": " + actual + " != " + expected);
        }
    }
}
