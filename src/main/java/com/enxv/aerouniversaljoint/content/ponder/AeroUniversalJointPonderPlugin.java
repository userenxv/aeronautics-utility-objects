package com.enxv.aerouniversaljoint.content.ponder;

import com.enxv.aerouniversaljoint.AeroUniversalJointMod;
import com.simibubi.create.foundation.ponder.CreatePonderPlugin;
import com.simibubi.create.foundation.ponder.PonderWorldBlockEntityFix;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.api.registration.IndexExclusionHelper;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.createmod.ponder.api.registration.SharedTextRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public class AeroUniversalJointPonderPlugin extends CreatePonderPlugin {
    @Override
    public String getModId() {
        return AeroUniversalJointMod.MOD_ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        AeroUniversalJointPonderScenes.register(helper);
    }

    @Override
    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
    }

    @Override
    public void registerSharedText(SharedTextRegistrationHelper helper) {
        helper.registerSharedText("universal_joint_connect",
                "Both rods connect universal joint heads; the rod item decides which link is created.");
        helper.registerSharedText("universal_joint_brass",
                "Brass is a short elastic link; over-stretch it and the link turns red before it disconnects.");
        helper.registerSharedText("universal_joint_andesite",
                "Andesite reaches much farther, but it keeps the linked rest length.");
        helper.registerSharedText("universal_joint_compression_break",
                "Moving either longer or shorter than that rest length can strain the joint until it breaks.");
        helper.registerSharedText("universal_joint_difference",
                "Both variants warn in red near their limits; keep linked bearings close to the length they were joined at.");
    }

    @Override
    public void onPonderLevelRestore(PonderLevel ponderLevel) {
        PonderWorldBlockEntityFix.fixControllerBlockEntities(ponderLevel);
    }

    @Override
    public void indexExclusions(IndexExclusionHelper helper) {
    }
}
