package com.enxv.aerouniversaljoint.content;

import com.enxv.aerouniversaljoint.AeroUniversalJointConfig;
import com.enxv.aerouniversaljoint.ModBlockEntities;
import com.enxv.aerouniversaljoint.ModBlocks;
import com.enxv.aerouniversaljoint.ModItems;
import com.enxv.aerouniversaljoint.content.hydraulic.HydraulicLengthControl;
import com.enxv.aerouniversaljoint.content.hydraulic.GiantHydraulicPhysics;
import com.enxv.aerouniversaljoint.content.hydraulic.GiantHydraulicSettingsState;
import com.enxv.aerouniversaljoint.content.hydraulic.HydraulicCylinderControl;
import com.enxv.aerouniversaljoint.content.hydraulic.HydraulicSettings;
import com.enxv.aerouniversaljoint.content.hydraulic.HydraulicSettingsState;
import com.enxv.aerouniversaljoint.network.SyncHydraulicSelectionPayload;
import com.enxv.aerouniversaljoint.util.SubLevelReferenceHelper;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis;
import dev.ryanhcode.sable.api.physics.constraint.GenericConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.GenericConstraintHandle;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle;
import dev.ryanhcode.sable.api.physics.constraint.RotaryConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.RotaryConstraintHandle;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class HydraulicConnectionHeadBlockEntity extends SmartBlockEntity implements BlockEntitySubLevelActor, MenuProvider,
        SubLevelLinkedEndpoint {
    private static final String TAG_LINKED_POS = "LinkedPos";
    private static final String TAG_LINKED_SUB_LEVEL = "LinkedSubLevel";
    private static final String TAG_STRETCH_RESISTANCE = "StretchResistance";
    private static final String TAG_FREE_MODE = "FreeMode";
    private static final String TAG_EXPECTED_LENGTH_TENTHS = "ExpectedLengthTenths";
    private static final String TAG_REDSTONE_MIN_LENGTH_TENTHS = "RedstoneMinLengthTenths";
    private static final String TAG_REDSTONE_MAX_LENGTH_TENTHS = "RedstoneMaxLengthTenths";
    private static final String TAG_EFFECTIVE_EXPECTED_LENGTH = "EffectiveExpectedLength";
    private static final String TAG_RETURN_FORCE = "ReturnForce";
    private static final String TAG_EFFECTIVE_RETURN_FORCE = "EffectiveReturnForce";
    private static final String TAG_CREATIVE_LINK = "CreativeLink";
    private static final String TAG_GIANT_HYDRAULIC_LINK = "GiantHydraulicLink";
    private static final String TAG_GIANT_HYDRAULIC_FLOW = "GiantHydraulicFlow";
    private static final String TAG_GIANT_HYDRAULIC_VENTED = "GiantHydraulicVented";
    private static final String TAG_GIANT_HYDRAULIC_TARGET_LENGTH = "GiantHydraulicTargetLength";
    private static final String TAG_GIANT_HYDRAULIC_PRESSURE = "GiantHydraulicPressure";
    private static final String TAG_GIANT_HYDRAULIC_REDSTONE_MIN = "GiantHydraulicRedstoneMin";
    private static final String TAG_GIANT_HYDRAULIC_REDSTONE_MAX = "GiantHydraulicRedstoneMax";
    private static final String TAG_RENDER_OWNER = "RenderOwner";
    private static final String TAG_LINK_STRAIN_EFFECT = "LinkStrainEffect";
    private static final String TAG_LINK_STRAINED = "LinkStrained";
    private static final String TAG_HINGE_SUB_LEVEL = "HingeSubLevel";
    private static final String TAG_HINGE_LINK_POS = "HingeLinkPos";
    private static final String TAG_HINGE_PARENT_SUB_LEVEL = "HingeParentSubLevel";
    private static final String TAG_HINGE_OWNER_POS = "HingeOwnerPos";
    private static final String TAG_HINGE_MIN_ANGLE = "HingeMinAngle";
    private static final String TAG_HINGE_MAX_ANGLE = "HingeMaxAngle";
    private static final int LINK_WARMUP_TICKS = 5;
    private static final double CREATIVE_LENGTH_SERVO_STIFFNESS = 16384.0D;
    private static final double CREATIVE_LENGTH_SERVO_DAMPING = 1024.0D;
    private static final double CREATIVE_LENGTH_APPROACH_RATE = 12.0D;
    private static final double CREATIVE_LENGTH_MAX_FORCE = 1.0E12D;
    private static final double GIANT_HYDRAULIC_HOLD_DAMPING = 1.0E6D;
    private static final double CREATIVE_LENGTH_WAKE_EPSILON = 0.01D;
    private static final int CREATIVE_LENGTH_WAKE_TICKS = 80;
    private static final double MIN_PHYSICS_DISTANCE = 1.0E-4D;
    private static final double HINGE_FREE_SPIN_DAMPING = 1.0E-3D;
    private static final double ALIGNMENT_DOT_THRESHOLD = Math.cos(Math.toRadians(35.0D));
    private static final double HINGED_AXIS_ALIGNMENT_THRESHOLD = Math.cos(Math.toRadians(85.0D));
    private static final double QUARTER_TURN_RADIANS = Math.PI * 0.5D;
    private static final double MIN_ROLL_REFERENCE_LENGTH_SQUARED = 1.0E-8D;
    private static final double HINGE_OWNER_MAX_DISTANCE_SQUARED = 4.0D;
    private static final int SERVER_LINK_VALIDATION_INTERVAL = 10;
    private static final int LINK_STRAIN_RECOVERY_TICKS = 12;
    private static final Set<ConstraintJointAxis> LOCKED_AXES = EnumSet.of(
            ConstraintJointAxis.LINEAR_X,
            ConstraintJointAxis.LINEAR_Z,
            ConstraintJointAxis.ANGULAR_X,
            ConstraintJointAxis.ANGULAR_Y,
            ConstraintJointAxis.ANGULAR_Z);
    private static final Set<ConstraintJointAxis> HINGE_LIMITED_AXES = EnumSet.of(
            ConstraintJointAxis.LINEAR_X,
            ConstraintJointAxis.LINEAR_Y,
            ConstraintJointAxis.LINEAR_Z,
            ConstraintJointAxis.ANGULAR_X,
            ConstraintJointAxis.ANGULAR_Z);
    private static final int DEFAULT_HINGE_MIN_ANGLE = -90;
    private static final int DEFAULT_HINGE_MAX_ANGLE = 90;
    @Nullable
    private BlockPos linkedPos;
    @Nullable
    private UUID linkedSubLevelId;
    private boolean hasRenderOwnerPreference = false;
    private boolean renderOwner = false;
    @Nullable
    private GenericConstraintHandle constraintHandle;
    @Nullable
    private UUID hingeSubLevelId;
    @Nullable
    private BlockPos hingeLinkPos;
    @Nullable
    private UUID hingeParentSubLevelId;
    @Nullable
    private BlockPos hingeOwnerPos;
    @Nullable
    private PhysicsConstraintHandle hingeConstraintHandle;
    @Nullable
    private Direction.Axis hingeConstraintAxis;
    private int hingeMinAngle = DEFAULT_HINGE_MIN_ANGLE;
    private int hingeMaxAngle = DEFAULT_HINGE_MAX_ANGLE;
    private int serverValidationCountdown = SERVER_LINK_VALIDATION_INTERVAL;
    private final HydraulicSettingsState settings = new HydraulicSettingsState();
    private final GiantHydraulicSettingsState giantHydraulicSettings = new GiantHydraulicSettingsState();
    private double effectiveExpectedLengthBlocks = AeroUniversalJointConfig.DEFAULT_HYDRAULIC_ROD_MIN_LINK_LENGTH;
    private boolean expectedLengthTransitionPending = true;
    private double expectedLengthApproachMultiplier = 1.0D;
    private double effectiveReturnForce = 0.0D;
    private GiantHydraulicPhysics.State giantHydraulicPhysics = GiantHydraulicPhysics.State.empty();
    private int linkWarmupTicks;
    private boolean creativeLink;
    private boolean giantHydraulicLink;
    private final ForceTotal lengthForceTotal = new ForceTotal();
    private final ForceTotal partnerLengthForceTotal = new ForceTotal();
    private boolean detachingForBlockRemoval;
    private boolean preservingLinkForSubLevelMove;
    private long lastHydraulicServoWakeGameTime = Long.MIN_VALUE;
    private long creativeServoWakeUntilGameTime = Long.MIN_VALUE;
    private double snappedConnectionRollOffset = Double.NaN;
    private int linkStrainEffectDelay;
    private float linkStrainEffect;
    private boolean linkWasStrained;
    private boolean giantHydraulicOverloadEffect;
    @Nullable
    private HydraulicConnectionHeadBlockEntity cachedLinkedHead;
    @Nullable
    private BlockPos cachedLinkedHeadPos;
    @Nullable
    private UUID cachedLinkedHeadSubLevelId;

    public HydraulicConnectionHeadBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.HYDRAULIC_CONNECTION_HEAD.get(), pos, state);
    }

    public HydraulicConnectionHeadBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        HingeAssemblyOrphanCleaner.register(this);
        HydraulicRodInteraction.register(this);
        this.effectiveExpectedLengthBlocks = AeroUniversalJointConfig.hydraulicRodMinLinkLength();
        this.setLazyTickRate(20);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void tick() {
        if (this.level == null) {
            return;
        }

        super.tick();
        this.tickLinkStrainEffect();

        if (!this.level.isClientSide) {
            this.normalizeConfiguredSettings(true);
        }

        if (this.level.isClientSide || this.linkedPos == null) {
            if (!this.level.isClientSide && this.hingeSubLevelId != null) {
                this.disassembleHingeAssembly();
            }
            return;
        }

        if (this.isHingedHead()) {
            this.ensureHingeAssemblyForLink();
        } else {
            this.disassembleHingeAssembly();
        }

        if (--this.serverValidationCountdown <= 0) {
            this.serverValidationCountdown = SERVER_LINK_VALIDATION_INTERVAL;
            this.validateLinkState(true);
            this.validateHingeAssemblyOwner();
        }
    }

    @Override
    public void lazyTick() {
        if (this.level == null) {
            return;
        }

        super.lazyTick();

        if (this.level.isClientSide || this.linkedPos == null) {
            return;
        }

        this.validateLinkState(false);
    }

    @Override
    public void remove() {
        HingeAssemblyOrphanCleaner.unregister(this);
        boolean preservingForSubLevelMove = this.preservingLinkForSubLevelMove;
        this.preservingLinkForSubLevelMove = false;
        if (this.level != null && !this.level.isClientSide && this.linkedPos != null) {
            if (preservingForSubLevelMove) {
                this.clearLinkInternal(false);
            } else if (this.detachingForBlockRemoval) {
                this.clearLinkInternal(true);
            } else {
                this.detachLink(true);
            }
        }
        if (this.level != null && !this.level.isClientSide && this.linkedPos == null && this.hingeSubLevelId != null) {
            this.disassembleHingeAssembly();
        }
        super.remove();
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        if (this.linkedPos != null) {
            tag.put(TAG_LINKED_POS, NbtUtils.writeBlockPos(this.linkedPos));
        }
        if (this.linkedSubLevelId != null) {
            tag.putUUID(TAG_LINKED_SUB_LEVEL, this.linkedSubLevelId);
        }
        if (this.linkedPos != null && this.hasRenderOwnerPreference) {
            tag.putBoolean(TAG_RENDER_OWNER, this.renderOwner);
        }
        tag.putInt(TAG_STRETCH_RESISTANCE, this.getStretchResistance());
        tag.putBoolean(TAG_FREE_MODE, this.settings.freeMode());
        tag.putInt(TAG_EXPECTED_LENGTH_TENTHS, this.getExpectedLengthTenths());
        tag.putInt(TAG_REDSTONE_MIN_LENGTH_TENTHS, this.getRedstoneMinLengthTenths());
        tag.putInt(TAG_REDSTONE_MAX_LENGTH_TENTHS, this.getRedstoneMaxLengthTenths());
        tag.putDouble(TAG_EFFECTIVE_EXPECTED_LENGTH, this.effectiveExpectedLengthBlocks);
        tag.putInt(TAG_RETURN_FORCE, this.getReturnForce());
        tag.putDouble(TAG_EFFECTIVE_RETURN_FORCE, this.effectiveReturnForce);
        if (this.isBrassHingeHead()) {
            tag.putInt(TAG_HINGE_MIN_ANGLE, this.hingeMinAngle);
            tag.putInt(TAG_HINGE_MAX_ANGLE, this.hingeMaxAngle);
        }
        if (this.creativeLink) {
            tag.putBoolean(TAG_CREATIVE_LINK, true);
        }
        if (this.giantHydraulicLink) {
            tag.putBoolean(TAG_GIANT_HYDRAULIC_LINK, true);
            tag.putInt(TAG_GIANT_HYDRAULIC_FLOW, this.giantHydraulicSettings.flowLitresPerMinute());
            tag.putBoolean(TAG_GIANT_HYDRAULIC_VENTED, this.giantHydraulicSettings.vented());
            tag.putInt(TAG_GIANT_HYDRAULIC_TARGET_LENGTH, this.giantHydraulicSettings.targetLengthTenths());
            tag.putInt(TAG_GIANT_HYDRAULIC_PRESSURE, this.giantHydraulicSettings.pressureBar());
            tag.putInt(TAG_GIANT_HYDRAULIC_REDSTONE_MIN, this.giantHydraulicSettings.redstoneMinLengthTenths());
            tag.putInt(TAG_GIANT_HYDRAULIC_REDSTONE_MAX, this.giantHydraulicSettings.redstoneMaxLengthTenths());
        }
        if (this.linkStrainEffect != 0.0F) {
            tag.putFloat(TAG_LINK_STRAIN_EFFECT, this.linkStrainEffect);
        }
        if (this.linkWasStrained) {
            tag.putBoolean(TAG_LINK_STRAINED, true);
        }
        if (this.hingeSubLevelId != null && this.hingeLinkPos != null) {
            tag.putUUID(TAG_HINGE_SUB_LEVEL, this.hingeSubLevelId);
            tag.put(TAG_HINGE_LINK_POS, NbtUtils.writeBlockPos(this.hingeLinkPos));
            if (this.hingeOwnerPos != null) {
                tag.put(TAG_HINGE_OWNER_POS, NbtUtils.writeBlockPos(this.hingeOwnerPos));
            }
            if (this.hingeParentSubLevelId != null) {
                tag.putUUID(TAG_HINGE_PARENT_SUB_LEVEL, this.hingeParentSubLevelId);
            }
        }
        super.write(tag, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        BlockPos oldLinkedPos = this.linkedPos;
        UUID oldLinkedSubLevelId = this.linkedSubLevelId;
        UUID oldHingeSubLevelId = this.hingeSubLevelId;
        BlockPos oldHingeLinkPos = this.hingeLinkPos;
        BlockPos oldHingeOwnerPos = this.hingeOwnerPos;
        UUID oldHingeParentSubLevelId = this.hingeParentSubLevelId;
        boolean oldHasRenderOwnerPreference = this.hasRenderOwnerPreference;
        boolean oldRenderOwner = this.renderOwner;
        boolean oldGiantHydraulicLink = this.giantHydraulicLink;
        this.linkedPos = tag.contains(TAG_LINKED_POS, Tag.TAG_INT_ARRAY)
                ? NbtUtils.readBlockPos(tag, TAG_LINKED_POS).orElse(null)
                : null;
        this.linkedSubLevelId = tag.hasUUID(TAG_LINKED_SUB_LEVEL) ? tag.getUUID(TAG_LINKED_SUB_LEVEL) : null;
        this.hasRenderOwnerPreference = this.linkedPos != null && tag.contains(TAG_RENDER_OWNER, Tag.TAG_BYTE);
        this.renderOwner = this.hasRenderOwnerPreference && tag.getBoolean(TAG_RENDER_OWNER);
        int stretchResistance = tag.contains(TAG_STRETCH_RESISTANCE, Tag.TAG_INT)
                ? clampStretchResistance(tag.getInt(TAG_STRETCH_RESISTANCE))
                : AeroUniversalJointConfig.hydraulicRodDefaultStretchResistance();
        boolean freeMode = tag.contains(TAG_FREE_MODE, Tag.TAG_BYTE) && tag.getBoolean(TAG_FREE_MODE);
        int expectedLengthTenths = tag.contains(TAG_EXPECTED_LENGTH_TENTHS, Tag.TAG_INT)
                ? clampExpectedLengthTenths(tag.getInt(TAG_EXPECTED_LENGTH_TENTHS))
                : getMinExpectedLengthTenths();
        int redstoneMinLengthTenths = tag.contains(TAG_REDSTONE_MIN_LENGTH_TENTHS, Tag.TAG_INT)
                ? clampExpectedLengthTenths(tag.getInt(TAG_REDSTONE_MIN_LENGTH_TENTHS))
                : getMinExpectedLengthTenths();
        int redstoneMaxLengthTenths = tag.contains(TAG_REDSTONE_MAX_LENGTH_TENTHS, Tag.TAG_INT)
                ? clampExpectedLengthTenths(tag.getInt(TAG_REDSTONE_MAX_LENGTH_TENTHS))
                : getMaxExpectedLengthTenths();
        int returnForce = tag.contains(TAG_RETURN_FORCE, Tag.TAG_INT)
                ? clampReturnForce(tag.getInt(TAG_RETURN_FORCE))
                : AeroUniversalJointConfig.hydraulicRodDefaultReturnForce();
        this.settings.applyBaseSettings(stretchResistance, freeMode, expectedLengthTenths, returnForce);
        this.settings.applyRedstoneLengthRange(redstoneMinLengthTenths, redstoneMaxLengthTenths);
        boolean hasEffectiveExpectedLength = tag.contains(TAG_EFFECTIVE_EXPECTED_LENGTH, Tag.TAG_DOUBLE);
        this.effectiveExpectedLengthBlocks = hasEffectiveExpectedLength
                ? clampEffectiveExpectedLength(tag.getDouble(TAG_EFFECTIVE_EXPECTED_LENGTH))
                : this.settings.expectedLengthTenths() / 10.0D;
        this.expectedLengthTransitionPending = !this.settings.freeMode() && this.linkedPos != null && !hasEffectiveExpectedLength;
        this.effectiveReturnForce = tag.contains(TAG_EFFECTIVE_RETURN_FORCE, Tag.TAG_DOUBLE)
                ? clampEffectiveReturnForce(tag.getDouble(TAG_EFFECTIVE_RETURN_FORCE))
                : 0.0D;
        this.linkWarmupTicks = 0;
        this.creativeLink = tag.contains(TAG_CREATIVE_LINK, Tag.TAG_BYTE) && tag.getBoolean(TAG_CREATIVE_LINK);
        this.giantHydraulicLink = tag.contains(TAG_GIANT_HYDRAULIC_LINK, Tag.TAG_BYTE)
                && tag.getBoolean(TAG_GIANT_HYDRAULIC_LINK);
        if (this.giantHydraulicLink) {
            int flow = tag.contains(TAG_GIANT_HYDRAULIC_FLOW, Tag.TAG_INT)
                    ? tag.getInt(TAG_GIANT_HYDRAULIC_FLOW)
                    : GiantHydraulicSettingsState.DEFAULT_FLOW_LITRES_PER_MINUTE;
            boolean vented = tag.contains(TAG_GIANT_HYDRAULIC_VENTED, Tag.TAG_BYTE)
                    && tag.getBoolean(TAG_GIANT_HYDRAULIC_VENTED);
            int target = tag.contains(TAG_GIANT_HYDRAULIC_TARGET_LENGTH, Tag.TAG_INT)
                    ? tag.getInt(TAG_GIANT_HYDRAULIC_TARGET_LENGTH)
                    : Math.max(30, expectedLengthTenths);
            int pressure = tag.contains(TAG_GIANT_HYDRAULIC_PRESSURE, Tag.TAG_INT)
                    ? tag.getInt(TAG_GIANT_HYDRAULIC_PRESSURE)
                    : GiantHydraulicSettingsState.DEFAULT_PRESSURE_BAR;
            int redstoneMin = tag.contains(TAG_GIANT_HYDRAULIC_REDSTONE_MIN, Tag.TAG_INT)
                    ? tag.getInt(TAG_GIANT_HYDRAULIC_REDSTONE_MIN)
                    : 30;
            int redstoneMax = tag.contains(TAG_GIANT_HYDRAULIC_REDSTONE_MAX, Tag.TAG_INT)
                    ? tag.getInt(TAG_GIANT_HYDRAULIC_REDSTONE_MAX)
                    : getMaxExpectedLengthTenths();
            this.giantHydraulicSettings.applyBase(flow, vented, target, pressure);
            this.giantHydraulicSettings.applyRedstoneRange(redstoneMin, redstoneMax);
        }
        this.linkStrainEffect = tag.contains(TAG_LINK_STRAIN_EFFECT, Tag.TAG_FLOAT)
                ? tag.getFloat(TAG_LINK_STRAIN_EFFECT)
                : 0.0F;
        this.linkWasStrained = tag.contains(TAG_LINK_STRAINED, Tag.TAG_BYTE)
                ? tag.getBoolean(TAG_LINK_STRAINED)
                : this.linkStrainEffect > 0.0F;
        this.hingeSubLevelId = tag.hasUUID(TAG_HINGE_SUB_LEVEL) ? tag.getUUID(TAG_HINGE_SUB_LEVEL) : null;
        this.hingeLinkPos = tag.contains(TAG_HINGE_LINK_POS, Tag.TAG_INT_ARRAY)
                ? NbtUtils.readBlockPos(tag, TAG_HINGE_LINK_POS).orElse(null)
                : null;
        this.hingeParentSubLevelId = tag.hasUUID(TAG_HINGE_PARENT_SUB_LEVEL)
                ? tag.getUUID(TAG_HINGE_PARENT_SUB_LEVEL)
                : null;
        this.hingeOwnerPos = tag.contains(TAG_HINGE_OWNER_POS, Tag.TAG_INT_ARRAY)
                ? NbtUtils.readBlockPos(tag, TAG_HINGE_OWNER_POS).orElse(null)
                : null;
        this.hingeMinAngle = clampHingeAngle(tag.contains(TAG_HINGE_MIN_ANGLE, Tag.TAG_INT)
                ? tag.getInt(TAG_HINGE_MIN_ANGLE) : DEFAULT_HINGE_MIN_ANGLE);
        this.hingeMaxAngle = clampHingeAngle(tag.contains(TAG_HINGE_MAX_ANGLE, Tag.TAG_INT)
                ? tag.getInt(TAG_HINGE_MAX_ANGLE) : DEFAULT_HINGE_MAX_ANGLE);
        if (this.hingeMaxAngle <= this.hingeMinAngle) {
            this.hingeMaxAngle = Math.min(180, this.hingeMinAngle + 1);
        }
        this.linkStrainEffectDelay = this.linkWasStrained || this.linkStrainEffect >= 0.0F
                ? 0
                : LINK_STRAIN_RECOVERY_TICKS;
        super.read(tag, registries, clientPacket);

        if (!Objects.equals(oldLinkedPos, this.linkedPos)
                || !Objects.equals(oldLinkedSubLevelId, this.linkedSubLevelId)
                || oldHasRenderOwnerPreference != this.hasRenderOwnerPreference
                || oldRenderOwner != this.renderOwner
                || oldGiantHydraulicLink != this.giantHydraulicLink
                || !Objects.equals(oldHingeSubLevelId, this.hingeSubLevelId)
                || !Objects.equals(oldHingeLinkPos, this.hingeLinkPos)
                || !Objects.equals(oldHingeOwnerPos, this.hingeOwnerPos)
                || !Objects.equals(oldHingeParentSubLevelId, this.hingeParentSubLevelId)) {
            this.clearCachedLinkedHead();
            this.invalidateRenderBoundingBox();
        }
    }

    public ItemInteractionResult handleHeldHydraulicRodItem(Player player, InteractionHand hand) {
        if (this.level == null) {
            return ItemInteractionResult.SUCCESS;
        }

        if (this.level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        Optional<JointBindingData.Selection> selection = PendingHydraulicSelections.read(player);
        if (player.isShiftKeyDown() && selection.isPresent()) {
            PendingHydraulicSelections.clear(player);
            syncSelectionToClient(player, null);
            player.displayClientMessage(Component.translatable("message.aeronautics_utility_objects.selection_cleared"), true);
            return ItemInteractionResult.SUCCESS;
        }

        if (selection.isEmpty()) {
            HydraulicRodItem rodItem = player.getItemInHand(hand).getItem() instanceof HydraulicRodItem item
                    ? item : null;
            JointBindingData.Selection newSelection = new JointBindingData.Selection(
                    this.level.dimension().location(),
                    this.worldPosition,
                    this.getContainingSubLevelId(),
                    rodItem != null && rodItem.isCreative(),
                    rodItem != null && rodItem.isGiant());
            PendingHydraulicSelections.write(player, newSelection);
            syncSelectionToClient(player, newSelection);
            player.displayClientMessage(Component.translatable("message.aeronautics_utility_objects.endpoint_selected"), true);
            return ItemInteractionResult.SUCCESS;
        }

        JointBindingData.Selection storedSelection = selection.get();
        JointBindingData.Selection stored = this.resolveReferenceFast(storedSelection.pos(), storedSelection.subLevelId()) != null
                ? storedSelection
                : RecentMoveRemapper.remap(this.level, storedSelection).orElse(storedSelection);
        if (!stored.equals(storedSelection)) {
            PendingHydraulicSelections.write(player, stored);
            syncSelectionToClient(player, stored);
        }
        HydraulicRodItem rodItem = player.getItemInHand(hand).getItem() instanceof HydraulicRodItem item
                ? item : null;
        LinkResult result = this.linkToSelection(stored,
                (rodItem != null && rodItem.isCreative()) || stored.creativeHydraulic(),
                (rodItem != null && rodItem.isGiant()) || stored.giantHydraulic());
        if (result.clearsSelection()) {
            PendingHydraulicSelections.clear(player);
            syncSelectionToClient(player, null);
            if (result.consumesRod() && !player.hasInfiniteMaterials()) {
                ItemStack heldStack = player.getItemInHand(hand);
                heldStack.shrink(1);
                player.setItemInHand(hand, heldStack.isEmpty() ? ItemStack.EMPTY : heldStack);
            }
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        }
        player.displayClientMessage(result.message(), true);
        return ItemInteractionResult.SUCCESS;
    }

    public LinkResult linkToSelection(JointBindingData.Selection selection) {
        return this.linkToSelection(selection, selection.creativeHydraulic(), selection.giantHydraulic());
    }

    public LinkResult linkToSelection(JointBindingData.Selection selection, boolean creativeLink) {
        return this.linkToSelection(selection, creativeLink, selection.giantHydraulic());
    }

    public LinkResult linkToSelection(JointBindingData.Selection selection, boolean creativeLink,
                                      boolean giantHydraulicLink) {
        if (this.level == null) {
            return LinkResult.TARGET_UNAVAILABLE;
        }
        if (!this.level.dimension().location().equals(selection.dimensionId())) {
            return LinkResult.WRONG_DIMENSION;
        }

        HydraulicConnectionHeadBlockEntity other = this.resolveReference(selection.pos(), selection.subLevelId());
        if (other == null) {
            return LinkResult.TARGET_UNAVAILABLE;
        }

        return this.createMutualLink(other, creativeLink, giantHydraulicLink);
    }

    public LinkResult createMutualLink(HydraulicConnectionHeadBlockEntity other) {
        return this.createMutualLink(other, false);
    }

    public LinkResult createMutualLink(HydraulicConnectionHeadBlockEntity other, boolean creativeLink) {
        return this.createMutualLink(other, creativeLink, false);
    }

    public LinkResult createMutualLink(HydraulicConnectionHeadBlockEntity other, boolean creativeLink,
                                      boolean giantHydraulicLink) {
        if (other == this) {
            return LinkResult.SELF;
        }

        if (this.level == null || other.level == null || !this.level.dimension().equals(other.level.dimension())) {
            return LinkResult.WRONG_DIMENSION;
        }

        if (!this.isAllowedLinkPair(other)) {
            return LinkResult.SAME_STRUCTURE;
        }

        if (!this.isWithinLinkRange(other)) {
            return LinkResult.TOO_FAR;
        }

        if (!this.isAlignedForLink(other)) {
            return LinkResult.NOT_ALIGNED;
        }

        if (this.references(other) && other.references(this)) {
            return LinkResult.ALREADY_LINKED;
        }

        this.clearLinkInternal(true);
        other.clearLinkInternal(true);

        if (!this.ensureHingeAssemblyForLink() || !other.ensureHingeAssemblyForLink()) {
            this.disassembleHingeAssembly();
            other.disassembleHingeAssembly();
            return LinkResult.TARGET_UNAVAILABLE;
        }
        this.alignHingeAssemblyForLink(other);
        other.alignHingeAssemblyForLink(this);

        this.applyLinkReference(other.worldPosition, other.getContainingSubLevelId(), false, true,
                creativeLink, giantHydraulicLink);
        other.applyLinkReference(this.worldPosition, this.getContainingSubLevelId(), true, true,
                creativeLink, giantHydraulicLink);
        this.cacheLinkedHead(other);
        other.cacheLinkedHead(this);
        this.initializeExpectedLength(other);
        return LinkResult.SUCCESS;
    }

    public void detachLink() {
        this.detachLink(true);
    }

    public void detachLinkWithoutDrop() {
        this.detachLink(false);
    }

    public void detachLinkForBlockRemoval() {
        this.detachingForBlockRemoval = true;
        this.detachLink(true);
    }

    public void preserveLinkForSubLevelMove() {
        this.disassembleHingeAssembly();
        this.preservingLinkForSubLevelMove = true;
    }

    public boolean hasLink() {
        return this.linkedPos != null;
    }

    @Nullable
    public BlockPos getLinkedPos() {
        return this.linkedPos;
    }

    @Nullable
    public UUID getLinkedSubLevelId() {
        return this.linkedSubLevelId;
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new HydraulicConnectionHeadMenu(containerId, inventory, this, false);
    }

    @Override
    public Component getDisplayName() {
        return this.getBlockState().getBlock().getName();
    }

    @Nullable
    public HydraulicConnectionHeadBlockEntity getLoadedLinkedConnectionHead() {
        return this.resolveLinkedConnectionHead();
    }

    @Override
    public @Nullable SubLevelLinkedEndpoint getLoadedLinkedEndpoint() {
        return this.getLoadedLinkedConnectionHead();
    }

    public boolean isHingedHead() {
        return HydraulicHingeHeadBlock.isHinged(this.getBlockState());
    }

    public boolean isBrassHingeHead() {
        return this.isHingedHead() && BrassHydraulicHingeHeadBlock.isBrass(this.getBlockState());
    }

    public int getHingeMinAngle() {
        return this.hingeMinAngle;
    }

    public int getHingeMaxAngle() {
        return this.hingeMaxAngle;
    }

    public void setHingeAngleLimits(int minAngle, int maxAngle) {
        if (!this.isBrassHingeHead()) {
            return;
        }
        int min = clampHingeAngle(minAngle);
        int max = clampHingeAngle(maxAngle);
        if (max <= min) {
            max = Math.min(180, min + 1);
            if (max <= min) {
                min = Math.max(-180, max - 1);
            }
        }
        if (this.hingeMinAngle == min && this.hingeMaxAngle == max) {
            return;
        }
        this.hingeMinAngle = min;
        this.hingeMaxAngle = max;
        this.setChanged();
        this.sendData();
        this.refreshHingeMotor();
    }

    private static int clampHingeAngle(int angle) {
        return Math.max(-180, Math.min(180, angle));
    }

    public boolean isCreativeLink() {
        return this.creativeLink;
    }

    public boolean isGiantHydraulicLink() {
        return this.giantHydraulicLink;
    }

    @Nullable
    public UUID getHingeSubLevelId() {
        return this.isHingeAssemblyOwnedByCurrentParent() ? this.hingeSubLevelId : null;
    }

    @Nullable
    public BlockPos getHingeLinkPos() {
        return this.isHingeAssemblyOwnedByCurrentParent() ? this.hingeLinkPos : null;
    }

    public void updateReferenceTo(BlockPos newPos, @Nullable UUID newSubLevelId) {
        if (this.linkedPos == null) {
            return;
        }
        this.applyLinkReference(newPos, newSubLevelId);
    }

    public void remapLinkedReferenceAfterSubLevelMove() {
        if (this.level == null || this.linkedPos == null) {
            return;
        }

        Optional<JointBindingData.Selection> remapped = RecentMoveRemapper.remap(this.level, this.linkedSelection());
        if (remapped.isPresent() && !this.matchesSelection(remapped.get())) {
            this.applyLinkReference(remapped.get().pos(), remapped.get().subLevelId());
        }
    }

    public boolean matchesSelection(JointBindingData.Selection selection) {
        return this.level != null
                && this.level.dimension().location().equals(selection.dimensionId())
                && this.worldPosition.equals(selection.pos())
                && Objects.equals(this.getContainingSubLevelId(), selection.subLevelId());
    }

    public LinkResult previewLinkToSelection(JointBindingData.Selection selection) {
        if (this.level == null) {
            return LinkResult.TARGET_UNAVAILABLE;
        }
        if (!this.level.dimension().location().equals(selection.dimensionId())) {
            return LinkResult.WRONG_DIMENSION;
        }

        HydraulicConnectionHeadBlockEntity other = this.resolveReference(selection.pos(), selection.subLevelId());
        if (other == null) {
            return LinkResult.TARGET_UNAVAILABLE;
        }
        if (other == this) {
            return LinkResult.SELF;
        }
        if (other.level == null || !this.level.dimension().equals(other.level.dimension())) {
            return LinkResult.WRONG_DIMENSION;
        }
        if (!this.isAllowedLinkPair(other)) {
            return LinkResult.SAME_STRUCTURE;
        }
        if (!this.isWithinLinkRange(other)) {
            return LinkResult.TOO_FAR;
        }
        if (!this.isAlignedForLink(other)) {
            return LinkResult.NOT_ALIGNED;
        }
        if (this.references(other) && other.references(this)) {
            return LinkResult.ALREADY_LINKED;
        }
        return LinkResult.SUCCESS;
    }

    public boolean references(HydraulicConnectionHeadBlockEntity other) {
        return this.linkedPos != null
                && this.linkedPos.equals(other.worldPosition)
                && Objects.equals(this.linkedSubLevelId, other.getContainingSubLevelId());
    }

    public boolean shouldRenderConnectionTo(HydraulicConnectionHeadBlockEntity other) {
        if (this.hasRenderOwnerPreference && other.hasRenderOwnerPreference
                && this.renderOwner != other.renderOwner) {
            return this.renderOwner;
        }

        if (this.hasRenderOwnerPreference != other.hasRenderOwnerPreference) {
            return this.hasRenderOwnerPreference ? this.renderOwner : !other.renderOwner;
        }

        return this.isCoordinateRenderOwnerComparedTo(other);
    }

    @Nullable
    public UUID getContainingSubLevelId() {
        if (this.level == null) {
            return null;
        }
        return SubLevelReferenceHelper.findContainingSubLevelId(this.level, this.worldPosition);
    }

    @Override
    public @Nullable Iterable<SubLevel> sable$getConnectionDependencies() {
        if (this.level == null || this.linkedPos == null) {
            return null;
        }

        List<SubLevel> dependencies = new ArrayList<>(3);
        this.addDependency(dependencies, this.getOwnedHingeSubLevel());

        HydraulicConnectionHeadBlockEntity other = this.resolveLinkedConnectionHead();
        if (other != null) {
            this.addDependency(dependencies, other.getRodConstraintSubLevel());
            this.addDependency(dependencies, other.getOwnedHingeSubLevel());
        } else if (this.linkedSubLevelId != null) {
            this.addDependency(dependencies, SubLevelContainer.getContainer(this.level).getSubLevel(this.linkedSubLevelId));
        }

        return dependencies.isEmpty() ? null : dependencies;
    }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
        if (this.level == null || this.level.isClientSide || this.linkedPos == null) {
            return;
        }

        if (!Objects.equals(this.getContainingSubLevelId(), subLevel.getUniqueId())) {
            return;
        }

        HydraulicConnectionHeadBlockEntity other = this.resolveLinkedConnectionHead();
        if (other == null || !other.references(this)) {
            return;
        }

        if (!this.ensureHingeAssemblyForLink()) {
            this.removeConstraintHandle();
            return;
        }

        if (!this.shouldControlConstraint(other)) {
            this.removeConstraintHandle();
            return;
        }

        if (!other.ensureHingeAssemblyForLink()) {
            this.removeConstraintHandle();
            return;
        }

        if (this.linkWarmupTicks > 0) {
            this.linkWarmupTicks--;
            this.removeConstraintHandle();
            return;
        }

        ServerSubLevel otherContainingSubLevel = other.getContainingServerSubLevel();
        ServerSubLevel ownSubLevel = this.getRodConstraintServerSubLevel(subLevel);
        ServerSubLevel otherSubLevel = other.getRodConstraintServerSubLevel(otherContainingSubLevel);
        if (ownSubLevel == null) {
            this.removeConstraintHandle();
            return;
        }

        RigidBodyHandle ownHandle = ownSubLevel == subLevel ? handle : RigidBodyHandle.of(ownSubLevel);
        if (ownHandle == null || !ownHandle.isValid()) {
            this.removeConstraintHandle();
            return;
        }

        Vector3d ownLocal = this.getRodConstraintLocalAnchor();
        Vector3d otherLocal = other.getRodConstraintLocalAnchor();
        Vector3d ownWorld = this.toWorldPosition(ownSubLevel, ownLocal);
        Vector3d otherWorld = this.toWorldPosition(otherSubLevel, otherLocal);

        Vector3d worldDirection = this.getLengthControlDirection(ownSubLevel, ownWorld, otherWorld);
        if (worldDirection.lengthSquared() < 1.0E-8D) {
            this.removeConstraintHandle();
            return;
        }

        Vector3d ownLocalAxis = this.getLocalFacingVector();
        Vector3d otherLocalAxis = other.getLocalFacingVector().negate(new Vector3d());
        if (ownLocalAxis.lengthSquared() < 1.0E-8D || otherLocalAxis.lengthSquared() < 1.0E-8D) {
            this.removeConstraintHandle();
            return;
        }

        Quaterniond ownOrientation = this.frameOrientation(ownLocalAxis);
        Quaterniond otherBaseOrientation = other.frameOrientation(otherLocalAxis);
        if (!Double.isFinite(this.snappedConnectionRollOffset)) {
            this.snappedConnectionRollOffset = this.snapConnectionRollOffset(
                    ownSubLevel, otherSubLevel, ownOrientation, otherBaseOrientation);
        }
        Quaterniond otherOrientation = new Quaterniond(otherBaseOrientation)
                .mul(new Quaterniond().rotationY(this.snappedConnectionRollOffset));
        if (this.constraintHandle == null || !this.constraintHandle.isValid()) {
            this.removeConstraintHandle();
            this.constraintHandle = SubLevelPhysicsSystem.require(this.level).getPipeline().addConstraint(
                    ownSubLevel, otherSubLevel,
                    new GenericConstraintConfiguration(ownLocal, otherLocal, ownOrientation, otherOrientation, LOCKED_AXES));
            if (this.constraintHandle == null) {
                return;
            }
        } else {
            this.constraintHandle.setFrame1(ownLocal, ownOrientation);
            this.constraintHandle.setFrame2(otherLocal, otherOrientation);
        }
        if (!this.creativeLink && !this.giantHydraulicLink) {
            this.refreshRodDampingMotor();
        }

        this.applyLengthLimits(ownSubLevel, ownHandle, ownLocal, ownWorld,
                otherSubLevel, otherLocal, otherWorld, worldDirection, timeStep);
    }

    private void refreshRodDampingMotor() {
        if (this.constraintHandle == null || !this.constraintHandle.isValid()) {
            return;
        }

        if (this.creativeLink) {
            double target = Double.isFinite(this.effectiveExpectedLengthBlocks)
                    ? this.effectiveExpectedLengthBlocks
                    : this.settings.expectedLengthTenths() / 10.0D;
            this.constraintHandle.setMotor(ConstraintJointAxis.LINEAR_Y, target,
                    CREATIVE_LENGTH_SERVO_STIFFNESS, CREATIVE_LENGTH_SERVO_DAMPING, true,
                    CREATIVE_LENGTH_MAX_FORCE);
            return;
        }

        int stretchResistance = this.getStretchResistance();
        double damping = Math.max(0.0D,
                stretchResistance * AeroUniversalJointConfig.hydraulicRodStretchDampingPerUnit());
        double maxForce = Math.max(0.0D,
                stretchResistance * AeroUniversalJointConfig.hydraulicRodStretchMaxForcePerUnit());
        this.constraintHandle.setMotor(ConstraintJointAxis.LINEAR_Y, 0.0D, 0.0D, damping, true, maxForce);
    }

    private Vector3d getRodConstraintLocalAnchor() {
        return this.isHingedHead() && this.hingeLinkPos != null
                ? this.localCenterOf(this.hingeLinkPos)
                : this.localCenterOf(this.worldPosition);
    }

    @Nullable
    private ServerSubLevel getRodConstraintServerSubLevel(@Nullable ServerSubLevel containingSubLevel) {
        if (this.isHingedHead()) {
            ServerSubLevel hingeSubLevel = this.getOwnedHingeServerSubLevel();
            if (hingeSubLevel != null) {
                return hingeSubLevel;
            }
        }
        return containingSubLevel != null ? containingSubLevel : this.getContainingServerSubLevel();
    }

    @Nullable
    private SubLevel getRodConstraintSubLevel() {
        if (this.isHingedHead()) {
            SubLevel hingeSubLevel = this.getOwnedHingeSubLevel();
            if (hingeSubLevel != null) {
                return hingeSubLevel;
            }
        }
        return this.getContainingSubLevel();
    }

    private Vector3d getLengthControlDirection(ServerSubLevel ownSubLevel, Vector3d ownWorld, Vector3d otherWorld) {
        Vector3d connection = otherWorld.sub(ownWorld, new Vector3d());
        if (normalizeIfUsable(connection)) {
            return connection;
        }

        return this.getWorldFacingVector(ownSubLevel);
    }

    @Override
    protected AABB createRenderBoundingBox() {
        AABB base = super.createRenderBoundingBox();
        if (this.linkedPos == null) {
            return base;
        }

        if (this.getContainingSubLevelId() != null || this.linkedSubLevelId != null) {
            double radius = AeroUniversalJointConfig.hydraulicRodBreakLinkLength() + 1.0D;
            double diameter = radius * 2.0D;
            return AABB.ofSize(Vec3.atCenterOf(this.worldPosition), diameter, diameter, diameter);
        }

        return base.minmax(AABB.ofSize(Vec3.atCenterOf(this.linkedPos), 1.0D, 1.0D, 1.0D)).inflate(1.0D);
    }

    private boolean isAllowedLinkPair(HydraulicConnectionHeadBlockEntity other) {
        UUID ownSubLevelId = this.getContainingSubLevelId();
        UUID otherSubLevelId = other.getContainingSubLevelId();
        if (ownSubLevelId == null && otherSubLevelId == null) {
            return false;
        }
        return !Objects.equals(ownSubLevelId, otherSubLevelId);
    }

    private boolean isWithinLinkRange(HydraulicConnectionHeadBlockEntity other) {
        double breakLength = AeroUniversalJointConfig.hydraulicRodBreakLinkLength();
        return this.distanceSquaredTo(other) <= breakLength * breakLength;
    }

    private boolean isAlignedForLink(HydraulicConnectionHeadBlockEntity other) {
        SubLevel ownSubLevel = this.getContainingSubLevel();
        SubLevel otherSubLevel = other.getContainingSubLevel();
        Vector3d ownWorld = this.toWorldPosition(ownSubLevel, this.localCenterOf(this.worldPosition));
        Vector3d otherWorld = this.toWorldPosition(otherSubLevel, this.localCenterOf(other.worldPosition));
        Vector3d connection = otherWorld.sub(ownWorld, new Vector3d());
        if (connection.lengthSquared() < MIN_PHYSICS_DISTANCE * MIN_PHYSICS_DISTANCE) {
            return false;
        }

        Vector3d direction = connection.normalize();
        Vector3d ownFacing = this.getWorldFacingVector(ownSubLevel);
        Vector3d otherFacing = other.getWorldFacingVector(otherSubLevel);
        if (this.isHingedHead() || other.isHingedHead()) {
            return this.isHingeEndpointAbleToFace(direction, ownSubLevel)
                    && other.isHingeEndpointAbleToFace(direction.negate(new Vector3d()), otherSubLevel);
        }

        return ownFacing.dot(direction) >= ALIGNMENT_DOT_THRESHOLD
                && otherFacing.dot(direction) <= -ALIGNMENT_DOT_THRESHOLD
                && ownFacing.dot(otherFacing) <= -ALIGNMENT_DOT_THRESHOLD;
    }

    private boolean isHingeEndpointAbleToFace(Vector3d targetDirection, @Nullable SubLevel subLevel) {
        if (!this.isHingedHead()) {
            Vector3d facing = this.getWorldFacingVector(subLevel);
            return facing.dot(targetDirection) >= ALIGNMENT_DOT_THRESHOLD;
        }

        Vector3d axis = this.getWorldHingeAxis(subLevel);
        if (axis.lengthSquared() < 1.0E-8D) {
            return false;
        }
        Vector3d target = new Vector3d(targetDirection);
        if (target.lengthSquared() < 1.0E-8D) {
            return false;
        }
        axis.normalize();
        target.normalize();

        double alongAxis = Math.abs(target.dot(axis));
        if (alongAxis > HINGED_AXIS_ALIGNMENT_THRESHOLD) {
            return false;
        }

        return true;
    }

    @Nullable
    private HydraulicConnectionHeadBlockEntity resolveLinkedConnectionHead() {
        if (this.linkedPos == null) {
            this.clearCachedLinkedHead();
            return null;
        }

        HydraulicConnectionHeadBlockEntity cached = this.cachedLinkedHead;
        if (this.isCachedLinkedHeadValid(cached)) {
            return cached;
        }

        HydraulicConnectionHeadBlockEntity resolved = this.resolveReference(this.linkedPos, this.linkedSubLevelId);
        this.cacheLinkedHead(resolved);
        return resolved;
    }

    private JointBindingData.Selection linkedSelection() {
        return new JointBindingData.Selection(this.level.dimension().location(), Objects.requireNonNull(this.linkedPos),
                this.linkedSubLevelId, this.creativeLink, this.giantHydraulicLink);
    }

    private void validateLinkState(boolean remapReference) {
        if (this.level == null || this.level.isClientSide || this.linkedPos == null) {
            return;
        }

        HydraulicConnectionHeadBlockEntity other = this.resolveLinkedConnectionHead();
        if (other == null && remapReference) {
            Optional<JointBindingData.Selection> remapped = RecentMoveRemapper.remap(this.level, this.linkedSelection());
            if (remapped.isPresent() && !this.matchesSelection(remapped.get())) {
                this.applyLinkReference(remapped.get().pos(), remapped.get().subLevelId());
                other = this.resolveLinkedConnectionHead();
            }
        }

        if (other == null || !other.references(this)) {
            if (!remapReference) {
                return;
            }
            this.clearLinkInternal(false);
            return;
        }

        if (!this.isAllowedLinkPair(other)) {
            this.detachLink();
        }
    }

    @Nullable
    private HydraulicConnectionHeadBlockEntity resolveReference(BlockPos pos, @Nullable UUID subLevelId) {
        if (this.level == null) {
            return null;
        }

        BlockEntity blockEntity = SubLevelReferenceHelper.resolveBlockEntity(this.level, pos, subLevelId);
        return blockEntity instanceof HydraulicConnectionHeadBlockEntity head ? head : null;
    }

    @Nullable
    private HydraulicConnectionHeadBlockEntity resolveReferenceFast(BlockPos pos, @Nullable UUID subLevelId) {
        if (this.level == null) {
            return null;
        }

        BlockEntity blockEntity = SubLevelReferenceHelper.resolveBlockEntityFast(this.level, pos, subLevelId);
        return blockEntity instanceof HydraulicConnectionHeadBlockEntity head ? head : null;
    }

    private boolean isCachedLinkedHeadValid(@Nullable HydraulicConnectionHeadBlockEntity head) {
        return head != null
                && this.level != null
                && !head.isRemoved()
                && head.getLevel() == this.level
                && this.linkedPos != null
                && this.linkedPos.equals(this.cachedLinkedHeadPos)
                && this.linkedPos.equals(head.worldPosition)
                && Objects.equals(this.linkedSubLevelId, this.cachedLinkedHeadSubLevelId)
                && Objects.equals(this.linkedSubLevelId, head.getContainingSubLevelId());
    }

    private void cacheLinkedHead(@Nullable HydraulicConnectionHeadBlockEntity head) {
        this.cachedLinkedHead = head;
        this.cachedLinkedHeadPos = this.linkedPos;
        this.cachedLinkedHeadSubLevelId = this.linkedSubLevelId;
    }

    private void clearCachedLinkedHead() {
        this.cachedLinkedHead = null;
        this.cachedLinkedHeadPos = null;
        this.cachedLinkedHeadSubLevelId = null;
    }

    private boolean shouldControlConstraint(HydraulicConnectionHeadBlockEntity other) {
        UUID ownSubLevelId = this.getContainingSubLevelId();
        UUID otherSubLevelId = other.getContainingSubLevelId();
        if (ownSubLevelId == null) {
            return false;
        }
        if (otherSubLevelId == null) {
            return true;
        }
        if (ownSubLevelId.equals(otherSubLevelId)) {
            return false;
        }
        return SubLevelReferenceHelper.compareNullableUuids(ownSubLevelId, otherSubLevelId) < 0;
    }

    @Nullable
    private ServerSubLevel getContainingServerSubLevel() {
        if (this.level == null) {
            return null;
        }

        SubLevel subLevel = this.getContainingSubLevel();
        return subLevel instanceof ServerSubLevel serverSubLevel ? serverSubLevel : null;
    }

    @Nullable
    private SubLevel getContainingSubLevel() {
        return this.level == null ? null : Sable.HELPER.getContaining(this);
    }

    public boolean isSettingsInteractionValid(Player player) {
        if (this.level == null || this.level != player.level() || this.isRemoved()) {
            return false;
        }
        Vec3 ownAnchor = this.getSettingsWorldAnchor();
        Vec3 otherAnchor = ownAnchor;
        HydraulicConnectionHeadBlockEntity other = this.getLoadedLinkedConnectionHead();
        if (other != null && other != this) {
            otherAnchor = other.getSettingsWorldAnchor();
        }
        Vec3 eye = player.getEyePosition();
        double interactionRange = player.blockInteractionRange();
        return distanceToSegmentSqr(eye, ownAnchor, otherAnchor)
                <= interactionRange * interactionRange;
    }

    private Vec3 getSettingsWorldAnchor() {
        Vector3d world = this.toWorldPosition(this.getRodConstraintSubLevel(), this.getRodConstraintLocalAnchor());
        return new Vec3(world.x, world.y, world.z);
    }

    private static double distanceToSegmentSqr(Vec3 point, Vec3 start, Vec3 end) {
        Vec3 segment = end.subtract(start);
        double lengthSqr = segment.lengthSqr();
        if (lengthSqr < 1.0E-8D) {
            return point.distanceToSqr(start);
        }
        double t = point.subtract(start).dot(segment) / lengthSqr;
        t = Math.max(0.0D, Math.min(1.0D, t));
        return point.distanceToSqr(start.add(segment.scale(t)));
    }

    private boolean ensureHingeAssemblyForLink() {
        if (!this.isHingedHead()) {
            this.disassembleHingeAssembly();
            return true;
        }
        if (this.level == null || this.level.isClientSide) {
            return this.hingeSubLevelId != null && this.hingeLinkPos != null;
        }

        if (!this.isHingeAssemblyOwnedByCurrentParent()) {
            this.discardForeignHingeAssemblyReference();
        }
        ServerSubLevel hingeSubLevel = this.getHingeServerSubLevel();
        if (hingeSubLevel == null || this.hingeLinkPos == null) {
            this.removeHingeConstraintHandle();
            this.hingeSubLevelId = null;
            this.hingeLinkPos = null;
            this.hingeParentSubLevelId = null;
            this.hingeOwnerPos = null;
            this.hingeConstraintAxis = null;
            hingeSubLevel = this.createHingeLinkSubLevel();
            if (hingeSubLevel == null) {
                return false;
            }
        }

        return this.attachOrRefreshHingeConstraint(hingeSubLevel);
    }

    @Nullable
    private ServerSubLevel createHingeLinkSubLevel() {
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return null;
        }

        ServerSubLevelContainer container = SubLevelContainer.getContainer(serverLevel);
        Pose3d pose = new Pose3d();
        pose.position().set(
                this.worldPosition.getX() + 0.5D,
                this.worldPosition.getY() + 0.5D,
                this.worldPosition.getZ() + 0.5D);

        ServerSubLevel hingeSubLevel = (ServerSubLevel) container.allocateNewSubLevel(pose);
        ServerLevelPlot plot = hingeSubLevel.getPlot();
        plot.newEmptyChunk(plot.getCenterChunk());
        plot.getEmbeddedLevelAccessor().setBlock(BlockPos.ZERO, ModBlocks.HYDRAULIC_HINGE_LINK.get().defaultBlockState(), 3);

        BlockPos plotCenter = plot.getCenterBlock();
        Vector3d position = JOMLConversion.atLowerCornerOf(this.worldPosition);
        org.joml.Vector3dc centerOfMass = hingeSubLevel.getMassTracker().getCenterOfMass();
        if (centerOfMass != null) {
            position.add(
                    centerOfMass.x() - plotCenter.getX(),
                    centerOfMass.y() - plotCenter.getY(),
                    centerOfMass.z() - plotCenter.getZ());
        } else {
            hingeSubLevel.logicalPose().rotationPoint().set(
                    plotCenter.getX() + 0.5D,
                    plotCenter.getY() + 0.5D,
                    plotCenter.getZ() + 0.5D);
        }
        hingeSubLevel.logicalPose().position().set(position);

        SubLevelPhysicsSystem physicsSystem = container.physicsSystem();
        PhysicsPipeline pipeline = physicsSystem.getPipeline();
        SubLevel containing = this.getContainingSubLevel();
        this.hingeParentSubLevelId = containing != null ? containing.getUniqueId() : null;
        this.hingeOwnerPos = this.worldPosition.immutable();
        if (containing != null) {
            hingeSubLevel.logicalPose().orientation().set(containing.logicalPose().orientation());
            SubLevelAssemblyHelper.kickFromContainingSubLevel(serverLevel, physicsSystem, pipeline, hingeSubLevel, containing);
        }
        pipeline.teleport(hingeSubLevel, hingeSubLevel.logicalPose().position(), hingeSubLevel.logicalPose().orientation());
        hingeSubLevel.updateLastPose();

        this.hingeSubLevelId = hingeSubLevel.getUniqueId();
        this.hingeLinkPos = plotCenter;
        this.invalidateRenderBoundingBox();
        this.setChanged();
        this.sendData();
        return hingeSubLevel;
    }

    private boolean attachOrRefreshHingeConstraint(ServerSubLevel hingeSubLevel) {
        if (!(this.level instanceof ServerLevel serverLevel) || this.hingeLinkPos == null) {
            return false;
        }

        Direction.Axis currentAxis = HydraulicHingeHeadBlock.getHingeAxis(this.getBlockState());
        if (this.hingeConstraintHandle != null
                && this.hingeConstraintHandle.isValid()
                && this.hingeConstraintAxis == currentAxis
                && this.isBrassHingeHead() == (this.hingeConstraintHandle instanceof GenericConstraintHandle)) {
            this.refreshHingeMotor();
            return true;
        }

        this.removeHingeConstraintHandle();
        Vector3d parentAnchor = this.localCenterOf(this.worldPosition);
        Vector3d linkAnchor = this.localCenterOf(this.hingeLinkPos);
        Vector3d parentAxis = axisVector(currentAxis);
        Vector3d linkAxis = axisVector(currentAxis);
        if (this.isBrassHingeHead()) {
            Quaterniond parentOrientation = this.frameOrientation(parentAxis);
            Quaterniond linkOrientation = this.frameOrientation(linkAxis);
            this.hingeConstraintHandle = SubLevelContainer.getContainer(serverLevel).physicsSystem().getPipeline().addConstraint(
                    this.getContainingServerSubLevel(), hingeSubLevel,
                    new GenericConstraintConfiguration(parentAnchor, linkAnchor, parentOrientation, linkOrientation,
                            HINGE_LIMITED_AXES));
        } else {
            this.hingeConstraintHandle = SubLevelContainer.getContainer(serverLevel).physicsSystem().getPipeline().addConstraint(
                    this.getContainingServerSubLevel(), hingeSubLevel,
                    new RotaryConstraintConfiguration(parentAnchor, linkAnchor, parentAxis, linkAxis));
        }
        if (this.hingeConstraintHandle == null) {
            return false;
        }

        this.hingeConstraintHandle.setContactsEnabled(false);
        this.refreshHingeMotor();
        this.hingeConstraintAxis = currentAxis;
        return true;
    }

    private void refreshHingeMotor() {
        if (this.hingeConstraintHandle == null || !this.hingeConstraintHandle.isValid()) {
            return;
        }

        this.hingeConstraintHandle.setMotor(
                this.isBrassHingeHead() ? ConstraintJointAxis.ANGULAR_Y : RotaryConstraintHandle.DEFAULT_AXIS,
                0.0D,
                0.0D,
                HINGE_FREE_SPIN_DAMPING,
                false,
                0.0D);
        if (this.isBrassHingeHead() && this.hingeConstraintHandle instanceof GenericConstraintHandle generic) {
            generic.setLimit(ConstraintJointAxis.ANGULAR_Y,
                    Math.toRadians(this.hingeMinAngle), Math.toRadians(this.hingeMaxAngle));
        }
    }

    private void alignHingeAssemblyForLink(HydraulicConnectionHeadBlockEntity other) {
        if (!this.isHingedHead() || !(this.level instanceof ServerLevel serverLevel)) {
            return;
        }

        ServerSubLevel hingeSubLevel = this.getHingeServerSubLevel();
        if (hingeSubLevel == null || this.hingeLinkPos == null) {
            return;
        }

        SubLevel ownContainingSubLevel = this.getContainingSubLevel();
        Vector3d ownAnchor = this.toWorldPosition(ownContainingSubLevel, this.localCenterOf(this.worldPosition));
        Vector3d otherAnchor = other.toWorldPosition(other.getRodConstraintSubLevel(), other.getRodConstraintLocalAnchor());
        Vector3d targetDirection = otherAnchor.sub(ownAnchor, new Vector3d());
        if (!normalizeIfUsable(targetDirection)) {
            return;
        }

        Vector3d hingeAxis = this.getWorldHingeAxis(ownContainingSubLevel);
        if (!normalizeIfUsable(hingeAxis) || !projectOntoPlane(targetDirection, hingeAxis)) {
            return;
        }

        Vector3d currentDirection = this.getWorldFacingVector(hingeSubLevel);
        if (!projectOntoPlane(currentDirection, hingeAxis)) {
            return;
        }

        double angle = Math.atan2(new Vector3d(currentDirection).cross(targetDirection).dot(hingeAxis),
                currentDirection.dot(targetDirection));
        if (!Double.isFinite(angle) || Math.abs(angle) <= 1.0E-6D) {
            return;
        }

        Quaterniond alignedOrientation = new Quaterniond()
                .rotationAxis(angle, hingeAxis.x(), hingeAxis.y(), hingeAxis.z())
                .mul(hingeSubLevel.logicalPose().orientation())
                .normalize();
        Vector3d localAnchor = this.localCenterOf(this.hingeLinkPos);
        Vector3d rotatedOffset = localAnchor.sub(hingeSubLevel.logicalPose().rotationPoint(), new Vector3d())
                .mul(hingeSubLevel.logicalPose().scale());
        alignedOrientation.transform(rotatedOffset);

        Vector3d alignedPosition = ownAnchor.sub(rotatedOffset, new Vector3d());
        hingeSubLevel.logicalPose().orientation().set(alignedOrientation);
        hingeSubLevel.logicalPose().position().set(alignedPosition);
        SubLevelContainer.getContainer(serverLevel)
                .physicsSystem()
                .getPipeline()
                .teleport(hingeSubLevel, alignedPosition, alignedOrientation);
        hingeSubLevel.updateLastPose();
    }

    @Nullable
    private SubLevel getHingeSubLevel() {
        if (this.level == null || this.hingeSubLevelId == null) {
            return null;
        }
        return SubLevelContainer.getContainer(this.level).getSubLevel(this.hingeSubLevelId);
    }

    @Nullable
    private ServerSubLevel getHingeServerSubLevel() {
        SubLevel subLevel = this.getHingeSubLevel();
        return subLevel instanceof ServerSubLevel serverSubLevel && !serverSubLevel.isRemoved() ? serverSubLevel : null;
    }

    @Nullable
    private SubLevel getOwnedHingeSubLevel() {
        return this.isHingeAssemblyOwnedByCurrentParent() ? this.getHingeSubLevel() : null;
    }

    @Nullable
    private ServerSubLevel getOwnedHingeServerSubLevel() {
        SubLevel subLevel = this.getOwnedHingeSubLevel();
        return subLevel instanceof ServerSubLevel serverSubLevel && !serverSubLevel.isRemoved() ? serverSubLevel : null;
    }

    private void validateHingeAssemblyOwner() {
        if (this.level == null || this.level.isClientSide || this.hingeSubLevelId == null) {
            return;
        }
        if (this.linkedPos == null || !this.isHingedHead()) {
            this.disassembleHingeAssembly();
            return;
        }
        ServerSubLevel hingeSubLevel = this.getHingeServerSubLevel();
        if (hingeSubLevel == null || this.hingeLinkPos == null) {
            this.disassembleHingeAssembly();
            return;
        }

        if (!this.isHingeAssemblyOwnedByCurrentParent()) {
            this.discardForeignHingeAssemblyReference();
            return;
        }

        Vector3d parentCenter = this.toWorldPosition(this.getContainingSubLevel(), this.localCenterOf(this.worldPosition));
        Vector3d hingeCenter = this.toWorldPosition(hingeSubLevel, this.localCenterOf(this.hingeLinkPos));
        if (parentCenter.sub(hingeCenter, new Vector3d()).lengthSquared() > HINGE_OWNER_MAX_DISTANCE_SQUARED) {
            this.disassembleHingeAssembly();
        }
    }

    private void disassembleHingeAssembly() {
        this.removeHingeConstraintHandle();
        if (this.level != null && !this.level.isClientSide && this.hingeSubLevelId != null) {
            SubLevel subLevel = SubLevelContainer.getContainer(this.level).getSubLevel(this.hingeSubLevelId);
            if (subLevel != null && !subLevel.isRemoved()) {
                SubLevelContainer.getContainer(this.level).removeSubLevel(subLevel, SubLevelRemovalReason.REMOVED);
            }
        }

        if (this.hingeSubLevelId != null || this.hingeLinkPos != null
                || this.hingeParentSubLevelId != null || this.hingeOwnerPos != null) {
            this.hingeSubLevelId = null;
            this.hingeLinkPos = null;
            this.hingeParentSubLevelId = null;
            this.hingeOwnerPos = null;
            this.hingeConstraintAxis = null;
            this.invalidateRenderBoundingBox();
            this.setChanged();
            this.sendData();
        }
    }

    private boolean isHingeAssemblyOwnedByCurrentParent() {
        UUID currentParentSubLevelId = this.getContainingSubLevelId();
        return this.hingeSubLevelId != null
                && this.hingeLinkPos != null
                && this.hingeOwnerPos != null
                && this.worldPosition.equals(this.hingeOwnerPos)
                && !Objects.equals(currentParentSubLevelId, this.hingeSubLevelId)
                && Objects.equals(currentParentSubLevelId, this.hingeParentSubLevelId);
    }

    /**
     * Drops a copied or stale runtime reference without touching the referenced sublevel.
     * The referenced sublevel may still belong to another loaded structure.
     */
    private void discardForeignHingeAssemblyReference() {
        this.removeHingeConstraintHandle();
        if (this.hingeSubLevelId == null && this.hingeLinkPos == null
                && this.hingeParentSubLevelId == null && this.hingeOwnerPos == null) {
            return;
        }

        this.hingeSubLevelId = null;
        this.hingeLinkPos = null;
        this.hingeParentSubLevelId = null;
        this.hingeOwnerPos = null;
        this.hingeConstraintAxis = null;
        this.invalidateRenderBoundingBox();
        this.setChanged();
        this.sendData();
    }

    private void removeHingeConstraintHandle() {
        if (this.hingeConstraintHandle != null) {
            if (this.hingeConstraintHandle.isValid()) {
                this.hingeConstraintHandle.remove();
            }
            this.hingeConstraintHandle = null;
        }
        this.hingeConstraintAxis = null;
    }

    private void addDependency(List<SubLevel> dependencies, @Nullable SubLevel subLevel) {
        if (subLevel == null || subLevel.isRemoved() || dependencies.contains(subLevel)) {
            return;
        }
        dependencies.add(subLevel);
    }

    private void applyLinkReference(BlockPos pos, @Nullable UUID subLevelId) {
        this.applyLinkReference(pos, subLevelId, this.renderOwner, this.hasRenderOwnerPreference,
                this.creativeLink, this.giantHydraulicLink);
    }

    private void applyLinkReference(BlockPos pos, @Nullable UUID subLevelId, boolean renderOwner, boolean hasRenderOwnerPreference) {
        this.applyLinkReference(pos, subLevelId, renderOwner, hasRenderOwnerPreference,
                this.creativeLink, this.giantHydraulicLink);
    }

    private void applyLinkReference(BlockPos pos, @Nullable UUID subLevelId, boolean renderOwner,
                                    boolean hasRenderOwnerPreference, boolean creativeLink,
                                    boolean giantHydraulicLink) {
        if (this.linkedPos != null
                && this.linkedPos.equals(pos)
                && Objects.equals(this.linkedSubLevelId, subLevelId)
                && this.renderOwner == renderOwner
                && this.hasRenderOwnerPreference == hasRenderOwnerPreference
                && this.creativeLink == creativeLink
                && this.giantHydraulicLink == giantHydraulicLink) {
            return;
        }

        this.linkedPos = pos.immutable();
        this.linkedSubLevelId = subLevelId;
        this.renderOwner = renderOwner;
        this.hasRenderOwnerPreference = hasRenderOwnerPreference;
        this.creativeLink = creativeLink;
        this.giantHydraulicLink = giantHydraulicLink;
        this.giantHydraulicPhysics = GiantHydraulicPhysics.State.empty();
        this.clearCachedLinkedHead();
        this.snappedConnectionRollOffset = Double.NaN;
        this.invalidateRenderBoundingBox();
        this.setChanged();
        this.sendData();
    }

    private boolean isCoordinateRenderOwnerComparedTo(HydraulicConnectionHeadBlockEntity other) {
        BlockPos ownPos = this.getBlockPos();
        BlockPos otherPos = other.getBlockPos();
        int byX = Integer.compare(ownPos.getX(), otherPos.getX());
        if (byX != 0) {
            return byX < 0;
        }

        int byY = Integer.compare(ownPos.getY(), otherPos.getY());
        if (byY != 0) {
            return byY < 0;
        }

        int byZ = Integer.compare(ownPos.getZ(), otherPos.getZ());
        if (byZ != 0) {
            return byZ < 0;
        }

        return SubLevelReferenceHelper.compareNullableUuids(this.getContainingSubLevelId(), other.getContainingSubLevelId()) <= 0;
    }

    private void initializeExpectedLength(HydraulicConnectionHeadBlockEntity other) {
        double currentLength = this.currentRodControlDistanceTo(other);
        if (!Double.isFinite(currentLength)) {
            currentLength = other.currentRodControlDistanceTo(this);
        }
        if (!Double.isFinite(currentLength)) {
            currentLength = Math.sqrt(this.distanceSquaredTo(other));
        }

        int currentLengthTenths = this.clampExpectedLengthTenthsForLink((int) Math.round(currentLength * 10.0D));
        this.setExpectedLengthTenths(currentLengthTenths);
        other.setExpectedLengthTenths(currentLengthTenths);
        double currentLengthBlocks = currentLengthTenths / 10.0D;
        this.effectiveExpectedLengthBlocks = currentLengthBlocks;
        other.effectiveExpectedLengthBlocks = currentLengthBlocks;
        this.expectedLengthTransitionPending = false;
        other.expectedLengthTransitionPending = false;
        this.effectiveReturnForce = 0.0D;
        other.effectiveReturnForce = 0.0D;
        this.linkWarmupTicks = LINK_WARMUP_TICKS;
        other.linkWarmupTicks = LINK_WARMUP_TICKS;
        int minimumLength = getMinExpectedLengthTenths(this.giantHydraulicLink);
        this.settings.applyRedstoneLengthRange(minimumLength, getMaxExpectedLengthTenths());
        other.settings.applyRedstoneLengthRange(minimumLength, getMaxExpectedLengthTenths());
        if (this.giantHydraulicLink) {
            this.giantHydraulicSettings.applyBase(GiantHydraulicSettingsState.DEFAULT_FLOW_LITRES_PER_MINUTE,
                    false, currentLengthTenths, GiantHydraulicSettingsState.DEFAULT_PRESSURE_BAR);
            other.giantHydraulicSettings.applyBase(GiantHydraulicSettingsState.DEFAULT_FLOW_LITRES_PER_MINUTE,
                    false, currentLengthTenths, GiantHydraulicSettingsState.DEFAULT_PRESSURE_BAR);
            this.giantHydraulicSettings.applyRedstoneRange(30, getMaxExpectedLengthTenths());
            other.giantHydraulicSettings.applyRedstoneRange(30, getMaxExpectedLengthTenths());
        }
    }

    private void clearLinkInternal(boolean updateOther) {
        BlockPos oldPos = this.linkedPos;
        UUID oldSubLevelId = this.linkedSubLevelId;
        HydraulicConnectionHeadBlockEntity linkedOther = null;
        if (updateOther && this.level != null && oldPos != null) {
            HydraulicConnectionHeadBlockEntity other = this.resolveReference(oldPos, oldSubLevelId);
            if (other != null && other.references(this)) {
                linkedOther = other;
                linkedOther.removeConstraintHandle();
            }
        }

        this.removeConstraintHandle();
        this.disassembleHingeAssembly();
        this.linkedPos = null;
        this.linkedSubLevelId = null;
        this.creativeLink = false;
        this.giantHydraulicLink = false;
        this.clearCachedLinkedHead();
        this.hasRenderOwnerPreference = false;
        this.renderOwner = false;
        this.expectedLengthApproachMultiplier = 1.0D;
        this.giantHydraulicPhysics = GiantHydraulicPhysics.State.empty();
        this.linkWarmupTicks = 0;
        this.snappedConnectionRollOffset = Double.NaN;
        this.invalidateRenderBoundingBox();
        this.setChanged();
        this.sendData();

        if (linkedOther != null) {
            linkedOther.clearLinkInternal(false);
        }
    }

    private void removeConstraintHandle() {
        if (this.constraintHandle != null) {
            if (this.constraintHandle.isValid()) {
                this.constraintHandle.remove();
            }
            this.constraintHandle = null;
        }
    }

    private void detachLink(boolean dropRod) {
        if (this.linkedPos == null) {
            return;
        }

        if (dropRod) {
            this.dropLinkRod();
        }
        this.clearLinkInternal(true);
    }

    private void dropLinkRod() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }

        Block.popResource(this.level, this.worldPosition, new ItemStack(
                this.giantHydraulicLink ? ModItems.GIANT_HYDRAULIC_ROD.get()
                        : this.creativeLink ? ModItems.CREATIVE_HYDRAULIC_ROD.get() : ModItems.HYDRAULIC_ROD.get()));
    }

    private Vector3d localCenterOf(BlockPos pos) {
        return JOMLConversion.atCenterOf(pos);
    }

    private Vector3d toWorldPosition(@Nullable SubLevel subLevel, Vector3d localPosition) {
        return subLevel == null ? localPosition : subLevel.logicalPose().transformPosition(localPosition, new Vector3d());
    }

    private Vector3d getWorldFacingVector(@Nullable SubLevel subLevel) {
        Vector3d vector = this.getLocalFacingVector();
        if (subLevel != null) {
            subLevel.logicalPose().transformNormal(vector, vector);
        }
        if (vector.lengthSquared() > 1.0E-8D) {
            vector.normalize();
        }
        return vector;
    }

    private Vector3d getLocalFacingVector() {
        Direction facing = this.getBlockState().getValue(HydraulicConnectionHeadBlock.FACING);
        return new Vector3d(facing.getStepX(), facing.getStepY(), facing.getStepZ());
    }

    private Vector3d getWorldHingeAxis(@Nullable SubLevel subLevel) {
        Vector3d vector = axisVector(HydraulicHingeHeadBlock.getHingeAxis(this.getBlockState()));
        if (subLevel != null) {
            subLevel.logicalPose().transformNormal(vector, vector);
        }
        if (vector.lengthSquared() > 1.0E-8D) {
            vector.normalize();
        }
        return vector;
    }

    private static Vector3d axisVector(Direction.Axis axis) {
        return switch (axis) {
            case X -> new Vector3d(1.0D, 0.0D, 0.0D);
            case Y -> new Vector3d(0.0D, 1.0D, 0.0D);
            case Z -> new Vector3d(0.0D, 0.0D, 1.0D);
        };
    }

    private Quaterniond frameOrientation(Vector3d axis) {
        Vector3d normalized = new Vector3d(axis);
        if (normalized.lengthSquared() > 1.0E-8D) {
            normalized.normalize();
        }
        return new Quaterniond().rotationTo(new Vector3d(0.0D, 1.0D, 0.0D), normalized);
    }

    private double snapConnectionRollOffset(@Nullable SubLevel ownSubLevel, @Nullable SubLevel otherSubLevel,
                                            Quaterniond ownLocalFrame, Quaterniond otherLocalFrame) {
        Quaterniond ownWorldFrame = this.toWorldFrame(ownSubLevel, ownLocalFrame);
        Quaterniond otherWorldFrame = this.toWorldFrame(otherSubLevel, otherLocalFrame);
        Vector3d axis = ownWorldFrame.transform(new Vector3d(0.0D, 1.0D, 0.0D));
        if (axis.lengthSquared() < MIN_ROLL_REFERENCE_LENGTH_SQUARED) {
            return 0.0D;
        }
        axis.normalize();

        Vector3d ownReference = ownWorldFrame.transform(new Vector3d(1.0D, 0.0D, 0.0D));
        Vector3d otherReference = otherWorldFrame.transform(new Vector3d(1.0D, 0.0D, 0.0D));
        if (!projectOntoPlane(ownReference, axis) || !projectOntoPlane(otherReference, axis)) {
            return 0.0D;
        }

        double angle = Math.atan2(new Vector3d(otherReference).cross(ownReference).dot(axis), otherReference.dot(ownReference));
        return Math.round(angle / QUARTER_TURN_RADIANS) * QUARTER_TURN_RADIANS;
    }

    private Quaterniond toWorldFrame(@Nullable SubLevel subLevel, Quaterniond localFrame) {
        Quaterniond worldFrame = subLevel != null
                ? new Quaterniond(subLevel.logicalPose().orientation())
                : new Quaterniond();
        return worldFrame.mul(localFrame);
    }

    private static boolean projectOntoPlane(Vector3d vector, Vector3d normal) {
        vector.sub(new Vector3d(normal).mul(vector.dot(normal)));
        if (vector.lengthSquared() < MIN_ROLL_REFERENCE_LENGTH_SQUARED) {
            return false;
        }
        vector.normalize();
        return true;
    }

    private static boolean normalizeIfUsable(Vector3d vector) {
        if (vector.lengthSquared() < MIN_ROLL_REFERENCE_LENGTH_SQUARED) {
            return false;
        }
        vector.normalize();
        return true;
    }

    private double distanceSquaredTo(HydraulicConnectionHeadBlockEntity other) {
        Vec3 ownCenter = Vec3.atCenterOf(this.worldPosition);
        Vec3 otherCenter = Vec3.atCenterOf(other.worldPosition);
        return Sable.HELPER.distanceSquaredWithSubLevels(this.level, ownCenter, otherCenter);
    }

    private double currentRodControlDistanceTo(HydraulicConnectionHeadBlockEntity other) {
        ServerSubLevel ownSubLevel = this.getRodConstraintServerSubLevel(this.getContainingServerSubLevel());
        if (ownSubLevel == null) {
            return Double.NaN;
        }

        ServerSubLevel otherSubLevel = other.getRodConstraintServerSubLevel(other.getContainingServerSubLevel());
        Vector3d ownWorld = this.toWorldPosition(ownSubLevel, this.getRodConstraintLocalAnchor());
        Vector3d otherWorld = other.toWorldPosition(otherSubLevel, other.getRodConstraintLocalAnchor());
        Vector3d connection = otherWorld.sub(ownWorld, new Vector3d());
        double actualDistance = connection.length();
        if (!Double.isFinite(actualDistance) || actualDistance < MIN_PHYSICS_DISTANCE) {
            return Double.NaN;
        }

        Vector3d direction = this.getLengthControlDirection(ownSubLevel, ownWorld, otherWorld);
        if (!normalizeIfUsable(direction)) {
            return actualDistance;
        }

        double projectedDistance = connection.dot(direction);
        return Double.isFinite(projectedDistance) && projectedDistance > MIN_PHYSICS_DISTANCE
                ? projectedDistance
                : actualDistance;
    }

    public void setStretchResistance(int value) {
        this.applySettingsLocal(value, this.settings.freeMode(), this.settings.expectedLengthTenths(),
                this.settings.returnForce(), true);
    }

    public void setFreeMode(boolean value) {
        this.applySettingsLocal(this.settings.stretchResistance(), value, this.settings.expectedLengthTenths(),
                this.settings.returnForce(), true);
    }

    public void setExpectedLengthTenths(int value) {
        this.applySettingsLocal(this.settings.stretchResistance(), this.settings.freeMode(), value,
                this.settings.returnForce(), true);
    }

    public void setRedstoneLengthRangeAndMirror(int minLengthTenths, int maxLengthTenths) {
        this.applyRedstoneLengthRangeLocal(minLengthTenths, maxLengthTenths, true);

        HydraulicConnectionHeadBlockEntity other = this.resolveLinkedConnectionHead();
        if (other != null && other.references(this)) {
            other.applyRedstoneLengthRangeLocal(minLengthTenths, maxLengthTenths, true);
        }
    }

    public void setReturnForce(int value) {
        this.applySettingsLocal(this.settings.stretchResistance(), this.settings.freeMode(),
                this.settings.expectedLengthTenths(), value, true);
    }

    public int getStretchResistance() {
        return this.giantHydraulicLink ? this.giantHydraulicSettings.flowLitresPerMinute()
                : this.settings.stretchResistance();
    }

    public boolean isFreeMode() {
        return this.giantHydraulicLink ? this.giantHydraulicSettings.vented() : this.settings.freeMode();
    }

    public int getExpectedLengthTenths() {
        return this.giantHydraulicLink ? this.giantHydraulicSettings.targetLengthTenths()
                : this.settings.expectedLengthTenths();
    }

    public int getRedstoneMinLengthTenths() {
        return this.giantHydraulicLink ? this.giantHydraulicSettings.redstoneMinLengthTenths()
                : this.settings.redstoneMinLengthTenths();
    }

    public int getRedstoneMaxLengthTenths() {
        return this.giantHydraulicLink ? this.giantHydraulicSettings.redstoneMaxLengthTenths()
                : this.settings.redstoneMaxLengthTenths();
    }

    public int getReturnForce() {
        return this.giantHydraulicLink ? this.giantHydraulicSettings.pressureBar() : this.settings.returnForce();
    }

    public boolean isExpectedLengthControlledByRegulator() {
        if (this.level == null || !this.hasLink()) {
            return false;
        }
        if (this.hasControllingRegulator()) {
            return true;
        }

        HydraulicConnectionHeadBlockEntity other = this.resolveLinkedConnectionHead();
        return other != null && other.references(this) && other.hasControllingRegulator();
    }

    private boolean hasControllingRegulator() {
        if (this.level == null) {
            return false;
        }

        BlockState state = this.getBlockState();
        if (!state.hasProperty(HydraulicConnectionHeadBlock.FACING)) {
            return false;
        }

        Direction facing = state.getValue(HydraulicConnectionHeadBlock.FACING);
        BlockEntity blockEntity = SubLevelReferenceHelper.resolveBlockEntity(
                this.level,
                this.worldPosition.relative(facing.getOpposite()),
                this.getContainingSubLevelId());
        return blockEntity instanceof HydraulicRegulatorBlockEntity regulator && regulator.controlsHead(this);
    }

    public int getExpectedLengthTenthsForRedstoneSignal(int signal) {
        int clampedSignal = Mth.clamp(signal, 0, 15);
        int minLength = this.getRedstoneMinLengthTenths();
        int maxLength = this.getRedstoneMaxLengthTenths();
        return minLength + Math.round((maxLength - minLength) * (clampedSignal / 15.0F));
    }

    public void applySyncedSettings(int stretchResistance, boolean freeMode, int expectedLengthTenths, int returnForce,
                                    int redstoneMinLengthTenths, int redstoneMaxLengthTenths) {
        if (this.giantHydraulicLink) {
            this.applyGiantHydraulicSettingsLocal(stretchResistance, freeMode, expectedLengthTenths, returnForce, false);
            this.applyGiantHydraulicRedstoneRangeLocal(redstoneMinLengthTenths, redstoneMaxLengthTenths, false);
            return;
        }
        this.applySettingsLocal(stretchResistance, freeMode, expectedLengthTenths, returnForce, false);
        this.applyRedstoneLengthRangeLocal(redstoneMinLengthTenths, redstoneMaxLengthTenths, false);
    }

    public void setGiantHydraulicSettingsAndMirror(int flowLitresPerMinute, boolean vented, int targetLengthTenths,
                                                    int pressureBar, int redstoneMinLengthTenths,
                                                    int redstoneMaxLengthTenths) {
        if (!this.giantHydraulicLink) {
            return;
        }
        this.applyGiantHydraulicSettingsLocal(flowLitresPerMinute, vented, targetLengthTenths, pressureBar, true);
        this.applyGiantHydraulicRedstoneRangeLocal(redstoneMinLengthTenths, redstoneMaxLengthTenths, true);
        HydraulicConnectionHeadBlockEntity other = this.resolveLinkedConnectionHead();
        if (other != null && other.references(this) && other.giantHydraulicLink) {
            other.applyGiantHydraulicSettingsLocal(flowLitresPerMinute, vented, targetLengthTenths, pressureBar, true);
            other.applyGiantHydraulicRedstoneRangeLocal(redstoneMinLengthTenths, redstoneMaxLengthTenths, true);
        }
    }

    public void setGiantHydraulicTargetAndMirror(int targetLengthTenths, double approachMultiplier) {
        if (!this.giantHydraulicLink) {
            return;
        }
        this.applyGiantHydraulicSettingsLocal(this.giantHydraulicSettings.flowLitresPerMinute(), false,
                targetLengthTenths, this.giantHydraulicSettings.pressureBar(), true);
        this.applyExpectedLengthApproachMultiplierLocal(approachMultiplier);
        HydraulicConnectionHeadBlockEntity other = this.resolveLinkedConnectionHead();
        if (other != null && other.references(this) && other.giantHydraulicLink) {
            other.applyGiantHydraulicSettingsLocal(other.giantHydraulicSettings.flowLitresPerMinute(), false,
                    targetLengthTenths, other.giantHydraulicSettings.pressureBar(), true);
            other.applyExpectedLengthApproachMultiplierLocal(approachMultiplier);
        }
    }

    public void setSettingsAndMirror(int stretchResistance, boolean freeMode, int expectedLengthTenths, int returnForce) {
        this.setSettingsAndMirror(stretchResistance, freeMode, expectedLengthTenths, returnForce, 1.0D);
    }

    public void setSettingsAndMirror(int stretchResistance, boolean freeMode, int expectedLengthTenths, int returnForce,
                                     double expectedLengthApproachMultiplier) {
        this.applySettingsLocal(stretchResistance, freeMode, expectedLengthTenths, returnForce, true);
        this.applyExpectedLengthApproachMultiplierLocal(expectedLengthApproachMultiplier);

        HydraulicConnectionHeadBlockEntity other = this.resolveLinkedConnectionHead();
        if (other != null && other.references(this)) {
            other.applySettingsLocal(stretchResistance, freeMode, expectedLengthTenths, returnForce, true);
            other.applyExpectedLengthApproachMultiplierLocal(expectedLengthApproachMultiplier);
        }
    }

    public void setExpectedLengthApproachMultiplierAndMirror(double value) {
        this.applyExpectedLengthApproachMultiplierLocal(value);

        HydraulicConnectionHeadBlockEntity other = this.resolveLinkedConnectionHead();
        if (other != null && other.references(this)) {
            other.applyExpectedLengthApproachMultiplierLocal(value);
        }
    }

    private void applySettingsLocal(int stretchResistance, boolean freeMode, int expectedLengthTenths, int returnForce, boolean notify) {
        boolean effectiveFreeMode = this.creativeLink ? false : freeMode;
        int clampedStretchResistance = this.creativeLink ? 0 : clampStretchResistance(stretchResistance);
        int clampedExpectedLengthTenths = this.clampExpectedLengthTenthsForLink(expectedLengthTenths);
        int clampedReturnForce = this.creativeLink ? 0 : clampReturnForce(returnForce);
        boolean creativeTargetChanged = this.creativeLink
                && this.linkedPos != null
                && this.settings.expectedLengthTenths() != clampedExpectedLengthTenths;
        HydraulicSettingsState.Change change = this.settings.applyBaseSettings(
                clampedStretchResistance, effectiveFreeMode, clampedExpectedLengthTenths, clampedReturnForce);
        if (change.leftFreeMode()) {
            this.expectedLengthTransitionPending = true;
            this.effectiveReturnForce = 0.0D;
        } else if (effectiveFreeMode) {
            this.effectiveReturnForce = 0.0D;
        }
        if (creativeTargetChanged) {
            this.scheduleCreativeServoWake();
        }
        if (notify && change.changed()) {
            this.setChanged();
            this.sendData();
        }
    }

    private void applyGiantHydraulicSettingsLocal(int flowLitresPerMinute, boolean vented,
                                                   int targetLengthTenths, int pressureBar, boolean notify) {
        boolean changed = this.giantHydraulicSettings.applyBase(flowLitresPerMinute, vented,
                this.clampExpectedLengthTenthsForLink(targetLengthTenths), pressureBar);
        if (changed) {
            this.wakeGiantHydraulicBodiesNow();
        }
        if (changed && notify) {
            this.setChanged();
            this.sendData();
        }
    }

    private void applyExpectedLengthApproachMultiplierLocal(double value) {
        this.expectedLengthApproachMultiplier = clampExpectedLengthApproachMultiplier(value);
    }

    private void applyRedstoneLengthRangeLocal(int minLengthTenths, int maxLengthTenths, boolean notify) {
        int minimum = getMinExpectedLengthTenths(this.giantHydraulicLink);
        boolean changed = this.settings.applyRedstoneLengthRange(
                Math.max(minimum, minLengthTenths), Math.max(minimum, maxLengthTenths));
        if (notify && changed) {
            this.setChanged();
            this.sendData();
        }
    }

    private void applyGiantHydraulicRedstoneRangeLocal(int minLengthTenths, int maxLengthTenths, boolean notify) {
        boolean changed = this.giantHydraulicSettings.applyRedstoneRange(
                Math.max(getMinExpectedLengthTenths(true), minLengthTenths),
                Math.min(getMaxExpectedLengthTenths(), maxLengthTenths));
        if (changed && notify) {
            this.setChanged();
            this.sendData();
        }
    }

    private void normalizeConfiguredSettings(boolean notify) {
        boolean changed = this.settings.normalize();
        if (this.giantHydraulicLink) {
            changed |= this.giantHydraulicSettings.applyBase(this.giantHydraulicSettings.flowLitresPerMinute(),
                    this.giantHydraulicSettings.vented(), this.giantHydraulicSettings.targetLengthTenths(),
                    this.giantHydraulicSettings.pressureBar());
            changed |= this.giantHydraulicSettings.applyRedstoneRange(
                    Math.max(getMinExpectedLengthTenths(true), this.giantHydraulicSettings.redstoneMinLengthTenths()),
                    Math.min(getMaxExpectedLengthTenths(), this.giantHydraulicSettings.redstoneMaxLengthTenths()));
        }
        if (notify && changed) {
            this.setChanged();
            this.sendData();
        }
    }

    private int clampExpectedLengthTenthsForLink(int value) {
        return this.giantHydraulicLink
                ? Math.max(getMinExpectedLengthTenths(true), clampExpectedLengthTenths(value))
                : clampExpectedLengthTenths(value);
    }

    public static int clampStretchResistance(int value) {
        return HydraulicSettings.clampStretchResistance(value);
    }

    public static int clampReturnForce(int value) {
        return HydraulicSettings.clampReturnForce(value);
    }

    public static int clampExpectedLengthTenths(int value) {
        return HydraulicSettings.clampExpectedLengthTenths(value);
    }

    public static double clampExpectedLengthApproachMultiplier(double value) {
        return HydraulicSettings.clampApproachMultiplier(value);
    }

    public static int getMaxSettingValue() {
        return getMaxReturnForceValue();
    }

    public static int getMaxStretchResistanceValue() {
        return HydraulicSettings.maxStretchResistance();
    }

    public static int getMaxStretchResistanceLevel() {
        return HydraulicSettings.maxStretchResistanceLevel();
    }

    public static int getStretchResistanceWarningLevel() {
        return HydraulicSettings.stretchResistanceWarningLevel();
    }

    public static int stretchResistanceFromLevel(int level) {
        return HydraulicSettings.stretchResistanceFromLevel(level);
    }

    public static int stretchResistanceToLevel(int resistance) {
        return HydraulicSettings.stretchResistanceToLevel(resistance);
    }

    public static int getMaxReturnForceValue() {
        return HydraulicSettings.maxReturnForce();
    }

    public static int getMaxReturnForceLevel() {
        return HydraulicSettings.maxReturnForceLevel();
    }

    public static int returnForceFromLevel(int level) {
        return HydraulicSettings.returnForceFromLevel(level);
    }

    public static int returnForceToLevel(int force) {
        return HydraulicSettings.returnForceToLevel(force);
    }

    public static int getMinExpectedLengthTenths() {
        return HydraulicSettings.minExpectedLengthTenths();
    }

    public static int getMinExpectedLengthTenths(boolean giantHydraulic) {
        return giantHydraulic ? (int) Math.round(GiantHydraulicPhysics.MINIMUM_LENGTH * 10.0D)
                : getMinExpectedLengthTenths();
    }

    public static int getMaxExpectedLengthTenths() {
        return HydraulicSettings.maxExpectedLengthTenths();
    }

    public static double getMaxLinkLength() {
        return AeroUniversalJointConfig.hydraulicRodBreakLinkLength();
    }

    private static double clampEffectiveExpectedLength(double value) {
        if (!Double.isFinite(value)) {
            return AeroUniversalJointConfig.hydraulicRodMinLinkLength();
        }
        return Math.max(0.0D, Math.min(AeroUniversalJointConfig.hydraulicRodBreakLinkLength(), value));
    }

    private static double clampEffectiveReturnForce(double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(getMaxReturnForceValue(), value));
    }

    public static String formatPlainValue(int value) {
        return Integer.toString(Math.max(0, value));
    }

    public static String formatTenths(int value) {
        return HydraulicSettings.formatTenths(value);
    }

    public static double calculateExpectedReturnForce(int returnForce, double deviation) {
        return HydraulicLengthControl.calculateReturnForce(clampReturnForce(returnForce), deviation);
    }

    private void applyLengthLimits(ServerSubLevel ownSubLevel, RigidBodyHandle ownHandle,
                                   Vector3d ownLocal, Vector3d ownWorld,
                                   @Nullable ServerSubLevel otherSubLevel, Vector3d otherLocal, Vector3d otherWorld,
                                   Vector3d worldDirection, double timeStep) {
        if (timeStep <= 0.0D) {
            return;
        }

        Vector3d connection = otherWorld.sub(ownWorld, new Vector3d());
        double actualDistance = connection.length();
        this.updateLinkStrainEffect(actualDistance);
        if (actualDistance > AeroUniversalJointConfig.hydraulicRodBreakLinkLength()) {
            this.breakOverstretchedLink();
            return;
        }

        double distance = connection.dot(worldDirection);
        Vector3d correctionDirection = new Vector3d(worldDirection);
        if (this.creativeLink) {
            double effectiveExpectedLength = this.updateEffectiveExpectedLength(distance, timeStep);
            this.refreshRodDampingMotor();
            this.wakeCreativeServoBodiesIfNeeded(distance, effectiveExpectedLength, ownSubLevel, otherSubLevel);
            this.applyHardLengthLimits(distance, correctionDirection, ownSubLevel, ownHandle,
                    ownLocal, otherSubLevel, otherLocal, timeStep,
                    AeroUniversalJointConfig.hydraulicRodMinLinkLength());
            return;
        }

        if (this.giantHydraulicLink) {
            this.applyGiantHydraulicForce(distance, correctionDirection, ownSubLevel, ownHandle,
                    ownLocal, otherSubLevel, otherLocal, timeStep);
            return;
        }

        double springImpulseMagnitude = 0.0D;
        if (!this.settings.freeMode()) {
            double effectiveExpectedLength = this.updateEffectiveExpectedLength(distance, timeStep);
            double effectiveReturnForce = this.updateEffectiveReturnForce(timeStep);
            springImpulseMagnitude += calculateExpectedLengthReturnImpulse(
                    distance, effectiveExpectedLength, effectiveReturnForce, timeStep);
        }

        springImpulseMagnitude = HydraulicLengthControl.clampImpulse(springImpulseMagnitude,
                AeroUniversalJointConfig.hydraulicRodMaxCombinedLengthControlImpulse());
        if (Math.abs(springImpulseMagnitude) > 1.0E-8D) {
            Vector3d lengthControl = new Vector3d(correctionDirection).mul(springImpulseMagnitude);
            this.applyLengthImpulse(ownSubLevel, ownHandle, ownLocal, otherSubLevel, otherLocal, lengthControl);
        }

        this.applyHardLengthLimits(distance, correctionDirection, ownSubLevel, ownHandle,
                ownLocal, otherSubLevel, otherLocal, timeStep, AeroUniversalJointConfig.hydraulicRodMinLinkLength());
    }

    private void applyGiantHydraulicForce(double distance, Vector3d correctionDirection,
                                          ServerSubLevel ownSubLevel, RigidBodyHandle ownHandle,
                                          Vector3d ownLocal, @Nullable ServerSubLevel otherSubLevel,
                                          Vector3d otherLocal, double timeStep) {
        double targetDistance = this.updateGiantHydraulicExpectedLength(distance, timeStep);
        GiantHydraulicPhysics.Result result = GiantHydraulicPhysics.step(
                this.giantHydraulicPhysics, distance, targetDistance, this.giantHydraulicSettings.vented(),
                this.giantHydraulicSettings.pressureBar(), this.giantHydraulicSettings.flowLitresPerMinute(), timeStep);
        this.giantHydraulicPhysics = result.state();
        boolean closedCenter = Math.abs(result.valve()) <= 1.0E-8D;
        double maximumForce = this.giantHydraulicSettings.pressureBar()
                * HydraulicCylinderControl.CAP_FORCE_AREA;
        if (closedCenter) {
            maximumForce = Math.max(maximumForce,
                    HydraulicCylinderControl.MAX_WORKING_PRESSURE_BAR * HydraulicCylinderControl.CAP_FORCE_AREA);
        }
        this.refreshGiantHydraulicMotor(result.motorTargetDistance(), maximumForce, closedCenter);
        this.updateGiantHydraulicOverloadEffect(result, targetDistance, distance);
        this.wakeGiantHydraulicBodiesIfNeeded(distance, targetDistance, ownSubLevel, otherSubLevel);
    }

    private void updateGiantHydraulicOverloadEffect(GiantHydraulicPhysics.Result result,
                                                     double targetDistance, double distance) {
        double valve = result.valve();
        double activePressure = valve > 0.0D
                ? result.state().pressure().capPressure()
                : result.state().pressure().rodPressure();
        boolean overloaded = !this.giantHydraulicSettings.vented()
                && this.giantHydraulicSettings.flowLitresPerMinute() > 0
                && Math.abs(valve) >= 0.15D
                && Math.abs(targetDistance - distance) >= 0.125D
                && Math.abs(result.velocity()) <= 0.025D
                && activePressure >= this.giantHydraulicSettings.pressureBar() * 0.92D;
        if (this.giantHydraulicOverloadEffect == overloaded) {
            return;
        }

        this.giantHydraulicOverloadEffect = overloaded;
        HydraulicConnectionHeadBlockEntity other = this.resolveLinkedConnectionHead();
        boolean stretched = other != null && other.references(this)
                && this.distanceSquaredTo(other) >= Math.pow(AeroUniversalJointConfig.hydraulicRodStrainLinkLength(), 2.0D);
        this.setLinkStrainState(overloaded || stretched);
        if (other != null && other.references(this)) {
            other.giantHydraulicOverloadEffect = overloaded;
            other.setLinkStrainState(overloaded || stretched);
        }
    }

    private void refreshGiantHydraulicMotor(double targetDistance, double maximumForce, boolean closedCenter) {
        if (this.constraintHandle == null || !this.constraintHandle.isValid()) {
            return;
        }

        this.constraintHandle.setLimit(ConstraintJointAxis.LINEAR_Y,
                GiantHydraulicPhysics.MINIMUM_LENGTH, AeroUniversalJointConfig.hydraulicRodMaxLinkLength());

        if (this.giantHydraulicSettings.vented() || maximumForce <= 1.0E-8D) {
            this.constraintHandle.setMotor(ConstraintJointAxis.LINEAR_Y, 0.0D,
                    0.0D, 0.0D, false, 0.0D);
            return;
        }

        this.constraintHandle.setMotor(ConstraintJointAxis.LINEAR_Y, targetDistance,
                GiantHydraulicPhysics.MOTOR_POSITION_STIFFNESS,
                closedCenter ? GIANT_HYDRAULIC_HOLD_DAMPING : GiantHydraulicPhysics.MOTOR_POSITION_DAMPING,
                true, maximumForce);
    }

    private void applyHardLengthLimits(double distance, Vector3d correctionDirection,
                                       ServerSubLevel ownSubLevel, RigidBodyHandle ownHandle,
                                       Vector3d ownLocal, @Nullable ServerSubLevel otherSubLevel,
                                       Vector3d otherLocal, double timeStep, double minimumLength) {
        double impulseMagnitude = HydraulicLengthControl.calculateHardLimitImpulse(distance, timeStep,
                minimumLength, AeroUniversalJointConfig.hydraulicRodMaxLinkLength());
        if (Math.abs(impulseMagnitude) <= 1.0E-8D) {
            return;
        }

        Vector3d worldImpulse = new Vector3d(correctionDirection).mul(impulseMagnitude);
        this.applyLengthImpulse(ownSubLevel, ownHandle, ownLocal, otherSubLevel, otherLocal, worldImpulse);
    }

    private void wakeCreativeServoBodiesIfNeeded(double distance, double effectiveExpectedLength,
                                                 ServerSubLevel ownSubLevel,
                                                 @Nullable ServerSubLevel otherSubLevel) {
        if (this.level == null || !this.creativeLink) {
            return;
        }

        long gameTime = this.level.getGameTime();
        if (gameTime > this.creativeServoWakeUntilGameTime) {
            return;
        }

        double targetDistance = this.getExpectedLengthTenths() / 10.0D;
        boolean targetStillApproaching = Math.abs(effectiveExpectedLength - targetDistance) > CREATIVE_LENGTH_WAKE_EPSILON;
        boolean servoStillPulling = Math.abs(distance - effectiveExpectedLength) > CREATIVE_LENGTH_WAKE_EPSILON;
        if (!targetStillApproaching && !servoStillPulling) {
            this.creativeServoWakeUntilGameTime = Long.MIN_VALUE;
            return;
        }

        this.wakeCreativeServoBodies(ownSubLevel, otherSubLevel);
    }

    private void scheduleCreativeServoWake() {
        if (this.level == null || this.level.isClientSide || !this.creativeLink) {
            return;
        }

        this.creativeServoWakeUntilGameTime = this.level.getGameTime() + CREATIVE_LENGTH_WAKE_TICKS;
        ServerSubLevel ownSubLevel = this.getRodConstraintServerSubLevel(this.getContainingServerSubLevel());
        HydraulicConnectionHeadBlockEntity other = this.resolveLinkedConnectionHead();
        ServerSubLevel otherSubLevel = other != null
                ? other.getRodConstraintServerSubLevel(other.getContainingServerSubLevel())
                : null;
        this.wakeCreativeServoBodies(ownSubLevel, otherSubLevel);
    }

    private void wakeCreativeServoBodies(@Nullable ServerSubLevel ownSubLevel,
                                         @Nullable ServerSubLevel otherSubLevel) {
        if (this.level == null || this.level.isClientSide || !this.creativeLink) {
            return;
        }

        this.wakeHydraulicBodies(ownSubLevel, otherSubLevel);
    }

    private void wakeGiantHydraulicBodiesIfNeeded(double distance, double targetDistance,
                                                   @Nullable ServerSubLevel ownSubLevel,
                                                   @Nullable ServerSubLevel otherSubLevel) {
        if (this.level == null || this.level.isClientSide || !this.giantHydraulicLink
                || this.giantHydraulicSettings.vented() || this.giantHydraulicSettings.flowLitresPerMinute() <= 0
                || Math.abs(targetDistance - distance) <= 1.0D / 128.0D) {
            return;
        }

        this.wakeHydraulicBodies(ownSubLevel, otherSubLevel);
    }

    private void wakeGiantHydraulicBodiesNow() {
        if (this.level == null || this.level.isClientSide || !this.giantHydraulicLink) {
            return;
        }

        ServerSubLevel ownSubLevel = this.getRodConstraintServerSubLevel(this.getContainingServerSubLevel());
        HydraulicConnectionHeadBlockEntity other = this.resolveLinkedConnectionHead();
        ServerSubLevel otherSubLevel = other != null
                ? other.getRodConstraintServerSubLevel(other.getContainingServerSubLevel())
                : null;
        this.wakeHydraulicBodies(ownSubLevel, otherSubLevel);
    }

    private void wakeHydraulicBodies(@Nullable ServerSubLevel ownSubLevel,
                                     @Nullable ServerSubLevel otherSubLevel) {
        if (this.level == null || this.level.isClientSide) {
            return;
        }

        long gameTime = this.level.getGameTime();
        if (this.lastHydraulicServoWakeGameTime == gameTime) {
            return;
        }
        this.lastHydraulicServoWakeGameTime = gameTime;

        PhysicsPipeline pipeline = SubLevelPhysicsSystem.require(this.level).getPipeline();
        if (ownSubLevel != null && !ownSubLevel.isRemoved()) {
            pipeline.wakeUp(ownSubLevel);
        }
        if (otherSubLevel != null && !otherSubLevel.isRemoved()) {
            pipeline.wakeUp(otherSubLevel);
        }
    }

    private void breakOverstretchedLink() {
        this.detachLink(true);
    }

    public float getLinkStrainEffect() {
        return this.linkStrainEffect;
    }

    private void updateLinkStrainEffect(double actualDistance) {
        boolean strained = this.giantHydraulicOverloadEffect
                || actualDistance >= AeroUniversalJointConfig.hydraulicRodStrainLinkLength();
        this.setLinkStrainState(strained);
        HydraulicConnectionHeadBlockEntity other = this.resolveLinkedConnectionHead();
        if (other != null && other.references(this)) {
            other.setLinkStrainState(strained);
        }
    }

    private void triggerLinkStrainEffect(boolean strained) {
        this.setLinkStrainState(strained);
    }

    private void setLinkStrainState(boolean strained) {
        if (this.linkWasStrained == strained) {
            return;
        }

        this.linkWasStrained = strained;
        this.linkStrainEffect = strained ? 1.0F : -1.0F;
        this.linkStrainEffectDelay = strained ? 0 : LINK_STRAIN_RECOVERY_TICKS;
        if (this.level != null && !this.level.isClientSide) {
            this.setChanged();
            this.sendData();
        }
    }

    private void tickLinkStrainEffect() {
        if (this.linkWasStrained) {
            this.linkStrainEffect = 1.0F;
            return;
        }
        if (this.linkStrainEffectDelay > 0) {
            this.linkStrainEffectDelay--;
            return;
        }
        if (this.linkStrainEffect == 0.0F) {
            return;
        }

        this.linkStrainEffect -= this.linkStrainEffect * 0.1F;
        if (Math.abs(this.linkStrainEffect) < 1.0F / 128.0F) {
            this.linkStrainEffect = 0.0F;
        }
    }

    private double calculateExpectedLengthReturnImpulse(double distance, double targetDistance,
                                                       double effectiveReturnForce, double timeStep) {
        return HydraulicLengthControl.calculateReturnImpulse(effectiveReturnForce, distance, targetDistance, timeStep);
    }

    private double updateEffectiveExpectedLength(double actualDistance, double timeStep) {
        if (this.expectedLengthTransitionPending || !Double.isFinite(this.effectiveExpectedLengthBlocks)) {
            this.effectiveExpectedLengthBlocks = actualDistance;
            this.expectedLengthTransitionPending = false;
            return this.effectiveExpectedLengthBlocks;
        }

        double targetDistance = this.getExpectedLengthTenths() / 10.0D;
        double approachRate = this.creativeLink
                ? CREATIVE_LENGTH_APPROACH_RATE
                : AeroUniversalJointConfig.hydraulicRodExpectedLengthApproachRate();
        double maxStep = Math.max(0.0D, timeStep) * approachRate * this.expectedLengthApproachMultiplier;
        this.effectiveExpectedLengthBlocks = HydraulicLengthControl.approach(
                this.effectiveExpectedLengthBlocks, targetDistance, maxStep);
        return this.effectiveExpectedLengthBlocks;
    }

    private double updateGiantHydraulicExpectedLength(double actualDistance, double timeStep) {
        if (this.expectedLengthTransitionPending || !Double.isFinite(this.effectiveExpectedLengthBlocks)) {
            this.effectiveExpectedLengthBlocks = actualDistance;
            this.expectedLengthTransitionPending = false;
            return this.effectiveExpectedLengthBlocks;
        }

        this.effectiveExpectedLengthBlocks = this.getExpectedLengthTenths() / 10.0D;
        return this.effectiveExpectedLengthBlocks;
    }

    private double updateEffectiveReturnForce(double timeStep) {
        if (this.settings.freeMode()) {
            this.effectiveReturnForce = 0.0D;
            return 0.0D;
        }

        if (!Double.isFinite(this.effectiveReturnForce)) {
            this.effectiveReturnForce = 0.0D;
        }

        double targetForce = this.settings.returnForce();
        double maxStep = Math.max(0.0D, timeStep)
                * AeroUniversalJointConfig.hydraulicRodReturnForceApproachRate();
        this.effectiveReturnForce = clampEffectiveReturnForce(HydraulicLengthControl.approach(
                this.effectiveReturnForce, targetForce, maxStep));
        return this.effectiveReturnForce;
    }

    private static Vector3d clampVector(Vector3d vector, double maxMagnitude) {
        double lengthSquared = vector.lengthSquared();
        if (lengthSquared > maxMagnitude * maxMagnitude) {
            vector.mul(maxMagnitude / Math.sqrt(lengthSquared));
        }
        return vector;
    }

    private void applyLengthVelocityChange(RigidBodyHandle ownHandle, @Nullable ServerSubLevel otherSubLevel,
                                           Vector3d worldVelocityDelta) {
        if (ownHandle == null || !ownHandle.isValid()) {
            return;
        }
        ownHandle.addLinearAndAngularVelocity(worldVelocityDelta, new Vector3d());

        if (otherSubLevel == null || otherSubLevel.isRemoved()) {
            return;
        }

        RigidBodyHandle otherHandle = RigidBodyHandle.of(otherSubLevel);
        if (otherHandle == null || !otherHandle.isValid()) {
            return;
        }

        otherHandle.addLinearAndAngularVelocity(worldVelocityDelta.negate(new Vector3d()), new Vector3d());
    }

    private void applyLengthImpulse(ServerSubLevel ownSubLevel, RigidBodyHandle ownHandle, Vector3d ownLocal,
                                    @Nullable ServerSubLevel otherSubLevel, Vector3d otherLocal,
                                    Vector3d worldImpulse) {
        if (ownSubLevel == null || ownSubLevel.isRemoved() || ownHandle == null || !ownHandle.isValid()) {
            return;
        }
        RigidBodyHandle otherHandle = null;
        if (otherSubLevel != null) {
            if (otherSubLevel.isRemoved()) {
                return;
            }
            otherHandle = RigidBodyHandle.of(otherSubLevel);
            if (otherHandle == null || !otherHandle.isValid()) {
                return;
            }
        }

        Vector3d ownImpulse = ownSubLevel.logicalPose().transformNormalInverse(worldImpulse, new Vector3d());
        this.lengthForceTotal.applyImpulseAtPoint(ownSubLevel, ownLocal, ownImpulse);
        ownHandle.applyForcesAndReset(this.lengthForceTotal);

        if (otherSubLevel == null) {
            return;
        }

        Vector3d partnerImpulse = otherSubLevel.logicalPose().transformNormalInverse(worldImpulse.negate(new Vector3d()), new Vector3d());
        this.partnerLengthForceTotal.applyImpulseAtPoint(otherSubLevel, otherLocal, partnerImpulse);
        otherHandle.applyForcesAndReset(this.partnerLengthForceTotal);
    }

    private static void syncSelectionToClient(Player player, @Nullable JointBindingData.Selection selection) {
        if (player instanceof ServerPlayer serverPlayer) {
            SyncHydraulicSelectionPayload.send(serverPlayer, selection);
        }
    }

    public enum LinkResult {
        SUCCESS("linked", true, true),
        ALREADY_LINKED("linked", true, false),
        TARGET_UNAVAILABLE("target_missing", true, false),
        WRONG_DIMENSION("wrong_dimension", false, false),
        TOO_FAR("too_far", false, false),
        NOT_ALIGNED("not_aligned", false, false),
        SELF("same_joint", false, false),
        SAME_STRUCTURE("same_structure", false, false);

        private final String keySuffix;
        private final boolean clearsSelection;
        private final boolean consumesRod;

        LinkResult(String keySuffix, boolean clearsSelection, boolean consumesRod) {
            this.keySuffix = keySuffix;
            this.clearsSelection = clearsSelection;
            this.consumesRod = consumesRod;
        }

        public Component message() {
            return Component.translatable("message.aeronautics_utility_objects." + this.keySuffix);
        }

        public boolean clearsSelection() {
            return this.clearsSelection;
        }

        public boolean consumesRod() {
            return this.consumesRod;
        }
    }
}
