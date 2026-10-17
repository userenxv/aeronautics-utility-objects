package com.enxv.aerouniversaljoint;

import com.enxv.aerouniversaljoint.content.PendingRodSelections;
import com.enxv.aerouniversaljoint.content.RecentMoveRemapper;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class AeroUniversalJointCommonEvents {
    private AeroUniversalJointCommonEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(AeroUniversalJointCommonEvents::onPlayerClone);
        NeoForge.EVENT_BUS.addListener(AeroUniversalJointCommonEvents::onPlayerRespawn);
        NeoForge.EVENT_BUS.addListener(AeroUniversalJointCommonEvents::onPlayerChangedDimension);
        NeoForge.EVENT_BUS.addListener(AeroUniversalJointCommonEvents::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(AeroUniversalJointCommonEvents::onServerTick);
        NeoForge.EVENT_BUS.addListener(AeroUniversalJointCommonEvents::onServerStopped);
    }

    private static void onPlayerClone(PlayerEvent.Clone event) {
        clearSelections(event.getOriginal());
        clearSelections(event.getEntity());
    }

    private static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        clearSelections(event.getEntity());
    }

    private static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        clearSelections(event.getEntity());
    }

    private static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        clearSelections(event.getEntity());
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        RecentMoveRemapper.pruneExpired(event.getServer().overworld().getGameTime());
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        RecentMoveRemapper.clear();
    }

    private static void clearSelections(net.minecraft.world.entity.player.Player player) {
        PendingRodSelections.clear(player);
        PendingRodSelections.clearClient(player);
    }
}
