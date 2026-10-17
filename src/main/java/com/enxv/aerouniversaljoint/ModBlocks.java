package com.enxv.aerouniversaljoint;

import com.enxv.aerouniversaljoint.content.JointVariant;
import com.enxv.aerouniversaljoint.content.UniversalJointBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AeroUniversalJointMod.MOD_ID);

    // Registry id kept as "brass_universal_joint" so existing worlds keep their joint heads.
    public static final DeferredBlock<UniversalJointBlock> BRASS_UNIVERSAL_JOINT = BLOCKS.register(
            "brass_universal_joint",
            () -> new UniversalJointBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(3.5f, 6.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .noOcclusion(), JointVariant.BRASS));

    private ModBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
