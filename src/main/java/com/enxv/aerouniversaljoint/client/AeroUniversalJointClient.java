package com.enxv.aerouniversaljoint.client;

import com.enxv.aerouniversaljoint.ModBlockEntities;
import com.enxv.aerouniversaljoint.ModMenuTypes;
import com.enxv.aerouniversaljoint.content.ponder.AeroUniversalJointPonderPlugin;
import com.enxv.aerouniversaljoint.content.DampingStressBearingRenderer;
import com.enxv.aerouniversaljoint.content.HydraulicConnectionHeadRenderer;
import com.enxv.aerouniversaljoint.content.HydraulicRegulatorRenderer;
import com.enxv.aerouniversaljoint.content.UniversalJointRenderer;
import net.createmod.ponder.foundation.PonderIndex;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class AeroUniversalJointClient {
    private AeroUniversalJointClient() {
    }

    public static void init(IEventBus modBus) {
        PonderIndex.addPlugin(new AeroUniversalJointPonderPlugin());
        AeroUniversalJointPartials.init();
        modBus.addListener(AeroUniversalJointClient::registerRenderers);
        modBus.addListener(AeroUniversalJointClient::registerScreens);
        NeoForge.EVENT_BUS.addListener(ConnectionPreviewRenderer::render);
        NeoForge.EVENT_BUS.addListener(HydraulicRodTargeting::render);
        NeoForge.EVENT_BUS.addListener(HydraulicRodTargeting::onInteraction);
        NeoForge.EVENT_BUS.addListener(HydraulicRodTargeting::onClientTick);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.UNIVERSAL_JOINT.get(), UniversalJointRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.HYDRAULIC_CONNECTION_HEAD.get(), HydraulicConnectionHeadRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.DAMPING_STRESS_BEARING.get(), DampingStressBearingRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.HYDRAULIC_REGULATOR.get(), HydraulicRegulatorRenderer::new);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.DAMPING_STRESS_BEARING.get(), DampingStressBearingScreen::new);
        event.register(ModMenuTypes.UNIVERSAL_JOINT.get(), UniversalJointScreen::new);
        event.register(ModMenuTypes.HYDRAULIC_CONNECTION_HEAD.get(), HydraulicConnectionHeadScreen::new);
    }

}
