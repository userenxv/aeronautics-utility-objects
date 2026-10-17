package com.enxv.aerouniversaljoint.content;

import com.enxv.aerouniversaljoint.ModBlocks;
import com.enxv.aerouniversaljoint.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class UniversalJointMenu extends AbstractContainerMenu implements ContainerData {
    private final ContainerLevelAccess access;
    private final BlockPos blockPos;
    @Nullable
    private final UniversalJointBlockEntity blockEntity;

    public UniversalJointMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, inventory, extraData.readBlockPos());
    }

    public UniversalJointMenu(int containerId, Inventory inventory, UniversalJointBlockEntity blockEntity) {
        this(containerId, inventory, blockEntity.getBlockPos(), blockEntity);
    }

    private UniversalJointMenu(int containerId, Inventory inventory, BlockPos blockPos) {
        this(containerId, inventory, blockPos, resolveBlockEntity(inventory, blockPos));
    }

    private UniversalJointMenu(int containerId, Inventory inventory, BlockPos blockPos,
                               @Nullable UniversalJointBlockEntity blockEntity) {
        super(ModMenuTypes.UNIVERSAL_JOINT.get(), containerId);
        this.blockPos = blockPos.immutable();
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(inventory.player.level(), this.blockPos);
        this.addDataSlots(this);
    }

    @Nullable
    private static UniversalJointBlockEntity resolveBlockEntity(Inventory inventory, BlockPos blockPos) {
        return inventory.player.level().getBlockEntity(blockPos) instanceof UniversalJointBlockEntity be ? be : null;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.BRASS_UNIVERSAL_JOINT.get());
    }

    @Override
    public int get(int index) {
        if (index == 0 && this.blockEntity != null) {
            return (int) (this.blockEntity.getSpeedRatio() * 100.0F);
        }
        return 0;
    }

    @Override
    public void set(int index, int value) {
        if (index == 0 && this.blockEntity != null) {
            this.blockEntity.applySyncedSpeedRatio(value / 100.0F);
        }
    }

    @Override
    public int getCount() {
        return 1;
    }

    public float getCurrentSpeedRatio() {
        return this.get(0) / 100.0F;
    }

    public BlockPos getBlockPos() {
        return this.blockPos;
    }
}
