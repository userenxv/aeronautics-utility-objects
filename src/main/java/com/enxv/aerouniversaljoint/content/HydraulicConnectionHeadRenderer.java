package com.enxv.aerouniversaljoint.content;

import com.enxv.aerouniversaljoint.client.AeroUniversalJointPartials;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.createmod.catnip.math.AngleHelper;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;

public class HydraulicConnectionHeadRenderer extends SmartBlockEntityRenderer<HydraulicConnectionHeadBlockEntity> {
    private static final double MIN_LINK_LENGTH = 1.0E-3D;
    private static final float PIXELS_PER_BLOCK = 16.0F;
    private static final float CORE_MODEL_LENGTH = 24.0F / PIXELS_PER_BLOCK;
    private static final float ROD_SCALE = 0.85F;
    private static final float ROD_OFFSET = 0.125F;
    private static final float GIANT_FIXED_OWNER_OFFSET = 23.0F / PIXELS_PER_BLOCK;
    private static final float GIANT_ROD_OWNER_OFFSET = 24.0F / PIXELS_PER_BLOCK;
    private static final float GIANT_MODEL_Y_SHIFT = 8.0F / PIXELS_PER_BLOCK;
    private static final float MIN_ROLL_REFERENCE_LENGTH_SQUARED = 1.0E-6F;
    private static final Vector3f MODEL_AXIS = new Vector3f(0.0F, 1.0F, 0.0F);
    private static final Vector3f MODEL_ROLL_REFERENCE = new Vector3f(1.0F, 0.0F, 0.0F);
    private static final Vector3f MODEL_SECONDARY_ROLL_REFERENCE = new Vector3f(0.0F, 0.0F, 1.0F);

    public HydraulicConnectionHeadRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(HydraulicConnectionHeadBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        renderConnectingRod(be, partialTicks, ms, buffer, light);
    }

    @Override
    public boolean shouldRenderOffScreen(HydraulicConnectionHeadBlockEntity be) {
        return true;
    }

    public static void renderPreview(HydraulicConnectionHeadBlockEntity be, Vec3 start, Vec3 end,
                                     PoseStack ms, VertexConsumer buffer, int light, Color color, float partialTicks) {
        renderPreview(be, start, end, ms, buffer, light, color, partialTicks, false);
    }

    public static void renderPreview(HydraulicConnectionHeadBlockEntity be, Vec3 start, Vec3 end,
                                     PoseStack ms, VertexConsumer buffer, int light, Color color, float partialTicks,
                                     boolean creative) {
        renderPreview(be, start, end, ms, buffer, light, color, partialTicks, creative, false);
    }

    public static void renderPreview(HydraulicConnectionHeadBlockEntity be, Vec3 start, Vec3 end,
                                     PoseStack ms, VertexConsumer buffer, int light, Color color, float partialTicks,
                                     boolean creative, boolean giant) {
        Vec3 connection = end.subtract(start);
        double distance = connection.length();
        Vec3 direction = distance < MIN_LINK_LENGTH
                ? worldFacingVector(be, partialTicks)
                : connection.scale(1.0D / distance);
        Vector3f renderDirection = direction.toVector3f();
        Vector3f reverseRenderDirection = new Vector3f(renderDirection).mul(-1.0F);

        if (giant) {
            renderPreviewSleeve(be, ms, buffer, light, start, renderDirection, color);
            renderPreviewSleeve(be, ms, buffer, light, end, reverseRenderDirection, color);
            renderGiantVisual(be, ms, buffer, light, start, end, distance, color, null);
            return;
        }

        renderPreviewSleeve(be, ms, buffer, light, start, renderDirection, color);
        renderPreviewSleeve(be, ms, buffer, light, end, reverseRenderDirection, color);
        renderPreviewRod(be, ms, buffer, light, start.add(direction.scale(ROD_OFFSET)), renderDirection, color, creative);
        float coreScale = (float) (distance / CORE_MODEL_LENGTH);
        if (coreScale > 0.0F) {
            renderPreviewCore(be, ms, buffer, light, start, renderDirection, coreScale, color);
        }
    }

    private void renderConnectingRod(HydraulicConnectionHeadBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light) {
        if (!be.hasLink() || be.getLevel() == null) {
            return;
        }

        HydraulicConnectionHeadBlockEntity other = be.getLoadedLinkedConnectionHead();
        if (other == null || !other.references(be)) {
            return;
        }

        if (!shouldRenderFrom(be, other)) {
            return;
        }

        Vec3 ownCenter = projectedCenter(be, partialTicks);
        Vec3 otherCenter = projectedCenter(other, partialTicks);
        Vec3 start = toRenderSpace(be, ownCenter, partialTicks);
        Vec3 end = toRenderSpace(be, otherCenter, partialTicks);
        Vec3 connection = end.subtract(start);
        double distance = connection.length();

        Vec3 direction = distance < MIN_LINK_LENGTH
                ? getStableFallbackDirection(be, other, partialTicks)
                : connection.scale(1.0D / distance);
        Vector3f renderDirection = direction.toVector3f();
        Vector3f reverseRenderDirection = new Vector3f(renderDirection).mul(-1.0F);
        RollReference startRollReference = endpointRollReferenceInRenderSpace(be, be, partialTicks);
        RollReference endRollReference = endpointRollReferenceInRenderSpace(be, other, partialTicks);

        VertexConsumer cutoutBuffer = buffer.getBuffer(RenderType.cutoutMipped());
        Color effectColor = LinkVisualEffects.effectColor(be.getLinkStrainEffect());
        if (be.isGiantHydraulicLink()) {
            renderSleeve(be, ms, cutoutBuffer, light, start, renderDirection, startRollReference, effectColor);
            renderSleeve(be, ms, cutoutBuffer, light, end, reverseRenderDirection, endRollReference, effectColor);
            renderGiantVisual(be, ms, cutoutBuffer, light, start, end, distance, effectColor, startRollReference);
            return;
        }
        renderSleeve(be, ms, cutoutBuffer, light, start, renderDirection, startRollReference, effectColor);
        renderSleeve(be, ms, cutoutBuffer, light, end, reverseRenderDirection, endRollReference, effectColor);
        renderRod(be, ms, cutoutBuffer, light, start.add(direction.scale(ROD_OFFSET)), renderDirection, startRollReference, effectColor);

        float coreScale = (float) (distance / CORE_MODEL_LENGTH);
        if (coreScale > 0.0F) {
            renderCore(be, ms, cutoutBuffer, light, start, renderDirection, coreScale, effectColor);
        }
    }

    private void renderRod(HydraulicConnectionHeadBlockEntity be, PoseStack ms, VertexConsumer buffer, int light,
                           Vec3 anchor, Vector3f direction, RollReference rollReference, Color effectColor) {
        SuperByteBuffer rod = CachedBuffers.partial(be.isCreativeLink()
                ? AeroUniversalJointPartials.CREATIVE_HYDRAULIC_ROD
                : AeroUniversalJointPartials.HYDRAULIC_ROD, be.getBlockState());
        if (rod.isEmpty()) {
            return;
        }

        rod.translate(anchor)
                .rotateTo(MODEL_AXIS, direction)
                .rotateY(rollCorrection(direction, rollReference))
                .scaleY(ROD_SCALE)
                .color(effectColor)
                .light(light)
                .renderInto(ms, buffer);
    }

    private void renderCore(HydraulicConnectionHeadBlockEntity be, PoseStack ms, VertexConsumer buffer, int light,
                            Vec3 anchor, Vector3f direction, float scale, Color effectColor) {
        SuperByteBuffer core = CachedBuffers.partial(AeroUniversalJointPartials.UNIVERSAL_JOINT_LINK_CORE_STRETCHED, be.getBlockState());
        if (core.isEmpty()) {
            return;
        }

        core.translate(anchor)
                .rotateTo(MODEL_AXIS, direction)
                .scaleY(scale)
                .color(effectColor)
                .light(light)
                .renderInto(ms, buffer);
    }

    private static void renderGiantVisual(HydraulicConnectionHeadBlockEntity be, PoseStack ms,
                                          VertexConsumer buffer, int light, Vec3 owner, Vec3 remote,
                                          double distance, Color color, @Nullable RollReference rollReference) {
        Vec3 connection = remote.subtract(owner);
        double safeDistance = connection.length();
        Vec3 direction = safeDistance < MIN_LINK_LENGTH
                ? worldFacingVector(be, 0.0F)
                : connection.scale(1.0D / safeDistance);
        Vector3f axis = direction.toVector3f();
        float roll = rollReference == null ? 0.0F : rollCorrection(axis, rollReference);
        GiantHydraulicRodVisualState visual = GiantHydraulicRodVisualState.fromDistance(distance);

        renderGiantPart(AeroUniversalJointPartials.GIANT_HYDRAULIC_ROD_FIXED, be, ms, buffer, light,
                owner.add(direction.scale(GIANT_FIXED_OWNER_OFFSET - GIANT_MODEL_Y_SHIFT)), axis, roll, 1.0F, color);
        renderGiantRod(AeroUniversalJointPartials.GIANT_HYDRAULIC_ROD_THICK, visual.thickOffset(), visual,
                be, ms, buffer, light, owner, direction, axis, roll, color);
        renderGiantRod(AeroUniversalJointPartials.GIANT_HYDRAULIC_ROD_MEDIUM, visual.mediumOffset(), visual,
                be, ms, buffer, light, owner, direction, axis, roll, color);
        renderGiantRod(AeroUniversalJointPartials.GIANT_HYDRAULIC_ROD_THIN, visual.thinOffset(), visual,
                be, ms, buffer, light, owner, direction, axis, roll, color);
    }

    private static void renderGiantRod(PartialModel partial, double offset, GiantHydraulicRodVisualState visual,
                                       HydraulicConnectionHeadBlockEntity be, PoseStack ms, VertexConsumer buffer,
                                       int light, Vec3 owner, Vec3 direction, Vector3f axis, float roll,
                                       Color color) {
        float scale = (float) visual.continuousScale();
        Vec3 origin = owner.add(direction.scale(offset + (GIANT_ROD_OWNER_OFFSET - GIANT_MODEL_Y_SHIFT) * scale));
        renderGiantPart(partial, be, ms, buffer, light, origin, axis, roll, scale, color);
    }

    private static void renderGiantPart(PartialModel partial, HydraulicConnectionHeadBlockEntity be,
                                        PoseStack ms, VertexConsumer buffer, int light, Vec3 origin,
                                        Vector3f axis, float roll, float scale, Color color) {
        SuperByteBuffer model = CachedBuffers.partial(partial, be.getBlockState());
        if (model.isEmpty()) {
            return;
        }

        model.translate(origin)
                .rotateTo(MODEL_AXIS, axis)
                .rotateY(roll)
                .scaleY(scale)
                .color(color)
                .light(light)
                .renderInto(ms, buffer);
    }

    private void renderSleeve(HydraulicConnectionHeadBlockEntity be, PoseStack ms, VertexConsumer buffer, int light,
                              Vec3 anchor, Vector3f direction, RollReference rollReference, Color effectColor) {
        SuperByteBuffer sleeve = CachedBuffers.partial(AeroUniversalJointPartials.HYDRAULIC_HINGE_SLEEVE, be.getBlockState());
        if (sleeve.isEmpty()) {
            return;
        }

        sleeve.translate(anchor)
                .rotateTo(MODEL_AXIS, direction)
                .rotateY(rollCorrection(direction, rollReference))
                .color(effectColor)
                .light(light)
                .renderInto(ms, buffer);
    }

    private static void renderPreviewRod(HydraulicConnectionHeadBlockEntity be, PoseStack ms, VertexConsumer buffer,
                                         int light, Vec3 anchor, Vector3f direction, Color color, boolean creative) {
        SuperByteBuffer rod = CachedBuffers.partial(creative
                ? AeroUniversalJointPartials.CREATIVE_HYDRAULIC_ROD
                : AeroUniversalJointPartials.HYDRAULIC_ROD, be.getBlockState());
        if (rod.isEmpty()) {
            return;
        }

        rod.translate(anchor)
                .rotateTo(MODEL_AXIS, direction)
                .scaleY(ROD_SCALE)
                .color(color)
                .light(light)
                .renderInto(ms, buffer);
    }

    private static void renderPreviewSleeve(HydraulicConnectionHeadBlockEntity be, PoseStack ms, VertexConsumer buffer,
                                            int light, Vec3 anchor, Vector3f direction, Color color) {
        SuperByteBuffer sleeve = CachedBuffers.partial(AeroUniversalJointPartials.HYDRAULIC_HINGE_SLEEVE, be.getBlockState());
        if (sleeve.isEmpty()) {
            return;
        }

        sleeve.translate(anchor)
                .rotateTo(MODEL_AXIS, direction)
                .color(color)
                .light(light)
                .renderInto(ms, buffer);
    }

    private static void renderPreviewCore(HydraulicConnectionHeadBlockEntity be, PoseStack ms, VertexConsumer buffer,
                                          int light, Vec3 anchor, Vector3f direction, float scale, Color color) {
        SuperByteBuffer core = CachedBuffers.partial(AeroUniversalJointPartials.UNIVERSAL_JOINT_LINK_CORE_STRETCHED, be.getBlockState());
        if (core.isEmpty()) {
            return;
        }

        core.translate(anchor)
                .rotateTo(MODEL_AXIS, direction)
                .scaleY(scale)
                .color(color)
                .light(light)
                .renderInto(ms, buffer);
    }

    private Vec3 projectedCenter(HydraulicConnectionHeadBlockEntity be, float partialTicks) {
        SubLevel hingeSubLevel = hingeSubLevel(be);
        BlockPos hingeLinkPos = be.getHingeLinkPos();
        if (hingeSubLevel != null && hingeLinkPos != null) {
            return getRenderPose(hingeSubLevel, partialTicks).transformPosition(Vec3.atCenterOf(hingeLinkPos));
        }

        Vec3 center = Vec3.atCenterOf(be.getBlockPos());
        SubLevel containing = Sable.HELPER.getContaining(be);
        return containing != null ? getRenderPose(containing, partialTicks).transformPosition(center) : center;
    }

    private Vec3 toRenderSpace(HydraulicConnectionHeadBlockEntity be, Vec3 worldPosition, float partialTicks) {
        Vec3 localPosition = worldPosition;
        SubLevel containing = Sable.HELPER.getContaining(be);
        if (containing != null) {
            localPosition = getRenderPose(containing, partialTicks).transformPositionInverse(worldPosition);
        }

        return localPosition.subtract(Vec3.atLowerCornerOf(be.getBlockPos()));
    }

    private Vec3 getWorldFacingVector(HydraulicConnectionHeadBlockEntity be, float partialTicks) {
        return worldFacingVector(be, partialTicks);
    }

    private static Vec3 worldFacingVector(HydraulicConnectionHeadBlockEntity be, float partialTicks) {
        Direction facing = be.getBlockState().getValue(HydraulicConnectionHeadBlock.FACING);
        Vec3 normal = new Vec3(facing.getStepX(), facing.getStepY(), facing.getStepZ());
        SubLevel containing = endpointSubLevel(be);
        if (containing != null) {
            normal = getRenderPose(containing, partialTicks).transformNormal(normal);
        }
        return normal.lengthSqr() > 1.0E-8D ? normal.normalize() : new Vec3(0.0D, 1.0D, 0.0D);
    }

    private Vec3 getStableFallbackDirection(HydraulicConnectionHeadBlockEntity be,
                                            HydraulicConnectionHeadBlockEntity other,
                                            float partialTicks) {
        Vec3 ownFacing = getWorldFacingVector(be, partialTicks);
        Vec3 otherFacing = getWorldFacingVector(other, partialTicks).scale(-1.0D);
        Vec3 blended = ownFacing.add(otherFacing);
        if (blended.lengthSqr() > 1.0E-8D) {
            return blended.normalize();
        }
        return ownFacing;
    }

    private Vec3 normalToWorld(HydraulicConnectionHeadBlockEntity be, Vector3f localNormal, float partialTicks) {
        Vec3 normal = new Vec3(localNormal.x(), localNormal.y(), localNormal.z());
        SubLevel containing = endpointSubLevel(be);
        return containing != null ? getRenderPose(containing, partialTicks).transformNormal(normal) : normal;
    }

    private Vec3 normalToRenderSpace(HydraulicConnectionHeadBlockEntity be, Vec3 worldNormal, float partialTicks) {
        SubLevel containing = Sable.HELPER.getContaining(be);
        return containing != null ? getRenderPose(containing, partialTicks).transformNormalInverse(worldNormal) : worldNormal;
    }

    private RollReference endpointRollReferenceInRenderSpace(HydraulicConnectionHeadBlockEntity renderBe,
                                                             HydraulicConnectionHeadBlockEntity endpoint,
                                                             float partialTicks) {
        Direction facing = endpoint.getBlockState().getValue(HydraulicConnectionHeadBlock.FACING);
        Vector3f primary = modelRollReference(facing, MODEL_ROLL_REFERENCE);
        Vector3f secondary = modelRollReference(facing, MODEL_SECONDARY_ROLL_REFERENCE);
        return new RollReference(
                normalToRenderSpace(renderBe, normalToWorld(endpoint, primary, partialTicks), partialTicks).toVector3f(),
                normalToRenderSpace(renderBe, normalToWorld(endpoint, secondary, partialTicks), partialTicks).toVector3f());
    }

    private Vector3f modelRollReference(Direction facing, Vector3f modelReference) {
        Vector3f reference = new Vector3f(modelReference);
        new Quaternionf()
                .rotateY(AngleHelper.rad(AngleHelper.horizontalAngle(facing)))
                .rotateX(AngleHelper.rad(AngleHelper.verticalAngle(facing) + 90.0F))
                .transform(reference);
        return reference;
    }

    private static float rollCorrection(Vector3f direction, RollReference reference) {
        Vector3f axis = new Vector3f(direction);
        if (!normalizeIfUsable(axis)) {
            return 0.0F;
        }

        Quaternionf alignment = new Quaternionf().rotationTo(MODEL_AXIS, axis);
        Float primaryCorrection = rollCorrection(axis, alignment, MODEL_ROLL_REFERENCE, reference.primary());
        if (primaryCorrection != null) {
            return primaryCorrection;
        }

        Float secondaryCorrection = rollCorrection(axis, alignment, MODEL_SECONDARY_ROLL_REFERENCE, reference.secondary());
        return secondaryCorrection != null ? secondaryCorrection : 0.0F;
    }

    private static Float rollCorrection(Vector3f axis, Quaternionf alignment, Vector3f modelReference, Vector3f targetReference) {
        Vector3f target = projectOntoPlane(targetReference, axis);
        if (!normalizeIfUsable(target)) {
            return null;
        }

        Vector3f current = alignment.transform(new Vector3f(modelReference));
        current = projectOntoPlane(current, axis);
        if (!normalizeIfUsable(current)) {
            return null;
        }

        Vector3f cross = new Vector3f(current).cross(target);
        return (float) Math.atan2(cross.dot(axis), current.dot(target));
    }

    private static Vector3f projectOntoPlane(Vector3f vector, Vector3f normal) {
        return new Vector3f(vector).sub(new Vector3f(normal).mul(vector.dot(normal)));
    }

    private static boolean normalizeIfUsable(Vector3f vector) {
        if (vector.lengthSquared() < MIN_ROLL_REFERENCE_LENGTH_SQUARED) {
            return false;
        }
        vector.normalize();
        return true;
    }

    private static Pose3dc getRenderPose(SubLevel subLevel, float partialTicks) {
        return subLevel instanceof ClientSubLevel clientSubLevel ? clientSubLevel.renderPose(partialTicks) : subLevel.logicalPose();
    }

    private static SubLevel endpointSubLevel(HydraulicConnectionHeadBlockEntity be) {
        SubLevel hingeSubLevel = hingeSubLevel(be);
        return hingeSubLevel != null ? hingeSubLevel : Sable.HELPER.getContaining(be);
    }

    private static SubLevel hingeSubLevel(HydraulicConnectionHeadBlockEntity be) {
        if (be.getLevel() == null || be.getHingeSubLevelId() == null) {
            return null;
        }
        return SubLevelContainer.getContainer(be.getLevel()).getSubLevel(be.getHingeSubLevelId());
    }

    private boolean shouldRenderFrom(HydraulicConnectionHeadBlockEntity be, HydraulicConnectionHeadBlockEntity other) {
        return be.shouldRenderConnectionTo(other);
    }

    private record RollReference(Vector3f primary, Vector3f secondary) {
    }

}
