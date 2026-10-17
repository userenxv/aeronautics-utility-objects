package com.enxv.aerouniversaljoint.client;

import com.enxv.aerouniversaljoint.AeroUniversalJointMod;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;

public final class AeroUniversalJointPartials {
    public static final PartialModel UNIVERSAL_JOINT_LINK_CORE =
            block("universal_joint_link_core");
    public static final PartialModel ANDESITE_UNIVERSAL_JOINT_LINK_CORE =
            block("universal_joint_link_core0_a1");
    public static final PartialModel UNIVERSAL_JOINT_LINK_CORE_STRETCHED =
            block("universal_joint_link_core0");
    public static final PartialModel UNIVERSAL_JOINT_LINK_SLEEVE =
            block("universal_joint_link_sleeve");
    public static final PartialModel BRASS_UNIVERSAL_JOINT_HEAD =
            block("brass_universal_jointhead");

    private AeroUniversalJointPartials() {
    }

    public static void init() {
    }

    private static PartialModel block(String path) {
        return PartialModel.of(ResourceLocation.fromNamespaceAndPath(AeroUniversalJointMod.MOD_ID, "block/" + path));
    }

}

