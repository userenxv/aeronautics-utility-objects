package com.enxv.aerouniversaljoint.content.ponder;

import com.enxv.aerouniversaljoint.AeroUniversalJointMod;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.SharedTextRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

// Restore callbacks run for every scene. Create already repairs multiblock controllers;
// repeating that repair here would offset their controller positions a second time.
public class AeroUniversalJointPonderPlugin implements PonderPlugin {
    @Override
    public String getModId() {
        return AeroUniversalJointMod.MOD_ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        AeroUniversalJointPonderScenes.register(helper);
    }

    @Override
    public void registerSharedText(SharedTextRegistrationHelper helper) {
        helper.registerSharedText("hydraulic_rod_connect_after_assembly",
                "Connect two pneumatic heads after the structures have been assembled.");
        helper.registerSharedText("hydraulic_rod_stretch_along_connection",
                "The rod extends and retracts along the line between its two heads.");
        helper.registerSharedText("hydraulic_rod_return_force",
                "Target length and return force pull the moving end back toward the set distance.");
        helper.registerSharedText("hydraulic_rod_stretch_resistance",
                "Stretch resistance damps back-and-forth shaking along the rod axis.");
        helper.registerSharedText("hydraulic_rod_open_gui",
                "Empty-hand right-click a linked head to open the pneumatic rod settings.");
        helper.registerSharedText("hydraulic_rod_expected_length",
                "Expected Length moves the target distance along the link.");
        helper.registerSharedText("hydraulic_rod_return_force_setting",
                "Return Force decides how firmly the rod pulls back after it is displaced.");
        helper.registerSharedText("hydraulic_rod_stretch_resistance_setting",
                "Stretch Resistance absorbs axial shake without changing the target length.");
        helper.registerSharedText("hydraulic_rod_free_mode_setting",
                "Free mode lets the link slide instead of actively returning to the target.");
        helper.registerSharedText("hydraulic_rod_redstone_range_setting",
                "Redstone Range maps signal strength to the short and long target lengths.");
        helper.registerSharedText("regulator_place_behind_head",
                "Place a pneumatic regulator behind a head, facing into it, to control that link.");
        helper.registerSharedText("regulator_redstone_signal",
                "Redstone signal selects the target length from the head's redstone range.");
        helper.registerSharedText("regulator_speed_controls_transition",
                "Shaft speed controls how quickly the rod transitions to the new target.");
        helper.registerSharedText("regulator_requires_speed",
                "With no rotation, or when overstressed, the regulator stops commanding the head.");
        helper.registerSharedText("hinge_axis_setting",
                "The hinge head stores a pin axis that is different from the rod facing.");
        helper.registerSharedText("hinge_allows_axis_motion",
                "After linking, the end may swing around that pin axis.");
        helper.registerSharedText("hinge_rod_controls_length",
                "The hydraulic rod still controls only the distance between the two ends.");
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
        helper.registerSharedText("damping_bearing_assemble",
                "Empty-hand right-click to assemble the attached blocks into a physical rotating structure.");
        helper.registerSharedText("damping_bearing_reads_physics",
                "The bearing reads the real angular motion of the assembled structure.");
        helper.registerSharedText("damping_bearing_rear_output",
                "It publishes that motion as Create rotation from the rear shaft.");
        helper.registerSharedText("damping_bearing_resistance",
                "Sneak-right-click the bearing to open its rotation bearing settings and tune Resistance.");
        helper.registerSharedText("damping_bearing_damped_bearing",
                "It can also be used as a damped physical bearing, smoothing rotation before it reaches the shaft network.");
    }
}
