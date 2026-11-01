package com.enxv.aerouniversaljoint.client;

import com.enxv.aerouniversaljoint.content.HydraulicRodGeometry;
import com.enxv.aerouniversaljoint.content.HydraulicRodInteraction;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.LevelPoseProviderExtension;
import com.enxv.aerouniversaljoint.content.GiantHydraulicRodVisualState;
import com.enxv.aerouniversaljoint.network.OpenHydraulicRodSettingsPayload;
import com.enxv.aerouniversaljoint.network.BreakHydraulicRodPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Client-side hit testing for visual hydraulic rods. */
public final class HydraulicRodTargeting {
    private static final double PIXEL = 1.0D / 16.0D;
    private static boolean usePressConsumed;
    private static boolean attackPressConsumed;

    private HydraulicRodTargeting() {}

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        HydraulicRodInteraction.Hit hovered = pick(minecraft, event.getPartialTick().getGameTimeDeltaPartialTick(true));
        if (hovered == null) return;

        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        RenderType lines = RenderType.lines();
        renderModelEdges(poseStack, buffers.getBuffer(lines), hovered.geometry());
        buffers.endBatch(lines);
        poseStack.popPose();
    }

    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        boolean use = event.isUseItem();
        boolean attack = event.isAttack();
        if (!use && !attack) return;
        if (use) {
            if (usePressConsumed) return;
            usePressConsumed = true;
        }
        if (attack) {
            if (attackPressConsumed) return;
            attackPressConsumed = true;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        HydraulicRodInteraction.Hit hovered = pick(minecraft, minecraft.getTimer().getGameTimeDeltaPartialTick(true));
        if (hovered == null) return;
        if (attack) {
            PacketDistributor.sendToServer(new BreakHydraulicRodPayload(hovered.pos(), hovered.subLevelId()));
        } else {
            PacketDistributor.sendToServer(new OpenHydraulicRodSettingsPayload(hovered.pos(), hovered.subLevelId()));
        }
        event.setCanceled(true);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.options.keyUse.isDown()) usePressConsumed = false;
        if (!minecraft.options.keyAttack.isDown()) attackPressConsumed = false;
    }

    private static HydraulicRodInteraction.Hit pick(Minecraft minecraft, float partialTicks) {
        // Use the same interpolated poses for the block ray and the rod geometry.
        LevelPoseProviderExtension level = (LevelPoseProviderExtension) minecraft.level;
        level.sable$pushPoseSupplier(subLevel -> ((ClientSubLevel) subLevel).renderPose(partialTicks));
        try {
            return HydraulicRodInteraction.pick(minecraft.player,
                    Sable.HELPER.getEyePositionInterpolated(minecraft.player, partialTicks),
                    minecraft.player.getViewVector(partialTicks), level::sable$getPose);
        } finally {
            level.sable$popPoseSupplier();
        }
    }

    private static void renderModelEdges(PoseStack poseStack, VertexConsumer consumer, HydraulicRodGeometry candidate) {
        Vec3 axis = candidate.end().subtract(candidate.start());
        double distance = axis.length();
        if (distance < 1.0E-6D) return;
        axis = axis.scale(1.0D / distance);
        Vec3 basisX = candidate.rollReference().subtract(axis.scale(candidate.rollReference().dot(axis)));
        if (basisX.lengthSqr() < 1.0E-6D) {
            basisX = Math.abs(axis.y) < 0.9D ? new Vec3(0.0D, 1.0D, 0.0D) : new Vec3(1.0D, 0.0D, 0.0D);
            basisX = basisX.subtract(axis.scale(basisX.dot(axis)));
        }
        basisX = basisX.normalize();
        Vec3 basisZ = axis.cross(basisX).normalize();

        if (!candidate.giant()) {
            renderPrism(poseStack, consumer, candidate.start().add(axis.scale(1.0D / 16.0D)),
                    candidate.end().subtract(axis.scale(1.0D / 16.0D)), basisX, basisZ,
                    Math.sqrt(2.0D) * 3.5D / 16.0D, 4, Math.PI / 4.0D);
            return;
        }

        renderFixedBarrelEdges(poseStack, consumer, candidate, axis, basisX, basisZ, distance);

        GiantHydraulicRodVisualState visual = GiantHydraulicRodVisualState.fromDistance(distance);
        double movingLength = GiantHydraulicRodVisualState.NOMINAL_ROD_LENGTH * visual.continuousScale();
        double thickEnd = visual.thickOffset() + movingLength;
        double mediumEnd = visual.mediumOffset() + movingLength;
        double thinEnd = visual.thinOffset() + movingLength;
        renderMovingStage(poseStack, consumer, candidate.start(), axis, basisX, basisZ,
                visual.thickOffset(), Math.min(distance, thickEnd), 2.5D * PIXEL);
        renderMovingStage(poseStack, consumer, candidate.start(), axis, basisX, basisZ,
                Math.max(visual.mediumOffset(), thickEnd), Math.min(distance, mediumEnd), 2.0D * PIXEL);
        renderMovingStage(poseStack, consumer, candidate.start(), axis, basisX, basisZ,
                Math.max(visual.thinOffset(), Math.max(thickEnd, mediumEnd)),
                Math.min(distance, thinEnd), 1.5D * PIXEL);
    }

    private static void renderMovingStage(PoseStack poseStack, VertexConsumer consumer, Vec3 owner, Vec3 axis,
                                          Vec3 basisX, Vec3 basisZ, double start, double end, double halfWidth) {
        if (end <= start + 1.0E-6D) return;
        renderPrism(poseStack, consumer, owner.add(axis.scale(start)), owner.add(axis.scale(end)),
                basisX, basisZ, halfWidth * Math.sqrt(2.0D), 4, Math.PI / 4.0D);
    }

    private static void renderFixedBarrelEdges(PoseStack poseStack, VertexConsumer consumer,
                                               HydraulicRodGeometry candidate, Vec3 axis, Vec3 basisX, Vec3 basisZ,
                                               double distance) {
        double barrelRadius = Math.hypot(6.5D, 2.69239D) * PIXEL;
        double barrelPhase = Math.atan2(2.69239D, 6.5D);
        renderProfile(poseStack, consumer, candidate.start(), axis, basisX, basisZ,
                0.9D * PIXEL, Math.min(distance, 19.9D * PIXEL), barrelRadius, barrelPhase);
        double collarRadius = Math.hypot(7.0D, 2.89949D) * PIXEL;
        double collarPhase = Math.atan2(2.89949D, 7.0D);
        renderProfile(poseStack, consumer, candidate.start(), axis, basisX, basisZ,
                19.9D * PIXEL, Math.min(distance, 23.1D * PIXEL), collarRadius, collarPhase);
        renderProfile(poseStack, consumer, candidate.start(), axis, basisX, basisZ,
                23.1D * PIXEL, Math.min(distance, 24.9D * PIXEL), barrelRadius, barrelPhase);
        renderProfile(poseStack, consumer, candidate.start(), axis, basisX, basisZ,
                24.9D * PIXEL, Math.min(distance, 28.1D * PIXEL), collarRadius, collarPhase);
        renderProfile(poseStack, consumer, candidate.start(), axis, basisX, basisZ,
                28.1D * PIXEL, Math.min(distance, 34.1D * PIXEL), barrelRadius, barrelPhase);
        double noseRadius = Math.hypot(5.0D, 2.07107D) * PIXEL;
        double nosePhase = Math.atan2(2.07107D, 5.0D);
        renderProfile(poseStack, consumer, candidate.start(), axis, basisX, basisZ,
                34.1D * PIXEL, Math.min(distance, 37.1D * PIXEL), noseRadius, nosePhase);
        renderProfile(poseStack, consumer, candidate.start(), axis, basisX, basisZ,
                37.1D * PIXEL, Math.min(distance, 37.9D * PIXEL),
                Math.hypot(4.0D, 1.65685D) * PIXEL, Math.atan2(1.65685D, 4.0D));
        renderProfile(poseStack, consumer, candidate.start(), axis, basisX, basisZ,
                37.9D * PIXEL, Math.min(distance, 39.1D * PIXEL), noseRadius, nosePhase);
        renderProfile(poseStack, consumer, candidate.start(), axis, basisX, basisZ,
                39.1D * PIXEL, Math.min(distance, 40.0D * PIXEL),
                3.0D * Math.sqrt(2.0D) * PIXEL, Math.PI / 4.0D, 4);
        renderProfile(poseStack, consumer, candidate.start(), axis, basisX, basisZ,
                0.0D, Math.min(distance, 0.9D * PIXEL),
                Math.hypot(5.5D, 2.27817D) * PIXEL, Math.atan2(2.27817D, 5.5D));
    }

    private static void renderProfile(PoseStack poseStack, VertexConsumer consumer, Vec3 owner, Vec3 axis,
                                      Vec3 basisX, Vec3 basisZ, double start, double end,
                                      double radius, double phase) {
        if (end <= start + 1.0E-6D) return;
        renderPrism(poseStack, consumer, owner.add(axis.scale(start)), owner.add(axis.scale(end)),
                basisX, basisZ, radius, 8, phase);
    }

    private static void renderProfile(PoseStack poseStack, VertexConsumer consumer, Vec3 owner, Vec3 axis,
                                      Vec3 basisX, Vec3 basisZ, double start, double end,
                                      double radius, double phase, int sides) {
        if (end <= start + 1.0E-6D) return;
        renderPrism(poseStack, consumer, owner.add(axis.scale(start)), owner.add(axis.scale(end)),
                basisX, basisZ, radius, sides, phase);
    }

    private static void renderPrism(PoseStack poseStack, VertexConsumer consumer, Vec3 start, Vec3 end,
                                    Vec3 basisX, Vec3 basisZ, double radius, int sides, double phase) {
        Vec3[] startRing = new Vec3[sides];
        Vec3[] endRing = new Vec3[sides];
        for (int i = 0; i < sides; i++) {
            double angle = phase + Math.PI * 2.0D * i / sides;
            Vec3 radial = basisX.scale(Math.cos(angle) * radius).add(basisZ.scale(Math.sin(angle) * radius));
            startRing[i] = start.add(radial);
            endRing[i] = end.add(radial);
        }
        for (int i = 0; i < sides; i++) {
            int next = (i + 1) % sides;
            line(poseStack, consumer, startRing[i], startRing[next]);
            line(poseStack, consumer, endRing[i], endRing[next]);
            line(poseStack, consumer, startRing[i], endRing[i]);
        }
    }

    private static void line(PoseStack poseStack, VertexConsumer consumer, Vec3 from, Vec3 to) {
        Vec3 normal = to.subtract(from).normalize();
        PoseStack.Pose pose = poseStack.last();
        consumer.addVertex(pose.pose(), (float) from.x, (float) from.y, (float) from.z)
                .setColor(5, 5, 5, 255).setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
        consumer.addVertex(pose.pose(), (float) to.x, (float) to.y, (float) to.z)
                .setColor(5, 5, 5, 255).setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
    }

}
