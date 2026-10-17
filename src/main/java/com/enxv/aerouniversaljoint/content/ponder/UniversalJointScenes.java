package com.enxv.aerouniversaljoint.content.ponder;

import com.enxv.aerouniversaljoint.ModBlocks;
import com.enxv.aerouniversaljoint.ModItems;
import com.enxv.aerouniversaljoint.content.JointVariant;
import com.enxv.aerouniversaljoint.content.UniversalJointBlock;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import dev.simulated_team.simulated.ponder.SmoothMovementUtils;
import dev.simulated_team.simulated.ponder.instructions.CustomAnimateWorldSectionInstruction;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class UniversalJointScenes {
    private UniversalJointScenes() {
    }

    public static void universalJointVariants(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = baseScene(builder, util, "universal_joint_variants", "Universal joint variants");
        var world = scene.world();
        var overlay = scene.overlay();
        var select = util.select();
        var vector = util.vector();

        BlockPos brassLeft = new BlockPos(2, 2, 2);
        BlockPos brassRight = new BlockPos(5, 2, 2);
        BlockPos andesiteLeft = new BlockPos(2, 2, 5);
        BlockPos andesiteRight = new BlockPos(7, 2, 5);
        BlockState brassLeftState = universalJoint(Direction.EAST);
        BlockState brassRightState = universalJoint(Direction.WEST);
        BlockState andesiteLeftState = universalJoint(Direction.EAST);
        BlockState andesiteRightState = universalJoint(Direction.WEST);

        world.setBlocks(select.layersFrom(1), Blocks.AIR.defaultBlockState(), false);
        world.setBlocks(select.fromTo(1, 1, 1, 2, 1, 3), AllBlocks.BRASS_CASING.getDefaultState(), false);
        world.setBlocks(select.fromTo(5, 1, 1, 6, 1, 3), AllBlocks.BRASS_CASING.getDefaultState(), false);
        world.setBlocks(select.fromTo(1, 1, 4, 2, 1, 6), AllBlocks.ANDESITE_CASING.getDefaultState(), false);
        world.setBlocks(select.fromTo(7, 1, 4, 8, 1, 6), AllBlocks.ANDESITE_CASING.getDefaultState(), false);
        world.setBlock(new BlockPos(1, 2, 2), AllBlocks.BRASS_CASING.getDefaultState(), false);
        world.setBlock(new BlockPos(6, 2, 2), AllBlocks.BRASS_CASING.getDefaultState(), false);
        world.setBlock(new BlockPos(1, 2, 5), AllBlocks.ANDESITE_CASING.getDefaultState(), false);
        world.setBlock(new BlockPos(8, 2, 5), AllBlocks.ANDESITE_CASING.getDefaultState(), false);
        world.setBlock(brassLeft, brassLeftState, false);
        world.setBlock(brassRight, brassRightState, false);
        world.setBlock(andesiteLeft, andesiteLeftState, false);
        world.setBlock(andesiteRight, andesiteRightState, false);

        ElementLink<WorldSectionElement> brassLeftSection = world.showIndependentSection(
                select.fromTo(1, 1, 1, 2, 1, 3)
                        .add(select.position(1, 2, 2))
                        .add(select.position(brassLeft)), Direction.DOWN);
        ElementLink<WorldSectionElement> brassRightSection = world.showIndependentSection(
                select.fromTo(5, 1, 1, 6, 1, 3)
                        .add(select.position(6, 2, 2))
                        .add(select.position(brassRight)), Direction.DOWN);
        scene.idle(5);
        ElementLink<WorldSectionElement> andesiteLeftSection = world.showIndependentSection(
                select.fromTo(1, 1, 4, 2, 1, 6)
                        .add(select.position(1, 2, 5))
                        .add(select.position(andesiteLeft)), Direction.DOWN);
        ElementLink<WorldSectionElement> andesiteRightSection = world.showIndependentSection(
                select.fromTo(7, 1, 4, 8, 1, 6)
                        .add(select.position(8, 2, 5))
                        .add(select.position(andesiteRight)), Direction.DOWN);
        scene.idle(8);
        UniversalJointLinkElement brassLink = new UniversalJointLinkElement(
                brassLeftSection, brassRightSection, Vec3.atCenterOf(brassLeft), Vec3.atCenterOf(brassRight),
                brassLeftState, JointVariant.BRASS);
        UniversalJointLinkElement andesiteLink = new UniversalJointLinkElement(
                andesiteLeftSection, andesiteRightSection, Vec3.atCenterOf(andesiteLeft), Vec3.atCenterOf(andesiteRight),
                andesiteLeftState, JointVariant.ANDESITE);
        scene.addInstruction(new CreateUniversalJointLinkInstruction(brassLink));
        scene.addInstruction(new CreateUniversalJointLinkInstruction(andesiteLink));
        scene.idle(12);

        overlay.showControls(vector.centerOf(brassLeft), Pointing.RIGHT, 45)
                .withItem(new ItemStack(ModItems.UNIVERSAL_JOINT_ROD.get()))
                .rightClick();
        overlay.showControls(vector.centerOf(andesiteLeft), Pointing.RIGHT, 45)
                .withItem(new ItemStack(ModItems.ANDESITE_UNIVERSAL_JOINT_ROD.get()))
                .rightClick();
        overlay.showText(55)
                .sharedText("universal_joint_connect")
                .pointAt(vector.centerOf(andesiteRight))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(64);

        overlay.showText(60)
                .sharedText("universal_joint_brass")
                .pointAt(vector.centerOf(brassRight).add(0.9D, 0.0D, 0.0D))
                .placeNearTarget()
                .attachKeyFrame();
        scene.addInstruction(CustomAnimateWorldSectionInstruction.move(brassRightSection,
                new Vec3(2.0D, 0.0D, 0.0D), 48, SmoothMovementUtils.cubicSmoothing()));
        scene.idle(54);
        scene.addInstruction(CustomAnimateWorldSectionInstruction.move(brassRightSection,
                new Vec3(-2.0D, 0.0D, 0.0D), 42, SmoothMovementUtils.cubicSmoothing()));
        scene.idle(42);

        overlay.showText(60)
                .sharedText("universal_joint_andesite")
                .pointAt(vector.centerOf(andesiteRight).add(0.9D, 0.0D, 0.0D))
                .placeNearTarget()
                .attachKeyFrame();
        scene.addInstruction(CustomAnimateWorldSectionInstruction.move(andesiteRightSection,
                new Vec3(2.25D, 0.0D, 0.0D), 58, SmoothMovementUtils.cubicSmoothing()));
        scene.idle(66);
        overlay.showText(60)
                .sharedText("universal_joint_compression_break")
                .pointAt(vector.centerOf(andesiteRight).add(-1.0D, 0.0D, 0.0D))
                .placeNearTarget()
                .attachKeyFrame();
        scene.addInstruction(CustomAnimateWorldSectionInstruction.move(andesiteRightSection,
                new Vec3(-4.75D, 0.0D, 0.0D), 76, SmoothMovementUtils.quadraticRiseInOut()));
        scene.idle(84);
        scene.addInstruction(CustomAnimateWorldSectionInstruction.move(andesiteRightSection,
                new Vec3(2.5D, 0.0D, 0.0D), 54, SmoothMovementUtils.cubicSmoothing()));
        scene.idle(52);

        overlay.showText(55)
                .sharedText("universal_joint_difference")
                .pointAt(vector.centerOf(andesiteLeft))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(60);
        scene.markAsFinished();
    }

    private static CreateSceneBuilder baseScene(SceneBuilder builder, SceneBuildingUtil util, String id, String fallbackTitle) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title(id, fallbackTitle);
        scene.configureBasePlate(0, 0, 8);
        scene.scaleSceneView(0.75F);
        scene.setSceneOffsetY(-1.0F);
        scene.showBasePlate();
        scene.world().setBlocks(util.select().layersFrom(1), Blocks.AIR.defaultBlockState(), false);
        return scene;
    }

    private static BlockState universalJoint(Direction facing) {
        return ModBlocks.BRASS_UNIVERSAL_JOINT.get()
                .defaultBlockState()
                .setValue(UniversalJointBlock.FACING, facing);
    }
}
