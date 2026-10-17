package com.enxv.aerouniversaljoint.content.ponder;

import com.enxv.aerouniversaljoint.ModItems;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public final class AeroUniversalJointPonderScenes {
    private AeroUniversalJointPonderScenes() {
    }

    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        ResourceLocation simulatedContraptions = ResourceLocation.fromNamespaceAndPath("simulated",
                "physics_assembler/simulated_contraptions");

        helper.forComponents(
                        ModItems.BRASS_UNIVERSAL_JOINT.getId(),
                        ModItems.UNIVERSAL_JOINT_ROD.getId(),
                        ModItems.ANDESITE_UNIVERSAL_JOINT_ROD.getId())
                .addStoryBoard(simulatedContraptions, UniversalJointScenes::universalJointVariants);
    }
}
