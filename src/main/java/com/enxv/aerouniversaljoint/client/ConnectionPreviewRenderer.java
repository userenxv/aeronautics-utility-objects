package com.enxv.aerouniversaljoint.client;

import com.enxv.aerouniversaljoint.content.JointBindingData;
import com.enxv.aerouniversaljoint.content.JointVariant;
import com.enxv.aerouniversaljoint.content.PendingRodSelections;
import com.enxv.aerouniversaljoint.content.UniversalJointBlockEntity;
import com.enxv.aerouniversaljoint.content.UniversalJointRenderer;
import com.enxv.aerouniversaljoint.content.UniversalJointRodItem;
import com.enxv.aerouniversaljoint.util.SubLevelReferenceHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;

public final class ConnectionPreviewRenderer {
    private static final int PREVIEW_ALPHA = 255;
    private static final double LIMIT_WARNING_LAG_BLOCKS = 0.25D;
    private static final Color VALID = new Color(80, 255, 120, PREVIEW_ALPHA);
    private static final Color INVALID = new Color(255, 70, 55, PREVIEW_ALPHA);
    private static final Color NEUTRAL = new Color(255, 215, 70, PREVIEW_ALPHA);

    private ConnectionPreviewRenderer() {
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        Level level = minecraft.level;
        if (player == null || level == null || !(minecraft.hitResult instanceof BlockHitResult hit)) {
            return;
        }
        if (hit.getType() == HitResult.Type.MISS) {
            return;
        }

        ItemStack held = player.getMainHandItem();
        float partialTicks = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        if (held.getItem() instanceof UniversalJointRodItem rodItem) {
            PendingRodSelections.readClientPending(player).ifPresent(selection ->
                    renderUniversalPreview(level, event, selection.selection(), hit, rodItem.isBrass(), partialTicks));
            return;
        }
    }

    private static void renderUniversalPreview(Level level, RenderLevelStageEvent event, JointBindingData.Selection selection,
                                               BlockHitResult hit, boolean brassRod, float partialTicks) {
        BlockEntity startBlockEntity = resolve(level, selection);
        if (!(startBlockEntity instanceof UniversalJointBlockEntity start) || !level.dimension().location().equals(selection.dimensionId())) {
            return;
        }

        BlockPos targetPos = hit.getBlockPos();
        BlockEntity targetBlockEntity = SubLevelReferenceHelper.resolveBlockEntity(level, targetPos, null);
        JointVariant variant = brassRod ? JointVariant.BRASS : JointVariant.ANDESITE;
        Vec3 startCenter = projectedCenter(start, partialTicks);
        Vec3 targetCenter = targetBlockEntity != null
                ? projectedCenter(targetBlockEntity, partialTicks)
                : projectedBlockCenter(level, targetPos, partialTicks);
        boolean beyondWarningLimit = isBeyondWarningLimit(startCenter, targetCenter, variant.getLinkRange());
        PreviewState state = PreviewState.NEUTRAL;
        if (beyondWarningLimit) {
            state = PreviewState.INVALID;
        } else if (targetBlockEntity instanceof UniversalJointBlockEntity target) {
            UniversalJointBlockEntity.LinkResult result = target.previewLinkToSelection(selection, brassRod);
            state = result == UniversalJointBlockEntity.LinkResult.SUCCESS
                    ? PreviewState.VALID
                    : result == UniversalJointBlockEntity.LinkResult.TOO_FAR ? PreviewState.NEUTRAL : PreviewState.INVALID;
        }

        boolean targetIsConnectionHead = isConnectionHead(targetBlockEntity);
        Vec3 endAnchor = targetIsConnectionHead ? targetCenter : projectedFaceCenter(level, targetBlockEntity, hit, partialTicks);
        Vec3 startAnchor = targetIsConnectionHead ? startCenter : projectedSurfaceToward(start, endAnchor, partialTicks);
        Color color = colorFor(state);
        renderInWorld(event, startAnchor, endAnchor, buffer ->
                UniversalJointRenderer.renderPreview(start, variant,
                        startAnchor, endAnchor, event.getPoseStack(), buffer, LightTexture.FULL_BRIGHT,
                        color, partialTicks));
    }

    private static void renderInWorld(RenderLevelStageEvent event, Vec3 start, Vec3 end, Renderer renderer) {
        if (start.distanceToSqr(end) < 1.0E-6D) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        RenderType renderType = RenderType.translucent();
        renderer.render(bufferSource.getBuffer(renderType));
        bufferSource.endBatch(renderType);
        poseStack.popPose();
    }

    private static Color colorFor(PreviewState state) {
        Color base = switch (state) {
            case VALID -> VALID;
            case INVALID -> INVALID;
            case NEUTRAL -> NEUTRAL;
        };
        return base.copy();
    }

    private static boolean isBeyondWarningLimit(Vec3 start, Vec3 end, double limit) {
        double warningLimit = limit + LIMIT_WARNING_LAG_BLOCKS;
        return start.distanceToSqr(end) > warningLimit * warningLimit;
    }

    private static boolean isConnectionHead(@Nullable BlockEntity blockEntity) {
        return blockEntity instanceof UniversalJointBlockEntity;
    }

    @Nullable
    private static BlockEntity resolve(Level level, JointBindingData.Selection selection) {
        if (!level.dimension().location().equals(selection.dimensionId())) {
            return null;
        }

        return SubLevelReferenceHelper.resolveBlockEntity(level, selection.pos(), selection.subLevelId());
    }

    private static Vec3 projectedCenter(BlockEntity blockEntity, float partialTicks) {
        Vec3 center = Vec3.atCenterOf(blockEntity.getBlockPos());
        SubLevel containing = Sable.HELPER.getContaining(blockEntity);
        return containing != null ? getRenderPose(containing, partialTicks).transformPosition(center) : center;
    }

    private static Vec3 projectedBlockCenter(Level level, BlockPos pos, float partialTicks) {
        SubLevel containing = Sable.HELPER.getContaining(level, pos);
        Vec3 center = Vec3.atCenterOf(pos);
        return containing != null ? getRenderPose(containing, partialTicks).transformPosition(center) : center;
    }

    private static Vec3 projectedSurfaceToward(BlockEntity blockEntity, Vec3 worldTarget, float partialTicks) {
        SubLevel containing = Sable.HELPER.getContaining(blockEntity);
        Vec3 localCenter = Vec3.atCenterOf(blockEntity.getBlockPos());
        Vec3 localTarget = containing != null
                ? getRenderPose(containing, partialTicks).transformPositionInverse(worldTarget)
                : worldTarget;
        Vec3 localSurface = surfacePointToward(localCenter, localTarget);
        return containing != null ? getRenderPose(containing, partialTicks).transformPosition(localSurface) : localSurface;
    }

    private static Vec3 projectedFaceCenter(Level level, @Nullable BlockEntity targetBlockEntity, BlockHitResult hit, float partialTicks) {
        SubLevel containing = targetBlockEntity != null
                ? Sable.HELPER.getContaining(targetBlockEntity)
                : Sable.HELPER.getContaining(level, hit.getBlockPos());
        BlockPos pos = targetBlockEntity != null ? targetBlockEntity.getBlockPos() : hit.getBlockPos();
        Vec3 location = Vec3.atCenterOf(pos)
                .add(Vec3.atLowerCornerOf(hit.getDirection().getNormal()).scale(0.5D));
        return containing != null ? getRenderPose(containing, partialTicks).transformPosition(location) : location;
    }

    private static Vec3 surfacePointToward(Vec3 center, Vec3 target) {
        Vec3 delta = target.subtract(center);
        double maxAxis = Math.max(Math.abs(delta.x), Math.max(Math.abs(delta.y), Math.abs(delta.z)));
        if (maxAxis < 1.0E-8D) {
            return center;
        }
        return center.add(delta.scale(0.5D / maxAxis));
    }

    private static Pose3dc getRenderPose(SubLevel subLevel, float partialTicks) {
        return subLevel instanceof ClientSubLevel clientSubLevel ? clientSubLevel.renderPose(partialTicks) : subLevel.logicalPose();
    }

    private enum PreviewState {
        VALID,
        INVALID,
        NEUTRAL
    }

    @FunctionalInterface
    private interface Renderer {
        void render(VertexConsumer buffer);
    }
}
