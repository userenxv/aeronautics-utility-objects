package com.enxv.aerouniversaljoint.content;

import com.enxv.aerouniversaljoint.util.SubLevelReferenceHelper;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.UUID;
import java.util.function.Function;

/** Selects live links independently of block-entity rendering. All distances use world space. */
public final class HydraulicRodInteraction {
    // Weak keys do not keep unloaded worlds or temporary preview block entities alive.
    private static final Set<HydraulicConnectionHeadBlockEntity> HEADS =
            Collections.newSetFromMap(new WeakHashMap<>());

    private HydraulicRodInteraction() {}

    public static void register(HydraulicConnectionHeadBlockEntity head) {
        synchronized (HEADS) { HEADS.add(head); }
    }

    @Nullable
    public static Hit pick(Player player, Vec3 origin, Vec3 direction,
                           Function<SubLevel, Pose3dc> poses) {
        Level level = player.level();
        direction = direction.normalize();
        double limit = player.blockInteractionRange();
        BlockHitResult block = level.clip(new ClipContext(origin, origin.add(direction.scale(limit)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS) {
            limit = Math.min(limit, worldHitPosition(level, block, poses).distanceTo(origin));
        }
        Vec3 end = origin.add(direction.scale(limit));
        EntityHitResult entity = ProjectileUtil.getEntityHitResult(player, origin, end,
                new AABB(origin, end).inflate(1.0D),
                candidate -> !candidate.isSpectator() && candidate.isPickable(), limit * limit);
        if (entity != null) {
            limit = Math.min(limit, Sable.HELPER.projectOutOfSubLevel(level, entity.getLocation()).distanceTo(origin));
        }
        ArrayList<HydraulicConnectionHeadBlockEntity> heads;
        synchronized (HEADS) { heads = new ArrayList<>(HEADS); }
        Hit best = null;
        for (HydraulicConnectionHeadBlockEntity head : heads) {
            if (head.getLevel() != level || head.isRemoved() || !head.hasLink()
                    || level.getBlockEntity(head.getBlockPos()) != head) continue;
            HydraulicConnectionHeadBlockEntity other = head.getLoadedLinkedConnectionHead();
            if (other == null || other.isRemoved() || !other.references(head)
                    || !head.shouldRenderConnectionTo(other)) continue;
            HydraulicRodGeometry geometry = geometry(head, other, poses);
            double distance = geometry.intersect(origin, direction, limit);
            if (!Double.isFinite(distance)) continue;
            HydraulicConnectionHeadBlockEntity settingsHead = head.isBrassHingeHead() ? other : head;
            limit = distance;
            best = new Hit(settingsHead, geometry, distance);
        }
        return best;
    }

    public static boolean isTarget(Player player, HydraulicConnectionHeadBlockEntity requested) {
        Vec3 eye = Sable.HELPER.projectOutOfSubLevel(player.level(), player.getEyePosition());
        Hit hit = pick(player, eye, player.getViewVector(1.0F), SubLevel::logicalPose);
        return hit != null && hit.head() == requested;
    }

    private static Vec3 worldHitPosition(Level level, BlockHitResult hit,
                                        Function<SubLevel, Pose3dc> poses) {
        SubLevel subLevel = Sable.HELPER.getContaining(level, hit.getBlockPos());
        return subLevel == null ? hit.getLocation() : poses.apply(subLevel).transformPosition(hit.getLocation());
    }

    private static HydraulicRodGeometry geometry(HydraulicConnectionHeadBlockEntity head,
                                                  HydraulicConnectionHeadBlockEntity other,
                                                  Function<SubLevel, Pose3dc> poses) {
        Direction facing = head.getBlockState().getValue(HydraulicConnectionHeadBlock.FACING);
        Vector3f roll = new Quaternionf()
                .rotateY(AngleHelper.rad(AngleHelper.horizontalAngle(facing)))
                .rotateX(AngleHelper.rad(AngleHelper.verticalAngle(facing) + 90.0F))
                .transform(new Vector3f(1.0F, 0.0F, 0.0F));
        Vec3 rollReference = new Vec3(roll.x(), roll.y(), roll.z());
        SubLevel containing = endpointSubLevel(head);
        if (containing != null) rollReference = poses.apply(containing).transformNormal(rollReference);
        return new HydraulicRodGeometry(anchor(head, poses), anchor(other, poses), rollReference,
                head.isGiantHydraulicLink() ? 0.72D : 0.28D, head.isGiantHydraulicLink());
    }

    private static Vec3 anchor(HydraulicConnectionHeadBlockEntity head, Function<SubLevel, Pose3dc> poses) {
        SubLevel subLevel = endpointSubLevel(head);
        BlockPos pos = head.getHingeSubLevelId() != null && subLevel != null
                && subLevel.getUniqueId().equals(head.getHingeSubLevelId()) && head.getHingeLinkPos() != null
                ? head.getHingeLinkPos() : head.getBlockPos();
        Vec3 anchor = Vec3.atCenterOf(pos);
        return subLevel == null ? anchor : poses.apply(subLevel).transformPosition(anchor);
    }

    @Nullable
    private static SubLevel endpointSubLevel(HydraulicConnectionHeadBlockEntity head) {
        if (head.getHingeSubLevelId() != null) {
            SubLevel hinge = SubLevelContainer.getContainer(head.getLevel()).getSubLevel(head.getHingeSubLevelId());
            if (hinge != null && !hinge.isRemoved()) return hinge;
        }
        return Sable.HELPER.getContaining(head);
    }

    public record Hit(HydraulicConnectionHeadBlockEntity head, HydraulicRodGeometry geometry, double distance) {
        public BlockPos pos() { return head.getBlockPos(); }
        public UUID subLevelId() { return SubLevelReferenceHelper.findContainingSubLevelId(head); }
    }
}
