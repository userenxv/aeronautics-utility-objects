package com.enxv.aerouniversaljoint.verification;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CompatibilityContractRegressionCheck {
    private CompatibilityContractRegressionCheck() {
    }

    public static void main(String[] args) throws IOException {
        Path root = Path.of("").toAbsolutePath();
        String blockEntities = read(root, "src/main/java/com/enxv/aerouniversaljoint/ModBlockEntities.java");
        String blocks = read(root, "src/main/java/com/enxv/aerouniversaljoint/ModBlocks.java");
        String mod = read(root, "src/main/java/com/enxv/aerouniversaljoint/AeroUniversalJointMod.java");
        String universalJoint = read(root,
                "src/main/java/com/enxv/aerouniversaljoint/content/UniversalJointBlockEntity.java");
        String moveRemapper = read(root,
                "src/main/java/com/enxv/aerouniversaljoint/content/RecentMoveRemapper.java");
        String moveHandler = read(root,
                "src/main/java/com/enxv/aerouniversaljoint/content/SubLevelMoveHandler.java");
        String universalJointBlock = read(root,
                "src/main/java/com/enxv/aerouniversaljoint/content/UniversalJointBlock.java");
        String commonEvents = read(root,
                "src/main/java/com/enxv/aerouniversaljoint/AeroUniversalJointCommonEvents.java");
        String modMetadata = read(root, "src/main/templates/META-INF/neoforge.mods.toml");
        String gradleProperties = read(root, "gradle.properties");

        requireContains(blockEntities, "\"universal_joint\"", "universal joint block-entity id");
        requireContains(blocks, "\"brass_universal_joint\"", "universal joint head block id");
        requireLinkReferenceTags(universalJoint, "universal joint");
        requireContains(modMetadata, "modId=\"sable\"", "Sable dependency");
        requireContains(modMetadata, "versionRange=\"${sable_version_range}\"", "Sable version-range template");
        requireContains(gradleProperties, "sable_version_range=[2.0.0,)", "Sable 2.x minimum dependency");
        requireMoveLifecycle(moveRemapper, moveHandler, universalJoint, universalJointBlock, commonEvents);
        requireNotContains(universalJoint, "TAG_SPEED_RATIO", "disabled speed-ratio persistence");
        requireContains(mod, "ModConfig.Type.COMMON", "file-based common config");
        requireNotContains(mod, "ModConfig.Type.SERVER", "per-world server config");
        requireContains(mod, "BlockStressValues.IMPACTS.register", "configurable joint stress impact");
    }

    private static String read(Path root, String relativePath) throws IOException {
        return Files.readString(root.resolve(relativePath));
    }

    private static void requireLinkReferenceTags(String source, String subject) {
        requireContains(source, "TAG_LINKED_POS = \"LinkedPos\"", subject + " LinkedPos tag");
        requireContains(source, "TAG_LINKED_SUB_LEVEL = \"LinkedSubLevel\"", subject + " LinkedSubLevel tag");
    }

    private static void requireMoveLifecycle(String remapper, String handler, String universalJoint,
                                             String universalJointBlock, String commonEvents) {
        requireContains(remapper, "ENTRY_TTL_TICKS", "game-tick move-remap expiry");
        requireContains(handler, "RecentMoveRemapper.prepare", "shared move preparation");
        requireContains(handler, "RecentMoveRemapper.record", "shared move completion");
        requireContains(universalJoint, "boolean preservingLinkForSubLevelMove", "universal-joint move transaction");
        requireContains(universalJointBlock, "SubLevelMoveHandler.beforeMove", "universal-joint shared move handler");
        requireContains(commonEvents, "RecentMoveRemapper.pruneExpired", "per-tick move-remap cleanup");
        requireContains(commonEvents, "RecentMoveRemapper.clear", "server-stop move-remap cleanup");
        requireNotContains(remapper, "System.currentTimeMillis", "wall-clock move-remap expiry");
        requireNotContains(universalJoint, "System.currentTimeMillis", "universal-joint wall-clock move grace period");
    }

    private static void requireContains(String source, String expected, String description) {
        if (!source.contains(expected)) {
            throw new AssertionError("Missing compatibility contract: " + description);
        }
    }

    private static void requireNotContains(String source, String forbidden, String description) {
        if (source.contains(forbidden)) {
            throw new AssertionError("Unexpected implementation detail: " + description);
        }
    }

    private static void requireCondition(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError("Missing compatibility contract: " + description);
        }
    }
}
