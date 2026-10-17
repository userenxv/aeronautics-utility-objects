package com.enxv.aerouniversaljoint;

import com.enxv.aerouniversaljoint.client.AeroUniversalJointClient;
import com.enxv.aerouniversaljoint.network.AeroUniversalJointNetwork;
import com.simibubi.create.api.stress.BlockStressValues;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(AeroUniversalJointMod.MOD_ID)
public final class AeroUniversalJointMod {
    public static final String MOD_ID = "aeronautics_utility_objects";

    public AeroUniversalJointMod(IEventBus modBus, ModContainer modContainer) {
        ModBlocks.register(modBus);
        ModItems.register(modBus);
        ModBlockEntities.register(modBus);
        ModMenuTypes.register(modBus);
        ModCreativeTabs.register(modBus);
        AeroUniversalJointNetwork.register(modBus);
        AeroUniversalJointCommonEvents.register();

        // Common config: a real file at config/aeronautics_utility_objects-common.toml, shared by every world.
        modContainer.registerConfig(ModConfig.Type.COMMON, AeroUniversalJointConfig.COMMON_SPEC);
        modBus.addListener(AeroUniversalJointMod::commonSetup);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            AeroUniversalJointClient.init(modBus);
        }
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        // The supplier is read live, so edits to stress_impact apply without a restart
        // (existing networks pick it up the next time they recalculate).
        event.enqueueWork(() -> BlockStressValues.IMPACTS.register(
                ModBlocks.BRASS_UNIVERSAL_JOINT.get(), AeroUniversalJointConfig::jointStressImpact));
    }
}
