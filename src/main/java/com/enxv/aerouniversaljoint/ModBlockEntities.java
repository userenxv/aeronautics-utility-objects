package com.enxv.aerouniversaljoint;

import com.enxv.aerouniversaljoint.content.UniversalJointBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AeroUniversalJointMod.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<UniversalJointBlockEntity>> UNIVERSAL_JOINT =
            BLOCK_ENTITIES.register(
                    "universal_joint",
                    () -> BlockEntityType.Builder.of(
                            UniversalJointBlockEntity::new,
                            ModBlocks.BRASS_UNIVERSAL_JOINT.get()).build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
    }
}
